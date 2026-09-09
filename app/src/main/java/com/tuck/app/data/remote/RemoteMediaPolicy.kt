package com.tuck.app.data.remote

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/** Whether Tuck is willing to fetch a URL, and if not, why. */
sealed interface FetchVerdict {
    data object Allowed : FetchVerdict
    data class Blocked(val reason: String) : FetchVerdict
}

/**
 * Guards every fetch of a URL that came out of page content rather than out of the user.
 *
 * The distinction matters. When the user shares a link, they chose that destination and Tuck
 * fetching it is the whole point. But `og:image`, a syndication payload's media list and a
 * `Location` header are all written by whoever controls the page, and Tuck fetches them from
 * inside the user's phone and the user's network. Unguarded, that turns a saved link into a
 * request against `127.0.0.1`, the router at `192.168.1.1`, a NAS, a printer — anything the
 * handset can reach but the attacker cannot, with timing differences leaking what is there.
 *
 * The defence is address-based rather than a host allowlist, because Tuck saves the whole web
 * and legitimate thumbnails live on arbitrary CDNs. [isAllowedHost] adds a tighter per-platform
 * allowlist for the cases where the media host family *is* known — see CAPTURE_ARCHITECTURE.md §6.
 */
@Singleton
class RemoteMediaPolicy @Inject constructor() {

    companion object {
        /** Media hosts X serves from. Anything else in a syndication payload is not X's. */
        val X_MEDIA_HOSTS = setOf("twimg.com")

        /** Media hosts Instagram serves from. */
        val INSTAGRAM_MEDIA_HOSTS = setOf("cdninstagram.com", "fbcdn.net")

        private const val MAX_REDIRECTS = 5
    }

    /** Redirect hops to follow, each of which is re-checked before it is taken. */
    val maxRedirects: Int get() = MAX_REDIRECTS

    /**
     * Whether [rawUrl] may be fetched. Resolves the host, so call it off the main thread.
     */
    fun check(rawUrl: String): FetchVerdict {
        val uri = try {
            URI(rawUrl.trim())
        } catch (e: Exception) {
            return FetchVerdict.Blocked("unparseable URL")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            // Blocks file://, content:// and android_asset among others, any of which would
            // turn a remote fetch into a read of the device's own storage.
            return FetchVerdict.Blocked("scheme not http(s): $scheme")
        }

        // `http://trusted.example@192.168.1.1/` reads as a trusted host to a human and to a
        // careless string check, but connects to the address after the '@'.
        if (uri.userInfo != null) return FetchVerdict.Blocked("URL carries userinfo")

        val host = uri.host?.takeIf { it.isNotBlank() }
            ?: return FetchVerdict.Blocked("no host")

        val addresses = try {
            InetAddress.getAllByName(host).toList()
        } catch (e: UnknownHostException) {
            return FetchVerdict.Blocked("host does not resolve: $host")
        } catch (e: Exception) {
            return FetchVerdict.Blocked("host resolution failed: $host")
        }

        return classify(addresses)
    }

    /**
     * The address half of [check], split out so it can be tested against literal addresses
     * without touching DNS.
     *
     * Every resolved address must be public: a name with one public and one private A record
     * is a rebinding attempt, not a fallback. Note the residual gap — the JVM resolves the host
     * again when the connection is opened, so a TTL-0 record can in principle answer
     * differently the second time. Closing that needs connecting to the pinned address with a
     * manual `Host` header, which breaks TLS verification unless done very carefully; for
     * fetching a thumbnail it is not worth the trade.
     */
    internal fun classify(addresses: List<InetAddress>): FetchVerdict {
        if (addresses.isEmpty()) return FetchVerdict.Blocked("host resolved to nothing")

        addresses.forEach { address ->
            reasonToBlock(address)?.let { reason ->
                return FetchVerdict.Blocked("$reason (${address.hostAddress})")
            }
        }
        return FetchVerdict.Allowed
    }

    private fun reasonToBlock(address: InetAddress): String? = when {
        address.isAnyLocalAddress -> "wildcard address"
        address.isLoopbackAddress -> "loopback address"
        address.isLinkLocalAddress -> "link-local address"
        address.isSiteLocalAddress -> "private address"
        address.isMulticastAddress -> "multicast address"
        address is Inet4Address && address.isCarrierGradeNat() -> "carrier-grade NAT address"
        address is Inet6Address && address.isUniqueLocal() -> "unique-local address"
        else -> null
    }

    /** 100.64.0.0/10 — shared address space, not covered by [isSiteLocalAddress]. */
    private fun Inet4Address.isCarrierGradeNat(): Boolean {
        val bytes = address
        val second = bytes[1].toInt() and 0xFF
        return (bytes[0].toInt() and 0xFF) == 100 && second in 64..127
    }

    /** fc00::/7 — `isSiteLocalAddress` only covers the deprecated fec0::/10. */
    private fun Inet6Address.isUniqueLocal(): Boolean =
        (address[0].toInt() and 0xFE) == 0xFC

    /**
     * Whether [rawUrl]'s host is one of [allowedHosts] or a subdomain of one.
     *
     * Matched on label boundaries so `twimg.com.evil.test` does not pass as `twimg.com`.
     * This is the tighter check for media URLs read out of a platform payload, where the
     * legitimate host family is known ahead of time.
     */
    fun isAllowedHost(rawUrl: String, allowedHosts: Set<String>): Boolean {
        val host = try {
            URI(rawUrl.trim()).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return false

        return allowedHosts.any { allowed ->
            val suffix = allowed.lowercase()
            host == suffix || host.endsWith(".$suffix")
        }
    }
}

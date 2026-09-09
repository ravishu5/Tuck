package com.tuck.app

import com.tuck.app.data.remote.FetchVerdict
import com.tuck.app.data.remote.RemoteMediaPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

/**
 * SSRF guard on page-controlled URLs — CAPTURE_ARCHITECTURE.md §6.
 *
 * These use literal addresses throughout so no test touches DNS.
 */
class RemoteMediaPolicyTest {

    private val policy = RemoteMediaPolicy()

    private fun addr(literal: String): InetAddress = InetAddress.getByName(literal)

    private fun assertBlocked(url: String) {
        val verdict = policy.check(url)
        assertTrue("expected $url to be blocked, got $verdict", verdict is FetchVerdict.Blocked)
    }

    private fun assertBlockedAddress(literal: String) {
        val verdict = policy.classify(listOf(addr(literal)))
        assertTrue("expected $literal to be blocked, got $verdict", verdict is FetchVerdict.Blocked)
    }

    // ------------------------------------------------------------- addresses

    @Test
    fun testLoopbackIsBlocked() {
        assertBlockedAddress("127.0.0.1")
        assertBlockedAddress("127.1.2.3")
        assertBlockedAddress("::1")
    }

    @Test
    fun testPrivateRangesAreBlocked() {
        assertBlockedAddress("10.0.0.1")
        assertBlockedAddress("172.16.5.4")
        assertBlockedAddress("172.31.255.254")
        // The router the phone is behind is the highest-value target on the list.
        assertBlockedAddress("192.168.1.1")
    }

    @Test
    fun testLinkLocalIsBlocked() {
        // 169.254.169.254 is the cloud metadata address; fe80:: is IPv6 link-local.
        assertBlockedAddress("169.254.169.254")
        assertBlockedAddress("fe80::1")
    }

    @Test
    fun testWildcardAndMulticastAreBlocked() {
        assertBlockedAddress("0.0.0.0")
        assertBlockedAddress("224.0.0.1")
    }

    @Test
    fun testCarrierGradeNatIsBlocked() {
        // 100.64.0.0/10 is not covered by isSiteLocalAddress.
        assertBlockedAddress("100.64.0.1")
        assertBlockedAddress("100.127.255.255")
    }

    @Test
    fun testUniqueLocalIpv6IsBlocked() {
        // fc00::/7 is not covered by isSiteLocalAddress either.
        assertBlockedAddress("fd00::1")
        assertBlockedAddress("fc00::1")
    }

    @Test
    fun testIpv4MappedLoopbackIsBlocked() {
        // ::ffff:127.0.0.1 must not sneak past the IPv4 loopback check.
        assertBlockedAddress("::ffff:127.0.0.1")
    }

    @Test
    fun testPublicAddressesAreAllowed() {
        assertEquals(FetchVerdict.Allowed, policy.classify(listOf(addr("93.184.216.34"))))
        assertEquals(FetchVerdict.Allowed, policy.classify(listOf(addr("2606:2800:220:1::1"))))
        // 100.63.x and 100.128.x sit just outside carrier-grade NAT.
        assertEquals(FetchVerdict.Allowed, policy.classify(listOf(addr("100.63.255.255"))))
        assertEquals(FetchVerdict.Allowed, policy.classify(listOf(addr("100.128.0.1"))))
    }

    @Test
    fun testOnePrivateRecordPoisonsTheWholeName() {
        // A name answering with both a public and a private address is a rebinding attempt,
        // not a fallback, so the public record must not rescue it.
        val verdict = policy.classify(listOf(addr("93.184.216.34"), addr("127.0.0.1")))
        assertTrue(verdict is FetchVerdict.Blocked)
    }

    @Test
    fun testEmptyResolutionIsBlocked() {
        assertTrue(policy.classify(emptyList()) is FetchVerdict.Blocked)
    }

    // ------------------------------------------------------------------ URLs

    @Test
    fun testNonHttpSchemesAreBlocked() {
        assertBlocked("file:///data/data/com.tuck.app/databases/tuck.db")
        assertBlocked("content://com.android.contacts/contacts")
        assertBlocked("ftp://example.com/image.jpg")
        assertBlocked("javascript:alert(1)")
    }

    @Test
    fun testUserInfoIsBlocked() {
        // Reads as a trusted host, connects to the address after the '@'.
        assertBlocked("http://cdn.example.com@127.0.0.1/image.jpg")
    }

    @Test
    fun testMalformedUrlsAreBlocked() {
        assertBlocked("")
        assertBlocked("not a url")
        assertBlocked("http://")
    }

    @Test
    fun testPrivateIpLiteralInUrlIsBlocked() {
        assertBlocked("http://192.168.1.1/admin")
        assertBlocked("http://127.0.0.1:8080/debug")
        assertBlocked("http://[::1]:3000/")
        // A non-standard port on a private host is the port-scan shape.
        assertBlocked("https://10.0.0.5:9200/_cluster/health")
    }

    @Test
    fun testPublicIpLiteralInUrlIsAllowed() {
        assertEquals(FetchVerdict.Allowed, policy.check("https://93.184.216.34/image.jpg"))
    }

    // ------------------------------------------------- per-platform allowlist

    @Test
    fun testPlatformAllowlistAcceptsHostAndSubdomains() {
        assertTrue(policy.isAllowedHost(
            "https://pbs.twimg.com/media/abc.jpg", RemoteMediaPolicy.X_MEDIA_HOSTS))
        assertTrue(policy.isAllowedHost(
            "https://video.twimg.com/ext_tw_video/1/vid.mp4", RemoteMediaPolicy.X_MEDIA_HOSTS))
        assertTrue(policy.isAllowedHost(
            "https://twimg.com/x.jpg", RemoteMediaPolicy.X_MEDIA_HOSTS))
        assertTrue(policy.isAllowedHost(
            "https://scontent.cdninstagram.com/v/x.jpg", RemoteMediaPolicy.INSTAGRAM_MEDIA_HOSTS))
    }

    @Test
    fun testPlatformAllowlistMatchesOnLabelBoundaries() {
        // The whole point: a suffix check without the dot lets these through.
        assertFalse(policy.isAllowedHost(
            "https://twimg.com.evil.test/x.jpg", RemoteMediaPolicy.X_MEDIA_HOSTS))
        assertFalse(policy.isAllowedHost(
            "https://eviltwimg.com/x.jpg", RemoteMediaPolicy.X_MEDIA_HOSTS))
        assertFalse(policy.isAllowedHost(
            "https://pbs.twimg.com.attacker.example/x.jpg", RemoteMediaPolicy.X_MEDIA_HOSTS))
    }

    @Test
    fun testPlatformAllowlistRejectsOtherHostsAndJunk() {
        assertFalse(policy.isAllowedHost(
            "https://cdn.example.com/x.jpg", RemoteMediaPolicy.X_MEDIA_HOSTS))
        assertFalse(policy.isAllowedHost("not a url", RemoteMediaPolicy.X_MEDIA_HOSTS))
        assertFalse(policy.isAllowedHost("", RemoteMediaPolicy.X_MEDIA_HOSTS))
    }
}

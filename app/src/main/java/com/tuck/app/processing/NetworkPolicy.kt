package com.tuck.app.processing

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.net.ConnectivityManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Decides whether background enrichment is allowed to use the network right now. */
@Singleton
class NetworkPolicy @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isActiveNetworkMetered(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        return ConnectivityManagerCompat.isActiveNetworkMetered(manager)
    }

    /**
     * Honors the "Wi-Fi Only for Previews" setting. When it blocks a fetch the caller
     * should retry later rather than saving a permanently un-enriched item.
     */
    fun allowsRemoteFetch(wifiOnly: Boolean): Boolean = !wifiOnly || !isActiveNetworkMetered()
}

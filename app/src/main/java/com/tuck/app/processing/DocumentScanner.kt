package com.tuck.app.processing

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * The real scanner behind Home's Scan button.
 *
 * ML Kit's scanner runs in a Play-Services-provided UI: edge detection, deskew, multi-page
 * and PDF output, none of it bundled into the APK - the whole dependency costs about 80KB
 * because the model is fetched on demand.
 *
 * That also means it is absent on de-Googled devices, so every entry point here reports
 * failure rather than throwing, and the caller falls back to the photo picker.
 */
object DocumentScanner {

    private val options: GmsDocumentScannerOptions by lazy {
        GmsDocumentScannerOptions.Builder()
            // Importing an existing photo is the same job; no reason to send people elsewhere.
            .setGalleryImportAllowed(true)
            .setPageLimit(MAX_PAGES)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }

    /**
     * Asks Play Services for the scanner UI.
     *
     * [onUnavailable] fires when the module is missing or cannot start, which is the normal
     * outcome on a device without Play Services rather than an error worth surfacing.
     */
    fun start(
        activity: Activity,
        onReady: (IntentSender) -> Unit,
        onUnavailable: () -> Unit
    ) {
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(activity)
            .addOnSuccessListener { onReady(it) }
            .addOnFailureListener { onUnavailable() }
    }

    /** The scanned PDF, or null if the scan was cancelled or produced nothing. */
    fun pdfFrom(data: Intent?): Uri? =
        GmsDocumentScanningResult.fromActivityResultIntent(data)?.pdf?.uri

    /** Page count, used only to title the save honestly. */
    fun pageCount(data: Intent?): Int =
        GmsDocumentScanningResult.fromActivityResultIntent(data)?.pages?.size ?: 0

    const val MAX_PAGES = 20
}

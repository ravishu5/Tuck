package com.tuck.app.domain.model

import androidx.annotation.StringRes
import com.tuck.app.R

enum class ContentType {
    URL,
    TEXT,
    IMAGE,
    PDF,
    DOCUMENT,
    VIDEO,
    AUDIO,
    MULTI_IMAGE,
    CONTACT,
    LOCATION,
    UNKNOWN;

    /**
     * The label to show a person. Prefer this over [displayName], which stays English for
     * logs and stored metadata.
     */
    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            URL -> R.string.type_link
            TEXT -> R.string.type_note
            IMAGE -> R.string.type_image
            PDF -> R.string.type_pdf
            DOCUMENT -> R.string.type_document
            VIDEO -> R.string.type_video
            AUDIO -> R.string.type_audio
            MULTI_IMAGE -> R.string.type_gallery
            CONTACT -> R.string.type_contact
            LOCATION -> R.string.type_location
            UNKNOWN -> R.string.type_item
        }

    val displayName: String
        get() = when (this) {
            URL -> "Link"
            TEXT -> "Note"
            IMAGE -> "Image"
            PDF -> "PDF"
            DOCUMENT -> "Document"
            VIDEO -> "Video"
            AUDIO -> "Audio"
            MULTI_IMAGE -> "Gallery"
            CONTACT -> "Contact"
            LOCATION -> "Location"
            UNKNOWN -> "Item"
        }
}

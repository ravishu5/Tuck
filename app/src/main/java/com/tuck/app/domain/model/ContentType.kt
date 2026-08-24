package com.tuck.app.domain.model

enum class ContentType {
    URL,
    TEXT,
    IMAGE,
    PDF,
    DOCUMENT,
    VIDEO,
    AUDIO,
    MULTI_IMAGE,
    UNKNOWN;

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
            UNKNOWN -> "Item"
        }
}

package com.tuck.app.domain.model

enum class EntityType {
    PERSON,
    ORGANIZATION,
    LOCATION,
    DATE,
    MONEY,
    EMAIL,
    PHONE,
    URL,
    PRODUCT,
    HASHTAG,
    OTHER
}

enum class SortOrder {
    RELEVANCE,
    NEWEST,
    OLDEST,
    RECENTLY_OPENED
}

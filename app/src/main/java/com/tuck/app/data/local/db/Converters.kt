package com.tuck.app.data.local.db

import androidx.room.TypeConverter
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.EntityType
import com.tuck.app.domain.model.ProcessingStatus

class Converters {
    @TypeConverter
    fun fromContentType(value: ContentType?): String? = value?.name

    @TypeConverter
    fun toContentType(value: String?): ContentType? = value?.let {
        try {
            ContentType.valueOf(it)
        } catch (e: Exception) {
            ContentType.UNKNOWN
        }
    }

    @TypeConverter
    fun fromProcessingStatus(value: ProcessingStatus?): String? = value?.name

    @TypeConverter
    fun toProcessingStatus(value: String?): ProcessingStatus? = value?.let {
        try {
            ProcessingStatus.valueOf(it)
        } catch (e: Exception) {
            ProcessingStatus.PENDING
        }
    }

    @TypeConverter
    fun fromEntityType(value: EntityType?): String? = value?.name

    @TypeConverter
    fun toEntityType(value: String?): EntityType? = value?.let {
        try {
            EntityType.valueOf(it)
        } catch (e: Exception) {
            EntityType.OTHER
        }
    }
}

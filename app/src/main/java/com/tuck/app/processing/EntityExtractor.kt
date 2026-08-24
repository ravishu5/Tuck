package com.tuck.app.processing

import com.tuck.app.domain.model.EntityType
import com.tuck.app.domain.model.ExtractedEntity
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntityExtractor @Inject constructor() {

    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
        Pattern.CASE_INSENSITIVE
    )

    private val urlPattern = Pattern.compile(
        "https?://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,})+(?::\\d+)?(?:/[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )

    private val phonePattern = Pattern.compile(
        "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}|(?:(?:\\+91|0)?[6-9]\\d{9})",
        Pattern.CASE_INSENSITIVE
    )

    private val moneyPattern = Pattern.compile(
        "(?:[₹$€£¥]|Rs\\.?|INR|USD|EUR)\\s*\\d+(?:,\\d+)*(?:\\.\\d{1,2})?|\\d+(?:,\\d+)*(?:\\.\\d{1,2})?\\s*(?:INR|USD|EUR|dollars|rupees|euros)",
        Pattern.CASE_INSENSITIVE
    )

    private val datePattern = Pattern.compile(
        "\\b\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}\\b|\\b\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}\\b|\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2}(?:st|nd|rd|th)?,?\\s+\\d{4}\\b|\\b\\d{1,2}(?:st|nd|rd|th)?\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*,?\\s+\\d{4}\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val hashtagPattern = Pattern.compile(
        "#[a-zA-Z0-9_]{2,}",
        Pattern.CASE_INSENSITIVE
    )

    fun extractEntities(text: String, savedItemId: Long = 0): List<ExtractedEntity> {
        if (text.isBlank()) return emptyList()

        val results = mutableListOf<ExtractedEntity>()
        val seen = mutableSetOf<String>()

        fun addMatch(type: EntityType, value: String, normalized: String = value.trim()) {
            val key = "${type.name}:$normalized"
            if (seen.add(key)) {
                results.add(
                    ExtractedEntity(
                        savedItemId = savedItemId,
                        type = type,
                        value = value.trim(),
                        normalizedValue = normalized
                    )
                )
            }
        }

        // 1. URLs
        val urlMatcher = urlPattern.matcher(text)
        while (urlMatcher.find()) {
            val value = urlMatcher.group()
            addMatch(EntityType.URL, value, value.lowercase())
        }

        // 2. Emails
        val emailMatcher = emailPattern.matcher(text)
        while (emailMatcher.find()) {
            val value = emailMatcher.group()
            addMatch(EntityType.EMAIL, value, value.lowercase())
        }

        // 3. Phone Numbers
        val phoneMatcher = phonePattern.matcher(text)
        while (phoneMatcher.find()) {
            val value = phoneMatcher.group()
            val cleanDigits = value.replace(Regex("[^0-9+]"), "")
            if (cleanDigits.length >= 10) {
                addMatch(EntityType.PHONE, value, cleanDigits)
            }
        }

        // 4. Money / Currency
        val moneyMatcher = moneyPattern.matcher(text)
        while (moneyMatcher.find()) {
            val value = moneyMatcher.group()
            addMatch(EntityType.MONEY, value, value.replace("\\s+".toRegex(), " "))
        }

        // 5. Dates
        val dateMatcher = datePattern.matcher(text)
        while (dateMatcher.find()) {
            val value = dateMatcher.group()
            addMatch(EntityType.DATE, value, value)
        }

        // 6. Hashtags
        val hashtagMatcher = hashtagPattern.matcher(text)
        while (hashtagMatcher.find()) {
            val value = hashtagMatcher.group()
            addMatch(EntityType.HASHTAG, value, value.lowercase())
        }

        return results
    }
}

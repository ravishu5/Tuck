package com.tuck.app.processing

import com.tuck.app.domain.classifier.ClassificationResult
import com.tuck.app.domain.classifier.ContentClassifier
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.EntityType
import com.tuck.app.domain.model.SavedItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleBasedContentClassifier @Inject constructor() : ContentClassifier {

    private data class CategoryRule(
        val category: String,
        val domainKeywords: List<String>,
        val contentKeywords: List<String>,
        val requiredContentType: ContentType? = null,
        val requiredEntityType: EntityType? = null
    )

    private val rules = listOf(
        CategoryRule(
            category = "Programming",
            domainKeywords = listOf("github", "gitlab", "stackoverflow", "stackexchange", "dev.to", "hashnode", "npmjs", "pypi", "developer.android", "developer.apple", "react.dev", "kotlinlang", "rust-lang", "golang"),
            contentKeywords = listOf("react", "compose", "kotlin", "javascript", "typescript", "python", "rust", "golang", "swift", "java", "c++", "function", "class", "api", "sdk", "git", "repository", "bug", "stacktrace", "algorithm", "database", "sql", "docker", "kubernetes", "frontend", "backend", "compiler", "pull request", "npm", "gradle", "android studio", "ios", "debug")
        ),
        CategoryRule(
            category = "Research",
            domainKeywords = listOf("arxiv", "semanticscholar", "pubmed", "nature.com", "sciencedirect", "ieee", "acm.org", "researchgate"),
            contentKeywords = listOf("research", "abstract", "methodology", "dataset", "empirical", "citation", "hypothesis", "conclusion", "experiment", "peer-reviewed", "arxiv", "paper", "survey", "literature review", "findings")
        ),
        CategoryRule(
            category = "Shopping",
            domainKeywords = listOf("amazon", "flipkart", "ebay", "target.com", "walmart", "etsy", "bestbuy", "myntra", "ajio", "aliexpress", "ikea", "shopify"),
            contentKeywords = listOf("buy", "cart", "order", "price", "discount", "deal", "sale", "shipping", "product", "reviews", "headphone", "laptop", "monitor", "shoes", "clothing", "delivery", "in stock", "checkout"),
            requiredEntityType = EntityType.MONEY
        ),
        CategoryRule(
            category = "Travel",
            domainKeywords = listOf("booking.com", "airbnb", "tripadvisor", "expedia", "skyscanner", "makemytrip", "kayak", "agoda", "maps.google"),
            contentKeywords = listOf("hotel", "resort", "flight", "airline", "airport", "destination", "ticket", "tour", "trip", "travel", "itinerary", "booking", "visa", "vacation", "beach", "sightseeing", "train")
        ),
        CategoryRule(
            category = "Food & Dining",
            domainKeywords = listOf("swiggy", "zomato", "yelp", "allrecipes", "foodnetwork", "opentable", "doordash", "ubereats"),
            contentKeywords = listOf("restaurant", "cafe", "menu", "recipe", "ingredients", "dish", "cuisine", "breakfast", "lunch", "dinner", "pizza", "burger", "biryani", "dessert", "bakery", "cook", "chef", "dining")
        ),
        CategoryRule(
            category = "Finance",
            domainKeywords = listOf("zerodha", "groww", "robinhood", "coinmarketcap", "coingecko", "bloomberg", "reuters", "wsj", "moneycontrol", "mint.com"),
            contentKeywords = listOf("investment", "stock", "portfolio", "crypto", "bitcoin", "ethereum", "invoice", "tax", "dividend", "mutual fund", "interest rate", "bank", "loan", "trading", "asset", "expense", "budget", "profit", "insurance")
        ),
        CategoryRule(
            category = "Work",
            domainKeywords = listOf("linkedin", "slack.com", "jira", "atlassian", "asana.com", "notion.so", "trello", "figma", "loom.com", "zoom.us"),
            contentKeywords = listOf("meeting", "presentation", "resume", "candidate", "sprint", "roadmap", "standup", "client", "proposal", "stakeholder", "project", "hiring", "colleague", "interview")
        ),
        CategoryRule(
            category = "Education",
            domainKeywords = listOf("coursera", "edx.org", "udemy", "khanacademy", "mit.edu", "stanford.edu", "harvard.edu", "duolingo", "quizlet"),
            contentKeywords = listOf("course", "tutorial", "syllabus", "lecture", "professor", "student", "exam", "homework", "lesson", "learning", "certification", "university", "degree")
        ),
        CategoryRule(
            category = "Videos",
            domainKeywords = listOf("youtube.com", "youtu.be", "vimeo.com", "tiktok.com", "twitch.tv", "dailymotion.com"),
            contentKeywords = listOf("video", "watch", "subscriber", "channel", "playlist", "views"),
            requiredContentType = ContentType.VIDEO
        ),
        CategoryRule(
            category = "Images",
            domainKeywords = listOf("unsplash", "pinterest", "imgur", "flickr", "instagram.com"),
            contentKeywords = listOf("photo", "image", "screenshot", "wallpaper", "picture", "infographic", "meme"),
            requiredContentType = ContentType.IMAGE
        ),
        CategoryRule(
            category = "PDFs",
            domainKeywords = emptyList(),
            contentKeywords = listOf("pdf", "document", "page", "section"),
            requiredContentType = ContentType.PDF
        ),
        CategoryRule(
            category = "Articles",
            domainKeywords = listOf("medium.com", "substack.com", "theverge.com", "techcrunch.com", "nytimes.com", "bbc.com", "cnn.com", "wired.com", "theatlantic.com"),
            contentKeywords = listOf("article", "news", "editorial", "opinion", "read", "author", "published", "journalism")
        )
    )

    override suspend fun classify(item: SavedItem): ClassificationResult {
        val domain = item.sourceDomain?.lowercase().orEmpty()
        val combinedText = buildString {
            append(item.title).append(" ")
            append(item.description.orEmpty()).append(" ")
            append(item.originalText.orEmpty()).append(" ")
            append(item.extractedText.orEmpty()).append(" ")
            append(item.ocrText.orEmpty()).append(" ")
            append(item.originalUrl.orEmpty()).append(" ")
        }.lowercase()

        val categoryScores = mutableMapOf<String, Float>()

        for (rule in rules) {
            var score = 0f

            // Check ContentType match
            if (rule.requiredContentType != null && item.contentType == rule.requiredContentType) {
                score += 3.0f
            }

            // Check domain match
            if (domain.isNotBlank()) {
                if (rule.domainKeywords.any { domain.contains(it) }) {
                    score += 4.0f
                }
            }

            // Check entity match
            if (rule.requiredEntityType != null && item.entities.any { it.type == rule.requiredEntityType }) {
                score += 2.0f
            }

            // Check keyword occurrences
            var keywordMatches = 0
            for (kw in rule.contentKeywords) {
                if (combinedText.contains(kw)) {
                    keywordMatches++
                }
            }
            score += (keywordMatches.coerceAtMost(5) * 1.0f)

            if (score > 0f) {
                categoryScores[rule.category] = score
            }
        }

        val sorted = categoryScores.entries.sortedByDescending { it.value }
        val primary = sorted.firstOrNull()?.key ?: when (item.contentType) {
            ContentType.IMAGE, ContentType.MULTI_IMAGE -> "Images"
            ContentType.PDF -> "PDFs"
            ContentType.VIDEO -> "Videos"
            ContentType.URL -> "Articles"
            else -> "Other"
        }

        val matchedCategories = sorted.map { it.key }
        val suggestedTags = mutableListOf<String>()

        // Generate tags from primary category and hashtags
        item.entities.filter { it.type == EntityType.HASHTAG }.forEach {
            suggestedTags.add(it.value.removePrefix("#"))
        }

        if (primary != "Other") {
            suggestedTags.add(primary.lowercase().replace(" & ", "-").replace(" ", "-"))
        }

        val confidence = (sorted.firstOrNull()?.value ?: 1f) / 10f

        return ClassificationResult(
            primaryCategory = primary,
            matchedCategories = matchedCategories,
            suggestedTags = suggestedTags.distinct(),
            confidence = confidence.coerceIn(0.1f, 1.0f)
        )
    }
}

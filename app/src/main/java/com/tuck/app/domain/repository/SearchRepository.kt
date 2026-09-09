package com.tuck.app.domain.repository

import com.tuck.app.domain.model.SearchFilter
import com.tuck.app.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface SearchEngine {
    val name: String
    suspend fun search(filter: SearchFilter): List<SearchResult>
}

interface SearchRepository {
    fun search(filter: SearchFilter): Flow<List<SearchResult>>
    suspend fun executeSearch(filter: SearchFilter): List<SearchResult>
    fun getRecentSearchQueries(): Flow<List<String>>
    suspend fun saveSearchQuery(query: String)
    suspend fun clearSearchHistory()
}

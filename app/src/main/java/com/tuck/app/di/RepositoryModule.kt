package com.tuck.app.di

import com.tuck.app.data.memory.RelatedItemsEngineImpl
import com.tuck.app.data.repository.CollectionRepositoryImpl
import com.tuck.app.data.repository.SavedItemRepositoryImpl
import com.tuck.app.data.repository.SearchRepositoryImpl
import com.tuck.app.data.repository.SettingsRepositoryImpl
import com.tuck.app.domain.classifier.ContentClassifier
import com.tuck.app.domain.memory.RelatedItemsEngine
import com.tuck.app.domain.repository.CollectionRepository
import com.tuck.app.domain.repository.SavedItemRepository
import com.tuck.app.domain.repository.SearchRepository
import com.tuck.app.domain.repository.SettingsRepository
import com.tuck.app.processing.RuleBasedContentClassifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSavedItemRepository(
        impl: SavedItemRepositoryImpl
    ): SavedItemRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        impl: SearchRepositoryImpl
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(
        impl: CollectionRepositoryImpl
    ): CollectionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindContentClassifier(
        impl: RuleBasedContentClassifier
    ): ContentClassifier

    @Binds
    @Singleton
    abstract fun bindRelatedItemsEngine(
        impl: RelatedItemsEngineImpl
    ): RelatedItemsEngine
}

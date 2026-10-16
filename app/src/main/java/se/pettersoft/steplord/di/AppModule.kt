package se.pettersoft.steplord.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import se.pettersoft.steplord.data.repository.StepsUpdatedListener
import se.pettersoft.steplord.notifications.GoalReachedNotifier
import se.pettersoft.steplord.ui.home.LocaleWeekStart
import se.pettersoft.steplord.ui.home.WeekStart
import se.pettersoft.steplord.util.CurrentDate
import se.pettersoft.steplord.util.SystemCurrentDate
import javax.inject.Qualifier
import javax.inject.Singleton

/** Scope for work that should outlive any single screen, such as app startup tasks. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindStepsUpdatedListener(impl: GoalReachedNotifier): StepsUpdatedListener

    @Binds
    abstract fun bindCurrentDate(impl: SystemCurrentDate): CurrentDate

    @Binds
    abstract fun bindWeekStart(impl: LocaleWeekStart): WeekStart

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

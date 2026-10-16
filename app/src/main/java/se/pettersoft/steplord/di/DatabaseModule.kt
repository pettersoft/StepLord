package se.pettersoft.steplord.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import se.pettersoft.steplord.data.local.AppDatabase
import se.pettersoft.steplord.data.local.DailyStepsDao
import se.pettersoft.steplord.data.local.UserSettingsDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "steplord.db").build()

    @Provides
    fun provideDailyStepsDao(db: AppDatabase): DailyStepsDao = db.dailyStepsDao()

    @Provides
    fun provideUserSettingsDao(db: AppDatabase): UserSettingsDao = db.userSettingsDao()
}

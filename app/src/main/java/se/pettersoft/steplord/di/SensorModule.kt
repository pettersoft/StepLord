package se.pettersoft.steplord.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import se.pettersoft.steplord.data.history.RecordingApiStepHistory
import se.pettersoft.steplord.data.history.StepHistory
import se.pettersoft.steplord.data.sensor.AndroidStepSensor
import se.pettersoft.steplord.data.sensor.StepSensor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SensorModule {
    @Binds
    @Singleton
    abstract fun bindStepSensor(impl: AndroidStepSensor): StepSensor

    @Binds
    @Singleton
    abstract fun bindStepHistory(impl: RecordingApiStepHistory): StepHistory
}

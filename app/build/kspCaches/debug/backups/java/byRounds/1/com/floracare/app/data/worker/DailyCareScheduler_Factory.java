package com.floracare.app.data.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.floracare.app.domain.repository.PlantRepository;
import com.floracare.app.domain.repository.WeatherRepository;
import com.floracare.app.domain.usecase.ComputeNextCareTaskUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class DailyCareScheduler_Factory {
  private final Provider<PlantRepository> plantsProvider;

  private final Provider<WeatherRepository> weatherProvider;

  private final Provider<ComputeNextCareTaskUseCase> computeNextTaskProvider;

  public DailyCareScheduler_Factory(Provider<PlantRepository> plantsProvider,
      Provider<WeatherRepository> weatherProvider,
      Provider<ComputeNextCareTaskUseCase> computeNextTaskProvider) {
    this.plantsProvider = plantsProvider;
    this.weatherProvider = weatherProvider;
    this.computeNextTaskProvider = computeNextTaskProvider;
  }

  public DailyCareScheduler get(Context context, WorkerParameters params) {
    return newInstance(context, params, plantsProvider.get(), weatherProvider.get(), computeNextTaskProvider.get());
  }

  public static DailyCareScheduler_Factory create(Provider<PlantRepository> plantsProvider,
      Provider<WeatherRepository> weatherProvider,
      Provider<ComputeNextCareTaskUseCase> computeNextTaskProvider) {
    return new DailyCareScheduler_Factory(plantsProvider, weatherProvider, computeNextTaskProvider);
  }

  public static DailyCareScheduler newInstance(Context context, WorkerParameters params,
      PlantRepository plants, WeatherRepository weather,
      ComputeNextCareTaskUseCase computeNextTask) {
    return new DailyCareScheduler(context, params, plants, weather, computeNextTask);
  }
}

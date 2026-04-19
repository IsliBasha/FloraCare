package com.floracare.app.data.repository;

import com.floracare.app.data.local.WeatherDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class WeatherRepositoryImpl_Factory implements Factory<WeatherRepositoryImpl> {
  private final Provider<WeatherDao> weatherDaoProvider;

  public WeatherRepositoryImpl_Factory(Provider<WeatherDao> weatherDaoProvider) {
    this.weatherDaoProvider = weatherDaoProvider;
  }

  @Override
  public WeatherRepositoryImpl get() {
    return newInstance(weatherDaoProvider.get());
  }

  public static WeatherRepositoryImpl_Factory create(Provider<WeatherDao> weatherDaoProvider) {
    return new WeatherRepositoryImpl_Factory(weatherDaoProvider);
  }

  public static WeatherRepositoryImpl newInstance(WeatherDao weatherDao) {
    return new WeatherRepositoryImpl(weatherDao);
  }
}

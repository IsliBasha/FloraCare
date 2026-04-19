package com.floracare.app.di;

import com.floracare.app.data.local.FloraCareDatabase;
import com.floracare.app.data.local.WeatherDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideWeatherDaoFactory implements Factory<WeatherDao> {
  private final Provider<FloraCareDatabase> dbProvider;

  public DatabaseModule_ProvideWeatherDaoFactory(Provider<FloraCareDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public WeatherDao get() {
    return provideWeatherDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideWeatherDaoFactory create(
      Provider<FloraCareDatabase> dbProvider) {
    return new DatabaseModule_ProvideWeatherDaoFactory(dbProvider);
  }

  public static WeatherDao provideWeatherDao(FloraCareDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideWeatherDao(db));
  }
}

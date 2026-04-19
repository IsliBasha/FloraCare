package com.floracare.app.di;

import com.floracare.app.data.local.FloraCareDatabase;
import com.floracare.app.data.local.PlantDao;
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
public final class DatabaseModule_ProvidePlantDaoFactory implements Factory<PlantDao> {
  private final Provider<FloraCareDatabase> dbProvider;

  public DatabaseModule_ProvidePlantDaoFactory(Provider<FloraCareDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public PlantDao get() {
    return providePlantDao(dbProvider.get());
  }

  public static DatabaseModule_ProvidePlantDaoFactory create(
      Provider<FloraCareDatabase> dbProvider) {
    return new DatabaseModule_ProvidePlantDaoFactory(dbProvider);
  }

  public static PlantDao providePlantDao(FloraCareDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePlantDao(db));
  }
}

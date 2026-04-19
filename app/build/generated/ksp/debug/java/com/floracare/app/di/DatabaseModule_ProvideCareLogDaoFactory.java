package com.floracare.app.di;

import com.floracare.app.data.local.CareLogDao;
import com.floracare.app.data.local.FloraCareDatabase;
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
public final class DatabaseModule_ProvideCareLogDaoFactory implements Factory<CareLogDao> {
  private final Provider<FloraCareDatabase> dbProvider;

  public DatabaseModule_ProvideCareLogDaoFactory(Provider<FloraCareDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CareLogDao get() {
    return provideCareLogDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideCareLogDaoFactory create(
      Provider<FloraCareDatabase> dbProvider) {
    return new DatabaseModule_ProvideCareLogDaoFactory(dbProvider);
  }

  public static CareLogDao provideCareLogDao(FloraCareDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCareLogDao(db));
  }
}

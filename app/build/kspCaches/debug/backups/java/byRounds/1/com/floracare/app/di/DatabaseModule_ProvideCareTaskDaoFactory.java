package com.floracare.app.di;

import com.floracare.app.data.local.CareTaskDao;
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
public final class DatabaseModule_ProvideCareTaskDaoFactory implements Factory<CareTaskDao> {
  private final Provider<FloraCareDatabase> dbProvider;

  public DatabaseModule_ProvideCareTaskDaoFactory(Provider<FloraCareDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CareTaskDao get() {
    return provideCareTaskDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideCareTaskDaoFactory create(
      Provider<FloraCareDatabase> dbProvider) {
    return new DatabaseModule_ProvideCareTaskDaoFactory(dbProvider);
  }

  public static CareTaskDao provideCareTaskDao(FloraCareDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCareTaskDao(db));
  }
}

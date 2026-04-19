package com.floracare.app.di;

import com.floracare.app.data.local.FloraCareDatabase;
import com.floracare.app.data.local.SpeciesDao;
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
public final class DatabaseModule_ProvideSpeciesDaoFactory implements Factory<SpeciesDao> {
  private final Provider<FloraCareDatabase> dbProvider;

  public DatabaseModule_ProvideSpeciesDaoFactory(Provider<FloraCareDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SpeciesDao get() {
    return provideSpeciesDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideSpeciesDaoFactory create(
      Provider<FloraCareDatabase> dbProvider) {
    return new DatabaseModule_ProvideSpeciesDaoFactory(dbProvider);
  }

  public static SpeciesDao provideSpeciesDao(FloraCareDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSpeciesDao(db));
  }
}

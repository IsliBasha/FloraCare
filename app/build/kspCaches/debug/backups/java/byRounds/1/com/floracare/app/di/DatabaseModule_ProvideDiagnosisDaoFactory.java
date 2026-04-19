package com.floracare.app.di;

import com.floracare.app.data.local.DiagnosisDao;
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
public final class DatabaseModule_ProvideDiagnosisDaoFactory implements Factory<DiagnosisDao> {
  private final Provider<FloraCareDatabase> dbProvider;

  public DatabaseModule_ProvideDiagnosisDaoFactory(Provider<FloraCareDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DiagnosisDao get() {
    return provideDiagnosisDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideDiagnosisDaoFactory create(
      Provider<FloraCareDatabase> dbProvider) {
    return new DatabaseModule_ProvideDiagnosisDaoFactory(dbProvider);
  }

  public static DiagnosisDao provideDiagnosisDao(FloraCareDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDiagnosisDao(db));
  }
}

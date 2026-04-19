package com.floracare.app.di;

import com.floracare.app.data.local.FloraCareDatabase;
import com.floracare.app.data.local.JournalDao;
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
public final class DatabaseModule_ProvideJournalDaoFactory implements Factory<JournalDao> {
  private final Provider<FloraCareDatabase> dbProvider;

  public DatabaseModule_ProvideJournalDaoFactory(Provider<FloraCareDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public JournalDao get() {
    return provideJournalDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideJournalDaoFactory create(
      Provider<FloraCareDatabase> dbProvider) {
    return new DatabaseModule_ProvideJournalDaoFactory(dbProvider);
  }

  public static JournalDao provideJournalDao(FloraCareDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideJournalDao(db));
  }
}

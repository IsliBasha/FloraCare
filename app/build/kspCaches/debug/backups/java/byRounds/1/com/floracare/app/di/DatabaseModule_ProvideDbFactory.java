package com.floracare.app.di;

import android.content.Context;
import com.floracare.app.data.local.FloraCareDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DatabaseModule_ProvideDbFactory implements Factory<FloraCareDatabase> {
  private final Provider<Context> ctxProvider;

  public DatabaseModule_ProvideDbFactory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public FloraCareDatabase get() {
    return provideDb(ctxProvider.get());
  }

  public static DatabaseModule_ProvideDbFactory create(Provider<Context> ctxProvider) {
    return new DatabaseModule_ProvideDbFactory(ctxProvider);
  }

  public static FloraCareDatabase provideDb(Context ctx) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDb(ctx));
  }
}

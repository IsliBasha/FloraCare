package com.floracare.app.data.ml;

import android.content.Context;
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
public final class TfliteSpeciesClassifier_Factory implements Factory<TfliteSpeciesClassifier> {
  private final Provider<Context> contextProvider;

  public TfliteSpeciesClassifier_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TfliteSpeciesClassifier get() {
    return newInstance(contextProvider.get());
  }

  public static TfliteSpeciesClassifier_Factory create(Provider<Context> contextProvider) {
    return new TfliteSpeciesClassifier_Factory(contextProvider);
  }

  public static TfliteSpeciesClassifier newInstance(Context context) {
    return new TfliteSpeciesClassifier(context);
  }
}

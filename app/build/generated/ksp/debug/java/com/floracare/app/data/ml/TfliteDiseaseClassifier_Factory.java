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
public final class TfliteDiseaseClassifier_Factory implements Factory<TfliteDiseaseClassifier> {
  private final Provider<Context> contextProvider;

  public TfliteDiseaseClassifier_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TfliteDiseaseClassifier get() {
    return newInstance(contextProvider.get());
  }

  public static TfliteDiseaseClassifier_Factory create(Provider<Context> contextProvider) {
    return new TfliteDiseaseClassifier_Factory(contextProvider);
  }

  public static TfliteDiseaseClassifier newInstance(Context context) {
    return new TfliteDiseaseClassifier(context);
  }
}

package com.floracare.app;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class FloraCareApp_MembersInjector implements MembersInjector<FloraCareApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public FloraCareApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<FloraCareApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new FloraCareApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(FloraCareApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.floracare.app.FloraCareApp.workerFactory")
  public static void injectWorkerFactory(FloraCareApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}

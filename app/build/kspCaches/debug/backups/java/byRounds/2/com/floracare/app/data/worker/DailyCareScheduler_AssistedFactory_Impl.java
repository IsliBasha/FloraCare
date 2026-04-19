package com.floracare.app.data.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class DailyCareScheduler_AssistedFactory_Impl implements DailyCareScheduler_AssistedFactory {
  private final DailyCareScheduler_Factory delegateFactory;

  DailyCareScheduler_AssistedFactory_Impl(DailyCareScheduler_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public DailyCareScheduler create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<DailyCareScheduler_AssistedFactory> create(
      DailyCareScheduler_Factory delegateFactory) {
    return InstanceFactory.create(new DailyCareScheduler_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<DailyCareScheduler_AssistedFactory> createFactoryProvider(
      DailyCareScheduler_Factory delegateFactory) {
    return InstanceFactory.create(new DailyCareScheduler_AssistedFactory_Impl(delegateFactory));
  }
}

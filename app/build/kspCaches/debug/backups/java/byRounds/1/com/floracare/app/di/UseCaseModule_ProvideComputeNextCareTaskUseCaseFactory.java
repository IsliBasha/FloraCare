package com.floracare.app.di;

import com.floracare.app.domain.usecase.ComputeNextCareTaskUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class UseCaseModule_ProvideComputeNextCareTaskUseCaseFactory implements Factory<ComputeNextCareTaskUseCase> {
  @Override
  public ComputeNextCareTaskUseCase get() {
    return provideComputeNextCareTaskUseCase();
  }

  public static UseCaseModule_ProvideComputeNextCareTaskUseCaseFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ComputeNextCareTaskUseCase provideComputeNextCareTaskUseCase() {
    return Preconditions.checkNotNullFromProvides(UseCaseModule.INSTANCE.provideComputeNextCareTaskUseCase());
  }

  private static final class InstanceHolder {
    private static final UseCaseModule_ProvideComputeNextCareTaskUseCaseFactory INSTANCE = new UseCaseModule_ProvideComputeNextCareTaskUseCaseFactory();
  }
}

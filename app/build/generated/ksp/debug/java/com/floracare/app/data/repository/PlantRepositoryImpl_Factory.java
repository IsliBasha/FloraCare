package com.floracare.app.data.repository;

import com.floracare.app.data.local.CareLogDao;
import com.floracare.app.data.local.CareTaskDao;
import com.floracare.app.data.local.PlantDao;
import com.floracare.app.data.local.SpeciesDao;
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
public final class PlantRepositoryImpl_Factory implements Factory<PlantRepositoryImpl> {
  private final Provider<PlantDao> plantDaoProvider;

  private final Provider<SpeciesDao> speciesDaoProvider;

  private final Provider<CareTaskDao> taskDaoProvider;

  private final Provider<CareLogDao> logDaoProvider;

  public PlantRepositoryImpl_Factory(Provider<PlantDao> plantDaoProvider,
      Provider<SpeciesDao> speciesDaoProvider, Provider<CareTaskDao> taskDaoProvider,
      Provider<CareLogDao> logDaoProvider) {
    this.plantDaoProvider = plantDaoProvider;
    this.speciesDaoProvider = speciesDaoProvider;
    this.taskDaoProvider = taskDaoProvider;
    this.logDaoProvider = logDaoProvider;
  }

  @Override
  public PlantRepositoryImpl get() {
    return newInstance(plantDaoProvider.get(), speciesDaoProvider.get(), taskDaoProvider.get(), logDaoProvider.get());
  }

  public static PlantRepositoryImpl_Factory create(Provider<PlantDao> plantDaoProvider,
      Provider<SpeciesDao> speciesDaoProvider, Provider<CareTaskDao> taskDaoProvider,
      Provider<CareLogDao> logDaoProvider) {
    return new PlantRepositoryImpl_Factory(plantDaoProvider, speciesDaoProvider, taskDaoProvider, logDaoProvider);
  }

  public static PlantRepositoryImpl newInstance(PlantDao plantDao, SpeciesDao speciesDao,
      CareTaskDao taskDao, CareLogDao logDao) {
    return new PlantRepositoryImpl(plantDao, speciesDao, taskDao, logDao);
  }
}

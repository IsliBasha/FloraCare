package com.floracare.app.di;

import com.squareup.moshi.Moshi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideWeatherRetrofitFactory implements Factory<Retrofit> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<Moshi> moshiProvider;

  public NetworkModule_ProvideWeatherRetrofitFactory(Provider<OkHttpClient> clientProvider,
      Provider<Moshi> moshiProvider) {
    this.clientProvider = clientProvider;
    this.moshiProvider = moshiProvider;
  }

  @Override
  public Retrofit get() {
    return provideWeatherRetrofit(clientProvider.get(), moshiProvider.get());
  }

  public static NetworkModule_ProvideWeatherRetrofitFactory create(
      Provider<OkHttpClient> clientProvider, Provider<Moshi> moshiProvider) {
    return new NetworkModule_ProvideWeatherRetrofitFactory(clientProvider, moshiProvider);
  }

  public static Retrofit provideWeatherRetrofit(OkHttpClient client, Moshi moshi) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideWeatherRetrofit(client, moshi));
  }
}

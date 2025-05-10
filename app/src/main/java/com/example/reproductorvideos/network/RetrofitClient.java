    package com.example.reproductorvideos.network;

    import retrofit2.Retrofit;
    import retrofit2.converter.gson.GsonConverterFactory;
    import okhttp3.OkHttpClient;
    import okhttp3.logging.HttpLoggingInterceptor;

    public class RetrofitClient {
        private static Retrofit retrofit = null;
        private static String BASE_URL = "http://10.20.106.81:3000/api/"; // valor por defecto

        // Permite cambiar la URL en tiempo de ejecución
        public static void setBaseUrl(String url) {
            if (url != null && !url.isEmpty()) {
                BASE_URL = url.endsWith("/") ? url : url + "/";
                retrofit = null; // Forzar recrear Retrofit
            }
        }

        private static Retrofit getRetrofitInstance() {
            if (retrofit == null) {
                if (BASE_URL == null || BASE_URL.isEmpty()) {
                    throw new IllegalStateException("❌ BASE_URL no puede ser null o vacía");
                }

                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(logging)
                        .build();

                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
            return retrofit;
        }

        public static ApiService getApiService() {
            return getRetrofitInstance().create(ApiService.class);
        }
    }

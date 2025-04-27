    package com.example.reproductorvideos;

    import android.os.Bundle;
    import android.util.Log;
    import android.widget.Toast;

    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.example.reproductorvideos.model.Video;
    import com.example.reproductorvideos.network.RetrofitClient;
    import com.example.reproductorvideos.ui.VideoAdapter;

    import java.io.IOException;
    import java.util.List;

    import retrofit2.Call;
    import retrofit2.Callback;
    import retrofit2.Response;

    public class MainActivity extends AppCompatActivity {

        private RecyclerView recyclerView;
        private VideoAdapter adapter;
        private static final String TAG = "MainActivity"; // Para loguear errores

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // Log para verificar si la actividad se ha creado correctamente
            Log.d(TAG, "onCreate: Actividad creada");

            recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Llamada para cargar los videos
            fetchVideos();
        }

        private void fetchVideos() {
            Log.d(TAG, "fetchVideos: Iniciando solicitud de videos");

            RetrofitClient.getApiService().getVideosByCategory("categoria1")
                    .enqueue(new Callback<List<Video>>() {
                        @Override
                        public void onResponse(Call<List<Video>> call, Response<List<Video>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<Video> videos = response.body();
                                Log.d(TAG, "onResponse: Videos recibidos, tamaño de la lista: " + videos.size());

                                // Configuración del adaptador y asignación al RecyclerView
                                adapter = new VideoAdapter(MainActivity.this, videos);
                                recyclerView.setAdapter(adapter);
                            } else {
                                // Log cuando la respuesta no es exitosa
                                String error = "Error en la carga de videos. Código: " + response.code();
                                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Respuesta no exitosa: " + error);

                                // Intentando leer el cuerpo del error (si lo hay)
                                try {
                                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "No hay cuerpo de error";
                                    Log.e(TAG, "Cuerpo del error: " + errorBody);
                                } catch (IOException e) {
                                    Log.e(TAG, "Error al leer el cuerpo del error", e);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Video>> call, Throwable t) {
                            // Log en caso de que falle la conexión
                            String error = "Fallo de conexión: " + t.getLocalizedMessage();
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error de conexión", t);
                        }
                    });
        }
    }

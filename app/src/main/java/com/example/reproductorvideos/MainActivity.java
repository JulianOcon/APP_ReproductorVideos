package com.example.reproductorvideos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.network.IPService;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.network.ServerInfo;
import com.example.reproductorvideos.ui.VideoAdapter;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private static final String TAG = "MainActivity";
    private String categoriaSeleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Recibir la categoría (puede venir vacía)
        Intent intent = getIntent();
        categoriaSeleccionada = intent.hasExtra("categoria")
                ? intent.getStringExtra("categoria")
                : "";

        Log.d(TAG, "Categoría seleccionada: '" + categoriaSeleccionada + "'");

        // 👉 Obtener IP del servidor antes de cargar videos
        obtenerUrlDelServidor();
    }

    private void obtenerUrlDelServidor() {
        // Retrofit temporal para consultar la IP pública
        Retrofit retrofitTemporal = new Retrofit.Builder()
                .baseUrl("http://10.20.106.75:3000/api/") // Dirección por defecto temporal
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        IPService ipService = retrofitTemporal.create(IPService.class);
        ipService.getServerInfo().enqueue(new Callback<ServerInfo>() {
            @Override
            public void onResponse(Call<ServerInfo> call, Response<ServerInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String ip = response.body().getIpPublica();
                    String url = "http://" + ip + ":3000/api/";
                    Log.d(TAG, "✅ IP del servidor obtenida: " + url);

                    RetrofitClient.setBaseUrl(url); // actualiza baseUrl dinámica
                    fetchVideos(); // luego carga los videos
                } else {
                    Toast.makeText(MainActivity.this, "No se pudo obtener la IP del servidor", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "❌ Falló obtener IP: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ServerInfo> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Fallo conexión IP: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "❌ Error al obtener IP dinámica", t);
            }
        });
    }

    private void fetchVideos() {
        ApiService api = RetrofitClient.getApiService();
        Call<List<Video>> call;

        if (categoriaSeleccionada == null || categoriaSeleccionada.isEmpty()) {
            call = api.getAllVideos();
        } else {
            call = api.getVideosByCategory(categoriaSeleccionada);
        }

        call.enqueue(new Callback<List<Video>>() {
            @Override
            public void onResponse(Call<List<Video>> call, Response<List<Video>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    List<Video> videos = resp.body();
                    adapter = new VideoAdapter(MainActivity.this, videos);
                    recyclerView.setAdapter(adapter);
                } else {
                    String err = "Error carga videos: " + resp.code();
                    Toast.makeText(MainActivity.this, err, Toast.LENGTH_LONG).show();
                    Log.e(TAG, err);
                }
            }

            @Override
            public void onFailure(Call<List<Video>> call, Throwable t) {
                String err = "Fallo de conexión: " + t.getMessage();
                Toast.makeText(MainActivity.this, err, Toast.LENGTH_LONG).show();
                Log.e(TAG, err, t);
            }
        });
    }
}

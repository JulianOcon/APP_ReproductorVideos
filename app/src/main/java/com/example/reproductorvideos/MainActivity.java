package com.example.reproductorvideos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.ui.VideoAdapter;
import java.io.IOException;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        fetchVideos();
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

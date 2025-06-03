package com.example.reproductorvideos;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.ui.VideoAdapter;
import com.example.reproductorvideos.utils.FavoritosManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private ImageButton btnBack;

    private List<Video> listaFavoritos = new ArrayList<>();
    private List<Video> todosLosVideos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoritos_videos);

        recyclerView = findViewById(R.id.recyclerViewFavoritos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VideoAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        cargarVideosFavoritos();
    }

    private void cargarVideosFavoritos() {
        RetrofitClient.getApiService(this).obtenerVideos().enqueue(new retrofit2.Callback<List<Video>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Video>> call, retrofit2.Response<List<Video>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    todosLosVideos = response.body();
                    Set<String> favoritos = FavoritosManager.obtenerFavoritos(FavoritosActivity.this);

                    listaFavoritos = new ArrayList<>();
                    for (Video video : todosLosVideos) {
                        if (favoritos.contains(video.getUrl())) {
                            video.setFavorito(true);
                            listaFavoritos.add(video);
                        }
                    }

                    if (listaFavoritos.isEmpty()) {
                        Toast.makeText(FavoritosActivity.this, "No tienes videos favoritos aún.", Toast.LENGTH_SHORT).show();
                    }

                    adapter.actualizarLista(listaFavoritos);
                } else {
                    Toast.makeText(FavoritosActivity.this, "Error al obtener videos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<Video>> call, Throwable t) {
                Toast.makeText(FavoritosActivity.this, "Fallo de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

package com.example.reproductorvideos;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Mp3File;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.ui.Mp3Adapter;
import com.example.reproductorvideos.utils.FavoritosManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritosMp3Activity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Mp3Adapter adapter;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoritos_mp3);

        recyclerView = findViewById(R.id.recyclerViewFavoritos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Mp3Adapter(this);
        recyclerView.setAdapter(adapter);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        cargarMp3Favoritos();
    }

    private void cargarMp3Favoritos() {
        RetrofitClient.getApiService(this).obtenerMp3().enqueue(new Callback<List<Mp3File>>() {
            @Override
            public void onResponse(Call<List<Mp3File>> call, Response<List<Mp3File>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Set<String> favoritos = FavoritosManager.obtenerFavoritosMp3(FavoritosMp3Activity.this);
                    List<Mp3File> soloFavoritos = new ArrayList<>();
                    for (Mp3File mp3 : response.body()) {
                        if (favoritos.contains(mp3.getUrl())) {
                            soloFavoritos.add(mp3);
                        }
                    }

                    if (soloFavoritos.isEmpty()) {
                        Toast.makeText(FavoritosMp3Activity.this, "No tienes MP3 favoritos aún.", Toast.LENGTH_SHORT).show();
                    }

                    adapter.setMp3List(soloFavoritos);
                } else {
                    Toast.makeText(FavoritosMp3Activity.this, "Error al obtener MP3", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Mp3File>> call, Throwable t) {
                Toast.makeText(FavoritosMp3Activity.this, "Fallo de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}


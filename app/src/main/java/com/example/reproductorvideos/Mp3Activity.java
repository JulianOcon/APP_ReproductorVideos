// Mp3Activity.java
package com.example.reproductorvideos;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Mp3File;
import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.ui.Mp3Adapter;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Mp3Activity extends AppCompatActivity {

    private RecyclerView recyclerViewMp3;
    private Mp3Adapter adapter;
    private Button btnSwitchMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3);

        recyclerViewMp3 = findViewById(R.id.recyclerViewMp3);
        btnSwitchMode   = findViewById(R.id.btnSwitchMode);

        // Botón para volver a video
        btnSwitchMode.setOnClickListener(v -> {
            startActivity(new Intent(Mp3Activity.this, MainActivity.class));
            finish();
        });

        // Configura RecyclerView con el adaptador “ui.Mp3Adapter”
        recyclerViewMp3.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Mp3Adapter(this);
        recyclerViewMp3.setAdapter(adapter);

        fetchMp3Files();
    }

    private void fetchMp3Files() {
        ApiService api = RetrofitClient.getApiService(this);
        api.obtenerMp3().enqueue(new Callback<List<Mp3File>>() {
            @Override
            public void onResponse(Call<List<Mp3File>> call,
                                   Response<List<Mp3File>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Aquí le das al adaptador la lista completa;
                    // internamente lanzará ExoPlayerActivity con lista+posición
                    adapter.setMp3List(response.body());
                } else {
                    Toast.makeText(Mp3Activity.this,
                            "Error al cargar MP3", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Mp3File>> call, Throwable t) {
                Toast.makeText(Mp3Activity.this,
                        "Fallo red: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private byte[] getAlbumArt(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(uri);
            return retriever.getEmbeddedPicture();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

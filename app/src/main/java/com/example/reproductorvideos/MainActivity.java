package com.example.reproductorvideos;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.ui.VideoAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchVideos();
    }

    private void fetchVideos() {
        RetrofitClient.getApiService().getVideosByCategory("categoria1")
                .enqueue(new Callback<List<Video>>() {
                    @Override
                    public void onResponse(Call<List<Video>> call, Response<List<Video>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Video> videos = response.body();
                            adapter = new VideoAdapter(MainActivity.this, videos);
                            recyclerView.setAdapter(adapter);
                        } else {
                            Toast.makeText(MainActivity.this, "Error en la carga de videos", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Video>> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

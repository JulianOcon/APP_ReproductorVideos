package com.example.reproductorvideos;

import android.content.Intent;
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
        btnSwitchMode = findViewById(R.id.btnSwitchMode);

        btnSwitchMode.setText("MP4");
        btnSwitchMode.setOnClickListener(v -> {
            Intent intent = new Intent(Mp3Activity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        recyclerViewMp3.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Mp3Adapter(this);
        recyclerViewMp3.setAdapter(adapter);

        fetchMp3Files();
    }

    private void fetchMp3Files() {
        ApiService api = RetrofitClient.getApiService(this);
        api.obtenerMp3().enqueue(new Callback<List<Mp3File>>() {
            @Override
            public void onResponse(Call<List<Mp3File>> call, Response<List<Mp3File>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setMp3List(response.body());
                } else {
                    Toast.makeText(Mp3Activity.this, "Error al cargar MP3", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Mp3File>> call, Throwable t) {
                Toast.makeText(Mp3Activity.this, "Fallo red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
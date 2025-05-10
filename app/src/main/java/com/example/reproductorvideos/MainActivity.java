package com.example.reproductorvideos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.network.IPService;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.network.ServerInfo;
import com.example.reproductorvideos.ui.VideoAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private EditText searchEditText;
    private ImageView logoImageView;

    private String categoriaSeleccionada;
    private List<Video> videoListOriginal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoImageView = findViewById(R.id.logoImageView);
        searchEditText = findViewById(R.id.searchEditText);

        logoImageView.setOnClickListener(v -> {
            View popupView = LayoutInflater.from(this).inflate(R.layout.popup_logout, null);
            PopupWindow popupWindow = new PopupWindow(popupView,
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    true);

            popupWindow.setElevation(12);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setBackgroundDrawable(null); // fondo transparente

            popupWindow.showAsDropDown(logoImageView, 0, 10);

            TextView btnCerrarSesion = popupView.findViewById(R.id.btnCerrarSesion);
            btnCerrarSesion.setOnClickListener(view -> {
                Toast.makeText(MainActivity.this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                popupWindow.dismiss();
            });
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarVideos(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        categoriaSeleccionada = intent.hasExtra("categoria")
                ? intent.getStringExtra("categoria")
                : "";

        Log.d(TAG, "Categoría seleccionada: '" + categoriaSeleccionada + "'");
        obtenerUrlDelServidor();
    }

    private void obtenerUrlDelServidor() {
        Retrofit retrofitTemporal = new Retrofit.Builder()
                .baseUrl("http://192.168.1.12:3000/api/")
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
                    RetrofitClient.setBaseUrl(url);
                    fetchVideos();
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
        Call<List<Video>> call = (categoriaSeleccionada == null || categoriaSeleccionada.isEmpty())
                ? api.obtenerVideos()
                : api.getVideosByCategory(categoriaSeleccionada);

        call.enqueue(new Callback<List<Video>>() {
            @Override
            public void onResponse(Call<List<Video>> call, Response<List<Video>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    videoListOriginal = resp.body();
                    adapter = new VideoAdapter(MainActivity.this, new ArrayList<>(videoListOriginal));
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Permiso de notificaciones concedido");
            } else {
                Toast.makeText(this, "❌ Permiso de notificaciones denegado", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void filtrarVideos(String query) {
        if (videoListOriginal == null || adapter == null) return;
        List<Video> filtrados = new ArrayList<>();
        for (Video video : videoListOriginal) {
            if (video.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filtrados.add(video);
            }
        }
        adapter.actualizarLista(filtrados);
    }
}

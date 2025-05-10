// ReproducirActivity.java CORREGIDO para eliminar la tarea de recientes al cerrar
package com.example.reproductorvideos;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.ui.VideoAdapter;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@OptIn(markerClass = UnstableApi.class)
public class ReproducirActivity extends AppCompatActivity {

    private TextView videoTitle;
    private PlayerView playerView;
    private RecyclerView recyclerViewRecomendados;
    private VideoAdapter videoAdapter;

    private String videoUrlActual = "";
    private String videoTituloActual = "";

    private MediaPlaybackService mediaService;
    private boolean serviceBound = false;

    private List<Video> listaVideos;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlaybackService.LocalBinder binder = (MediaPlaybackService.LocalBinder) service;
            mediaService = binder.getService();
            serviceBound = true;

            playerView.setPlayer(mediaService.getPlayer());
            mediaService.setVideoChangeCallback((url, titulo) -> runOnUiThread(() -> {
                videoTitle.setText(titulo);
                videoUrlActual = url;
                videoTituloActual = titulo;
                obtenerVideosRecomendados();
            }));

            if (listaVideos != null && !listaVideos.isEmpty()) {
                mediaService.setListaVideos(listaVideos);
                mediaService.playNewVideo(videoUrlActual, videoTituloActual);
                videoTitle.setText(videoTituloActual);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private final BroadcastReceiver cerrarAppReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ReproducirActivity", "Recibido cierre total de app");
            // Limpia el PlayerView y adaptador
            if (playerView != null) playerView.setPlayer(null);
            if (recyclerViewRecomendados != null) recyclerViewRecomendados.setAdapter(null);
            if (serviceBound) {
                unbindService(connection);
                serviceBound = false;
            }
            // Elimina la tarea de recientes y cierra
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (ActivityManager.AppTask task : am.getAppTasks()) {
                    task.finishAndRemoveTask();
                }
            } else {
                finishAffinity();
            }
        }
    };

    public void reproducirNuevoVideo(String url, String titulo) {
        if (mediaService != null && listaVideos != null && !listaVideos.isEmpty()) {
            mediaService.setListaVideos(listaVideos);
            mediaService.playNewVideo(url, titulo);
        } else {
            Log.w("ReproducirActivity", "⚠ No hay suficientes videos para reproducir.");
        }
        videoTitle.setText(titulo);
        videoUrlActual = url;
        videoTituloActual = titulo;
        obtenerVideosRecomendados();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reproducir);

        videoTitle = findViewById(R.id.videoTitle);
        playerView = findViewById(R.id.playerView);
        recyclerViewRecomendados = findViewById(R.id.recyclerViewRecomendados);

        videoUrlActual = getIntent().getStringExtra("video_url");
        videoTituloActual = getIntent().getStringExtra("video_titulo");

        recyclerViewRecomendados.setLayoutManager(new LinearLayoutManager(this));
        videoAdapter = new VideoAdapter(this, new ArrayList<>());
        recyclerViewRecomendados.setAdapter(videoAdapter);

        ImageButton fullscreenButton = findViewById(R.id.fullscreenButton);
        fullscreenButton.setOnClickListener(v -> {
            if (mediaService != null) {
                long currentPosition = mediaService.getPlayer().getCurrentPosition();
                Intent intent = new Intent(ReproducirActivity.this, FullscreenPlayerActivity.class);
                intent.putExtra("video_url", videoUrlActual);
                intent.putExtra("video_titulo", videoTituloActual);
                intent.putExtra("video_position", currentPosition);
                startActivity(intent);
            }
        });

        Intent intent = new Intent(this, MediaPlaybackService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        obtenerVideosRecomendados();
    }

    private void obtenerVideosRecomendados() {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.obtenerVideos().enqueue(new Callback<List<Video>>() {
            @Override
            public void onResponse(Call<List<Video>> call, Response<List<Video>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Video> todos = response.body();
                    listaVideos = new ArrayList<>();
                    for (Video video : todos) {
                        if (!video.getUrl().equals(videoUrlActual)) {
                            listaVideos.add(video);
                        }
                    }
                    Collections.shuffle(listaVideos);
                    if (listaVideos.size() > 5) {
                        listaVideos = listaVideos.subList(0, 5);
                    }
                    videoAdapter.updateData(listaVideos);
                    if (mediaService != null) {
                        mediaService.setListaVideos(listaVideos);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Video>> call, Throwable t) {
                Log.e("API_ERROR", "Error al cargar videos: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(
                    cerrarAppReceiver,
                    new IntentFilter("com.example.reproductorvideos.CERRAR_APP"),
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            ? Context.RECEIVER_NOT_EXPORTED : 0
            );
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(cerrarAppReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(connection);
            serviceBound = false;
        }
    }
}

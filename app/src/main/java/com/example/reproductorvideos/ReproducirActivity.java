package com.example.reproductorvideos;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.ui.VideoAdapter;

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

    private String videoUrlActual;
    private String videoTituloActual;

    private MediaPlaybackService mediaService;
    private boolean serviceBound;

    private List<Video> listaVideos;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediaService = ((MediaPlaybackService.LocalBinder) service).getService();
            serviceBound = true;
            playerView.setPlayer(mediaService.getPlayer());

            // Siempre inicia reproducción del vídeo actual
            mediaService.playNewVideo(videoUrlActual, videoTituloActual);
            videoTitle.setText(videoTituloActual);

            // Si ya cargaste recomendaciones, asígnalas al servicio
            if (listaVideos != null && !listaVideos.isEmpty()) {
                mediaService.setListaVideos(listaVideos);
            }

            // Callback para cuando cambie el vídeo
            mediaService.setVideoChangeCallback((url, titulo) -> runOnUiThread(() -> {
                videoTitle.setText(titulo);
                videoUrlActual    = url;
                videoTituloActual = titulo;
                cargarRecomendaciones();
            }));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Aquí sí debes implementar este método, aunque solo marques serviceBound=false
            serviceBound = false;
        }
    };


    private final BroadcastReceiver cerrarAppReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            Log.d("ReproducirActivity", "🔴 cierre solicitado");
            // limpia UI
            playerView.setPlayer(null);
            recyclerViewRecomendados.setAdapter(null);
            if (serviceBound) {
                unbindService(connection);
                serviceBound = false;
            }
            // cierra toda la app
            finishAffinity();
        }
    };

    /** Llamado por el adapter para cambiar vídeo sin recrear la Activity */
    public void reproducirNuevoVideo(String url, String titulo) {
        if (mediaService != null && listaVideos != null && !listaVideos.isEmpty()) {
            mediaService.setListaVideos(listaVideos);
            mediaService.playNewVideo(url, titulo);
        } else {
            Log.w("ReproducirActivity","⚠ lista vacía");
        }
        videoTitle.setText(titulo);
        videoUrlActual    = url;
        videoTituloActual = titulo;
        cargarRecomendaciones();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reproducir);

        videoTitle             = findViewById(R.id.videoTitle);
        playerView             = findViewById(R.id.playerView);
        recyclerViewRecomendados = findViewById(R.id.recyclerViewRecomendados);

        videoUrlActual    = getIntent().getStringExtra("video_url");
        videoTituloActual = getIntent().getStringExtra("video_titulo");

        recyclerViewRecomendados.setLayoutManager(new LinearLayoutManager(this));
        videoAdapter = new VideoAdapter(this, new ArrayList<>());
        recyclerViewRecomendados.setAdapter(videoAdapter);

        ImageButton btnFull = findViewById(R.id.fullscreenButton);
        btnFull.setOnClickListener(v -> {
            if (mediaService != null) {
                long pos = mediaService.getPlayer().getCurrentPosition();
                Intent i = new Intent(this, FullscreenPlayerActivity.class);
                i.putExtra("video_url", videoUrlActual);
                i.putExtra("video_titulo", videoTituloActual);
                i.putExtra("video_position", pos);
                startActivity(i);
            }
        });

        // arranca y vincula el servicio
        Intent svc = new Intent(this, MediaPlaybackService.class);
        startService(svc);
        bindService(svc, connection, Context.BIND_AUTO_CREATE);

        cargarRecomendaciones();
    }

    private void cargarRecomendaciones() {
        ApiService api = RetrofitClient.getApiService(this);
        api.obtenerVideos().enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> call, Response<List<Video>> resp) {
                if (!resp.isSuccessful() || resp.body()==null) return;
                List<Video> todos = resp.body();
                listaVideos = new ArrayList<>();
                for (Video v: todos) if (!v.getUrl().equals(videoUrlActual)) listaVideos.add(v);
                Collections.shuffle(listaVideos);
                if (listaVideos.size()>5) listaVideos = listaVideos.subList(0,5);
                videoAdapter.updateData(listaVideos);
                if (mediaService!=null) mediaService.setListaVideos(listaVideos);
            }
            @Override public void onFailure(Call<List<Video>> call, Throwable t) {
                Log.e("ReproducirActivity","❌ recomendaciones:",t);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(cerrarAppReceiver,
                        new IntentFilter("com.example.reproductorvideos.CERRAR_APP"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(cerrarAppReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(connection);
            serviceBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaService != null) {
            playerView.setPlayer(null);
            playerView.postDelayed(() -> playerView.setPlayer(mediaService.getPlayer()), 100);
        }
    }


}
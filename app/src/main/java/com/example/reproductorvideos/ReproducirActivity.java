package com.example.reproductorvideos;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.ui.VideoAdapter;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ConcatenatingMediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerNotificationManager;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class ReproducirActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private TextView videoTitle;
    private RecyclerView recyclerViewRecomendados;
    private VideoAdapter videoAdapter;
    private String videoUrlActual;
    private String videoTituloActual;

    private static final String CHANNEL_ID = "video_channel";
    private static final int NOTIFICATION_ID = 1;
    private PlayerNotificationManager notificationManager;

    private List<Video> listaVideos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reproducir);

        playerView = findViewById(R.id.playerView);
        videoTitle = findViewById(R.id.videoTitle);
        recyclerViewRecomendados = findViewById(R.id.recyclerViewRecomendados);

        videoUrlActual = getIntent().getStringExtra("video_url");
        videoTituloActual = getIntent().getStringExtra("video_titulo");

        recyclerViewRecomendados.setLayoutManager(new LinearLayoutManager(this));
        videoAdapter = new VideoAdapter(this, new ArrayList<>());
        recyclerViewRecomendados.setAdapter(videoAdapter);

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
                        if (video.getUrl().equals(videoUrlActual)) {
                            listaVideos.add(0, video);
                        } else {
                            listaVideos.add(video);
                        }
                    }

                    videoAdapter.updateData(listaVideos);
                    inicializarExoPlayer(listaVideos);
                }
            }

            @Override
            public void onFailure(Call<List<Video>> call, Throwable t) {
                Log.e("API_ERROR", "Error al cargar videos recomendados: " + t.getMessage());
            }
        });
    }

    private void inicializarExoPlayer(List<Video> videos) {
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
        ConcatenatingMediaSource playlist = new ConcatenatingMediaSource();

        for (Video video : videos) {
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(video.getUrl()));
            ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
            playlist.addMediaSource(source);
        }

        exoPlayer.setMediaSource(playlist);
        exoPlayer.prepare();
        exoPlayer.play();

        iniciarNotificacion();
        escucharCambioDeVideo();
    }

    private void iniciarNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reproductor de Video",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        notificationManager = new PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
                .setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public CharSequence getCurrentContentTitle(Player player) {
                        return videoTitle.getText().toString();
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(ReproducirActivity.this, ReproducirActivity.class);
                        return PendingIntent.getActivity(ReproducirActivity.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                    }

                    @Nullable
                    @Override
                    public CharSequence getCurrentContentText(Player player) {
                        return "Reproduciendo en segundo plano";
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        return null;
                    }
                })
                .build();

        notificationManager.setUseNextAction(true);
        notificationManager.setUsePreviousAction(true);
        notificationManager.setPlayer(exoPlayer);
    }

    private void escucharCambioDeVideo() {
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                if (mediaItem != null && listaVideos != null) {
                    for (Video v : listaVideos) {
                        if (v.getUrl().equals(mediaItem.localConfiguration.uri.toString())) {
                            videoTitle.setText(v.getTitle());
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }


}

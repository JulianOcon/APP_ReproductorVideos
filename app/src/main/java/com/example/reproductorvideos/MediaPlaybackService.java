package com.example.reproductorvideos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerNotificationManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.reproductorvideos.model.Video;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@UnstableApi
public class MediaPlaybackService extends Service {

    public static final String ACTION_VIDEO_CHANGED = "com.example.reproductorvideos.VIDEO_CHANGED";
    public static final String EXTRA_VIDEO_TITLE = "video_title";

    public interface VideoChangeCallback {
        void onVideoChanged(String url, String titulo);
    }

    private static final String CHANNEL_ID = "video_channel";
    private static final int NOTIFICATION_ID = 1;

    private ExoPlayer exoPlayer;
    private PlayerNotificationManager notificationManager;

    private final IBinder binder = new LocalBinder();
    private List<Video> listaVideos = new ArrayList<>();
    private String urlUltimoVideo = "";
    private VideoChangeCallback callback;

    public class LocalBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Reproducción de video", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    Log.d("ExoPlayer", "🔚 Video terminado. Reproduciendo otro aleatorio...");
                    reproducirSiguienteAleatorio();
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                if (mediaItem != null && mediaItem.mediaMetadata != null) {
                    String nuevoTitulo = String.valueOf(mediaItem.mediaMetadata.title);
                    Intent intent = new Intent(ACTION_VIDEO_CHANGED);
                    intent.putExtra(EXTRA_VIDEO_TITLE, nuevoTitulo);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    Log.d("ExoPlayer", "🎬 Nuevo video: " + nuevoTitulo);
                }
            }
        });

        // Configurar notificación
        notificationManager = new PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
                .setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public CharSequence getCurrentContentTitle(Player player) {
                        return player.getMediaMetadata().title != null
                                ? player.getMediaMetadata().title.toString()
                                : "Reproduciendo";
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(MediaPlaybackService.this, ReproducirActivity.class);
                        return PendingIntent.getActivity(MediaPlaybackService.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                    }

                    @Nullable
                    @Override
                    public CharSequence getCurrentContentText(Player player) {
                        return "Video";
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        if (player.getCurrentMediaItem() != null &&
                                player.getCurrentMediaItem().localConfiguration != null) {
                            String videoUrl = player.getCurrentMediaItem().localConfiguration.uri.toString();
                            return BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground);

                        }
                        return null;
                    }
                })
                .build();

        // Habilitar solo el botón de reproducir/pausar (no hay lista para "Siguiente")
        notificationManager.setUseNextAction(true);
        notificationManager.setPlayer(exoPlayer);

        startForeground(NOTIFICATION_ID, buildDummyNotification());
    }

    private Notification buildDummyNotification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder.setContentTitle("Reproducción en curso")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    public void setVideoChangeCallback(VideoChangeCallback callback) {
        this.callback = callback;
    }

    public void setListaVideos(List<Video> videos) {
        this.listaVideos = videos != null ? videos : new ArrayList<>();
    }

    public void playNewVideo(String videoUrl, String title) {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.clearMediaItems();

            List<MediaItem> mediaItems = new ArrayList<>();

            // 1. Agrega el video actual
            MediaItem currentItem = new MediaItem.Builder()
                    .setUri(videoUrl)
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setTitle(title)
                            .build())
                    .build();
            mediaItems.add(currentItem);

            // 2. Agrega los recomendados (excepto el actual)
            for (Video video : listaVideos) {
                if (!video.getUrl().equals(videoUrl)) {
                    MediaItem item = new MediaItem.Builder()
                            .setUri(video.getUrl())
                            .setMediaMetadata(new MediaMetadata.Builder()
                                    .setTitle(video.getTitle())
                                    .build())
                            .build();
                    mediaItems.add(item);
                }
            }

            // 3. Reproducir como lista real
            exoPlayer.setMediaItems(mediaItems, /* startIndex */ 0, /* startPositionMs */ 0);
            exoPlayer.prepare();
            exoPlayer.play();
        }
    }


    public void reproducirSiguienteAleatorio() {
        if (listaVideos != null && !listaVideos.isEmpty()) {
            Video videoAleatorio;
            do {
                int randomIndex = new Random().nextInt(listaVideos.size());
                videoAleatorio = listaVideos.get(randomIndex);
            } while (videoAleatorio.getUrl().equals(urlUltimoVideo) && listaVideos.size() > 1);

            urlUltimoVideo = videoAleatorio.getUrl();
            playNewVideo(videoAleatorio.getUrl(), videoAleatorio.getTitle());

            if (callback != null) {
                callback.onVideoChanged(videoAleatorio.getUrl(), videoAleatorio.getTitle());
            }
        } else {
            Log.w("MediaService", "⚠ Lista de videos vacía o no inicializada.");
        }
    }

    public ExoPlayer getPlayer() {
        return exoPlayer;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}

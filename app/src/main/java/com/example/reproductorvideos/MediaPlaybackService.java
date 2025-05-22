package com.example.reproductorvideos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerNotificationManager;

import com.example.reproductorvideos.model.Video;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@UnstableApi
public class MediaPlaybackService extends Service {

    public static final String ACTION_VIDEO_CHANGED = "com.example.reproductorvideos.VIDEO_CHANGED";
    public static final String EXTRA_VIDEO_TITLE = "video_title";
    private static final String CHANNEL_ID = "video_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";

    private ExoPlayer exoPlayer;
    private PlayerNotificationManager notificationManager;
    private final IBinder binder = new LocalBinder();
    private List<Video> listaVideos = new ArrayList<>();
    private String urlUltimoVideo = "";
    private VideoChangeCallback callback;
    private Bitmap currentCoverBitmap;

    // --- NUEVO: Receiver para el cierre global
    private final BroadcastReceiver closeAllReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            detenerServicio();
        }
    };

    public Bitmap getCurrentCoverBitmap() {
        return currentCoverBitmap;
    }

    public interface VideoChangeCallback {
        void onVideoChanged(String url, String titulo);
    }

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
                    CHANNEL_ID,
                    "Reproducción de video",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        // REGISTRAR el receiver global (corrección)
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(closeAllReceiver, new IntentFilter("com.example.reproductorvideos.CERRAR_APP"));

        // Inicializa ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    reproducirSiguienteAleatorio();
                }
            }
        });

        // Configura la notificación con solo prev/play-pause/next + custom "Cerrar"
        notificationManager = new PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
                .setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public CharSequence getCurrentContentTitle(Player player) {
                        return player.getMediaMetadata().title != null
                                ? player.getMediaMetadata().title.toString()
                                : "Reproduciendo";
                    }

                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(MediaPlaybackService.this, ReproducirActivity.class);
                        return PendingIntent.getActivity(
                                MediaPlaybackService.this,
                                0,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE
                        );
                    }

                    @Override
                    public CharSequence getCurrentContentText(Player player) {
                        return player.getMediaMetadata().artist != null
                                ? player.getMediaMetadata().artist.toString()
                                : "Video";
                    }

                    @Override
                    public Bitmap getCurrentLargeIcon(Player player,
                                                      PlayerNotificationManager.BitmapCallback callback) {
                        return BitmapFactory.decodeResource(
                                getResources(),
                                R.drawable.ic_launcher_foreground
                        );
                    }
                })
                .setCustomActionReceiver(new PlayerNotificationManager.CustomActionReceiver() {
                    @NonNull
                    @Override
                    public Map<String, NotificationCompat.Action> createCustomActions(Context context, int instanceId) {
                        Intent stopIntent = new Intent(context, MediaPlaybackService.class);
                        stopIntent.setAction(ACTION_STOP_SERVICE);
                        PendingIntent pi = PendingIntent.getService(
                                context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
                        );
                        NotificationCompat.Action closeAction = new NotificationCompat.Action.Builder(
                                R.drawable.ic_close, "Cerrar", pi
                        ).build();
                        Map<String, NotificationCompat.Action> map = new HashMap<>();
                        map.put(ACTION_STOP_SERVICE, closeAction);
                        return map;
                    }

                    @NonNull
                    @Override
                    public List<String> getCustomActions(@NonNull Player player) {
                        return Collections.singletonList(ACTION_STOP_SERVICE);
                    }

                    @Override
                    public void onCustomAction(@NonNull Player player,
                                               @NonNull String action,
                                               @NonNull Intent intent) {
                        if (ACTION_STOP_SERVICE.equals(action)) {
                            detenerServicio();
                        }
                    }
                })
                .build();

        notificationManager.setUsePreviousAction(true);
        notificationManager.setUsePlayPauseActions(true);
        notificationManager.setUseNextAction(true);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseFastForwardAction(false);
        notificationManager.setUseStopAction(false);
        notificationManager.setPlayer(exoPlayer);

        // Inicia en primer plano
        startForeground(NOTIFICATION_ID, buildDummyNotification());
    }

    @Override
    public void onDestroy() {
        // DESREGISTRAR el receiver global (corrección)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeAllReceiver);
        super.onDestroy();
    }

    private void detenerServicio() {
        // Libera player y notificación
        if (notificationManager != null) {
            notificationManager.setPlayer(null);
        }
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
        stopForeground(true);
        stopSelf();

        // Envía broadcast para cerrar UI
        Intent closeIntent = new Intent("com.example.reproductorvideos.CERRAR_APP");
        closeIntent.setPackage(getPackageName());
        LocalBroadcastManager.getInstance(this).sendBroadcast(closeIntent);
    }

    private Notification buildDummyNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Reproducción en curso")
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
            List<MediaItem> items = new ArrayList<>();
            items.add(new MediaItem.Builder()
                    .setUri(videoUrl)
                    .setMediaMetadata(new MediaMetadata.Builder().setTitle(title).build())
                    .build());
            for (Video v : listaVideos) {
                if (!v.getUrl().equals(videoUrl)) {
                    items.add(new MediaItem.Builder()
                            .setUri(v.getUrl())
                            .setMediaMetadata(new MediaMetadata.Builder().setTitle(v.getTitle()).build())
                            .build());
                }
            }
            exoPlayer.setMediaItems(items, 0, 0);
            exoPlayer.prepare();
            exoPlayer.play();
        }
    }

    public void reproducirSiguienteAleatorio() {
        if (listaVideos != null && !listaVideos.isEmpty()) {
            Video v;
            do {
                int idx = new Random().nextInt(listaVideos.size());
                v = listaVideos.get(idx);
            } while (v.getUrl().equals(urlUltimoVideo) && listaVideos.size() > 1);
            urlUltimoVideo = v.getUrl();
            playNewVideo(v.getUrl(), v.getTitle());
            if (callback != null) callback.onVideoChanged(v.getUrl(), v.getTitle());
        }
    }

    public ExoPlayer getPlayer() { return exoPlayer; }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            detenerServicio();
        }
        return START_NOT_STICKY;
    }
}

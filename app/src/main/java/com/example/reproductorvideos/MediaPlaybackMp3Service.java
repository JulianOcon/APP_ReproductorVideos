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
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

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
import androidx.palette.graphics.Palette;

import com.example.reproductorvideos.model.Mp3File;

import java.util.ArrayList;
import java.util.List;

@UnstableApi
public class MediaPlaybackMp3Service extends Service {
    public static final String ACTION_TRACK_CHANGED = "com.example.reproductorvideos.TRACK_CHANGED";
    public static final String EXTRA_TRACK_INDEX = "track_index";
    private static final String CHANNEL_ID = "mp3_channel";
    private static final int NOTIFICATION_ID = 2;

    private final IBinder binder = new LocalBinder();
    private ExoPlayer exoPlayer;
    private PlayerNotificationManager notificationManager;
    private List<Mp3File> playlist = new ArrayList<>();
    private Bitmap currentCoverBitmap;

    public class LocalBinder extends Binder {
        public MediaPlaybackMp3Service getService() {
            return MediaPlaybackMp3Service.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID, "Reproducción de MP3", NotificationManager.IMPORTANCE_LOW
            );
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(chan);
        }

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem item, int reason) {
                MediaMetadata md = exoPlayer.getMediaMetadata();
                byte[] artData = md.artworkData;
                if (artData != null) {
                    currentCoverBitmap = BitmapFactory.decodeByteArray(artData, 0, artData.length);
                } else {
                    currentCoverBitmap = null;
                }
                int notifColor = 0xFF222222;
                if (currentCoverBitmap != null) {
                    Palette palette = Palette.from(currentCoverBitmap).generate();
                    Palette.Swatch swatch = palette.getDominantSwatch();
                    if (swatch != null) notifColor = swatch.getRgb();
                }
                notificationManager.setColor(notifColor);

                int idx = exoPlayer.getCurrentMediaItemIndex();
                Intent b = new Intent(ACTION_TRACK_CHANGED)
                        .putExtra(EXTRA_TRACK_INDEX, idx);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(b);
            }
        });

        // --- NOTIFICACIÓN: solo acciones estándar (no custom X)
        notificationManager = new PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
                .setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public CharSequence getCurrentContentTitle(Player p) {
                        CharSequence t = p.getMediaMetadata().title;
                        return t != null ? t : "Reproduciendo MP3";
                    }
                    @Override
                    public PendingIntent createCurrentContentIntent(Player p) {
                        Intent i = new Intent(MediaPlaybackMp3Service.this, Mp3Activity.class);
                        return PendingIntent.getActivity(
                                MediaPlaybackMp3Service.this, 0, i, PendingIntent.FLAG_IMMUTABLE
                        );
                    }
                    @Override
                    public CharSequence getCurrentContentText(Player p) {
                        CharSequence a = p.getMediaMetadata().artist;
                        return a != null ? a : "";
                    }
                    @Override
                    public Bitmap getCurrentLargeIcon(Player p, PlayerNotificationManager.BitmapCallback cb) {
                        return currentCoverBitmap != null
                                ? currentCoverBitmap
                                : BitmapFactory.decodeResource(getResources(), R.drawable.default_cover);
                    }
                })
                .setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                    @Override
                    public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                        cerrarApp();
                    }
                    @Override
                    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
                        // Nada especial
                    }

                })
                .build();

        notificationManager.setUsePreviousAction(true);
        notificationManager.setUsePlayPauseActions(true);
        notificationManager.setUseNextAction(true);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseFastForwardAction(false);
        notificationManager.setUseStopAction(true); // Habilita botón stop
        notificationManager.setPlayer(exoPlayer);

        startForeground(NOTIFICATION_ID, buildDummyNotification());
    }

    private void cerrarApp() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
        if (notificationManager != null) {
            notificationManager.setPlayer(null);
        }

        // 1. Manda broadcast LOCAL para Activities
        Intent closeIntent = new Intent("com.example.reproductorvideos.CERRAR_APP");
        LocalBroadcastManager.getInstance(this).sendBroadcast(closeIntent);

        // 2. Detén foreground y el service
        stopForeground(true);
        stopSelf();
        // ¡NO mates el proceso!
    }


    @Override
    public void onDestroy() {
        cerrarApp();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Ya no necesitas una acción custom, sólo usa stop
        return START_NOT_STICKY;
    }

    private Notification buildDummyNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Reproduciendo MP3")
                .setSmallIcon(R.drawable.ic_music_note)
                .build();
    }

    public void setPlaylist(List<Mp3File> list, int playIndex) {
        if (list == null || list.isEmpty()) return;
        playlist = list;
        List<MediaItem> items = new ArrayList<>();
        for (Mp3File m : playlist) {
            MediaMetadata.Builder mdB = new MediaMetadata.Builder()
                    .setTitle(m.getTitulo())
                    .setArtist(m.getArtista());
            byte[] art = m.getArtworkData();
            if (art != null) {
                mdB.setArtworkData(art, MediaMetadata.PICTURE_TYPE_FRONT_COVER);
            }
            items.add(new MediaItem.Builder()
                    .setUri(m.getUrl())
                    .setMediaMetadata(mdB.build())
                    .build());
        }
        exoPlayer.setMediaItems(items, playIndex, 0);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    public void playTrackAt(int index) {
        if (exoPlayer != null && playlist != null && !playlist.isEmpty()) {
            exoPlayer.seekTo(index, 0);
            exoPlayer.play();
        }
    }

    public ExoPlayer getPlayer() { return exoPlayer; }
    public List<Mp3File> getPlaylist() { return playlist; }
    public Bitmap getCurrentCoverBitmap() { return currentCoverBitmap; }
}

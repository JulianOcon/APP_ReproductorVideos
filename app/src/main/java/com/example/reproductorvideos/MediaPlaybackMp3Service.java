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
import android.media.MediaMetadataRetriever;
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
import androidx.palette.graphics.Palette;

import com.example.reproductorvideos.model.Mp3File;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UnstableApi
public class MediaPlaybackMp3Service extends Service {
    public static final String ACTION_TRACK_CHANGED   = "com.example.reproductorvideos.TRACK_CHANGED";
    public static final String EXTRA_TRACK_INDEX      = "track_index";
    private static final String CHANNEL_ID            = "mp3_channel";
    private static final int    NOTIFICATION_ID       = 2;
    private static final String ACTION_STOP_SERVICE   = "ACTION_STOP_SERVICE";
    private static final String ACTION_PREVIOUS       = "ACTION_PREVIOUS";
    private static final String ACTION_NEXT           = "ACTION_NEXT";

    private final IBinder binder = new LocalBinder();
    private ExoPlayer exoPlayer;
    private PlayerNotificationManager notificationManager;
    private List<Mp3File> playlist = new ArrayList<>();
    private Bitmap currentCoverBitmap;

    /** Historial de índices para “Anterior” */
    private final Deque<Integer> playbackHistory = new ArrayDeque<>();

    private final BroadcastReceiver closeAllReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            cerrarApp();
        }
    };

    public class LocalBinder extends Binder {
        public MediaPlaybackMp3Service getService() {
            return MediaPlaybackMp3Service.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 1) Canal de notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID, "Reproducción de MP3", NotificationManager.IMPORTANCE_LOW
            );
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(chan);
        }

        // 2) Receiver para cerrar app
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(closeAllReceiver,
                        new IntentFilter("com.example.reproductorvideos.CERRAR_APP"));

        // 3) Inicializa ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);

        // 4) Listener para detectar cambio de pista
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem item, int reason) {
                // Extrae carátula
                MediaMetadata md = exoPlayer.getMediaMetadata();
                byte[] artData = md.artworkData;
                currentCoverBitmap = artData != null
                        ? BitmapFactory.decodeByteArray(artData,0,artData.length)
                        : null;
                // Ajusta color de notificación
                int notifColor = 0xFF222222;
                if (currentCoverBitmap != null) {
                    Palette.Swatch sw = Palette.from(currentCoverBitmap)
                            .generate()
                            .getDominantSwatch();
                    if (sw != null) notifColor = sw.getRgb();
                }
                notificationManager.setColor(notifColor);

                // Broadcast para Activities
                int idx = exoPlayer.getCurrentMediaItemIndex();
                Intent b = new Intent(ACTION_TRACK_CHANGED)
                        .putExtra(EXTRA_TRACK_INDEX, idx);
                LocalBroadcastManager.getInstance(getApplicationContext())
                        .sendBroadcast(b);
            }
        });

        // 5) Construye PlayerNotificationManager con acciones personalizadas
        notificationManager = new PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
                // Adapter de contenido
                .setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override public CharSequence getCurrentContentTitle(Player p) {
                        CharSequence t = p.getMediaMetadata().title;
                        return t != null ? t : "Reproduciendo MP3";
                    }
                    @Override public PendingIntent createCurrentContentIntent(Player p) {
                        Intent i = new Intent(MediaPlaybackMp3Service.this, Mp3Activity.class);
                        return PendingIntent.getActivity(
                                MediaPlaybackMp3Service.this, 0, i, PendingIntent.FLAG_IMMUTABLE
                        );
                    }
                    @Override public CharSequence getCurrentContentText(Player p) {
                        CharSequence a = p.getMediaMetadata().artist;
                        return a != null ? a : "";
                    }
                    @Override public Bitmap getCurrentLargeIcon(
                            Player p,
                            PlayerNotificationManager.BitmapCallback cb
                    ) {
                        // Aquí pones la carátula (o la por defecto)
                        return currentCoverBitmap != null
                                ? currentCoverBitmap
                                : BitmapFactory.decodeResource(getResources(), R.drawable.default_cover);
                    }
                })
                // Custom Actions: Prev / Next / Close
                .setCustomActionReceiver(new PlayerNotificationManager.CustomActionReceiver() {
                    @NonNull @Override
                    public Map<String, NotificationCompat.Action> createCustomActions(
                            Context ctx, int instanceId
                    ) {
                        Map<String, NotificationCompat.Action> map = new HashMap<>();

                        // Anterior
                        Intent prevIntent = new Intent(ctx, MediaPlaybackMp3Service.class)
                                .setAction(ACTION_PREVIOUS)
                                .putExtra(PlayerNotificationManager.EXTRA_INSTANCE_ID, instanceId);
                        PendingIntent prevPI = PendingIntent.getService(
                                ctx, instanceId, prevIntent, PendingIntent.FLAG_IMMUTABLE
                        );
                        map.put(ACTION_PREVIOUS, new NotificationCompat.Action.Builder(
                                R.drawable.ic_skip_previous, "Anterior", prevPI
                        ).build());

                        // Siguiente
                        Intent nextIntent = new Intent(ctx, MediaPlaybackMp3Service.class)
                                .setAction(ACTION_NEXT)
                                .putExtra(PlayerNotificationManager.EXTRA_INSTANCE_ID, instanceId);
                        PendingIntent nextPI = PendingIntent.getService(
                                ctx, instanceId+1, nextIntent, PendingIntent.FLAG_IMMUTABLE
                        );
                        map.put(ACTION_NEXT, new NotificationCompat.Action.Builder(
                                R.drawable.ic_skip_next, "Siguiente", nextPI
                        ).build());

                        // Cerrar
                        Intent stopIntent = new Intent(ctx, MediaPlaybackMp3Service.class)
                                .setAction(ACTION_STOP_SERVICE);
                        PendingIntent stopPI = PendingIntent.getService(
                                ctx, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
                        );
                        map.put(ACTION_STOP_SERVICE, new NotificationCompat.Action.Builder(
                                R.drawable.ic_close, "Cerrar", stopPI
                        ).build());

                        return map;
                    }

                    @NonNull @Override
                    public List<String> getCustomActions(@NonNull Player player) {
                        return Arrays.asList(ACTION_PREVIOUS, ACTION_NEXT, ACTION_STOP_SERVICE);
                    }

                    @Override
                    public void onCustomAction(
                            @NonNull Player player,
                            @NonNull String action,
                            @NonNull Intent intent
                    ) {
                        if (ACTION_PREVIOUS.equals(action)) {
                            playPrevious();
                        } else if (ACTION_NEXT.equals(action)) {
                            playNext();
                        } else if (ACTION_STOP_SERVICE.equals(action)) {
                            cerrarApp();
                        }
                    }
                })
                .build();

        // 6) Desactiva los botones nativos prev/next
        notificationManager.setUsePreviousAction(false);
        notificationManager.setUsePlayPauseActions(true);
        notificationManager.setUseNextAction(false);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseFastForwardAction(false);
        notificationManager.setUseStopAction(false);

        // 7) Enlaza player y arranca en foreground
        notificationManager.setPlayer(exoPlayer);
        startForeground(NOTIFICATION_ID, buildDummyNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // También manejamos Prev/Next/Stop desde aquí
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_PREVIOUS.equals(action)) {
                playPrevious();
            } else if (ACTION_NEXT.equals(action)) {
                playNext();
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                cerrarApp();
            }
        }
        return START_NOT_STICKY;
    }

    private Notification buildDummyNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Reproduciendo MP3")
                .setSmallIcon(R.drawable.ic_music_note)
                .build();
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeAllReceiver);
        super.onDestroy();
    }

    /** Detiene/release y avisa a Activities */
    private void cerrarApp() {
        if (notificationManager != null) notificationManager.setPlayer(null);
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
        stopForeground(true);
        stopSelf();
        Intent closeIntent = new Intent("com.example.reproductorvideos.CERRAR_APP");
        closeIntent.setPackage(getPackageName());
        LocalBroadcastManager.getInstance(this).sendBroadcast(closeIntent);
    }

    /**
     * Construye la playlist inyectando Artwork si hace falta.
     */
    public void setPlaylist(List<Mp3File> list, int playIndex) {
        if (list == null || list.isEmpty()) return;
        playlist = list;
        playbackHistory.clear(); // limpiamos historial al cambiar lista

        List<MediaItem> items = new ArrayList<>();
        for (Mp3File m : playlist) {
            MediaMetadata.Builder mdB = new MediaMetadata.Builder()
                    .setTitle(m.getTitulo())
                    .setArtist(m.getArtista());
            byte[] artData = m.getArtworkData();
            if (artData == null) {
                artData = extractEmbeddedArt(m.getUrl());
            }
            if (artData != null) {
                mdB.setArtworkData(artData, MediaMetadata.PICTURE_TYPE_FRONT_COVER);
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

    /**
     * Siguiente: guarda el índice actual, luego mezcla si toca.
     */
    public void playNext() {
        if (exoPlayer == null || playlist.isEmpty()) return;
        int current = exoPlayer.getCurrentMediaItemIndex();
        // guardamos para “anterior”
        playbackHistory.push(current);

        int size = playlist.size();
        if (exoPlayer.getShuffleModeEnabled() && size > 1) {
            int rand;
            do { rand = (int)(Math.random()*size);
            } while (rand == current);
            exoPlayer.seekTo(rand, 0);
        } else {
            exoPlayer.seekToNextMediaItem();
        }
        exoPlayer.play();
    }

    /**
     * Anterior: si hay historial, vuelve a esa pista; si no, secuencial.
     */
    public void playPrevious() {
        if (exoPlayer == null || playlist.isEmpty()) return;
        if (!playbackHistory.isEmpty()) {
            int prevIndex = playbackHistory.pop();
            exoPlayer.seekTo(prevIndex, 0);
        } else {
            // fallback secuencial
            int size = playlist.size();
            int current = exoPlayer.getCurrentMediaItemIndex();
            int prev = (current-1+size)%size;
            exoPlayer.seekTo(prev, 0);
        }
        exoPlayer.play();
    }

    /** Extrae embedded artwork si no venía en Mp3File */
    private byte[] extractEmbeddedArt(String uri) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(uri, new HashMap<>());
            return r.getEmbeddedPicture();
        } catch (Exception e) {
            return null;
        } finally {
            try { r.release(); } catch (Exception ignored) {}
        }
    }

    public ExoPlayer getPlayer() { return exoPlayer; }
    public List<Mp3File> getPlaylist() { return playlist; }
    public Bitmap getCurrentCoverBitmap() { return currentCoverBitmap; }
}

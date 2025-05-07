package com.example.reproductorvideos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerNotificationManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

@UnstableApi
public class MediaPlaybackService extends Service {

    private static final String CHANNEL_ID = "video_channel";
    private static final int NOTIFICATION_ID = 1;

    private ExoPlayer exoPlayer;
    private PlayerNotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Crear canal con IMPORTANCE_DEFAULT para controles visibles
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reproducción de video",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        exoPlayer = new ExoPlayer.Builder(this).build();

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
                        return player.getMediaMetadata().artist != null
                                ? player.getMediaMetadata().artist.toString()
                                : "Video";
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        if (player.getCurrentMediaItem() != null &&
                                player.getCurrentMediaItem().localConfiguration != null) {

                            String videoUrl = player.getCurrentMediaItem().localConfiguration.uri.toString();

                            Glide.with(getApplicationContext())
                                    .asBitmap()
                                    .load(videoUrl)
                                    .into(new CustomTarget<Bitmap>() {
                                        @Override
                                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                            callback.onBitmap(resource);
                                        }

                                        @Override
                                        public void onLoadCleared(@Nullable Drawable placeholder) {
                                            // No se requiere limpieza especial
                                        }
                                    });
                        }
                        return null;
                    }
                })
                .build();

        notificationManager.setUsePlayPauseActions(true);
        notificationManager.setUseNextAction(false);
        notificationManager.setUsePreviousAction(false);
        notificationManager.setPlayer(exoPlayer);

        // Notificación dummy para startForeground compatible con API 24+
        Notification dummyNotification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dummyNotification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Reproducción en curso")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();
        } else {
            dummyNotification = new Notification.Builder(this)
                    .setContentTitle("Reproducción en curso")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();
        }

        startForeground(NOTIFICATION_ID, dummyNotification);
    }

    public ExoPlayer getPlayer() {
        return exoPlayer;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

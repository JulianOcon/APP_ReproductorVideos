package com.example.reproductorvideos;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SimpleExoPlayer;
import androidx.media3.ui.PlayerNotificationManager;
import androidx.media3.ui.PlayerView;

@UnstableApi
public class PlayerService extends Service {

    private static ExoPlayer exoPlayer;
    private static PlayerNotificationManager notificationManager;


    public static void setup(ExoPlayer player, PlayerNotificationManager manager) {
        exoPlayer = player;
        notificationManager = manager;
        // Aquí ya se encarga de mostrar la notificación
        notificationManager.setPlayer(exoPlayer);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

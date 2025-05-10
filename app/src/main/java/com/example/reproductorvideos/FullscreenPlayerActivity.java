package com.example.reproductorvideos;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;

@OptIn(markerClass = UnstableApi.class)
public class FullscreenPlayerActivity extends AppCompatActivity {

    private PlayerView fullscreenPlayerView;
    private MediaPlaybackService mediaService;
    private boolean serviceBound = false;

    private final android.content.ServiceConnection connection = new android.content.ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlaybackService.LocalBinder binder = (MediaPlaybackService.LocalBinder) service;
            mediaService = binder.getService();
            fullscreenPlayerView.setPlayer(mediaService.getPlayer());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pantalla completa y orientación horizontal
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        setContentView(R.layout.activity_fullscreen_player);

        fullscreenPlayerView = findViewById(R.id.fullscreenPlayerView);

        ImageButton exitButton = findViewById(R.id.exitFullscreenButton);
        exitButton.setOnClickListener(v -> finish());

        Intent intent = new Intent(this, MediaPlaybackService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        serviceBound = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fullscreenPlayerView != null) {
            fullscreenPlayerView.setPlayer(null);
        }
        if (serviceBound) {
            unbindService(connection);
            serviceBound = false;
        }
    }

}

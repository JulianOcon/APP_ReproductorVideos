package com.example.reproductorvideos;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.reproductorvideos.model.Mp3File;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import jp.wasabeef.glide.transformations.BlurTransformation;

import java.io.IOException;
import java.util.List;

@UnstableApi
public class ExoPlayerActivity extends AppCompatActivity {

    private MediaPlaybackMp3Service mp3Service;
    private boolean serviceBound = false;

    private ImageView backBtn, prevBtn, nextBtn, coverImage, bgBlurImage, shuffleBtn, repeatBtn;
    private FloatingActionButton playPauseBtn;
    private SeekBar seekBar;
    private TextView tituloText, artistaText, durationPlayed, durationTotal;

    private final Handler handler = new Handler();
    private boolean isShuffleEnabled = false;
    private int repeatMode = Player.REPEAT_MODE_OFF;

    private final BroadcastReceiver trackChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            int idx = intent.getIntExtra(MediaPlaybackMp3Service.EXTRA_TRACK_INDEX, 0);
            actualizarUI(idx);

            ExoPlayer p = mp3Service.getPlayer();
            int tot = (int) (p.getDuration() / 1000);
            seekBar.setMax(tot);
            durationTotal.setText(formattedTime(tot));
            playPauseBtn.setImageResource(
                    p.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
            );
        }
    };

    private final BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finishAffinity(); // Cierra todas las activities de la app en la task stack
            // ¡NO LLAMES a killProcess aquí!
        }
    };




    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (!serviceBound) return;
            ExoPlayer p = mp3Service.getPlayer();
            int sec = (int) (p.getCurrentPosition() / 1000);
            seekBar.setProgress(sec);
            durationPlayed.setText(formattedTime(sec));
            handler.postDelayed(this, 1000);
        }
    };

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder b) {
            mp3Service = ((MediaPlaybackMp3Service.LocalBinder) b).getService();
            serviceBound = true;

            @SuppressWarnings("unchecked")
            List<Mp3File> list =
                    (List<Mp3File>) getIntent().getSerializableExtra("mp3List");
            int pos = getIntent().getIntExtra("position", 0);

            mp3Service.setPlaylist(list, pos);

            actualizarUI(pos);

            ExoPlayer player = mp3Service.getPlayer();

            isShuffleEnabled = player.getShuffleModeEnabled();
            shuffleBtn.setImageResource(
                    isShuffleEnabled ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off
            );
            repeatMode = player.getRepeatMode();
            repeatBtn.setImageResource(
                    repeatMode == Player.REPEAT_MODE_OFF ? R.drawable.ic_repeat_off :
                            repeatMode == Player.REPEAT_MODE_ONE ? R.drawable.ic_repeat_one_on :
                                    R.drawable.ic_repeat_on);

            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    playPauseBtn.setImageResource(
                            player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
                    );
                    if (state == Player.STATE_READY) {
                        int tot = (int) (player.getDuration() / 1000);
                        seekBar.setMax(tot);
                        durationTotal.setText(formattedTime(tot));
                    }
                }
            });

            playPauseBtn.setOnClickListener(v -> {
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
            });

            prevBtn.setOnClickListener(v ->
                    mp3Service.playTrackAt(
                            (player.getCurrentMediaItemIndex() - 1 + list.size()) % list.size()
                    )
            );
            nextBtn.setOnClickListener(v -> {
                int listSize = list.size();
                int currentIdx = player.getCurrentMediaItemIndex();

                if (player.getShuffleModeEnabled() && listSize > 1) {
                    int randomIdx;
                    do {
                        randomIdx = (int) (Math.random() * listSize);
                    } while (randomIdx == currentIdx);

                    mp3Service.playTrackAt(randomIdx);
                } else {
                    mp3Service.playTrackAt((currentIdx + 1) % listSize);
                }
            });

            shuffleBtn.setOnClickListener(v -> {
                boolean wasShuffleEnabled = player.getShuffleModeEnabled();
                int currentIndex = player.getCurrentMediaItemIndex();
                long currentPosition = player.getCurrentPosition();

                player.setShuffleModeEnabled(!wasShuffleEnabled);
                shuffleBtn.setImageResource(
                        !wasShuffleEnabled ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off
                );

                if (!wasShuffleEnabled) {
                    int shuffledIndex = player.getCurrentMediaItemIndex();
                    player.seekTo(shuffledIndex, currentPosition);
                }
            });

            repeatBtn.setOnClickListener(v -> {
                if (repeatMode == Player.REPEAT_MODE_OFF) {
                    repeatMode = Player.REPEAT_MODE_ALL;
                    repeatBtn.setImageResource(R.drawable.ic_repeat_on);
                } else if (repeatMode == Player.REPEAT_MODE_ALL) {
                    repeatMode = Player.REPEAT_MODE_ONE;
                    repeatBtn.setImageResource(R.drawable.ic_repeat_one_on);
                } else {
                    repeatMode = Player.REPEAT_MODE_OFF;
                    repeatBtn.setImageResource(R.drawable.ic_repeat_off);
                }
                player.setRepeatMode(repeatMode);
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int prog, boolean u) {
                    if (u) {
                        player.seekTo((long) prog * 1000);
                        durationPlayed.setText(formattedTime(prog));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });

            handler.post(updateProgress);
            // Refuerza la UI
            handler.postDelayed(() -> actualizarUI(player.getCurrentMediaItemIndex()), 150);
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_exoplayer);

        backBtn        = findViewById(R.id.back_btn);
        prevBtn        = findViewById(R.id.id_prev);
        nextBtn        = findViewById(R.id.id_next);
        shuffleBtn     = findViewById(R.id.id_shuffle);
        repeatBtn      = findViewById(R.id.id_repeat);
        playPauseBtn   = findViewById(R.id.play_pause);
        coverImage     = findViewById(R.id.coverImage);
        bgBlurImage    = findViewById(R.id.bg_blur);
        tituloText     = findViewById(R.id.tituloText);
        artistaText    = findViewById(R.id.artistaText);
        seekBar        = findViewById(R.id.seekBar);
        durationPlayed = findViewById(R.id.durationPlayed);
        durationTotal  = findViewById(R.id.durationTotal);

        backBtn.setOnClickListener(v -> finish());
        // NO registres receivers aquí
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent svc = new Intent(this, MediaPlaybackMp3Service.class);
        startService(svc);
        bindService(svc, conn, Context.BIND_AUTO_CREATE);

        // Registra los receivers con LocalBroadcastManager
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(closeReceiver, new IntentFilter("com.example.reproductorvideos.CERRAR_APP"));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(trackChangedReceiver, new IntentFilter(MediaPlaybackMp3Service.ACTION_TRACK_CHANGED));
    }

    @Override
    protected void onStop() {
        // Desregistra los receivers
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackChangedReceiver);

        if (serviceBound) {
            unbindService(conn);
            serviceBound = false;
        }
        handler.removeCallbacks(updateProgress);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mp3Service != null && serviceBound) {
            ExoPlayer p = mp3Service.getPlayer();
            actualizarUI(p.getCurrentMediaItemIndex());
        }
    }

    private void actualizarUI(int index) {
        List<Mp3File> list = mp3Service.getPlaylist();
        if (list == null || index < 0 || index >= list.size()) return;
        Mp3File m = list.get(index);

        tituloText.setText(m.getTitulo());
        artistaText.setText(m.getArtista());

        // Toma la carátula del MediaItem actual
        ExoPlayer player = mp3Service.getPlayer();
        Bitmap cover = null;

        MediaMetadata md = player.getMediaMetadata();
        byte[] art = md.artworkData;
        if (art != null) {
            cover = BitmapFactory.decodeByteArray(art, 0, art.length);
        } else if (m.getArtworkData() != null) {
            cover = BitmapFactory.decodeByteArray(m.getArtworkData(), 0, m.getArtworkData().length);
        } else {
            art = getAlbumArt(m.getRuta());
            if (art != null) cover = BitmapFactory.decodeByteArray(art, 0, art.length);
            else cover = BitmapFactory.decodeResource(getResources(), R.drawable.default_cover);
        }

        coverImage.setImageBitmap(cover);
        Glide.with(this)
                .load(cover)
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(25,3)))
                .into(bgBlurImage);
    }

    private byte[] getAlbumArt(String uri) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(uri);
            return r.getEmbeddedPicture();
        } catch (Exception e) {
            return null;
        } finally {
            try { r.release(); } catch (IOException ignored) {}
        }
    }

    private String formattedTime(int s) {
        int m = s / 60, sec = s % 60;
        return String.format("%d:%02d", m, sec);
    }
}

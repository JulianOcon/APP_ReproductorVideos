package com.example.reproductorvideos;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.reproductorvideos.model.Mp3File;
import com.example.reproductorvideos.utils.FavoritosManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import jp.wasabeef.glide.transformations.BlurTransformation;

import java.util.List;

@UnstableApi
public class ExoPlayerActivity extends AppCompatActivity {

    private MediaPlaybackMp3Service mp3Service;
    private boolean serviceBound = false;

    private ImageView backBtn, prevBtn, nextBtn, coverImage, bgBlurImage,
            shuffleBtn, repeatBtn;
    private FloatingActionButton playPauseBtn;
    private SeekBar seekBar;
    private TextView tituloText, artistaText, durationPlayed, durationTotal;

    private final Handler handler = new Handler();
    private boolean isShuffledEnabled = false;

    private ImageButton btnFavorito;

    private final Runnable updateProgress = new Runnable() {
        @Override public void run() {
            if (!serviceBound) return;
            ExoPlayer p = mp3Service.getPlayer();
            int cur = (int)(p.getCurrentPosition() / 1000);
            seekBar.setProgress(cur);
            durationPlayed.setText(formattedTime(cur));
            handler.postDelayed(this, 1000);
        }
    };

    private final BroadcastReceiver trackChangedReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            int idx = intent.getIntExtra(MediaPlaybackMp3Service.EXTRA_TRACK_INDEX, 0);
            actualizarUI(idx);
        }
    };
    private final BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            if (serviceBound) {
                unbindService(conn);
                serviceBound = false;
            }
            stopService(new Intent(ExoPlayerActivity.this, MediaPlaybackMp3Service.class));
            finishAffinity();
        }
    };

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder b) {
            mp3Service = ((MediaPlaybackMp3Service.LocalBinder) b).getService();
            serviceBound = true;

            @SuppressWarnings("unchecked")
            List<Mp3File> list =
                    (List<Mp3File>) getIntent().getSerializableExtra("mp3List");
            int pos = getIntent().getIntExtra("position", 0);
            mp3Service.setPlaylist(list, pos);

            // UI inicial
            actualizarUI(pos);
            handler.post(updateProgress);

            ExoPlayer player = mp3Service.getPlayer();
            isShuffledEnabled = player.getShuffleModeEnabled();
            shuffleBtn.setImageResource(
                    isShuffledEnabled ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off
            );

            // Listeners estándar
            player.addListener(new Player.Listener() {
                @Override public void onIsPlayingChanged(boolean isPlaying) {
                    playPauseBtn.setImageResource(isPlaying
                            ? R.drawable.ic_pause : R.drawable.ic_play);
                }
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        int tot = (int)(player.getDuration() / 1000);
                        seekBar.setMax(tot);
                        durationTotal.setText(formattedTime(tot));
                    }
                }
            });

            // Play/Pause
            playPauseBtn.setOnClickListener(v -> {
                if (player.isPlaying()) player.pause();
                else                   player.play();
            });

            prevBtn.setOnClickListener(v -> {
                if (!serviceBound) return;
                mp3Service.playPrevious();
            });

            nextBtn.setOnClickListener(v -> {
                if (!serviceBound) return;
                mp3Service.playNext();
            });

            shuffleBtn.setOnClickListener(v -> {
                boolean was = player.getShuffleModeEnabled();
                long posMs = player.getCurrentPosition();
                player.setShuffleModeEnabled(!was);
                shuffleBtn.setImageResource(
                        !was ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off
                );
                if (!was) {
                    int newIdx = player.getCurrentMediaItemIndex();
                    player.seekTo(newIdx, posMs);
                }
            });

            repeatBtn.setOnClickListener(v -> {
                int mode = player.getRepeatMode();
                int next = mode == Player.REPEAT_MODE_OFF
                        ? Player.REPEAT_MODE_ALL
                        : mode == Player.REPEAT_MODE_ALL
                        ? Player.REPEAT_MODE_ONE
                        : Player.REPEAT_MODE_OFF;
                player.setRepeatMode(next);
                repeatBtn.setImageResource(
                        next == Player.REPEAT_MODE_OFF   ? R.drawable.ic_repeat_off
                                : next == Player.REPEAT_MODE_ONE  ? R.drawable.ic_repeat_one_on
                                : R.drawable.ic_repeat_on
                );
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
                    if (fromUser) {
                        player.seekTo((long)prog * 1000);
                        durationPlayed.setText(formattedTime(prog));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        btnFavorito = findViewById(R.id.btnFavoritoRepro);

        backBtn.setOnClickListener(v -> finish());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (serviceBound) {
            int idx = mp3Service.getPlayer().getCurrentMediaItemIndex();
            actualizarUI(idx);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        Intent svc = new Intent(this, MediaPlaybackMp3Service.class);
        startService(svc);
        bindService(svc, conn, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(trackChangedReceiver,
                new IntentFilter(MediaPlaybackMp3Service.ACTION_TRACK_CHANGED));
        lbm.registerReceiver(closeReceiver,
                new IntentFilter("com.example.reproductorvideos.CERRAR_APP"));
    }

    @Override protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackChangedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeReceiver);
        if (serviceBound) {
            unbindService(conn);
            serviceBound = false;
        }
        handler.removeCallbacks(updateProgress);
        super.onDestroy();
    }

    private void actualizarUI(int index) {
        List<Mp3File> list = mp3Service.getPlaylist();
        if (list == null || index < 0 || index >= list.size()) return;
        Mp3File m = list.get(index);

        tituloText.setText(m.getTitulo());
        artistaText.setText(m.getArtista());

        // === FAVORITOS ===
        String url = m.getUrl();
        boolean esFavorito = FavoritosManager.esFavoritoMp3(this, url);
        btnFavorito.setImageResource(
                esFavorito ? R.drawable.ic_fav_on : R.drawable.ic_fav_off
        );
        btnFavorito.setOnClickListener(v -> {
            boolean nuevoEstado = !FavoritosManager.esFavoritoMp3(this, url);
            if (nuevoEstado) {
                FavoritosManager.agregarFavoritoMp3(this, url);
                btnFavorito.setImageResource(R.drawable.ic_fav_on);
            } else {
                FavoritosManager.quitarFavoritoMp3(this, url);
                btnFavorito.setImageResource(R.drawable.ic_fav_off);
            }
        });

        byte[] art = m.getArtworkData();
        if (art != null && art.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(art, 0, art.length);
            applyCoverAndBlur(bmp);
        } else if (m.getCoverUrl() != null && !m.getCoverUrl().isEmpty()) {
            Glide.with(this).asBitmap().load(m.getCoverUrl())
                    .into(new CustomTarget<Bitmap>() {
                        @Override public void onResourceReady(@NonNull Bitmap bmp,
                                                              @Nullable Transition<? super Bitmap> t) {
                            applyCoverAndBlur(bmp);
                        }
                        @Override public void onLoadCleared(@Nullable Drawable ph) {}
                    });
        } else {
            Bitmap ph = BitmapFactory.decodeResource(
                    getResources(), R.drawable.default_cover);
            applyCoverAndBlur(ph);
        }
    }

    private void applyCoverAndBlur(Bitmap bmp) {
        coverImage.setImageBitmap(bmp);
        Glide.with(this)
                .load(bmp)
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(25,3)))
                .into(bgBlurImage);
    }

    private String formattedTime(int s) {
        int m = s / 60, sec = s % 60;
        return String.format("%d:%02d", m, sec);
    }
}

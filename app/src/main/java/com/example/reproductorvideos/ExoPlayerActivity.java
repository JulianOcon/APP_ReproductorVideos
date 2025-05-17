package com.example.reproductorvideos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import jp.wasabeef.glide.transformations.BlurTransformation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.reproductorvideos.model.Mp3File;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExoPlayerActivity extends AppCompatActivity {

    private static final String TAG = "ExoPlayerActivity";
    private static final long LOOP_BUFFER_MS = 200; // buffer para el loop

    private ExoPlayer player;
    private FloatingActionButton playPauseBtn;
    private ImageView backBtn, prevBtn, nextBtn, coverImage, bgBlurImage, shuffleBtn, repeatBtn;
    private SeekBar seekBar;
    private TextView tituloText, artistaText, durationPlayed, durationTotal;
    private Handler handler = new Handler();
    private Bitmap currentCoverBitmap;

    private boolean isShuffleEnabled = false;
    private int repeatMode = Player.REPEAT_MODE_OFF;

    // Runnable que fuerza el loop en REPEAT_MODE_ONE
    private final Runnable manualLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && repeatMode == Player.REPEAT_MODE_ONE) {
                int idx = player.getCurrentMediaItemIndex();
                player.seekTo(idx, /* positionMs= */ 0);
                player.play();
            }
        }
    };

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exoplayer);

        // 1️⃣ Encuentra todas las vistas
        bgBlurImage    = findViewById(R.id.bg_blur);
        backBtn        = findViewById(R.id.back_btn);
        prevBtn        = findViewById(R.id.id_prev);
        nextBtn        = findViewById(R.id.id_next);
        shuffleBtn     = findViewById(R.id.id_shuffle);
        repeatBtn      = findViewById(R.id.id_repeat);
        playPauseBtn   = findViewById(R.id.play_pause);
        coverImage     = findViewById(R.id.coverImage);
        tituloText     = findViewById(R.id.tituloText);
        artistaText    = findViewById(R.id.artistaText);
        seekBar        = findViewById(R.id.seekBar);
        durationPlayed = findViewById(R.id.durationPlayed);
        durationTotal  = findViewById(R.id.durationTotal);

        // 2️⃣ Back
        backBtn.setOnClickListener(v -> finish());

        // 3️⃣ Iconos iniciales
        shuffleBtn.setImageResource(R.drawable.ic_shuffle_off);
        repeatBtn.setImageResource(R.drawable.ic_repeat_off);

        // 4️⃣ Configurar ExoPlayer
        player = new ExoPlayer.Builder(this).build();

        // 5️⃣ Carga la lista y posición inicial
        @SuppressWarnings("unchecked")
        List<Mp3File> mp3List = (ArrayList<Mp3File>) getIntent().getSerializableExtra("mp3List");
        int position = getIntent().getIntExtra("position", 0);

        List<MediaItem> items = new ArrayList<>();
        for (Mp3File m : mp3List) {
            items.add(MediaItem.fromUri(Uri.parse(m.getRuta())));
        }
        player.setMediaItems(items, position, C.TIME_UNSET);
        player.prepare();

        // → Aplica aquí **después** de prepare() el shuffle y repeat iniciales
        player.setShuffleModeEnabled(isShuffleEnabled);
        player.setRepeatMode(repeatMode);

        player.play();

        // 6️⃣ Shuffle toggle (añadido log para depurar)
        shuffleBtn.setOnClickListener(v -> {
            isShuffleEnabled = !isShuffleEnabled;
            player.setShuffleModeEnabled(isShuffleEnabled);
            shuffleBtn.setImageResource(
                    isShuffleEnabled
                            ? R.drawable.ic_shuffle_on
                            : R.drawable.ic_shuffle_off
            );
            Log.d(TAG, "Shuffle enabled: " + isShuffleEnabled);
        });

        // 7️⃣ Repeat cycle OFF → ALL → ONE → OFF (añadido log)
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
            Log.d(TAG, "Repeat mode set to: " + repeatMode);
        });

        // 8️⃣ Listener para cambio de pista y estado de reproducción
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                int idx = player.getCurrentMediaItemIndex();
                actualizarUI(mp3List.get(idx));
            }
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    // Cuando está listo, si es REPEAT_ONE, programa el loop manual
                    if (repeatMode == Player.REPEAT_MODE_ONE) {
                        handler.removeCallbacks(manualLoopRunnable);
                        long delay = player.getDuration() - player.getCurrentPosition() + LOOP_BUFFER_MS;
                        handler.postDelayed(manualLoopRunnable, delay);
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    // Fallback por si STATE_READY no llegara a tiempo
                    if (repeatMode == Player.REPEAT_MODE_ONE) {
                        int idx = player.getCurrentMediaItemIndex();
                        player.seekTo(idx, 0);
                        player.play();
                    }
                }
            }
        });

        // UI inicial
        actualizarUI(mp3List.get(position));

        // 9️⃣ Play / Pause
        playPauseBtn.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                playPauseBtn.setImageResource(R.drawable.ic_play);
            } else {
                player.play();
                playPauseBtn.setImageResource(R.drawable.ic_pause);
            }
        });

        // 🔟 Siguiente / Anterior
        nextBtn.setOnClickListener(v -> player.seekToNextMediaItem());
        prevBtn.setOnClickListener(v -> player.seekToPreviousMediaItem());

        // 1️⃣1️⃣ SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
                if (fromUser) {
                    player.seekTo((long) prog * 1000);
                    durationPlayed.setText(formattedTime(prog));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // 1️⃣2️⃣ Actualizar progreso cada segundo
        handler.post(updateProgress);
    }

    // Actualiza título, artista, carátula y fondo blur
    private void actualizarUI(Mp3File mp3) {
        tituloText.setText(mp3.getTitulo());
        artistaText.setText(mp3.getArtista());

        byte[] art = getAlbumArt(mp3.getRuta());
        if (art != null) {
            currentCoverBitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
        } else {
            currentCoverBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_cover);
        }
        coverImage.setImageBitmap(currentCoverBitmap);

        Glide.with(this)
                .load(currentCoverBitmap)
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3)))
                .into(bgBlurImage);

        handler.postDelayed(() -> {
            int tot = (int) (player.getDuration() / 1000);
            seekBar.setMax(tot);
            durationTotal.setText(formattedTime(tot));
        }, 300);
    }

    private final Runnable updateProgress = new Runnable() {
        @Override public void run() {
            if (player != null) {
                int sec = (int) (player.getCurrentPosition() / 1000);
                seekBar.setProgress(sec);
                durationPlayed.setText(formattedTime(sec));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(updateProgress);
        handler.removeCallbacks(manualLoopRunnable);
        if (player != null) {
            player.release();
            player = null;
        }
    }

    // Obtiene la carátula embebida
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

    // Convierte segundos a M:SS
    private String formattedTime(int s) {
        int m = s / 60, sec = s % 60;
        return String.format("%d:%02d", m, sec);
    }
}

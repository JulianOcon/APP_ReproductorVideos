package com.example.reproductorvideos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.reproductorvideos.model.Mp3File;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExoPlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private FloatingActionButton playPauseBtn;
    private ImageView backBtn, prevBtn, nextBtn, coverImage;
    private SeekBar seekBar;
    private TextView tituloText, artistaText, durationPlayed, durationTotal;
    private Handler handler = new Handler();

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exoplayer);

        // 1) Recoge lista y posición
        @SuppressWarnings("unchecked")
        List<Mp3File> mp3List = (ArrayList<Mp3File>)
                getIntent().getSerializableExtra("mp3List");
        int position = getIntent().getIntExtra("position", 0);

        // 2) Linkea vistas
        backBtn        = findViewById(R.id.back_btn);
        prevBtn        = findViewById(R.id.id_prev);
        nextBtn        = findViewById(R.id.id_next);
        playPauseBtn   = findViewById(R.id.play_pause);
        coverImage     = findViewById(R.id.coverImage);
        tituloText     = findViewById(R.id.tituloText);
        artistaText    = findViewById(R.id.artistaText);
        seekBar        = findViewById(R.id.seekBar);
        durationPlayed = findViewById(R.id.durationPlayed);
        durationTotal  = findViewById(R.id.durationTotal);

        // 3) Botón Back → finish()
        backBtn.setOnClickListener(v -> finish());

        // 4) Crea playlist en ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        List<MediaItem> items = new ArrayList<>();
        for (Mp3File m: mp3List) {
            items.add(MediaItem.fromUri(Uri.parse(m.getRuta())));
        }
        player.setMediaItems(items, position, 0);
        player.prepare();
        player.play();

        // 5) Cuando cambias de pista actualiza UI
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                int idx = player.getCurrentMediaItemIndex();
                actualizarUI(mp3List.get(idx));
            }
        });

        // 6) UI inicial
        actualizarUI(mp3List.get(position));

        // 7) Play/Pause
        playPauseBtn.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                playPauseBtn.setImageResource(R.drawable.ic_play);
            } else {
                player.play();
                playPauseBtn.setImageResource(R.drawable.ic_pause);
            }
        });

        // 8) Siguiente pista
        nextBtn.setOnClickListener(v -> {
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem();
            }
        });

        // 9) Pista anterior
        prevBtn.setOnClickListener(v -> {
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem();
            }
        });

        // 10) SeekBar manual + Handler
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
        handler.post(updateProgress);
    }

    private void actualizarUI(Mp3File mp3) {
        // Título y artista
        tituloText.setText(mp3.getTitulo());
        artistaText.setText(mp3.getArtista());
        // Carátula
        byte[] art = getAlbumArt(mp3.getRuta());
        if (art != null) {
            coverImage.setImageBitmap(
                    BitmapFactory.decodeByteArray(art, 0, art.length));
        } else {
            coverImage.setImageResource(R.drawable.default_cover);
        }
        // Duración total + SeekBar
        handler.postDelayed(() -> {
            int tot = (int)(player.getDuration()/1000);
            seekBar.setMax(tot);
            durationTotal.setText(formattedTime(tot));
        }, 300);
    }

    private final Runnable updateProgress = new Runnable() {
        @Override public void run() {
            if (player != null) {
                int sec = (int)(player.getCurrentPosition()/1000);
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
        if (player != null) {
            player.release();
            player = null;
        }
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
        int m = s/60, sec = s%60;
        return String.format("%d:%02d", m, sec);
    }
}

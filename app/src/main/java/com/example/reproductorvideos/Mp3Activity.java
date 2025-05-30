package com.example.reproductorvideos;

import android.app.Activity;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.reproductorvideos.model.Mp3File;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.ui.Mp3Adapter;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.BlurTransformation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@UnstableApi
public class Mp3Activity extends AppCompatActivity {

    private RecyclerView recyclerViewMp3, recyclerViewHistory;
    private Mp3Adapter adapter;
    private List<Mp3File> mp3ListOriginal = new ArrayList<>();

    // Vistas de búsqueda
    private ImageView icSearch, icClose;
    private EditText searchEditText;
    private HistoryAdapter historyAdapter;
    private final List<String> searchHistory = new ArrayList<>();

    private MediaPlaybackMp3Service mp3Service;
    private boolean serviceBound = false;

    // Mini-player views
    private View cardMini;
    private ImageView miniBgBlur, miniCover, miniPrev, miniPlayPause, miniNext;
    private TextView miniTitle, miniArtist, miniCurrentTime, miniTotalTime;
    private ImageButton btnVideos, btnMp3;

    // Handler para actualizar solo el tiempo cada segundo
    private final Handler miniHandler = new Handler();
    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            if (!serviceBound) return;
            Player p = mp3Service.getPlayer();
            int cur = (int)(p.getCurrentPosition() / 1000);
            miniCurrentTime.setText(formattedTime(cur));
            long durMs = p.getDuration();
            if (durMs > 0) {
                miniTotalTime.setText(formattedTime((int)(durMs / 1000)));
            }
            miniPlayPause.setImageResource(
                    p.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
            );
            // reprograma este mismo Runnable usando 'this'
            miniHandler.postDelayed(this, 1000);
        }
    };

    private final BroadcastReceiver trackChangedReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            updateMiniMetadata();
        }
    };
    private final BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            if (serviceBound) {
                unbindService(conn);
                serviceBound = false;
            }
            finishAffinity();
        }
    };

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            mp3Service = ((MediaPlaybackMp3Service.LocalBinder) binder).getService();
            serviceBound = true;
            updateMiniMetadata();
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3);

        // RecyclerView de MP3
        recyclerViewMp3 = findViewById(R.id.recyclerViewMp3);
        recyclerViewMp3.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Mp3Adapter(this);
        recyclerViewMp3.setAdapter(adapter);

        // Historial de búsqueda
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter();
        recyclerViewHistory.setAdapter(historyAdapter);

        // Inicialización buscador
        icSearch       = findViewById(R.id.ic_Search);
        icClose        = findViewById(R.id.ic_Close);
        searchEditText = findViewById(R.id.searchEditText);

        searchEditText.setVisibility(View.GONE);
        recyclerViewHistory.setVisibility(View.GONE);
        icClose.setVisibility(View.GONE);

        // Botones de modo
        btnVideos = findViewById(R.id.btnVideos);
        btnMp3    = findViewById(R.id.btnMp3);
        btnVideos.setImageResource(R.drawable.ic_video_off);
        btnMp3   .setImageResource(R.drawable.ic_music_on);
        btnVideos.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(
                    R.anim.slide_in_left, R.anim.slide_out_right
            );
            finish();
        });

        // Mini-player oculto
        cardMini = findViewById(R.id.cardMiniPlayer);
        cardMini.setVisibility(View.GONE);
        miniBgBlur      = findViewById(R.id.miniBgBlur);
        miniCover       = findViewById(R.id.miniCover);
        miniTitle       = findViewById(R.id.miniTitle);
        miniArtist      = findViewById(R.id.miniArtist);
        miniCurrentTime = findViewById(R.id.miniCurrentTime);
        miniTotalTime   = findViewById(R.id.miniTotalTime);
        miniPrev        = findViewById(R.id.miniPrev);
        miniPlayPause   = findViewById(R.id.miniPlayPause);
        miniNext        = findViewById(R.id.miniNext);

        miniPrev.setOnClickListener(v -> {
            if (!serviceBound) return;
            mp3Service.playPrevious();
            miniPlayPause.setImageResource(R.drawable.ic_pause);
        });
        miniPlayPause.setOnClickListener(v -> {
            if (!serviceBound) return;
            Player p = mp3Service.getPlayer();
            if (p.isPlaying()) {
                p.pause();
                miniPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                p.play();
                miniPlayPause.setImageResource(R.drawable.ic_pause);
            }
        });
        miniNext.setOnClickListener(v -> {
            if (!serviceBound) return;
            mp3Service.playNext();
            miniPlayPause.setImageResource(R.drawable.ic_pause);
        });
        cardMini.setOnClickListener(v -> {
            if (!serviceBound) return;
            startActivity(new Intent(Mp3Activity.this, ExoPlayerActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        });

        // Apertura del buscador
        icSearch.setOnClickListener(v -> {
            icSearch.setVisibility(View.GONE);
            searchEditText.setVisibility(View.VISIBLE);
            recyclerViewHistory.setVisibility(View.VISIBLE);
            icClose.setVisibility(View.VISIBLE);

            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);

            historyAdapter.updateHistory(searchHistory);
        });

        // Cierre del buscador
        icClose.setOnClickListener(v -> {
            searchEditText.setText("");
            searchEditText.setVisibility(View.GONE);
            recyclerViewHistory.setVisibility(View.GONE);
            icClose.setVisibility(View.GONE);
            icSearch.setVisibility(View.VISIBLE);
        });

        // Filtrado en tiempo real
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filtrarMp3(s.toString());
            }
        });

        // Guardar término en historial
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String term = searchEditText.getText().toString().trim();
                if (!term.isEmpty() && !searchHistory.contains(term)) {
                    searchHistory.add(0, term);
                    historyAdapter.updateHistory(searchHistory);
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        fetchMp3Files();
    }

    @Override
    protected void onStart() {
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

    @Override
    protected void onResume() {
        super.onResume();
        miniHandler.post(timeUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        miniHandler.removeCallbacks(timeUpdater);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(trackChangedReceiver);
        lbm.unregisterReceiver(closeReceiver);
        if (serviceBound) {
            unbindService(conn);
            serviceBound = false;
        }
    }

    private void fetchMp3Files() {
        RetrofitClient.getApiService(this)
                .obtenerMp3()
                .enqueue(new Callback<List<Mp3File>>() {
                    @Override public void onResponse(Call<List<Mp3File>> call,
                                                     Response<List<Mp3File>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            mp3ListOriginal = resp.body();
                            adapter.setMp3List(mp3ListOriginal);
                        } else {
                            Toast.makeText(Mp3Activity.this,
                                    "Error al cargar MP3", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<List<Mp3File>> call, Throwable t) {
                        Toast.makeText(Mp3Activity.this,
                                "Fallo red: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void filtrarMp3(String query) {
        if (mp3ListOriginal == null) return;
        List<Mp3File> filtrados = new ArrayList<>();
        for (Mp3File mp3 : mp3ListOriginal) {
            if (mp3.getTitulo().toLowerCase().contains(query.toLowerCase())
                    || mp3.getArtista().toLowerCase().contains(query.toLowerCase())) {
                filtrados.add(mp3);
            }
        }
        adapter.setMp3List(filtrados);
    }

    private void updateMiniMetadata() {
        if (!serviceBound) return;
        Player p = mp3Service.getPlayer();
        List<Mp3File> pl = mp3Service.getPlaylist();
        if (pl == null || pl.isEmpty() || p.getMediaItemCount() == 0) {
            cardMini.setVisibility(View.GONE);
            return;
        }
        cardMini.setVisibility(View.VISIBLE);
        Mp3File m = pl.get(p.getCurrentMediaItemIndex());
        miniTitle.setText(m.getTitulo());
        miniArtist.setText(m.getArtista());

        byte[] art = m.getArtworkData();
        if (art != null && art.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(art, 0, art.length);
            applyMiniCoverAndBlur(bmp);
        } else if (m.getCoverUrl() != null && !m.getCoverUrl().isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(m.getCoverUrl())
                    .into(new CustomTarget<Bitmap>() {
                        @Override public void onResourceReady(@NonNull Bitmap bmp,
                                                              @Nullable Transition<? super Bitmap> t) {
                            applyMiniCoverAndBlur(bmp);
                        }
                        @Override public void onLoadCleared(@Nullable Drawable ph) {}
                    });
        } else {
            Bitmap ph = BitmapFactory.decodeResource(
                    getResources(), R.drawable.default_cover);
            applyMiniCoverAndBlur(ph);
        }
    }

    private void applyMiniCoverAndBlur(Bitmap bmp) {
        miniCover.setImageBitmap(bmp);
        Glide.with(this)
                .load(bmp)
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(15,3)))
                .into(miniBgBlur);
    }

    private String formattedTime(int s) {
        int m = s / 60, sec = s % 60;
        return String.format("%d:%02d", m, sec);
    }

    public boolean isServiceBound() {
        return serviceBound;
    }

    private static class HistoryAdapter
            extends RecyclerView.Adapter<HistoryAdapter.HViewHolder> {
        private final List<String> items = new ArrayList<>();
        void updateHistory(List<String> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }
        @NonNull @Override
        public HViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = (TextView) LayoutInflater
                    .from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            tv.setTextColor(0xFFFFFFFF);
            return new HViewHolder(tv);
        }
        @Override
        public void onBindViewHolder(@NonNull HViewHolder holder, int position) {
            String txt = items.get(position);
            holder.tv.setText(txt);
            holder.tv.setOnClickListener(v -> {
                EditText et = ((Activity)v.getContext())
                        .findViewById(R.id.searchEditText);
                et.setText(txt);
                et.setSelection(txt.length());
                ((Mp3Activity)v.getContext()).adapter
                        .setMp3List(((Mp3Activity)v.getContext()).mp3ListOriginal);
            });
        }
        @Override public int getItemCount() {
            return items.size();
        }
        static class HViewHolder extends RecyclerView.ViewHolder {
            final TextView tv;
            HViewHolder(TextView v) {
                super(v);
                tv = v;
            }
        }
    }
}

package com.example.reproductorvideos;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Mp3File;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.ui.Mp3Adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@UnstableApi
public class Mp3Activity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION = 1001;

    private RecyclerView recyclerViewMp3, recyclerViewHistory;
    private Mp3Adapter adapter;
    private EditText searchEditText;
    private ImageView icSearch, logoImageView;
    private Button btnSwitchMode;
    private List<Mp3File> mp3ListOriginal = new ArrayList<>();
    private final List<String> searchHistory = new ArrayList<>();

    private MediaPlaybackMp3Service mp3Service;
    private boolean serviceBound = false;

    private final ServiceConnection conn = new ServiceConnection() {
        @OptIn(markerClass = UnstableApi.class)
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mp3Service = ((MediaPlaybackMp3Service.LocalBinder) binder).getService();
            serviceBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private final BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (serviceBound) {
                unbindService(conn);
                serviceBound = false;
            }

            finishAffinity(); // solo esto aquí, sin TransparentExitActivity
        }
    };











    public boolean isServiceBound() {
        return serviceBound;
    }

    @OptIn(markerClass = UnstableApi.class)
    public MediaPlaybackMp3Service getMp3Service() {
        return mp3Service;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp3);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION);
            }
        }

        logoImageView       = findViewById(R.id.logoImageView);
        icSearch            = findViewById(R.id.ic_Search);
        searchEditText      = findViewById(R.id.searchEditText);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewMp3     = findViewById(R.id.recyclerViewMp3);
        btnSwitchMode       = findViewById(R.id.btnSwitchMode);

        logoImageView.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add("Cerrar sesión");
            popup.setOnMenuItemClickListener(item -> {
                startActivity(new Intent(this, LoginActivity.class));
                finishAffinity();
                return true;
            });
            popup.show();
        });

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        HistoryAdapter historyAdapter = new HistoryAdapter();
        recyclerViewHistory.setAdapter(historyAdapter);

        recyclerViewMp3.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Mp3Adapter(this);
        recyclerViewMp3.setAdapter(adapter);

        icSearch.setOnClickListener(v -> {
            icSearch.setVisibility(View.GONE);
            searchEditText.setVisibility(View.VISIBLE);
            recyclerViewHistory.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchEditText,
                    InputMethodManager.SHOW_IMPLICIT);
            historyAdapter.updateHistory(searchHistory);
        });

        searchEditText.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_UP
                    && searchEditText.getCompoundDrawables()[0] != null) {
                int w = searchEditText.getCompoundDrawables()[0].getBounds().width();
                if (e.getX() <= searchEditText.getPaddingLeft() + w) {
                    searchEditText.setText("");
                    searchEditText.setVisibility(View.GONE);
                    recyclerViewHistory.setVisibility(View.GONE);
                    icSearch.setVisibility(View.VISIBLE);
                    adapter.setMp3List(mp3ListOriginal);
                    return true;
                }
            }
            return false;
        });

        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void afterTextChanged(android.text.Editable s){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){
                String q = s.toString().toLowerCase();
                List<Mp3File> filt = new ArrayList<>();
                for (Mp3File m : mp3ListOriginal) {
                    if (m.getTitulo().toLowerCase().contains(q)
                            || m.getArtista().toLowerCase().contains(q)) {
                        filt.add(m);
                    }
                }
                adapter.setMp3List(filt);
            }
        });

        searchEditText.setOnEditorActionListener((v, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String term = v.getText().toString().trim();
                if (!term.isEmpty() && !searchHistory.contains(term)) {
                    searchHistory.add(0, term);
                    historyAdapter.updateHistory(searchHistory);
                }
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(
                        v.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        btnSwitchMode.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        fetchMp3Files();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onStart() {
        super.onStart();
        Intent svc = new Intent(this, MediaPlaybackMp3Service.class);
        startService(svc);
        bindService(svc, conn, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(closeReceiver, new IntentFilter("com.example.reproductorvideos.CERRAR_APP"));
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeReceiver);
        if (serviceBound) {
            unbindService(conn);
            serviceBound = false;
        }
        super.onDestroy();
    }

    private void fetchMp3Files() {
        RetrofitClient.getApiService(this)
                .obtenerMp3()
                .enqueue(new Callback<List<Mp3File>>() {
                    @OptIn(markerClass = UnstableApi.class)
                    @Override
                    public void onResponse(Call<List<Mp3File>> call,
                                           Response<List<Mp3File>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            mp3ListOriginal = resp.body();
                            Executors.newSingleThreadExecutor().execute(() -> {
                                for (Mp3File m : mp3ListOriginal) {
                                    m.setArtworkData(getAlbumArt(m.getUrl()));
                                }
                                runOnUiThread(() -> {
                                    adapter.setMp3List(mp3ListOriginal);
                                });
                            });
                        } else {
                            Toast.makeText(Mp3Activity.this,
                                    "Error al cargar MP3", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Mp3File>> call, Throwable t) {
                        Toast.makeText(Mp3Activity.this,
                                "Fallo red: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private byte[] getAlbumArt(String uri) {
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

    private static class HistoryAdapter
            extends RecyclerView.Adapter<HistoryAdapter.HViewHolder> {
        private final List<String> items = new ArrayList<>();
        void updateHistory(List<String> data) {
            items.clear(); items.addAll(data); notifyDataSetChanged();
        }
        @NonNull @Override
        public HViewHolder onCreateViewHolder(@NonNull ViewGroup p, int v){
            TextView tv = (TextView) LayoutInflater
                    .from(p.getContext())
                    .inflate(android.R.layout.simple_list_item_1,p,false);
            tv.setTextColor(Color.WHITE);
            return new HViewHolder(tv);
        }
        @Override public void onBindViewHolder(@NonNull HViewHolder h,int pos){
            String text = items.get(pos);
            h.tv.setText(text);
            h.tv.setOnClickListener(v -> {
                EditText et = ((Activity)v.getContext())
                        .findViewById(R.id.searchEditText);
                et.setText(text);
                et.setSelection(text.length());
                ((Mp3Activity)v.getContext()).adapter.setMp3List(
                        ((Mp3Activity)v.getContext()).mp3ListOriginal
                );
            });
        }
        @Override public int getItemCount(){ return items.size(); }
        static class HViewHolder extends RecyclerView.ViewHolder {
            final TextView tv;
            HViewHolder(TextView v){ super(v); tv = v;}
        }
    }
}

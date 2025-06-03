package com.example.reproductorvideos;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reproductorvideos.model.Video;
import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.network.IPService;
import com.example.reproductorvideos.network.RetrofitClient;
import com.example.reproductorvideos.network.ServerInfo;
import com.example.reproductorvideos.ui.VideoAdapter;
import com.example.reproductorvideos.utils.FavoritosManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView, recyclerViewHistory;
    private VideoAdapter adapter;
    private HistoryAdapter historyAdapter;
    private EditText searchEditText;
    private ImageView icSearch, icClose, logoImageView;
    private SharedPreferences prefs;
    private ImageButton btnVideos, btnMp3;

    private ImageView btnVerFavoritos;

    private List<Video> videoListOriginal;
    private final List<String> searchHistory = new ArrayList<>();

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // autenticación
        prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // vistas
        logoImageView       = findViewById(R.id.logoImageView);
        icSearch            = findViewById(R.id.ic_Search);
        icClose             = findViewById(R.id.ic_Close);
        searchEditText      = findViewById(R.id.searchEditText);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerView        = findViewById(R.id.recyclerView);
        btnVideos           = findViewById(R.id.btnVideos);
        btnMp3              = findViewById(R.id.btnMp3);
        btnVerFavoritos     = findViewById(R.id.btnVerFavoritos);

        btnVerFavoritos.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FavoritosActivity.class));
        });

        btnVideos.setImageResource(R.drawable.ic_video_on);
        btnMp3.setImageResource(R.drawable.ic_music_off);
        btnVideos.setOnClickListener(v -> {
        });
        btnMp3.setOnClickListener(v -> {
            startActivity(new Intent(this, Mp3Activity.class));
            overridePendingTransition(R.anim.slide_in_rigth, R.anim.slide_out_left);
            finish();
        });

        int iconSizePx = getResources().getDimensionPixelSize(R.dimen.search_icon_size);
        Drawable[] drawables = searchEditText.getCompoundDrawables();
        if (drawables[0] != null) {
            drawables[0].setBounds(0, 0, iconSizePx, iconSizePx);
            searchEditText.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
        }

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter();
        recyclerViewHistory.setAdapter(historyAdapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VideoAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        logoImageView.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add("Cerrar sesión");
            popup.setOnMenuItemClickListener(item -> {
                prefs.edit().remove("jwt_token").apply();
                startActivity(new Intent(this, LoginActivity.class));
                finishAffinity();
                return true;
            });
            popup.show();
        });

        searchEditText.setVisibility(View.GONE);
        recyclerViewHistory.setVisibility(View.GONE);
        icClose.setVisibility(View.GONE);

        icSearch.setOnClickListener(v -> {
            icSearch.setVisibility(View.GONE);
            icClose.setVisibility(View.VISIBLE);
            searchEditText.setVisibility(View.VISIBLE);
            recyclerViewHistory.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            historyAdapter.updateHistory(searchHistory);
        });

        icClose.setOnClickListener(v -> {
            searchEditText.setText("");
            searchEditText.setVisibility(View.GONE);
            recyclerViewHistory.setVisibility(View.GONE);
            icClose.setVisibility(View.GONE);
            icSearch.setVisibility(View.VISIBLE);
        });

        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                filtrarVideos(s.toString());
            }
        });

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

        fetchServerIpAndVideos();
    }

    private void fetchServerIpAndVideos() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.9:3000/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        IPService ipService = retrofit.create(IPService.class);
        ipService.getServerInfo().enqueue(new Callback<ServerInfo>() {
            @Override public void onResponse(Call<ServerInfo> call, Response<ServerInfo> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    String base = "http://" + resp.body().getIpPublica() + ":3000/api/";
                    RetrofitClient.setBaseUrl(base);
                    fetchVideos();
                }
            }
            @Override public void onFailure(Call<ServerInfo> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Error IP: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchVideos() {
        ApiService api = RetrofitClient.getApiService(this);
        api.obtenerVideos().enqueue(new Callback<List<Video>>() {
            @Override public void onResponse(Call<List<Video>> call, Response<List<Video>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    videoListOriginal = resp.body();
                    for (Video v : videoListOriginal) {
                        v.setFavorito(FavoritosManager.esFavorito(MainActivity.this, v.getUrl()));
                    }
                    adapter.actualizarLista(new ArrayList<>(videoListOriginal));
                } else if (resp.code() == 401) {
                    prefs.edit().remove("jwt_token").apply();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Error videos: " + resp.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<List<Video>> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Fallo red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filtrarVideos(String query) {
        if (videoListOriginal == null) return;
        List<Video> filtrados = new ArrayList<>();
        for (Video v : videoListOriginal) {
            if (v.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filtrados.add(v);
            }
        }
        adapter.actualizarLista(filtrados);
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
        public HViewHolder onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
            TextView tv = (TextView) LayoutInflater.from(p.getContext())
                    .inflate(android.R.layout.simple_list_item_1, p, false);
            tv.setTextColor(Color.WHITE);
            return new HViewHolder(tv);
        }
        @Override public void onBindViewHolder(@NonNull HViewHolder h, int pos) {
            String text = items.get(pos);
            h.tv.setText(text);
            h.tv.setOnClickListener(v -> {
                EditText et = ((Activity) v.getContext())
                        .findViewById(R.id.searchEditText);
                et.setText(text);
                et.setSelection(text.length());
            });
        }
        @Override public int getItemCount() { return items.size(); }
        static class HViewHolder extends RecyclerView.ViewHolder {
            final TextView tv;
            HViewHolder(TextView v) { super(v); tv = v; }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

    }
}

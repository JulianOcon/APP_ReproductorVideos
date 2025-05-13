package com.example.reproductorvideos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoriasActivity extends AppCompatActivity {
    private ListView listViewCategorias;
    private static final String TAG = "CategoriasActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categorias);

        listViewCategorias = findViewById(R.id.listViewCategorias);
        fetchCategorias();
    }

    private void fetchCategorias() {
        // Llamada al API incluyendo el JWT en cabecera si existe
        ApiService api = RetrofitClient.getApiService(this);
        api.getCategoriasDisponibles().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(@NonNull Call<List<String>> call,
                                   @NonNull Response<List<String>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    List<String> categorias = resp.body();

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            CategoriasActivity.this,
                            android.R.layout.simple_list_item_1,
                            categorias);
                    listViewCategorias.setAdapter(adapter);

                    listViewCategorias.setOnItemClickListener((parent, view, position, id) -> {
                        String categoria = categorias.get(position);
                        Intent intent = new Intent(CategoriasActivity.this, MainActivity.class);
                        intent.putExtra("categoria", categoria);
                        startActivity(intent);
                    });

                } else {
                    Toast.makeText(CategoriasActivity.this,
                            "Error al cargar categorías: " + resp.code(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "HTTP " + resp.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {
                Toast.makeText(CategoriasActivity.this,
                        "Fallo de conexión: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "onFailure", t);
            }
        });
    }
}

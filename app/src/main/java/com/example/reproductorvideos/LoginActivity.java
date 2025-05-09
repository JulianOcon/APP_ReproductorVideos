package com.example.reproductorvideos;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reproductorvideos.model.LoginRequest;
import com.example.reproductorvideos.model.LoginResponse;
import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.network.RetrofitClient;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsuario, etContrasena;
    private Button btnIniciarSesion;
    private TextView tvIrARegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsuario = findViewById(R.id.etUsuario);
        etContrasena = findViewById(R.id.etContrasena);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        tvIrARegistro = findViewById(R.id.tvIrARegistro); // 👈 debes tener este ID en tu XML

        btnIniciarSesion.setOnClickListener(v -> {
            String usuario = etUsuario.getText().toString().trim();
            String contrasena = etContrasena.getText().toString().trim();

            Log.d("LOGIN_DEBUG", "Enviando usuario: [" + usuario + "]");

            if (usuario.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            LoginRequest request = new LoginRequest(usuario, contrasena);
            ApiService api = RetrofitClient.getApiService();

            api.login(request).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse res = response.body();
                        Log.d("LOGIN_DEBUG", "Respuesta: " + new Gson().toJson(res));

                        if (res.isSuccess()) {
                            Toast.makeText(LoginActivity.this, "Bienvenido " + res.getUsuario(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, res.getMensaje(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("LOGIN_DEBUG", "Error en la respuesta: code=" + response.code());
                        Toast.makeText(LoginActivity.this, "Respuesta inválida del servidor", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Log.e("LOGIN_DEBUG", "Error de red: " + t.getMessage());
                    Toast.makeText(LoginActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // ✅ Ir a registro
        tvIrARegistro.setOnClickListener(v -> {
            Log.d("LOGIN_DEBUG", "Click en 'Regístrate aquí'");
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}

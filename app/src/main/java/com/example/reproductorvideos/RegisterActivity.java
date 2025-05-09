package com.example.reproductorvideos;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reproductorvideos.model.RegisterRequest;
import com.example.reproductorvideos.model.RegisterResponse;
import com.example.reproductorvideos.network.ApiService;
import com.example.reproductorvideos.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNombre, etApellidos, etTelefono, etContrasena;
    private Button btnRegistrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre = findViewById(R.id.etNombre);
        etApellidos = findViewById(R.id.etApellidos);
        etTelefono = findViewById(R.id.etTelefono);
        etContrasena = findViewById(R.id.etContrasena);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String apellidos = etApellidos.getText().toString().trim();
            String telefono = etTelefono.getText().toString().trim();
            String contrasena = etContrasena.getText().toString().trim();

            if (nombre.isEmpty() || apellidos.isEmpty() || telefono.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            String tipoUsuario = "Basico"; // fijo

            RegisterRequest request = new RegisterRequest(
                    nombre, apellidos, telefono, contrasena, tipoUsuario
            );

            ApiService api = RetrofitClient.getApiService();
            api.register(request).enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(RegisterActivity.this, response.body().getMensaje(), Toast.LENGTH_SHORT).show();
                        if (response.body().isSuccess()) {
                            finish(); // cierra la pantalla si fue exitoso
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error en el registro", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable t) {
                    Toast.makeText(RegisterActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}

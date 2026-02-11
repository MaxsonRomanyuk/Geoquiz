package com.example.geoquiz_frontend.UI.Auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.appcompat.app.AppCompatActivity;

import com.example.geoquiz_frontend.ApiClient;
import com.example.geoquiz_frontend.ApiService;
import com.example.geoquiz_frontend.AuthManager;
import com.example.geoquiz_frontend.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.TestRequest;
import com.example.geoquiz_frontend.TestResponse;

public class MainActivity extends AppCompatActivity {
    private PreferencesHelper preferencesHelper;
    private AuthManager authManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.btnSend);
        Button start = findViewById(R.id.btnStartSolo);
        TextView txt = findViewById(R.id.txtResult);

        preferencesHelper = new PreferencesHelper(this);
        authManager = new AuthManager(this);

        btn.setOnClickListener(v -> {
            ApiService api = ApiClient.getApi();

            TestRequest request = new TestRequest("Hello from Android");

            api.echo(request).enqueue(new Callback<TestResponse>() {
                @Override
                public void onResponse(Call<TestResponse> call, Response<TestResponse> response) {
                    if (response.isSuccessful()) {
                        txt.setText(response.body().message +
                                "\nReceived: " + response.body().received);
                    }
                }

                @Override
                public void onFailure(Call<TestResponse> call, Throwable t) {
                    txt.setText("Error: " + t.getMessage());
                }
            });
        });
        start.setOnClickListener(v -> showLogoutConfirmation());
    }
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        String toastMessage = "You have logged out";

        authManager.logout();
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
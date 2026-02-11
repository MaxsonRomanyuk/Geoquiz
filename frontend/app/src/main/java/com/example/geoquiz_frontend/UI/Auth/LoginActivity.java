package com.example.geoquiz_frontend.UI.Auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.geoquiz_frontend.ApiClient;
import com.example.geoquiz_frontend.ApiService;
import com.example.geoquiz_frontend.AuthManager;
import com.example.geoquiz_frontend.DTOs.AuthResponse;
import com.example.geoquiz_frontend.DTOs.LoginRequest;
import com.example.geoquiz_frontend.DTOs.RegisterRequest;
import com.example.geoquiz_frontend.Entities.User;
import com.example.geoquiz_frontend.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity{
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButton btnAuth;
    private TextInputLayout emailLayout, passwordLayout, nameLayout;
    private TextInputEditText etEmail, etPassword, etName;
    private TextView tvForgotPassword;
    private MaterialCardView btnGuest;

    private boolean isLoginMode = true;
    private AuthManager authManager;
    private PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new AuthManager(this);
        preferencesHelper = new PreferencesHelper(this);
        if (authManager.isLoggedIn()) {
            startMainActivity();
            return;
        }
        initViews();
        setupClickListeners();

    }

    private void initViews() {
        toggleGroup = findViewById(R.id.toggleGroup);
        btnAuth = findViewById(R.id.btn_auth);

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        nameLayout = findViewById(R.id.nameLayout);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);

        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnGuest = findViewById(R.id.btn_guest);

        nameLayout.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isLoginMode = checkedId == R.id.btn_login;
                updateAuthMode();
            }
        });

        btnAuth.setOnClickListener(v -> performAuth());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        btnGuest.setOnClickListener(v -> loginAsGuest());
    }
    private void updateAuthMode() {
        if (isLoginMode) {
            btnAuth.setText(getString(R.string.login));
            tvForgotPassword.setVisibility(View.VISIBLE);
            nameLayout.setVisibility(View.GONE);
        } else {
            btnAuth.setText(getString(R.string.register));
            tvForgotPassword.setVisibility(View.GONE);
            nameLayout.setVisibility(View.VISIBLE);
        }
    }

    private void performAuth() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = isLoginMode ? "" : etName.getText().toString().trim();

        if (!validateInputs(email, password, name)) {
            return;
        }

        showLoading(true);

        if (isLoginMode) {
            loginUser(email, password);
        } else {
            registerUser(email, password, name);
        }
    }

    private boolean validateInputs(String email, String password, String name) {
        emailLayout.setError(null);
        passwordLayout.setError(null);
        nameLayout.setError(null);
        if (!isLoginMode && TextUtils.isEmpty(name)) {
            nameLayout.setError("Введите имя");
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Введите email");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Введите корректный email");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Введите пароль");
            return false;
        }
        if (password.length() < 6) {
            passwordLayout.setError("Пароль должен содержать минимум 6 символов");
            return false;
        }

        return true;
    }

    private void loginUser(String email, String password) {
        ApiService api = ApiClient.getApi();

        LoginRequest request = new LoginRequest(email, password);

        api.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();

                    getUserData(email, token);
                } else {
                    showTempMessage(getString(R.string.error_user_not_found));
                    showLoading(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                showTempMessage("Ошибка подключения: " + t.getMessage());
                showLoading(false);
            }
        });
    }

    private void registerUser(String email, String password, String name) {
        ApiService api = ApiClient.getApi();

        RegisterRequest request = new RegisterRequest(name, email, password);

        api.register(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    showTempMessage("Регистрация успешна!");
                    loginUser(email, password);
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        showTempMessage("Ошибка регистрации: email уже занят");
                    } catch (IOException e) {
                        showTempMessage("Ошибка регистрации");
                    }
                    showLoading(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                showTempMessage("Ошибка подключения: " + t.getMessage());
                showLoading(false);
            }
        });
    }

    private void getUserData(String email, String token) {
        if (preferencesHelper == null) {
            preferencesHelper = new PreferencesHelper(LoginActivity.this);
        }
        saveAuthToken(token);

        ApiService api = ApiClient.getApi();

        User user = new User();
        user.setEmail(email);
        user.setName(email.split("@")[0]); // Временное решение
        user.setPremium(false);

        authManager.LoginWithEmail(user);
        startMainActivity();
    }

    private void saveAuthToken(String token) {
        preferencesHelper.saveAuthToken(token);
    }

    private void loginAsGuest() {
        authManager.loginAsGuest();
        startMainActivity();
    }
    private void showTempMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void showForgotPasswordDialog() {
        Toast.makeText(this, "Функция восстановления пароля будет доступна в следующем обновлении", Toast.LENGTH_LONG).show();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        btnAuth.setEnabled(!show);
        btnAuth.setText(show ? "Загрузка..." : (isLoginMode ? "Вход" : "Регистрация"));
    }
}

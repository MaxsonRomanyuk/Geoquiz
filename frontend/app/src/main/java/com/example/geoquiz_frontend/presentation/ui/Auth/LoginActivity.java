package com.example.geoquiz_frontend.presentation.ui.Auth;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.annotation.NonNull;
import com.example.geoquiz_frontend.data.remote.ApiClient;
import com.example.geoquiz_frontend.data.remote.ApiService;
import com.example.geoquiz_frontend.data.remote.NotificationManager;
import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.example.geoquiz_frontend.presentation.utils.AuthManager;
import com.example.geoquiz_frontend.data.remote.dtos.auth.AuthResponse;
import com.example.geoquiz_frontend.data.remote.dtos.solo.BootstrapResponse;
import com.example.geoquiz_frontend.data.remote.dtos.auth.LoginRequest;
import com.example.geoquiz_frontend.data.remote.dtos.auth.RegisterRequest;
import com.example.geoquiz_frontend.data.repositories.GameRepository;
import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.domain.engine.GameManager;
import com.example.geoquiz_frontend.domain.entities.User;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.presentation.ui.Home.MainActivity;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.example.geoquiz_frontend.presentation.utils.TokenRefreshHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

public class LoginActivity extends BaseActivity {
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButton btnAuth;
    private TextInputLayout emailLayout, passwordLayout, nameLayout;
    private TextInputEditText etEmail, etPassword, etName;
    private TextView tvForgotPassword;
    private MaterialCardView btnGuest;

    private boolean isLoginMode = true;
    private AuthManager authManager;
    private UserRepository userRepository;
    private GameManager gameManager;

    private UserStats userStats;
    private NotificationManager notificationManager;
    private String activityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferencesHelper = new SecurePreferencesHelper(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new AuthManager(this);

        if (authManager.isLoggedIn()) {
            loadBoostrapData();

            userRepository = UserRepository.getInstance(this);
            userRepository.loadUserData(false);

            if (preferencesHelper.hasValidAccessToken()) {
                activityId = "login_" + System.currentTimeMillis();
                notificationManager = NotificationManager.getInstance();
                String token = preferencesHelper.getAuthToken();
                String userId = preferencesHelper.getUserId();

                notificationManager.reset();
                notificationManager.init(token, userId);
                connectToSignalR();
            }
            else {
                TokenRefreshHelper tokenRefreshHelper = new TokenRefreshHelper(this, preferencesHelper);
                tokenRefreshHelper.refreshTokenAsync(new TokenRefreshHelper.TokenRefreshCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("TokenManager", "Token refreshed on app start");
                        activityId = "login_" + System.currentTimeMillis();
                        notificationManager = NotificationManager.getInstance();
                        String newToken = preferencesHelper.getAuthToken();
                        String userId = preferencesHelper.getUserId();

                        notificationManager.reset();
                        notificationManager.init(newToken, userId);
                        connectToSignalR();
                    }
                    @Override
                    public void onFailure(String error) {
                        Log.w("TokenManager", "Could not refresh token on start: " + error);
                    }
                });
            }
            startMainActivity();
            return;
        }
        initViews();
        setupClickListeners();

        String statsJson = getIntent().getStringExtra("USER_STATS_JSON");
        if (statsJson != null) {
            Gson gson = new Gson();
            userStats = gson.fromJson(statsJson, UserStats.class);
            userStats.getExperience();
        }
    }
    private void loadBoostrapData()
    {
        gameManager = GameManager.getInstance(this);
        gameManager.loadBootstrapData(new GameRepository.BootstrapCallback() {
            @Override
            public void onSuccess(BootstrapResponse data) {
            }
            @Override
            public void onError(String error) {
            }
        });
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
    private void connectToSignalR() {
        notificationManager.addListener(activityId, new NotificationManager.ConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Connected, joining queue");
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Disconnected");
                });
            }

            @Override
            public void onAchievementUnlocked(List<ProfileResponse.AchievementDto> data) {
            }

            @Override
            public void onConnectionFailed(String reason) {
            }
        });
        notificationManager.start();
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
        @SuppressLint("HardwareIds") String deviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        LoginRequest request = new LoginRequest(email, password, deviceId);
        api.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String accessToken = response.body().getAccessToken();
                    String refreshToken = response.body().getRefreshToken();
                    String uid = response.body().getUserId();
                    String name = response.body().getUserName();
                    long expiresIn = response.body().getExpiresIn();
                    saveUserData(accessToken, refreshToken, email, uid, name, expiresIn);
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
        if (userStats != null) request = new RegisterRequest(name, email, password, userStats);


        api.register(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    showTempMessage("Регистрация успешна!");
                    loginUser(email, password);
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        showTempMessage(errorBody);
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

    private void saveUserData(String accessToken, String refreshToken, String email, String uid, String name, long expiresIn) {
        if (preferencesHelper == null) {
            preferencesHelper = new SecurePreferencesHelper(LoginActivity.this);
        }

        saveAuthToken(accessToken, refreshToken , expiresIn);
        User user = new User(uid, email, name);
        authManager.LoginWithEmail(user);

        loadBoostrapData();

        userRepository = UserRepository.getInstance(this);
        userRepository.loadUserData(true);

        activityId = "login_" + System.currentTimeMillis();
        notificationManager = NotificationManager.getInstance();

        if (accessToken != null && !accessToken.isEmpty()) {
            notificationManager.reset();
            notificationManager.init(accessToken, uid);
        }
        connectToSignalR();

        startMainActivity();
    }

    private void saveAuthToken(String accessToken, String refreshToken, long expiresInSeconds) {
        preferencesHelper.saveAuthTokens(accessToken, refreshToken, expiresInSeconds);
    }

    private void loginAsGuest() {
        authManager.loginAsGuest();
        loadBoostrapData();

        userRepository = UserRepository.getInstance(this);
        userRepository.loadUserData(false);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationManager != null) {
            notificationManager.removeListener(activityId);
        }
    }
}

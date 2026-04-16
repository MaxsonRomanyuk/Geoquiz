package com.example.geoquiz_frontend.presentation.ui.Base;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.remote.NotificationManager;
import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.domain.entities.Achievement;
import com.example.geoquiz_frontend.presentation.ui.Game.GameResultActivity;
import com.example.geoquiz_frontend.presentation.utils.AchievementDialogHelper;
import com.example.geoquiz_frontend.presentation.utils.LocaleHelper;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;

import java.util.List;

public abstract class BaseActivity extends AppCompatActivity {
    private TextView tvConnectionStatus;
    private ImageView ivCloseConnectionStatus;
    private View connectionStatusBanner;
    private NotificationManager notificationManager;
    private UserRepository userRepository;
    protected PreferencesHelper preferencesHelper;

    private String currentConnectionKey;
    @Override
    protected void attachBaseContext(Context newBase) {
        preferencesHelper = new PreferencesHelper(newBase);
        String lang = preferencesHelper.getLanguage();

        Context context = LocaleHelper.setLocale(newBase, lang);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        if (preferencesHelper == null) {
            preferencesHelper = new PreferencesHelper(this);
        }
    }

    private void applyTheme() {
        PreferencesHelper prefs = new PreferencesHelper(this);
        String theme = prefs.getTheme();

        AppCompatDelegate.setDefaultNightMode(
                "dark".equals(theme)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        initConnectionStatusBanner();
    }

    private void initConnectionStatusBanner() {
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        ivCloseConnectionStatus = findViewById(R.id.ivCloseConnectionStatus);
        connectionStatusBanner = findViewById(R.id.connectionStatusBanner);

        if (ivCloseConnectionStatus != null) {
            ivCloseConnectionStatus.setOnClickListener(v -> showHideBannerOptions());
        }

        setupConnectionListener();
    }
    protected void setupConnectionListener() {
        notificationManager = NotificationManager.getInstance();
        currentConnectionKey = getClass().getSimpleName() + "_" + System.currentTimeMillis();
        if (!notificationManager.isConnected())
        {
            if (shouldShowConnectionBanner()) {
                showConnectionBanner(getLocalizedMessage(ConnectionErrorType.NO_INTERNET));
            }
        }

        notificationManager.addListener(currentConnectionKey, new NotificationManager.ConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> hideConnectionBanner());
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    if (shouldShowConnectionBanner()) {
                        showConnectionBanner(getLocalizedMessage(ConnectionErrorType.DISCONNECTED));
                    }
                });
            }

            @Override
            public void onAchievementUnlocked(List<ProfileResponse.AchievementDto> achievements) {
//                if (isFinishing() || isDestroyed()) {
//                    Log.d("NotificationManager", "Ignoring achievement - activity is finishing/destroyed");
//                    return;
//                }
                handleAchievementUnlocked(achievements);
            }

            @Override
            public void onConnectionFailed(String reason) {
                runOnUiThread(() -> {
                    if (shouldShowConnectionBanner()) {
                        ConnectionErrorType errorType = parseErrorType(reason);
                        showConnectionBanner(getLocalizedMessage(errorType));
                    }
                });
            }
        });
    }
    protected void handleAchievementUnlocked(List<ProfileResponse.AchievementDto> achievements) {
        Log.d("NotificationManager", "Achievement unlocked in BaseActivity - override this method to handle");
    }
    private void showConnectionBanner(String message) {
        if (connectionStatusBanner == null) return;

        connectionStatusBanner.setVisibility(View.VISIBLE);
        tvConnectionStatus.setText(message);

        connectionStatusBanner.setTranslationY(-connectionStatusBanner.getHeight());
        connectionStatusBanner.animate()
                .translationY(0)
                .setDuration(300)
                .start();
    }

    private void hideConnectionBanner() {
        if (connectionStatusBanner == null || connectionStatusBanner.getVisibility() != View.VISIBLE) {
            return;
        }

        connectionStatusBanner.animate()
                .translationY(-connectionStatusBanner.getHeight())
                .setDuration(300)
                .withEndAction(() -> connectionStatusBanner.setVisibility(View.GONE))
                .start();
    }

    private void showHideBannerOptions() {
        String language = preferencesHelper.getLanguage();
        boolean isRussian = "ru".equals(language);

        String[] options = isRussian ?
                new String[]{"Скрыть на сегодня", "Скрыть навсегда", "Отмена"} :
                new String[]{"Hide for today", "Hide forever", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle(isRussian ? "Скрыть уведомление" : "Hide notification")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            preferencesHelper.hideConnectionBannerTemporarily();
                            hideConnectionBanner();
                            Toast.makeText(this,
                                    isRussian ? "Уведомления скрыты на 24 часа" : "Hidden for 24 hours",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            preferencesHelper.hideConnectionBannerPermanently();
                            hideConnectionBanner();
                            Toast.makeText(this,
                                    isRussian ? "Уведомления скрыты навсегда" : "Hidden forever",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }}
                ).show();
    }
    private boolean shouldShowConnectionBanner() {
        if (preferencesHelper.isConnectionBannerPermanentlyHidden()) {
            return false;
        }

        return preferencesHelper.shouldShowBannerAfterCooldown();
    }
    private String getLocalizedMessage(ConnectionErrorType errorType) {
        String language = preferencesHelper.getLanguage();
        boolean isRussian = "ru".equals(language);

        switch (errorType) {
            case TIMEOUT:
                return isRussian ?
                        "⏱ Сервер не отвечает. Проверьте соединение." :
                        "⏱ Server timeout. Check your connection.";
            case CANNOT_CONNECT:
                return isRussian ?
                        "🔌 Не удалось подключиться к серверу." :
                        "🔌 Cannot connect to server.";
            case NO_INTERNET:
                return isRussian ?
                        "📡 Нет интернета. Работаем оффлайн." :
                        "📡 No internet. Working offline.";
            case DISCONNECTED:
                return isRussian ?
                        "🔴 Соединение потеряно." :
                        "🔴 Connection lost.";
            default:
                return isRussian ?
                        "⚠️ Проблемы с соединением." :
                        "⚠️ Connection issues.";
        }
    }
    private ConnectionErrorType parseErrorType(String errorMessage) {
        if (errorMessage == null) {
            return ConnectionErrorType.UNKNOWN;
        }

        if (errorMessage.contains("TIMEOUT")) {
            return ConnectionErrorType.TIMEOUT;
        } else if (errorMessage.contains("CANNOT_CONNECT")) {
            return ConnectionErrorType.CANNOT_CONNECT;
        } else if (errorMessage.contains("NO_INTERNET")) {
            return ConnectionErrorType.NO_INTERNET;
        } else if (errorMessage.contains("DISCONNECTED")) {
            return ConnectionErrorType.DISCONNECTED;
        }
        return ConnectionErrorType.UNKNOWN;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationManager != null && currentConnectionKey != null) {
            notificationManager.removeListener(currentConnectionKey);
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (notificationManager != null && currentConnectionKey != null) {
            notificationManager.removeListener(currentConnectionKey);
        }
    }
    protected enum ConnectionErrorType {
        TIMEOUT, CANNOT_CONNECT, NO_INTERNET, DISCONNECTED, UNKNOWN
    }
}
package com.example.geoquiz_frontend.presentation.ui.Game;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.example.geoquiz_frontend.presentation.ui.Auth.LoginActivity;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.presentation.ui.Home.MainActivity;
import com.example.geoquiz_frontend.presentation.ui.King.KingLobbyActivity;
import com.example.geoquiz_frontend.presentation.ui.Profile.ProfileActivity;
import com.example.geoquiz_frontend.presentation.ui.PvP.MatchmakingActivity;
import com.example.geoquiz_frontend.presentation.ui.achievements.AchievementsActivity;
import com.example.geoquiz_frontend.presentation.utils.AuthManager;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

public class GameTypesActivity extends BaseActivity {
    private MaterialButton btnSoloMode, btnPvPMode, btnKingMode;
    private View lockedOverlayPvP, lockedOverlayKing;
    private LinearLayout layoutLockedBadgePvP, layoutLockedBadgeKing;
    private BottomNavigationView bottomNavigationView;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_types);

        initViews();
        setupClickListeners();
        setupBottomNavigation();

        preferencesHelper = new SecurePreferencesHelper(this);
        authManager = new AuthManager(this);
        if (preferencesHelper.getUserId().equals("uid")) showLockedBadge();

    }

    private void initViews() {
        btnSoloMode = findViewById(R.id.btnSoloMode);
        btnPvPMode = findViewById(R.id.btnPvPMode);
        btnKingMode = findViewById(R.id.btnKingMode);

        lockedOverlayPvP = findViewById(R.id.vLockedOverlayPvP);
        lockedOverlayKing = findViewById(R.id.vLockedOverlayKing);
        layoutLockedBadgePvP = findViewById(R.id.layoutLockedBadgePvP);
        layoutLockedBadgeKing = findViewById(R.id.layoutLockedBadgeKing);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }

    private void setupClickListeners() {
        btnSoloMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, GameModesActivity.class);
            startActivity(intent);
        });

        btnPvPMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, MatchmakingActivity.class);
            startActivity(intent);
        });

        btnKingMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, KingLobbyActivity.class);
            startActivity(intent);
        });

        layoutLockedBadgePvP.setOnClickListener(v -> {
            showTransferConfirmation();
        });

        layoutLockedBadgeKing.setOnClickListener(v -> {
            showTransferConfirmation();
        });
    }

    private void showLockedBadge() {
        lockedOverlayPvP.setVisibility(View.VISIBLE);
        lockedOverlayKing.setVisibility(View.VISIBLE);
        layoutLockedBadgePvP.setVisibility(View.VISIBLE);
        layoutLockedBadgeKing.setVisibility(View.VISIBLE);

        btnKingMode.setEnabled(false);
        btnPvPMode.setEnabled(false);
    }
    private void showTransferConfirmation() {
        String currentLanguage = preferencesHelper.getLanguage();

        String title = "ru".equals(currentLanguage)
                ? "Перенести прогресс на новый аккаунт?"
                : "Transfer progress to a new account?";

        String message = "ru".equals(currentLanguage)
                ? "Вы сохраните все достижения и сможете продолжить игру позже.\n\n" +
                "Для этого потребуется интернет-соединение.\n" +
                "Вы выйдете из текущего режима и сможете зарегистрироваться."
                : "Your progress and achievements will be saved.\n\n" +
                "An internet connection is required.\n" +
                "You will exit the current mode and can create an account.";

        String positiveButton = "ru".equals(currentLanguage)
                ? "Сохранить"
                : "Save";

        String negativeButton = "ru".equals(currentLanguage)
                ? "Отмена"
                : "Cancel";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> openRegistration())
                .setNegativeButton(negativeButton, null)
                .show();
    }
    private void openRegistration() {
        Intent intent = new Intent(this, LoginActivity.class);
        UserStats stats = authManager.getCurrentStats();

        if (stats != null) {
            Gson gson = new Gson();
            String statsJson = gson.toJson(stats);
            intent.putExtra("USER_STATS_JSON", statsJson);
        }

        authManager.logout();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_play) {
                return true;
            } else if (itemId == R.id.nav_achievements) {
                Intent intent = new Intent(this, AchievementsActivity.class);
                startActivity(intent);
            } else if (itemId == R.id.nav_leaderboard) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_play);
    }
}

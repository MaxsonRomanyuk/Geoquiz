package com.example.geoquiz_frontend.presentation.ui.Home;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geoquiz_frontend.domain.entities.Achievement;
import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.example.geoquiz_frontend.domain.enums.LocalizedText;
import com.example.geoquiz_frontend.presentation.ui.King.KingLobbyActivity;
import com.example.geoquiz_frontend.data.remote.ApiClient;
import com.example.geoquiz_frontend.data.remote.ApiService;
import com.example.geoquiz_frontend.presentation.ui.achievements.AchievementsActivity;
import com.example.geoquiz_frontend.presentation.utils.AchievementDialogHelper;
import com.example.geoquiz_frontend.presentation.utils.AuthManager;
import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.presentation.ui.Auth.LoginActivity;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.presentation.ui.Game.GameModesActivity;
import com.example.geoquiz_frontend.presentation.ui.Game.GameTypesActivity;
import com.example.geoquiz_frontend.presentation.ui.Profile.ProfileActivity;
import com.example.geoquiz_frontend.presentation.ui.PvP.MatchmakingActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private TextView tvUsername, tvLevel, tvXP, tvTotalScore;
    private TextView tvDailyStreak;
    private TextView tvGamesPlayed, tvWins, tvAccuracy, tvBestContinent;
    private MaterialCardView cardSolo, cardDuel, cardKing, cardLevel;
    private View lockedOverlayPvP, lockedOverlayKing;
    private LinearLayout layoutLockedBadgePvP, layoutLockedBadgeKing;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressXP;

    private AuthManager authManager;
    private PreferencesHelper preferencesHelper;
    private UserRepository userRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        preferencesHelper = new PreferencesHelper(this);

        boolean isGuest = preferencesHelper.getUserId().equals("uid");
        if (!preferencesHelper.hasValidToken() && !isGuest) {
            handleUnauthorized();
            //redirectToLogin();
            return;
        }

        initViews();
        if (isGuest)
        {
            showLockedBadge();
        }
        setupClickListeners();
        setupBottomNavigation();
        observeUserData();

        //userRepository.loadUserData(false);
    }

    private void initViews() {
        tvUsername = findViewById(R.id.tvUsername);
        tvDailyStreak = findViewById(R.id.tvDailyStreak);
        cardLevel = findViewById(R.id.cardLevel);

        tvLevel = findViewById(R.id.tvLevel);
        tvXP = findViewById(R.id.tvXP);
        tvTotalScore = findViewById(R.id.tvTotalScore);
        progressXP = findViewById(R.id.progressXP);


        cardSolo = findViewById(R.id.cardSolo);
        cardDuel = findViewById(R.id.cardDuel);
        cardKing = findViewById(R.id.cardKing);

        lockedOverlayPvP = findViewById(R.id.vLockedOverlayPvP);
        lockedOverlayKing = findViewById(R.id.vLockedOverlayKing);
        layoutLockedBadgePvP = findViewById(R.id.layoutLockedBadgePvP);
        layoutLockedBadgeKing = findViewById(R.id.layoutLockedBadgeKing);

        tvGamesPlayed = findViewById(R.id.tvGamesPlayed);
        tvWins = findViewById(R.id.tvWins);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        tvBestContinent = findViewById(R.id.tvBestContinent);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }
    private void setupClickListeners() {
        cardLevel.setOnClickListener(v -> {
//            showLogoutConfirmation(); //temporarily
        });

        cardSolo.setOnClickListener(v -> {
            Intent intent = new Intent(this, GameModesActivity.class);
            startActivity(intent);
        });

        cardDuel.setOnClickListener(v -> {
            Intent intent = new Intent(this, MatchmakingActivity.class);
            startActivity(intent);
        });

        cardKing.setOnClickListener(v -> {
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

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_play) {
                Intent intent = new Intent(this, GameTypesActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_achievements) {
                Intent intent = new Intent(this, AchievementsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                // Navigate to leaderboard
                return true;
            } else if (itemId == R.id.nav_profile){
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }
    private void showLockedBadge() {
        lockedOverlayPvP.setVisibility(View.VISIBLE);
        lockedOverlayKing.setVisibility(View.VISIBLE);
        layoutLockedBadgePvP.setVisibility(View.VISIBLE);
        layoutLockedBadgeKing.setVisibility(View.VISIBLE);

        cardDuel.setAlpha(0.75F);
        cardKing.setAlpha(0.75F);

        cardDuel.setEnabled(false);
        cardKing.setEnabled(false);
    }
    private void observeUserData() {
        userRepository = UserRepository.getInstance(this);
        userRepository.getUserData().observe(this, profileData -> {
            if (profileData != null) {
                updateUI(profileData);
            }
        });

        userRepository.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                showLoadingState();
            }
        });

        userRepository.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
        userRepository.getAchievements().observe(this, achievements -> {
            if (achievements != null) {
            }
        });
    }

    private void updateUI(ProfileResponse profileData) {
        String language = preferencesHelper.getLanguage();

        if (profileData == null) {
            Log.e("MainActivity", "profileData is null");
            return;
        }

        Log.d("MainActivity", "Updating UI with: " +
                "user=" + (profileData.getUser() != null) +
                ", stats=" + (profileData.getStats() != null) +
                ", geography=" + (profileData.getGeography() != null));

        ProfileResponse.UserDto user = profileData.getUser();
        ProfileResponse.StatsDto stats = profileData.getStats();
        ProfileResponse.GeographyDto geography = profileData.getGeography();

        if (user != null) {
            tvUsername.setText(user.getUserName());
        } else {
            tvUsername.setText("Гость");
        }

        if (stats != null) {
            tvDailyStreak.setText("🔥 " + stats.getDailyStreak());
            tvLevel.setText(getString(R.string.level_prefix) + " " + stats.getLevel());

            int currentXP = stats.getExperience();
            int nextLevelXP = calculateNextLevelXP(stats.getLevel());
            tvXP.setText(String.format("%d/%d XP", currentXP, nextLevelXP));

            int progressPercent = (int) ((float) currentXP / nextLevelXP * 100);
            progressXP.setProgress(progressPercent);

            //int totalScore = calculateTotalScore(stats.getLevel(), currentXP);
            int totalScore = stats.getScore();
            tvTotalScore.setText(String.format("%,d", totalScore));

            tvGamesPlayed.setText(String.valueOf(stats.getGamesPlayed()));
            tvWins.setText(String.valueOf(stats.getWins()));
            tvAccuracy.setText(String.format("%.0f%%", stats.getWinRate()));
        } else {
            tvDailyStreak.setText("🔥 0");
            tvLevel.setText("Уровень 1");
            tvXP.setText("0/100 XP");
            progressXP.setProgress(0);
            tvTotalScore.setText("0");
            tvGamesPlayed.setText("0");
            tvWins.setText("0");
            tvAccuracy.setText("0%");
        }

        if (geography != null) {
            String bestContinent = "";
            switch (geography.getBestContinent())
            {
                case "europe":
                    bestContinent = language.equals("ru") ? "Европа" : "Europe";
                    break;
                case "asia":
                    bestContinent = language.equals("ru") ? "Азия" : "Asia";
                    break;
                case "africa":
                    bestContinent = language.equals("ru") ? "Африка" : "Africa";
                    break;
                case "america":
                    bestContinent = language.equals("ru") ? "Америка" : "America";
                    break;
                case "oceania":
                    bestContinent = language.equals("ru") ? "Океания" : "Oceania";
                    break;
                default:
                    bestContinent = language.equals("ru") ? "Европа" : "Europe";
                    break;
            }
            tvBestContinent.setText(bestContinent);
        } else {
            tvBestContinent.setText("—");
        }
    }
    private int calculateNextLevelXP(int currentLevel) {
        return currentLevel * 100;
    }
    private static int calculateTotalScore(int level, int experience)
    {
        return (100 * (level - 1) * level) / 2 + experience;
    }
    private void showLoadingState() {
        tvUsername.setText("Loading...");
        tvLevel.setText("Level 1");
        tvTotalScore.setText(String.valueOf(0));
        tvXP.setText(String.format("%d/%d XP", 0, 100));
        progressXP.setProgress((int) ((float) 1 / 100));
        tvGamesPlayed.setText("—");
        tvWins.setText("—");
        tvAccuracy.setText("—%");
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
    private void handleUnauthorized() {
        //preferencesHelper.clearCurrentUser();
        Toast.makeText(this, "Сессия истекла. Войдите снова.", Toast.LENGTH_LONG).show();
        redirectToLogin();
    }
    private void redirectToLogin() {
        authManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    protected void onResume() {
        super.onResume();
//        if (preferencesHelper != null && preferencesHelper.hasValidToken() && apiService != null) {
//            loadUserData();
//        }
    }
}
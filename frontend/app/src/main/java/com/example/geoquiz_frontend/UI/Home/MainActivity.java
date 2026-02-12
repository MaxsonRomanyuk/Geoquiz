package com.example.geoquiz_frontend.UI.Home;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.geoquiz_frontend.ApiClient;
import com.example.geoquiz_frontend.ApiService;
import com.example.geoquiz_frontend.AuthManager;
import com.example.geoquiz_frontend.DTOs.ProfileResponse;
import com.example.geoquiz_frontend.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.UI.Auth.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvUsername, tvLevel, tvXP, tvTotalScore;
    private TextView tvDailyStreak;
    private TextView tvGamesPlayed, tvWins, tvAccuracy, tvBestContinent;
    private CardView cardSolo, cardDuel, cardKing, cardLevel;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressXP;

    private AuthManager authManager;
    private PreferencesHelper preferencesHelper;
    private ApiService apiService;
    private ProfileResponse profileData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        initViews();

        preferencesHelper = new PreferencesHelper(this);
        if (!preferencesHelper.hasValidToken()) {
            redirectToLogin();
            return;
        }

        apiService = ApiClient.getApiWithAuth(preferencesHelper);

        setupClickListeners();
        setupBottomNavigation();
        loadUserData();
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


        tvGamesPlayed = findViewById(R.id.tvGamesPlayed);
        tvWins = findViewById(R.id.tvWins);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        tvBestContinent = findViewById(R.id.tvBestContinent);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }
    private void initData() {
        preferencesHelper = new PreferencesHelper(this);

        if (!preferencesHelper.hasValidToken()) {
            redirectToLogin();
            return;
        }

        apiService = ApiClient.getApiWithAuth(preferencesHelper);
    }
    private void setupClickListeners() {
        cardLevel.setOnClickListener(v -> {
            showLogoutConfirmation(); //temporarily
        });

        cardSolo.setOnClickListener(v -> {
            // Navigate to solo mode
        });

        cardDuel.setOnClickListener(v -> {
            // Navigate to duel mode
        });

        cardKing.setOnClickListener(v -> {
            // Navigate to king of the hill
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_play) {
                // Navigate to play screen
                return true;
            } else if (itemId == R.id.nav_achievements) {
                // Navigate to achievements
                return true;
            } else // Navigate to profile
                if (itemId == R.id.nav_leaderboard) {
                // Navigate to leaderboard
                return true;
            } else return itemId == R.id.nav_profile;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void loadUserData() {
        showLoadingState();

        Call<ProfileResponse> call = apiService.getProfile();
        call.enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    profileData = response.body();
                    updateUI();
                } else {
                    Log.e("MainActivity", "Profile load failed: " + response.code());
                    redirectToLogin();
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                Log.e("MainActivity", "Network error", t);
//                redirectToLogin();
            }
        });
    }
    private void updateUI() {
        if (profileData == null) return;

        ProfileResponse.UserDto user = profileData.getUser();
        ProfileResponse.StatsDto stats = profileData.getStats();
        ProfileResponse.GeographyDto geography = profileData.getGeography();

        tvUsername.setText(user.getUserName());
        tvDailyStreak.setText("🔥 " + stats.getDailyStreak());

        tvLevel.setText(getString(R.string.level_prefix) + " " + stats.getLevel());

        int currentXP = stats.getExperience();
        int nextLevelXP = calculateNextLevelXP(stats.getLevel());
        tvXP.setText(String.format("%d/%d XP", currentXP, nextLevelXP));

        int progressPercent = (int) ((float) currentXP / nextLevelXP * 100);
        progressXP.setProgress(progressPercent);

        int totalScore = calculateTotalScore(stats.getLevel(), currentXP);
        tvTotalScore.setText(String.format("%,d", totalScore));

        tvGamesPlayed.setText(String.valueOf(stats.getGamesPlayed()));
        tvWins.setText(String.valueOf(stats.getWins()));
        tvAccuracy.setText(String.format("%.0f%%", stats.getWinRate()));
        tvBestContinent.setText(geography.getBestContinent());
    }
    private int calculateNextLevelXP(int currentLevel) {
        return currentLevel * 100;
    }
    private static int calculateTotalScore(int level, int experience)
    {
        return (100 * (level - 1) * level) / 2 + experience;
    }
    private void showLoadingState() {
        tvUsername.setText("Загрузка...");
        tvGamesPlayed.setText("—");
        tvWins.setText("—");
        tvAccuracy.setText("—%");
    }
    private void handleUnauthorized() {
        preferencesHelper.clearCurrentUser();
        Toast.makeText(this, "Сессия истекла. Войдите снова.", Toast.LENGTH_LONG).show();
        redirectToLogin();
    }
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        tvUsername.setText("Ошибка");
        tvGamesPlayed.setText("0");
        tvWins.setText("0");
        tvAccuracy.setText("0%");
        tvBestContinent.setText("—");
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
    @Override
    protected void onResume() {
        super.onResume();
//        if (preferencesHelper != null && preferencesHelper.hasValidToken() && apiService != null) {
//            loadUserData();
//        }
    }
}
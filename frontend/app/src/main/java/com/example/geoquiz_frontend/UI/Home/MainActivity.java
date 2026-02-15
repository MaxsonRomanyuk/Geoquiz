package com.example.geoquiz_frontend.UI.Home;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.example.geoquiz_frontend.ApiClient;
import com.example.geoquiz_frontend.ApiService;
import com.example.geoquiz_frontend.AuthManager;
import com.example.geoquiz_frontend.DTOs.ProfileResponse;
import com.example.geoquiz_frontend.Data.UserRepository;
import com.example.geoquiz_frontend.LocaleHelper;
import com.example.geoquiz_frontend.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.UI.Auth.LoginActivity;
import com.example.geoquiz_frontend.UI.Profile.ProfileActivity;
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
    private UserRepository userRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        initViews();
        preferencesHelper = new PreferencesHelper(this);
        if (!preferencesHelper.hasValidToken()) {
            handleUnauthorized();
            //redirectToLogin();
            return;
        }

        userRepository = UserRepository.getInstance(this);
        apiService = ApiClient.getApiWithAuth(preferencesHelper);

        setupClickListeners();
        setupBottomNavigation();
        observeUserData();

        userRepository.loadUserData(false);
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
//            showLogoutConfirmation(); //temporarily
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
            } else if (itemId == R.id.nav_leaderboard) {
                // Navigate to leaderboard
                return true;
            } else if (itemId == R.id.nav_profile){
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }
    private void observeUserData() {
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
    }

    private void savePreferencesStats() {
        if (profileData == null) return;
        preferencesHelper.setCurrentUserStats(profileData.getUser(), profileData.getStats(), profileData.getGeography());
    }
    private void updateUI(ProfileResponse profileData) {
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

            int totalScore = calculateTotalScore(stats.getLevel(), currentXP);
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
            tvBestContinent.setText(geography.getBestContinent());
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
        preferencesHelper.clearCurrentUser();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void applyLanguage() {
        preferencesHelper = new PreferencesHelper(this);
        String language = preferencesHelper.getLanguage();
        LocaleHelper.setLocale(this, language);

    }
    private void applyTheme() {
        String theme = preferencesHelper.getTheme();
        if ("dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
//        if (preferencesHelper != null && preferencesHelper.hasValidToken() && apiService != null) {
//            loadUserData();
//        }
    }
}
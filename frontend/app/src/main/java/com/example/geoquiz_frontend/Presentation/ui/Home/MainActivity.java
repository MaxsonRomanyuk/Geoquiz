package com.example.geoquiz_frontend.Presentation.ui.Home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geoquiz_frontend.data.remote.ApiClient;
import com.example.geoquiz_frontend.data.remote.ApiService;
import com.example.geoquiz_frontend.Presentation.utils.AuthManager;
import com.example.geoquiz_frontend.data.remote.dtos.ProfileResponse;
import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.Presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.Presentation.ui.Auth.LoginActivity;
import com.example.geoquiz_frontend.Presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.Presentation.ui.Game.GameModesActivity;
import com.example.geoquiz_frontend.Presentation.ui.Game.GameTypesActivity;
import com.example.geoquiz_frontend.Presentation.ui.Profile.ProfileActivity;
import com.example.geoquiz_frontend.Presentation.ui.PvP.MatchmakingActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends BaseActivity {

    private TextView tvUsername, tvLevel, tvXP, tvTotalScore;
    private TextView tvDailyStreak;
    private TextView tvGamesPlayed, tvWins, tvAccuracy, tvBestContinent;
    private MaterialCardView cardSolo, cardDuel, cardKing, cardLevel;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressXP;

    private AuthManager authManager;
    private PreferencesHelper preferencesHelper;
    private ApiService apiService;
    private ProfileResponse profileData;
    private UserRepository userRepository;
    private DatabaseHelper databaseHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        initViews();
        preferencesHelper = new PreferencesHelper(this);
        databaseHelper = new DatabaseHelper(this);
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

        userRepository.loadUserData(false);//ошибка для гостя
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
            Intent intent = new Intent(this, GameModesActivity.class);
            startActivity(intent);
        });

        cardDuel.setOnClickListener(v -> {
            Intent intent = new Intent(this, MatchmakingActivity.class);
            startActivity(intent);
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
                Intent intent = new Intent(this, GameTypesActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_achievements) {
                // Navigate to achievements
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
        tvUsername.setText("Loading...");
        tvLevel.setText("Level 1");
        tvTotalScore.setText(String.valueOf(0));
        tvXP.setText(String.format("%d/%d XP", 0, 100));
        progressXP.setProgress((int) ((float) 1 / 100));
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
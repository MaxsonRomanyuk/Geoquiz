package com.example.geoquiz_frontend.UI.Profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.geoquiz_frontend.ApiClient;
import com.example.geoquiz_frontend.AuthManager;
import com.example.geoquiz_frontend.DTOs.ProfileResponse;
import com.example.geoquiz_frontend.Data.UserRepository;
import com.example.geoquiz_frontend.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.UI.Base.BaseActivity;
import com.example.geoquiz_frontend.UI.Game.GameTypesActivity;
import com.example.geoquiz_frontend.UI.Home.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class ProfileActivity extends BaseActivity {
    private ImageView ivAvatar;
    private TextView tvUserName, tvLevel, tvXP;
    private ProgressBar progressXP;
    private ImageView ivSettings;
    private TextView tvTotalPoints, tvGamesPlayed, tvWins, tvWinRate, tvCurrentStreak;

    private TextView tvAfricaCorrect, tvAsiaCorrect, tvEuropeCorrect, tvAmericaCorrect, tvOceaniaCorrect;
    private MaterialCardView btnMatchHistory;
    private MaterialButton btnGetPlus;
    private BottomNavigationView bottomNavigationView;
    private UserRepository userRepository;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userRepository = UserRepository.getInstance(this);
        initViews();
        setupClickListeners();
        setupBottomNavigation();

        observeUserData();
    }
    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvLevel = findViewById(R.id.tvLevel);
        tvXP = findViewById(R.id.tvXP);
        progressXP = findViewById(R.id.progressXP);
        ivSettings = findViewById(R.id.ivSettings);

        tvTotalPoints = findViewById(R.id.tvTotalPoints);

        tvGamesPlayed = findViewById(R.id.tvGamesPlayed);
        tvWins = findViewById(R.id.tvWins);
        tvWinRate = findViewById(R.id.tvWinRate);
        tvCurrentStreak = findViewById(R.id.tvLongestStreak);

        tvAfricaCorrect = findViewById(R.id.tvAfricaCorrect);
        tvAsiaCorrect = findViewById(R.id.tvAsiaCorrect);
        tvEuropeCorrect = findViewById(R.id.tvEuropeCorrect);
        tvAmericaCorrect = findViewById(R.id.tvAmericaCorrect);
        tvOceaniaCorrect = findViewById(R.id.tvOceaniaCorrect);

        btnMatchHistory = findViewById(R.id.btnMatchHistory);
        btnGetPlus = findViewById(R.id.btnGetPlus);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }
    private void setupClickListeners() {
        ivSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        btnMatchHistory.setOnClickListener(v -> {
            // startActivity(new Intent(this, MatchHistoryActivity.class));
        });

        btnGetPlus.setOnClickListener(v -> {
            // startActivity(new Intent(this, PremiumActivity.class));
        });
    }
    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
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
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }
    private void observeUserData() {
        userRepository.getUserData().observe(this, profileData -> {
            if (profileData != null) {
                updateUI(profileData);
            } else {
                showLoadingState();
            }
        });

        userRepository.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                showLoadingState();
            }
        });
    }
    private void updateUI(ProfileResponse data) {
        if (data == null) return;

        ProfileResponse.UserDto user = data.getUser();
        ProfileResponse.StatsDto stats = data.getStats();
        ProfileResponse.GeographyDto geography = data.getGeography();

        if (user != null) {
            tvUserName.setText(user.getUserName());
        }

        if (stats != null) {
            tvLevel.setText(String.valueOf(stats.getLevel()));
            int currentXP = stats.getExperience();
            int nextLevelXP = calculateNextLevelXP(stats.getLevel());
            tvXP.setText(String.format("%d/%d XP", currentXP, nextLevelXP));
            progressXP.setProgress((int) ((float) currentXP / nextLevelXP * 100));

            tvTotalPoints.setText(String.format("%,d", calculateTotalScore(stats.getLevel(), currentXP)));
            tvGamesPlayed.setText(String.valueOf(stats.getGamesPlayed()));
            tvWins.setText(String.valueOf(stats.getWins()));
            tvWinRate.setText(String.format("%.0f%%", stats.getWinRate()));
            tvCurrentStreak.setText(String.valueOf(stats.getCurrentWinStreak()));
        }

        if (geography != null) {
            tvAfricaCorrect.setText(String.valueOf(geography.getAfricaCorrect()));
            tvAsiaCorrect.setText(String.valueOf(geography.getAsiaCorrect()));
            tvEuropeCorrect.setText(String.valueOf(geography.getEuropeCorrect()));
            tvAmericaCorrect.setText(String.valueOf(geography.getAmericaCorrect()));
            tvOceaniaCorrect.setText(String.valueOf(geography.getOceaniaCorrect()));
        }
    }
    private void showLoadingState() {
        tvUserName.setText("Loading...");
        tvLevel.setText(String.valueOf(1));
        tvXP.setText(String.format("%d/%d XP", 0, 100));
        progressXP.setProgress((int) ((float) 1 / 100));
        tvTotalPoints.setText(String.valueOf(0));
        tvGamesPlayed.setText("—");
        tvWins.setText("—");
        tvWinRate.setText("—%");
        tvCurrentStreak.setText(0);
        tvAfricaCorrect.setText(String.valueOf(0));
        tvAsiaCorrect.setText(String.valueOf(0));
        tvEuropeCorrect.setText(String.valueOf(0));
        tvAmericaCorrect.setText(String.valueOf(0));
        tvOceaniaCorrect.setText(String.valueOf(0));
    }
    private int calculateNextLevelXP(int currentLevel) {
        return currentLevel * 100;
    }
    private static int calculateTotalScore(int level, int experience)
    {
        return (100 * (level - 1) * level) / 2 + experience;
    }
}

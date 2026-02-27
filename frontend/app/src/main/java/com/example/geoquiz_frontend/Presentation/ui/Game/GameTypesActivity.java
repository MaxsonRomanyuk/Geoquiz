package com.example.geoquiz_frontend.Presentation.ui.Game;

import android.content.Intent;
import android.os.Bundle;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.Presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.Presentation.ui.Home.MainActivity;
import com.example.geoquiz_frontend.Presentation.ui.Profile.ProfileActivity;
import com.example.geoquiz_frontend.Presentation.ui.PvP.MatchmakingActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class GameTypesActivity extends BaseActivity {
    private MaterialButton btnSoloMode, btnPvPMode, btnKingMode;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_types);

        initViews();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        btnSoloMode = findViewById(R.id.btnSoloMode);
        btnPvPMode = findViewById(R.id.btnPvPMode);
        btnKingMode = findViewById(R.id.btnKingMode);

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
        });
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
                return true;
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

package com.example.geoquiz_frontend.UI.Game;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.UI.Base.BaseActivity;
import com.example.geoquiz_frontend.UI.Home.MainActivity;
import com.example.geoquiz_frontend.UI.Profile.ProfileActivity;
import com.example.geoquiz_frontend.UI.Profile.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class GameModesActivity extends BaseActivity {

    private MaterialCardView cardCapitals, cardFlags, cardOutlines, cardLanguages;
    private MaterialCardView btnCapitals, btnFlags, btnOutlines, btnLanguages;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_modes);

        initViews();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initViews() {
        cardCapitals = findViewById(R.id.cardCapitals);
        cardFlags = findViewById(R.id.cardFlags);
        cardOutlines = findViewById(R.id.cardOutlines);
        cardLanguages = findViewById(R.id.cardLanguages);

        btnCapitals = findViewById(R.id.btnCapitals);
        btnFlags = findViewById(R.id.btnFlags);
        btnOutlines = findViewById(R.id.btnOutlines);
        btnLanguages = findViewById(R.id.btnLanguages);


        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }

    private void setupClickListeners() {
        cardCapitals.setOnClickListener(v -> {
            //
        });

        btnCapitals.setOnClickListener(v -> {
            //
        });
        cardFlags.setOnClickListener(v -> {
            //
        });

        btnFlags.setOnClickListener(v -> {
            //
        });

        cardOutlines.setOnClickListener(v -> {
            //
        });

        btnOutlines.setOnClickListener(v -> {
            //
        });

        cardLanguages.setOnClickListener(v -> {
            //
        });

        btnLanguages.setOnClickListener(v -> {
            //
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

package com.example.geoquiz_frontend.UI.PvP;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.geoquiz_frontend.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.UI.Base.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;
import java.util.Random;

public class MatchmakingActivity extends BaseActivity {

    private ImageView ivClose;
    private TextView tvSearchStatus, tvSearchTime;
    private TextView tvCurrentPlayerName, tvCurrentPlayerRating, tvCurrentPlayerLevel;
    private MaterialCardView cardOpponent;
    private TextView tvOpponentName, tvOpponentRating, tvOpponentLevel;
    private TextView tvVS;
    private MaterialButton btnCancel;
    private ProgressBar progressSearch;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private int seconds = 0;
    private boolean isSearching = true;
    private boolean opponentFound = false;

    private PreferencesHelper preferencesHelper;
    private Random random = new Random();

    private String[] opponentNames = {"PlayerXYZ", "GeoMaster", "MapLegend", "QuizKing", "GlobeTrotter"};
    private int[] opponentLevels = {5, 7, 8, 9, 10, 12};
    private int[] opponentScore = {950, 1100, 1250, 1300, 1450, 1600};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking);

        preferencesHelper = new PreferencesHelper(this);

        initViews();
        setupClickListeners();
        loadCurrentPlayerData();
        startSearchTimer();
    }

    private void initViews() {
        ivClose = findViewById(R.id.ivClose);
        progressSearch = findViewById(R.id.progressSearch);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        tvSearchTime = findViewById(R.id.tvSearchTime);

        tvCurrentPlayerName = findViewById(R.id.tvCurrentPlayerName);
        tvCurrentPlayerRating = findViewById(R.id.tvCurrentPlayerRating);
        tvCurrentPlayerLevel = findViewById(R.id.tvCurrentPlayerLevel);

        cardOpponent = findViewById(R.id.cardOpponent);
        tvOpponentName = findViewById(R.id.tvOpponentName);
        tvOpponentRating = findViewById(R.id.tvOpponentRating);
        tvOpponentLevel = findViewById(R.id.tvOpponentLevel);

        tvVS = findViewById(R.id.tvVS);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupClickListeners() {
        ivClose.setOnClickListener(v -> {
            cancelSearch();
            finish();
        });

        btnCancel.setOnClickListener(v -> {
            cancelSearch();
            finish();
        });
    }

    private void loadCurrentPlayerData() {
        tvCurrentPlayerName.setText("Traveler42");
        tvCurrentPlayerRating.setText(getString(R.string.score_format, 1250));
        tvCurrentPlayerLevel.setText("Lvl 8");
    }

    private void startSearchTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                seconds++;
                updateTimerDisplay();

                if (isSearching && !opponentFound) {
                    if (seconds > 3 && random.nextInt(10) > 7) {
                        findOpponent();
                    }
                }

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateTimerDisplay() {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
        tvSearchTime.setText(timeFormatted);
    }

    private void findOpponent() {
        opponentFound = true;
        isSearching = false;

        String name = opponentNames[random.nextInt(opponentNames.length)];
        int level = opponentLevels[random.nextInt(opponentLevels.length)];
        int score = opponentScore[random.nextInt(opponentScore.length)];

        tvOpponentName.setText(name);
        tvOpponentRating.setText(getString(R.string.score_format, score));
        tvOpponentLevel.setText("Lvl " + level);

        cardOpponent.setVisibility(View.VISIBLE);

        cardOpponent.setAlpha(0f);
        cardOpponent.animate()
                .alpha(1f)
                .setDuration(500)
                .start();

        tvSearchStatus.setText(R.string.opponent_found);
        progressSearch.setVisibility(View.GONE);

        new Handler().postDelayed(() -> {
            tvVS.setVisibility(View.VISIBLE);
            tvVS.setAlpha(0f);
            tvVS.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .withEndAction(() -> {
                        navigateToDraftMode(name, level, score);
                    })
                    .start();
        }, 1000);
    }

    private void navigateToDraftMode(String opponentName, int opponentLevel, int opponentRating) {
        Intent intent = new Intent(this, DraftModeActivity.class);
        startActivity(intent);
    }

    private void cancelSearch() {
        isSearching = false;
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelSearch();
    }
}

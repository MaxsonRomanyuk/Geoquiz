package com.example.geoquiz_frontend.Presentation.ui.PvP;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.geoquiz_frontend.Presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.Presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.data.remote.SignalRClient;
import com.example.geoquiz_frontend.data.remote.dtos.MatchFoundData;
import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;
import java.util.Random;

public class MatchmakingActivity extends BaseActivity {

    private ImageView ivClose;
    private TextView tvSearchStatus, tvSearchTime;
    private TextView tvCurrentPlayerName, tvCurrentPlayerScore, tvCurrentPlayerLevel;
    private MaterialCardView cardOpponent;
    private TextView tvOpponentName, tvOpponentScore, tvOpponentLevel;
    private TextView tvVS;
    private MaterialButton btnCancel;
    private ProgressBar progressSearch;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private int seconds = 0;



    private SignalRClient signalRClient;
    private PreferencesHelper preferencesHelper;
    private DatabaseHelper databaseHelper;
    private boolean isSearching = false;
    private MatchFoundData matchData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking);

        preferencesHelper = new PreferencesHelper(this);
        databaseHelper = new DatabaseHelper(this);
        initViews();
        setupClickListeners();
        loadCurrentPlayerData();

        connectToSignalR();
    }

    private void initViews() {
        ivClose = findViewById(R.id.ivClose);
        progressSearch = findViewById(R.id.progressSearch);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        tvSearchTime = findViewById(R.id.tvSearchTime);

        tvCurrentPlayerName = findViewById(R.id.tvCurrentPlayerName);
        tvCurrentPlayerScore = findViewById(R.id.tvCurrentPlayerScore);
        tvCurrentPlayerLevel = findViewById(R.id.tvCurrentPlayerLevel);

        cardOpponent = findViewById(R.id.cardOpponent);
        tvOpponentName = findViewById(R.id.tvOpponentName);
        tvOpponentScore = findViewById(R.id.tvOpponentScore);
        tvOpponentLevel = findViewById(R.id.tvOpponentLevel);

        tvVS = findViewById(R.id.tvVS);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupClickListeners() {
        ivClose.setOnClickListener(v -> {
            cancelSearchAndExit();
        });

        btnCancel.setOnClickListener(v -> {
            cancelSearchAndExit();
        });
    }

    private void loadCurrentPlayerData() {
        String username = preferencesHelper.getUserName();
        UserStats userStats = databaseHelper.getUserStats(preferencesHelper.getUserId());
        int level = userStats.getLevel();
        int score = level*100 + userStats.getExperience();

        tvCurrentPlayerName.setText(username != null ? username : "Guest");
        tvCurrentPlayerScore.setText(getString(R.string.score_format, Math.max(score, 0)));
        tvCurrentPlayerLevel.setText("Lvl " + (level > 0 ? level : 1));
    }
    private void connectToSignalR() {
        String token = preferencesHelper.getAuthToken();
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "No auth token found");
            finish();
            return;
        }

        showStatus("Connected...");

        signalRClient = new SignalRClient(token, new SignalRClient.ConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Connected to SignalR, joining queue");
                    showStatus("Search opponent...");
                    startSearch();
                    signalRClient.joinQueue();
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Disconnected from SignalR");
                    if (isSearching) {
                        showStatus("Connection lost");
                        new Handler().postDelayed(() -> connectToSignalR(), 3000);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "SignalR error: " + error);
                    showStatus("Error: " + error);
                });
            }

            @Override
            public void onMatchFound(MatchFoundData data) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Match found! Match ID: " + data.getMatchId());
                    matchData = data;
                    showOpponentFound(data);
                });
            }
        });

        signalRClient.start();
    }
    private void startSearch() {
        isSearching = true;

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isSearching) {
                    seconds++;
                    updateTimerDisplay();
                    timerHandler.postDelayed(this, 1000);
                }
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
    private void showStatus(String status) {
        tvSearchStatus.setText(status);
    }
    private void showOpponentFound(MatchFoundData data) {
        isSearching = false;
        progressSearch.setVisibility(View.GONE);
        showStatus("Opponent found!");

        tvOpponentName.setText(data.getOpponentName());
        tvOpponentScore.setText(getString(R.string.score_format, calculateRatingFromLevel(data.getOpponentLevel())));
        tvOpponentLevel.setText("Lvl " + data.getOpponentLevel());

        cardOpponent.setVisibility(View.VISIBLE);
        cardOpponent.setAlpha(0f);
        cardOpponent.animate()
                .alpha(1f)
                .setDuration(500)
                .start();

        new Handler().postDelayed(() -> {
            tvVS.setVisibility(View.VISIBLE);
            tvVS.setAlpha(0f);
            tvVS.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .withEndAction(() -> {
                        navigateToDraftMode(data);
                    })
                    .start();
        }, 1000);
    }

    private int calculateRatingFromLevel(int level) {
        return level * 100 + 67; // temporary
    }

    private void navigateToDraftMode(MatchFoundData data) {
        Intent intent = new Intent(this, DraftModeActivity.class);
        intent.putExtra("matchId", data.getMatchId());
        intent.putExtra("opponentName", data.getOpponentName());
        intent.putExtra("opponentLevel", data.getOpponentLevel());
        intent.putExtra("availableModes", data.getAvailableModes().toArray(new String[0]));
        intent.putExtra("currentTurnUserId", data.getCurrentTurnUserId());
        intent.putExtra("yourId", data.getYourId());
        intent.putExtra("timePerTurn", data.getTimePerTurnSeconds());
        startActivity(intent);
        finish();
    }
    private void cancelSearchAndExit() {
        cancelSearch();
        if (signalRClient != null && signalRClient.isConnected()) {
            signalRClient.leaveQueue();
            signalRClient.stop();
        }
        finish();
    }
    private void cancelSearch() {
        isSearching = false;
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelSearch();
        if (signalRClient != null) {
            signalRClient.stop();
        }
    }
}

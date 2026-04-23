package com.example.geoquiz_frontend.presentation.ui.PvP;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.geoquiz_frontend.data.remote.dtos.pvp.SubmitAnswerResponse;
import com.example.geoquiz_frontend.presentation.utils.GameTokenManager;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.data.remote.PvPSignalRClientManager;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.DisconnectData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.DraftUpdateData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.GameFinishedData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.GameReadyData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.MatchFoundData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.TimerUpdateData;
import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.example.geoquiz_frontend.presentation.utils.TokenRefreshHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

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

    private PvPSignalRClientManager signalRManager;
    private GameTokenManager gameTokenManager;
    //private SecurePreferencesHelper preferencesHelper;
    private DatabaseHelper databaseHelper;
    private String language;
    private String activityId;
    private boolean isSearching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking);

        preferencesHelper = new SecurePreferencesHelper(this);
        TokenRefreshHelper tokenRefreshHelper = new TokenRefreshHelper(this, preferencesHelper);
        gameTokenManager = new GameTokenManager(preferencesHelper, tokenRefreshHelper);
        activityId = "matchmaking_" + System.currentTimeMillis();
        databaseHelper = new DatabaseHelper(this);

        language  = preferencesHelper.getLanguage();

        initViews();
        setupClickListeners();
        loadCurrentPlayerData();

        initSignalR();
    }
    private void initSignalR() {
        gameTokenManager.prepareForShortGameAsync(new GameTokenManager.GameTokenCallback() {
            @Override
            public void onSuccess() {

                String token = preferencesHelper.getAuthToken();
                String userId = preferencesHelper.getUserId();

                if (token != null && !token.isEmpty()) {
                    signalRManager = PvPSignalRClientManager.getInstance();
                    signalRManager.reset();
                    signalRManager.init(token, userId);
                    signalRManager.removeListener(activityId);
                    connectToSignalR();
                } else {
                    finish();
                }
            }

            @Override
            public void onFailure(String error) {
                if (isFinishing() || isDestroyed()) return;
                runOnUiThread(() -> {
                    Toast.makeText(MatchmakingActivity.this, "Ошибка: " + error, Toast.LENGTH_LONG).show();
                    finish();
                });
                //Toast.makeText(MatchmakingActivity.this, "Не удалось обновить сессию. Попробуйте перелогиниться.", Toast.LENGTH_LONG).show();
            }
        });
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
        int score = userStats.getScore();

        tvCurrentPlayerName.setText(username != null ? username : "Guest");
        tvCurrentPlayerScore.setText(getString(R.string.score_format, Math.max(score, 0)));
        tvCurrentPlayerLevel.setText(language.equals("ru") ? "Ур " + (level > 0 ? level : 1) : "Lvl " + (level > 0 ? level : 1));
    }
    private void connectToSignalR() {
        signalRManager.addListener(activityId, new PvPSignalRClientManager.PvPConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Connected, joining queue");
                    showStatus(getString(R.string.searching_for_opponent));
                    startSearch();
                    signalRManager.joinQueue();
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Disconnected");
                    showStatus(getString(R.string.connection_lost));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error: " + error);
                    showStatus(getString(R.string.error) + " " + getString(R.string.connection_lost));
                });
            }

            @Override
            public void onMatchFound(MatchFoundData data) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Match found: " + data.getMatchId());
                    stopSearch();
                    showOpponentFound(data);
                });
            }

            @Override
            public void onDraftUpdated(DraftUpdateData data) {
            }
            @Override
            public void onGameReady(GameReadyData gameData) {
            }
            @Override
            public void onQuestionResult(SubmitAnswerResponse resultData) {
            }
            @Override
            public void onTimerUpdate(TimerUpdateData timerData) {
            }
            @Override
            public void onGameFinished(GameFinishedData finishData) {
            }
            @Override
            public void onOpponentDisconnected(DisconnectData disconnectData) {
            }
        });

        signalRManager.start();
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
    private void stopSearch() {
        isSearching = false;
        timerHandler.removeCallbacks(timerRunnable);
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
        stopSearch();

        gameTokenManager.prepareForShortGameAsync(new GameTokenManager.GameTokenCallback() {
            @Override
            public void onSuccess() {
                progressSearch.setVisibility(View.GONE);
                showStatus(getString(R.string.opponent_found));

                tvOpponentName.setText(data.getOpponentName());
                tvOpponentScore.setText(getString(R.string.score_format,data.getOpponentScore()));
                //tvOpponentScore.setText(getString(R.string.score_format, calculateRatingFromLevel(data.getOpponentLevel())));
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
            @Override
            public void onFailure(String error) {
                //Toast.makeText(MatchmakingActivity.this, "Не удалось обновить сессию. Попробуйте перелогиниться.", Toast.LENGTH_LONG).show();
                //cancelSearchAndExit();
                if (!isFinishing() && !isDestroyed()) {
                    String userMessage = getErrorMessage(error);
                    showErrorAndFinish(userMessage);
                }
            }
        });
    }


    private void navigateToDraftMode(MatchFoundData data) {
        Intent intent = new Intent(MatchmakingActivity.this, DraftModeActivity.class);
        intent.putExtra("matchId", data.getMatchId());
        intent.putExtra("opponentName", data.getOpponentName());
        intent.putExtra("opponentLevel", data.getOpponentLevel());
        intent.putExtra("opponentScore", data.getOpponentScore());
        intent.putExtra("availableModes", data.getAvailableModes().toArray(new String[0]));
        intent.putExtra("currentTurnUserId", data.getCurrentTurnUserId());
        intent.putExtra("yourId", data.getYourId());
        intent.putExtra("yourLevel", databaseHelper.getUserStats(preferencesHelper.getUserId()).getLevel());
        intent.putExtra("yourScore", databaseHelper.getUserStats(preferencesHelper.getUserId()).getScore());
        intent.putExtra("timePerTurn", data.getTimePerTurnSeconds());

        intent.putExtra("connectionActive", true);
        startActivity(intent);
    }
    private void cancelSearchAndExit() {
        cancelSearch();
        if (signalRManager!= null && signalRManager.isConnected()) {
            signalRManager.leaveQueue();
            signalRManager.stop();
        }
        finish();
    }
    private void cancelSearch() {
        isSearching = false;
        timerHandler.removeCallbacks(timerRunnable);
    }
    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
    private String getErrorMessage(String error) {
        if (error != null && error.contains("timeout")) {
            return "Сервер недоступен. Проверьте подключение.";
        } else if (error != null && error.contains("Network error")) {
            return "Нет подключения к серверу";
        } else if (error != null && error.contains("No internet")) {
            return "Нет интернет-соединения";
        }
        return "Не удалось подключиться. Попробуйте позже.";
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (signalRManager != null) {
            signalRManager.removeListener(activityId);
        }
    }
}

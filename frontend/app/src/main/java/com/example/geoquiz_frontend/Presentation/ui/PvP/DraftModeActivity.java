package com.example.geoquiz_frontend.Presentation.ui.PvP;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.geoquiz_frontend.Presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.Presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.data.remote.SignalRClientManager;
import com.example.geoquiz_frontend.data.remote.dtos.DisconnectData;
import com.example.geoquiz_frontend.data.remote.dtos.DraftUpdateData;
import com.example.geoquiz_frontend.data.remote.dtos.GameFinishedData;
import com.example.geoquiz_frontend.data.remote.dtos.GameReadyData;
import com.example.geoquiz_frontend.data.remote.dtos.MatchFoundData;
import com.example.geoquiz_frontend.data.remote.dtos.QuestionResultData;
import com.example.geoquiz_frontend.data.remote.dtos.TimerUpdateData;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DraftModeActivity extends BaseActivity {
    private static final String TAG = "DraftModeActivity";

    private ImageView ivBack;
    private TextView tvPlayer1Name, tvPlayer1Score, tvPlayer1Level;
    private TextView tvPlayer2Name, tvPlayer2Score, tvPlayer2Level;
    private TextView tvTurnStatus, tvTimer;
    private ProgressBar progressTurn;
    private LinearProgressIndicator progressTimer;


    private MaterialCardView cardCapitals, cardFlags, cardOutlines, cardLanguages;
    private List<MaterialCardView> modeCards;
    private List<String> bannedModes;

    private SignalRClientManager signalRManager;
    private PreferencesHelper preferencesHelper;
    private String activityId;

    private String matchId;
    private String yourId;
    private Integer yourLvl;
    private String currentTurnUserId;
    private List<String> availableModes;
    private int timePerTurn;
    private boolean isPlayerTurn = false;
    private boolean isDraftActive = true;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private int timeLeft = 10;

    private String opponentName;
    private int opponentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_mode);

        preferencesHelper = new PreferencesHelper(this);
        activityId = "draft_" + System.currentTimeMillis();


        initViews();
        setupClickListeners();
        getIntentData();


        bannedModes = new ArrayList<>();
        modeCards = Arrays.asList(cardCapitals, cardFlags, cardOutlines, cardLanguages);


        signalRManager = SignalRClientManager.getInstance();
        signalRManager.setCurrentMatch(matchId);
        connectToSignalR();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);

        tvPlayer1Name = findViewById(R.id.tvPlayer1Name);
        tvPlayer1Score = findViewById(R.id.tvPlayer1Score);
        tvPlayer1Level = findViewById(R.id.tvPlayer1Level);

        tvPlayer2Name = findViewById(R.id.tvPlayer2Name);
        tvPlayer2Score = findViewById(R.id.tvPlayer2Score);
        tvPlayer2Level = findViewById(R.id.tvPlayer2Level);


        tvTurnStatus = findViewById(R.id.tvTurnStatus);
        progressTurn = findViewById(R.id.progressTurn);
        tvTimer = findViewById(R.id.tvTimer);
        progressTimer = findViewById(R.id.progressTimer);

        cardCapitals = findViewById(R.id.cardCapitals);
        cardFlags = findViewById(R.id.cardFlags);
        cardOutlines = findViewById(R.id.cardOutlines);
        cardLanguages = findViewById(R.id.cardLanguages);

    }
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        cardCapitals.setOnClickListener(v -> {
            if (isPlayerTurn && isCardAvailable(cardCapitals)) {
                sendBanMode("capitals");
            }
            else if (!isPlayerTurn) Toast.makeText(this, "Not your turn!", Toast.LENGTH_SHORT).show();
        });

        cardFlags.setOnClickListener(v -> {
            if (isPlayerTurn && isCardAvailable(cardFlags)) {
                sendBanMode("flags");
            }
            else if (!isPlayerTurn) Toast.makeText(this, "Not your turn!", Toast.LENGTH_SHORT).show();
        });

        cardOutlines.setOnClickListener(v -> {
            if (isPlayerTurn && isCardAvailable(cardOutlines)) {
                sendBanMode("outlines");
            }
            else if (!isPlayerTurn) Toast.makeText(this, "Not your turn!", Toast.LENGTH_SHORT).show();
        });

        cardLanguages.setOnClickListener(v -> {
            if (isPlayerTurn && isCardAvailable(cardLanguages)) {
                sendBanMode("languages");
            }
            else if (!isPlayerTurn) Toast.makeText(this, "Not your turn!", Toast.LENGTH_SHORT).show();
        });
    }
    private void getIntentData() {
        Intent intent = getIntent();
        matchId = intent.getStringExtra("matchId");
        opponentName = intent.getStringExtra("opponentName");
        opponentLevel = intent.getIntExtra("opponentLevel", 1);
        currentTurnUserId = intent.getStringExtra("currentTurnUserId");
        yourId = intent.getStringExtra("yourId");
        yourLvl = intent.getIntExtra("yourLevel", 1);
        timePerTurn = intent.getIntExtra("timePerTurn", 10);

        String[] modesArray = intent.getStringArrayExtra("availableModes");
        if (modesArray != null) {
            availableModes = Arrays.asList(modesArray);
        }

        isPlayerTurn = yourId.equals(currentTurnUserId);

        loadPlayerData();
    }
    private void loadPlayerData() {
        String username = preferencesHelper.getUserName();
        int level = yourLvl;
        int score = (100 * (level - 1) * level) / 2 + 67; // temp

        tvPlayer1Name.setText(username != null ? username : "You");
        tvPlayer1Score.setText(String.valueOf(score));
        tvPlayer1Level.setText("Level " + level);

        tvPlayer2Name.setText(opponentName);
        tvPlayer2Score.setText(String.valueOf((100 * (opponentLevel - 1) * opponentLevel) / 2 + 67));
        tvPlayer2Level.setText("Level " + opponentLevel);
    }
    private void connectToSignalR() {
        signalRManager.addListener(activityId, new SignalRClientManager.ConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Connected to SignalR for draft");
                    updateTurnStatus();
                    if (isPlayerTurn) {
                        startTimer();
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Disconnected from SignalR");
                    Toast.makeText(DraftModeActivity.this,"Connection lost", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "SignalR error: " + error);
                    Toast.makeText(DraftModeActivity.this,"Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onMatchFound(MatchFoundData data) {
                //
            }

            @Override
            public void onDraftUpdated(DraftUpdateData data) {
                runOnUiThread(() -> handleDraftUpdate(data));
            }
            @Override
            public void onGameReady(GameReadyData gameData) {
                runOnUiThread(() -> handleGameReady(gameData));
            }
            @Override
            public void onQuestionResult(QuestionResultData resultData) {
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

        if (!signalRManager.isConnected()) {
            signalRManager.start();
        } else {
            updateTurnStatus();
            if (isPlayerTurn) {
                startTimer();
            }
        }
    }
    private void handleDraftUpdate(DraftUpdateData data) {
        Log.d(TAG, "Draft updated: " + data.getBannedMode() + " banned by " + data.getBannedByUserId());

        if (!matchId.equals(data.getMatchId())) {
            Log.w(TAG, "Received update for wrong match: " + data.getMatchId());
            return;
        }
        availableModes = data.getRemainingModes();

        String bannedMode = convertServerModeToClient(data.getBannedMode());
        if (bannedMode != null) {
            banModeLocally(bannedMode, data.getBannedByUserId());
        }

        currentTurnUserId = data.getNextTurnUserId();
        isPlayerTurn = yourId.equals(currentTurnUserId);

        updateTurnStatus();

        if (isPlayerTurn && isDraftActive) {
            startTimer();
        } else {
            stopTimer();
        }

        if (data.isDraftCompleted()) {
            tvTurnStatus.setText("Draft completed! Starting game...");
            isDraftActive = false;
            stopTimer();

        } else if (availableModes.size() == 1) {
            tvTurnStatus.setText("Final mode selected!");
        }
    }
    private void banModeLocally(String mode, String bannedByUserId) {
        MaterialCardView card = getCardByMode(mode);
        if (card != null) {
            card.setStrokeColor(getColorFromAttr(R.attr.colorError));
            card.setAlpha(0.5f);
            card.setClickable(false);

            bannedModes.add(mode);

            String message;
            if (bannedByUserId.equals(yourId)) {
                message = "You banned: " + getModeName(mode);
            } else {
                message = "Opponent banned: " + getModeName(mode);
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
    private void sendBanMode(String mode) {
        if (signalRManager != null && signalRManager.isConnected()) {
            stopTimer();
            Log.d(TAG, "Attempting to ban mode: " + mode + " for match: " + matchId);

            if (!isPlayerTurn) {
                Log.w(TAG, "Attempted to ban mode but not player's turn");
                Toast.makeText(this, "Not your turn!", Toast.LENGTH_SHORT).show();
                return;
            }
            signalRManager.banMode(matchId, mode, getLanguageCode());

            MaterialCardView card = getCardByMode(mode);
            if (card != null) {
                card.setEnabled(false);
                card.setAlpha(0.5f);
            }

            Log.d(TAG, "Ban mode sent: " + mode);
        } else {
            Toast.makeText(this, "Not connected to server", Toast.LENGTH_SHORT).show();
        }
    }
    private String getLanguageCode() {
        return "ru".equals(preferencesHelper.getLanguage()) ? "ru" : "en";
    }
    private boolean isCardAvailable(MaterialCardView card) {
        return card.isClickable();
    }
    private String convertServerModeToClient(String serverMode) {
        if (serverMode == null) return null;

        switch (serverMode) {
            case "Capital": return "capitals";
            case "Flag": return "flags";
            case "Outline": return "outlines";
            case "Language": return "languages";
            default:
                Log.w(TAG, "Unknown server mode: " + serverMode);
                return null;
        }
    }
    private MaterialCardView getCardByMode(String mode) {
        switch (mode) {
            case "capitals": return cardCapitals;
            case "flags": return cardFlags;
            case "outlines": return cardOutlines;
            case "languages": return cardLanguages;
            default: return null;
        }
    }
    private String getModeFromCard(MaterialCardView card) {
        if (card == cardCapitals) return "capitals";
        if (card == cardFlags) return "flags";
        if (card == cardOutlines) return "outlines";
        if (card == cardLanguages) return "languages";
        return "";
    }
    private void startTimer() {
        timeLeft = timePerTurn;
        progressTimer.setProgress(0);
        progressTimer.setMax(100);
        progressTimer.setVisibility(View.VISIBLE);
        tvTimer.setText(timeLeft + "s");

        progressTimer.setIndicatorColor(getColorFromAttr(R.attr.colorPrimary));
        tvTimer.setTextColor(getColorFromAttr(R.attr.colorPrimary));

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0 && isPlayerTurn && isDraftActive) {
                    timeLeft--;

                    int progress = (timePerTurn - timeLeft) * 100 / timePerTurn;
                    progressTimer.setProgress(progress);
                    tvTimer.setText(timeLeft + "s");

                    if (timeLeft <= 3) {
                        progressTimer.setIndicatorColor(ContextCompat.getColor(
                                DraftModeActivity.this, R.color.colorError));
                        tvTimer.setTextColor(ContextCompat.getColor(
                                DraftModeActivity.this, R.color.colorError));
                    }

                    timerHandler.postDelayed(this, 1000);
                } else if (timeLeft == 0 && isPlayerTurn && isDraftActive) {
                    onPlayerTimeout();
                }
            }
        };

        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        progressTimer.setVisibility(View.GONE);
        tvTimer.setVisibility(View.GONE);

    }

    private void onPlayerTimeout() {
        stopTimer();

        List<MaterialCardView> availableCards = new ArrayList<>();
        for (MaterialCardView card : modeCards) {
            if (card.isClickable()) {
                availableCards.add(card);
            }
        }

        if (!availableCards.isEmpty()) {
            int randomIndex = (int) (Math.random() * availableCards.size());
            MaterialCardView randomChoice = availableCards.get(randomIndex);

            String modeName;
            if (randomChoice == cardCapitals) modeName = "capitals";
            else if (randomChoice == cardFlags) modeName = "flags";
            else if (randomChoice == cardOutlines) modeName = "outlines";
            else if (randomChoice == cardLanguages) modeName = "languages";
            else {
                modeName = "";
            }

            sendBanMode(modeName);

            Toast.makeText(this,
                    "ru".equals(preferencesHelper.getLanguage()) ?
                            "Время вышло! Режим выбран случайно" :
                            "Time's up! Mode selected randomly",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private String getModeName(String mode) {
        String lang = preferencesHelper.getLanguage();
        switch (mode) {
            case "capitals":
                return "ru".equals(lang) ? "Столицы" : "Capitals";
            case "flags":
                return "ru".equals(lang) ? "Флаги" : "Flags";
            case "outlines":
                return "ru".equals(lang) ? "Контуры" : "Outlines";
            case "languages":
                return "ru".equals(lang) ? "Языки" : "Languages";
            default:
                return mode;
        }
    }

    private void updateTurnStatus() {
        String lang = preferencesHelper.getLanguage();

        if (!isDraftActive) {
            tvTurnStatus.setText("Draft completed");
            return;
        }

        if (isPlayerTurn) {
            tvTurnStatus.setText("ru".equals(lang) ? "Ваш ход! Забаньте режим" : "Your turn! Ban a mode");
            tvTurnStatus.setTextColor(getColorFromAttr(R.attr.colorPrimary));
            tvTimer.setVisibility(View.VISIBLE);
            progressTurn.setVisibility(View.GONE);
        } else {
            tvTurnStatus.setText("ru".equals(lang) ? "Противник выбирает..." : "Opponent is selecting...");
            tvTurnStatus.setTextColor(getColorFromAttr(R.attr.colorTertiaryFixed));
            tvTimer.setVisibility(View.GONE);
            progressTurn.setVisibility(View.VISIBLE);
        }
    }

    private int getColorFromAttr(int attrResId) {
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return ContextCompat.getColor(this, R.color.primary);
    }

    private void handleGameReady(GameReadyData data) {
        Log.d(TAG, "Game ready! Mode: " + data.getSelectedMode());

        Gson gson = new Gson();
        String gameDataJson = gson.toJson(data);

        Intent intent = new Intent(this, PvPGameActivity.class);
        intent.putExtra("matchId", data.getMatchId());
        intent.putExtra("opponentName", opponentName);
        intent.putExtra("opponentLevel", opponentLevel);
        intent.putExtra("yourLevel", yourLvl);
        intent.putExtra("gameData", gameDataJson);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        signalRManager.removeListener(activityId);
    }
}

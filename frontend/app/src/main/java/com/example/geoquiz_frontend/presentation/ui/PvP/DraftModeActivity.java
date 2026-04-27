package com.example.geoquiz_frontend.presentation.ui.PvP;

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

import com.example.geoquiz_frontend.data.remote.dtos.pvp.GameResumeData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.SubmitAnswerResponse;
import com.example.geoquiz_frontend.domain.enums.LocalizedText;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.data.remote.PvPSignalRClientManager;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.DisconnectData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.DraftUpdateData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.GameFinishedData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.GameReadyData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.MatchFoundData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.TimerUpdateData;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

    private PvPSignalRClientManager signalRManager;
    private String language;
    private String activityId;

    private String matchId;
    private String yourId;
    private int yourLvl;
    private int yourScore;
    private String currentTurnUserId;
    private List<String> availableModes;
    private List<String> bannedModes;
    private boolean isPlayerTurn = false;
    private boolean isDraftActive = true;

    private final Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private long serverTime = 0;
    private long serverTimeOffset = 0;
    private long turnEndsAtMillis = 0;
    private boolean isTimerRunning = false;
    private boolean isResumedGame = false;
    private String opponentName;
    private int opponentLevel;
    private int opponentScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_mode);

        preferencesHelper = new SecurePreferencesHelper(this);
        activityId = "draft_" + System.currentTimeMillis();
        language = preferencesHelper.getLanguage();

        initViews();
        setupClickListeners();
        getIntentData();

        signalRManager = PvPSignalRClientManager.getInstance();
        signalRManager.setCurrentMatch(matchId);
        connectToSignalR();
        if (!isResumedGame) {
            sendReadyForDraft(matchId);
        }
        else {
            handleTimerUpdate(new TimerUpdateData(serverTime, turnEndsAtMillis));
        }
        updateTurnStatus();

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

        String message = language.equals("ru") ? "Сейчас не ваша очередь!" : "Not your turn!";
        cardCapitals.setOnClickListener(v -> {
            if (isPlayerTurn && isCardAvailable(cardCapitals)) {
                sendBanMode("capitals");
            }
            else if (!isPlayerTurn) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        cardFlags.setOnClickListener(v -> {
            if (isPlayerTurn && isCardAvailable(cardFlags)) {
                sendBanMode("flags");
            }
            else if (!isPlayerTurn) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        cardOutlines.setOnClickListener(v -> {
            if (isPlayerTurn && isCardAvailable(cardOutlines)) {
                sendBanMode("outlines");
            }
            else if (!isPlayerTurn) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        cardLanguages.setOnClickListener(v -> {
            if (isPlayerTurn && isCardAvailable(cardLanguages)) {
                sendBanMode("languages");
            }
            else if (!isPlayerTurn) Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }
    private void getIntentData() {
        Intent intent = getIntent();
        matchId = intent.getStringExtra("matchId");
        opponentName = intent.getStringExtra("opponentName");
        opponentLevel = intent.getIntExtra("opponentLevel", 1);
        opponentScore = intent.getIntExtra("opponentScore", 0);
        currentTurnUserId = intent.getStringExtra("currentTurnUserId");
        yourId = intent.getStringExtra("yourId");
        yourLvl = intent.getIntExtra("yourLevel", 1);
        yourScore = intent.getIntExtra("yourScore", 0);
        turnEndsAtMillis = intent.getLongExtra("timerEndAt", System.currentTimeMillis());
        serverTime = intent.getLongExtra("serverTime", System.currentTimeMillis());
        isResumedGame = intent.getBooleanExtra("isResumedGame", false);

        String[] avModesArray = intent.getStringArrayExtra("availableModes");
        if (avModesArray != null) {
            availableModes = Arrays.asList(avModesArray);
        }
        String[] banModesArray = intent.getStringArrayExtra("bannedModes");
        if (banModesArray != null) {
            bannedModes = new ArrayList<>(Arrays.asList(banModesArray));
            for (String mode : bannedModes) {
                String bannedMode = convertServerModeToClient(mode);
                banModeLocally(bannedMode, yourId, false);
            }
        }

        isPlayerTurn = yourId.equals(currentTurnUserId);

        loadPlayerData();
    }
    private void loadPlayerData() {
        String username = preferencesHelper.getUserName();

        tvPlayer1Name.setText(username != null ? username : "You");
        tvPlayer1Score.setText(String.valueOf(yourScore));
        tvPlayer1Level.setText(getString(R.string.level_prefix) + " " + yourLvl);

        tvPlayer2Name.setText(opponentName);
        tvPlayer2Score.setText(String.valueOf((opponentScore)));
        tvPlayer2Level.setText(getString(R.string.level_prefix) + " " + opponentLevel);
    }
    private void connectToSignalR() {
        if (!signalRManager.isConnected()) {
            signalRManager.start();
        }
        signalRManager.addListener(activityId, new PvPSignalRClientManager.PvPConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Connected to SignalR for draft");
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Disconnected from SignalR");
                    Toast.makeText(DraftModeActivity.this, getString(R.string.connection_lost), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "SignalR error: " + error);
                    Toast.makeText(DraftModeActivity.this, getString(R.string.error) + error, Toast.LENGTH_SHORT).show();
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
            public void onDraftResume(MatchFoundData resumeData) {
            }

            @Override
            public void onGameReady(GameReadyData gameData) {
                runOnUiThread(() -> handleGameReady(gameData));
            }

            @Override
            public void onGameResume(GameResumeData resumeData) {
            }

            @Override
            public void onQuestionResult(SubmitAnswerResponse resultData) {
            }
            @Override
            public void onTimerUpdate(TimerUpdateData timerData) {
                runOnUiThread(() -> handleTimerUpdate(timerData));
            }
            @Override
            public void onGameFinished(GameFinishedData finishData) {
            }
            @Override
            public void onOpponentDisconnected(DisconnectData disconnectData) {
                runOnUiThread(() -> handleOpponentDisconnected(disconnectData));
            }

            @Override
            public void onForceDisconnect(LocalizedText message) {
                runOnUiThread(() -> handleForceDisconnect(message));
            }
        });
    }
    private void handleTimerUpdate(TimerUpdateData timerData) {
        long clientTime = System.currentTimeMillis();
        serverTime = timerData.getServerTime();
        serverTimeOffset = serverTime - clientTime;
        turnEndsAtMillis = timerData.getTimerEndsAt();

        Log.d(TAG, "onTimerUpdate: endsAt=" + turnEndsAtMillis + ", offset=" + serverTimeOffset);

        if (isPlayerTurn && isDraftActive && !isTimerRunning && turnEndsAtMillis > 0) {
            Log.d(TAG, "Starting timer from onTimerUpdate");
            startTimerWithData();
        }
    }
    private void startTimerWithData() {
        if (isTimerRunning) {
            stopTimer();
        }

        isTimerRunning = true;

        progressTimer.setVisibility(View.VISIBLE);
        tvTimer.setVisibility(View.VISIBLE);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPlayerTurn || !isDraftActive) {
                    stopTimer();
                    return;
                }

                //updateTimerDisplay();

                long now = System.currentTimeMillis() + serverTimeOffset;
                long remainingMs = turnEndsAtMillis - now;
                Log.d(TAG, "startTimerWithData: remainingMs=" + remainingMs + ", now=" + now + ", turnEndsAtMillis=" + turnEndsAtMillis);
                if (remainingMs <= 0) {
                    stopTimer();
                    return;
                }
                updateTimerDisplay(remainingMs);
                timerHandler.postDelayed(this, 1000);
            }
        };

        timerHandler.post(timerRunnable);
    }
    private void updateTimerDisplay(long remainingMs) {
        //long now = System.currentTimeMillis() + serverTimeOffset;
        //long remainingMs = turnEndsAtMillis - now;
        Log.d(TAG, "updateTimerDisplay: remainingMs=" + remainingMs + ", offset=" + serverTimeOffset + ", servertime=" + serverTime);
        int timeLeft = (int) Math.ceil(remainingMs / 1000.0);
        if (remainingMs <= 0) {
            tvTimer.setText("0 s");
            progressTimer.setProgress(100);
            return;
        }

        int timePerTurn = 10;
        int progress = (timePerTurn - timeLeft) * 100 / timePerTurn;
        progressTimer.setProgress(Math.max(0, Math.min(100, progress)));
        tvTimer.setText(timeLeft + " s");

        if (timeLeft <= 3) {
            progressTimer.setIndicatorColor(ContextCompat.getColor(
                    DraftModeActivity.this, R.color.colorError));
            tvTimer.setTextColor(ContextCompat.getColor(
                    DraftModeActivity.this, R.color.colorError));
        } else {
            progressTimer.setIndicatorColor(getColorFromAttr(R.attr.colorPrimary));
            tvTimer.setTextColor(getColorFromAttr(R.attr.colorPrimary));
        }
    }

    private void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        progressTimer.setVisibility(View.GONE);
        tvTimer.setVisibility(View.GONE);

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
            banModeLocally(bannedMode, data.getBannedByUserId(), true);
        }

        currentTurnUserId = data.getNextTurnUserId();
        isPlayerTurn = yourId.equals(currentTurnUserId);

        updateTurnStatus();

        if (data.isDraftCompleted()) {
            tvTurnStatus.setText("Draft completed! Starting game...");
            isDraftActive = false;
            stopTimer();
        } else if (availableModes.size() == 1) {
            tvTurnStatus.setText("Final mode selected!");
        }
    }
    private void banModeLocally(String mode, String bannedByUserId, boolean needMessage) {
        MaterialCardView card = getCardByMode(mode);
        if (card != null) {
            card.setStrokeColor(getColorFromAttr(R.attr.colorError));
            card.setAlpha(0.5f);
            card.setClickable(false);

            //bannedModes.add(mode);

            String message;
            if (needMessage) {
                bannedModes.add(mode);
                if (bannedByUserId.equals(yourId)) {
                    message = getString(R.string.you_banned) + getModeName(mode);
                } else {
                    message = getString(R.string.opponent_banned) + getModeName(mode);
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void sendReadyForDraft(String matchId)
    {
        if (signalRManager != null && signalRManager.isConnected()) {
            signalRManager.playerReadyForDraft(matchId);
        }
    }
    private void sendBanMode(String mode) {
        if (signalRManager != null && signalRManager.isConnected()) {
            stopTimer();
            Log.d(TAG, "Attempting to ban mode: " + mode + " for match: " + matchId);

            if (!isPlayerTurn) {
                Log.w(TAG, "Attempted to ban mode but not player's turn");
                Toast.makeText(this, getString(R.string.not_your_turn), Toast.LENGTH_SHORT).show();
                return;
            }
            signalRManager.banMode(matchId, mode, getLanguageCode(), bannedModes.size());

            Log.d(TAG, "Ban mode sent: " + mode);
        } else {
            Toast.makeText(this, getString(R.string.no_server), Toast.LENGTH_SHORT).show();
        }
    }
    private void handleOpponentDisconnected(DisconnectData data) {
        Toast.makeText(this, getString(R.string.opponent_disconnected_2), Toast.LENGTH_LONG).show();

        new Handler().postDelayed(() -> {
            finish();
        }, 1000);
    }
    private void handleGameReady(GameReadyData data) {
        Log.d(TAG, "Game ready! Mode: " + data.getSelectedMode());

        Gson gson = new Gson();
        String gameDataJson = gson.toJson(data);

        Intent intent = new Intent(this, PvPGameActivity.class);
        intent.putExtra("matchId", data.getMatchId());
        intent.putExtra("opponentName", opponentName);
        intent.putExtra("opponentScore", opponentScore);
        intent.putExtra("yourScore", yourScore);
        intent.putExtra("gameData", gameDataJson);

        startActivity(intent);
        finish();
    }
    private void handleForceDisconnect(LocalizedText message)
    {
        String msg = getLanguageCode().equals("ru") ? message.getRu() : message.getEn();
        Toast.makeText(DraftModeActivity.this, msg, Toast.LENGTH_SHORT).show();
        if (signalRManager != null) signalRManager.stop();
        finish();
    }
    private void updateTurnStatus() {
        if (!isDraftActive) {
            tvTurnStatus.setText("Draft completed");
            return;
        }

        if (isPlayerTurn) {
            tvTurnStatus.setText(getString(R.string.your_turn_ban));
            tvTurnStatus.setTextColor(getColorFromAttr(R.attr.colorPrimary));
            tvTimer.setVisibility(View.VISIBLE);
            progressTurn.setVisibility(View.GONE);
        } else {
            tvTurnStatus.setText(getString(R.string.opponent_selecting));
            tvTurnStatus.setTextColor(getColorFromAttr(R.attr.colorTertiaryFixed));
            tvTimer.setVisibility(View.GONE);
            progressTurn.setVisibility(View.VISIBLE);
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
    private int getColorFromAttr(int attrResId) {
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return ContextCompat.getColor(this, R.color.primary);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        signalRManager.removeListener(activityId);
    }
}

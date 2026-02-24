package com.example.geoquiz_frontend.Presentation.ui.PvP;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class DraftModeActivity extends BaseActivity {
    private ImageView ivBack;
    private TextView tvPlayer1Name, tvPlayer1Score, tvPlayer1Level;
    private TextView tvPlayer2Name, tvPlayer2Score, tvPlayer2Level;
    private TextView tvTurnStatus;
    private ProgressBar progressTurn;

    private MaterialCardView cardCapitals, cardFlags, cardOutlines, cardLanguages;


    private List<MaterialCardView> modeCards;
    private List<String> bannedModes;
    private boolean isPlayerTurn = true;
    private int bannedCount = 0;
    private final int TOTAL_MODES = 4;
    private final int BANS_PER_PLAYER = 1;
    private String selectedMode = "";

    private PreferencesHelper preferencesHelper;
    private Handler handler = new Handler();


    private TextView tvTimer;
    private LinearProgressIndicator progressTimer;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private int timeLeft = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_mode);

        preferencesHelper = new PreferencesHelper(this);

        initViews();
        setupClickListeners();
        //loadPlayerData();


        updateTurnStatus();
        startPlayerTurnTimer();

        bannedModes = new ArrayList<>();
        modeCards = new ArrayList<>();

        modeCards.add(cardCapitals);
        modeCards.add(cardFlags);
        modeCards.add(cardOutlines);
        modeCards.add(cardLanguages);
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
            if (isPlayerTurn && !isCardBanned(cardCapitals)) {
                banMode(cardCapitals, "capitals");
            }
        });

        cardFlags.setOnClickListener(v -> {
            if (isPlayerTurn && !isCardBanned(cardFlags)) {
                banMode(cardFlags, "flags");
            }
        });

        cardOutlines.setOnClickListener(v -> {
            if (isPlayerTurn && !isCardBanned(cardOutlines)) {
                banMode(cardOutlines, "outlines");
            }
        });

        cardLanguages.setOnClickListener(v -> {
            if (isPlayerTurn && !isCardBanned(cardLanguages)) {
                banMode(cardLanguages, "languages");
            }
        });
    }

    private void loadPlayerData() {

        Intent intent = getIntent();
        if (intent != null) {
            String player1Name = intent.getStringExtra("player1_name");
            String player1Score = intent.getStringExtra("player1_score");
            String player1Level = intent.getStringExtra("player1_level");
            String player2Name = intent.getStringExtra("player2_name");
            String player2Score = intent.getStringExtra("player2_score");
            String player2Level = intent.getStringExtra("player2_level");

            if (player1Name != null) tvPlayer1Name.setText(player1Name);
            if (player1Score != null) tvPlayer1Score.setText(player1Score);
            if (player1Level != null) tvPlayer1Level.setText("Level " + player1Level);
            if (player2Name != null) tvPlayer2Name.setText(player2Name);
            if (player2Score != null) tvPlayer2Score.setText(player2Score);
            if (player2Level != null) tvPlayer2Level.setText("Level " + player2Level);
        }
    }

    private void startPlayerTurnTimer() {
        timeLeft = 10;
        progressTimer.setProgress(0);
        progressTimer.setMax(100);
        progressTimer.setVisibility(View.VISIBLE);
        tvTimer.setVisibility(View.VISIBLE);
        tvTimer.setText(timeLeft + "s");

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    timeLeft--;

                    int progress = (10 - timeLeft) * 10;
                    progressTimer.setProgress(progress);
                    tvTimer.setText(timeLeft + "s");

                    if (timeLeft <= 3) {
                        progressTimer.setIndicatorColor(ContextCompat.getColor(
                                DraftModeActivity.this, R.color.colorError));
                        tvTimer.setTextColor(ContextCompat.getColor(
                                DraftModeActivity.this, R.color.colorError));
                    }

                    timerHandler.postDelayed(this, 1000);
                } else {
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

        progressTimer.setIndicatorColor(getColorFromAttr(R.attr.colorPrimary));
        tvTimer.setTextColor(getColorFromAttr(R.attr.colorPrimary));
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
            banMode(randomChoice, modeName);

            Toast.makeText(this,
                    "ru".equals(preferencesHelper.getLanguage()) ?
                            "Время вышло! Режим выбран случайно" :
                            "Time's up! Mode selected randomly",
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void banMode(MaterialCardView card, String modeName) {
        stopTimer();

        card.setStrokeColor(getColorFromAttr(R.attr.colorError));
        card.setAlpha(0.5f);
        card.setClickable(false);

        bannedModes.add(modeName);
        bannedCount++;

        String message = getString(
                "ru".equals(preferencesHelper.getLanguage()) ?
                        "Режим забанен: " + getModeName(modeName) :
                        "Mode banned: " + getModeName(modeName)
        );
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        if (bannedCount >= 3) {
            stopTimer();
            determineRemainingMode();
        } else {
            switchTurn();
        }
    }

    private void determineRemainingMode() {
        for (MaterialCardView card : modeCards) {
            if (card.isClickable()) {
                if (card == cardCapitals) selectedMode = "capitals";
                else if (card == cardFlags) selectedMode = "flags";
                else if (card == cardOutlines) selectedMode = "outlines";
                else if (card == cardLanguages) selectedMode = "languages";

                startGameWithMode(selectedMode);
                return;
            }
        }
    }

    private void startGameWithMode(String mode) {
        String message = getString(
                "ru".equals(preferencesHelper.getLanguage()) ?
                        "Играем в режим: " + getModeName(mode) :
                        "Playing mode: " + getModeName(mode)
        );
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // startIntent

        handler.postDelayed(() -> finish(), 2000);
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

    private boolean isCardBanned(MaterialCardView card) {
        return !card.isClickable();
    }

    private void switchTurn() {
        isPlayerTurn = !isPlayerTurn;
        updateTurnStatus();

        if (!isPlayerTurn) {
            progressTurn.setVisibility(View.VISIBLE);

            handler.postDelayed(this::opponentTurn, 2000);
        } else {
            progressTurn.setVisibility(View.GONE);
            startPlayerTurnTimer();
        }
    }

    private void opponentTurn() {
        List<MaterialCardView> availableCards = new ArrayList<>();
        for (MaterialCardView card : modeCards) {
            if (card.isClickable()) {
                availableCards.add(card);
            }
        }

        if (!availableCards.isEmpty()) {
            int randomIndex = (int) (Math.random() * availableCards.size());
            MaterialCardView opponentChoice = availableCards.get(randomIndex);
            String modeName;

            if (opponentChoice == cardCapitals) modeName = "capitals";
            else if (opponentChoice == cardFlags) modeName = "flags";
            else if (opponentChoice == cardOutlines) modeName = "outlines";
            else if (opponentChoice == cardLanguages) modeName = "languages";
            else {
                modeName = "";
            }

            runOnUiThread(() -> {
                opponentChoice.setStrokeColor(getColorFromAttr(R.attr.colorError));
                opponentChoice.setAlpha(0.5f);
                opponentChoice.setClickable(false);

                bannedModes.add(modeName);
                bannedCount++;

                String message = getString(
                        "ru".equals(preferencesHelper.getLanguage()) ?
                                "Противник забанил: " + getModeName(modeName) :
                                "Opponent banned: " + getModeName(modeName)
                );
                Toast.makeText(DraftModeActivity.this, message, Toast.LENGTH_SHORT).show();

                if (bannedCount >= 3) {
                    determineRemainingMode();
                } else {
                    switchTurn();
                }
            });
        }
    }

    private void updateTurnStatus() {
        String lang = preferencesHelper.getLanguage();

        if (isPlayerTurn) {
            tvTurnStatus.setText("ru".equals(lang) ? "Ваш ход! Забаньте режим" : "Your turn! Ban a mode");
            tvTurnStatus.setTextColor(getColorFromAttr(R.attr.colorPrimary));
        } else {
            tvTurnStatus.setText("ru".equals(lang) ? "Противник выбирает..." : "Opponent is selecting...");
            tvTurnStatus.setTextColor(getColorFromAttr(R.attr.colorTertiaryFixed));
        }
    }

    private int getColorFromAttr(int attrResId) {
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return ContextCompat.getColor(this, R.color.primary);
    }

    private String getString(String text) {
        return text;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        timerHandler.removeCallbacksAndMessages(null);
    }
}

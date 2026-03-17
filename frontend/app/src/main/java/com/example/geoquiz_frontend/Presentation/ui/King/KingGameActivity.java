package com.example.geoquiz_frontend.Presentation.ui.King;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.geoquiz_frontend.Presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.Presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.remote.KothSignalRClientManager;
import com.example.geoquiz_frontend.data.remote.dtos.koth.*;
import com.example.geoquiz_frontend.domain.enums.LocalizedText;
import com.example.geoquiz_frontend.domain.enums.RoundType;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KingGameActivity extends BaseActivity {

    private static final String TAG = "KingGameActivity";

    private ImageView ivClose;
    private TextView tvPlayersRemaining, tvTotalPlayers;
    private TextView tvQuestionNumber, tvRoundTypeIcon, tvRoundType, tvTimer, tvScore, tvQuestionTitle;
    private FrameLayout imageContainer;
    private ImageView ivQuestionImage;
    private Button btnPlayAudio;
    private Button[] optionButtons = new Button[4];
    private Button btnEndGame;

    private KothSignalRClientManager signalRManager;
    private PreferencesHelper preferencesHelper;
    private String activityId;
    private String matchId;
    private String userId;
    private String language;
    private int yourScore = 0;
    private int totalPlayers = 0;
    private int playersRemaining = 0;
    private List<PlayerInfo> allPlayers = new ArrayList<>();


    private int currentRound = 0;
    private RoundType currentRoundType = RoundType.CLASSIC;
    private QuestionData currentQuestion;
    private int selectedOptionIndex = -1;
    private boolean isQuestionActive = false;
    private boolean hasAnswered = false;
    private boolean isEliminated = false;
    private long questionStartTime;


    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private int timeLeft = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_king_game);

        preferencesHelper = new PreferencesHelper(this);
        userId = preferencesHelper.getUserId();
        language = preferencesHelper.getLanguage();
        activityId = "king_game_" + System.currentTimeMillis();

        initViews();
        getIntentData();
        setupClickListeners();

        signalRManager = KothSignalRClientManager.getInstance();
        connectToSignalR();
    }

    private void initViews() {
        ivClose = findViewById(R.id.ivClose);
        tvPlayersRemaining = findViewById(R.id.tvPlayersRemaining);
        tvTotalPlayers = findViewById(R.id.tvTotalPlayers);
        tvQuestionNumber = findViewById(R.id.tv_question_number);
        tvRoundTypeIcon = findViewById(R.id.tvRoundTypeIcon);
        tvRoundType = findViewById(R.id.tvRoundType);
        tvTimer = findViewById(R.id.tv_timer);
        tvScore = findViewById(R.id.tv_score);
        tvQuestionTitle = findViewById(R.id.tv_question_title);
        imageContainer = findViewById(R.id.image_container);
        ivQuestionImage = findViewById(R.id.iv_question_image);
        btnPlayAudio = findViewById(R.id.btn_play_audio);
        btnEndGame = findViewById(R.id.btn_end_game);

        optionButtons[0] = findViewById(R.id.btn_option1);
        optionButtons[1] = findViewById(R.id.btn_option2);
        optionButtons[2] = findViewById(R.id.btn_option3);
        optionButtons[3] = findViewById(R.id.btn_option4);

        setOptionsEnabled(false);
    }

    private void setupClickListeners() {
        ivClose.setOnClickListener(v -> exitGame());

        btnEndGame.setOnClickListener(v -> exitGame());

        btnPlayAudio.setOnClickListener(v -> {
            Toast.makeText(this, "Audio playback not added", Toast.LENGTH_SHORT).show();
        });

        for (int i = 0; i < optionButtons.length; i++) {
            final int index = i;
            optionButtons[i].setOnClickListener(v -> submitAnswer(index));
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        matchId = intent.getStringExtra("match_id");
        totalPlayers = intent.getIntExtra("total_players", 0);
        allPlayers = (List<PlayerInfo>) intent.getSerializableExtra("all_players");
        if (allPlayers == null) {
            allPlayers = new ArrayList<>();
        }

//        for (PlayerInfo playerInfo : allPlayers) {
//            Toast.makeText(this, playerInfo.getPlayerName(), Toast.LENGTH_SHORT).show();
//        }


        playersRemaining = totalPlayers;

        tvPlayersRemaining.setText(String.valueOf(playersRemaining));
        tvTotalPlayers.setText("/" + String.valueOf(totalPlayers));
    }

    private void connectToSignalR() {
        signalRManager.addListener(activityId, new KothSignalRClientManager.KothConnectionListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "Connected to SignalR for game");
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Toast.makeText(KingGameActivity.this, getString(R.string.connection_lost), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(KingGameActivity.this,
                            getString(R.string.error) + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onPlayerJoinedToOthers(PlayerJoinedData data) { }
            @Override public void onPlayerAboutLobby(LobbyInitialStateData data) { }
            @Override public void onPlayerLeft(PlayerLeftData data) { }
            @Override public void onLobbyCountdown(int secondsRemaining) { }
            @Override public void onLobbyCountdownCancelled() { }
            @Override public void onMatchStarted(MatchStartedData data) {}

            @Override
            public void onRoundStarted(RoundStartedData data) {
                runOnUiThread(() -> handleRoundStarted(data));
            }

            @Override
            public void onRoundFinished(RoundFinishedData data) {
                runOnUiThread(() -> handleRoundFinished(data));
            }

            @Override
            public void onPlayerEliminated(PlayerEliminatedData data) {
                runOnUiThread(() -> handlePlayerEliminated(data));
            }

            @Override
            public void onAnswerResult(AnswerResultData data) {
                runOnUiThread(() -> handleAnswerResult(data));
            }

            @Override
            public void onMatchFinished(MatchFinishedData data) {
                runOnUiThread(() -> handleMatchFinished(data));
            }
        });

        signalRManager.start();
    }

    private void handleRoundStarted(RoundStartedData data) {
        Log.d(TAG, "Round " + data.getRoundNumber() + " started, type: " + data.getRoundType());

        currentRound = data.getRoundNumber();
        currentRoundType = data.getRoundType();
        currentQuestion = data.getQuestion();
        isQuestionActive = true;
        hasAnswered = false;
        selectedOptionIndex = -1;
        questionStartTime = SystemClock.elapsedRealtime();

        tvQuestionNumber.setText(getString(R.string.question) +" " +  currentRound);

        if (currentRoundType == RoundType.SPEED) {
            tvRoundTypeIcon.setText("⚡");
            tvRoundType.setText(R.string.speed_round);
            tvRoundTypeIcon.setTextColor(ContextCompat.getColor(this, R.color.colorWarning));
            tvRoundType.setTextColor(ContextCompat.getColor(this, R.color.colorWarning));
        } else {
            tvRoundTypeIcon.setText("📚");
            tvRoundType.setText(R.string.classic_round);
            tvRoundTypeIcon.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvRoundType.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }

        tvQuestionTitle.setText(getLocalizedText(currentQuestion.getQuestionText()));

        if (currentQuestion.getImageUrl() != null && !currentQuestion.getImageUrl().isEmpty()) {
            imageContainer.setVisibility(View.VISIBLE);
            loadImageFromAssets(currentQuestion.getImageUrl(), ivQuestionImage);
        } else {
            imageContainer.setVisibility(View.GONE);
        }

        if (currentQuestion.getAudioUrl() != null && !currentQuestion.getAudioUrl().isEmpty()) {
            btnPlayAudio.setVisibility(View.VISIBLE);
        } else {
            btnPlayAudio.setVisibility(View.GONE);
        }

        List<OptionData> options = currentQuestion.getOptions();
        for (int i = 0; i < optionButtons.length; i++) {
            if (i < options.size()) {
                optionButtons[i].setVisibility(View.VISIBLE);
                optionButtons[i].setText(getLocalizedText(options.get(i).getText()));
                optionButtons[i].setEnabled(true);
                optionButtons[i].setBackgroundResource(R.drawable.option_button_normal);
                optionButtons[i].setTextColor(ContextCompat.getColor(this, android.R.color.black));
            } else {
                optionButtons[i].setVisibility(View.GONE);
            }
        }
        if (isEliminated) setOptionsEnabled(false);
        startTimer(data.getTimeLimitSeconds());
    }

    private void handleRoundFinished(RoundFinishedData data) {
        Log.d(TAG, "Round " + data.getRoundNumber() + " finished, eliminated: " +
                data.getEliminatedPlayerIds().size());

        stopTimer();

        playersRemaining = data.getRemainingPlayers();
        tvPlayersRemaining.setText(String.valueOf(playersRemaining));

        highlightCorrectAnswer(data.getCorrectOptionIndex());
        setOptionsEnabled(false);
    }

    private void handlePlayerEliminated(PlayerEliminatedData data) {
        if (data.getPlayerId().equals(userId)) {
            Log.d(TAG, "You have been eliminated! Place: " + data.getPlace());
            isEliminated = true;

            String playerName = "Player";
            for (PlayerInfo player: allPlayers) {
                if (player.getPlayerId().equals(userId)) playerName = player.getPlayerName();
            }

            Intent intent = new Intent(this, KingEliminatedActivity.class);
            intent.putExtra("player_name", playerName);
            intent.putExtra("rounds_survived", data.getRoundsSurvived());
            intent.putExtra("place", data.getPlace());
            intent.putExtra("correct_answers", data.getCorrectAnswers());
            intent.putExtra("total_score", data.getTotalScore());
            intent.putExtra("is_manually_disabled", data.isManuallyDisabled());
            intent.putExtra("total_players", totalPlayers);
            startActivity(intent);
        }

    }

    private void handleAnswerResult(AnswerResultData data) {
        Log.d(TAG, "Answer result: correct=" + data.isCorrect() + ", score=" + data.getScoreGained());

        if (!isEliminated) {
            yourScore += data.getScoreGained();
            tvScore.setText(String.valueOf(yourScore));

            highlightSelectedAnswer(data.isCorrect(), data.getCorrectOptionIndex());

            if (data.getRemainingPlayers() > 0) {
                playersRemaining = data.getRemainingPlayers();
                tvPlayersRemaining.setText(String.valueOf(playersRemaining));
            }
        }
    }

    private void handleMatchFinished(MatchFinishedData data) {
        Log.d(TAG, "Match finished! Winner: " + data.getWinnerId());

        Gson gson = new Gson();
        String matchDataJson = gson.toJson(data);

        Intent intent = new Intent(KingGameActivity.this, KingResultActivity.class);
        intent.putExtra("match_data", matchDataJson);
        startActivity(intent);
        finish();
    }

    private void submitAnswer(int index) {
        if (!isQuestionActive || hasAnswered || isEliminated) return;

        long answerTime = SystemClock.elapsedRealtime() - questionStartTime;

        hasAnswered = true;
        isQuestionActive = false;
        selectedOptionIndex = index;

        setOptionsEnabled(false);

        signalRManager.submitAnswer(matchId, currentRound, currentQuestion.getQuestionId(),
                index,  (int) answerTime);
    }

    private void startTimer(int seconds) {
        stopTimer();
        timeLeft = seconds;

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    updateTimerDisplay();
                    timeLeft--;
                    timerHandler.postDelayed(this, 1000);
                } else {
                    updateTimerDisplay();
                    if (isQuestionActive && !hasAnswered && !isEliminated) {
                        setOptionsEnabled(false);
                        isQuestionActive = false;
                    }
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void updateTimerDisplay() {
        String timeFormatted = String.format(Locale.getDefault(), "00:%02d", timeLeft);
        tvTimer.setText(timeFormatted);

        if (timeLeft <= 3) {
            tvTimer.setTextColor(ContextCompat.getColor(this, R.color.colorError));
        } else {
            tvTimer.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    private void highlightSelectedAnswer(boolean isCorrect, int correctIndex) {
        if (correctIndex >= 0 && correctIndex < optionButtons.length) {
            optionButtons[correctIndex].setBackgroundResource(R.drawable.correct_answer_bg);
            optionButtons[correctIndex].setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }

        if (!isCorrect && selectedOptionIndex >= 0 && selectedOptionIndex < optionButtons.length) {
            optionButtons[selectedOptionIndex].setBackgroundResource(R.drawable.wrong_answer_bg);
            optionButtons[selectedOptionIndex].setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void highlightCorrectAnswer(int correctIndex) {
        if (correctIndex >= 0 && correctIndex < optionButtons.length) {
            optionButtons[correctIndex].setBackgroundResource(R.drawable.correct_answer_bg);
            optionButtons[correctIndex].setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void setOptionsEnabled(boolean enabled) {
        for (Button button : optionButtons) {
            button.setEnabled(enabled);
        }
    }


    private String getLocalizedText(LocalizedText text) {
        if (text == null) return "";
        if ("ru".equals(language) && text.getRu() != null && !text.getRu().isEmpty()) {
            return text.getRu();
        }
        return text.getEn() != null ? text.getEn() : "";
    }

    private void loadImageFromAssets(String imagePath, ImageView imageView) {
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                InputStream ims = getAssets().open(imagePath);
                Drawable d = Drawable.createFromStream(ims, null);
                imageView.setImageDrawable(d);
                ims.close();
                Log.d(TAG, "Image loaded successfully: " + imagePath);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image from assets: " + imagePath, e);
        }
    }

    private void exitGame() {
        String currentLanguage = preferencesHelper.getLanguage();

        String title = "ru".equals(currentLanguage) ? "Выход из игры" : "Exit Game";
        String message = "ru".equals(currentLanguage)
                ? "Вы уверены, что хотите покинуть игру?"
                : "Are you sure you want to leave the game?";
        String positiveButton = "ru".equals(currentLanguage) ? "Да, выйти" : "Yes, leave";
        String negativeButton = "ru".equals(currentLanguage) ? "Отмена" : "Cancel";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    performExitGame();
                })
                .setNegativeButton(negativeButton, null)
                .show();
    }

    private void performExitGame() {
        stopTimer();
        if (signalRManager != null && signalRManager.isConnected() && matchId != null) {
            signalRManager.leaveMatch(matchId);
            signalRManager.stop();
        }
        //finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (signalRManager != null) {
            signalRManager.removeListener(activityId);
        }
    }

    @Override
    public void onBackPressed() {
        exitGame();
    }
}
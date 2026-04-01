package com.example.geoquiz_frontend.presentation.ui.PvP;
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

import androidx.core.content.ContextCompat;

import com.example.geoquiz_frontend.data.remote.dtos.pvp.SubmitAnswerResponse;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.remote.PvPSignalRClientManager;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.DisconnectData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.DraftUpdateData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.GameFinishedData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.GameReadyData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.MatchFoundData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.OptionData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.QuestionData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.QuestionResultData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.TimerUpdateData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.UnlockedAchievement;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
public class PvPGameActivity extends BaseActivity {

    private static final String TAG = "PvPGameActivity";

    private ImageView ivClose;

    private TextView tvPlayer1Name, tvPlayer1TotalScore, tvPlayer1Score;
    private TextView tvPlayer2Name, tvPlayer2TotalScore, tvPlayer2Score;
    private TextView tvCrownPlayer1, tvCrownPlayer2;

    private TextView tvQuestionNumber, tvTimer, tvQuestionTitle;
    private FrameLayout imageContainer;
    private ImageView ivQuestionImage;
    private Button btnPlayAudio;
    private Button[] optionButtons = new Button[4];

    private PvPSignalRClientManager signalRManager;
    private PreferencesHelper preferencesHelper;
    private String activityId;

    private String matchId;
    private String yourId;
    private int yourLvl;
    private String opponentName;
    private int opponentLevel;
    private int yourTotalScore = 0;
    private int opponentTotalScore = 0;


    private List<QuestionData> questions;
    private int currentQuestionIndex = 0;
    private int selectedIndex;
    private int totalQuestions = 10;
    private int totalGameTime = 60;

    private boolean isQuestionActive = true;
    private boolean hasAnswered = false;
    private boolean allAnswered = false;
    private long questionStartTime;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private long serverTime = 0;
    private long serverTimeOffset = 0;
    private long turnEndsAtMillis = 0;
    private boolean isTimerRunning = false;



    private int yourCurrentScore = 0;
    private int opponentCurrentScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pvp_game);

        preferencesHelper = new PreferencesHelper(this);

        activityId = "game_" + System.currentTimeMillis();

        initViews();
        getIntentData();

        String gameDataJson = getIntent().getStringExtra("gameData");
        if (gameDataJson != null) {
            Gson gson = new Gson();
            GameReadyData gameData = gson.fromJson(gameDataJson, GameReadyData.class);
            handleGameReady(gameData);
        } else {
            Log.e(TAG, "No game data received!");
            Toast.makeText(this, getString(R.string.no_game_data), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupClickListeners();
        loadPlayerData();

        signalRManager = PvPSignalRClientManager.getInstance();
        connectToSignalR();

        if (turnEndsAtMillis > 0) {
            startTimerWithData(turnEndsAtMillis);
        }
    }

    private void initViews() {
        ivClose = findViewById(R.id.ivClose);

        tvPlayer1Name = findViewById(R.id.tvPlayer1Name);
        tvPlayer1TotalScore = findViewById(R.id.tvPlayer1TotalScore);
        tvPlayer1Score = findViewById(R.id.tvPlayer1Score);

        tvPlayer2Name = findViewById(R.id.tvPlayer2Name);
        tvPlayer2TotalScore = findViewById(R.id.tvPlayer2TotalScore);
        tvPlayer2Score = findViewById(R.id.tvPlayer2Score);

        tvCrownPlayer1 = findViewById(R.id.tvCrownPlayer1);
        tvCrownPlayer2 = findViewById(R.id.tvCrownPlayer2);

        tvQuestionNumber = findViewById(R.id.tv_question_number);
        tvTimer = findViewById(R.id.tv_timer);

        tvQuestionTitle = findViewById(R.id.tv_question_title);
        imageContainer = findViewById(R.id.image_container);
        ivQuestionImage = findViewById(R.id.iv_question_image);
        btnPlayAudio = findViewById(R.id.btn_play_audio);

        optionButtons[0] = findViewById(R.id.btn_option1);
        optionButtons[1] = findViewById(R.id.btn_option2);
        optionButtons[2] = findViewById(R.id.btn_option3);
        optionButtons[3] = findViewById(R.id.btn_option4);

        tvCrownPlayer1.setVisibility(View.GONE);
        tvCrownPlayer2.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        ivClose.setOnClickListener(v -> {
            finish();
        });

        btnPlayAudio.setOnClickListener(v -> {
            //
        });

    }

    private void getIntentData() {
        Intent intent = getIntent();
        matchId = intent.getStringExtra("matchId");
        opponentName = intent.getStringExtra("opponentName");
        opponentLevel = intent.getIntExtra("opponentLevel", 1);
        yourId = preferencesHelper.getUserId();
        yourLvl = intent.getIntExtra("yourLevel", 1);

        serverTimeOffset = intent.getLongExtra("serverTimeOffset", 0);
        turnEndsAtMillis = intent.getLongExtra("turnEndsAtMillis", 0);

        yourTotalScore = yourLvl * 100 + 67; // temp
        opponentTotalScore = opponentLevel * 100 + 67; // temp
    }

    private void loadPlayerData() {
        String username = preferencesHelper.getUserName();

        tvPlayer1Name.setText(username != null ? username : "You");
        tvPlayer1TotalScore.setText(String.valueOf(yourTotalScore));
        tvPlayer1Score.setText("0");

        tvPlayer2Name.setText(opponentName);
        tvPlayer2TotalScore.setText(String.valueOf(opponentTotalScore));
        tvPlayer2Score.setText("0");
    }

    private void connectToSignalR() {
        signalRManager.addListener(activityId, new PvPSignalRClientManager.ConnectionListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "Connected to SignalR for game");
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Toast.makeText(PvPGameActivity.this,
                            getString(R.string.connection_lost), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(PvPGameActivity.this,
                            getString(R.string.error) + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onMatchFound(MatchFoundData data) { }

            @Override
            public void onDraftUpdated(DraftUpdateData data) { }

            @Override
            public void onGameReady(GameReadyData data) {
            }

            @Override
            public void onQuestionResult(SubmitAnswerResponse data) {
                runOnUiThread(() -> handleQuestionResult(data));
            }

            @Override
            public void onTimerUpdate(TimerUpdateData data) {
                runOnUiThread(() -> {
                    long clientTime = System.currentTimeMillis();
                    serverTime = data.getServerTime();
                    serverTimeOffset = serverTime - clientTime;
                    turnEndsAtMillis = data.getTimerEndsAt();
                    if (isQuestionActive && !hasAnswered && !isTimerRunning && turnEndsAtMillis > 0) {
                        startTimerWithData(turnEndsAtMillis);
                    }
                });
            }

            @Override
            public void onGameFinished(GameFinishedData data) {
                runOnUiThread(() -> handleGameFinished(data));
            }

            @Override
            public void onOpponentDisconnected(DisconnectData data) {
                runOnUiThread(() -> handleOpponentDisconnected(data));
            }
        });

        signalRManager.start();
    }

    private void handleGameReady(GameReadyData data) {
        Log.d(TAG, "Game ready! Mode: " + data.getSelectedMode());

        this.questions = data.getQuestions();
        this.totalQuestions = data.getTotalQuestions();
        this.totalGameTime = data.getTotalGameTimeSeconds();


        showQuestion(0);
    }

    private void showQuestion(int index) {
        if (questions == null || index >= questions.size()) return;

        QuestionData question = questions.get(index);

        tvQuestionNumber.setText(String.format(Locale.getDefault(),
                "Question %d/%d", index + 1, totalQuestions));

        String questionText = preferencesHelper.getLanguage().equals("ru") ?
                question.getQuestionText().getRu() :
                question.getQuestionText().getEn();
        tvQuestionTitle.setText(questionText);

        if (question.getImageUrl() != null && !question.getImageUrl().isEmpty()) {
            imageContainer.setVisibility(View.VISIBLE);
            loadImageFromAssets(question.getImageUrl(), ivQuestionImage);
        } else {
            imageContainer.setVisibility(View.GONE);
        }


        if (question.getAudioUrl() != null && !question.getAudioUrl().isEmpty()) {
            btnPlayAudio.setVisibility(View.VISIBLE);
        } else {
            btnPlayAudio.setVisibility(View.GONE);
        }

        String lang = preferencesHelper.getLanguage();
        List<OptionData> options = question.getOptions();
        for (int i = 0; i < optionButtons.length && i < options.size(); i++) {
            if (optionButtons[i] != null) {
                final int ind = i;
                optionButtons[i].setText(lang.equals("ru") ?
                        options.get(i).getText().getRu() :
                        options.get(i).getText().getEn());
                optionButtons[i].setEnabled(true);
                optionButtons[i].setBackgroundResource(R.drawable.option_button_normal);
                optionButtons[i].setTextColor(getResources().getColor(android.R.color.black, null));
                optionButtons[i].setOnClickListener(v -> submitAnswer(ind));
            }
        }

        currentQuestionIndex = index;
        isQuestionActive = true;
        hasAnswered = false;
        questionStartTime = SystemClock.elapsedRealtime();


    }
    private void loadImageFromAssets(String imagePath, ImageView imageView) {
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                InputStream ims = getAssets().open(imagePath);
                Drawable d = Drawable.createFromStream(ims, null);
                imageView.setImageDrawable(d);
                ims.close();
                Log.d(TAG, "Image loaded successfully: " + imagePath);
            } else {
                Log.w(TAG, "Image path is empty or null");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image from assets: " + imagePath, e);
        }
    }
    private void submitAnswer(int slcIndex) {
        if (!isQuestionActive || hasAnswered) return;

        long answerTime = SystemClock.elapsedRealtime() - questionStartTime;

        hasAnswered = true;
        isQuestionActive = false;

        for (Button button : optionButtons) {
            button.setEnabled(false);
        }

        QuestionData currentQuestion = questions.get(currentQuestionIndex);
        signalRManager.submitAnswer(
                matchId,
                currentQuestion.getQuestionId(),
                slcIndex,
                (int) answerTime,
                currentQuestionIndex + 1
        );

        selectedIndex = slcIndex;
    }

    private void handleQuestionResult(SubmitAnswerResponse result) {
        if (hasAnswered && !isQuestionActive && !allAnswered) {
            isQuestionActive = true;
            hasAnswered = false;

            yourCurrentScore = result.getYourScore();
            opponentCurrentScore = result.getOpponentScore();
            highlightAnswers(result.getCorrectOptionIndex());

            if (result.getQuestionNumber() < 10) {
                new Handler().postDelayed(() -> {
                    showQuestion(currentQuestionIndex + 1);
                }, 1000);
            }
            else {
                allAnswered = true;
            }
        }
        else {
            yourCurrentScore = result.getOpponentScore();
            opponentCurrentScore = result.getYourScore();
        }

        tvPlayer1Score.setText(String.valueOf(yourCurrentScore));
        tvPlayer2Score.setText(String.valueOf(opponentCurrentScore));
        updateCrown();
    }

    private void highlightAnswers(int correctIndex) {
        for (int i = 0; i < optionButtons.length; i++) {
            if (optionButtons[i] == null) continue;

            if (i == correctIndex) {
                optionButtons[i].setBackgroundResource(R.drawable.correct_answer_bg);
                optionButtons[i].setTextColor(getResources().getColor(android.R.color.white, null));
            } else if (i == selectedIndex && i != correctIndex) {
                optionButtons[i].setBackgroundResource(R.drawable.wrong_answer_bg);
                optionButtons[i].setTextColor(getResources().getColor(android.R.color.white, null));
            }
        }
    }

    private void startTimerWithData(long endsAt) {
        if (isTimerRunning) {
            stopTimer();
        }

        turnEndsAtMillis = endsAt;
        isTimerRunning = true;

        tvTimer.setVisibility(View.VISIBLE);

        timerRunnable = new Runnable() {
            @Override
            public void run() {

                updateTimerDisplay();

                long now = System.currentTimeMillis() + serverTimeOffset;
                if (turnEndsAtMillis <= now) {
                    stopTimer();
                    return;
                }

                timerHandler.postDelayed(this, 1000);
            }
        };

        timerHandler.post(timerRunnable);
    }
    private void updateTimerDisplay() {
        long now = System.currentTimeMillis() + serverTimeOffset;
        long remainingMs = turnEndsAtMillis - now;
        int timeLeft = (int) Math.ceil(remainingMs / 1000.0);

        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvTimer.setText(timeFormatted);

        if (remainingMs <= 0) {
            return;
        }

        if (timeLeft <= 10) {
            tvTimer.setTextColor(ContextCompat.getColor(PvPGameActivity.this, R.color.colorError));
        }
    }

    private void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        tvTimer.setVisibility(View.GONE);
    }
    private void handleTimerUpdate(TimerUpdateData data) {
        long clientTime = System.currentTimeMillis();
        serverTime = data.getServerTime();
        serverTimeOffset = serverTime - clientTime;
        turnEndsAtMillis = data.getTimerEndsAt();


//        timerRunnable = new Runnable() {
//            @Override
//            public void run() {
//                long now = System.currentTimeMillis() + serverTimeOffset;
//                long remainingMs = turnEndsAtMillis - now;
//
//                int timeLeft = (int) Math.ceil(remainingMs / 1000.0);
//                int minutes = timeLeft / 60;
//                int seconds = timeLeft % 60;
//
//                if (remainingMs <= 0) {
//                    tvTimer.setText("0 s");
//                    return;
//                }
//
//                String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
//                tvTimer.setText(timeFormatted);
//
//                if (timeLeft <= 10) {
//                    tvTimer.setTextColor(ContextCompat.getColor(PvPGameActivity.this, R.color.colorError));
//                }
//
//                timerHandler.postDelayed(this, 100);
//
//            }
//        };
    }

    private void updateCrown() {
        if (yourCurrentScore > opponentCurrentScore) {
            tvCrownPlayer1.setVisibility(View.VISIBLE);
            tvCrownPlayer2.setVisibility(View.GONE);
        } else if (opponentCurrentScore > yourCurrentScore) {
            tvCrownPlayer1.setVisibility(View.GONE);
            tvCrownPlayer2.setVisibility(View.VISIBLE);
        } else {
            tvCrownPlayer1.setVisibility(View.GONE);
            tvCrownPlayer2.setVisibility(View.GONE);
        }
    }

    private void handleGameFinished(GameFinishedData data) {
        stopTimer();
        Log.d(TAG, "Game finished! Winner: " + data.getWinnerId());

        Intent intent = new Intent(this, PvPResultActivity.class);
        intent.putExtra("player_won", data.isWinner());
        intent.putExtra("finish_reason", data.getFinishReason());

        intent.putExtra("player_score", data.getYourStats().getFinalScore());
        intent.putExtra("player_correct", data.getYourStats().getCorrectAnswers());
        intent.putExtra("player_total_questions", data.getYourStats().getTotalQuestionsAnswered());
        intent.putExtra("player_avg_time", data.getYourStats().getAverageAnswerTimeMs());

        intent.putExtra("opponent_score", data.getOpponentStats().getFinalScore());
        intent.putExtra("opponent_correct", data.getOpponentStats().getCorrectAnswers());
        intent.putExtra("opponent_total_questions", data.getOpponentStats().getTotalQuestionsAnswered());
        intent.putExtra("opponent_avg_time", data.getOpponentStats().getAverageAnswerTimeMs());
        intent.putExtra("opponent_name", opponentName);

        intent.putExtra("experience_gained", data.getExperienceGained());
        startActivity(intent);
        finish();
    }

    private void handleOpponentDisconnected(DisconnectData data) {
        Toast.makeText(this, getString(R.string.opponent_disconnected), Toast.LENGTH_LONG).show();

        new Handler().postDelayed(() -> {
            finish();
        }, 2000);
    }

    private void showAchievements(List<UnlockedAchievement> achievements) {
        StringBuilder sb = new StringBuilder("Achievements unlocked:\n");
        for (UnlockedAchievement achievement : achievements) {
            sb.append("• ").append(achievement.getTitle()).append("\n");
        }
        Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        signalRManager.removeListener(activityId);
    }
}

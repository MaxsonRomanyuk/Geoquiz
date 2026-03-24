package com.example.geoquiz_frontend.presentation.ui.PvP;

import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;


import com.example.geoquiz_frontend.presentation.ui.Home.MainActivity;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.remote.PvPSignalRClientManager;
import com.google.android.material.button.MaterialButton;

import java.util.Objects;

public class PvPResultActivity extends BaseActivity {

    private TextView ivResultIcon;
    private TextView tvResultTitle, tvFinishReason;
    private TextView tvPlayer1Name, tvPlayer1Score, tvPlayer1Correct, tvPlayer1AvgTime;
    private TextView tvPlayer2Name, tvPlayer2Score, tvPlayer2Correct, tvPlayer2AvgTime;
    private TextView tvExperienceGained;
    private TextView tvMessage;
    private MaterialButton btnRematch, btnMainMenu;

    private boolean playerWon;
    private int playerScore;
    private int playerCorrect;
    private int playerTotalQuestions;
    private float playerAvgTimeMs;
    private int opponentScore;
    private int opponentCorrect;
    private int opponentTotalQuestions;
    private float opponentAvgTimeMs;
    private int experienceGained;
    private String finishReason;
    private String opponentName;

    private PreferencesHelper preferencesHelper;
    private PvPSignalRClientManager signalRManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pvp_results);

        preferencesHelper = new PreferencesHelper(this);
        signalRManager = PvPSignalRClientManager.getInstance();
        getIntentData();
        initViews();
        setupClickListeners();
        displayResults();
        updateStats();
    }

    private void getIntentData() {
        Intent intent = getIntent();

        playerWon = intent.getBooleanExtra("player_won", false);
        finishReason = intent.getStringExtra("finish_reason");
        if (finishReason == null) finishReason = "AllQuestionsAnswered";

        playerScore = intent.getIntExtra("player_score", 0);
        playerCorrect = intent.getIntExtra("player_correct", 0);
        playerTotalQuestions = intent.getIntExtra("player_total_questions", 10);
        playerAvgTimeMs = (float)intent.getDoubleExtra("player_avg_time", 0.0);

        opponentScore = intent.getIntExtra("opponent_score", 0);
        opponentCorrect = intent.getIntExtra("opponent_correct", 0);
        opponentTotalQuestions = intent.getIntExtra("opponent_total_questions", 10);
        opponentAvgTimeMs = (float)intent.getDoubleExtra("opponent_avg_time", 0);
        opponentName = intent.getStringExtra("opponent_name");
        if (opponentName == null) opponentName = "Opponent";

        experienceGained = intent.getIntExtra("experience_gained", 0);
    }

    private void initViews() {
        ivResultIcon = findViewById(R.id.iv_result_icon);
        tvResultTitle = findViewById(R.id.tv_result_title);
        tvFinishReason = findViewById(R.id.tv_finish_reason);

        tvPlayer1Name = findViewById(R.id.tvPlayer1Name);
        tvPlayer1Score = findViewById(R.id.tvPlayer1Score);
        tvPlayer1Correct = findViewById(R.id.tvPlayer1Correct);
        tvPlayer1AvgTime = findViewById(R.id.tvPlayer1AvgTime);

        tvPlayer2Name = findViewById(R.id.tvPlayer2Name);
        tvPlayer2Score = findViewById(R.id.tvPlayer2Score);
        tvPlayer2Correct = findViewById(R.id.tvPlayer2Correct);
        tvPlayer2AvgTime = findViewById(R.id.tvPlayer2AvgTime);

        tvExperienceGained = findViewById(R.id.tv_experience_gained);
        tvMessage = findViewById(R.id.tv_message);

        btnRematch = findViewById(R.id.btn_rematch);
        btnMainMenu = findViewById(R.id.btn_main_menu);
    }

    private void setupClickListeners() {
        btnRematch.setOnClickListener(v -> {
            if (signalRManager!= null && signalRManager.isConnected()) {
                signalRManager.leaveQueue();
                signalRManager.stop();
            }
            Intent intent = new Intent(this, MatchmakingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnMainMenu.setOnClickListener(v -> {
            if (signalRManager!= null && signalRManager.isConnected()) {
                signalRManager.leaveQueue();
                signalRManager.stop();
            }
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void displayResults() {
        String currentLanguage = preferencesHelper.getLanguage();

        if (playerWon) {
            ivResultIcon.setText("🏆");
            tvResultTitle.setText("ru".equals(currentLanguage) ? "ПОБЕДА!" : "VICTORY!");
        } else {
            ivResultIcon.setText("💔");
            tvResultTitle.setText("ru".equals(currentLanguage) ? "ПОРАЖЕНИЕ" : "DEFEAT");
        }

        setFinishReasonText(finishReason);

        tvPlayer1Name.setText(preferencesHelper.getUserName());
        tvPlayer1Score.setText(String.valueOf(playerScore));
        tvPlayer1Correct.setText(playerCorrect + "/" + playerTotalQuestions);

        float avgTimeSec = playerAvgTimeMs / 1000f;
        tvPlayer1AvgTime.setText(String.format("%.2fs", avgTimeSec));

        tvPlayer2Name.setText(opponentName);
        tvPlayer2Score.setText(String.valueOf(opponentScore));
        tvPlayer2Correct.setText(opponentCorrect + "/" + opponentTotalQuestions);

        float opponentAvgTimeSec = opponentAvgTimeMs / 1000f;
        tvPlayer2AvgTime.setText(String.format("%.2fs", opponentAvgTimeSec));

        tvExperienceGained.setText("+" + experienceGained + " XP");

        setMessageText();
    }
    private void updateStats()
    {
        UserRepository userRepository = UserRepository.getInstance(this);
        userRepository.loadUserData(true);
    }
    private void setFinishReasonText(String reason) {
        String currentLanguage = preferencesHelper.getLanguage();
        String reasonText = "";

        switch (reason) {
            case "AllQuestionsAnswered":
                reasonText = "ru".equals(currentLanguage) ?
                        "Все вопросы отвечены" : "All questions answered";
                break;
            case "TimeOut":
                reasonText = "ru".equals(currentLanguage) ?
                        "Время вышло" : "Time out";
                break;
            case "OpponentDisconnected":
                reasonText = "ru".equals(currentLanguage) ?
                        "Противник отключился" : "Opponent disconnected";
                break;
            case "PlayerDisconnected":
                reasonText = "ru".equals(currentLanguage) ?
                        "Вы отключились" : "You disconnected";
                break;
        }

        tvFinishReason.setText(reasonText);
    }

    private void setMessageText() {
        String currentLanguage = preferencesHelper.getLanguage();
        String message = "";

        if (playerWon) {
            switch (finishReason) {
                case "AllQuestionsAnswered":
                    message = "ru".equals(currentLanguage) ?
                            "Отличная игра! Вы показали лучший результат!" :
                            "Great game! You showed the best result!";
                    break;
                case "TimeOut":
                    if (playerTotalQuestions>opponentTotalQuestions) {
                        message = "ru".equals(currentLanguage) ?
                                "Победа по времени! Ваши знания быстрее!" :
                                "Time victory! Your knowledge is faster!";
                    }
                    else {
                        message = "ru".equals(currentLanguage) ?
                                "Отличная игра! Вы показали лучший результат!" :
                                "Great game! You showed the best result!";
                    }
                    break;
                case "OpponentDisconnected":
                    message = "ru".equals(currentLanguage) ?
                            "Противник покинул игру. Техническая победа!" :
                            "Opponent left the game. Technical victory!";
                    break;
                default:
                    message = "ru".equals(currentLanguage) ?
                            "Поздравляем с победой!" :
                            "Congratulations on your victory!";
                    break;
            }
        } else {
            if (Objects.equals(finishReason, "PlayerDisconnected")) {
                message = "ru".equals(currentLanguage) ?
                        "Вы отключились от игры. Попробуйте ещё раз!" :
                        "You disconnected. Try again!";
            } else {
                message = "ru".equals(currentLanguage) ?
                        "В следующий раз обязательно получится!" :
                        "Next time you'll definitely win!";
            }
        }

        tvMessage.setText(message);
    }


}
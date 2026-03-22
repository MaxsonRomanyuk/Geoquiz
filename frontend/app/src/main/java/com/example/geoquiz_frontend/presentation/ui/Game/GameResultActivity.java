package com.example.geoquiz_frontend.presentation.ui.Game;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.presentation.ui.Home.MainActivity;
import com.google.android.material.button.MaterialButton;

public class GameResultActivity extends BaseActivity {
    private TextView tvResultIcon, tvCorrectAnswers, tvScore;
    private TextView tvMessage, tvTime, tvAccuracy;
    private MaterialButton btnRestart, btnMainMenu;
    int score;
    int correctAnswers;
    int totalQuestions;
    int timeSpent;
    int gameMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solo_results);

        score = getIntent().getIntExtra("SCORE", 0);
        correctAnswers = getIntent().getIntExtra("CORRECT", 0);
        totalQuestions = getIntent().getIntExtra("TOTAL", 10);
        timeSpent = getIntent().getIntExtra("TIME", 0);
        gameMode = getIntent().getIntExtra("GAME_MODE", 1);

        initViews();
        setupClickListeners();
        updateUI();
    }

    private void initViews() {
        tvResultIcon = findViewById(R.id.iv_result_icon);
        tvCorrectAnswers = findViewById(R.id.tv_correct_answers);
        tvScore = findViewById(R.id.tv_score);
        tvMessage = findViewById(R.id.tv_message);
        tvTime = findViewById(R.id.tv_time);
        tvAccuracy = findViewById(R.id.tv_accuracy);
        btnRestart = findViewById(R.id.btn_restart);
        btnMainMenu = findViewById(R.id.btn_main_menu);
    }
    private void setupClickListeners() {
        btnRestart.setOnClickListener(v -> {
            Intent intent = new Intent(this, SoloGameActivity.class);
            intent.putExtra("GAME_MODE", gameMode);
            startActivity(intent);
        });

        btnMainMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
    private void updateUI() {

        tvCorrectAnswers.setText(correctAnswers + " / " + totalQuestions);

        tvScore.setText(String.valueOf(score));

        if (correctAnswers == totalQuestions) {
            tvResultIcon.setText("🏆");
            tvMessage.setText(getString(R.string.excellent_result));

        } else if (correctAnswers >= totalQuestions * 0.8f) {
            tvResultIcon.setText("🎉");
            tvMessage.setText(getString(R.string.great_result));

        } else if (correctAnswers >= totalQuestions * 0.5f) {
            tvResultIcon.setText("👍");
            tvMessage.setText(getString(R.string.good_result));

        } else {
            tvResultIcon.setText("💪");
            tvMessage.setText(getString(R.string.try_again));
        }

        int seconds = timeSpent / 1000;
        tvTime.setText(seconds + " сек");

        int accuracy = 0;
        if (totalQuestions > 0) {
            accuracy = (int) ((correctAnswers * 100f) / totalQuestions);
        }
        tvAccuracy.setText(accuracy + "%");
    }
}

package com.example.geoquiz_frontend.presentation.ui.King;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.presentation.ui.Home.MainActivity;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.remote.KothSignalRClientManager;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.google.android.material.button.MaterialButton;

public class KingEliminatedActivity extends BaseActivity {

    private TextView tvPlace, tvRoundsSurvived, tvCorrectAnswers, tvTotalScore;
    private TextView tvPlayerName, tvMatchMessage, tvManualDisableMessage;
    private MaterialButton btnExit;

    private String playerName;
    private int roundsSurvived;
    private int place;
    private int correctAnswers;
    private int totalScore;
    private boolean isManuallyDisabled;
    private int totalPlayers;
    private KothSignalRClientManager signalRManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_king_eliminated);

        preferencesHelper = new SecurePreferencesHelper(this);
        signalRManager = KothSignalRClientManager.getInstance();

        getIntentData();
        initViews();
        setupClickListeners();
        displayPlayerData();
        updateStats();
    }

    private void getIntentData() {
        Intent intent = getIntent();

        playerName = intent.getStringExtra("player_name");
        if (playerName == null) {
            playerName = "Player";
        }

        roundsSurvived = intent.getIntExtra("rounds_survived", 0);
        place = intent.getIntExtra("place", 0);
        correctAnswers = intent.getIntExtra("correct_answers", 0);
        totalScore = intent.getIntExtra("total_score", 0);
        isManuallyDisabled = intent.getBooleanExtra("is_manually_disabled", false);
        totalPlayers = intent.getIntExtra("total_players", 32);
    }

    private void initViews() {
        tvPlace = findViewById(R.id.tvPlace);
        tvRoundsSurvived = findViewById(R.id.tvRoundsSurvived);
        tvCorrectAnswers = findViewById(R.id.tvCorrectAnswers);
        tvTotalScore = findViewById(R.id.tvTotalScore);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvMatchMessage = findViewById(R.id.tvMatchMessage);
        tvManualDisableMessage = findViewById(R.id.tvManualDisableMessage);
        btnExit = findViewById(R.id.btnExit);
    }

    private void setupClickListeners() {
        btnExit.setOnClickListener(v -> exitToMainMenu());
    }

    private void displayPlayerData() {
        String currentLanguage = preferencesHelper.getLanguage();

        String placeText = "ru".equals(currentLanguage)
                ? place + " место из " + totalPlayers
                : "Place " + place + " of " + totalPlayers;
        tvPlace.setText(placeText);

        tvRoundsSurvived.setText(String.valueOf(roundsSurvived));
        tvCorrectAnswers.setText(String.valueOf(correctAnswers));
        tvTotalScore.setText(String.valueOf(totalScore));

        tvPlayerName.setText(playerName);

        if (isManuallyDisabled) {
            tvManualDisableMessage.setVisibility(View.VISIBLE);
            tvMatchMessage.setVisibility(View.GONE);
        } else {
            tvManualDisableMessage.setVisibility(View.GONE);
            tvMatchMessage.setVisibility(View.VISIBLE);
        }
    }
    private void updateStats()
    {
        UserRepository userRepository = UserRepository.getInstance(this);
        userRepository.loadUserData(true);
    }
    private void exitToMainMenu() {
        if (signalRManager != null) {
            signalRManager.stop();
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        exitToMainMenu();
    }
}
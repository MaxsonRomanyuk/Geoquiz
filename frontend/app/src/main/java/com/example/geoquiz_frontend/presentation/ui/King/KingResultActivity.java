package com.example.geoquiz_frontend.presentation.ui.King;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.presentation.ui.Home.MainActivity;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.remote.KothSignalRClientManager;
import com.example.geoquiz_frontend.data.remote.dtos.koth.MatchFinishedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerFinalStanding;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import java.util.List;

public class KingResultActivity extends BaseActivity {

    private TextView tvWinnerName;
    private LinearLayout layoutStandings;
    private MaterialButton btnMainMenu;

    private MatchFinishedData matchData;
    private String currentUserId;
    private KothSignalRClientManager signalRManager;
    private PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_king_results);

        preferencesHelper = new PreferencesHelper(this);
        signalRManager = KothSignalRClientManager.getInstance();
        currentUserId = preferencesHelper.getUserId();

        initViews();
        getIntentData();
        setupClickListeners();
        displayResults();
        updateStats();
    }

    private void initViews() {
        tvWinnerName = findViewById(R.id.tvWinnerName);
        layoutStandings = findViewById(R.id.layoutStandings);
        btnMainMenu = findViewById(R.id.btnMainMenu);
    }

    private void getIntentData() {
        Intent intent = getIntent();

        String matchDataJson = intent.getStringExtra("match_data");

        if (matchDataJson != null) {
            Gson gson = new Gson();
            matchData = gson.fromJson(matchDataJson, MatchFinishedData.class);
        }

        if (matchData == null) {
            createDemoData();
        }
    }

    private void createDemoData() {
        matchData = new MatchFinishedData();
    }

    private void setupClickListeners() {
        btnMainMenu.setOnClickListener(v -> exitToMainMenu());
    }

    private void displayResults() {
        if (matchData == null || matchData.getFinalStandings() == null) {
            return;
        }

        List<PlayerFinalStanding> standings = matchData.getFinalStandings();

        PlayerFinalStanding winner = null;
        for (PlayerFinalStanding player : standings) {
            if (player.getPlace() == 1) {
                winner = player;
                break;
            }
        }

        if (winner != null) {
            tvWinnerName.setText(winner.getPlayerName());
        } else {
            tvWinnerName.setText("Winner");
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (PlayerFinalStanding player : standings) {
            View itemView = inflater.inflate(R.layout.item_standing_row, layoutStandings, false);

            TextView tvPlace = itemView.findViewById(R.id.tvPlace);
            TextView tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            TextView tvRounds = itemView.findViewById(R.id.tvRounds);
            TextView tvCorrect = itemView.findViewById(R.id.tvCorrect);
            TextView tvScore = itemView.findViewById(R.id.tvScore);
            TextView tvCrown = itemView.findViewById(R.id.tvCrown);
            View vSelfIndicator = itemView.findViewById(R.id.vSelfIndicator);
            TextView tvYouBadge = itemView.findViewById(R.id.tvYouBadge);

            tvPlace.setText(String.valueOf(player.getPlace()));

            tvPlayerName.setText(player.getPlayerName());

            tvRounds.setText(String.valueOf(player.getRoundsSurvived()));
            tvCorrect.setText(String.valueOf(player.getCorrectAnswers()));
            tvScore.setText(String.valueOf(player.getTotalScore()));

            if (player.getPlace() == 1) {
                tvCrown.setVisibility(View.VISIBLE);
            } else {
                tvCrown.setVisibility(View.GONE);
            }

            if (player.getPlayerId() != null && player.getPlayerId().equals(currentUserId)) {
                itemView.setBackgroundResource(R.drawable.bg_row_self);
                itemView.setAlpha(1f);

                vSelfIndicator.setVisibility(View.VISIBLE);
                tvYouBadge.setVisibility(View.VISIBLE);
            }

            layoutStandings.addView(itemView);
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

    private int getColorFromAttr(int attrResId) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }

    @Override
    public void onBackPressed() {
        exitToMainMenu();
    }
}
package com.example.geoquiz_frontend.presentation.ui.history;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.remote.ApiClient;
import com.example.geoquiz_frontend.data.remote.ApiService;
import com.example.geoquiz_frontend.data.remote.dtos.history.GameHistoryResponse;
import com.example.geoquiz_frontend.data.remote.dtos.history.GameSessionDto;
import com.example.geoquiz_frontend.data.remote.dtos.history.PageRequest;
import com.example.geoquiz_frontend.presentation.ui.base.BaseActivity;
import com.example.geoquiz_frontend.presentation.ui.home.MainActivity;
import com.example.geoquiz_frontend.presentation.ui.leaderboard.LeaderboardActivity;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchHistoryActivity extends BaseActivity {

    private ImageView ivBack;
    private TextView tvTotalMatches, tvWinRate, tvPageInfo;
    private RecyclerView recyclerMatchHistory;
    private MaterialButton btnPrev, btnNext;
    private ProgressBar progressBar;

    private MatchHistoryAdapter adapter;
    private List<MatchHistoryEntry> currentMatches = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;
    private int totalMatches = 0;
    private int totalWins = 0;
    private long serverTimeOffset = 0;

    private ApiService apiServiceWithAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_history);

        preferencesHelper = new SecurePreferencesHelper(this);

        initViews();
        setupClickListeners();
        setupRecyclerView();
        loadMatchHistory(1);
        hideSystemBars();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvTotalMatches = findViewById(R.id.tvTotalMatches);
        tvWinRate = findViewById(R.id.tvWinRate);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        recyclerMatchHistory = findViewById(R.id.recyclerMatchHistory);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 1) {
                loadMatchHistory(currentPage - 1);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                loadMatchHistory(currentPage + 1);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new MatchHistoryAdapter(this, currentMatches, serverTimeOffset);
        recyclerMatchHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerMatchHistory.setAdapter(adapter);
    }

    private void loadMatchHistory(int page) {
        progressBar.setVisibility(View.VISIBLE);
        if (preferencesHelper.hasValidAccessToken()) {
            apiServiceWithAuth = ApiClient.getApiWithAuth();
        }
        else {
            showError();
            return;
        }
        apiServiceWithAuth.getGameHistory(page).enqueue(new Callback<GameHistoryResponse>() {
            @Override
            public void onResponse(Call<GameHistoryResponse> call, Response<GameHistoryResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    GameHistoryResponse data = response.body();

                    long serverTime = data.getServerTime();
                    long deviceTime = System.currentTimeMillis();
                    serverTimeOffset = serverTime - deviceTime;

                    totalMatches = data.getTotalCount();
                    totalPages = data.getTotalPages();
                    totalWins = data.getTotalWins();
                    currentPage = page;

                    updateStats();
                    updatePagination();

                    currentMatches.clear();
                    currentMatches.addAll(convertToEntries(data.getMatches()));
                    adapter.updateList(currentMatches, serverTimeOffset);

                    recyclerMatchHistory.smoothScrollToPosition(0);
                } else {
                    showError();
                }
            }

            @Override
            public void onFailure(Call<GameHistoryResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError();
            }
        });
    }

    private void updateStats() {
        tvTotalMatches.setText(String.valueOf(totalMatches));
        int winRate = totalMatches == 0 ? 0 : (totalWins * 100 / totalMatches);
        tvWinRate.setText(winRate + "%");
    }

    private void updatePagination() {
        tvPageInfo.setText(getString(R.string.page_template, currentPage, totalPages));

        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);

        if (currentPage > 1) {
            btnPrev.setBackgroundTintList(getColorStateList(R.color.white));
            btnPrev.setStrokeColor(getColorStateList(R.color.primary));
            btnPrev.setTextColor(getColor(R.color.primary));
        } else {
            btnPrev.setBackgroundTintList(getColorStateList(R.color.surface));
            btnPrev.setStrokeColor(getColorStateList(R.color.text_secondary));
            btnPrev.setTextColor(getColor(R.color.text_primary));
        }

        if (currentPage < totalPages) {
            btnNext.setBackgroundTintList(getColorStateList(R.color.primary));
            btnNext.setTextColor(getColor(R.color.white));
        } else {
            btnNext.setBackgroundTintList(getColorStateList(R.color.surface));
            btnNext.setStrokeWidth(1);
            btnNext.setStrokeColor(getColorStateList(R.color.text_secondary));
            btnNext.setTextColor(getColor(R.color.text_primary));
        }
    }

    private List<MatchHistoryEntry> convertToEntries(List<GameSessionDto> sessions) {
        List<MatchHistoryEntry> entries = new ArrayList<>();
        for (GameSessionDto session : sessions) {
            entries.add(MatchHistoryEntry.fromDto(session, serverTimeOffset));
        }
        return entries;
    }

    private void showError() {
        String errorMessage = preferencesHelper.getLanguage().equals("ru") ? "Не удалось загрузить историю матчей" : "Failed to load match history";
        Toast.makeText(MatchHistoryActivity.this, errorMessage, Toast.LENGTH_SHORT ).show();
    }

    private int getCustomColor(int colorResId) {
        return ContextCompat.getColor(this, colorResId);
    }

    private void hideSystemBars() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
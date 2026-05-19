package com.example.geoquiz_frontend.presentation.ui.leaderboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.remote.ApiClient;
import com.example.geoquiz_frontend.data.remote.ApiService;
import com.example.geoquiz_frontend.data.remote.dtos.profile.LeaderboardDto;
import com.example.geoquiz_frontend.presentation.ui.base.BaseActivity;
import com.example.geoquiz_frontend.presentation.ui.soloGame.GameTypesActivity;
import com.example.geoquiz_frontend.presentation.ui.home.MainActivity;
import com.example.geoquiz_frontend.presentation.ui.profile.ProfileActivity;
import com.example.geoquiz_frontend.presentation.ui.achievements.AchievementsActivity;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardActivity extends BaseActivity {
    private TextView tvYourRank, tvYourScore;
    private RecyclerView recyclerLeaderboard;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigationView;

    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> allEntries = new ArrayList<>();
    private final int PAGE_SIZE = 100;
    private int userRank = -1;
    private int userScore = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        preferencesHelper = new SecurePreferencesHelper(this);

        initViews();
        setupBottomNavigation();
        setupRecyclerView();
        loadLeaderboardData();
    }

    private void initViews() {
        tvYourRank = findViewById(R.id.tvYourRank);
        tvYourScore = findViewById(R.id.tvYourScore);
        recyclerLeaderboard = findViewById(R.id.recyclerLeaderboard);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }
    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_play) {
                Intent intent = new Intent(this, GameTypesActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_achievements) {
                Intent intent = new Intent(this, AchievementsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                return true;
            } else if (itemId == R.id.nav_profile){
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_leaderboard);
    }
    private void setupRecyclerView() {
        String currentUserId = preferencesHelper.getUserId();
        adapter = new LeaderboardAdapter(this, new ArrayList<>(), currentUserId);
        recyclerLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        recyclerLeaderboard.setAdapter(adapter);
    }

    private void loadLeaderboardData() {
        progressBar.setVisibility(View.VISIBLE);
        if (!preferencesHelper.hasValidAccessToken()) return;
        String errorMessage = preferencesHelper.getLanguage().equals("ru") ? "Не удалось загрузить таблицу лидеров" : "Failed to load leaderboard";
        ApiService api = ApiClient.getApiWithAuth();
        api.getLeaderboard().enqueue(new Callback<LeaderboardDto>() {
            @Override
            public void onResponse(Call<LeaderboardDto> call, Response<LeaderboardDto> response) {
                LeaderboardDto leaderboardDto = response.body();
                if (leaderboardDto != null)
                {
                    if (leaderboardDto.getYourRank() != 0 && leaderboardDto.getLeaderboardEntries() != null)
                    {
                        userRank = leaderboardDto.getYourRank();
                        userScore = leaderboardDto.getYourScore();
                        allEntries = leaderboardDto.getLeaderboardEntries();
                        loadPage(1);
                        progressBar.setVisibility(View.GONE);
                        updateUserStats();
                    }
                    else
                    {
                        Toast.makeText(LeaderboardActivity.this, errorMessage, Toast.LENGTH_SHORT ).show();
                    }
                }
                else {
                    Toast.makeText(LeaderboardActivity.this, errorMessage, Toast.LENGTH_SHORT ).show();
                }
            }

            @Override
            public void onFailure(Call<LeaderboardDto> call, Throwable t) {
                Toast.makeText(LeaderboardActivity.this, errorMessage, Toast.LENGTH_SHORT ).show();
            }
        });
    }

    private void loadPage(int page) {
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allEntries.size());

        List<LeaderboardEntry> pageEntries = allEntries.subList(start, end);
        adapter.updateList(allEntries);

        if (userRank < end) recyclerLeaderboard.smoothScrollToPosition(userRank);
        else recyclerLeaderboard.smoothScrollToPosition(0);
    }
    private void updateUserStats() {
        if (userRank != -1) {
            tvYourRank.setText(String.valueOf(userRank));
            tvYourScore.setText(String.valueOf(userScore));
        } else {
            tvYourRank.setText("—");
            tvYourScore.setText("0");
        }
    }
}
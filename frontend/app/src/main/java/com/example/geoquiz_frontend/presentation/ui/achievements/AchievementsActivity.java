package com.example.geoquiz_frontend.presentation.ui.achievements;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.domain.entities.Achievement;
import com.example.geoquiz_frontend.domain.enums.AchievementCategory;
import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;
import com.example.geoquiz_frontend.presentation.ui.Game.GameTypesActivity;
import com.example.geoquiz_frontend.presentation.ui.Home.MainActivity;
import com.example.geoquiz_frontend.presentation.ui.Profile.ProfileActivity;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementsActivity extends BaseActivity {
    private TextView tvUnlockedCount, tvTotalCount;
    private TabLayout tabLayout;
    private RecyclerView recyclerAchievements;
    private BottomNavigationView bottomNavigationView;
    private UserRepository userRepository;
    private List<Achievement> allAchievements = new ArrayList<>();
    private List<String> unlockedAchievementIds = new ArrayList<>();
    private AchievementAdapter adapter;
    private Map<String, Integer> categoryOrder = new HashMap<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        preferencesHelper = new SecurePreferencesHelper(this);
        userRepository = UserRepository.getInstance(this);

        initViews();
        setupClickListeners();
        setupCategories();
        setupRecyclerView();

        loadDataFromDatabase();
        observeUserData();
    }

    private void initViews() {
        tvUnlockedCount = findViewById(R.id.tvUnlockedCount);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tabLayout = findViewById(R.id.tabLayout);
        recyclerAchievements = findViewById(R.id.recyclerAchievements);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }

    private void setupClickListeners() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            } else if (itemId == R.id.nav_play) {
                Intent intent = new Intent(this, GameTypesActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_achievements) {
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                // Navigate to leaderboard
                return true;
            } else if (itemId == R.id.nav_profile){
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_achievements);
    }
    private void loadDataFromDatabase() {
        String userId = preferencesHelper.getUserId();
        if (userId != null) {
            List<Achievement> cached = userRepository.getFullAchievements(userId);
            if (cached != null && !cached.isEmpty()) {
                loadAchievementsData(cached);
            }
        }

        userRepository.loadUserData(false);
    }
    private void observeUserData() {
        userRepository.getAchievements().observe(this, achievements -> {
            if (achievements != null) {
                loadAchievementsData(achievements);
            }
        });

        userRepository.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadAchievementsData(List<Achievement> achievements) {
        allAchievements = achievements;
        unlockedAchievementIds = getUnlockedAchievementIds();

        updateStats();

        int selectedTab = tabLayout.getSelectedTabPosition();
        if (selectedTab >= 0) {
            filterAchievementsByCategory(selectedTab + 1);
        } else {
            filterAchievementsByCategory(1);
        }
    }

    private List<String> getUnlockedAchievementIds() {
        List<String> unlocked = new ArrayList<>();
        for (Achievement achievement: allAchievements) {
            if (achievement.isUnlocked) unlocked.add(achievement.code);
        }
        return unlocked;
    }

    private void setupCategories() {
        String language = preferencesHelper.getLanguage();
        for (AchievementCategory cat : AchievementCategory.values()) {
            tabLayout.addTab(tabLayout.newTab().setText(language.equals("ru")? cat.getTitle().getRu() : cat.getTitle().getEn()));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int categoryId = tab.getPosition() + 1;
                filterAchievementsByCategory(categoryId);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterAchievementsByCategory(int categoryId) {
        List<Achievement> unlocked = new ArrayList<>();
        List<Achievement> locked = new ArrayList<>();

        for (Achievement achievement : allAchievements) {
            if (achievement.category == categoryId) {
                if (unlockedAchievementIds.contains(achievement.code)) {
                    unlocked.add(achievement);
                } else {
                    locked.add(achievement);
                }
            }
        }

        List<Achievement> filtered = new ArrayList<>();
        filtered.addAll(unlocked);
        filtered.addAll(locked);

        adapter.updateList(filtered);
    }

    private void setupRecyclerView() {
        adapter = new AchievementAdapter(this, new ArrayList<>());
        recyclerAchievements.setLayoutManager(new LinearLayoutManager(this));
        recyclerAchievements.setAdapter(adapter);
        recyclerAchievements.setNestedScrollingEnabled(true);
    }

    private void updateStats() {

        int unlocked = unlockedAchievementIds.size();
        int total = allAchievements.size();
        tvUnlockedCount.setText(String.valueOf(unlocked));
        tvTotalCount.setText(String.valueOf(total));
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
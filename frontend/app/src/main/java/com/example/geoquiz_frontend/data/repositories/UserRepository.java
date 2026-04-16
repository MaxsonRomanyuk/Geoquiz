package com.example.geoquiz_frontend.data.repositories;
import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.geoquiz_frontend.data.remote.ApiClient;
import com.example.geoquiz_frontend.data.remote.ApiService;
import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.domain.entities.Achievement;
import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.example.geoquiz_frontend.domain.enums.LocalizedText;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class UserRepository {
    private static UserRepository instance;
    private MutableLiveData<ProfileResponse> userData = new MutableLiveData<>();
    private MutableLiveData<List<Achievement>> userAchievements = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final List<ProfileResponse.AchievementDto> pendingAchievements = new ArrayList<>();

    private ApiService apiService;
    private PreferencesHelper preferencesHelper;
    private DatabaseHelper databaseHelper;
    private static final String ACHIEVEMENTS_FILE = "achievements.json";
    private static Map<String, JSONObject> achievementsCache;
    private final Context context;

    private UserRepository(Context context) {
        this.context = context.getApplicationContext();
        preferencesHelper = new PreferencesHelper(context);
        databaseHelper = new DatabaseHelper(context);

        initAchievementsCache();

        if (preferencesHelper.hasValidToken()) {
            apiService = ApiClient.getApiWithAuth(preferencesHelper);
        }
    }

    public static synchronized UserRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UserRepository(context.getApplicationContext());
        }
        return instance;
    }
    public static synchronized void reset() {
        if (instance != null) {
            instance.clearData();
            instance = null;
        }
    }
    public LiveData<ProfileResponse> getUserData() {
        return userData;
    }
    public LiveData<List<Achievement>> getAchievements() {
        return userAchievements;
    }
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadUserData(boolean forceRefresh) {
        if (!forceRefresh && userData.getValue() != null && userAchievements.getValue() != null) {
            return;
        }

        isLoading.setValue(true);

        String userId = preferencesHelper.getUserId();
        if (userId == null) {
            errorMessage.setValue("User not logged in");
            isLoading.setValue(false);
            return;
        }

        if (!forceRefresh) {
            UserStats cachedStats = databaseHelper.getUserStats(userId);
            List<Achievement> achievements = getFullAchievements(userId); // for empty saved datenow
            if (cachedStats != null && achievements!= null) {
                ProfileResponse profileResponse = convertStatsToProfile(cachedStats);
                userData.setValue(profileResponse);
                userAchievements.setValue(achievements);
                isLoading.setValue(false);
                return;
            }
        }

        if (apiService == null) {
            errorMessage.setValue("No authentication");
            isLoading.setValue(false);
            return;
        }

        if (!userId.equals("uid")) {
            apiService.getProfile().enqueue(new Callback<ProfileResponse>() {
                @Override
                public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                    isLoading.setValue(false);
                    if (response.isSuccessful() && response.body() != null) {
                        ProfileResponse profile = response.body();

                        UserStats stats = convertProfileToStats(profile, userId);
                        databaseHelper.saveUserStats(stats);

                        List<ProfileResponse.AchievementDto> achievements = profile.getAchievements();
                        for (ProfileResponse.AchievementDto achievement : achievements) {
                            databaseHelper.saveUserAchievements(achievement, userId);
                        }

                        userAchievements.setValue(getFullAchievements(userId));
                        userData.setValue(profile);
                    } else {
                        errorMessage.setValue("Error loading data: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ProfileResponse> call, Throwable t) {
                    isLoading.setValue(false);
                    errorMessage.setValue("Network error: " + t.getMessage());
                }
            });
        }
    }
    public void refreshFromDb() {
        String userId = preferencesHelper.getUserId();
        if (userId == null) return;

        UserStats cachedStats = databaseHelper.getUserStats(userId);
        if (cachedStats != null) {
            ProfileResponse profileResponse = convertStatsToProfile(cachedStats);
            userData.setValue(profileResponse);
        }
    }
    private ProfileResponse convertStatsToProfile(UserStats stats) {
        ProfileResponse profile = new ProfileResponse();

        ProfileResponse.UserDto user = new ProfileResponse.UserDto();
        user.setId(stats.getUserId());
        user.setUserName(preferencesHelper.getUserName());
        user.setEmail(preferencesHelper.getUserEmail());

        ProfileResponse.StatsDto statsDto = new ProfileResponse.StatsDto();
        statsDto.setGamesPlayed(stats.getGamesPlayed());
        statsDto.setWins(stats.getGamesWon());
        statsDto.setWinRate(stats.getWinRate());
        statsDto.setLevel(stats.getLevel());
        statsDto.setExperience(stats.getExperience());
        statsDto.setDailyStreak(stats.getDailyStreak());
        statsDto.setCurrentWinStreak(stats.getWinStreak());

        ProfileResponse.GeographyDto geo = new ProfileResponse.GeographyDto();
        geo.setEuropeCorrect(stats.getEuropeCorrect());
        geo.setAsiaCorrect(stats.getAsiaCorrect());
        geo.setAfricaCorrect(stats.getAfricaCorrect());
        geo.setAmericaCorrect(stats.getAmericaCorrect());
        geo.setOceaniaCorrect(stats.getOceaniaCorrect());

        profile.setUser(user);
        profile.setStats(statsDto);
        profile.setGeography(geo);

        return profile;
    }
    public void clearData() {
        userData.setValue(null);
        userAchievements.setValue(null);
        isLoading.setValue(false);
        errorMessage.setValue(null);
    }

    private UserStats convertProfileToStats(ProfileResponse profile, String userId) {
        UserStats stats = new UserStats(userId);

        ProfileResponse.StatsDto s = profile.getStats();
        stats.setGamesPlayed(s.getGamesPlayed());
        stats.setGamesWon(s.getWins());
        stats.setWinRate((float) s.getWinRate());
        stats.setLevel(s.getLevel());
        stats.setExperience(s.getExperience());
        stats.setDailyStreak(s.getDailyStreak());
        stats.setWinStreak(s.getCurrentWinStreak());

        ProfileResponse.GeographyDto g = profile.getGeography();
        stats.setEuropeCorrect(g.getEuropeCorrect());
        stats.setAsiaCorrect(g.getAsiaCorrect());
        stats.setAfricaCorrect(g.getAfricaCorrect());
        stats.setAmericaCorrect(g.getAmericaCorrect());
        stats.setOceaniaCorrect(g.getOceaniaCorrect());
        stats.setBestContinent(g.getBestContinent());

        if (profile.getGameModes() != null) {
            stats.setCapitalsCorrect(profile.getGameModes().getCapitalsCorrect());
            stats.setFlagsCorrect(profile.getGameModes().getFlagsCorrect());
            stats.setOutlinesCorrect(profile.getGameModes().getOutlinesCorrect());
            stats.setLanguagesCorrect(profile.getGameModes().getLanguagesCorrect());
        }

        return stats;
    }
    public void unlockAchievement(ProfileResponse.AchievementDto achievementDto)
    {
        databaseHelper.updateUserAchievement(achievementDto);
        //pendingAchievements.add(achievementDto);
        List<Achievement> achievements = getFullAchievements(achievementDto.getUserId());
        userAchievements.setValue(achievements);
    }
    public void savePendingAchievements(ProfileResponse.AchievementDto achievementDto)
    {
        pendingAchievements.add(achievementDto);
    }
    public List<ProfileResponse.AchievementDto> consumePendingAchievements() {
        List<ProfileResponse.AchievementDto> copy = new ArrayList<>(pendingAchievements);
        clearPendingAchievements();
        return copy;
    }
    public void clearPendingAchievements(){
        pendingAchievements.clear();
    }
    public List<Achievement> getFullAchievements(String userId, List<ProfileResponse.AchievementDto> achievements) {
        if (achievements == null) return null;
        List<Achievement> fullAchievements = new ArrayList<>();

        for (ProfileResponse.AchievementDto dto : achievements) {
            fullAchievements.add(convertDtoToAchievement(dto));
        }

        return fullAchievements;
    }
    public List<Achievement> getFullAchievements(String userId) {
        List<ProfileResponse.AchievementDto> dtos = databaseHelper.getUserAchievements(userId);
        if (dtos == null) return null;
        List<Achievement> fullAchievements = new ArrayList<>();

        for (ProfileResponse.AchievementDto dto : dtos) {
            fullAchievements.add(convertDtoToAchievement(dto));
        }

        return fullAchievements;
    }
    private void initAchievementsCache() {
        if (achievementsCache != null) return;

        achievementsCache = new HashMap<>();
        try {
            String jsonString = readJsonFromAssets(ACHIEVEMENTS_FILE);
            JSONObject root = new JSONObject(jsonString);
            JSONArray achievements = root.getJSONArray("achievements");

            for (int i = 0; i < achievements.length(); i++) {
                JSONObject achievement = achievements.getJSONObject(i);
                String code = achievement.getString("code");
                achievementsCache.put(code, achievement);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to init achievements cache", e);
        }
    }
    private String readJsonFromAssets(String fileName) throws IOException {
        InputStream is = context.getAssets().open(fileName);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }
    private Achievement convertDtoToAchievement(ProfileResponse.AchievementDto achievementDto)
    {
        Achievement achievement = new Achievement(
                achievementDto.getCode(),
                null,
                null,
                0,
                achievementDto.getRarity(),
                achievementDto.getProgress(),
                achievementDto.isUnlocked(),
                dateToString(achievementDto.getUnlockedAt()),
                null
        );

        fillFromCache(achievement);
        return achievement;
    }
    private void fillFromCache(Achievement achievement) {
        if (achievementsCache == null) {
            Log.d(TAG, "Achievements cache not initialized");
            return;
        }

        JSONObject json = achievementsCache.get(achievement.code);
        if (json == null) {
            Log.d(TAG, "Achievement not found in cache: " + achievement.code);
            return;
        }

        try {
            achievement.category = json.optInt("category", 0);
            achievement.icon = json.optString("icon", "");

            JSONObject titleJson = json.optJSONObject("title");
            if (titleJson != null) {
                achievement.title = new LocalizedText(
                        titleJson.optString("ru", ""),
                        titleJson.optString("en", "")
                );
            }

            JSONObject descJson = json.optJSONObject("description");
            if (descJson != null) {
                achievement.description = new LocalizedText(
                        descJson.optString("ru", ""),
                        descJson.optString("en", "")
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filling achievement from cache: " + achievement.code, e);
        }
    }
    private String dateToString(Date unlockedAt)
    {
        if (unlockedAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            return sdf.format(unlockedAt);
        } else {
            return  "";
        }
    }
}

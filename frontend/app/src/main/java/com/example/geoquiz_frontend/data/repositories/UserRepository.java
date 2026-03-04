package com.example.geoquiz_frontend.data.repositories;
import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.geoquiz_frontend.data.remote.ApiClient;
import com.example.geoquiz_frontend.data.remote.ApiService;
import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.example.geoquiz_frontend.Presentation.utils.PreferencesHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class UserRepository {
    private static UserRepository instance;
    private MutableLiveData<ProfileResponse> userData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private ApiService apiService;
    private PreferencesHelper preferencesHelper;
    private DatabaseHelper databaseHelper;

    private UserRepository(Context context) {
        preferencesHelper = new PreferencesHelper(context);
        databaseHelper = new DatabaseHelper(context);
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

    public LiveData<ProfileResponse> getUserData() {
        return userData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadUserData(boolean forceRefresh) {
        if (!forceRefresh && userData.getValue() != null) {
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
            if (cachedStats != null) {
                ProfileResponse profileResponse = convertStatsToProfile(cachedStats);
                userData.setValue(profileResponse);
                isLoading.setValue(false);
                return;
            }
        }

        if (apiService == null) {
            errorMessage.setValue("No authentication");
            isLoading.setValue(false);
            return;
        }

        apiService.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse profile = response.body();

                    UserStats stats = convertProfileToStats(profile, userId);
                    databaseHelper.saveUserStats(stats);

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
        preferencesHelper.clearCurrentUser();
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
}

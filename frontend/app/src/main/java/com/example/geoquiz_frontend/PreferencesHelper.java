package com.example.geoquiz_frontend;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.geoquiz_frontend.DTOs.ProfileResponse;
import com.example.geoquiz_frontend.Entities.User;

import java.util.Date;

public class PreferencesHelper {
    private static final String PREFS_NAME = "QuizPreferences";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_CURRENT_USER_NAME = "current_user_name";
    private static final String KEY_CURRENT_USER_EMAIL = "current_user_email";

    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";


    private static final String KEY_DAILY_STREAK = "daily_streak";
    private static final String KEY_USER_LEVEL = "user_level";
    private static final String KEY_CURRENT_XP = "current_xp";
    private static final String KEY_GAMES_PLAYED = "games_played";
    private static final String KEY_GAMES_WIN = "games_win";
    private static final String KEY_WIN_RATE = "games_win_rate";
    private static final String KEY_WIN_STREAK = "games_win_streak";
    private static final String KEY_BEST_CONTINENT = "best_continent";
    private static final String KEY_EUROPE_CORRECT = "europe_correct";
    private static final String KEY_ASIA_CORRECT = "asia_correct";
    private static final String KEY_AFRICA_CORRECT = "africa_correct";
    private static final String KEY_AMERICA_CORRECT = "america_correct";
    private static final String KEY_OCEANIA_CORRECT = "oceania_correct";
    private SharedPreferences sharedPreferences;
    public PreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setCurrentUser(User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CURRENT_USER_ID, user.getId());
        editor.putString(KEY_CURRENT_USER_NAME, user.getName());
        editor.putString(KEY_CURRENT_USER_EMAIL, user.getEmail());
        editor.apply();
    }

    public User getCurrentUser() {
        String name = sharedPreferences.getString(KEY_CURRENT_USER_NAME, null);
        if (name == null) {
            return null;
        }

        User user = new User();
        user.setId(sharedPreferences.getString(KEY_CURRENT_USER_ID, "0"));
        user.setName(sharedPreferences.getString(KEY_CURRENT_USER_NAME, "Гость"));
        user.setEmail(sharedPreferences.getString(KEY_CURRENT_USER_EMAIL, ""));

        return user;
    }

    public void clearCurrentUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_CURRENT_USER_ID);
        editor.remove(KEY_CURRENT_USER_NAME);
        editor.remove(KEY_CURRENT_USER_EMAIL);
        editor.remove(KEY_AUTH_TOKEN);
        editor.remove(KEY_TOKEN_EXPIRY);

        editor.remove(KEY_GAMES_PLAYED);
        editor.remove(KEY_GAMES_WIN);
        editor.remove(KEY_WIN_RATE);
        editor.remove(KEY_USER_LEVEL);
        editor.remove(KEY_CURRENT_XP);
        editor.remove(KEY_DAILY_STREAK);
        editor.remove(KEY_WIN_STREAK);

        editor.remove(KEY_EUROPE_CORRECT);
        editor.remove(KEY_ASIA_CORRECT);
        editor.remove(KEY_AFRICA_CORRECT);
        editor.remove(KEY_AMERICA_CORRECT);
        editor.remove(KEY_OCEANIA_CORRECT);
        editor.remove(KEY_BEST_CONTINENT);
        editor.apply();
    }
    public void setCurrentUserStats(ProfileResponse.UserDto user, ProfileResponse.StatsDto stats, ProfileResponse.GeographyDto geography) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CURRENT_USER_NAME, user.getUserName());
        editor.putInt(KEY_DAILY_STREAK, stats.getDailyStreak());
        editor.putInt(KEY_USER_LEVEL, stats.getLevel());
        editor.putInt(KEY_CURRENT_XP, stats.getExperience());
        editor.putInt(KEY_GAMES_PLAYED, stats.getGamesPlayed());
        editor.putInt(KEY_GAMES_WIN, stats.getWins());
        editor.putFloat(KEY_WIN_RATE, (float) stats.getWinRate());
        editor.putInt(KEY_WIN_STREAK, stats.getCurrentWinStreak());
        editor.putInt(KEY_EUROPE_CORRECT, geography.getEuropeCorrect());
        editor.putInt(KEY_AFRICA_CORRECT, geography.getAfricaCorrect());
        editor.putInt(KEY_AMERICA_CORRECT, geography.getAmericaCorrect());
        editor.putInt(KEY_ASIA_CORRECT, geography.getAsiaCorrect());
        editor.putInt(KEY_OCEANIA_CORRECT, geography.getOceaniaCorrect());
        editor.putString(KEY_BEST_CONTINENT, geography.getBestContinent());
        editor.apply();
    }
    public ProfileResponse getCurrentUserStats() {
        String name = sharedPreferences.getString(KEY_CURRENT_USER_NAME, null);
        if (name == null) {
            return null;
        }

        ProfileResponse profileResponse = new ProfileResponse();

        ProfileResponse.UserDto user = new ProfileResponse.UserDto();
        user.setId(sharedPreferences.getString(KEY_CURRENT_USER_ID, "0"));
        user.setUserName(sharedPreferences.getString(KEY_CURRENT_USER_NAME, "Гость"));
        user.setEmail(sharedPreferences.getString(KEY_CURRENT_USER_EMAIL, ""));
        //long registeredAt = sharedPreferences.getLong(KEY_REGISTERED_AT, System.currentTimeMillis());
        //user.setRegisteredAt(new Date(registeredAt));

        ProfileResponse.StatsDto stats = new ProfileResponse.StatsDto();
        stats.setGamesPlayed(sharedPreferences.getInt(KEY_GAMES_PLAYED, 0));
        stats.setWins(sharedPreferences.getInt(KEY_GAMES_WIN, 0));
        stats.setWinRate(sharedPreferences.getFloat(KEY_WIN_RATE, 0f));
        stats.setLevel(sharedPreferences.getInt(KEY_USER_LEVEL, 1));
        stats.setExperience(sharedPreferences.getInt(KEY_CURRENT_XP, 0));
        stats.setDailyStreak(sharedPreferences.getInt(KEY_DAILY_STREAK, 0));
        stats.setCurrentWinStreak(sharedPreferences.getInt(KEY_WIN_STREAK, 0));

        ProfileResponse.GeographyDto geography = new ProfileResponse.GeographyDto();
        geography.setEuropeCorrect(sharedPreferences.getInt(KEY_EUROPE_CORRECT, 0));
        geography.setAsiaCorrect(sharedPreferences.getInt(KEY_ASIA_CORRECT, 0));
        geography.setAfricaCorrect(sharedPreferences.getInt(KEY_AFRICA_CORRECT, 0));
        geography.setAmericaCorrect(sharedPreferences.getInt(KEY_AMERICA_CORRECT, 0));
        geography.setOceaniaCorrect(sharedPreferences.getInt(KEY_OCEANIA_CORRECT, 0));

        profileResponse.setUser(user);
        profileResponse.setStats(stats);
        profileResponse.setGeography(geography);

        return profileResponse;
    }
    public void saveAuthToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_AUTH_TOKEN, token);

        long expiresAt = System.currentTimeMillis() + 86400000;
        editor.putLong(KEY_TOKEN_EXPIRY, expiresAt);
        editor.apply();
    }

    public String getAuthToken() {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null);
    }
    public boolean hasValidToken() {
        String token = getAuthToken();
        if (token == null) return false;

        long expiresAt = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
        long now = System.currentTimeMillis();

        return now < expiresAt;

        //return true;
    }
}

package com.example.geoquiz_frontend.presentation.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.geoquiz_frontend.domain.entities.User;

import java.util.Locale;
import java.util.Objects;

public class PreferencesHelper {
    private static final String PREFS_NAME = "QuizPreferences";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_CURRENT_USER_NAME = "current_user_name";
    private static final String KEY_CURRENT_USER_EMAIL = "current_user_email";

    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";

    private static final String KEY_APP_LANG = "app_language";
    private static final String KEY_APP_THEME = "app_theme";
    private static final String KEY_IS_PREMIUM = "is_premium";
    private static final String KEY_HIDE_CONNECTION_BANNER = "hide_connection_banner";
    private static final String KEY_BANNER_DISMISSED_TIME = "banner_dismissed_time";
    private static final long BANNER_COOLDOWN_MS = 24 * 60 * 60 * 1000;
    private SharedPreferences sharedPreferences;

    public PreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    public String getUserId() {
        return sharedPreferences.getString(KEY_CURRENT_USER_ID, null);
    }
    public String getUserName() {
        return sharedPreferences.getString(KEY_CURRENT_USER_NAME, "Гость");
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_CURRENT_USER_EMAIL, "");
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
        user.setPremium(sharedPreferences.getBoolean(KEY_IS_PREMIUM, false));

        return user;
    }
    public void setCurrentUser(User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CURRENT_USER_ID, user.getId());
        editor.putString(KEY_CURRENT_USER_NAME, user.getName());
        editor.putString(KEY_CURRENT_USER_EMAIL, user.getEmail());
        editor.putBoolean(KEY_IS_PREMIUM, user.isPremium());
        editor.apply();
    }
    public void clearCurrentUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_CURRENT_USER_ID);
        editor.remove(KEY_CURRENT_USER_NAME);
        editor.remove(KEY_CURRENT_USER_EMAIL);
        editor.remove(KEY_AUTH_TOKEN);
        editor.remove(KEY_TOKEN_EXPIRY);
        editor.remove(KEY_IS_PREMIUM);

        editor.apply();
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
        if (Objects.equals(sharedPreferences.getString(KEY_CURRENT_USER_ID, null), "uid")) return false;

        String token = getAuthToken();
        if (token == null) return false;

        long expiresAt = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
        long now = System.currentTimeMillis();

        return now < expiresAt;

        //return true;
    }
    public void setLanguage(String language) {
        sharedPreferences.edit().putString(KEY_APP_LANG, language).apply();
    }

    public String getLanguage() {
        return sharedPreferences.getString(KEY_APP_LANG, Locale.getDefault().getLanguage());
    }

    public void setTheme(String theme) {
        sharedPreferences.edit().putString(KEY_APP_THEME, theme).apply();
    }

    public String getTheme() {
        return sharedPreferences.getString(KEY_APP_THEME, "light");
    }
    public void setPremium(boolean isPremium) {
        sharedPreferences.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply();
    }
    public boolean isPremium() {
        return sharedPreferences.getBoolean(KEY_IS_PREMIUM, false);
    }
    public void hideConnectionBannerPermanently() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_HIDE_CONNECTION_BANNER, true);
        editor.apply();
    }

    public void hideConnectionBannerTemporarily() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_BANNER_DISMISSED_TIME, System.currentTimeMillis());
        editor.apply();
    }

    public boolean isConnectionBannerPermanentlyHidden() {
        return sharedPreferences.getBoolean(KEY_HIDE_CONNECTION_BANNER, false);
    }

    public boolean shouldShowBannerAfterCooldown() {
        long dismissedTime = sharedPreferences.getLong(KEY_BANNER_DISMISSED_TIME, 0);
        if (dismissedTime == 0) return true;
        return System.currentTimeMillis() - dismissedTime > BANNER_COOLDOWN_MS;
    }
}

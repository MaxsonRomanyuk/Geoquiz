package com.example.geoquiz_frontend.presentation.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.example.geoquiz_frontend.domain.entities.User;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Locale;

public class SecurePreferencesHelper {
    private final Context context;
    private static final String PREFS_NAME = "SecureQuizPreferences";
    private static final String ENCRYPTED_PREFS_NAME = "EncryptedQuizPreferences";

    private static final String KEY_APP_LANG = "app_language";
    private static final String KEY_APP_THEME = "app_theme";
    private static final String KEY_HIDE_CONNECTION_BANNER = "hide_connection_banner";
    private static final String KEY_BANNER_DISMISSED_TIME = "banner_dismissed_time";

    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_CURRENT_USER_NAME = "current_user_name";
    private static final String KEY_CURRENT_USER_EMAIL = "current_user_email";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    private static final String KEY_IS_PREMIUM = "is_premium";

    private static final long BANNER_COOLDOWN_MS = 24 * 60 * 60 * 1000;

    private SharedPreferences regularPrefs;
    private SharedPreferences encryptedPrefs;

    public SecurePreferencesHelper(Context context) {
        this.context = context.getApplicationContext();
        try {
            regularPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            String masterKeyAlias = MasterKeys.getOrCreate(
                    new KeyGenParameterSpec.Builder(
                            MasterKeys.AES256_GCM_SPEC.getKeystoreAlias(),
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .build()
            );

            encryptedPrefs = EncryptedSharedPreferences.create(
                    ENCRYPTED_PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            encryptedPrefs = context.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE);
        }
    }


    public void saveAuthTokens(String accessToken, String refreshToken, long expiresInSeconds) {
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putString(KEY_AUTH_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);

        long expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000);
        editor.putLong(KEY_TOKEN_EXPIRY, expiresAt);
        editor.apply();
    }

    public String getAuthToken() {
        return encryptedPrefs.getString(KEY_AUTH_TOKEN, null);
    }

    public String getRefreshToken() {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public long getTokenExpiry() {
        return encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0);
    }
    public long getTokenRemainingTime() {
        long expiresAt = getTokenExpiry();
        long now = System.currentTimeMillis();
        return expiresAt - now;
    }
    public boolean hasValidAccessToken() {
        if (getUserId().equals("uid")) return false;
        String token = getAuthToken();
        if (token == null) return false;

        long expiresAt = getTokenExpiry();
        long now = System.currentTimeMillis();

        return now < (expiresAt - 5 * 60 * 1000);
    }

    public void saveCurrentUser(User user) {
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putString(KEY_CURRENT_USER_ID, user.getId());
        editor.putString(KEY_CURRENT_USER_NAME, user.getName());
        editor.putString(KEY_CURRENT_USER_EMAIL, user.getEmail());
        editor.putBoolean(KEY_IS_PREMIUM, user.isPremium());
        editor.apply();
    }

    public User getCurrentUser() {
        String name = encryptedPrefs.getString(KEY_CURRENT_USER_NAME, null);
        if (name == null) {
            return null;
        }

        User user = new User();
        user.setId(encryptedPrefs.getString(KEY_CURRENT_USER_ID, "0"));
        user.setName(name);
        user.setEmail(encryptedPrefs.getString(KEY_CURRENT_USER_EMAIL, ""));
        user.setPremium(encryptedPrefs.getBoolean(KEY_IS_PREMIUM, false));

        return user;
    }

    public String getUserId() {
        return encryptedPrefs.getString(KEY_CURRENT_USER_ID, null);
    }

    public String getUserName() {
        return encryptedPrefs.getString(KEY_CURRENT_USER_NAME, "Гость");
    }

    public String getUserEmail() {
        return encryptedPrefs.getString(KEY_CURRENT_USER_EMAIL, "");
    }

    public boolean isPremium() {
        return encryptedPrefs.getBoolean(KEY_IS_PREMIUM, false);
    }

    public void setPremium(boolean isPremium) {
        encryptedPrefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply();
    }

    public void clearUserAndTokens() {
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.remove(KEY_CURRENT_USER_ID);
        editor.remove(KEY_CURRENT_USER_NAME);
        editor.remove(KEY_CURRENT_USER_EMAIL);
        editor.remove(KEY_AUTH_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.remove(KEY_TOKEN_EXPIRY);
        editor.remove(KEY_IS_PREMIUM);
        editor.apply();
    }


    public void setLanguage(String language) {
        regularPrefs.edit().putString(KEY_APP_LANG, language).apply();
    }

    public String getLanguage() {
        return regularPrefs.getString(KEY_APP_LANG, Locale.getDefault().getLanguage());
    }

    public void setTheme(String theme) {
        regularPrefs.edit().putString(KEY_APP_THEME, theme).apply();
    }

    public String getTheme() {
        return regularPrefs.getString(KEY_APP_THEME, "light");
    }

    public void hideConnectionBannerPermanently() {
        regularPrefs.edit().putBoolean(KEY_HIDE_CONNECTION_BANNER, true).apply();
    }

    public void hideConnectionBannerTemporarily() {
        regularPrefs.edit().putLong(KEY_BANNER_DISMISSED_TIME, System.currentTimeMillis()).apply();
    }

    public boolean isConnectionBannerPermanentlyHidden() {
        return regularPrefs.getBoolean(KEY_HIDE_CONNECTION_BANNER, false);
    }

    public boolean shouldShowBannerAfterCooldown() {
        long dismissedTime = regularPrefs.getLong(KEY_BANNER_DISMISSED_TIME, 0);
        if (dismissedTime == 0) return true;
        return System.currentTimeMillis() - dismissedTime > BANNER_COOLDOWN_MS;
    }
    public boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivityManager == null) {
                return false;
            }

            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        } catch (Exception e) {
            return false;
        }
    }
}
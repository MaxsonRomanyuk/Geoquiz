package com.example.geoquiz_frontend;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.geoquiz_frontend.Entities.User;

public class PreferencesHelper {
    private static final String PREFS_NAME = "QuizPreferences";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_CURRENT_USER_NAME = "current_user_name";
    private static final String KEY_CURRENT_USER_EMAIL = "current_user_email";

    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
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
        editor.apply();
    }
    public void saveAuthToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_AUTH_TOKEN, token);


        editor.putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis());
        editor.apply();
    }

    public String getAuthToken() {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null);
    }
    public boolean hasValidToken() {
        String token = getAuthToken();
        if (token == null) return false;

        long expiryTime = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
        long tokenAge = System.currentTimeMillis() - expiryTime;
        return tokenAge < 86400000;

        //return true;
    }
}

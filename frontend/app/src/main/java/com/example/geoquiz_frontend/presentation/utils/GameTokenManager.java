package com.example.geoquiz_frontend.presentation.utils;

import android.os.Looper;

public class GameTokenManager {
    private SecurePreferencesHelper preferencesHelper;
    private TokenRefreshHelper tokenRefreshHelper;
    public GameTokenManager(SecurePreferencesHelper preferencesHelper, TokenRefreshHelper tokenRefreshHelper)
    {
        this.preferencesHelper = preferencesHelper;
        this.tokenRefreshHelper = tokenRefreshHelper;
    }
    public boolean prepareForShortGameSync() {
        if (preferencesHelper.hasValidAccessToken()) {
            return true;
        }
        return tokenRefreshHelper.refreshTokenSync();

    }

    public boolean prepareForLongGameSync() {
        if (preferencesHelper.getTokenRemainingTime() > 15 * 60 * 1000) {
            return true;
        }

        if (tokenRefreshHelper.refreshTokenSync()) {
            return preferencesHelper.getTokenRemainingTime() > 15 * 60 * 1000;
        }

        return false;
    }
    public void prepareForShortGameAsync(GameTokenCallback callback) {
        if (preferencesHelper.hasValidAccessToken()) {
            if (callback != null) callback.onSuccess();
            return;
        }

        new Thread(() -> {
            boolean success = prepareForShortGameSync();

            if (callback != null) {
                new android.os.Handler(Looper.getMainLooper()).post(() -> {
                    if (success) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure("Failed to refresh token");
                    }
                });
            }
        }).start();
    }

    public void prepareForLongGameAsync(GameTokenCallback callback) {
        if (preferencesHelper.getTokenRemainingTime() > 15 * 60 * 1000) {
            if (callback != null) callback.onSuccess();
            return;
        }

        new Thread(() -> {
            boolean success = prepareForLongGameSync();

            if (callback != null) {
                new android.os.Handler(Looper.getMainLooper()).post(() -> {
                    if (success) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure("Failed to refresh token for long game");
                    }
                });
            }
        }).start();
    }

    public interface GameTokenCallback {
        void onSuccess();
        void onFailure(String error);
    }
}

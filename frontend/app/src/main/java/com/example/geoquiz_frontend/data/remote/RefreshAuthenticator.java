package com.example.geoquiz_frontend.data.remote;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.example.geoquiz_frontend.presentation.utils.TokenRefreshHelper;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class RefreshAuthenticator implements Authenticator {
    private final Context context;
    private final SecurePreferencesHelper preferencesHelper;
    private final TokenRefreshHelper tokenRefreshHelper;
    private int retryCount = 0;
    private static final int MAX_RETRY = 1;

    public RefreshAuthenticator(Context context, SecurePreferencesHelper preferencesHelper) {
        this.context = context;
        this.preferencesHelper = preferencesHelper;
        this.tokenRefreshHelper = new TokenRefreshHelper(context, preferencesHelper);
    }

    @Override
    public Request authenticate(Route route, @NonNull Response response) throws IOException {
        if (retryCount >= MAX_RETRY) {
            retryCount = 0;
            return null;
        }
        retryCount++;
        if (tokenRefreshHelper.refreshTokenSync()) {
            retryCount = 0;
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + preferencesHelper.getAuthToken())
                    .build();
        }
        retryCount = 0;
        return null;
    }
}

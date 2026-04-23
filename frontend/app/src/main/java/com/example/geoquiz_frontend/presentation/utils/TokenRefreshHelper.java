package com.example.geoquiz_frontend.presentation.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.example.geoquiz_frontend.data.remote.ApiService;
import com.example.geoquiz_frontend.data.remote.dtos.auth.RefreshTokenRequest;
import com.example.geoquiz_frontend.data.remote.dtos.auth.RefreshTokenResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TokenRefreshHelper {
    private static final String TAG = "TokenRefreshHelper";
    private final Context context;
    private final SecurePreferencesHelper preferencesHelper;
    private final ApiService apiService;
    public TokenRefreshHelper(Context context, SecurePreferencesHelper preferencesHelper, ApiService apiService) {
        this.context = context;
        this.preferencesHelper = preferencesHelper;
        this.apiService = apiService;
    }
    public TokenRefreshHelper(Context context, SecurePreferencesHelper preferencesHelper) {
        this.context = context;
        this.preferencesHelper = preferencesHelper;
        this.apiService = createCleanApiService();
    }
    private ApiService createCleanApiService() {
        Gson gson = new GsonBuilder().create();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl("http://192.168.100.49:5238/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService.class);
    }
    public boolean refreshTokenSync() {
        if (!preferencesHelper.isNetworkAvailable()) {
            Log.w(TAG, "No internet connection, cannot refresh token");
            return false;
        }

        String refreshToken = preferencesHelper.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.w(TAG, "No refresh token available");
            return false;
        }

        String deviceId = getDeviceId();
        try {
            Log.d(TAG, "Attempting to refresh token...");
            RefreshTokenRequest request = new RefreshTokenRequest(refreshToken, deviceId);
            Response<RefreshTokenResponse> response = apiService.refreshToken(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                RefreshTokenResponse body = response.body();
                preferencesHelper.saveAuthTokens(
                        body.getAccessToken(),
                        body.getRefreshToken(),
                        body.getExpiresIn()
                );
                Log.d(TAG, "Token refresh successful");
                return true;
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Token refresh network error");
            return false;
        }
    }

    public void refreshTokenAsync(TokenRefreshCallback callback) {
        if (!preferencesHelper.isNetworkAvailable()) {
            Log.w(TAG, "No internet connection, cannot refresh token");
            if (callback != null) callback.onFailure("No internet connection");
            return;
        }

        String refreshToken = preferencesHelper.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.w(TAG, "No refresh token available");
            if (callback != null) callback.onFailure("No refresh token available");
            return;
        }

        String deviceId = getDeviceId();

        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken, deviceId);
        apiService.refreshToken(request).enqueue(new retrofit2.Callback<RefreshTokenResponse>() {
            @Override
            public void onResponse(retrofit2.Call<RefreshTokenResponse> call, Response<RefreshTokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RefreshTokenResponse body = response.body();
                    preferencesHelper.saveAuthTokens(
                            body.getAccessToken(),
                            body.getRefreshToken(),
                            body.getExpiresIn()
                    );
                    Log.d(TAG, "Token refresh successful");
                    if (callback != null) callback.onSuccess();
                } else {
                    String errorMsg = "Refresh failed with code: " + response.code();
                    Log.e(TAG, errorMsg);
                    if (callback != null) callback.onFailure(errorMsg);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<RefreshTokenResponse> call, Throwable t) {
                String errorMsg = "Network error: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                if (callback != null) callback.onFailure(errorMsg);
            }
        });
    }

    @SuppressLint("HardwareIds")
    private String getDeviceId() {
        return android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
    }

    public interface TokenRefreshCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
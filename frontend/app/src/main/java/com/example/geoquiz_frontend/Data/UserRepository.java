package com.example.geoquiz_frontend.Data;
import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.geoquiz_frontend.ApiClient;
import com.example.geoquiz_frontend.ApiService;
import com.example.geoquiz_frontend.DTOs.ProfileResponse;
import com.example.geoquiz_frontend.PreferencesHelper;

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

    private UserRepository(Context context) {
        preferencesHelper = new PreferencesHelper(context);
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

        ProfileResponse cachedData = preferencesHelper.getCurrentUserStats();
        if (cachedData != null && !forceRefresh) {
            userData.setValue(cachedData);
            isLoading.setValue(false);
            return;
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
                    userData.setValue(response.body());
                    preferencesHelper.setCurrentUserStats(
                            response.body().getUser(),
                            response.body().getStats(),
                            response.body().getGeography()
                    );
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

    public void clearData() {
        userData.setValue(null);
        preferencesHelper.clearCurrentUser();
    }
}

package com.example.geoquiz_frontend.presentation.utils;

import android.content.Context;

import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.data.remote.NotificationManager;
import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.example.geoquiz_frontend.data.repositories.GameRepository;
import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.domain.engine.GameManager;
import com.example.geoquiz_frontend.domain.entities.User;
import com.example.geoquiz_frontend.domain.entities.UserStats;

public class AuthManager {
    private SecurePreferencesHelper preferencesHelper;
    private DatabaseHelper databaseHelper;
    private GameRepository gameRepository;

    public AuthManager(Context context) {
        preferencesHelper = new SecurePreferencesHelper(context);
        databaseHelper = new DatabaseHelper(context);
        gameRepository = new GameRepository(context);
    }

    public void loginAsGuest() {
        User guestUser = new User(
                "uid",
                "guest@example.com",
                "Guest"
        );
        preferencesHelper.saveCurrentUser(guestUser);
        String uid = preferencesHelper.getUserId();
        databaseHelper.saveUserStats(new UserStats(uid));
        databaseHelper.saveAllEmptyAchievements(uid);

    }

    public void logout() {
        NotificationManager notificationManager = NotificationManager.getInstance();
        //String token = preferencesHelper.getAuthToken();
        String userId = preferencesHelper.getUserId();

        notificationManager.stop();

        databaseHelper.deleteUserStats(userId);
        databaseHelper.deleteUserAchievements(userId);
        gameRepository.clearPendingGames();
        preferencesHelper.clearUserAndTokens();
        UserRepository.reset();
    }

    public boolean isLoggedIn() {
        return preferencesHelper.getCurrentUser() != null;
    }

    public User getCurrentUser() {
        return preferencesHelper.getCurrentUser();
    }

    public UserStats getCurrentStats() { return databaseHelper.getUserStats(preferencesHelper.getUserId()); }


    public void registerWithEmail(String uid, String email, String name) {
        User user = new User(uid,email,name);
        preferencesHelper.saveCurrentUser(user);
    }

    public void LoginWithEmail(User user) {
        preferencesHelper.saveCurrentUser(user);
    }
}

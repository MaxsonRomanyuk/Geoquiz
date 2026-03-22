package com.example.geoquiz_frontend.presentation.utils;

import android.content.Context;

import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.data.repositories.GameRepository;
import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.domain.engine.GameManager;
import com.example.geoquiz_frontend.domain.entities.User;
import com.example.geoquiz_frontend.domain.entities.UserStats;

public class AuthManager {
    private PreferencesHelper preferencesHelper;
    private DatabaseHelper databaseHelper;
    private GameRepository gameRepository;

    public AuthManager(Context context) {

        preferencesHelper = new PreferencesHelper(context);
        databaseHelper = new DatabaseHelper(context);
        gameRepository = new GameRepository(context);
    }

    public void loginAsGuest() {
        User guestUser = new User(
                "uid",
                "guest@example.com",
                "Guest"
        );
        preferencesHelper.setCurrentUser(guestUser);
        databaseHelper.saveUserStats(new UserStats(preferencesHelper.getUserId()));
    }

    public void logout() {
        databaseHelper.deleteUserStats(preferencesHelper.getUserId());
        //preferencesHelper.clearCurrentUser();
        gameRepository.clearPendingGames();

        preferencesHelper.clearCurrentUser();

        UserRepository.reset();
    }

    public boolean isLoggedIn() {
        return preferencesHelper.getCurrentUser() != null;
    }

    public User getCurrentUser() {
        return preferencesHelper.getCurrentUser();
    }




    public void registerWithEmail(String uid, String email, String name) {
        User user = new User(uid,email,name);
        preferencesHelper.setCurrentUser(user);
    }

    public void LoginWithEmail(User user) {
        preferencesHelper.setCurrentUser(user);
    }
}

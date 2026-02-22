package com.example.geoquiz_frontend;

import android.content.Context;

import com.example.geoquiz_frontend.Data.DatabaseHelper;
import com.example.geoquiz_frontend.Entities.User;

public class AuthManager {
    private PreferencesHelper preferencesHelper;
    private DatabaseHelper databaseHelper;

    public AuthManager(Context context) {

        preferencesHelper = new PreferencesHelper(context);
        databaseHelper = new DatabaseHelper(context);
    }

    public void loginAsGuest() {
        User guestUser = new User(
                "uid",
                "guest@example.com",
                "Guest"
        );
        preferencesHelper.setCurrentUser(guestUser);
    }

    public void logout() {
        databaseHelper.deleteUserStats(preferencesHelper.getUserId());
        preferencesHelper.clearCurrentUser();
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

package com.example.geoquiz_frontend.Presentation.ui.Base;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.geoquiz_frontend.Presentation.utils.LocaleHelper;
import com.example.geoquiz_frontend.Presentation.utils.PreferencesHelper;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        PreferencesHelper prefs = new PreferencesHelper(newBase);
        String lang = prefs.getLanguage();

        Context context = LocaleHelper.setLocale(newBase, lang);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
    }

    private void applyTheme() {
        PreferencesHelper prefs = new PreferencesHelper(this);
        String theme = prefs.getTheme();

        AppCompatDelegate.setDefaultNightMode(
                "dark".equals(theme)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
package me.theoria.wifimuscles.ui.helpers;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    public static void apply(AppCompatActivity activity) {

        SharedPreferences prefs =
                activity.getSharedPreferences("settings", MODE_PRIVATE);

        boolean dark = prefs.getBoolean("dark_mode", false);

        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
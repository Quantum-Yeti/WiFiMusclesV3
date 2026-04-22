package me.theoria.wifimuscles.ui.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

import me.theoria.wifimuscles.R;

public class OptionsFragment extends Fragment {

    private static final String PREFS = "settings";
    private static final String KEY_DARK = "dark_mode";

    public OptionsFragment() {
        super(R.layout.fragment_options);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        MaterialSwitch darkTheme = view.findViewById(R.id.switchDarkTheme);

        View donate = view.findViewById(R.id.btnDonate);
        View rate = view.findViewById(R.id.btnRate);
        View share = view.findViewById(R.id.btnShare);

        TextView version = view.findViewById(R.id.tvVersion);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS, 0);

        setupVersion(version);
        setupTheme(darkTheme, prefs);
        setupButtons(donate, rate, share);
        animateHeaders(view);
    }

    // ---------------- VERSION ----------------
    private void setupVersion(TextView version) {
        try {
            PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);

            version.setText("Version " + info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            version.setText("Version ?");
        }
    }

    // ---------------- THEME BUTTON ----------------
    // @TODO: Switch should be initialized and setup before fragment lifecycle in MainActivity
    private void setupTheme(MaterialSwitch sw, SharedPreferences prefs) {

        boolean dark = prefs.getBoolean(KEY_DARK, false);

        sw.setOnCheckedChangeListener(null);
        sw.setChecked(dark);

        sw.setOnCheckedChangeListener((button, checked) -> {

            prefs.edit().putBoolean(KEY_DARK, checked).commit();
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );
            requireActivity().recreate();
        });
    }

    // ---------------- BUTTONS ----------------
    private void setupButtons(View donate, View rate, View share) {

        donate.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.paypal.com/"))));

        rate.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + requireContext().getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id="
                                + requireContext().getPackageName())));
            }
        });

        share.setOnClickListener(v -> {
            String link = "https://play.google.com/store/apps/details?id="
                    + requireContext().getPackageName();

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, "Check out Wi-Fi Muscles:\n" + link);
            startActivity(Intent.createChooser(i, "Share"));
        });
    }

    // ---------------- STYLE ANIMATION ----------------
    private void animateHeaders(View view) {

        animate(view.findViewById(R.id.headerAppearance), 0);
        animate(view.findViewById(R.id.headerBehavior), 80);
        animate(view.findViewById(R.id.headerSupport), 160);
        animate(view.findViewById(R.id.headerAbout), 240);
    }

    private void animate(View v, long delay) {
        if (v == null) return;

        v.setAlpha(0f);
        v.setTranslationY(16f);

        v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(450)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
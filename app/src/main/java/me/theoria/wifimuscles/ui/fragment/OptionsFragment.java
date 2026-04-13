package me.theoria.wifimuscles.ui.fragment;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import me.theoria.wifimuscles.R;

public class OptionsFragment extends Fragment {

    public OptionsFragment() {
        super(R.layout.fragment_options);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view,
                              @Nullable Bundle savedInstanceState) {

        // ui refs
        Switch darkTheme = view.findViewById(R.id.switchDarkTheme);

        Button donate = view.findViewById(R.id.btnDonate);
        Button rate = view.findViewById(R.id.btnRate);
        Button share = view.findViewById(R.id.btnShare);

        TextView version = view.findViewById(R.id.tvVersion);

        // -------------------------
        // version display
        // -------------------------
        try {
            PackageInfo pInfo = requireContext()
                    .getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);

            version.setText("Version " + pInfo.versionName);

        } catch (PackageManager.NameNotFoundException e) {
            version.setText("Version ?");
        }

        // -------------------------
        // theme toggle
        // -------------------------
        darkTheme.setOnCheckedChangeListener((b, checked) -> {
            AppCompatDelegate.setDefaultNightMode(
                    checked
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // -------------------------
        // donate (paypal)
        // -------------------------
        donate.setOnClickListener(v -> {
            String url = "https://www.paypal.com/";
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        // -------------------------
        // rate app
        // -------------------------
        rate.setOnClickListener(v -> {
            String uri = "market://details?id=" + requireContext().getPackageName();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        });

        // -------------------------
        // share app
        // -------------------------
        share.setOnClickListener(v -> {
            String link = "https://play.google.com/"
                    + requireContext().getPackageName();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT,
                    "Check out Wi-Fi Muscles:\n" + link);

            startActivity(Intent.createChooser(intent, "Share via"));
        });
    }
}
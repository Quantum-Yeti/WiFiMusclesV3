package me.theoria.wifimuscles.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.ads.MobileAds;

import me.theoria.wifimuscles.R;
import me.theoria.wifimuscles.databinding.ActivityMainBinding;
import me.theoria.wifimuscles.ui.widget.SignalLoadingView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {});

        // Setup Navigation
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNav, navController);
        }

        /*SignalLoadingView loading = findViewById(R.id.loadingView);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            loading.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction(() -> loading.setVisibility(View.GONE));
        }, 8000);*/
    }
}
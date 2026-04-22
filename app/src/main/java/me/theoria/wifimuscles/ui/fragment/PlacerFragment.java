package me.theoria.wifimuscles.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import me.theoria.wifimuscles.R;
import me.theoria.wifimuscles.databinding.FragmentPlacerBinding;
import me.theoria.wifimuscles.ui.viewmodel.HomeViewModel;

public class PlacerFragment extends Fragment {

    private FragmentPlacerBinding binding;
    private HomeViewModel viewModel;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final long WIFI_INTERVAL = 800;

    private final Runnable wifiRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null) {
                viewModel.updateWifiInfo();
                updateNetworkInfo();
                handler.postDelayed(this, WIFI_INTERVAL);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentPlacerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity())
                .get(HomeViewModel.class);

        applyInsets(view);
        observeSignal();
        loadBannerAd();
    }

    private void applyInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.topGuide, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            view.setPadding(
                    view.getPaddingLeft(),
                    bars.top + 16,
                    view.getPaddingRight(),
                    view.getPaddingBottom()
            );
            return insets;
        });
    }

    private void observeSignal() {

        viewModel.getRssi().observe(getViewLifecycleOwner(), rssiText -> {

            if (binding == null || rssiText == null) return;

            int rssi = parseRssi(rssiText);

            if (rssi == Integer.MIN_VALUE) {
                renderEmptyState();
                return;
            }

            renderSignal(rssi);
        });
    }

    private int parseRssi(String raw) {
        try {
            String cleaned = raw.replaceAll("[^0-9-]", "");
            return Integer.parseInt(cleaned);
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    // 🔥 MAIN RENDER PIPELINE
    private void renderSignal(int rssi) {

        // Hero
        binding.speedometerView.setRssi(rssi);
        binding.signalValue.setText(rssi + " dBm");

        // subtle pulse
        binding.signalValue.animate()
                .alpha(0.7f)
                .setDuration(120)
                .withEndAction(() ->
                        binding.signalValue.animate().alpha(1f).setDuration(120)
                );

        // Decision engine
        renderPlacementDecision(rssi);
    }

    private void renderEmptyState() {
        binding.signalValue.setText("-- dBm");

        binding.placementTitle.setText("Scanning…");
        binding.placementAction.setText("Walk around to find the best spot");
        binding.confidenceBar.setProgress(0);
    }

    // 🔥 CORE UX LOGIC
    private void renderPlacementDecision(int rssi) {

        String title;
        String action;
        int progress;

        if (rssi >= -55) {
            title = "Perfect spot";
            action = "Place your extender here";
            progress = 100;

        } else if (rssi >= -65) {
            title = "Good spot";
            action = "Works, but move slightly closer";
            progress = 75;

        } else if (rssi >= -72) {
            title = "Borderline";
            action = "Try a bit closer to router";
            progress = 50;

        } else if (rssi >= -80) {
            title = "Weak signal";
            action = "Move closer before placing";
            progress = 30;

        } else {
            title = "Too far";
            action = "You are too far from the router";
            progress = 10;
        }

        binding.placementTitle.setText(title);
        binding.placementAction.setText(action);

        // smooth progress animation
        binding.confidenceBar.setProgress(progress, true);
    }

    private void loadBannerAd() {
        AdView adView = binding.adView;
        adView.loadAd(new AdRequest.Builder().build());
    }

    private void updateNetworkInfo() {
        try {
            android.net.wifi.WifiManager wifiManager =
                    (android.net.wifi.WifiManager) requireContext()
                            .getApplicationContext()
                            .getSystemService(android.content.Context.WIFI_SERVICE);

            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            int ipInt = wifiInfo.getIpAddress();
            @SuppressLint("DefaultLocale")
            String ip = String.format(
                    "%d.%d.%d.%d",
                    (ipInt & 0xff),
                    (ipInt >> 8 & 0xff),
                    (ipInt >> 16 & 0xff),
                    (ipInt >> 24 & 0xff)
            );

            binding.ipValue.setText("IP: " + ip);

            String bssid = wifiInfo.getBSSID();
            binding.routerValue.setText(
                    bssid != null ? "Router: " + bssid : "Router: --"
            );

            if (wifiInfo.getNetworkId() == -1) {
                binding.routerValue.setText(R.string.router_not_connected);
            }

        } catch (Exception e) {
            binding.ipValue.setText(R.string.empty_ip);
            binding.routerValue.setText(R.string.empty_mac);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(wifiRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(wifiRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(wifiRunnable);
        binding = null;
    }
}
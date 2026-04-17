package me.theoria.wifimuscles.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    private final Runnable wifiRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null) {
                viewModel.updateWifiInfo();
                updateNetworkInfo();
                handler.postDelayed(this, 500);
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

        // top of fragment padding
        ViewCompat.setOnApplyWindowInsetsListener(binding.topGuide, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            view.setPadding(
                    view.getPaddingLeft(),
                    bars.top + 16, // padding
                    view.getPaddingRight(),
                    view.getPaddingBottom()
            );

            return insets;
        });

        observeSignal();
        loadBannerAd();
    }

    private void observeSignal() {

        viewModel.getRssi().observe(getViewLifecycleOwner(), rssiText -> {

            if (binding == null || rssiText == null) return;

            try {
                String cleaned = rssiText.replaceAll("[^0-9-]", "");
                int rssi = Integer.parseInt(cleaned);

                binding.speedometerView.setRssi(rssi);
                binding.signalValue.setText(rssi + " dBm");

                updateRecommendation(rssi);

            } catch (Exception e) {
                binding.signalValue.setText("-- dBm");
                binding.recommendationText.setText("Scanning signal...");
            }
        });
    }

    private void updateRecommendation(int rssi) {

        String message;

        if (rssi >= -50) {
            message = "😄 Great Signal\nNo extender needed";
        } else if (rssi >= -60) {
            message = "🙂 Good Signal\nPlace an extender nearby";
        } else if (rssi >= -70) {
            message = "😐 Fair Signal\nMove closer to router";
        } else if (rssi >= -80) {
            message = "😟 Weak Signal\nConsider repositioning";
        } else {
            message = "😨 Poor Signal\nToo far from router";
        }

        binding.recommendationText.setText(message);
    }

    private void loadBannerAd() {

        AdView adView = binding.adView;

        AdRequest request = new AdRequest.Builder().build();
        adView.loadAd(request);
    }

    private void updateNetworkInfo() {
        try {
            android.net.wifi.WifiManager wifiManager =
                    (android.net.wifi.WifiManager) requireContext()
                            .getApplicationContext()
                            .getSystemService(android.content.Context.WIFI_SERVICE);

            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            // IP Address
            int ipInt = wifiInfo.getIpAddress();
            @SuppressLint("DefaultLocale") String ip = String.format(
                    "%d.%d.%d.%d",
                    (ipInt & 0xff),
                    (ipInt >> 8 & 0xff),
                    (ipInt >> 16 & 0xff),
                    (ipInt >> 24 & 0xff)
            );

            binding.ipValue.setText(ip);

            // BSSID (router MAC)
            String bssid = wifiInfo.getBSSID();
            binding.routerValue.setText(bssid != null ? bssid : "--");

            if (wifiInfo.getNetworkId() == -1) {
                binding.routerValue.setText(R.string.router_not_connected);
                return;
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
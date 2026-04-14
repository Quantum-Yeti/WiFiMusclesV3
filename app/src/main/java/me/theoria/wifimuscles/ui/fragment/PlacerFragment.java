package me.theoria.wifimuscles.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import me.theoria.wifimuscles.databinding.FragmentPlacerBinding;
import me.theoria.wifimuscles.ui.viewmodel.HomeViewModel;
import me.theoria.wifimuscles.ui.widget.SpeedometerView;

public class PlacerFragment extends Fragment {

    private FragmentPlacerBinding binding;
    private HomeViewModel viewModel;

    // Polling handler for live RSSI updates
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable wifiRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null) {
                viewModel.updateWifiInfo();
                handler.postDelayed(this, 500); // slightly slower = more stable
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
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        observeSignal();
    }

    private void observeSignal() {

        viewModel.getRssi().observe(getViewLifecycleOwner(), rssiText -> {

            if (binding == null || rssiText == null) return;

            try {
                String cleaned = rssiText.replaceAll("[^0-9-]", "");
                int rssi = Integer.parseInt(cleaned);

                binding.signalText.setText("Signal: " + rssi + " dBm");

                updateRecommendation(rssi);
                updateSpeedometer(rssi);

            } catch (Exception ignored) {
                binding.signalText.setText("Signal: --");
            }
        });
    }

    private void updateRecommendation(int rssi) {

        String message;

        if (rssi >= -50) {
            message = "😄 Excellent signal — no extender needed";
        } else if (rssi >= -60) {
            message = "🙂 Good signal — place an extender here";
        } else if (rssi >= -70) {
            message = "😐 Fair signal — move closer to router and place an extender";
        } else if (rssi >= -80) {
            message = "😟 Weak signal — too weak to place an extender here";
        } else {
            message = "😨 Poor signal — way too far from the router";
        }

        binding.recommendationText.setText(message);
    }

    private void updateSpeedometer(int rssi) {

        // Normalize RSSI (-100 to -40 roughly) → 0..1
        float normalized = (rssi + 100f) / 60f;

        if (normalized < 0f) normalized = 0f;
        if (normalized > 1f) normalized = 1f;

        // set target, view handles smoothing internally
        binding.speedometerView.setRssi(rssi);
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
package me.theoria.wifimuscles.ui.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.AdRequest;

import java.util.ArrayList;
import java.util.List;

import me.theoria.wifimuscles.R;
import me.theoria.wifimuscles.databinding.FragmentHomeBinding;
import me.theoria.wifimuscles.ui.adapters.SignalPagerAdapter;
import me.theoria.wifimuscles.ui.helpers.StabilityHelper;
import me.theoria.wifimuscles.ui.viewmodel.HomeViewModel;
import me.theoria.wifimuscles.ui.widget.*;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final long WIFI_INTERVAL = 2000;
    private static final long LATENCY_INTERVAL = 5000;

    private boolean running = false;

    // Views for pager / user swipes
    private SignalBlobView blob;
    private SignalWaveChartView wave;
    private SignalChartView chartView;
    private SignalPongView pongView;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted && binding != null) {
                    binding.tvNetworkName.setText(R.string.perm_denied);
                    binding.tvSubtitle.setText(R.string.location_needed);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        setupUi();
        setupInsets();
        setupPager();
        setupObservers();
        startLoops();
        requestPermission();
    }

    private void setupUi() {
        binding.adView.loadAd(new AdRequest.Builder().build());
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottom + 24);
            return insets;
        });
    }

    // Pager to swipe between views
    private void setupPager() {

        blob = new SignalBlobView(requireContext(), null);
        chartView = new SignalChartView(requireContext(), null);
        pongView = new SignalPongView(requireContext(), null);
        wave = new SignalWaveChartView(requireContext(), null);

        List<View> pages = new ArrayList<>();
        pages.add(blob);
        pages.add(chartView);
        pages.add(wave);
        pages.add(pongView);

        binding.signalPager.setAdapter(new SignalPagerAdapter(pages));
        binding.signalPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    // Observers
    private void setupObservers() {

        viewModel.getSsid().observe(getViewLifecycleOwner(),
                binding.tvNetworkName::setText);

        viewModel.getRssi().observe(getViewLifecycleOwner(),
                binding.tvRssi::setText);

        viewModel.getLinkSpeed().observe(getViewLifecycleOwner(),
                binding.tvLinkSpeed::setText);

        viewModel.getFrequency().observe(getViewLifecycleOwner(),
                binding.tvFrequency::setText);

        viewModel.getSignalQuality().observe(getViewLifecycleOwner(),
                binding.tvSignalQuality::setText);

        viewModel.getStability().observe(getViewLifecycleOwner(),
                binding.tvStability::setText);

        viewModel.getChannelInfo().observe(getViewLifecycleOwner(),
                binding.tvChannel::setText);

        viewModel.getLatency().observe(getViewLifecycleOwner(),
                binding.tvLatency::setText);

        viewModel.getScore().observe(getViewLifecycleOwner(),
                s -> binding.tvScore.setText(s + "/100"));

        viewModel.getSignalColor().observe(getViewLifecycleOwner(),
                color -> {
                    if (color != null) {
                        binding.tvSignalQuality.setTextColor(color);
                    }
                });

        viewModel.getSignalLevel().observe(getViewLifecycleOwner(), level -> {

            Integer color = viewModel.getSignalColor().getValue();
            if (level == null || color == null) return;

            pushSignal(level, color);
        });
    }

    // Push the signal with colors to views
    private void pushSignal(int level, int color) {

        blob.feedSignal(level, color); // blob guy
        chartView.setSignalLevel(level, color); // equalizer bar-chart
        pongView.setSignalLevel(level, color); // pong game view
        List<Integer> history = viewModel.getRssiHistory().getValue();

        if (history != null && wave != null) {
            wave.setSignalLevel(level, color, history);
        }

        float stability = StabilityHelper.calculateStabilityScore(
                viewModel.getRssiHistory().getValue()
        );

        blob.setStability(stability);
    }

    // Loops
    private void startLoops() {
        running = true;
        handler.post(wifiLoop);
        handler.post(latencyLoop);
    }

    private final Runnable wifiLoop = new Runnable() {
        @Override
        public void run() {
            if (!running || viewModel == null) return;
            viewModel.updateWifiInfo();
            handler.postDelayed(this, WIFI_INTERVAL);
        }
    };

    private final Runnable latencyLoop = new Runnable() {
        @Override
        public void run() {
            if (!running || viewModel == null) return;
            viewModel.updateLatency();
            handler.postDelayed(this, LATENCY_INTERVAL);
        }
    };

    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        running = false;
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
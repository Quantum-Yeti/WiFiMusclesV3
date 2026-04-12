package me.theoria.wifimuscles.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import me.theoria.wifimuscles.ui.viewmodel.HomeViewModel;
import me.theoria.wifimuscles.ui.widget.SignalBlobView;
import me.theoria.wifimuscles.ui.widget.SignalBloomView;
import me.theoria.wifimuscles.ui.widget.SignalInvadersView;
import me.theoria.wifimuscles.ui.widget.SignalOceanView;
import me.theoria.wifimuscles.ui.adapters.SignalPagerAdapter;
import me.theoria.wifimuscles.ui.widget.SignalPlasmaView;
import me.theoria.wifimuscles.ui.widget.SignalRadarView;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    private final Handler handler = new Handler();
    private boolean isRunning = false;
    private boolean latencyRunning = false;

    private SignalBlobView blob;
    private SignalPlasmaView plasma;
    private SignalBloomView pulse;
    private SignalRadarView radar;
    private SignalOceanView ocean;
    private SignalInvadersView invaders;


    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    binding.tvNetworkName.setText(R.string.perm_denied);
                    binding.tvSubtitle.setText(R.string.location_needed);
                }
            });

    private final Runnable wifiUpdater = new Runnable() {
        @Override
        public void run() {
            if (!isRunning || viewModel == null || binding == null) return;

            viewModel.updateWifiInfo();
            handler.postDelayed(this, 2000);
        }
    };

    private final Runnable latencyUpdater = new Runnable() {
        @Override
        public void run() {

            if (!isRunning || viewModel == null || latencyRunning) return;

            latencyRunning = true;

            viewModel.updateLatency();

            handler.postDelayed(() -> latencyRunning = false, 1000);

            handler.postDelayed(this, 5000);
        }
    };

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottomInset + 24 // keeps breathing room for AdView
            );

            return insets;
        });

        binding.adView.loadAd(new AdRequest.Builder().build());

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupSignalPager();
        observeViewModel();

        viewModel.updateWifiInfo();

        isRunning = true;
        handler.post(wifiUpdater);
        handler.post(latencyUpdater);

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void setupSignalPager() {

        radar = new SignalRadarView(requireContext(), null);
        ocean = new SignalOceanView(requireContext(), null);
        blob = new SignalBlobView(requireContext(), null);
        plasma = new SignalPlasmaView(requireContext(), null);
        pulse = new SignalBloomView(requireContext(), null);
        invaders = new SignalInvadersView(requireContext(), null);

        List<View> pages = new ArrayList<>();
        pages.add(radar);
        pages.add(ocean);
        pages.add(blob);
        pages.add(plasma);
        pages.add(pulse);
        pages.add(invaders);

        SignalPagerAdapter adapter = new SignalPagerAdapter(pages);

        binding.signalPager.setAdapter(adapter);
        binding.signalPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void observeViewModel() {

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

        viewModel.getSignalColor().observe(getViewLifecycleOwner(),
                color -> {
                    if (color != null) {
                        binding.tvSignalQuality.setTextColor(color);
                    }
                });

        viewModel.getStability().observe(getViewLifecycleOwner(),
                binding.tvStability::setText);

        viewModel.getScore().observe(getViewLifecycleOwner(),
                score -> binding.tvScore.setText(score + "/100"));

        viewModel.getChannelInfo().observe(getViewLifecycleOwner(),
                binding.tvChannel::setText);

        viewModel.getLatency().observe(getViewLifecycleOwner(),
                binding.tvLatency::setText);

        viewModel.getSignalLevel().observe(getViewLifecycleOwner(), level -> {
            Integer color = viewModel.getSignalColor().getValue();
            if (level != null && color != null) {

                radar.setSignalLevel(level, color);
                ocean.setSignalLevel(level, color);
                blob.setSignalLevel(level, color);
                plasma.setSignalLevel(level, color);
                pulse.setSignalLevel(level, color);
                invaders.setSignalLevel(level, color);

            }
        });

        viewModel.updateWifiInfo();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isRunning = false;
        handler.removeCallbacks(wifiUpdater);
        handler.removeCallbacks(latencyUpdater);
        binding = null;
    }
}
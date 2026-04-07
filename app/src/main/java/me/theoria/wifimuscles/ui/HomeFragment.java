package me.theoria.wifimuscles.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.AdRequest;

import me.theoria.wifimuscles.R;
import me.theoria.wifimuscles.databinding.FragmentHomeBinding;
import me.theoria.wifimuscles.ui.viewmodel.HomeViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) registerRssiReceiver();
                else {
                    binding.tvNetworkName.setText(R.string.perm_denied);
                    binding.tvSubtitle.setText(R.string.location_needed);
                }
            });

    private final BroadcastReceiver rssiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            viewModel.updateWifiInfo();
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

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding.adView.loadAd(new AdRequest.Builder().build());

        observeViewModel();

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else registerRssiReceiver();
    }

    private void observeViewModel() {
        viewModel.getSsid().observe(getViewLifecycleOwner(), binding.tvNetworkName::setText);
        viewModel.getRssi().observe(getViewLifecycleOwner(), binding.tvRssi::setText);
        viewModel.getLinkSpeed().observe(getViewLifecycleOwner(), binding.tvLinkSpeed::setText);
        viewModel.getFrequency().observe(getViewLifecycleOwner(), binding.tvFrequency::setText);
        viewModel.getSignalQuality().observe(getViewLifecycleOwner(), binding.tvSignalQuality::setText);
        viewModel.getSignalLevel().observe(getViewLifecycleOwner(), level -> {
            Integer color = viewModel.getSignalColor().getValue();
            if (color != null) binding.signalView.setSignalLevel(level, color);
        });

        viewModel.getSignalColor().observe(getViewLifecycleOwner(), color -> {
            Integer level = viewModel.getSignalLevel().getValue();
            if (level != null) binding.signalView.setSignalLevel(level, color);
        });
    }

    private void registerRssiReceiver() {
        requireContext().registerReceiver(rssiReceiver,
                new IntentFilter(android.net.wifi.WifiManager.RSSI_CHANGED_ACTION));
        viewModel.updateWifiInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        try { requireContext().unregisterReceiver(rssiReceiver); }
        catch (IllegalArgumentException ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
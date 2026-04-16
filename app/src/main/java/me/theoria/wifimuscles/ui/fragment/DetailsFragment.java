package me.theoria.wifimuscles.ui.fragment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import me.theoria.wifimuscles.databinding.FragmentDetailsBinding;
import me.theoria.wifimuscles.ui.adapters.DetailsAdapter;
import me.theoria.wifimuscles.ui.viewmodel.DetailsViewModel;
import me.theoria.wifimuscles.ui.viewmodel.HomeViewModel;

public class DetailsFragment extends Fragment {

    private FragmentDetailsBinding binding;
    private DetailsViewModel detailsViewModel;
    private DetailsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        detailsViewModel = new ViewModelProvider(requireActivity())
                .get(DetailsViewModel.class);

        adapter = new DetailsAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(binding.title, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            view.setPadding(
                    view.getPaddingLeft(),
                    bars.top + 16, // padding
                    view.getPaddingRight(),
                    view.getPaddingBottom()
            );

            return insets;
        });

        observeWifi();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void observeWifi() {

        // Use HomeViewModel only as the RSSI trigger source
        HomeViewModel homeVm = new ViewModelProvider(requireActivity())
                .get(HomeViewModel.class);

        homeVm.getRssi().observe(getViewLifecycleOwner(), rssiText -> {
            if (rssiText == null || binding == null) return;

            String cleaned = rssiText.replaceAll("[^0-9\\-]", "");
            if (cleaned.isEmpty() || cleaned.equals("-")) return;

            int rssi;
            try {
                rssi = Integer.parseInt(cleaned);
            } catch (Exception e) {
                return;
            }

            Context appCtx = requireContext().getApplicationContext();

            WifiManager wm = (WifiManager) appCtx.getSystemService(Context.WIFI_SERVICE);
            ConnectivityManager cm = (ConnectivityManager) appCtx.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (wm == null) return;

            WifiInfo info = getWifiInfo(wm, cm);
            DhcpInfo dhcp = wm.getDhcpInfo();

            // Feed DetailsViewModel — it owns all the computation
            detailsViewModel.update(rssi, info, dhcp);
        });

        // Observe computed state and pass to adapter
        detailsViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || binding == null) return;
            adapter.setState(state);
        });
    }

    @SuppressWarnings("deprecation")
    private WifiInfo getWifiInfo(@NonNull WifiManager wifiManager,
                                 @Nullable ConnectivityManager connManager) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && connManager != null) {
            Network active = connManager.getActiveNetwork();
            if (active != null) {
                NetworkCapabilities caps = connManager.getNetworkCapabilities(active);
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Object ti = caps.getTransportInfo();
                    if (ti instanceof WifiInfo) return (WifiInfo) ti;
                }
            }
        }

        return wifiManager.getConnectionInfo();
    }
}
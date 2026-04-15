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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import me.theoria.wifimuscles.databinding.FragmentDetailsBinding;
import me.theoria.wifimuscles.ui.viewmodel.HomeViewModel;

public class DetailsFragment extends Fragment {

    private FragmentDetailsBinding binding;
    private HomeViewModel viewModel;

    private final List<Integer> rssiHistory = new ArrayList<>();

    // ---------------- LIFECYCLE ----------------

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

        viewModel = new ViewModelProvider(requireActivity())
                .get(HomeViewModel.class);

        observeWifi();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ---------------- WIFI OBSERVER ----------------

    private void observeWifi() {

        viewModel.getRssi().observe(getViewLifecycleOwner(), rssiText -> {

            if (binding == null || rssiText == null) return;

            String cleaned = rssiText.replaceAll("[^0-9\\-]", "");

            if (cleaned.isEmpty() || cleaned.equals("-")) {
                setAllUnavailable();
                return;
            }

            int rssi;
            try {
                rssi = Integer.parseInt(cleaned);
            } catch (Exception e) {
                setAllUnavailable();
                return;
            }

            Context ctx = getContext();
            if (ctx == null) return;

            Context appCtx = ctx.getApplicationContext();

            WifiManager wifiManager =
                    (WifiManager) appCtx.getSystemService(Context.WIFI_SERVICE);

            ConnectivityManager connManager =
                    (ConnectivityManager) appCtx.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (wifiManager == null) {
                setAllUnavailable();
                return;
            }

            WifiInfo info = getWifiInfo(wifiManager, connManager);
            DhcpInfo dhcp = wifiManager.getDhcpInfo();

            renderSignal(rssi, info);
            renderNetwork(info);
            renderDhcp(dhcp);
            renderCapabilities(wifiManager, connManager);
            renderStability(rssi);
            renderInsights(rssi);
        });
    }

    // ---------------- WIFI INFO (API SAFE) ----------------

    @SuppressWarnings("deprecation")
    private WifiInfo getWifiInfo(@NonNull WifiManager wifiManager,
                                 @Nullable ConnectivityManager connManager) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && connManager != null) {

            Network active = connManager.getActiveNetwork();
            if (active != null) {

                NetworkCapabilities caps =
                        connManager.getNetworkCapabilities(active);

                if (caps != null &&
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {

                    Object ti = caps.getTransportInfo();
                    if (ti instanceof WifiInfo) {
                        return (WifiInfo) ti;
                    }
                }
            }
        }

        return wifiManager.getConnectionInfo();
    }

    // ---------------- SIGNAL ----------------

    private void renderSignal(int rssi, @Nullable WifiInfo info) {

        if (binding == null) return;

        binding.rssi.setText("RSSI: " + rssi + " dBm");
        binding.signalLevel.setText("Signal Level: " +
                WifiManager.calculateSignalLevel(rssi, 5) + " / 5");

        binding.signalQuality.setText("Signal Quality: " + rssiToQuality(rssi) + "%");

        if (info == null) {
            binding.linkSpeed.setText("Link Speed: --");
            binding.txLinkSpeed.setText("TX Speed: --");
            binding.rxLinkSpeed.setText("RX Speed: --");
            binding.frequency.setText("Frequency: --");
            binding.channel.setText("Channel: --");
            binding.band.setText("Band: --");
            return;
        }

        binding.linkSpeed.setText("Link Speed: " + info.getLinkSpeed() + " Mbps");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.txLinkSpeed.setText("TX Speed: " + info.getTxLinkSpeedMbps() + " Mbps");
            binding.rxLinkSpeed.setText("RX Speed: " + info.getRxLinkSpeedMbps() + " Mbps");
        }

        int freq = info.getFrequency();
        binding.frequency.setText("Frequency: " + freq + " MHz");
        binding.channel.setText("Channel: " + frequencyToChannel(freq));
        binding.band.setText("Band: " + frequencyToBand(freq));
    }

    private int rssiToQuality(int rssi) {
        if (rssi <= -100) return 0;
        if (rssi >= -50) return 100;
        return 2 * (rssi + 100);
    }

    private String frequencyToChannel(int freq) {
        if (freq == 2484) return "14";
        if (freq >= 2412 && freq <= 2472) return String.valueOf((freq - 2407) / 5);
        if (freq >= 5170 && freq <= 5825) return String.valueOf((freq - 5000) / 5);
        return "?";
    }

    private String frequencyToBand(int freq) {
        if (freq < 2500) return "2.4 GHz";
        if (freq < 5900) return "5 GHz";
        return "6 GHz";
    }

    // ---------------- NETWORK ----------------

    @SuppressWarnings("deprecation")
    private void renderNetwork(@Nullable WifiInfo info) {

        if (binding == null) return;

        if (info == null) {
            binding.ssid.setText("SSID: --");
            binding.bssid.setText("BSSID: --");
            binding.ip.setText("IP: --");
            binding.macAddress.setText("MAC: --");
            binding.networkId.setText("Network ID: --");
            binding.hidden.setText("Hidden SSID: --");
            return;
        }

        binding.ssid.setText("SSID: " + info.getSSID());
        binding.bssid.setText("BSSID: " + info.getBSSID());

        int ip = info.getIpAddress();
        binding.ip.setText("IP: " +
                (ip & 0xff) + "." +
                ((ip >> 8) & 0xff) + "." +
                ((ip >> 16) & 0xff) + "." +
                ((ip >> 24) & 0xff));

        binding.macAddress.setText("MAC: " + info.getMacAddress());
        binding.networkId.setText("Network ID: " + info.getNetworkId());
    }

    // ---------------- DHCP ----------------

    private void renderDhcp(@Nullable DhcpInfo dhcp) {
        if (binding == null) return;

        if (dhcp == null) return;

        binding.gateway.setText("Gateway: " + dhcp.gateway);
        binding.subnet.setText("Subnet: " + dhcp.netmask);
        binding.dns1.setText("DNS1: " + dhcp.dns1);
        binding.dns2.setText("DNS2: " + dhcp.dns2);
        binding.dhcpServer.setText("DHCP: " + dhcp.serverAddress);
    }

    // ---------------- CAPABILITIES ----------------

    private void renderCapabilities(WifiManager wm, ConnectivityManager cm) {

        if (binding == null) return;

        binding.wifiEnabled.setText("WiFi: " + wm.isWifiEnabled());
        binding.wifiState.setText("State: " + wm.getWifiState());
    }

    // ---------------- STABILITY ----------------

    private void renderStability(int rssi) {

        if (binding == null) return;

        rssiHistory.add(rssi);
        if (rssiHistory.size() > 10) rssiHistory.remove(0);

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;

        for (int v : rssiHistory) {
            min = Math.min(min, v);
            max = Math.max(max, v);
            sum += v;
        }

        float avg = sum / (float) rssiHistory.size();
        int jitter = max - min;

        binding.avgRssi.setText("Avg: " + (int) avg);
        binding.minRssi.setText("Min: " + min);
        binding.maxRssi.setText("Max: " + max);
        binding.jitter.setText("Jitter: " + jitter);
        binding.sampleCount.setText("Samples: " + rssiHistory.size());
    }

    // ---------------- INSIGHTS ----------------

    private void renderInsights(int rssi) {

        if (binding == null) return;

        if (rssi >= -50) {
            binding.recommendation.setText("Excellent");
        } else if (rssi >= -60) {
            binding.recommendation.setText("Good");
        } else if (rssi >= -70) {
            binding.recommendation.setText("Fair");
        } else {
            binding.recommendation.setText("Weak");
        }
    }

    // ---------------- FALLBACK ----------------

    private void setAllUnavailable() {
        if (binding == null) return;

        binding.rssi.setText("RSSI: --");
        binding.signalLevel.setText("--");
        binding.signalQuality.setText("--");
        binding.linkSpeed.setText("--");
        binding.frequency.setText("--");
        binding.ssid.setText("--");
    }
}
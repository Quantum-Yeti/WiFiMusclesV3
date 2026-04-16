package me.theoria.wifimuscles.ui.adapters;

import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Locale;

import me.theoria.wifimuscles.R;
import me.theoria.wifimuscles.models.DetailsUiState;

public class DetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SIGNAL       = 0;
    private static final int TYPE_NETWORK      = 1;
    private static final int TYPE_DHCP         = 2;
    private static final int TYPE_CAPABILITIES = 3;
    private static final int TYPE_STABILITY    = 4;
    private static final int TYPE_INSIGHTS     = 5;
    private static final int ITEM_COUNT        = 6;

    private static final int WIFI_STANDARD_LEGACY = 1;
    private static final int WIFI_STANDARD_11N    = 4;
    private static final int WIFI_STANDARD_11AC   = 5;
    private static final int WIFI_STANDARD_11AX   = 6;
    private static final int WIFI_STANDARD_11BE   = 7;

    private DetailsUiState state;

    public void setState(DetailsUiState newState) {
        this.state = newState;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return state == null ? 0 : ITEM_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        return position; // each position maps 1:1 to a card type
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_SIGNAL:
                return new SignalVH(inflater.inflate(R.layout.item_signal, parent, false));
            case TYPE_NETWORK:
                return new NetworkVH(inflater.inflate(R.layout.item_network, parent, false));
            case TYPE_DHCP:
                return new DhcpVH(inflater.inflate(R.layout.item_dhcp, parent, false));
            case TYPE_CAPABILITIES:
                return new CapVH(inflater.inflate(R.layout.item_capabilities, parent, false));
            case TYPE_STABILITY:
                return new StabilityVH(inflater.inflate(R.layout.item_stability, parent, false));
            case TYPE_INSIGHTS:
                return new InsightsVH(inflater.inflate(R.layout.item_insights, parent, false));
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (state == null) return;
        switch (position) {
            case TYPE_SIGNAL:       ((SignalVH)       holder).bind(state); break;
            case TYPE_NETWORK:      ((NetworkVH)      holder).bind(state); break;
            case TYPE_DHCP:         ((DhcpVH)         holder).bind(state); break;
            case TYPE_CAPABILITIES: ((CapVH)          holder).bind(state); break;
            case TYPE_STABILITY:    ((StabilityVH)    holder).bind(state); break;
            case TYPE_INSIGHTS:     ((InsightsVH)     holder).bind(state); break;
        }
    }

    // ─────────────────────────────────────────────
    // SIGNAL
    // ─────────────────────────────────────────────

    static class SignalVH extends RecyclerView.ViewHolder {
        TextView rssi, signalLevel, signalQuality, linkSpeed, txLinkSpeed, rxLinkSpeed,
                frequency, channel, band;

        SignalVH(View v) {
            super(v);
            rssi         = v.findViewById(R.id.rssi);
            signalLevel  = v.findViewById(R.id.signalLevel);
            signalQuality= v.findViewById(R.id.signalQuality);
            linkSpeed    = v.findViewById(R.id.linkSpeed);
            txLinkSpeed  = v.findViewById(R.id.txLinkSpeed);
            rxLinkSpeed  = v.findViewById(R.id.rxLinkSpeed);
            frequency    = v.findViewById(R.id.frequency);
            channel      = v.findViewById(R.id.channel);
            band         = v.findViewById(R.id.band);
        }

        void bind(DetailsUiState s) {
            WifiInfo info = s.wifiInfo;

            rssi.setText("RSSI: " + s.rssi + " dBm");
            signalLevel.setText("Signal Level: " +
                    WifiManager.calculateSignalLevel(s.rssi, 5) + " / 5");
            signalQuality.setText("Signal Quality: " +
                    Math.max(0, Math.min(100, 2 * (s.rssi + 100))) + "%");

            if (info != null) {
                linkSpeed.setText("Link Speed: " + info.getLinkSpeed() + " Mbps");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    int tx = info.getTxLinkSpeedMbps();
                    int rx = info.getRxLinkSpeedMbps();
                    txLinkSpeed.setText("TX Speed: " + (tx >= 0 ? tx + " Mbps" : "--"));
                    rxLinkSpeed.setText("RX Speed: " + (rx >= 0 ? rx + " Mbps" : "--"));
                } else {
                    txLinkSpeed.setText("TX Speed: --");
                    rxLinkSpeed.setText("RX Speed: --");
                }

                int freq = info.getFrequency();
                frequency.setText("Frequency: " + freq + " MHz");
                channel.setText("Channel: " + freqToChannel(freq));
                band.setText("Band: " + freqToBand(freq));
            } else {
                linkSpeed.setText("Link Speed: --");
                txLinkSpeed.setText("TX Speed: --");
                rxLinkSpeed.setText("RX Speed: --");
                frequency.setText("Frequency: --");
                channel.setText("Channel: --");
                band.setText("Band: --");
            }
        }
    }

    // ─────────────────────────────────────────────
    // NETWORK
    // ─────────────────────────────────────────────

    static class NetworkVH extends RecyclerView.ViewHolder {
        TextView ssid, bssid, ip, macAddress, networkId, hidden;

        NetworkVH(View v) {
            super(v);
            ssid      = v.findViewById(R.id.ssid);
            bssid     = v.findViewById(R.id.bssid);
            ip        = v.findViewById(R.id.ip);
            macAddress= v.findViewById(R.id.macAddress);
            networkId = v.findViewById(R.id.networkId);
            hidden    = v.findViewById(R.id.hidden);
        }

        void bind(DetailsUiState s) {
            WifiInfo info = s.wifiInfo;
            if (info == null) return;

            ssid.setText("SSID: " + info.getSSID().replace("\"", ""));
            bssid.setText("BSSID: " + info.getBSSID());
            ip.setText("IP: " + intToIp(info.getIpAddress()));
            macAddress.setText("MAC: " + info.getMacAddress());
            networkId.setText("Network ID: " + info.getNetworkId());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //hidden.setText("Hidden SSID: " + info.isHiddenSsid());
            } else {
                hidden.setText("Hidden SSID: --");
            }
        }
    }

    // ─────────────────────────────────────────────
    // DHCP
    // ─────────────────────────────────────────────

    static class DhcpVH extends RecyclerView.ViewHolder {
        TextView gateway, subnet, dns1, dns2, dhcpServer, leaseDuration;

        DhcpVH(View v) {
            super(v);
            gateway      = v.findViewById(R.id.gateway);
            subnet       = v.findViewById(R.id.subnet);
            dns1         = v.findViewById(R.id.dns1);
            dns2         = v.findViewById(R.id.dns2);
            dhcpServer   = v.findViewById(R.id.dhcpServer);
            leaseDuration= v.findViewById(R.id.leaseDuration);
        }

        void bind(DetailsUiState s) {
            DhcpInfo d = s.dhcpInfo;
            if (d == null) return;

            gateway.setText("Gateway: "       + intToIp(d.gateway));
            subnet.setText("Subnet Mask: "    + intToIp(d.netmask));
            dns1.setText("DNS 1: "            + intToIp(d.dns1));
            dns2.setText("DNS 2: "            + intToIp(d.dns2));
            dhcpServer.setText("DHCP Server: "+ intToIp(d.serverAddress));
            leaseDuration.setText("Lease Duration: " + d.leaseDuration + "s");
        }
    }

    // ─────────────────────────────────────────────
    // CAPABILITIES
    // ─────────────────────────────────────────────

    static class CapVH extends RecyclerView.ViewHolder {
        TextView wifiEnabled, wifiState, hasInternet, metered, wifiStandard;

        CapVH(View v) {
            super(v);
            wifiEnabled  = v.findViewById(R.id.wifiEnabled);
            wifiState    = v.findViewById(R.id.wifiState);
            hasInternet  = v.findViewById(R.id.hasInternet);
            metered      = v.findViewById(R.id.metered);
            wifiStandard = v.findViewById(R.id.wifiStandard);
        }

        void bind(DetailsUiState s) {
            WifiInfo info = s.wifiInfo;

            wifiEnabled.setText("WiFi Enabled: " + (info != null ? "Yes" : "No"));
            wifiState.setText("WiFi State: " + (info != null ? "Connected" : "Disconnected"));

            // hasInternet and metered require NetworkCapabilities — not available in DetailsUiState,
            // so show a placeholder; wire in ConnectivityManager if needed later
            hasInternet.setText("Internet: --");
            metered.setText("Metered: --");

            if (info != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                int std = info.getWifiStandard();
                wifiStandard.setText("WiFi Standard: " + wifiStandardLabel(std));
            } else {
                wifiStandard.setText("WiFi Standard: --");
            }
        }
    }

    // ─────────────────────────────────────────────
    // STABILITY
    // ─────────────────────────────────────────────

    static class StabilityVH extends RecyclerView.ViewHolder {
        TextView avgRssi, minRssi, maxRssi, jitter, stdDev, sampleCount, stabilityScore;

        StabilityVH(View v) {
            super(v);
            avgRssi       = v.findViewById(R.id.avgRssi);
            minRssi       = v.findViewById(R.id.minRssi);
            maxRssi       = v.findViewById(R.id.maxRssi);
            jitter        = v.findViewById(R.id.jitter);
            stdDev        = v.findViewById(R.id.stdDev);
            sampleCount   = v.findViewById(R.id.sampleCount);
            stabilityScore= v.findViewById(R.id.stabilityScore);
        }

        void bind(DetailsUiState s) {
            avgRssi.setText("Avg RSSI: "       + s.avgRssi + " dBm");
            minRssi.setText("Min RSSI: "       + s.minRssi + " dBm");
            maxRssi.setText("Max RSSI: "       + s.maxRssi + " dBm");
            jitter.setText("Jitter: "          + s.jitter  + " dBm");
            stdDev.setText("Std Dev: "         + String.format(Locale.US, "%.1f", s.stdDev));
            sampleCount.setText("Samples: "    + s.history.size() + " / 10");
            stabilityScore.setText("Stability Score: " + s.stability + " / 100");
        }
    }

    // ─────────────────────────────────────────────
    // INSIGHTS
    // ─────────────────────────────────────────────

    static class InsightsVH extends RecyclerView.ViewHolder {
        TextView recommendation, placementHint, rangeNote;

        InsightsVH(View v) {
            super(v);
            recommendation = v.findViewById(R.id.recommendation);
            placementHint  = v.findViewById(R.id.placementHint);
            rangeNote      = v.findViewById(R.id.rangeNote);
        }

        void bind(DetailsUiState s) {
            recommendation.setText(s.recommendation);
            placementHint.setText("Placement: " + s.placement);
            rangeNote.setText(s.note);
        }
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private static String intToIp(int ip) {
        // Android stores IP as little-endian on little-endian devices
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ip = Integer.reverseBytes(ip);
        }
        try {
            return InetAddress.getByAddress(new byte[]{
                    (byte)(ip >> 24),
                    (byte)(ip >> 16),
                    (byte)(ip >> 8),
                    (byte) ip
            }).getHostAddress();
        } catch (UnknownHostException e) {
            return "--";
        }
    }

    private static int freqToChannel(int freq) {
        if (freq == 2484) return 14;
        if (freq < 2484)  return (freq - 2407) / 5;
        if (freq >= 5160) return (freq - 5000) / 5;
        return -1;
    }

    private static String freqToBand(int freq) {
        if (freq >= 2400 && freq <= 2500) return "2.4 GHz";
        if (freq >= 4900 && freq <= 5900) return "5 GHz";
        if (freq >= 5925 && freq <= 7125) return "6 GHz";
        return freq + " MHz";
    }

    private static String wifiStandardLabel(int std) {
        switch (std) {
            case WIFI_STANDARD_LEGACY: return "802.11 a/b/g";
            case WIFI_STANDARD_11N:    return "Wi-Fi 4 (802.11n)";
            case WIFI_STANDARD_11AC:   return "Wi-Fi 5 (802.11ac)";
            case WIFI_STANDARD_11AX:   return "Wi-Fi 6 (802.11ax)";
            case WIFI_STANDARD_11BE:   return "Wi-Fi 7 (802.11be)";
            default:                  return "Unknown (" + std + ")";
        }
    }
}
package me.theoria.wifimuscles.models;

import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;

import java.util.List;

public class DetailsUiState {

    public final int rssi;
    public final int avgRssi;
    public final int minRssi;
    public final int maxRssi;
    public final int jitter;
    public final float stdDev;
    public final int stability;

    public final String recommendation;
    public final String placement;
    public final String note;

    public final WifiInfo wifiInfo;
    public final DhcpInfo dhcpInfo;

    public final List<Integer> history;

    public DetailsUiState(
            int rssi,
            int avgRssi,
            int minRssi,
            int maxRssi,
            int jitter,
            float stdDev,
            int stability,
            String recommendation,
            String placement,
            String note,
            WifiInfo wifiInfo,
            DhcpInfo dhcpInfo,
            List<Integer> history
    ) {
        this.rssi = rssi;
        this.avgRssi = avgRssi;
        this.minRssi = minRssi;
        this.maxRssi = maxRssi;
        this.jitter = jitter;
        this.stdDev = stdDev;
        this.stability = stability;
        this.recommendation = recommendation;
        this.placement = placement;
        this.note = note;
        this.wifiInfo = wifiInfo;
        this.dhcpInfo = dhcpInfo;
        this.history = history;
    }
}
package me.theoria.wifimuscles.models;

import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.List;

public class DetailData {

    public static class SignalData {
        public int rssi;
        public WifiInfo info;
        public SignalData(int rssi, WifiInfo info) {
            this.rssi = rssi;
            this.info = info;
        }
    }

    public static class NetworkData {
        public WifiInfo info;
        public NetworkData(WifiInfo info) {
            this.info = info;
        }
    }

    public static class DhcpData {
        public DhcpInfo dhcp;
        public DhcpData(DhcpInfo dhcp) {
            this.dhcp = dhcp;
        }
    }

    public static class CapData {
        public WifiManager wm;
        public CapData(WifiManager wm) {
            this.wm = wm;
        }
    }

    public static class StabilityData {
        public List<Integer> history;
        public StabilityData(List<Integer> history) {
            this.history = history;
        }
    }

    public static class InsightsData {
        public int rssi;
        public InsightsData(int rssi) {
            this.rssi = rssi;
        }
    }
}

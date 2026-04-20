package me.theoria.wifimuscles.ui.viewmodel;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.theoria.wifimuscles.ui.helpers.ChannelHelper;
import me.theoria.wifimuscles.ui.helpers.ScoreHelper;
import me.theoria.wifimuscles.ui.helpers.StabilityHelper;

public class HomeViewModel extends AndroidViewModel {

    // UI color states for signal strength visualizations
    private static final int COLOR_GRAY   = 0xFF9E9E9E; // disconnected / neutral
    private static final int COLOR_RED    = 0xFFFF3B30; // weak signal
    private static final int COLOR_ORANGE = 0xFFFF9500; // poor / unstable
    private static final int COLOR_YELLOW = 0xFFFFD60A; // fair signal
    private static final int COLOR_GREEN  = 0xFF34C759; // good signal
    private static final int COLOR_CYAN   = 0xFF00C7FF; // excellent

    // Expose data observed in fragment to UI
    private final MutableLiveData<String> ssid = new MutableLiveData<>("..."); // WiFi name
    private final MutableLiveData<String> rssi = new MutableLiveData<>("..."); // signal strength (dBm)
    private final MutableLiveData<String> linkSpeed = new MutableLiveData<>("..."); // Mbps/Gbps
    private final MutableLiveData<Integer> linkSpeedValue = new MutableLiveData<>(0); // link speed for multi-line chart
    private final MutableLiveData<List<Integer>> linkSpeedHistory = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> frequency = new MutableLiveData<>("..."); // band (2.4/5/6 GHz)
    private final MutableLiveData<String> signalQuality = new MutableLiveData<>("..."); // text label

    private final MutableLiveData<Integer> signalLevel = new MutableLiveData<>(0); // 1–4 level for UI widgets
    private final MutableLiveData<Integer> signalColor = new MutableLiveData<>(COLOR_GRAY); // UI accent color

    // Advanced computed metrics (extra analytics layer)
    private final MutableLiveData<String> stability = new MutableLiveData<>("--"); // how stable signal is
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0); // overall Wi-Fi "health score"
    private final MutableLiveData<String> channelInfo = new MutableLiveData<>("--"); // channel diagnostics
    private final MutableLiveData<String> latency = new MutableLiveData<>("-- ms"); // ping latency
    private final MutableLiveData<List<Integer>> rssiHistory = new MutableLiveData<>(new ArrayList<>()); // signal history

    // Options
    private final MutableLiveData<Boolean> debugMode = new MutableLiveData<>(false);

    // System Wi-Fi service access
    private final WifiManager wifiManager;

    public HomeViewModel(@NonNull Application application) {
        super(application);

        // Get system Wi-Fi manager from Android
        wifiManager = (WifiManager) application.getSystemService(Context.WIFI_SERVICE);
    }

    // LiveData getters
    public LiveData<String> getSsid() { return ssid; }
    public LiveData<String> getRssi() { return rssi; }
    public LiveData<String> getLinkSpeed() { return linkSpeed; }
    public LiveData<String> getFrequency() { return frequency; }
    public LiveData<String> getSignalQuality() { return signalQuality; }
    public LiveData<Integer> getSignalLevel() { return signalLevel; }
    public LiveData<Integer> getSignalColor() { return signalColor; }

    public LiveData<String> getStability() { return stability; }
    public LiveData<Integer> getScore() { return score; }
    public LiveData<String> getChannelInfo() { return channelInfo; }
    public LiveData<String> getLatency() { return latency; }
    public LiveData<List<Integer>> getRssiHistory() { return rssiHistory; }

    // Options setters
    public void setDebugMode(boolean enabled) {
        debugMode.postValue(enabled);
    }

    // Options getters
    public LiveData<Boolean> getDebugMode() {
        return debugMode;
    }

    // Main Wi-Fi info update loop
    public void updateWifiInfo() {

        WifiInfo info = wifiManager.getConnectionInfo();

        if (info == null || info.getNetworkId() == -1) {
            postDisconnected();
            return;
        }

        // --- BASIC INFO ---
        ssid.postValue(info.getSSID().replace("\"", ""));
        rssi.postValue(info.getRssi() + " dBm");

        int speed = info.getLinkSpeed();
        linkSpeedValue.postValue(speed);

        float value;
        String unit;

        if (speed >= 1000) {
            value = speed / 1000f;
            unit = "Gbps";
        } else {
            value = speed;
            unit = "Mbps";
        }

        linkSpeed.postValue(String.format(Locale.US, "%.1f %s", value, unit));

        int freq = info.getFrequency();
        String band =
                (freq >= 2400 && freq <= 2500) ? "2.4 GHz" :
                        (freq >= 4900 && freq <= 5900) ? "5 GHz" :
                                (freq >= 5925 && freq <= 7125) ? "6 GHz" :
                                        (freq > 7125) ? "7 GHz" : freq + " MHz";
        frequency.postValue(band);

        // --- RSSI + HISTORY ---
        int rssiValue = info.getRssi();

        List<Integer> history = rssiHistory.getValue();
        if (history == null) history = new ArrayList<>();

        history.add(rssiValue);

        if (history.size() > 50) {
            history.remove(0);
        }

        rssiHistory.postValue(new ArrayList<>(history));

        // --- BLENDED RSSI ---
        int window = 10;
        int start = Math.max(0, history.size() - window);

        int sum = 0;
        int count = 0;

        for (int i = start; i < history.size(); i++) {
            sum += history.get(i);
            count++;
        }

        int avgRssi = count > 0 ? sum / count : rssiValue;

        // blend current + average
        float blendedRssi = (rssiValue * 0.6f) + (avgRssi * 0.4f);

        // slight stability influence
        float stabilityScore = StabilityHelper.calculateStabilityScore(history);
        blendedRssi -= (1f - stabilityScore) * 5f;

        int finalRssi = (int) blendedRssi;

        // --- SIGNAL LEVEL ---
        int level = WifiManager.calculateSignalLevel(finalRssi, 4) + 1;
        signalLevel.postValue(level);

        // --- QUALITY + COLOR ---
        String quality;
        int color;

        if (finalRssi >= -50) {
            quality = "Excellent";
            color = COLOR_CYAN;
        } else if (finalRssi >= -60) {
            quality = "Good";
            color = COLOR_GREEN;
        } else if (finalRssi >= -70) {
            quality = "Fair";
            color = COLOR_YELLOW;
        } else if (finalRssi >= -80) {
            quality = "Weak";
            color = COLOR_ORANGE;
        } else {
            quality = "Unusable";
            color = COLOR_RED;
        }

        signalQuality.postValue(quality);
        signalColor.postValue(color);

        // --- DISPLAY (use smoothed value) ---
        rssi.postValue(finalRssi + " dBm");

        // --- OTHER METRICS ---
        stability.postValue(StabilityHelper.calculateStability(history));
        channelInfo.postValue(ChannelHelper.getChannelInfo(freq));
        score.postValue(ScoreHelper.calculateScore(rssiValue, speed, history));
    }

    // Offline-disconnected state
    private void postDisconnected() {
        resetData();
        ssid.postValue("Not Connected");
        rssi.postValue("--");
        linkSpeed.postValue("--");
        frequency.postValue("--");
        signalQuality.postValue("--");

        signalLevel.postValue(0);
        signalColor.postValue(COLOR_GRAY);

        stability.postValue("--");
        score.postValue(0);
        channelInfo.postValue("--");
        latency.postValue("-- ms");

        rssiHistory.postValue(new ArrayList<>());
    }

    // Latency test method
    public void updateLatency() {

        new Thread(() -> {

            long start = System.currentTimeMillis();

            try {
                // Connect to a fast, reliable endpoint
                java.net.Socket socket = new java.net.Socket();
                socket.connect(
                        new java.net.InetSocketAddress("8.8.8.8", 53),
                        1500 // timeout in ms
                );

                long end = System.currentTimeMillis();
                socket.close();

                long latencyMs = end - start;

                if (latencyMs >= 1000) {
                    double seconds = latencyMs / 1000.0;
                    latency.postValue(String.format(Locale.US, "%.2f s", seconds));
                } else {
                    latency.postValue(latencyMs + " ms");
                }
            } catch (Exception e) {
                latency.postValue("Thinking");
            }

        }).start();
    }

    public void resetData() {

        ssid.postValue("...");
        rssi.postValue("...");
        linkSpeed.postValue("...");
        frequency.postValue("...");
        signalQuality.postValue("...");

        signalLevel.postValue(0);
        signalColor.postValue(COLOR_GRAY);

        stability.postValue("--");
        score.postValue(0);
        channelInfo.postValue("--");
        latency.postValue("-- ms");

        List<Integer> empty = new ArrayList<>();
        rssiHistory.postValue(empty);
    }
}
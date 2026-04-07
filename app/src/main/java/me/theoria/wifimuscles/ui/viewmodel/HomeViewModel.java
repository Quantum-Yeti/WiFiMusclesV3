package me.theoria.wifimuscles.ui.viewmodel;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Locale;

/**
 * ViewModel for the Home screen.
 * Fetches and exposes Wi-Fi connection info as LiveData for the UI to observe.
 */
public class HomeViewModel extends AndroidViewModel {

    // Constants
    private static final String UNKNOWN_SSID = "<unknown SSID>";
    private static final int    COLOR_GRAY    = 0xFFAAAAAA;
    private static final int    COLOR_GREEN   = 0xFF4CAF50;
    private static final int    COLOR_LIME    = 0xFF8BC34A;
    private static final int    COLOR_YELLOW  = 0xFFFFC107;
    private static final int    COLOR_RED     = 0xFFF44336;

    // LiveData fields — all exposed as immutable LiveData to the UI
    private final MutableLiveData<String>  ssid          = new MutableLiveData<>("--");
    private final MutableLiveData<String>  rssi          = new MutableLiveData<>("-- dBm");
    private final MutableLiveData<String>  linkSpeed     = new MutableLiveData<>("-- Mbps");
    private final MutableLiveData<String>  frequency     = new MutableLiveData<>("-- GHz");
    private final MutableLiveData<String>  signalQuality = new MutableLiveData<>("--");
    private final MutableLiveData<Integer> signalLevel   = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> signalColor   = new MutableLiveData<>(COLOR_GRAY);

    // System service
    private final WifiManager wifiManager;

    // Constructor
    public HomeViewModel(@NonNull Application application) {
        super(application);
        wifiManager = (WifiManager) application.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    // Public LiveData getters
    public LiveData<String>  getSsid()          { return ssid; }
    public LiveData<String>  getRssi()           { return rssi; }
    public LiveData<String>  getLinkSpeed()      { return linkSpeed; }
    public LiveData<String>  getFrequency()      { return frequency; }
    public LiveData<String>  getSignalQuality()  { return signalQuality; }
    public LiveData<Integer> getSignalLevel()    { return signalLevel; }
    public LiveData<Integer> getSignalColor()    { return signalColor; }

    // Main update method — called by the BroadcastReceiver on RSSI change
    public void updateWifiInfo() {
        WifiInfo info = wifiManager.getConnectionInfo();

        if (!isConnected(info)) {
            postDisconnectedState();
            return;
        }

        postSsid(info);
        postRssi(info);
        postLinkSpeed(info);
        postFrequency(info);
        postSignalLevelAndQuality(info);
    }

    // Private helpers
    /** Returns true only if we have a valid, named Wi-Fi connection. */
    private boolean isConnected(WifiInfo info) {
        return info != null
                && info.getSSID() != null
                && !info.getSSID().equals(UNKNOWN_SSID);
    }

    /** Resets all fields to their default "not connected" placeholder values. */
    private void postDisconnectedState() {
        ssid.postValue("Not Connected");
        rssi.postValue("-- dBm");
        linkSpeed.postValue("-- Mbps");
        frequency.postValue("-- GHz");
        signalQuality.postValue("--");
        signalLevel.postValue(0);
        signalColor.postValue(COLOR_GRAY);
    }

    /** Strips the quotes Android wraps around SSIDs and posts the clean name. */
    private void postSsid(WifiInfo info) {
        String cleanSsid = info.getSSID().replace("\"", "");
        ssid.postValue(cleanSsid);
    }

    /** Posts the raw signal strength in dBm. */
    private void postRssi(WifiInfo info) {
        rssi.postValue(info.getRssi() + " dBm");
    }

    /**
     * Converts link speed from Mbps (always returned by WifiInfo) to a
     * human-readable Mbps or Gbps label.
     */
    private void postLinkSpeed(WifiInfo info) {
        int speedMbps = info.getLinkSpeed();

        String label;
        if (speedMbps >= 1000) {
            // e.g. 1200 Mbps -> "1.2 Gbps"
            label = String.format(Locale.US, "%.1f Gbps", speedMbps / 1000f);
        } else {
            label = speedMbps + " Mbps";
        }

        linkSpeed.postValue(label);
    }

    /**
     * Maps the raw frequency in MHz to a friendly band label.
     * Ranges follow IEEE 802.11 spec plus Wi-Fi 6E (6 GHz) and a
     * forward-compatible catch-all for anything above 7.125 GHz.
     */
    private void postFrequency(WifiInfo info) {
        int freq = info.getFrequency(); // MHz

        String band;
        if      (freq >= 2400 && freq <= 2500) band = "2.4 GHz";
        else if (freq >= 4900 && freq <= 5900) band = "5 GHz";
        else if (freq >= 5925 && freq <= 7125) band = "6 GHz";  // Wi-Fi 6E
        else if (freq >  7125)                 band = "7 GHz";  // future-proof
        else                                   band = freq + " MHz"; // fallback

        frequency.postValue(band);
    }

    /**
     * Derives a 1–4 signal level and a matching quality label + color
     * from the raw RSSI value, then posts both.
     * RSSI thresholds (dBm):
     *   >= -50  → Excellent (green)
     *   >= -60  → Good      (lime)
     *   >= -70  → Fair      (yellow)
     *    < -70  → Poor      (red)
     */
    private void postSignalLevelAndQuality(WifiInfo info) {
        int currentRssi = info.getRssi();

        // calculateSignalLevel returns 0–3; add 1 so the view gets 1–4
        int level = WifiManager.calculateSignalLevel(currentRssi, 4) + 1;
        signalLevel.postValue(level);

        String quality;
        int    color;

        if      (currentRssi >= -50) { quality = "Excellent"; color = COLOR_GREEN;  }
        else if (currentRssi >= -60) { quality = "Good";      color = COLOR_LIME;   }
        else if (currentRssi >= -70) { quality = "Fair";      color = COLOR_YELLOW; }
        else                         { quality = "Poor";      color = COLOR_RED;    }

        signalQuality.postValue(quality);
        signalColor.postValue(color);
    }
}
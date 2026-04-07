package me.theoria.wifimuscles.ui.viewmodel;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class HomeViewModel extends AndroidViewModel {

    private final WifiManager wifiManager;

    private final MutableLiveData<String> ssid = new MutableLiveData<>("--");
    private final MutableLiveData<String> rssi = new MutableLiveData<>("-- dBm");
    private final MutableLiveData<String> linkSpeed = new MutableLiveData<>("-- Mbps");
    private final MutableLiveData<String> frequency = new MutableLiveData<>("-- GHz");
    private final MutableLiveData<String> signalQuality = new MutableLiveData<>("--");
    private final MutableLiveData<Integer> signalLevel = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> signalColor = new MutableLiveData<>(0xFFAAAAAA);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        wifiManager = (WifiManager) application.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    public LiveData<String> getSsid() { return ssid; }
    public LiveData<String> getRssi() { return rssi; }
    public LiveData<String> getLinkSpeed() { return linkSpeed; }
    public LiveData<String> getFrequency() { return frequency; }
    public LiveData<String> getSignalQuality() { return signalQuality; }
    public LiveData<Integer> getSignalLevel() { return signalLevel; }
    public LiveData<Integer> getSignalColor() { return signalColor; }

    public void updateWifiInfo() {
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null || info.getSSID() == null || info.getSSID().equals("<unknown ssid>")) {
            ssid.postValue("Not Connected");
            rssi.postValue("-- dBm");
            linkSpeed.postValue("-- Mbps");
            frequency.postValue("-- GHz");
            signalQuality.postValue("--");
            signalLevel.postValue(0);
            signalColor.postValue(0xFFAAAAAA);
            return;
        }

        String currentSsid = info.getSSID().replace("\"", "");
        ssid.postValue(currentSsid);

        int currentRssi = info.getRssi();
        int freq = info.getFrequency();
        rssi.postValue(currentRssi + " dBm");
        linkSpeed.postValue(info.getLinkSpeed() + " Mbps");

        String band;

        if (freq >= 2400 && freq <= 2500){
            band = "2.4GHz";
        } else if (freq >= 4900 && freq <= 5900) {
            band = "5 GHz";
        } else if (freq >= 5925 && freq <= 7125) {
            band = "6 GHz"; // Wi-Fi 6E
        } else if (freq > 7125) {
            band = "7 GHz"; // future proofing
        } else {
            band = freq + " MHz";
        }

        frequency.postValue(band);

        int level = WifiManager.calculateSignalLevel(currentRssi, 4);
        signalLevel.postValue(level + 1);

        int color;
        String quality;
        if (currentRssi >= -50) { quality = "Excellent"; color = 0xFF4CAF50; }
        else if (currentRssi >= -60) { quality = "Good"; color = 0xFF8BC34A; }
        else if (currentRssi >= -70) { quality = "Fair"; color = 0xFFFFC107; }
        else { quality = "Poor"; color = 0xFFF44336; }

        signalQuality.postValue(quality);
        signalColor.postValue(color);
    }
}
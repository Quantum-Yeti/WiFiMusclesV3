package me.theoria.wifimuscles.ui.viewmodel;

import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import me.theoria.wifimuscles.models.DetailsUiState;

public class DetailsViewModel extends ViewModel {

    private final MutableLiveData<DetailsUiState> state = new MutableLiveData<>();

    private final List<Integer> history = new ArrayList<>();

    public LiveData<DetailsUiState> getState() {
        return state;
    }

    public void update(int rssi, WifiInfo info, DhcpInfo dhcp) {

        if (history.isEmpty() || history.get(history.size() - 1) != rssi) {
            history.add(rssi);
        }

        if (history.size() > 10) {
            history.remove(0);
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;

        for (int v : history) {
            min = Math.min(min, v);
            max = Math.max(max, v);
            sum += v;
        }

        float avg = sum / (float) history.size();

        float variance = 0;
        for (int v : history) {
            variance += (float) Math.pow(v - avg, 2);
        }
        variance /= history.size();

        float stdDev = (float) Math.sqrt(variance);
        int jitter = max - min;

        int stability = Math.max(0, Math.min(100, 100 - (int) variance));

        String rec;
        String placement;
        String note;

        if (rssi >= -50) {
            rec = "Excellent signal";
            placement = "No extender needed";
            note = "Very close to router";
        } else if (rssi >= -60) {
            rec = "Good signal";
            placement = "Optional extender";
            note = "Stable for HD streaming";
        } else if (rssi >= -70) {
            rec = "Fair signal";
            placement = "Place extender halfway";
            note = "Possible latency spikes";
        } else if (rssi >= -80) {
            rec = "Weak signal";
            placement = "Move closer and add extender";
            note = "Expect drops";
        } else {
            rec = "Dead zone";
            placement = "Extender required";
            note = "Go back towards the router and place an extender";
        }

        state.setValue(new DetailsUiState(
                rssi,
                (int) avg,
                min,
                max,
                jitter,
                stdDev,
                stability,
                rec,
                placement,
                note,
                info,
                dhcp,
                new ArrayList<>(history)
        ));
    }
}

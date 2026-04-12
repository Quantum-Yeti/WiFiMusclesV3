package me.theoria.wifimuscles.models;

public class SignalSample {
    public final float x;
    public final float y;
    public final int rssi;

    public SignalSample(float x, float y, int rssi) {
        this.x = x;
        this.y = y;
        this.rssi = rssi;
    }
}

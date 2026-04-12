package me.theoria.wifimuscles.ui.helpers;

public class ChannelHelper {

    public static String getChannelInfo(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            int ch = (freq - 2412) / 5 + 1;
            return "2.4GHz • Ch " + ch;
        } else if (freq >= 5000 && freq <= 5900) {
            int ch = (freq - 5000) / 5;
            return "5GHz • Ch " + ch;
        } else if (freq >= 5925) {
            return "6GHz";
        }
        return "--";
    }

}

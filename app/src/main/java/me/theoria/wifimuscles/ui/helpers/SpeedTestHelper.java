package me.theoria.wifimuscles.ui.helpers;

public class SpeedTestHelper {

    public static String getEmbedHtml() {
        return "<html><body style='margin:0;padding:0;background:white;'>" +
                "<div style='width:100%;height:100%;'>" +
                "<iframe " +
                "style='border:none;width:100%;height:100%;' " +
                "src='https://openspeedtest.com/speedtest'>" +
                "</iframe>" +
                "</div>" +
                "</body></html>";
    }

}

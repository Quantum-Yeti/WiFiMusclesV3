package me.theoria.wifimuscles.models;

public class DetailItem {

    public enum Type {
        SIGNAL,
        NETWORK,
        DHCP,
        CAPABILITIES,
        STABILITY,
        INSIGHTS
    }

    public Type type;
    public Object data;

    public DetailItem(Type type, Object data) {
        this.type = type;
        this.data = data;
    }
}

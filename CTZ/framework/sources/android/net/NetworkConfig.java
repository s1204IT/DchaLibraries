package android.net;

import java.util.Locale;

public class NetworkConfig {
    public boolean dependencyMet;
    public String name;
    public int priority;
    public int radio;
    public int restoreTime;
    public int type;

    public NetworkConfig(String str) {
        String[] strArrSplit = str.split(",");
        this.name = strArrSplit[0].trim().toLowerCase(Locale.ROOT);
        this.type = Integer.parseInt(strArrSplit[1]);
        this.radio = Integer.parseInt(strArrSplit[2]);
        this.priority = Integer.parseInt(strArrSplit[3]);
        this.restoreTime = Integer.parseInt(strArrSplit[4]);
        this.dependencyMet = Boolean.parseBoolean(strArrSplit[5]);
    }

    public boolean isDefault() {
        return this.type == this.radio;
    }
}

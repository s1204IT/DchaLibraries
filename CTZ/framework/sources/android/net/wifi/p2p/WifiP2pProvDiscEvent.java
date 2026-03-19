package android.net.wifi.p2p;

import android.net.wifi.WifiEnterpriseConfig;

public class WifiP2pProvDiscEvent {
    public static final int ENTER_PIN = 3;
    public static final int PBC_REQ = 1;
    public static final int PBC_RSP = 2;
    public static final int SHOW_PIN = 4;
    private static final String TAG = "WifiP2pProvDiscEvent";
    public WifiP2pDevice device;
    public int event;
    public String pin;

    public WifiP2pProvDiscEvent() {
        this.device = new WifiP2pDevice();
    }

    public WifiP2pProvDiscEvent(String str) throws IllegalArgumentException {
        String[] strArrSplit = str.split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        if (strArrSplit.length < 2) {
            throw new IllegalArgumentException("Malformed event " + str);
        }
        if (strArrSplit[0].endsWith("PBC-REQ")) {
            this.event = 1;
        } else if (strArrSplit[0].endsWith("PBC-RESP")) {
            this.event = 2;
        } else if (strArrSplit[0].endsWith("ENTER-PIN")) {
            this.event = 3;
        } else {
            if (!strArrSplit[0].endsWith("SHOW-PIN")) {
                throw new IllegalArgumentException("Malformed event " + str);
            }
            this.event = 4;
        }
        if (this.event == 1 || this.event == 3) {
            this.device = new WifiP2pDevice(str);
        } else {
            this.device = new WifiP2pDevice();
        }
        this.device.deviceAddress = strArrSplit[1];
        if (this.event == 4) {
            this.pin = strArrSplit[2];
        }
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.device);
        stringBuffer.append("\n event: ");
        stringBuffer.append(this.event);
        stringBuffer.append("\n pin: ");
        stringBuffer.append(this.pin);
        return stringBuffer.toString();
    }
}

package com.android.internal.telephony;

import java.util.BitSet;

public class HardwareConfig {
    public static final int DEV_HARDWARE_STATE_DISABLED = 2;
    public static final int DEV_HARDWARE_STATE_ENABLED = 0;
    public static final int DEV_HARDWARE_STATE_STANDBY = 1;
    public static final int DEV_HARDWARE_TYPE_MODEM = 0;
    public static final int DEV_HARDWARE_TYPE_SIM = 1;
    public static final int DEV_MODEM_RIL_MODEL_MULTIPLE = 1;
    public static final int DEV_MODEM_RIL_MODEL_SINGLE = 0;
    static final String LOG_TAG = "HardwareConfig";
    public int maxActiveDataCall;
    public int maxActiveVoiceCall;
    public int maxStandby;
    public String modemUuid;
    public BitSet rat;
    public int rilModel;
    public int state;
    public int type;
    public String uuid;

    public HardwareConfig(int i) {
        this.type = i;
    }

    public HardwareConfig(String str) {
        String[] strArrSplit = str.split(",");
        this.type = Integer.parseInt(strArrSplit[0]);
        switch (this.type) {
            case 0:
                assignModem(strArrSplit[1].trim(), Integer.parseInt(strArrSplit[2]), Integer.parseInt(strArrSplit[3]), Integer.parseInt(strArrSplit[4]), Integer.parseInt(strArrSplit[5]), Integer.parseInt(strArrSplit[6]), Integer.parseInt(strArrSplit[7]));
                break;
            case 1:
                assignSim(strArrSplit[1].trim(), Integer.parseInt(strArrSplit[2]), strArrSplit[3].trim());
                break;
        }
    }

    public void assignModem(String str, int i, int i2, int i3, int i4, int i5, int i6) {
        if (this.type == 0) {
            char[] charArray = Integer.toBinaryString(i3).toCharArray();
            this.uuid = str;
            this.state = i;
            this.rilModel = i2;
            this.rat = new BitSet(charArray.length);
            for (int i7 = 0; i7 < charArray.length; i7++) {
                this.rat.set(i7, charArray[i7] == '1');
            }
            this.maxActiveVoiceCall = i4;
            this.maxActiveDataCall = i5;
            this.maxStandby = i6;
        }
    }

    public void assignSim(String str, int i, String str2) {
        if (this.type == 1) {
            this.uuid = str;
            this.modemUuid = str2;
            this.state = i;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.type == 0) {
            sb.append("Modem ");
            sb.append("{ uuid=" + this.uuid);
            sb.append(", state=" + this.state);
            sb.append(", rilModel=" + this.rilModel);
            sb.append(", rat=" + this.rat.toString());
            sb.append(", maxActiveVoiceCall=" + this.maxActiveVoiceCall);
            sb.append(", maxActiveDataCall=" + this.maxActiveDataCall);
            sb.append(", maxStandby=" + this.maxStandby);
            sb.append(" }");
        } else if (this.type == 1) {
            sb.append("Sim ");
            sb.append("{ uuid=" + this.uuid);
            sb.append(", modemUuid=" + this.modemUuid);
            sb.append(", state=" + this.state);
            sb.append(" }");
        } else {
            sb.append("Invalid Configration");
        }
        return sb.toString();
    }

    public int compareTo(HardwareConfig hardwareConfig) {
        return toString().compareTo(hardwareConfig.toString());
    }
}

package com.android.internal.telephony;

import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;

public class DriverCall implements Comparable<DriverCall> {
    public static final int AUDIO_QUALITY_AMR = 1;
    public static final int AUDIO_QUALITY_AMR_WB = 2;
    public static final int AUDIO_QUALITY_EVRC = 6;
    public static final int AUDIO_QUALITY_EVRC_B = 7;
    public static final int AUDIO_QUALITY_EVRC_NW = 9;
    public static final int AUDIO_QUALITY_EVRC_WB = 8;
    public static final int AUDIO_QUALITY_GSM_EFR = 3;
    public static final int AUDIO_QUALITY_GSM_FR = 4;
    public static final int AUDIO_QUALITY_GSM_HR = 5;
    public static final int AUDIO_QUALITY_UNSPECIFIED = 0;
    protected static final String LOG_TAG = "DriverCall";
    public int TOA;
    public int als;
    public int audioQuality = 0;
    public int index;
    public boolean isMT;
    public boolean isMpty;
    public boolean isVoice;
    public boolean isVoicePrivacy;
    public String name;
    public int namePresentation;
    public String number;
    public int numberPresentation;
    public State state;
    public UUSInfo uusInfo;

    public enum State {
        ACTIVE,
        HOLDING,
        DIALING,
        ALERTING,
        INCOMING,
        WAITING
    }

    static DriverCall fromCLCCLine(String str) {
        DriverCall driverCall = new DriverCall();
        ATResponseParser aTResponseParser = new ATResponseParser(str);
        try {
            driverCall.index = aTResponseParser.nextInt();
            driverCall.isMT = aTResponseParser.nextBoolean();
            driverCall.state = stateFromCLCC(aTResponseParser.nextInt());
            driverCall.isVoice = aTResponseParser.nextInt() == 0;
            driverCall.isMpty = aTResponseParser.nextBoolean();
            driverCall.numberPresentation = 1;
            if (aTResponseParser.hasMore()) {
                driverCall.number = PhoneNumberUtils.extractNetworkPortionAlt(aTResponseParser.nextString());
                if (driverCall.number.length() == 0) {
                    driverCall.number = null;
                }
                driverCall.TOA = aTResponseParser.nextInt();
                driverCall.number = PhoneNumberUtils.stringFromStringAndTOA(driverCall.number, driverCall.TOA);
            }
            return driverCall;
        } catch (ATParseEx e) {
            Rlog.e(LOG_TAG, "Invalid CLCC line: '" + str + "'");
            return null;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id=");
        sb.append(this.index);
        sb.append(",");
        sb.append(this.state);
        sb.append(",toa=");
        sb.append(this.TOA);
        sb.append(",");
        sb.append(this.isMpty ? "conf" : "norm");
        sb.append(",");
        sb.append(this.isMT ? "mt" : "mo");
        sb.append(",");
        sb.append(this.als);
        sb.append(",");
        sb.append(this.isVoice ? "voc" : "nonvoc");
        sb.append(",");
        sb.append(this.isVoicePrivacy ? "evp" : "noevp");
        sb.append(",,cli=");
        sb.append(this.numberPresentation);
        sb.append(",,");
        sb.append(this.namePresentation);
        sb.append(",audioQuality=");
        sb.append(this.audioQuality);
        return sb.toString();
    }

    public static State stateFromCLCC(int i) throws ATParseEx {
        switch (i) {
            case 0:
                return State.ACTIVE;
            case 1:
                return State.HOLDING;
            case 2:
                return State.DIALING;
            case 3:
                return State.ALERTING;
            case 4:
                return State.INCOMING;
            case 5:
                return State.WAITING;
            default:
                throw new ATParseEx("illegal call state " + i);
        }
    }

    public static int presentationFromCLIP(int i) throws ATParseEx {
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                throw new ATParseEx("illegal presentation " + i);
        }
    }

    @Override
    public int compareTo(DriverCall driverCall) {
        if (this.index < driverCall.index) {
            return -1;
        }
        if (this.index == driverCall.index) {
            return 0;
        }
        return 1;
    }
}

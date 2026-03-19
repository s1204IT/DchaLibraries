package com.mediatek.internal.telephony;

public class CellBroadcastConfigInfo {
    public String channelConfigInfo;
    public boolean isAllLanguageOn;
    public String languageConfigInfo;
    public int mode;

    public CellBroadcastConfigInfo(int mode, String channels, String languages, boolean allOn) {
        this.mode = 1;
        this.channelConfigInfo = null;
        this.languageConfigInfo = null;
        this.isAllLanguageOn = false;
        this.mode = mode;
        this.channelConfigInfo = channels;
        this.languageConfigInfo = languages;
        this.isAllLanguageOn = allOn;
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("CellBroadcastConfigInfo: mode = ");
        ret.append(this.mode);
        ret.append(", channel = ");
        ret.append(this.channelConfigInfo);
        ret.append(", language = ");
        if (!this.isAllLanguageOn) {
            ret.append(this.languageConfigInfo);
        } else {
            ret.append("all");
        }
        return ret.toString();
    }
}

package com.android.bluetooth.avrcp;

class Folder implements Cloneable {
    public boolean isPlayable;
    public String mediaId;
    public String title;

    Folder(String str, boolean z, String str2) {
        this.mediaId = str;
        this.isPlayable = z;
        this.title = str2;
    }

    public Folder m6clone() {
        return new Folder(this.mediaId, this.isPlayable, this.title);
    }
}

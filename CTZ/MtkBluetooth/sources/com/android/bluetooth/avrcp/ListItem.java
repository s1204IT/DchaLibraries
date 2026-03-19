package com.android.bluetooth.avrcp;

class ListItem implements Cloneable {
    public Folder folder;
    public boolean isFolder;
    public Metadata song;

    ListItem(Folder folder) {
        this.isFolder = false;
        this.isFolder = true;
        this.folder = folder;
    }

    ListItem(Metadata metadata) {
        this.isFolder = false;
        this.isFolder = false;
        this.song = metadata;
    }

    public ListItem m7clone() {
        if (this.isFolder) {
            return new ListItem(this.folder.m6clone());
        }
        return new ListItem(this.song.m8clone());
    }
}

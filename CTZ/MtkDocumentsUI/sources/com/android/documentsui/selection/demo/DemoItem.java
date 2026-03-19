package com.android.documentsui.selection.demo;

public class DemoItem {
    private final String mId;
    private final String mName;

    DemoItem(String str, String str2) {
        this.mId = str;
        this.mName = str2;
    }

    public String getId() {
        return this.mId;
    }

    public String getName() {
        return this.mName;
    }
}

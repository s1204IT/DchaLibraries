package com.android.launcher2;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class ItemInfo {
    int cellX;
    int cellY;
    long container;
    CharSequence contentDescription;
    int[] dropPos;
    long id;
    int itemType;
    int minSpanX;
    int minSpanY;
    boolean requiresDbUpdate;
    int screen;
    int spanX;
    int spanY;
    CharSequence title;
    UserHandle user;

    ItemInfo() {
        this.id = -1L;
        this.container = -1L;
        this.screen = -1;
        this.cellX = -1;
        this.cellY = -1;
        this.spanX = 1;
        this.spanY = 1;
        this.minSpanX = 1;
        this.minSpanY = 1;
        this.requiresDbUpdate = false;
        this.dropPos = null;
        this.user = Process.myUserHandle();
    }

    ItemInfo(ItemInfo info) {
        this.id = -1L;
        this.container = -1L;
        this.screen = -1;
        this.cellX = -1;
        this.cellY = -1;
        this.spanX = 1;
        this.spanY = 1;
        this.minSpanX = 1;
        this.minSpanY = 1;
        this.requiresDbUpdate = false;
        this.dropPos = null;
        this.id = info.id;
        this.cellX = info.cellX;
        this.cellY = info.cellY;
        this.spanX = info.spanX;
        this.spanY = info.spanY;
        this.screen = info.screen;
        this.itemType = info.itemType;
        this.container = info.container;
        this.user = info.user;
        this.contentDescription = info.contentDescription;
        LauncherModel.checkItemInfo(this);
    }

    protected void updateUser(Intent intent) {
        if (intent != null && intent.hasExtra("profile")) {
            this.user = (UserHandle) intent.getParcelableExtra("profile");
        }
    }

    void onAddToDatabase(Context context, ContentValues values) {
        values.put("itemType", Integer.valueOf(this.itemType));
        values.put("container", Long.valueOf(this.container));
        values.put("screen", Integer.valueOf(this.screen));
        values.put("cellX", Integer.valueOf(this.cellX));
        values.put("cellY", Integer.valueOf(this.cellY));
        values.put("spanX", Integer.valueOf(this.spanX));
        values.put("spanY", Integer.valueOf(this.spanY));
        long serialNumber = ((UserManager) context.getSystemService("user")).getSerialNumberForUser(this.user);
        values.put("profileId", Long.valueOf(serialNumber));
    }

    void updateValuesWithCoordinates(ContentValues values, int cellX, int cellY) {
        values.put("cellX", Integer.valueOf(cellX));
        values.put("cellY", Integer.valueOf(cellY));
    }

    static byte[] flattenBitmap(Bitmap bitmap) {
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w("Favorite", "Could not write icon");
            return null;
        }
    }

    static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap != null) {
            byte[] data = flattenBitmap(bitmap);
            values.put("icon", data);
        }
    }

    void unbind() {
    }

    public String toString() {
        return "Item(id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screen + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " dropPos=" + this.dropPos + " user=" + this.user + ")";
    }
}

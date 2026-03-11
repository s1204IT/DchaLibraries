package com.android.launcher3;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import java.util.Arrays;

public class ItemInfo {
    public int cellX;
    public int cellY;
    public long container;
    public CharSequence contentDescription;
    public int[] dropPos;
    public long id;
    public int itemType;
    public int minSpanX;
    public int minSpanY;
    public int rank;
    public boolean requiresDbUpdate;
    public long screenId;
    public int spanX;
    public int spanY;
    public CharSequence title;
    public UserHandleCompat user;

    public ItemInfo() {
        this.id = -1L;
        this.container = -1L;
        this.screenId = -1L;
        this.cellX = -1;
        this.cellY = -1;
        this.spanX = 1;
        this.spanY = 1;
        this.minSpanX = 1;
        this.minSpanY = 1;
        this.rank = 0;
        this.requiresDbUpdate = false;
        this.dropPos = null;
        this.user = UserHandleCompat.myUserHandle();
    }

    ItemInfo(ItemInfo info) {
        this.id = -1L;
        this.container = -1L;
        this.screenId = -1L;
        this.cellX = -1;
        this.cellY = -1;
        this.spanX = 1;
        this.spanY = 1;
        this.minSpanX = 1;
        this.minSpanY = 1;
        this.rank = 0;
        this.requiresDbUpdate = false;
        this.dropPos = null;
        copyFrom(info);
        LauncherModel.checkItemInfo(this);
    }

    public void copyFrom(ItemInfo info) {
        this.id = info.id;
        this.cellX = info.cellX;
        this.cellY = info.cellY;
        this.spanX = info.spanX;
        this.spanY = info.spanY;
        this.rank = info.rank;
        this.screenId = info.screenId;
        this.itemType = info.itemType;
        this.container = info.container;
        this.user = info.user;
        this.contentDescription = info.contentDescription;
    }

    public Intent getIntent() {
        throw new RuntimeException("Unexpected Intent");
    }

    void onAddToDatabase(Context context, ContentValues values) {
        values.put("itemType", Integer.valueOf(this.itemType));
        values.put("container", Long.valueOf(this.container));
        values.put("screen", Long.valueOf(this.screenId));
        values.put("cellX", Integer.valueOf(this.cellX));
        values.put("cellY", Integer.valueOf(this.cellY));
        values.put("spanX", Integer.valueOf(this.spanX));
        values.put("spanY", Integer.valueOf(this.spanY));
        values.put("rank", Integer.valueOf(this.rank));
        long serialNumber = UserManagerCompat.getInstance(context).getSerialNumberForUser(this.user);
        values.put("profileId", Long.valueOf(serialNumber));
        if (this.screenId != -201) {
        } else {
            throw new RuntimeException("Screen id should not be EXTRA_EMPTY_SCREEN_ID");
        }
    }

    static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        byte[] data = Utilities.flattenBitmap(bitmap);
        values.put("icon", data);
    }

    void unbind() {
    }

    public String toString() {
        return "Item(id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screenId + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " dropPos=" + Arrays.toString(this.dropPos) + " user=" + this.user + ")";
    }

    public boolean isDisabled() {
        return false;
    }
}

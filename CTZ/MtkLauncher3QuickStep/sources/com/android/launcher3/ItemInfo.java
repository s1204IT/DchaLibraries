package com.android.launcher3;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Process;
import android.os.UserHandle;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.util.ContentWriter;

public class ItemInfo {
    public static final int NO_ID = -1;
    public int cellX;
    public int cellY;
    public long container;
    public CharSequence contentDescription;
    public long id;
    public int itemType;
    public int minSpanX;
    public int minSpanY;
    public int rank;
    public long screenId;
    public int spanX;
    public int spanY;
    public CharSequence title;
    public UserHandle user;

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
        this.user = Process.myUserHandle();
    }

    ItemInfo(ItemInfo itemInfo) {
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
        copyFrom(itemInfo);
        LauncherModel.checkItemInfo(this);
    }

    public void copyFrom(ItemInfo itemInfo) {
        this.id = itemInfo.id;
        this.cellX = itemInfo.cellX;
        this.cellY = itemInfo.cellY;
        this.spanX = itemInfo.spanX;
        this.spanY = itemInfo.spanY;
        this.rank = itemInfo.rank;
        this.screenId = itemInfo.screenId;
        this.itemType = itemInfo.itemType;
        this.container = itemInfo.container;
        this.user = itemInfo.user;
        this.contentDescription = itemInfo.contentDescription;
    }

    public Intent getIntent() {
        return null;
    }

    public ComponentName getTargetComponent() {
        Intent intent = getIntent();
        if (intent != null) {
            return intent.getComponent();
        }
        return null;
    }

    public void writeToValues(ContentWriter contentWriter) {
        contentWriter.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, Integer.valueOf(this.itemType)).put(LauncherSettings.Favorites.CONTAINER, Long.valueOf(this.container)).put(LauncherSettings.Favorites.SCREEN, Long.valueOf(this.screenId)).put(LauncherSettings.Favorites.CELLX, Integer.valueOf(this.cellX)).put(LauncherSettings.Favorites.CELLY, Integer.valueOf(this.cellY)).put(LauncherSettings.Favorites.SPANX, Integer.valueOf(this.spanX)).put(LauncherSettings.Favorites.SPANY, Integer.valueOf(this.spanY)).put(LauncherSettings.Favorites.RANK, Integer.valueOf(this.rank));
    }

    public void readFromValues(ContentValues contentValues) {
        this.itemType = contentValues.getAsInteger(LauncherSettings.BaseLauncherColumns.ITEM_TYPE).intValue();
        this.container = contentValues.getAsLong(LauncherSettings.Favorites.CONTAINER).longValue();
        this.screenId = contentValues.getAsLong(LauncherSettings.Favorites.SCREEN).longValue();
        this.cellX = contentValues.getAsInteger(LauncherSettings.Favorites.CELLX).intValue();
        this.cellY = contentValues.getAsInteger(LauncherSettings.Favorites.CELLY).intValue();
        this.spanX = contentValues.getAsInteger(LauncherSettings.Favorites.SPANX).intValue();
        this.spanY = contentValues.getAsInteger(LauncherSettings.Favorites.SPANY).intValue();
        this.rank = contentValues.getAsInteger(LauncherSettings.Favorites.RANK).intValue();
    }

    public void onAddToDatabase(ContentWriter contentWriter) {
        if (this.screenId == -201) {
            throw new RuntimeException("Screen id should not be EXTRA_EMPTY_SCREEN_ID");
        }
        writeToValues(contentWriter);
        contentWriter.put(LauncherSettings.Favorites.PROFILE_ID, this.user);
    }

    public final String toString() {
        return getClass().getSimpleName() + "(" + dumpProperties() + ")";
    }

    protected String dumpProperties() {
        return "id=" + this.id + " type=" + LauncherSettings.Favorites.itemTypeToString(this.itemType) + " container=" + LauncherSettings.Favorites.containerToString((int) this.container) + " screen=" + this.screenId + " cell(" + this.cellX + "," + this.cellY + ") span(" + this.spanX + "," + this.spanY + ") minSpan(" + this.minSpanX + "," + this.minSpanY + ") rank=" + this.rank + " user=" + this.user + " title=" + ((Object) this.title);
    }

    public boolean isDisabled() {
        return false;
    }
}

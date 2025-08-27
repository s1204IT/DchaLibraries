package com.android.launcher3.model;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import com.android.launcher3.AppInfo;
import com.android.launcher3.IconCache;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.graphics.BitmapInfo;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.util.ContentWriter;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.LongArrayMap;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class LoaderCursor extends CursorWrapper {
    private static final String TAG = "LoaderCursor";
    public final LongSparseArray<UserHandle> allUsers;
    private final int cellXIndex;
    private final int cellYIndex;
    public long container;
    private final int containerIndex;
    private final int iconIndex;
    private final int iconPackageIndex;
    private final int iconResourceIndex;
    public long id;
    private final int idIndex;
    private final int intentIndex;
    public int itemType;
    private final int itemTypeIndex;
    private final ArrayList<Long> itemsToRemove;
    private final Context mContext;
    private final InvariantDeviceProfile mIDP;
    private final IconCache mIconCache;
    private final UserManagerCompat mUserManager;
    private final LongArrayMap<GridOccupancy> occupied;
    private final int profileIdIndex;
    public int restoreFlag;
    private final int restoredIndex;
    private final ArrayList<Long> restoredRows;
    private final int screenIndex;
    public long serialNumber;
    public final int titleIndex;
    public UserHandle user;

    public LoaderCursor(Cursor cursor, LauncherAppState launcherAppState) {
        super(cursor);
        this.allUsers = new LongSparseArray<>();
        this.itemsToRemove = new ArrayList<>();
        this.restoredRows = new ArrayList<>();
        this.occupied = new LongArrayMap<>();
        this.mContext = launcherAppState.getContext();
        this.mIconCache = launcherAppState.getIconCache();
        this.mIDP = launcherAppState.getInvariantDeviceProfile();
        this.mUserManager = UserManagerCompat.getInstance(this.mContext);
        this.iconIndex = getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON);
        this.iconPackageIndex = getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE);
        this.iconResourceIndex = getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE);
        this.titleIndex = getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.TITLE);
        this.idIndex = getColumnIndexOrThrow("_id");
        this.containerIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
        this.itemTypeIndex = getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
        this.screenIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
        this.cellXIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
        this.cellYIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
        this.profileIdIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.PROFILE_ID);
        this.restoredIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.RESTORED);
        this.intentIndex = getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
    }

    @Override // android.database.CursorWrapper, android.database.Cursor
    public boolean moveToNext() {
        boolean zMoveToNext = super.moveToNext();
        if (zMoveToNext) {
            this.itemType = getInt(this.itemTypeIndex);
            this.container = getInt(this.containerIndex);
            this.id = getLong(this.idIndex);
            this.serialNumber = getInt(this.profileIdIndex);
            this.user = this.allUsers.get(this.serialNumber);
            this.restoreFlag = getInt(this.restoredIndex);
        }
        return zMoveToNext;
    }

    public Intent parseIntent() {
        String string = getString(this.intentIndex);
        try {
            if (TextUtils.isEmpty(string)) {
                return null;
            }
            return Intent.parseUri(string, 0);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error parsing Intent");
            return null;
        }
    }

    public ShortcutInfo loadSimpleShortcut() {
        ShortcutInfo shortcutInfo = new ShortcutInfo();
        shortcutInfo.user = this.user;
        shortcutInfo.itemType = this.itemType;
        shortcutInfo.title = getTitle();
        if (!loadIcon(shortcutInfo)) {
            this.mIconCache.getDefaultIcon(shortcutInfo.user).applyTo(shortcutInfo);
        }
        return shortcutInfo;
    }

    protected boolean loadIcon(ShortcutInfo shortcutInfo) throws PackageManager.NameNotFoundException {
        if (this.itemType == 1) {
            String string = getString(this.iconPackageIndex);
            String string2 = getString(this.iconResourceIndex);
            if (!TextUtils.isEmpty(string) || !TextUtils.isEmpty(string2)) {
                shortcutInfo.iconResource = new Intent.ShortcutIconResource();
                shortcutInfo.iconResource.packageName = string;
                shortcutInfo.iconResource.resourceName = string2;
                LauncherIcons launcherIconsObtain = LauncherIcons.obtain(this.mContext);
                BitmapInfo bitmapInfoCreateIconBitmap = launcherIconsObtain.createIconBitmap(shortcutInfo.iconResource);
                launcherIconsObtain.recycle();
                if (bitmapInfoCreateIconBitmap != null) {
                    bitmapInfoCreateIconBitmap.applyTo(shortcutInfo);
                    return true;
                }
            }
        }
        byte[] blob = getBlob(this.iconIndex);
        try {
            LauncherIcons launcherIconsObtain2 = LauncherIcons.obtain(this.mContext);
            try {
                launcherIconsObtain2.createIconBitmap(BitmapFactory.decodeByteArray(blob, 0, blob.length)).applyTo(shortcutInfo);
                if (launcherIconsObtain2 != null) {
                    launcherIconsObtain2.close();
                }
                return true;
            } finally {
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load icon for info " + shortcutInfo, e);
            return false;
        }
    }

    private String getTitle() {
        String string = getString(this.titleIndex);
        return TextUtils.isEmpty(string) ? "" : Utilities.trim(string);
    }

    public ShortcutInfo getRestoredItemInfo(Intent intent) {
        ShortcutInfo shortcutInfo = new ShortcutInfo();
        shortcutInfo.user = this.user;
        shortcutInfo.intent = intent;
        if (!loadIcon(shortcutInfo)) {
            this.mIconCache.getTitleAndIcon(shortcutInfo, false);
        }
        if (hasRestoreFlag(1)) {
            String title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                shortcutInfo.title = Utilities.trim(title);
            }
        } else if (hasRestoreFlag(2)) {
            if (TextUtils.isEmpty(shortcutInfo.title)) {
                shortcutInfo.title = getTitle();
            }
        } else {
            throw new InvalidParameterException("Invalid restoreType " + this.restoreFlag);
        }
        shortcutInfo.contentDescription = this.mUserManager.getBadgedLabelForUser(shortcutInfo.title, shortcutInfo.user);
        shortcutInfo.itemType = this.itemType;
        shortcutInfo.status = this.restoreFlag;
        return shortcutInfo;
    }

    public ShortcutInfo getAppShortcutInfo(Intent intent, boolean z, boolean z2) throws PackageManager.NameNotFoundException {
        if (this.user == null) {
            Log.d(TAG, "Null user found in getShortcutInfo");
            return null;
        }
        ComponentName component = intent.getComponent();
        if (component == null) {
            Log.d(TAG, "Missing component found in getShortcutInfo");
            return null;
        }
        Intent intent2 = new Intent("android.intent.action.MAIN", (Uri) null);
        intent2.addCategory("android.intent.category.LAUNCHER");
        intent2.setComponent(component);
        LauncherActivityInfo launcherActivityInfoResolveActivity = LauncherAppsCompat.getInstance(this.mContext).resolveActivity(intent2, this.user);
        if (launcherActivityInfoResolveActivity == null && !z) {
            Log.d(TAG, "Missing activity found in getShortcutInfo: " + component);
            return null;
        }
        ShortcutInfo shortcutInfo = new ShortcutInfo();
        shortcutInfo.itemType = 0;
        shortcutInfo.user = this.user;
        shortcutInfo.intent = intent2;
        this.mIconCache.getTitleAndIcon(shortcutInfo, launcherActivityInfoResolveActivity, z2);
        if (this.mIconCache.isDefaultIcon(shortcutInfo.iconBitmap, this.user)) {
            loadIcon(shortcutInfo);
        }
        if (launcherActivityInfoResolveActivity != null) {
            AppInfo.updateRuntimeFlagsForActivityTarget(shortcutInfo, launcherActivityInfoResolveActivity);
        }
        if (TextUtils.isEmpty(shortcutInfo.title)) {
            shortcutInfo.title = getTitle();
        }
        if (shortcutInfo.title == null) {
            shortcutInfo.title = component.getClassName();
        }
        shortcutInfo.contentDescription = this.mUserManager.getBadgedLabelForUser(shortcutInfo.title, shortcutInfo.user);
        return shortcutInfo;
    }

    public ContentWriter updater() {
        return new ContentWriter(this.mContext, new ContentWriter.CommitParams("_id= ?", new String[]{Long.toString(this.id)}));
    }

    public void markDeleted(String str) {
        FileLog.e(TAG, str);
        this.itemsToRemove.add(Long.valueOf(this.id));
    }

    public boolean commitDeleted() {
        if (this.itemsToRemove.size() > 0) {
            this.mContext.getContentResolver().delete(LauncherSettings.Favorites.CONTENT_URI, Utilities.createDbSelectionQuery("_id", this.itemsToRemove), null);
            return true;
        }
        return false;
    }

    public void markRestored() {
        if (this.restoreFlag != 0) {
            this.restoredRows.add(Long.valueOf(this.id));
            this.restoreFlag = 0;
        }
    }

    public boolean hasRestoreFlag(int i) {
        return (i & this.restoreFlag) != 0;
    }

    public void commitRestoredItems() {
        if (this.restoredRows.size() > 0) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(LauncherSettings.Favorites.RESTORED, (Integer) 0);
            this.mContext.getContentResolver().update(LauncherSettings.Favorites.CONTENT_URI, contentValues, Utilities.createDbSelectionQuery("_id", this.restoredRows), null);
        }
    }

    public boolean isOnWorkspaceOrHotseat() {
        return this.container == -100 || this.container == -101;
    }

    public void applyCommonProperties(ItemInfo itemInfo) {
        itemInfo.id = this.id;
        itemInfo.container = this.container;
        itemInfo.screenId = getInt(this.screenIndex);
        itemInfo.cellX = getInt(this.cellXIndex);
        itemInfo.cellY = getInt(this.cellYIndex);
    }

    public void checkAndAddItem(ItemInfo itemInfo, BgDataModel bgDataModel) {
        if (checkItemPlacement(itemInfo, bgDataModel.workspaceScreens)) {
            bgDataModel.addItem(this.mContext, itemInfo, false);
        } else {
            markDeleted("Item position overlap");
        }
    }

    protected boolean checkItemPlacement(ItemInfo itemInfo, ArrayList<Long> arrayList) {
        long j = itemInfo.screenId;
        if (itemInfo.container == -101) {
            GridOccupancy gridOccupancy = this.occupied.get(-101L);
            if (itemInfo.screenId >= this.mIDP.numHotseatIcons) {
                Log.e(TAG, "Error loading shortcut " + itemInfo + " into hotseat position " + itemInfo.screenId + ", position out of bounds: (0 to " + (this.mIDP.numHotseatIcons - 1) + ")");
                return false;
            }
            if (gridOccupancy != null) {
                if (gridOccupancy.cells[(int) itemInfo.screenId][0]) {
                    Log.e(TAG, "Error loading shortcut into hotseat " + itemInfo + " into position (" + itemInfo.screenId + ":" + itemInfo.cellX + "," + itemInfo.cellY + ") already occupied");
                    return false;
                }
                gridOccupancy.cells[(int) itemInfo.screenId][0] = true;
                return true;
            }
            GridOccupancy gridOccupancy2 = new GridOccupancy(this.mIDP.numHotseatIcons, 1);
            gridOccupancy2.cells[(int) itemInfo.screenId][0] = true;
            this.occupied.put(-101L, gridOccupancy2);
            return true;
        }
        if (itemInfo.container != -100) {
            return true;
        }
        if (!arrayList.contains(Long.valueOf(itemInfo.screenId))) {
            return false;
        }
        int i = this.mIDP.numColumns;
        int i2 = this.mIDP.numRows;
        if ((itemInfo.container == -100 && itemInfo.cellX < 0) || itemInfo.cellY < 0 || itemInfo.cellX + itemInfo.spanX > i || itemInfo.cellY + itemInfo.spanY > i2) {
            Log.e(TAG, "Error loading shortcut " + itemInfo + " into cell (" + j + "-" + itemInfo.screenId + ":" + itemInfo.cellX + "," + itemInfo.cellY + ") out of screen bounds ( " + i + "x" + i2 + ")");
            return false;
        }
        if (!this.occupied.containsKey(itemInfo.screenId)) {
            int i3 = i + 1;
            GridOccupancy gridOccupancy3 = new GridOccupancy(i3, i2 + 1);
            if (itemInfo.screenId == 0) {
                gridOccupancy3.markCells(0, 0, i3, 1, false);
            }
            this.occupied.put(itemInfo.screenId, gridOccupancy3);
        }
        GridOccupancy gridOccupancy4 = this.occupied.get(itemInfo.screenId);
        if (gridOccupancy4.isRegionVacant(itemInfo.cellX, itemInfo.cellY, itemInfo.spanX, itemInfo.spanY)) {
            gridOccupancy4.markCells(itemInfo, true);
            return true;
        }
        Log.e(TAG, "Error loading shortcut " + itemInfo + " into cell (" + j + "-" + itemInfo.screenId + ":" + itemInfo.cellX + "," + itemInfo.cellX + "," + itemInfo.spanX + "," + itemInfo.spanY + ") already occupied");
        return false;
    }
}

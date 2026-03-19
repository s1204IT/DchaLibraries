package com.android.server.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.ShortcutService;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class ShortcutPackage extends ShortcutPackageItem {
    private static final String ATTR_ACTIVITY = "activity";
    private static final String ATTR_BITMAP_PATH = "bitmap-path";
    private static final String ATTR_CALL_COUNT = "call-count";
    private static final String ATTR_DISABLED_MESSAGE = "dmessage";
    private static final String ATTR_DISABLED_MESSAGE_RES_ID = "dmessageid";
    private static final String ATTR_DISABLED_MESSAGE_RES_NAME = "dmessagename";
    private static final String ATTR_DISABLED_REASON = "disabled-reason";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_ICON_RES_ID = "icon-res";
    private static final String ATTR_ICON_RES_NAME = "icon-resname";
    private static final String ATTR_ID = "id";
    private static final String ATTR_INTENT_LEGACY = "intent";
    private static final String ATTR_INTENT_NO_EXTRA = "intent-base";
    private static final String ATTR_LAST_RESET = "last-reset";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_NAME_XMLUTILS = "name";
    private static final String ATTR_RANK = "rank";
    private static final String ATTR_TEXT = "text";
    private static final String ATTR_TEXT_RES_ID = "textid";
    private static final String ATTR_TEXT_RES_NAME = "textname";
    private static final String ATTR_TIMESTAMP = "timestamp";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_TITLE_RES_ID = "titleid";
    private static final String ATTR_TITLE_RES_NAME = "titlename";
    private static final String KEY_BITMAPS = "bitmaps";
    private static final String KEY_BITMAP_BYTES = "bitmapBytes";
    private static final String KEY_DYNAMIC = "dynamic";
    private static final String KEY_MANIFEST = "manifest";
    private static final String KEY_PINNED = "pinned";
    private static final String NAME_CATEGORIES = "categories";
    private static final String TAG = "ShortcutService";
    private static final String TAG_CATEGORIES = "categories";
    private static final String TAG_EXTRAS = "extras";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_INTENT_EXTRAS_LEGACY = "intent-extras";
    static final String TAG_ROOT = "package";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String TAG_STRING_ARRAY_XMLUTILS = "string-array";
    private static final String TAG_VERIFY = "ShortcutService.verify";
    private int mApiCallCount;
    private long mLastKnownForegroundElapsedTime;
    private long mLastResetTime;
    private final int mPackageUid;
    final Comparator<ShortcutInfo> mShortcutRankComparator;
    final Comparator<ShortcutInfo> mShortcutTypeAndRankComparator;
    private final ArrayMap<String, ShortcutInfo> mShortcuts;

    private ShortcutPackage(ShortcutUser shortcutUser, int i, String str, ShortcutPackageInfo shortcutPackageInfo) {
        super(shortcutUser, i, str, shortcutPackageInfo == null ? ShortcutPackageInfo.newEmpty() : shortcutPackageInfo);
        this.mShortcuts = new ArrayMap<>();
        this.mShortcutTypeAndRankComparator = new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                return ShortcutPackage.lambda$new$1((ShortcutInfo) obj, (ShortcutInfo) obj2);
            }
        };
        this.mShortcutRankComparator = new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                return ShortcutPackage.lambda$new$2((ShortcutInfo) obj, (ShortcutInfo) obj2);
            }
        };
        this.mPackageUid = shortcutUser.mService.injectGetPackageUid(str, i);
    }

    public ShortcutPackage(ShortcutUser shortcutUser, int i, String str) {
        this(shortcutUser, i, str, null);
    }

    @Override
    public int getOwnerUserId() {
        return getPackageUserId();
    }

    public int getPackageUid() {
        return this.mPackageUid;
    }

    public Resources getPackageResources() {
        return this.mShortcutUser.mService.injectGetResourcesForApplicationAsUser(getPackageName(), getPackageUserId());
    }

    public int getShortcutCount() {
        return this.mShortcuts.size();
    }

    @Override
    protected boolean canRestoreAnyVersion() {
        return false;
    }

    @Override
    protected void onRestored(int i) {
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size);
            shortcutInfoValueAt.clearFlags(4096);
            shortcutInfoValueAt.setDisabledReason(i);
            if (i != 0) {
                shortcutInfoValueAt.addFlags(64);
            }
        }
        refreshPinnedFlags();
    }

    public ShortcutInfo findShortcutById(String str) {
        return this.mShortcuts.get(str);
    }

    public boolean isShortcutExistsAndInvisibleToPublisher(String str) {
        ShortcutInfo shortcutInfoFindShortcutById = findShortcutById(str);
        return (shortcutInfoFindShortcutById == null || shortcutInfoFindShortcutById.isVisibleToPublisher()) ? false : true;
    }

    public boolean isShortcutExistsAndVisibleToPublisher(String str) {
        ShortcutInfo shortcutInfoFindShortcutById = findShortcutById(str);
        return shortcutInfoFindShortcutById != null && shortcutInfoFindShortcutById.isVisibleToPublisher();
    }

    private void ensureNotImmutable(ShortcutInfo shortcutInfo, boolean z) {
        if (shortcutInfo != null && shortcutInfo.isImmutable()) {
            if (!z || shortcutInfo.isVisibleToPublisher()) {
                throw new IllegalArgumentException("Manifest shortcut ID=" + shortcutInfo.getId() + " may not be manipulated via APIs");
            }
        }
    }

    public void ensureNotImmutable(String str, boolean z) {
        ensureNotImmutable(this.mShortcuts.get(str), z);
    }

    public void ensureImmutableShortcutsNotIncludedWithIds(List<String> list, boolean z) {
        for (int size = list.size() - 1; size >= 0; size--) {
            ensureNotImmutable(list.get(size), z);
        }
    }

    public void ensureImmutableShortcutsNotIncluded(List<ShortcutInfo> list, boolean z) {
        for (int size = list.size() - 1; size >= 0; size--) {
            ensureNotImmutable(list.get(size).getId(), z);
        }
    }

    private ShortcutInfo forceDeleteShortcutInner(String str) {
        ShortcutInfo shortcutInfoRemove = this.mShortcuts.remove(str);
        if (shortcutInfoRemove != null) {
            this.mShortcutUser.mService.removeIconLocked(shortcutInfoRemove);
            shortcutInfoRemove.clearFlags(35);
        }
        return shortcutInfoRemove;
    }

    private void forceReplaceShortcutInner(ShortcutInfo shortcutInfo) {
        ShortcutService shortcutService = this.mShortcutUser.mService;
        forceDeleteShortcutInner(shortcutInfo.getId());
        shortcutService.saveIconAndFixUpShortcutLocked(shortcutInfo);
        shortcutService.fixUpShortcutResourceNamesAndValues(shortcutInfo);
        this.mShortcuts.put(shortcutInfo.getId(), shortcutInfo);
    }

    public void addOrReplaceDynamicShortcut(ShortcutInfo shortcutInfo) {
        Preconditions.checkArgument(shortcutInfo.isEnabled(), "add/setDynamicShortcuts() cannot publish disabled shortcuts");
        shortcutInfo.addFlags(1);
        ShortcutInfo shortcutInfo2 = this.mShortcuts.get(shortcutInfo.getId());
        boolean zIsPinned = false;
        if (shortcutInfo2 != null) {
            shortcutInfo2.ensureUpdatableWith(shortcutInfo, false);
            zIsPinned = shortcutInfo2.isPinned();
        }
        if (zIsPinned) {
            shortcutInfo.addFlags(2);
        }
        forceReplaceShortcutInner(shortcutInfo);
    }

    private void removeOrphans() {
        ArrayList arrayList = null;
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size);
            if (!shortcutInfoValueAt.isAlive()) {
                if (arrayList == null) {
                    arrayList = new ArrayList();
                }
                arrayList.add(shortcutInfoValueAt.getId());
            }
        }
        if (arrayList != null) {
            for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                forceDeleteShortcutInner((String) arrayList.get(size2));
            }
        }
    }

    public void deleteAllDynamicShortcuts(boolean z) {
        long jInjectCurrentTimeMillis = this.mShortcutUser.mService.injectCurrentTimeMillis();
        boolean z2 = false;
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size);
            if (shortcutInfoValueAt.isDynamic() && (!z || shortcutInfoValueAt.isVisibleToPublisher())) {
                shortcutInfoValueAt.setTimestamp(jInjectCurrentTimeMillis);
                shortcutInfoValueAt.clearFlags(1);
                shortcutInfoValueAt.setRank(0);
                z2 = true;
            }
        }
        if (z2) {
            removeOrphans();
        }
    }

    public boolean deleteDynamicWithId(String str, boolean z) {
        return deleteOrDisableWithId(str, false, false, z, 0) == null;
    }

    private boolean disableDynamicWithId(String str, boolean z, int i) {
        return deleteOrDisableWithId(str, true, false, z, i) == null;
    }

    public void disableWithId(String str, String str2, int i, boolean z, boolean z2, int i2) {
        ShortcutInfo shortcutInfoDeleteOrDisableWithId = deleteOrDisableWithId(str, true, z, z2, i2);
        if (shortcutInfoDeleteOrDisableWithId != null) {
            if (str2 != null) {
                shortcutInfoDeleteOrDisableWithId.setDisabledMessage(str2);
            } else if (i != 0) {
                shortcutInfoDeleteOrDisableWithId.setDisabledMessageResId(i);
                this.mShortcutUser.mService.fixUpShortcutResourceNamesAndValues(shortcutInfoDeleteOrDisableWithId);
            }
        }
    }

    private ShortcutInfo deleteOrDisableWithId(String str, boolean z, boolean z2, boolean z3, int i) {
        Preconditions.checkState(z == (i != 0), "disable and disabledReason disagree: " + z + " vs " + i);
        ShortcutInfo shortcutInfo = this.mShortcuts.get(str);
        if (shortcutInfo == null || (!shortcutInfo.isEnabled() && z3 && !shortcutInfo.isVisibleToPublisher())) {
            return null;
        }
        if (!z2) {
            ensureNotImmutable(shortcutInfo, true);
        }
        if (shortcutInfo.isPinned()) {
            shortcutInfo.setRank(0);
            shortcutInfo.clearFlags(33);
            if (z) {
                shortcutInfo.addFlags(64);
                if (shortcutInfo.getDisabledReason() == 0) {
                    shortcutInfo.setDisabledReason(i);
                }
            }
            shortcutInfo.setTimestamp(this.mShortcutUser.mService.injectCurrentTimeMillis());
            if (this.mShortcutUser.mService.isDummyMainActivity(shortcutInfo.getActivity())) {
                shortcutInfo.setActivity(null);
            }
            return shortcutInfo;
        }
        forceDeleteShortcutInner(str);
        return null;
    }

    public void enableWithId(String str) {
        ShortcutInfo shortcutInfo = this.mShortcuts.get(str);
        if (shortcutInfo != null) {
            ensureNotImmutable(shortcutInfo, true);
            shortcutInfo.clearFlags(64);
            shortcutInfo.setDisabledReason(0);
        }
    }

    public void updateInvisibleShortcutForPinRequestWith(ShortcutInfo shortcutInfo) {
        Preconditions.checkNotNull(this.mShortcuts.get(shortcutInfo.getId()));
        this.mShortcutUser.mService.validateShortcutForPinRequest(shortcutInfo);
        shortcutInfo.addFlags(2);
        forceReplaceShortcutInner(shortcutInfo);
        adjustRanks();
    }

    public void refreshPinnedFlags() {
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            this.mShortcuts.valueAt(size).clearFlags(2);
        }
        this.mShortcutUser.forAllLaunchers(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ShortcutPackage.lambda$refreshPinnedFlags$0(this.f$0, (ShortcutLauncher) obj);
            }
        });
        removeOrphans();
    }

    public static void lambda$refreshPinnedFlags$0(ShortcutPackage shortcutPackage, ShortcutLauncher shortcutLauncher) {
        ArraySet<String> pinnedShortcutIds = shortcutLauncher.getPinnedShortcutIds(shortcutPackage.getPackageName(), shortcutPackage.getPackageUserId());
        if (pinnedShortcutIds == null || pinnedShortcutIds.size() == 0) {
            return;
        }
        for (int size = pinnedShortcutIds.size() - 1; size >= 0; size--) {
            ShortcutInfo shortcutInfo = shortcutPackage.mShortcuts.get(pinnedShortcutIds.valueAt(size));
            if (shortcutInfo != null) {
                shortcutInfo.addFlags(2);
            }
        }
    }

    public int getApiCallCount(boolean z) {
        ShortcutService shortcutService = this.mShortcutUser.mService;
        if (shortcutService.isUidForegroundLocked(this.mPackageUid) || this.mLastKnownForegroundElapsedTime < shortcutService.getUidLastForegroundElapsedTimeLocked(this.mPackageUid) || z) {
            this.mLastKnownForegroundElapsedTime = shortcutService.injectElapsedRealtime();
            resetRateLimiting();
        }
        long lastResetTimeLocked = shortcutService.getLastResetTimeLocked();
        long jInjectCurrentTimeMillis = shortcutService.injectCurrentTimeMillis();
        if (ShortcutService.isClockValid(jInjectCurrentTimeMillis) && this.mLastResetTime > jInjectCurrentTimeMillis) {
            Slog.w(TAG, "Clock rewound");
            this.mLastResetTime = jInjectCurrentTimeMillis;
            this.mApiCallCount = 0;
            return this.mApiCallCount;
        }
        if (this.mLastResetTime < lastResetTimeLocked) {
            this.mApiCallCount = 0;
            this.mLastResetTime = lastResetTimeLocked;
        }
        return this.mApiCallCount;
    }

    public boolean tryApiCall(boolean z) {
        ShortcutService shortcutService = this.mShortcutUser.mService;
        if (getApiCallCount(z) >= shortcutService.mMaxUpdatesPerInterval) {
            return false;
        }
        this.mApiCallCount++;
        shortcutService.scheduleSaveUser(getOwnerUserId());
        return true;
    }

    public void resetRateLimiting() {
        if (this.mApiCallCount > 0) {
            this.mApiCallCount = 0;
            this.mShortcutUser.mService.scheduleSaveUser(getOwnerUserId());
        }
    }

    public void resetRateLimitingForCommandLineNoSaving() {
        this.mApiCallCount = 0;
        this.mLastResetTime = 0L;
    }

    public void findAll(List<ShortcutInfo> list, Predicate<ShortcutInfo> predicate, int i) {
        findAll(list, predicate, i, null, 0, false);
    }

    public void findAll(List<ShortcutInfo> list, Predicate<ShortcutInfo> predicate, int i, String str, int i2, boolean z) {
        if (getPackageInfo().isShadow()) {
            return;
        }
        ArraySet<String> pinnedShortcutIds = str == null ? null : this.mShortcutUser.mService.getLauncherShortcutsLocked(str, getPackageUserId(), i2).getPinnedShortcutIds(getPackageName(), getPackageUserId());
        for (int i3 = 0; i3 < this.mShortcuts.size(); i3++) {
            ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(i3);
            boolean z2 = str == null || (pinnedShortcutIds != null && pinnedShortcutIds.contains(shortcutInfoValueAt.getId()));
            if (z || !shortcutInfoValueAt.isFloating() || z2) {
                ShortcutInfo shortcutInfoClone = shortcutInfoValueAt.clone(i);
                if (!z && !z2) {
                    shortcutInfoClone.clearFlags(2);
                }
                if (predicate == null || predicate.test(shortcutInfoClone)) {
                    if (!z2) {
                        shortcutInfoClone.clearFlags(2);
                    }
                    list.add(shortcutInfoClone);
                }
            }
        }
    }

    public void resetThrottling() {
        this.mApiCallCount = 0;
    }

    public ArraySet<String> getUsedBitmapFiles() {
        ArraySet<String> arraySet = new ArraySet<>(this.mShortcuts.size());
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size);
            if (shortcutInfoValueAt.getBitmapPath() != null) {
                arraySet.add(getFileName(shortcutInfoValueAt.getBitmapPath()));
            }
        }
        return arraySet;
    }

    private static String getFileName(String str) {
        int iLastIndexOf = str.lastIndexOf(File.separatorChar);
        if (iLastIndexOf == -1) {
            return str;
        }
        return str.substring(iLastIndexOf + 1);
    }

    private boolean areAllActivitiesStillEnabled() {
        if (this.mShortcuts.size() == 0) {
            return true;
        }
        ShortcutService shortcutService = this.mShortcutUser.mService;
        ArrayList arrayList = new ArrayList(4);
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            ComponentName activity = this.mShortcuts.valueAt(size).getActivity();
            if (!arrayList.contains(activity)) {
                arrayList.add(activity);
                if (activity != null && !shortcutService.injectIsActivityEnabledAndExported(activity, getOwnerUserId())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean rescanPackageIfNeeded(boolean z, boolean z2) {
        List<ShortcutInfo> shortcuts;
        ShortcutService shortcutService = this.mShortcutUser.mService;
        long statStartTime = shortcutService.getStatStartTime();
        try {
            PackageInfo packageInfo = this.mShortcutUser.mService.getPackageInfo(getPackageName(), getPackageUserId());
            if (packageInfo == null) {
                return false;
            }
            if (!z && !z2 && getPackageInfo().getVersionCode() == packageInfo.getLongVersionCode() && getPackageInfo().getLastUpdateTime() == packageInfo.lastUpdateTime) {
                if (areAllActivitiesStillEnabled()) {
                    return false;
                }
            }
            shortcutService.logDurationStat(14, statStartTime);
            Resources packageResources = null;
            try {
                shortcuts = ShortcutParser.parseShortcuts(this.mShortcutUser.mService, getPackageName(), getPackageUserId());
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, "Failed to load shortcuts from AndroidManifest.xml.", e);
                shortcuts = null;
            }
            int size = shortcuts == null ? 0 : shortcuts.size();
            if (z && size == 0) {
                return false;
            }
            getPackageInfo().updateFromPackageInfo(packageInfo);
            long versionCode = getPackageInfo().getVersionCode();
            for (int size2 = this.mShortcuts.size() - 1; size2 >= 0; size2--) {
                ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size2);
                if (shortcutInfoValueAt.getDisabledReason() == 100 && getPackageInfo().getBackupSourceVersionCode() <= versionCode) {
                    Slog.i(TAG, String.format("Restoring shortcut: %s", shortcutInfoValueAt.getId()));
                    shortcutInfoValueAt.clearFlags(64);
                    shortcutInfoValueAt.setDisabledReason(0);
                }
            }
            if (!z) {
                for (int size3 = this.mShortcuts.size() - 1; size3 >= 0; size3--) {
                    ShortcutInfo shortcutInfoValueAt2 = this.mShortcuts.valueAt(size3);
                    if (shortcutInfoValueAt2.isDynamic()) {
                        if (shortcutInfoValueAt2.getActivity() == null) {
                            shortcutService.wtf("null activity detected.");
                        } else if (!shortcutService.injectIsMainActivity(shortcutInfoValueAt2.getActivity(), getPackageUserId())) {
                            Slog.w(TAG, String.format("%s is no longer main activity. Disabling shorcut %s.", getPackageName(), shortcutInfoValueAt2.getId()));
                            if (disableDynamicWithId(shortcutInfoValueAt2.getId(), false, 2)) {
                                continue;
                            }
                        }
                    }
                    if (shortcutInfoValueAt2.hasAnyResources()) {
                        if (!shortcutInfoValueAt2.isOriginallyFromManifest()) {
                            if (packageResources == null && (packageResources = getPackageResources()) == null) {
                                break;
                            }
                            shortcutInfoValueAt2.lookupAndFillInResourceIds(packageResources);
                        }
                        shortcutInfoValueAt2.setTimestamp(shortcutService.injectCurrentTimeMillis());
                    } else {
                        continue;
                    }
                }
            }
            publishManifestShortcuts(shortcuts);
            if (shortcuts != null) {
                pushOutExcessShortcuts();
            }
            shortcutService.verifyStates();
            shortcutService.packageShortcutsChanged(getPackageName(), getPackageUserId());
            return true;
        } finally {
            shortcutService.logDurationStat(14, statStartTime);
        }
    }

    private boolean publishManifestShortcuts(List<ShortcutInfo> list) {
        boolean z;
        ArraySet arraySet = null;
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size);
            if (shortcutInfoValueAt.isManifestShortcut()) {
                if (arraySet == null) {
                    arraySet = new ArraySet();
                }
                arraySet.add(shortcutInfoValueAt.getId());
            }
        }
        boolean z2 = false;
        if (list != null) {
            int size2 = list.size();
            int i = 0;
            boolean z3 = false;
            while (i < size2) {
                ShortcutInfo shortcutInfo = list.get(i);
                boolean z4 = !shortcutInfo.isEnabled();
                String id = shortcutInfo.getId();
                ShortcutInfo shortcutInfo2 = this.mShortcuts.get(id);
                if (shortcutInfo2 == null) {
                    z = false;
                    if (z4 || z) {
                        forceReplaceShortcutInner(shortcutInfo);
                        if (z4 && arraySet != null) {
                            arraySet.remove(id);
                        }
                    }
                } else if (!shortcutInfo2.isOriginallyFromManifest()) {
                    Slog.e(TAG, "Shortcut with ID=" + shortcutInfo.getId() + " exists but is not from AndroidManifest.xml, not updating.");
                } else {
                    if (shortcutInfo2.isPinned()) {
                        shortcutInfo.addFlags(2);
                        z = true;
                    }
                    if (z4) {
                        forceReplaceShortcutInner(shortcutInfo);
                        if (z4) {
                        }
                    }
                }
                i++;
                z3 = true;
            }
            z2 = z3;
        }
        if (arraySet != null) {
            int size3 = arraySet.size() - 1;
            while (size3 >= 0) {
                disableWithId((String) arraySet.valueAt(size3), null, 0, true, false, 2);
                size3--;
                z2 = true;
            }
            removeOrphans();
        }
        adjustRanks();
        return z2;
    }

    private boolean pushOutExcessShortcuts() {
        ShortcutService shortcutService = this.mShortcutUser.mService;
        int maxActivityShortcuts = shortcutService.getMaxActivityShortcuts();
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> arrayMapSortShortcutsToActivities = sortShortcutsToActivities();
        for (int size = arrayMapSortShortcutsToActivities.size() - 1; size >= 0; size--) {
            ArrayList<ShortcutInfo> arrayListValueAt = arrayMapSortShortcutsToActivities.valueAt(size);
            if (arrayListValueAt.size() > maxActivityShortcuts) {
                Collections.sort(arrayListValueAt, this.mShortcutTypeAndRankComparator);
                for (int size2 = arrayListValueAt.size() - 1; size2 >= maxActivityShortcuts; size2--) {
                    ShortcutInfo shortcutInfo = arrayListValueAt.get(size2);
                    if (shortcutInfo.isManifestShortcut()) {
                        shortcutService.wtf("Found manifest shortcuts in excess list.");
                    } else {
                        deleteDynamicWithId(shortcutInfo.getId(), true);
                    }
                }
            }
        }
        return false;
    }

    static int lambda$new$1(ShortcutInfo shortcutInfo, ShortcutInfo shortcutInfo2) {
        if (shortcutInfo.isManifestShortcut() && !shortcutInfo2.isManifestShortcut()) {
            return -1;
        }
        if (!shortcutInfo.isManifestShortcut() && shortcutInfo2.isManifestShortcut()) {
            return 1;
        }
        return Integer.compare(shortcutInfo.getRank(), shortcutInfo2.getRank());
    }

    private ArrayMap<ComponentName, ArrayList<ShortcutInfo>> sortShortcutsToActivities() {
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> arrayMap = new ArrayMap<>();
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size);
            if (!shortcutInfoValueAt.isFloating()) {
                ComponentName activity = shortcutInfoValueAt.getActivity();
                if (activity == null) {
                    this.mShortcutUser.mService.wtf("null activity detected.");
                } else {
                    ArrayList<ShortcutInfo> arrayList = arrayMap.get(activity);
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                        arrayMap.put(activity, arrayList);
                    }
                    arrayList.add(shortcutInfoValueAt);
                }
            }
        }
        return arrayMap;
    }

    private void incrementCountForActivity(ArrayMap<ComponentName, Integer> arrayMap, ComponentName componentName, int i) {
        Integer num = arrayMap.get(componentName);
        if (num == null) {
            num = 0;
        }
        arrayMap.put(componentName, Integer.valueOf(num.intValue() + i));
    }

    public void enforceShortcutCountsBeforeOperation(List<ShortcutInfo> list, int i) {
        ShortcutService shortcutService = this.mShortcutUser.mService;
        ArrayMap<ComponentName, Integer> arrayMap = new ArrayMap<>(4);
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size);
            if (shortcutInfoValueAt.isManifestShortcut()) {
                incrementCountForActivity(arrayMap, shortcutInfoValueAt.getActivity(), 1);
            } else if (shortcutInfoValueAt.isDynamic() && i != 0) {
                incrementCountForActivity(arrayMap, shortcutInfoValueAt.getActivity(), 1);
            }
        }
        for (int size2 = list.size() - 1; size2 >= 0; size2--) {
            ShortcutInfo shortcutInfo = list.get(size2);
            ComponentName activity = shortcutInfo.getActivity();
            if (activity == null) {
                if (i != 2) {
                    shortcutService.wtf("Activity must not be null at this point");
                }
            } else {
                ShortcutInfo shortcutInfo2 = this.mShortcuts.get(shortcutInfo.getId());
                if (shortcutInfo2 == null) {
                    if (i != 2) {
                        incrementCountForActivity(arrayMap, activity, 1);
                    }
                } else if (!shortcutInfo2.isFloating() || i != 2) {
                    if (i != 0) {
                        ComponentName activity2 = shortcutInfo2.getActivity();
                        if (!shortcutInfo2.isFloating()) {
                            incrementCountForActivity(arrayMap, activity2, -1);
                        }
                    }
                    incrementCountForActivity(arrayMap, activity, 1);
                }
            }
        }
        for (int size3 = arrayMap.size() - 1; size3 >= 0; size3--) {
            shortcutService.enforceMaxActivityShortcuts(arrayMap.valueAt(size3).intValue());
        }
    }

    public void resolveResourceStrings() {
        ShortcutService shortcutService = this.mShortcutUser.mService;
        boolean z = true;
        int size = this.mShortcuts.size() - 1;
        boolean z2 = false;
        Resources packageResources = null;
        while (true) {
            if (size >= 0) {
                ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size);
                if (shortcutInfoValueAt.hasStringResources()) {
                    if (packageResources == null && (packageResources = getPackageResources()) == null) {
                        break;
                    }
                    shortcutInfoValueAt.resolveResourceStrings(packageResources);
                    shortcutInfoValueAt.setTimestamp(shortcutService.injectCurrentTimeMillis());
                    z2 = true;
                }
                size--;
            } else {
                z = z2;
                break;
            }
        }
        if (z) {
            shortcutService.packageShortcutsChanged(getPackageName(), getPackageUserId());
        }
    }

    public void clearAllImplicitRanks() {
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            this.mShortcuts.valueAt(size).clearImplicitRankAndRankChangedFlag();
        }
    }

    static int lambda$new$2(ShortcutInfo shortcutInfo, ShortcutInfo shortcutInfo2) {
        int iCompare = Integer.compare(shortcutInfo.getRank(), shortcutInfo2.getRank());
        if (iCompare != 0) {
            return iCompare;
        }
        if (shortcutInfo.isRankChanged() != shortcutInfo2.isRankChanged()) {
            return shortcutInfo.isRankChanged() ? -1 : 1;
        }
        int iCompare2 = Integer.compare(shortcutInfo.getImplicitRank(), shortcutInfo2.getImplicitRank());
        if (iCompare2 != 0) {
            return iCompare2;
        }
        return shortcutInfo.getId().compareTo(shortcutInfo2.getId());
    }

    public void adjustRanks() {
        ShortcutService shortcutService = this.mShortcutUser.mService;
        long jInjectCurrentTimeMillis = shortcutService.injectCurrentTimeMillis();
        int size = this.mShortcuts.size();
        while (true) {
            size--;
            if (size < 0) {
                break;
            }
            ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size);
            if (shortcutInfoValueAt.isFloating() && shortcutInfoValueAt.getRank() != 0) {
                shortcutInfoValueAt.setTimestamp(jInjectCurrentTimeMillis);
                shortcutInfoValueAt.setRank(0);
            }
        }
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> arrayMapSortShortcutsToActivities = sortShortcutsToActivities();
        for (int size2 = arrayMapSortShortcutsToActivities.size() - 1; size2 >= 0; size2--) {
            ArrayList<ShortcutInfo> arrayListValueAt = arrayMapSortShortcutsToActivities.valueAt(size2);
            Collections.sort(arrayListValueAt, this.mShortcutRankComparator);
            int size3 = arrayListValueAt.size();
            int i = 0;
            for (int i2 = 0; i2 < size3; i2++) {
                ShortcutInfo shortcutInfo = arrayListValueAt.get(i2);
                if (!shortcutInfo.isManifestShortcut()) {
                    if (!shortcutInfo.isDynamic()) {
                        shortcutService.wtf("Non-dynamic shortcut found.");
                    } else {
                        int i3 = i + 1;
                        if (shortcutInfo.getRank() != i) {
                            shortcutInfo.setTimestamp(jInjectCurrentTimeMillis);
                            shortcutInfo.setRank(i);
                        }
                        i = i3;
                    }
                }
            }
        }
    }

    public boolean hasNonManifestShortcuts() {
        for (int size = this.mShortcuts.size() - 1; size >= 0; size--) {
            if (!this.mShortcuts.valueAt(size).isDeclaredInManifest()) {
                return true;
            }
        }
        return false;
    }

    public void dump(PrintWriter printWriter, String str, ShortcutService.DumpFilter dumpFilter) {
        printWriter.println();
        printWriter.print(str);
        printWriter.print("Package: ");
        printWriter.print(getPackageName());
        printWriter.print("  UID: ");
        printWriter.print(this.mPackageUid);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("  ");
        printWriter.print("Calls: ");
        printWriter.print(getApiCallCount(false));
        printWriter.println();
        printWriter.print(str);
        printWriter.print("  ");
        printWriter.print("Last known FG: ");
        printWriter.print(this.mLastKnownForegroundElapsedTime);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("  ");
        printWriter.print("Last reset: [");
        printWriter.print(this.mLastResetTime);
        printWriter.print("] ");
        printWriter.print(ShortcutService.formatTime(this.mLastResetTime));
        printWriter.println();
        getPackageInfo().dump(printWriter, str + "  ");
        printWriter.println();
        printWriter.print(str);
        printWriter.println("  Shortcuts:");
        ArrayMap<String, ShortcutInfo> arrayMap = this.mShortcuts;
        int size = arrayMap.size();
        long j = 0;
        for (int i = 0; i < size; i++) {
            ShortcutInfo shortcutInfoValueAt = arrayMap.valueAt(i);
            printWriter.println(shortcutInfoValueAt.toDumpString(str + "    "));
            if (shortcutInfoValueAt.getBitmapPath() != null) {
                long length = new File(shortcutInfoValueAt.getBitmapPath()).length();
                printWriter.print(str);
                printWriter.print("      ");
                printWriter.print("bitmap size=");
                printWriter.println(length);
                j += length;
            }
        }
        printWriter.print(str);
        printWriter.print("  ");
        printWriter.print("Total bitmap size: ");
        printWriter.print(j);
        printWriter.print(" (");
        printWriter.print(Formatter.formatFileSize(this.mShortcutUser.mService.mContext, j));
        printWriter.println(")");
    }

    @Override
    public JSONObject dumpCheckin(boolean z) throws JSONException {
        JSONObject jSONObjectDumpCheckin = super.dumpCheckin(z);
        ArrayMap<String, ShortcutInfo> arrayMap = this.mShortcuts;
        int size = arrayMap.size();
        int i = 0;
        int i2 = 0;
        long length = 0;
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 0; i5 < size; i5++) {
            ShortcutInfo shortcutInfoValueAt = arrayMap.valueAt(i5);
            if (shortcutInfoValueAt.isDynamic()) {
                i3++;
            }
            if (shortcutInfoValueAt.isDeclaredInManifest()) {
                i4++;
            }
            if (shortcutInfoValueAt.isPinned()) {
                i++;
            }
            if (shortcutInfoValueAt.getBitmapPath() != null) {
                i2++;
                length += new File(shortcutInfoValueAt.getBitmapPath()).length();
            }
        }
        jSONObjectDumpCheckin.put(KEY_DYNAMIC, i3);
        jSONObjectDumpCheckin.put(KEY_MANIFEST, i4);
        jSONObjectDumpCheckin.put(KEY_PINNED, i);
        jSONObjectDumpCheckin.put(KEY_BITMAPS, i2);
        jSONObjectDumpCheckin.put(KEY_BITMAP_BYTES, length);
        return jSONObjectDumpCheckin;
    }

    @Override
    public void saveToXml(XmlSerializer xmlSerializer, boolean z) throws XmlPullParserException, IOException {
        int size = this.mShortcuts.size();
        if (size == 0 && this.mApiCallCount == 0) {
            return;
        }
        xmlSerializer.startTag(null, "package");
        ShortcutService.writeAttr(xmlSerializer, Settings.ATTR_NAME, getPackageName());
        ShortcutService.writeAttr(xmlSerializer, ATTR_CALL_COUNT, this.mApiCallCount);
        ShortcutService.writeAttr(xmlSerializer, ATTR_LAST_RESET, this.mLastResetTime);
        getPackageInfo().saveToXml(this.mShortcutUser.mService, xmlSerializer, z);
        for (int i = 0; i < size; i++) {
            saveShortcut(xmlSerializer, this.mShortcuts.valueAt(i), z, getPackageInfo().isBackupAllowed());
        }
        xmlSerializer.endTag(null, "package");
    }

    private void saveShortcut(XmlSerializer xmlSerializer, ShortcutInfo shortcutInfo, boolean z, boolean z2) throws XmlPullParserException, IOException {
        ShortcutService shortcutService = this.mShortcutUser.mService;
        if (z && (!shortcutInfo.isPinned() || !shortcutInfo.isEnabled())) {
            return;
        }
        boolean z3 = !z || z2;
        if (shortcutInfo.isIconPendingSave()) {
            shortcutService.removeIconLocked(shortcutInfo);
        }
        xmlSerializer.startTag(null, TAG_SHORTCUT);
        ShortcutService.writeAttr(xmlSerializer, ATTR_ID, shortcutInfo.getId());
        ShortcutService.writeAttr(xmlSerializer, ATTR_ACTIVITY, shortcutInfo.getActivity());
        ShortcutService.writeAttr(xmlSerializer, ATTR_TITLE, shortcutInfo.getTitle());
        ShortcutService.writeAttr(xmlSerializer, ATTR_TITLE_RES_ID, shortcutInfo.getTitleResId());
        ShortcutService.writeAttr(xmlSerializer, ATTR_TITLE_RES_NAME, shortcutInfo.getTitleResName());
        ShortcutService.writeAttr(xmlSerializer, ATTR_TEXT, shortcutInfo.getText());
        ShortcutService.writeAttr(xmlSerializer, ATTR_TEXT_RES_ID, shortcutInfo.getTextResId());
        ShortcutService.writeAttr(xmlSerializer, ATTR_TEXT_RES_NAME, shortcutInfo.getTextResName());
        if (z3) {
            ShortcutService.writeAttr(xmlSerializer, ATTR_DISABLED_MESSAGE, shortcutInfo.getDisabledMessage());
            ShortcutService.writeAttr(xmlSerializer, ATTR_DISABLED_MESSAGE_RES_ID, shortcutInfo.getDisabledMessageResourceId());
            ShortcutService.writeAttr(xmlSerializer, ATTR_DISABLED_MESSAGE_RES_NAME, shortcutInfo.getDisabledMessageResName());
        }
        ShortcutService.writeAttr(xmlSerializer, ATTR_DISABLED_REASON, shortcutInfo.getDisabledReason());
        ShortcutService.writeAttr(xmlSerializer, "timestamp", shortcutInfo.getLastChangedTimestamp());
        if (z) {
            ShortcutService.writeAttr(xmlSerializer, ATTR_FLAGS, shortcutInfo.getFlags() & (-2062));
            if (getPackageInfo().getVersionCode() == 0) {
                shortcutService.wtf("Package version code should be available at this point.");
            }
        } else {
            ShortcutService.writeAttr(xmlSerializer, ATTR_RANK, shortcutInfo.getRank());
            ShortcutService.writeAttr(xmlSerializer, ATTR_FLAGS, shortcutInfo.getFlags());
            ShortcutService.writeAttr(xmlSerializer, ATTR_ICON_RES_ID, shortcutInfo.getIconResourceId());
            ShortcutService.writeAttr(xmlSerializer, ATTR_ICON_RES_NAME, shortcutInfo.getIconResName());
            ShortcutService.writeAttr(xmlSerializer, ATTR_BITMAP_PATH, shortcutInfo.getBitmapPath());
        }
        if (z3) {
            Set<String> categories = shortcutInfo.getCategories();
            if (categories != null && categories.size() > 0) {
                xmlSerializer.startTag(null, "categories");
                XmlUtils.writeStringArrayXml((String[]) categories.toArray(new String[categories.size()]), "categories", xmlSerializer);
                xmlSerializer.endTag(null, "categories");
            }
            Intent[] intentsNoExtras = shortcutInfo.getIntentsNoExtras();
            PersistableBundle[] intentPersistableExtrases = shortcutInfo.getIntentPersistableExtrases();
            int length = intentsNoExtras.length;
            for (int i = 0; i < length; i++) {
                xmlSerializer.startTag(null, "intent");
                ShortcutService.writeAttr(xmlSerializer, ATTR_INTENT_NO_EXTRA, intentsNoExtras[i]);
                ShortcutService.writeTagExtra(xmlSerializer, TAG_EXTRAS, intentPersistableExtrases[i]);
                xmlSerializer.endTag(null, "intent");
            }
            ShortcutService.writeTagExtra(xmlSerializer, TAG_EXTRAS, shortcutInfo.getExtras());
        }
        xmlSerializer.endTag(null, TAG_SHORTCUT);
    }

    public static ShortcutPackage loadFromXml(ShortcutService shortcutService, ShortcutUser shortcutUser, XmlPullParser xmlPullParser, boolean z) throws XmlPullParserException, IOException {
        String stringAttribute = ShortcutService.parseStringAttribute(xmlPullParser, Settings.ATTR_NAME);
        ShortcutPackage shortcutPackage = new ShortcutPackage(shortcutUser, shortcutUser.getUserId(), stringAttribute);
        shortcutPackage.mApiCallCount = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_CALL_COUNT);
        shortcutPackage.mLastResetTime = ShortcutService.parseLongAttribute(xmlPullParser, ATTR_LAST_RESET);
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            byte b = 1;
            if (next != 1 && (next != 3 || xmlPullParser.getDepth() > depth)) {
                if (next == 2) {
                    int depth2 = xmlPullParser.getDepth();
                    String name = xmlPullParser.getName();
                    if (depth2 == depth + 1) {
                        int iHashCode = name.hashCode();
                        if (iHashCode != -1923478059) {
                            if (iHashCode != -342500282 || !name.equals(TAG_SHORTCUT)) {
                                b = -1;
                            }
                            switch (b) {
                                case 0:
                                    shortcutPackage.getPackageInfo().loadFromXml(xmlPullParser, z);
                                    continue;
                                    continue;
                                case 1:
                                    ShortcutInfo shortcut = parseShortcut(xmlPullParser, stringAttribute, shortcutUser.getUserId(), z);
                                    shortcutPackage.mShortcuts.put(shortcut.getId(), shortcut);
                                    continue;
                                    continue;
                            }
                        } else {
                            if (name.equals("package-info")) {
                                b = 0;
                            }
                            switch (b) {
                            }
                        }
                    }
                    ShortcutService.warnForInvalidTag(depth2, name);
                }
            }
        }
        return shortcutPackage;
    }

    private static ShortcutInfo parseShortcut(XmlPullParser xmlPullParser, String str, int i, boolean z) throws XmlPullParserException, IOException {
        int i2;
        int i3;
        int depth;
        String name;
        ArrayList arrayList = new ArrayList();
        String stringAttribute = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_ID);
        ComponentName componentNameAttribute = ShortcutService.parseComponentNameAttribute(xmlPullParser, ATTR_ACTIVITY);
        String stringAttribute2 = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_TITLE);
        int intAttribute = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_TITLE_RES_ID);
        String stringAttribute3 = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_TITLE_RES_NAME);
        String stringAttribute4 = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_TEXT);
        int intAttribute2 = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_TEXT_RES_ID);
        String stringAttribute5 = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_TEXT_RES_NAME);
        String stringAttribute6 = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_DISABLED_MESSAGE);
        int intAttribute3 = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_DISABLED_MESSAGE_RES_ID);
        String stringAttribute7 = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_DISABLED_MESSAGE_RES_NAME);
        int intAttribute4 = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_DISABLED_REASON);
        Intent intentAttributeNoDefault = ShortcutService.parseIntentAttributeNoDefault(xmlPullParser, "intent");
        int longAttribute = (int) ShortcutService.parseLongAttribute(xmlPullParser, ATTR_RANK);
        long longAttribute2 = ShortcutService.parseLongAttribute(xmlPullParser, "timestamp");
        int longAttribute3 = (int) ShortcutService.parseLongAttribute(xmlPullParser, ATTR_FLAGS);
        int longAttribute4 = (int) ShortcutService.parseLongAttribute(xmlPullParser, ATTR_ICON_RES_ID);
        String stringAttribute8 = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_ICON_RES_NAME);
        String stringAttribute9 = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_BITMAP_PATH);
        int depth2 = xmlPullParser.getDepth();
        PersistableBundle persistableBundleRestoreFromXml = null;
        ArraySet arraySet = null;
        PersistableBundle persistableBundleRestoreFromXml2 = null;
        while (true) {
            int next = xmlPullParser.next();
            int i4 = longAttribute4;
            if (next != 1 && (next != 3 || xmlPullParser.getDepth() > depth2)) {
                if (next != 2) {
                    i2 = depth2;
                } else {
                    depth = xmlPullParser.getDepth();
                    name = xmlPullParser.getName();
                    switch (name) {
                        case "intent-extras":
                            persistableBundleRestoreFromXml = PersistableBundle.restoreFromXml(xmlPullParser);
                            longAttribute4 = i4;
                            depth2 = i2;
                            break;
                        case "intent":
                            i3 = intAttribute2;
                            arrayList.add(parseIntent(xmlPullParser));
                            longAttribute4 = i4;
                            depth2 = i2;
                            intAttribute2 = i3;
                            break;
                        case "extras":
                            persistableBundleRestoreFromXml2 = PersistableBundle.restoreFromXml(xmlPullParser);
                            longAttribute4 = i4;
                            depth2 = i2;
                            break;
                        case "categories":
                            i3 = intAttribute2;
                            longAttribute4 = i4;
                            depth2 = i2;
                            intAttribute2 = i3;
                            break;
                        case "string-array":
                            if ("categories".equals(ShortcutService.parseStringAttribute(xmlPullParser, Settings.ATTR_NAME))) {
                                String[] thisStringArrayXml = XmlUtils.readThisStringArrayXml(xmlPullParser, TAG_STRING_ARRAY_XMLUTILS, (String[]) null);
                                ArraySet arraySet2 = new ArraySet(thisStringArrayXml.length);
                                int i5 = 0;
                                while (true) {
                                    i3 = intAttribute2;
                                    if (i5 < thisStringArrayXml.length) {
                                        arraySet2.add(thisStringArrayXml[i5]);
                                        i5++;
                                        intAttribute2 = i3;
                                    } else {
                                        arraySet = arraySet2;
                                    }
                                }
                            }
                            longAttribute4 = i4;
                            depth2 = i2;
                            intAttribute2 = i3;
                            break;
                        default:
                            throw ShortcutService.throwForInvalidTag(depth, name);
                    }
                }
                i3 = intAttribute2;
                longAttribute4 = i4;
                depth2 = i2;
                intAttribute2 = i3;
            }
        }
    }

    private static Intent parseIntent(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        Intent intentAttribute = ShortcutService.parseIntentAttribute(xmlPullParser, ATTR_INTENT_NO_EXTRA);
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next == 2) {
                int depth2 = xmlPullParser.getDepth();
                String name = xmlPullParser.getName();
                byte b = -1;
                if (name.hashCode() == -1289032093 && name.equals(TAG_EXTRAS)) {
                    b = 0;
                }
                if (b == 0) {
                    ShortcutInfo.setIntentExtras(intentAttribute, PersistableBundle.restoreFromXml(xmlPullParser));
                } else {
                    throw ShortcutService.throwForInvalidTag(depth2, name);
                }
            }
        }
    }

    @VisibleForTesting
    List<ShortcutInfo> getAllShortcutsForTest() {
        return new ArrayList(this.mShortcuts.values());
    }

    @Override
    public void verifyStates() {
        super.verifyStates();
        ShortcutService shortcutService = this.mShortcutUser.mService;
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> arrayMapSortShortcutsToActivities = sortShortcutsToActivities();
        boolean z = false;
        for (int size = arrayMapSortShortcutsToActivities.size() - 1; size >= 0; size--) {
            ArrayList<ShortcutInfo> arrayListValueAt = arrayMapSortShortcutsToActivities.valueAt(size);
            if (arrayListValueAt.size() > this.mShortcutUser.mService.getMaxActivityShortcuts()) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": activity " + arrayMapSortShortcutsToActivities.keyAt(size) + " has " + arrayMapSortShortcutsToActivities.valueAt(size).size() + " shortcuts.");
                z = true;
            }
            Collections.sort(arrayListValueAt, new Comparator() {
                @Override
                public final int compare(Object obj, Object obj2) {
                    return Integer.compare(((ShortcutInfo) obj).getRank(), ((ShortcutInfo) obj2).getRank());
                }
            });
            ArrayList arrayList = new ArrayList(arrayListValueAt);
            arrayList.removeIf(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ShortcutPackage.lambda$verifyStates$4((ShortcutInfo) obj);
                }
            });
            List<ShortcutInfo> arrayList2 = new ArrayList<>(arrayListValueAt);
            arrayList.removeIf(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ShortcutPackage.lambda$verifyStates$5((ShortcutInfo) obj);
                }
            });
            verifyRanksSequential(arrayList);
            verifyRanksSequential(arrayList2);
        }
        for (int size2 = this.mShortcuts.size() - 1; size2 >= 0; size2--) {
            ShortcutInfo shortcutInfoValueAt = this.mShortcuts.valueAt(size2);
            if (!shortcutInfoValueAt.isDeclaredInManifest() && !shortcutInfoValueAt.isDynamic() && !shortcutInfoValueAt.isPinned()) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " is not manifest, dynamic or pinned.");
                z = true;
            }
            if (shortcutInfoValueAt.isDeclaredInManifest() && shortcutInfoValueAt.isDynamic()) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " is both dynamic and manifest at the same time.");
                z = true;
            }
            if (shortcutInfoValueAt.getActivity() == null && !shortcutInfoValueAt.isFloating()) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " has null activity, but not floating.");
                z = true;
            }
            if ((shortcutInfoValueAt.isDynamic() || shortcutInfoValueAt.isManifestShortcut()) && !shortcutInfoValueAt.isEnabled()) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " is not floating, but is disabled.");
                z = true;
            }
            if (shortcutInfoValueAt.isFloating() && shortcutInfoValueAt.getRank() != 0) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " is floating, but has rank=" + shortcutInfoValueAt.getRank());
                z = true;
            }
            if (shortcutInfoValueAt.getIcon() != null) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " still has an icon");
                z = true;
            }
            if (shortcutInfoValueAt.hasAdaptiveBitmap() && !shortcutInfoValueAt.hasIconFile()) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " has adaptive bitmap but was not saved to a file.");
                z = true;
            }
            if (shortcutInfoValueAt.hasIconFile() && shortcutInfoValueAt.hasIconResource()) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " has both resource and bitmap icons");
                z = true;
            }
            if (shortcutInfoValueAt.isEnabled() != (shortcutInfoValueAt.getDisabledReason() == 0)) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " isEnabled() and getDisabledReason() disagree: " + shortcutInfoValueAt.isEnabled() + " vs " + shortcutInfoValueAt.getDisabledReason());
                z = true;
            }
            if (shortcutInfoValueAt.getDisabledReason() == 100 && getPackageInfo().getBackupSourceVersionCode() == -1) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " RESTORED_VERSION_LOWER with no backup source version code.");
                z = true;
            }
            if (shortcutService.isDummyMainActivity(shortcutInfoValueAt.getActivity())) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfoValueAt.getId() + " has a dummy target activity");
                z = true;
            }
        }
        if (z) {
            throw new IllegalStateException("See logcat for errors");
        }
    }

    static boolean lambda$verifyStates$4(ShortcutInfo shortcutInfo) {
        return !shortcutInfo.isDynamic();
    }

    static boolean lambda$verifyStates$5(ShortcutInfo shortcutInfo) {
        return !shortcutInfo.isManifestShortcut();
    }

    private boolean verifyRanksSequential(List<ShortcutInfo> list) {
        boolean z = false;
        for (int i = 0; i < list.size(); i++) {
            ShortcutInfo shortcutInfo = list.get(i);
            if (shortcutInfo.getRank() != i) {
                Log.e(TAG_VERIFY, "Package " + getPackageName() + ": shortcut " + shortcutInfo.getId() + " rank=" + shortcutInfo.getRank() + " but expected to be " + i);
                z = true;
            }
        }
        return z;
    }
}

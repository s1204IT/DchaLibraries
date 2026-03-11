package com.android.launcher3.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.MainThreadExecutor;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@TargetApi(21)
public class ManagedProfileHeuristic {
    private final Context mContext;
    private ArrayList<ShortcutInfo> mHomescreenApps;
    private final LauncherModel mModel = LauncherAppState.getInstance().getModel();
    private final String mPackageSetKey;
    private final SharedPreferences mPrefs;
    private HashMap<ShortcutInfo, Long> mShortcutToInstallTimeMap;
    private final UserHandleCompat mUser;
    private final long mUserCreationTime;
    private final long mUserSerial;
    private ArrayList<ShortcutInfo> mWorkFolderApps;

    public static ManagedProfileHeuristic get(Context context, UserHandleCompat user) {
        if (Utilities.ATLEAST_LOLLIPOP && !UserHandleCompat.myUserHandle().equals(user)) {
            return new ManagedProfileHeuristic(context, user);
        }
        return null;
    }

    private ManagedProfileHeuristic(Context context, UserHandleCompat user) {
        this.mContext = context;
        this.mUser = user;
        UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        this.mUserSerial = userManager.getSerialNumberForUser(user);
        this.mUserCreationTime = userManager.getUserCreationTime(user);
        this.mPackageSetKey = "installed_packages_for_user_" + this.mUserSerial;
        this.mPrefs = this.mContext.getSharedPreferences("com.android.launcher3.managedusers.prefs", 0);
    }

    private void initVars() {
        this.mHomescreenApps = new ArrayList<>();
        this.mWorkFolderApps = new ArrayList<>();
        this.mShortcutToInstallTimeMap = new HashMap<>();
    }

    public void processUserApps(List<LauncherActivityInfoCompat> apps) {
        initVars();
        HashSet<String> packageSet = new HashSet<>();
        boolean userAppsExisted = getUserApps(packageSet);
        boolean newPackageAdded = false;
        for (LauncherActivityInfoCompat info : apps) {
            String packageName = info.getComponentName().getPackageName();
            if (!packageSet.contains(packageName)) {
                packageSet.add(packageName);
                newPackageAdded = true;
                markForAddition(info, info.getFirstInstallTime());
            }
        }
        if (!newPackageAdded) {
            return;
        }
        this.mPrefs.edit().putStringSet(this.mPackageSetKey, packageSet).apply();
        finalizeAdditions(userAppsExisted);
    }

    private void markForAddition(LauncherActivityInfoCompat info, long installTime) {
        ArrayList<ShortcutInfo> targetList = installTime <= this.mUserCreationTime + 28800000 ? this.mWorkFolderApps : this.mHomescreenApps;
        ShortcutInfo si = ShortcutInfo.fromActivityInfo(info, this.mContext);
        this.mShortcutToInstallTimeMap.put(si, Long.valueOf(installTime));
        targetList.add(si);
    }

    private void sortList(ArrayList<ShortcutInfo> infos) {
        Collections.sort(infos, new Comparator<ShortcutInfo>() {
            @Override
            public int compare(ShortcutInfo lhs, ShortcutInfo rhs) {
                Long lhsTime = (Long) ManagedProfileHeuristic.this.mShortcutToInstallTimeMap.get(lhs);
                Long rhsTime = (Long) ManagedProfileHeuristic.this.mShortcutToInstallTimeMap.get(rhs);
                return Utilities.longCompare(lhsTime == null ? 0L : lhsTime.longValue(), rhsTime != null ? rhsTime.longValue() : 0L);
            }
        });
    }

    private void finalizeWorkFolder() {
        if (this.mWorkFolderApps.isEmpty()) {
            return;
        }
        sortList(this.mWorkFolderApps);
        String folderIdKey = "user_folder_" + this.mUserSerial;
        if (this.mPrefs.contains(folderIdKey)) {
            long folderId = this.mPrefs.getLong(folderIdKey, 0L);
            final FolderInfo workFolder = this.mModel.findFolderById(Long.valueOf(folderId));
            if (workFolder == null || !workFolder.hasOption(2)) {
                this.mHomescreenApps.addAll(this.mWorkFolderApps);
                return;
            }
            saveWorkFolderShortcuts(folderId, workFolder.contents.size());
            final ArrayList<ShortcutInfo> shortcuts = this.mWorkFolderApps;
            new MainThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    for (ShortcutInfo info : shortcuts) {
                        workFolder.add(info);
                    }
                }
            });
            return;
        }
        FolderInfo workFolder2 = new FolderInfo();
        workFolder2.title = this.mContext.getText(R.string.work_folder_name);
        workFolder2.setOption(2, true, null);
        for (ShortcutInfo info : this.mWorkFolderApps) {
            workFolder2.add(info);
        }
        ArrayList<ItemInfo> itemList = new ArrayList<>(1);
        itemList.add(workFolder2);
        this.mModel.addAndBindAddedWorkspaceItems(this.mContext, itemList);
        this.mPrefs.edit().putLong("user_folder_" + this.mUserSerial, workFolder2.id).apply();
        saveWorkFolderShortcuts(workFolder2.id, 0);
    }

    private void saveWorkFolderShortcuts(long workFolderId, int startingRank) {
        for (ItemInfo info : this.mWorkFolderApps) {
            info.rank = startingRank;
            LauncherModel.addItemToDatabase(this.mContext, info, workFolderId, 0L, 0, 0);
            startingRank++;
        }
    }

    private void finalizeAdditions(boolean addHomeScreenShortcuts) {
        finalizeWorkFolder();
        if (!addHomeScreenShortcuts || this.mHomescreenApps.isEmpty()) {
            return;
        }
        sortList(this.mHomescreenApps);
        this.mModel.addAndBindAddedWorkspaceItems(this.mContext, this.mHomescreenApps);
    }

    public void processPackageAdd(String[] packages) {
        initVars();
        HashSet<String> packageSet = new HashSet<>();
        boolean userAppsExisted = getUserApps(packageSet);
        boolean newPackageAdded = false;
        long installTime = System.currentTimeMillis();
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this.mContext);
        for (String packageName : packages) {
            if (!packageSet.contains(packageName)) {
                packageSet.add(packageName);
                newPackageAdded = true;
                List<LauncherActivityInfoCompat> activities = launcherApps.getActivityList(packageName, this.mUser);
                if (!activities.isEmpty()) {
                    markForAddition(activities.get(0), installTime);
                }
            }
        }
        if (!newPackageAdded) {
            return;
        }
        this.mPrefs.edit().putStringSet(this.mPackageSetKey, packageSet).apply();
        finalizeAdditions(userAppsExisted);
    }

    public void processPackageRemoved(String[] packages) {
        HashSet<String> packageSet = new HashSet<>();
        getUserApps(packageSet);
        boolean packageRemoved = false;
        for (String packageName : packages) {
            if (packageSet.remove(packageName)) {
                packageRemoved = true;
            }
        }
        if (!packageRemoved) {
            return;
        }
        this.mPrefs.edit().putStringSet(this.mPackageSetKey, packageSet).apply();
    }

    private boolean getUserApps(HashSet<String> outExistingApps) {
        Set<String> userApps = this.mPrefs.getStringSet(this.mPackageSetKey, null);
        if (userApps == null) {
            return false;
        }
        outExistingApps.addAll(userApps);
        return true;
    }

    public static void processAllUsers(List<UserHandleCompat> users, Context context) {
        if (!Utilities.ATLEAST_LOLLIPOP) {
            return;
        }
        UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        HashSet<String> validKeys = new HashSet<>();
        for (UserHandleCompat user : users) {
            addAllUserKeys(userManager.getSerialNumberForUser(user), validKeys);
        }
        SharedPreferences prefs = context.getSharedPreferences("com.android.launcher3.managedusers.prefs", 0);
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (!validKeys.contains(key)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    private static void addAllUserKeys(long userSerial, HashSet<String> keysOut) {
        keysOut.add("installed_packages_for_user_" + userSerial);
        keysOut.add("user_folder_" + userSerial);
    }

    public static void markExistingUsersForNoFolderCreation(Context context) {
        UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        UserHandleCompat myUser = UserHandleCompat.myUserHandle();
        SharedPreferences prefs = null;
        for (UserHandleCompat user : userManager.getUserProfiles()) {
            if (!myUser.equals(user)) {
                if (prefs == null) {
                    prefs = context.getSharedPreferences("com.android.launcher3.managedusers.prefs", 0);
                }
                String folderIdKey = "user_folder_" + userManager.getSerialNumberForUser(user);
                if (!prefs.contains(folderIdKey)) {
                    prefs.edit().putLong(folderIdKey, -1L).apply();
                }
            }
        }
    }
}

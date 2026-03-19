package com.android.packageinstaller.permission.model;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.ArraySet;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.PermissionApps;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class PermissionGroups implements LoaderManager.LoaderCallbacks<List<PermissionGroup>> {
    private final PermissionsGroupsChangeCallback mCallback;
    private final Context mContext;
    private final ArrayList<PermissionGroup> mGroups = new ArrayList<>();
    private final LoaderManager mLoaderManager;

    public interface PermissionsGroupsChangeCallback {
        void onPermissionGroupsChanged();
    }

    public PermissionGroups(Context context, LoaderManager loaderManager, PermissionsGroupsChangeCallback permissionsGroupsChangeCallback) {
        this.mContext = context;
        this.mLoaderManager = loaderManager;
        this.mCallback = permissionsGroupsChangeCallback;
        this.mLoaderManager.initLoader(0, null, this);
    }

    @Override
    public Loader<List<PermissionGroup>> onCreateLoader(int i, Bundle bundle) {
        return new PermissionsLoader(this.mContext);
    }

    @Override
    public void onLoadFinished(Loader<List<PermissionGroup>> loader, List<PermissionGroup> list) {
        if (this.mGroups.equals(list)) {
            return;
        }
        this.mGroups.clear();
        this.mGroups.addAll(list);
        this.mCallback.onPermissionGroupsChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<PermissionGroup>> loader) {
        this.mGroups.clear();
        this.mCallback.onPermissionGroupsChanged();
    }

    public List<PermissionGroup> getGroups() {
        return this.mGroups;
    }

    public PermissionGroup getGroup(String str) {
        for (PermissionGroup permissionGroup : this.mGroups) {
            if (permissionGroup.getName().equals(str)) {
                return permissionGroup;
            }
        }
        return null;
    }

    private static final class PermissionsLoader extends AsyncTaskLoader<List<PermissionGroup>> implements PackageManager.OnPermissionsChangedListener {
        public PermissionsLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            getContext().getPackageManager().addOnPermissionsChangeListener(this);
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            getContext().getPackageManager().removeOnPermissionsChangeListener(this);
        }

        @Override
        public List<PermissionGroup> loadInBackground() {
            ArraySet<String> launcherPackages = Utils.getLauncherPackages(getContext());
            PermissionApps.PmCache pmCache = new PermissionApps.PmCache(getContext().getPackageManager());
            ArrayList arrayList = new ArrayList();
            ArraySet arraySet = new ArraySet();
            PackageManager packageManager = getContext().getPackageManager();
            int i = 0;
            Iterator<PermissionGroupInfo> it = packageManager.getAllPermissionGroups(0).iterator();
            while (true) {
                int i2 = 1073741824;
                if (it.hasNext()) {
                    PermissionGroupInfo next = it.next();
                    if (isLoadInBackgroundCanceled()) {
                        return Collections.emptyList();
                    }
                    try {
                        boolean z = false;
                        for (PermissionInfo permissionInfo : packageManager.queryPermissionsByGroup(next.name, 0)) {
                            arraySet.add(permissionInfo.name);
                            if ((permissionInfo.protectionLevel & 15) == 1 && (permissionInfo.flags & 1073741824) != 0 && (permissionInfo.flags & 2) == 0) {
                                z = true;
                            }
                        }
                        if (z) {
                            CharSequence charSequenceLoadItemInfoLabel = loadItemInfoLabel(next);
                            Drawable drawableLoadItemInfoIcon = loadItemInfoIcon(next);
                            PermissionApps permissionApps = new PermissionApps(getContext(), next.name, null, pmCache);
                            permissionApps.refreshSync();
                            arrayList.add(new PermissionGroup(next.name, next.packageName, charSequenceLoadItemInfoLabel, drawableLoadItemInfoIcon, permissionApps.getTotalCount(launcherPackages), permissionApps.getGrantedCount(launcherPackages)));
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                } else {
                    List<PackageInfo> installedPackages = getContext().getPackageManager().getInstalledPackages(4096);
                    ArraySet arraySet2 = new ArraySet();
                    for (PackageInfo packageInfo : installedPackages) {
                        if (packageInfo.requestedPermissions != null) {
                            for (String str : packageInfo.requestedPermissions) {
                                arraySet2.add(str);
                            }
                        }
                    }
                    for (PackageInfo packageInfo2 : installedPackages) {
                        if (packageInfo2.permissions != null) {
                            PermissionInfo[] permissionInfoArr = packageInfo2.permissions;
                            int length = permissionInfoArr.length;
                            int i3 = i;
                            while (i3 < length) {
                                PermissionInfo permissionInfo2 = permissionInfoArr[i3];
                                if (arraySet.add(permissionInfo2.name) && (permissionInfo2.protectionLevel & 15) == 1 && (permissionInfo2.flags & i2) != 0 && arraySet2.contains(permissionInfo2.name)) {
                                    CharSequence charSequenceLoadItemInfoLabel2 = loadItemInfoLabel(permissionInfo2);
                                    Drawable drawableLoadItemInfoIcon2 = loadItemInfoIcon(permissionInfo2);
                                    PermissionApps permissionApps2 = new PermissionApps(getContext(), permissionInfo2.name, null, pmCache);
                                    permissionApps2.refreshSync();
                                    arrayList.add(new PermissionGroup(permissionInfo2.name, permissionInfo2.packageName, charSequenceLoadItemInfoLabel2, drawableLoadItemInfoIcon2, permissionApps2.getTotalCount(launcherPackages), permissionApps2.getGrantedCount(launcherPackages)));
                                }
                                i3++;
                                i2 = 1073741824;
                            }
                            i = 0;
                            i2 = 1073741824;
                        }
                    }
                    Collections.sort(arrayList);
                    return arrayList;
                }
            }
        }

        private CharSequence loadItemInfoLabel(PackageItemInfo packageItemInfo) {
            CharSequence charSequenceLoadLabel = packageItemInfo.loadLabel(getContext().getPackageManager());
            if (charSequenceLoadLabel == null) {
                return packageItemInfo.name;
            }
            return charSequenceLoadLabel;
        }

        private Drawable loadItemInfoIcon(PackageItemInfo packageItemInfo) {
            Drawable drawableLoadDrawable;
            if (packageItemInfo.icon > 0) {
                drawableLoadDrawable = Utils.loadDrawable(getContext().getPackageManager(), packageItemInfo.packageName, packageItemInfo.icon);
            } else {
                drawableLoadDrawable = null;
            }
            if (drawableLoadDrawable == null) {
                return getContext().getDrawable(R.drawable.ic_perm_device_info);
            }
            return drawableLoadDrawable;
        }

        public void onPermissionsChanged(int i) {
            forceLoad();
        }
    }
}

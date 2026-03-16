package com.android.bluetooth.map;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class BluetoothMapEmailSettingsLoader {
    private static final boolean D = true;
    private static final long PROVIDER_ANR_TIMEOUT = 20000;
    private static final String TAG = "BluetoothMapEmailSettingsLoader";
    private static final boolean V = false;
    private Context mContext;
    private ContentResolver mResolver;
    private PackageManager mPackageManager = null;
    private int mAccountsEnabledCount = 0;
    private ContentProviderClient mProviderClient = null;

    public BluetoothMapEmailSettingsLoader(Context ctx) {
        this.mContext = null;
        this.mContext = ctx;
    }

    public LinkedHashMap<BluetoothMapEmailSettingsItem, ArrayList<BluetoothMapEmailSettingsItem>> parsePackages(boolean includeIcon) {
        LinkedHashMap<BluetoothMapEmailSettingsItem, ArrayList<BluetoothMapEmailSettingsItem>> groups = new LinkedHashMap<>();
        Intent searchIntent = new Intent("android.bluetooth.action.BLUETOOTH_MAP_PROVIDER");
        this.mAccountsEnabledCount = 0;
        this.mPackageManager = this.mContext.getPackageManager();
        List<ResolveInfo> resInfos = this.mPackageManager.queryIntentContentProviders(searchIntent, 0);
        if (resInfos != null) {
            Log.d(TAG, "Found " + resInfos.size() + " applications");
            for (ResolveInfo rInfo : resInfos) {
                if ((rInfo.providerInfo.applicationInfo.flags & 2097152) == 0) {
                    BluetoothMapEmailSettingsItem app = createAppItem(rInfo, includeIcon);
                    if (app != null) {
                        ArrayList<BluetoothMapEmailSettingsItem> accounts = parseAccounts(app);
                        if (accounts.size() > 0) {
                            app.mIsChecked = true;
                            Iterator<BluetoothMapEmailSettingsItem> it = accounts.iterator();
                            while (true) {
                                if (!it.hasNext()) {
                                    break;
                                }
                                BluetoothMapEmailSettingsItem acc = it.next();
                                if (!acc.mIsChecked) {
                                    app.mIsChecked = false;
                                    break;
                                }
                            }
                            groups.put(app, accounts);
                        }
                    }
                } else {
                    Log.d(TAG, "Ignoring force-stopped authority " + rInfo.providerInfo.authority + "\n");
                }
            }
        } else {
            Log.d(TAG, "Found no applications");
        }
        return groups;
    }

    public BluetoothMapEmailSettingsItem createAppItem(ResolveInfo rInfo, boolean includeIcon) {
        String provider = rInfo.providerInfo.authority;
        if (provider == null) {
            return null;
        }
        String name = rInfo.loadLabel(this.mPackageManager).toString();
        Log.d(TAG, rInfo.providerInfo.packageName + " - " + name + " - meta-data(provider = " + provider + ")\n");
        return new BluetoothMapEmailSettingsItem("0", name, rInfo.providerInfo.packageName, provider, includeIcon ? rInfo.loadIcon(this.mPackageManager) : null);
    }

    public ArrayList<BluetoothMapEmailSettingsItem> parseAccounts(BluetoothMapEmailSettingsItem app) {
        Cursor c = null;
        Log.d(TAG, "Adding app " + app.getPackageName());
        ArrayList<BluetoothMapEmailSettingsItem> children = new ArrayList<>();
        this.mResolver = this.mContext.getContentResolver();
        try {
            try {
                this.mProviderClient = this.mResolver.acquireUnstableContentProviderClient(Uri.parse(app.mBase_uri_no_account));
            } catch (RemoteException e) {
                Log.d(TAG, "Could not establish ContentProviderClient for " + app.getPackageName() + " - returning empty account list");
                if (0 != 0) {
                    c.close();
                }
            }
            if (this.mProviderClient == null) {
                throw new RemoteException("Failed to acquire provider for " + app.getPackageName());
            }
            this.mProviderClient.setDetectNotResponding(PROVIDER_ANR_TIMEOUT);
            Uri uri = Uri.parse(app.mBase_uri_no_account + "/Account");
            Cursor c2 = this.mProviderClient.query(uri, BluetoothMapContract.BT_ACCOUNT_PROJECTION, null, null, "_id DESC");
            while (c2 != null && c2.moveToNext()) {
                BluetoothMapEmailSettingsItem child = new BluetoothMapEmailSettingsItem(String.valueOf(c2.getInt(c2.getColumnIndex("_id"))), c2.getString(c2.getColumnIndex("account_display_name")), app.getPackageName(), app.getProviderAuthority(), null);
                child.mIsChecked = c2.getInt(c2.getColumnIndex("flag_expose")) != 0;
                if (child.mIsChecked) {
                    this.mAccountsEnabledCount++;
                }
                children.add(child);
            }
            if (c2 != null) {
                c2.close();
            }
            return children;
        } catch (Throwable th) {
            if (0 != 0) {
                c.close();
            }
            throw th;
        }
    }

    public int getAccountsEnabledCount() {
        Log.d(TAG, "Enabled Accounts count:" + this.mAccountsEnabledCount);
        return this.mAccountsEnabledCount;
    }
}

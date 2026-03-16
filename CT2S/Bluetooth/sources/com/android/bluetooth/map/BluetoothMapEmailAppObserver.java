package com.android.bluetooth.map;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class BluetoothMapEmailAppObserver {
    private static final boolean D = true;
    private static final String TAG = "BluetoothMapEmailAppObserver";
    private static final boolean V = false;
    private Context mContext;
    private LinkedHashMap<BluetoothMapEmailSettingsItem, ArrayList<BluetoothMapEmailSettingsItem>> mFullList;
    BluetoothMapEmailSettingsLoader mLoader;
    BluetoothMapService mMapService;
    private LinkedHashMap<String, ContentObserver> mObserverMap = new LinkedHashMap<>();
    private PackageManager mPackageManager = null;
    private BroadcastReceiver mReceiver;
    private ContentResolver mResolver;

    public BluetoothMapEmailAppObserver(Context context, BluetoothMapService mapService) {
        this.mMapService = null;
        this.mContext = context;
        this.mMapService = mapService;
        this.mResolver = context.getContentResolver();
        this.mLoader = new BluetoothMapEmailSettingsLoader(this.mContext);
        this.mFullList = this.mLoader.parsePackages(false);
        createReceiver();
        initObservers();
    }

    private BluetoothMapEmailSettingsItem getApp(String packageName) {
        for (BluetoothMapEmailSettingsItem app : this.mFullList.keySet()) {
            if (app.getPackageName().equals(packageName)) {
                return app;
            }
        }
        return null;
    }

    private void handleAccountChanges(String packageNameWithProvider) {
        Log.d(TAG, "handleAccountChanges (packageNameWithProvider: " + packageNameWithProvider + "\n");
        String packageName = packageNameWithProvider.replaceFirst("\\.[^\\.]+$", "");
        BluetoothMapEmailSettingsItem app = getApp(packageName);
        if (app != null) {
            ArrayList<BluetoothMapEmailSettingsItem> newAccountList = this.mLoader.parseAccounts(app);
            ArrayList<BluetoothMapEmailSettingsItem> oldAccountList = this.mFullList.get(app);
            ArrayList<BluetoothMapEmailSettingsItem> addedAccountList = (ArrayList) newAccountList.clone();
            ArrayList<BluetoothMapEmailSettingsItem> removedAccountList = this.mFullList.get(app);
            this.mFullList.put(app, newAccountList);
            for (BluetoothMapEmailSettingsItem newAcc : newAccountList) {
                Iterator<BluetoothMapEmailSettingsItem> it = oldAccountList.iterator();
                while (true) {
                    if (it.hasNext()) {
                        BluetoothMapEmailSettingsItem oldAcc = it.next();
                        if (newAcc.getId() == oldAcc.getId()) {
                            removedAccountList.remove(oldAcc);
                            addedAccountList.remove(newAcc);
                            if (!newAcc.getName().equals(oldAcc.getName()) && newAcc.mIsChecked) {
                                this.mMapService.updateMasInstances(2);
                            }
                            if (newAcc.mIsChecked != oldAcc.mIsChecked) {
                                if (newAcc.mIsChecked) {
                                    this.mMapService.updateMasInstances(0);
                                } else {
                                    this.mMapService.updateMasInstances(1);
                                }
                            }
                        }
                    }
                }
            }
            for (BluetoothMapEmailSettingsItem bluetoothMapEmailSettingsItem : removedAccountList) {
                this.mMapService.updateMasInstances(1);
            }
            for (BluetoothMapEmailSettingsItem bluetoothMapEmailSettingsItem2 : addedAccountList) {
                this.mMapService.updateMasInstances(0);
            }
            return;
        }
        Log.e(TAG, "Received change notification on package not registered for notifications!");
    }

    public void registerObserver(BluetoothMapEmailSettingsItem app) {
        Uri uri = BluetoothMapContract.buildAccountUri(app.getProviderAuthority());
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri2) {
                if (uri2 != null) {
                    BluetoothMapEmailAppObserver.this.handleAccountChanges(uri2.getHost());
                } else {
                    Log.e(BluetoothMapEmailAppObserver.TAG, "Unable to handle change as the URI is NULL!");
                }
            }
        };
        this.mObserverMap.put(uri.toString(), observer);
        this.mResolver.registerContentObserver(uri, true, observer);
    }

    public void unregisterObserver(BluetoothMapEmailSettingsItem app) {
        Uri uri = BluetoothMapContract.buildAccountUri(app.getProviderAuthority());
        this.mResolver.unregisterContentObserver(this.mObserverMap.get(uri.toString()));
        this.mObserverMap.remove(uri.toString());
    }

    private void initObservers() {
        Log.d(TAG, "initObservers()");
        for (BluetoothMapEmailSettingsItem app : this.mFullList.keySet()) {
            registerObserver(app);
        }
    }

    private void deinitObservers() {
        Log.d(TAG, "deinitObservers()");
        for (BluetoothMapEmailSettingsItem app : this.mFullList.keySet()) {
            unregisterObserver(app);
        }
    }

    private void createReceiver() {
        Log.d(TAG, "createReceiver()\n");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(BluetoothMapEmailAppObserver.TAG, "onReceive\n");
                String action = intent.getAction();
                if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    Uri data = intent.getData();
                    Log.d(BluetoothMapEmailAppObserver.TAG, "The installed package is: " + data.getEncodedSchemeSpecificPart());
                    ResolveInfo rInfo = BluetoothMapEmailAppObserver.this.mPackageManager.resolveActivity(intent, 0);
                    BluetoothMapEmailSettingsItem app = BluetoothMapEmailAppObserver.this.mLoader.createAppItem(rInfo, false);
                    if (app != null) {
                        BluetoothMapEmailAppObserver.this.registerObserver(app);
                        ArrayList<BluetoothMapEmailSettingsItem> newAccountList = BluetoothMapEmailAppObserver.this.mLoader.parseAccounts(app);
                        BluetoothMapEmailAppObserver.this.mFullList.put(app, newAccountList);
                        return;
                    }
                    return;
                }
                if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                    Uri data2 = intent.getData();
                    String packageName = data2.getEncodedSchemeSpecificPart();
                    Log.d(BluetoothMapEmailAppObserver.TAG, "The removed package is: " + packageName);
                    BluetoothMapEmailSettingsItem app2 = BluetoothMapEmailAppObserver.this.getApp(packageName);
                    if (app2 != null) {
                        BluetoothMapEmailAppObserver.this.unregisterObserver(app2);
                        BluetoothMapEmailAppObserver.this.mFullList.remove(app2);
                    }
                }
            }
        };
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.PACKAGE_ADDED"));
    }

    private void removeReceiver() {
        Log.d(TAG, "removeReceiver()\n");
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private PackageInfo getPackageInfo(String packageName) {
        this.mPackageManager = this.mContext.getPackageManager();
        try {
            return this.mPackageManager.getPackageInfo(packageName, BluetoothMapContentObserver.MESSAGE_TYPE_RETRIEVE_CONF);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting package metadata", e);
            return null;
        }
    }

    public ArrayList<BluetoothMapEmailSettingsItem> getEnabledAccountItems() {
        Log.d(TAG, "getEnabledAccountItems()\n");
        ArrayList<BluetoothMapEmailSettingsItem> list = new ArrayList<>();
        for (BluetoothMapEmailSettingsItem app : this.mFullList.keySet()) {
            ArrayList<BluetoothMapEmailSettingsItem> accountList = this.mFullList.get(app);
            for (BluetoothMapEmailSettingsItem acc : accountList) {
                if (acc.mIsChecked) {
                    list.add(acc);
                }
            }
        }
        return list;
    }

    public ArrayList<BluetoothMapEmailSettingsItem> getAllAccountItems() {
        Log.d(TAG, "getAllAccountItems()\n");
        ArrayList<BluetoothMapEmailSettingsItem> list = new ArrayList<>();
        for (BluetoothMapEmailSettingsItem app : this.mFullList.keySet()) {
            ArrayList<BluetoothMapEmailSettingsItem> accountList = this.mFullList.get(app);
            list.addAll(accountList);
        }
        return list;
    }

    public void shutdown() {
        deinitObservers();
        removeReceiver();
    }
}

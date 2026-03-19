package com.android.bluetooth.map;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.util.Log;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class BluetoothMapAppObserver {
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final String TAG = "BluetoothMapAppObserver";
    private static final boolean V = false;
    private Context mContext;
    private LinkedHashMap<BluetoothMapAccountItem, ArrayList<BluetoothMapAccountItem>> mFullList;
    BluetoothMapAccountLoader mLoader;
    BluetoothMapService mMapService;
    private BroadcastReceiver mReceiver;
    private ContentResolver mResolver;
    private LinkedHashMap<String, ContentObserver> mObserverMap = new LinkedHashMap<>();
    private PackageManager mPackageManager = null;
    private boolean mRegisteredReceiver = false;

    public BluetoothMapAppObserver(Context context, BluetoothMapService bluetoothMapService) {
        this.mMapService = null;
        this.mContext = context;
        this.mMapService = bluetoothMapService;
        this.mResolver = context.getContentResolver();
        this.mLoader = new BluetoothMapAccountLoader(this.mContext);
        this.mFullList = this.mLoader.parsePackages(false);
        createReceiver();
        initObservers();
    }

    private BluetoothMapAccountItem getApp(String str) {
        for (BluetoothMapAccountItem bluetoothMapAccountItem : this.mFullList.keySet()) {
            if (bluetoothMapAccountItem.getProviderAuthority().equals(str)) {
                return bluetoothMapAccountItem;
            }
        }
        return null;
    }

    private void handleAccountChanges(String str) {
        if (D) {
            Log.d(TAG, "handleAccountChanges (packageNameWithProvider: " + str + "\n");
        }
        BluetoothMapAccountItem app = getApp(str);
        if (app != null) {
            ArrayList<BluetoothMapAccountItem> accounts = this.mLoader.parseAccounts(app);
            ArrayList<BluetoothMapAccountItem> arrayList = this.mFullList.get(app);
            ArrayList<BluetoothMapAccountItem> arrayList2 = (ArrayList) accounts.clone();
            ArrayList<BluetoothMapAccountItem> arrayList3 = this.mFullList.get(app);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
            }
            if (arrayList3 == null) {
                arrayList3 = new ArrayList<>();
            }
            this.mFullList.put(app, accounts);
            for (BluetoothMapAccountItem bluetoothMapAccountItem : accounts) {
                Iterator<BluetoothMapAccountItem> it = arrayList.iterator();
                while (true) {
                    if (it.hasNext()) {
                        BluetoothMapAccountItem next = it.next();
                        if (Objects.equals(bluetoothMapAccountItem.getId(), next.getId())) {
                            arrayList3.remove(next);
                            arrayList2.remove(bluetoothMapAccountItem);
                            if (!bluetoothMapAccountItem.getName().equals(next.getName()) && bluetoothMapAccountItem.mIsChecked) {
                                this.mMapService.updateMasInstances(2);
                            }
                            if (bluetoothMapAccountItem.mIsChecked != next.mIsChecked) {
                                if (bluetoothMapAccountItem.mIsChecked) {
                                    this.mMapService.updateMasInstances(0);
                                } else {
                                    this.mMapService.updateMasInstances(1);
                                }
                            }
                        }
                    }
                }
            }
            for (BluetoothMapAccountItem bluetoothMapAccountItem2 : arrayList3) {
                this.mMapService.updateMasInstances(1);
            }
            for (BluetoothMapAccountItem bluetoothMapAccountItem3 : arrayList2) {
                this.mMapService.updateMasInstances(0);
            }
            return;
        }
        Log.e(TAG, "Received change notification on package not registered for notifications!");
    }

    public void registerObserver(BluetoothMapAccountItem bluetoothMapAccountItem) {
        Uri uriBuildAccountUri = BluetoothMapContract.buildAccountUri(bluetoothMapAccountItem.getProviderAuthority());
        ContentObserver contentObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean z) {
                onChange(z, null);
            }

            @Override
            public void onChange(boolean z, Uri uri) {
                if (uri != null) {
                    BluetoothMapAppObserver.this.handleAccountChanges(uri.getHost());
                } else {
                    Log.e(BluetoothMapAppObserver.TAG, "Unable to handle change as the URI is NULL!");
                }
            }
        };
        this.mObserverMap.put(uriBuildAccountUri.toString(), contentObserver);
        this.mResolver.registerContentObserver(uriBuildAccountUri, false, contentObserver);
    }

    public void unregisterObserver(BluetoothMapAccountItem bluetoothMapAccountItem) {
        Uri uriBuildAccountUri = BluetoothMapContract.buildAccountUri(bluetoothMapAccountItem.getProviderAuthority());
        this.mResolver.unregisterContentObserver(this.mObserverMap.get(uriBuildAccountUri.toString()));
        this.mObserverMap.remove(uriBuildAccountUri.toString());
    }

    private void initObservers() {
        if (D) {
            Log.d(TAG, "initObservers()");
        }
        Iterator<BluetoothMapAccountItem> it = this.mFullList.keySet().iterator();
        while (it.hasNext()) {
            registerObserver(it.next());
        }
    }

    private void deinitObservers() {
        if (D) {
            Log.d(TAG, "deinitObservers()");
        }
        Iterator<BluetoothMapAccountItem> it = this.mFullList.keySet().iterator();
        while (it.hasNext()) {
            unregisterObserver(it.next());
        }
    }

    private void createReceiver() {
        if (D) {
            Log.d(TAG, "createReceiver()\n");
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothMapUtils.TYPE type;
                if (BluetoothMapAppObserver.D) {
                    Log.d(BluetoothMapAppObserver.TAG, "onReceive\n");
                }
                String action = intent.getAction();
                if (!"android.intent.action.PACKAGE_ADDED".equals(action)) {
                    if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                        String encodedSchemeSpecificPart = intent.getData().getEncodedSchemeSpecificPart();
                        if (BluetoothMapAppObserver.D) {
                            Log.d(BluetoothMapAppObserver.TAG, "The removed package is: " + encodedSchemeSpecificPart);
                        }
                        BluetoothMapAccountItem app = BluetoothMapAppObserver.this.getApp(encodedSchemeSpecificPart);
                        if (app != null) {
                            BluetoothMapAppObserver.this.unregisterObserver(app);
                            BluetoothMapAppObserver.this.mFullList.remove(app);
                            return;
                        }
                        return;
                    }
                    return;
                }
                String encodedSchemeSpecificPart2 = intent.getData().getEncodedSchemeSpecificPart();
                if (BluetoothMapAppObserver.D) {
                    Log.d(BluetoothMapAppObserver.TAG, "The installed package is: " + encodedSchemeSpecificPart2);
                }
                BluetoothMapUtils.TYPE type2 = BluetoothMapUtils.TYPE.NONE;
                ResolveInfo resolveInfo = null;
                Intent[] intentArr = {new Intent(BluetoothMapContract.PROVIDER_INTERFACE_EMAIL), new Intent(BluetoothMapContract.PROVIDER_INTERFACE_IM)};
                BluetoothMapAppObserver.this.mPackageManager = BluetoothMapAppObserver.this.mContext.getPackageManager();
                BluetoothMapUtils.TYPE type3 = type2;
                for (Intent intent2 : intentArr) {
                    List<ResolveInfo> listQueryIntentContentProviders = BluetoothMapAppObserver.this.mPackageManager.queryIntentContentProviders(intent2, 0);
                    if (listQueryIntentContentProviders != null) {
                        if (BluetoothMapAppObserver.D) {
                            Log.d(BluetoothMapAppObserver.TAG, "Found " + listQueryIntentContentProviders.size() + " application(s) with intent " + intent2.getAction());
                        }
                        Iterator<ResolveInfo> it = listQueryIntentContentProviders.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            ResolveInfo next = it.next();
                            if (next != null && encodedSchemeSpecificPart2.equals(((PackageItemInfo) next.providerInfo).packageName)) {
                                if (Objects.equals(intent2.getAction(), BluetoothMapContract.PROVIDER_INTERFACE_EMAIL)) {
                                    type = BluetoothMapUtils.TYPE.EMAIL;
                                } else {
                                    if (Objects.equals(intent2.getAction(), BluetoothMapContract.PROVIDER_INTERFACE_IM)) {
                                        type = BluetoothMapUtils.TYPE.IM;
                                    }
                                    resolveInfo = next;
                                }
                                type3 = type;
                                resolveInfo = next;
                            }
                        }
                    }
                }
                if (resolveInfo != null) {
                    if (BluetoothMapAppObserver.D) {
                        Log.d(BluetoothMapAppObserver.TAG, "Found " + ((PackageItemInfo) resolveInfo.providerInfo).packageName + " application of type " + type3);
                    }
                    BluetoothMapAccountItem bluetoothMapAccountItemCreateAppItem = BluetoothMapAppObserver.this.mLoader.createAppItem(resolveInfo, false, type3);
                    if (bluetoothMapAccountItemCreateAppItem != null) {
                        BluetoothMapAppObserver.this.registerObserver(bluetoothMapAccountItemCreateAppItem);
                        BluetoothMapAppObserver.this.mFullList.put(bluetoothMapAccountItemCreateAppItem, BluetoothMapAppObserver.this.mLoader.parseAccounts(bluetoothMapAccountItemCreateAppItem));
                    }
                }
            }
        };
        if (!this.mRegisteredReceiver) {
            try {
                this.mContext.registerReceiver(this.mReceiver, intentFilter);
                this.mRegisteredReceiver = true;
            } catch (Exception e) {
                Log.e(TAG, "Unable to register MapAppObserver receiver", e);
            }
        }
    }

    private void removeReceiver() {
        if (D) {
            Log.d(TAG, "removeReceiver()\n");
        }
        if (this.mRegisteredReceiver) {
            try {
                this.mRegisteredReceiver = false;
                this.mContext.unregisterReceiver(this.mReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Unable to unregister mapAppObserver receiver", e);
            }
        }
    }

    public ArrayList<BluetoothMapAccountItem> getEnabledAccountItems() {
        if (D) {
            Log.d(TAG, "getEnabledAccountItems()\n");
        }
        ArrayList<BluetoothMapAccountItem> arrayList = new ArrayList<>();
        for (BluetoothMapAccountItem bluetoothMapAccountItem : this.mFullList.keySet()) {
            if (bluetoothMapAccountItem != null) {
                ArrayList<BluetoothMapAccountItem> arrayList2 = this.mFullList.get(bluetoothMapAccountItem);
                if (arrayList2 != null) {
                    for (BluetoothMapAccountItem bluetoothMapAccountItem2 : arrayList2) {
                        if (bluetoothMapAccountItem2.mIsChecked) {
                            arrayList.add(bluetoothMapAccountItem2);
                        }
                    }
                } else {
                    Log.w(TAG, "getEnabledAccountItems() - No AccountList enabled\n");
                }
            } else {
                Log.w(TAG, "getEnabledAccountItems() - No Account in App enabled\n");
            }
        }
        return arrayList;
    }

    public ArrayList<BluetoothMapAccountItem> getAllAccountItems() {
        if (D) {
            Log.d(TAG, "getAllAccountItems()\n");
        }
        ArrayList<BluetoothMapAccountItem> arrayList = new ArrayList<>();
        Iterator<BluetoothMapAccountItem> it = this.mFullList.keySet().iterator();
        while (it.hasNext()) {
            arrayList.addAll(this.mFullList.get(it.next()));
        }
        return arrayList;
    }

    public void shutdown() {
        deinitObservers();
        removeReceiver();
    }
}

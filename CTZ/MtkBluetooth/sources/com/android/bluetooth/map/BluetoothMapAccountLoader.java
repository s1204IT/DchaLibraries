package com.android.bluetooth.map;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class BluetoothMapAccountLoader {
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final long PROVIDER_ANR_TIMEOUT = 20000;
    private static final String TAG = "BluetoothMapAccountLoader";
    private static final boolean V = false;
    private Context mContext;
    private ContentResolver mResolver;
    private PackageManager mPackageManager = null;
    private int mAccountsEnabledCount = 0;
    private ContentProviderClient mProviderClient = null;

    public BluetoothMapAccountLoader(Context context) {
        this.mContext = null;
        this.mContext = context;
    }

    public LinkedHashMap<BluetoothMapAccountItem, ArrayList<BluetoothMapAccountItem>> parsePackages(boolean z) {
        LinkedHashMap<BluetoothMapAccountItem, ArrayList<BluetoothMapAccountItem>> linkedHashMap = new LinkedHashMap<>();
        Intent[] intentArr = {new Intent(BluetoothMapContract.PROVIDER_INTERFACE_EMAIL), new Intent(BluetoothMapContract.PROVIDER_INTERFACE_IM)};
        this.mAccountsEnabledCount = 0;
        this.mPackageManager = this.mContext.getPackageManager();
        for (Intent intent : intentArr) {
            List<ResolveInfo> listQueryIntentContentProviders = this.mPackageManager.queryIntentContentProviders(intent, 0);
            if (listQueryIntentContentProviders != null) {
                if (D) {
                    Log.d(TAG, "Found " + listQueryIntentContentProviders.size() + " application(s) with intent " + intent.getAction());
                }
                BluetoothMapUtils.TYPE type = Objects.equals(intent.getAction(), BluetoothMapContract.PROVIDER_INTERFACE_EMAIL) ? BluetoothMapUtils.TYPE.EMAIL : BluetoothMapUtils.TYPE.IM;
                for (ResolveInfo resolveInfo : listQueryIntentContentProviders) {
                    if (D) {
                        Log.d(TAG, "ResolveInfo " + resolveInfo.toString());
                    }
                    if ((((ComponentInfo) resolveInfo.providerInfo).applicationInfo.flags & 2097152) == 0) {
                        BluetoothMapAccountItem bluetoothMapAccountItemCreateAppItem = createAppItem(resolveInfo, z, type);
                        if (bluetoothMapAccountItemCreateAppItem != null) {
                            ArrayList<BluetoothMapAccountItem> accounts = parseAccounts(bluetoothMapAccountItemCreateAppItem);
                            if (accounts.size() > 0) {
                                bluetoothMapAccountItemCreateAppItem.mIsChecked = true;
                                Iterator<BluetoothMapAccountItem> it = accounts.iterator();
                                while (true) {
                                    if (!it.hasNext()) {
                                        break;
                                    }
                                    if (!it.next().mIsChecked) {
                                        bluetoothMapAccountItemCreateAppItem.mIsChecked = false;
                                        break;
                                    }
                                }
                                linkedHashMap.put(bluetoothMapAccountItemCreateAppItem, accounts);
                            }
                        }
                    } else if (D) {
                        Log.d(TAG, "Ignoring force-stopped authority " + resolveInfo.providerInfo.authority + "\n");
                    }
                }
            } else if (D) {
                Log.d(TAG, "Found no applications");
            }
        }
        return linkedHashMap;
    }

    public BluetoothMapAccountItem createAppItem(ResolveInfo resolveInfo, boolean z, BluetoothMapUtils.TYPE type) {
        Drawable drawableLoadIcon;
        String str = resolveInfo.providerInfo.authority;
        if (str == null) {
            return null;
        }
        String string = resolveInfo.loadLabel(this.mPackageManager).toString();
        if (D) {
            Log.d(TAG, ((PackageItemInfo) resolveInfo.providerInfo).packageName + " - " + string + " - meta-data(provider = " + str + ")\n");
        }
        String str2 = ((PackageItemInfo) resolveInfo.providerInfo).packageName;
        if (!z) {
            drawableLoadIcon = null;
        } else {
            drawableLoadIcon = resolveInfo.loadIcon(this.mPackageManager);
        }
        return BluetoothMapAccountItem.create("0", string, str2, str, drawableLoadIcon, type);
    }

    public ArrayList<BluetoothMapAccountItem> parseAccounts(BluetoothMapAccountItem bluetoothMapAccountItem) {
        String str;
        String str2;
        if (D) {
            Log.d(TAG, "Finding accounts for app " + bluetoothMapAccountItem.getPackageName());
        }
        ArrayList<BluetoothMapAccountItem> arrayList = new ArrayList<>();
        this.mResolver = this.mContext.getContentResolver();
        try {
            try {
                this.mProviderClient = this.mResolver.acquireUnstableContentProviderClient(Uri.parse(bluetoothMapAccountItem.mBase_uri_no_account));
                if (this.mProviderClient == null) {
                    throw new RemoteException("Failed to acquire provider for " + bluetoothMapAccountItem.getPackageName());
                }
                this.mProviderClient.setDetectNotResponding(PROVIDER_ANR_TIMEOUT);
                Uri uri = Uri.parse(bluetoothMapAccountItem.mBase_uri_no_account + "/" + BluetoothMapContract.TABLE_ACCOUNT);
                Cursor cursorQuery = bluetoothMapAccountItem.getType() == BluetoothMapUtils.TYPE.IM ? this.mProviderClient.query(uri, BluetoothMapContract.BT_IM_ACCOUNT_PROJECTION, null, null, "_id DESC") : this.mProviderClient.query(uri, BluetoothMapContract.BT_ACCOUNT_PROJECTION, null, null, "_id DESC");
                if (this.mProviderClient != null) {
                    this.mProviderClient.release();
                }
                if (cursorQuery != null) {
                    cursorQuery.moveToPosition(-1);
                    int columnIndex = cursorQuery.getColumnIndex("_id");
                    int columnIndex2 = cursorQuery.getColumnIndex(BluetoothMapContract.AccountColumns.ACCOUNT_DISPLAY_NAME);
                    int columnIndex3 = cursorQuery.getColumnIndex(BluetoothMapContract.AccountColumns.FLAG_EXPOSE);
                    int columnIndex4 = cursorQuery.getColumnIndex(BluetoothMapContract.AccountColumns.ACCOUNT_UCI);
                    int columnIndex5 = cursorQuery.getColumnIndex(BluetoothMapContract.AccountColumns.ACCOUNT_UCI_PREFIX);
                    while (cursorQuery.moveToNext()) {
                        if (D) {
                            Log.d(TAG, "Adding account " + cursorQuery.getString(columnIndex2) + " with ID " + String.valueOf(cursorQuery.getInt(columnIndex)));
                        }
                        if (bluetoothMapAccountItem.getType() == BluetoothMapUtils.TYPE.IM) {
                            String string = cursorQuery.getString(columnIndex4);
                            String string2 = cursorQuery.getString(columnIndex5);
                            if (D) {
                                Log.d(TAG, "   Account UCI " + string);
                            }
                            str2 = string2;
                            str = string;
                        } else {
                            str = null;
                            str2 = null;
                        }
                        BluetoothMapAccountItem bluetoothMapAccountItemCreate = BluetoothMapAccountItem.create(String.valueOf(cursorQuery.getInt(columnIndex)), cursorQuery.getString(columnIndex2), bluetoothMapAccountItem.getPackageName(), bluetoothMapAccountItem.getProviderAuthority(), null, bluetoothMapAccountItem.getType(), str, str2);
                        bluetoothMapAccountItemCreate.mIsChecked = cursorQuery.getInt(columnIndex3) != 0;
                        bluetoothMapAccountItemCreate.mIsChecked = true;
                        if (bluetoothMapAccountItemCreate.mIsChecked) {
                            this.mAccountsEnabledCount++;
                        }
                        arrayList.add(bluetoothMapAccountItemCreate);
                    }
                    cursorQuery.close();
                } else if (D) {
                    Log.d(TAG, "query failed");
                }
                return arrayList;
            } catch (RemoteException e) {
                if (D) {
                    Log.d(TAG, "Could not establish ContentProviderClient for " + bluetoothMapAccountItem.getPackageName() + " - returning empty account list");
                }
                if (this.mProviderClient != null) {
                    this.mProviderClient.release();
                }
                return arrayList;
            }
        } catch (Throwable th) {
            if (this.mProviderClient != null) {
                this.mProviderClient.release();
            }
            throw th;
        }
    }

    public int getAccountsEnabledCount() {
        if (D) {
            Log.d(TAG, "Enabled Accounts count:" + this.mAccountsEnabledCount);
        }
        return this.mAccountsEnabledCount;
    }
}

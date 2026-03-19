package com.android.bluetooth.map;

import android.graphics.drawable.Drawable;
import com.android.bluetooth.map.BluetoothMapUtils;

public class BluetoothMapAccountItem implements Comparable<BluetoothMapAccountItem> {
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final String TAG = "BluetoothMapAccountItem";
    private static final boolean V = false;
    public final String mBase_uri;
    public final String mBase_uri_no_account;
    private final Drawable mIcon;
    private final String mId;
    protected boolean mIsChecked;
    private final String mName;
    private final String mPackageName;
    private final String mProviderAuthority;
    private final BluetoothMapUtils.TYPE mType;
    private final String mUci;
    private final String mUciPrefix;

    public BluetoothMapAccountItem(String str, String str2, String str3, String str4, Drawable drawable, BluetoothMapUtils.TYPE type, String str5, String str6) {
        this.mName = str2;
        this.mIcon = drawable;
        this.mPackageName = str3;
        this.mId = str;
        this.mProviderAuthority = str4;
        this.mType = type;
        this.mBase_uri_no_account = "content://" + str4;
        this.mBase_uri = this.mBase_uri_no_account + "/" + str;
        this.mUci = str5;
        this.mUciPrefix = str6;
    }

    public static BluetoothMapAccountItem create(String str, String str2, String str3, String str4, Drawable drawable, BluetoothMapUtils.TYPE type) {
        return new BluetoothMapAccountItem(str, str2, str3, str4, drawable, type, null, null);
    }

    public static BluetoothMapAccountItem create(String str, String str2, String str3, String str4, Drawable drawable, BluetoothMapUtils.TYPE type, String str5, String str6) {
        return new BluetoothMapAccountItem(str, str2, str3, str4, drawable, type, str5, str6);
    }

    public long getAccountId() {
        if (this.mId != null) {
            return Long.parseLong(this.mId);
        }
        return -1L;
    }

    public String getUci() {
        return this.mUci;
    }

    public String getUciPrefix() {
        return this.mUciPrefix;
    }

    public String getUciFull() {
        if (this.mUci == null || this.mUciPrefix == null) {
            return null;
        }
        return this.mUciPrefix + ":" + this.mUci;
    }

    @Override
    public int compareTo(BluetoothMapAccountItem bluetoothMapAccountItem) {
        return (bluetoothMapAccountItem.mId.equals(this.mId) && bluetoothMapAccountItem.mName.equals(this.mName) && bluetoothMapAccountItem.mPackageName.equals(this.mPackageName) && bluetoothMapAccountItem.mProviderAuthority.equals(this.mProviderAuthority) && bluetoothMapAccountItem.mIsChecked == this.mIsChecked && bluetoothMapAccountItem.mType.equals(this.mType)) ? 0 : -1;
    }

    public int hashCode() {
        return (31 * ((((((this.mId == null ? 0 : this.mId.hashCode()) + 31) * 31) + (this.mName == null ? 0 : this.mName.hashCode())) * 31) + (this.mPackageName == null ? 0 : this.mPackageName.hashCode()))) + (this.mProviderAuthority != null ? this.mProviderAuthority.hashCode() : 0);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BluetoothMapAccountItem bluetoothMapAccountItem = (BluetoothMapAccountItem) obj;
        if (this.mId == null) {
            if (bluetoothMapAccountItem.mId != null) {
                return false;
            }
        } else if (!this.mId.equals(bluetoothMapAccountItem.mId)) {
            return false;
        }
        if (this.mName == null) {
            if (bluetoothMapAccountItem.mName != null) {
                return false;
            }
        } else if (!this.mName.equals(bluetoothMapAccountItem.mName)) {
            return false;
        }
        if (this.mPackageName == null) {
            if (bluetoothMapAccountItem.mPackageName != null) {
                return false;
            }
        } else if (!this.mPackageName.equals(bluetoothMapAccountItem.mPackageName)) {
            return false;
        }
        if (this.mProviderAuthority == null) {
            if (bluetoothMapAccountItem.mProviderAuthority != null) {
                return false;
            }
        } else if (!this.mProviderAuthority.equals(bluetoothMapAccountItem.mProviderAuthority)) {
            return false;
        }
        if (this.mType == null) {
            if (bluetoothMapAccountItem.mType != null) {
                return false;
            }
        } else if (!this.mType.equals(bluetoothMapAccountItem.mType)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return this.mName + " (" + this.mBase_uri + ")";
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    public String getName() {
        return this.mName;
    }

    public String getId() {
        return this.mId;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getProviderAuthority() {
        return this.mProviderAuthority;
    }

    public BluetoothMapUtils.TYPE getType() {
        return this.mType;
    }
}

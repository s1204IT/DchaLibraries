package com.android.managedprovisioning;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.Base64;
import java.util.Locale;

public class ProvisioningParams implements Parcelable {
    public static final Parcelable.Creator<ProvisioningParams> CREATOR = new Parcelable.Creator<ProvisioningParams>() {
        @Override
        public ProvisioningParams createFromParcel(Parcel in) {
            ProvisioningParams params = new ProvisioningParams();
            params.mTimeZone = in.readString();
            params.mLocalTime = in.readLong();
            params.mLocale = (Locale) in.readSerializable();
            params.mWifiSsid = in.readString();
            params.mWifiHidden = in.readInt() == 1;
            params.mWifiSecurityType = in.readString();
            params.mWifiPassword = in.readString();
            params.mWifiProxyHost = in.readString();
            params.mWifiProxyPort = in.readInt();
            params.mWifiProxyBypassHosts = in.readString();
            params.mDeviceAdminPackageName = in.readString();
            params.mDeviceAdminPackageDownloadLocation = in.readString();
            params.mDeviceAdminPackageDownloadCookieHeader = in.readString();
            int checksumLength = in.readInt();
            params.mDeviceAdminPackageChecksum = new byte[checksumLength];
            in.readByteArray(params.mDeviceAdminPackageChecksum);
            params.mAdminExtrasBundle = (PersistableBundle) in.readParcelable(null);
            params.mStartedByNfc = in.readInt() == 1;
            params.mLeaveAllSystemAppsEnabled = in.readInt() == 1;
            return params;
        }

        @Override
        public ProvisioningParams[] newArray(int size) {
            return new ProvisioningParams[size];
        }
    };
    public PersistableBundle mAdminExtrasBundle;
    public String mDeviceAdminPackageDownloadCookieHeader;
    public String mDeviceAdminPackageDownloadLocation;
    public String mDeviceAdminPackageName;
    public boolean mLeaveAllSystemAppsEnabled;
    public Locale mLocale;
    public boolean mStartedByNfc;
    public String mTimeZone;
    public String mWifiPacUrl;
    public String mWifiPassword;
    public String mWifiProxyBypassHosts;
    public String mWifiProxyHost;
    public String mWifiSecurityType;
    public String mWifiSsid;
    public long mLocalTime = -1;
    public boolean mWifiHidden = false;
    public int mWifiProxyPort = 0;
    public byte[] mDeviceAdminPackageChecksum = new byte[0];

    public String getLocaleAsString() {
        if (this.mLocale != null) {
            return this.mLocale.getLanguage() + "_" + this.mLocale.getCountry();
        }
        return null;
    }

    public String getDeviceAdminPackageChecksumAsString() {
        return Base64.encodeToString(this.mDeviceAdminPackageChecksum, 11);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.mTimeZone);
        out.writeLong(this.mLocalTime);
        out.writeSerializable(this.mLocale);
        out.writeString(this.mWifiSsid);
        out.writeInt(this.mWifiHidden ? 1 : 0);
        out.writeString(this.mWifiSecurityType);
        out.writeString(this.mWifiPassword);
        out.writeString(this.mWifiProxyHost);
        out.writeInt(this.mWifiProxyPort);
        out.writeString(this.mWifiProxyBypassHosts);
        out.writeString(this.mDeviceAdminPackageName);
        out.writeString(this.mDeviceAdminPackageDownloadLocation);
        out.writeString(this.mDeviceAdminPackageDownloadCookieHeader);
        out.writeInt(this.mDeviceAdminPackageChecksum.length);
        out.writeByteArray(this.mDeviceAdminPackageChecksum);
        out.writeParcelable(this.mAdminExtrasBundle, 0);
        out.writeInt(this.mStartedByNfc ? 1 : 0);
        out.writeInt(this.mLeaveAllSystemAppsEnabled ? 1 : 0);
    }
}

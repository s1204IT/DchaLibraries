package com.android.managedprovisioning.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.TextUtils;
import com.android.internal.annotations.Immutable;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.PersistableBundlable;
import com.android.managedprovisioning.common.StoreUtils;

@Immutable
public final class PackageDownloadInfo extends PersistableBundlable {
    public final String cookieHeader;
    public final String location;
    public final int minVersion;
    public final byte[] packageChecksum;
    public final boolean packageChecksumSupportsSha1;
    public final byte[] signatureChecksum;
    public static final byte[] DEFAULT_PACKAGE_CHECKSUM = new byte[0];
    public static final byte[] DEFAULT_SIGNATURE_CHECKSUM = new byte[0];
    public static final Parcelable.Creator<PackageDownloadInfo> CREATOR = new Parcelable.Creator<PackageDownloadInfo>() {
        @Override
        public PackageDownloadInfo createFromParcel(Parcel parcel) {
            return new PackageDownloadInfo(parcel);
        }

        @Override
        public PackageDownloadInfo[] newArray(int i) {
            return new PackageDownloadInfo[i];
        }
    };

    private PackageDownloadInfo(Builder builder) {
        this.location = builder.mLocation;
        this.cookieHeader = builder.mCookieHeader;
        this.packageChecksum = (byte[]) Preconditions.checkNotNull(builder.mPackageChecksum, "package checksum can't be null");
        this.signatureChecksum = (byte[]) Preconditions.checkNotNull(builder.mSignatureChecksum, "signature checksum can't be null");
        this.minVersion = builder.mMinVersion;
        this.packageChecksumSupportsSha1 = builder.mPackageChecksumSupportsSha1;
        validateFields();
    }

    private PackageDownloadInfo(Parcel parcel) {
        this(createBuilderFromPersistableBundle(PersistableBundlable.getPersistableBundleFromParcel(parcel)));
    }

    private void validateFields() {
        if (TextUtils.isEmpty(this.location)) {
            throw new IllegalArgumentException("Download location must not be empty.");
        }
        if (this.packageChecksum.length == 0 && this.signatureChecksum.length == 0) {
            throw new IllegalArgumentException("Package checksum or signature checksum must be provided.");
        }
    }

    static PackageDownloadInfo fromPersistableBundle(PersistableBundle persistableBundle) {
        return createBuilderFromPersistableBundle(persistableBundle).build();
    }

    private static Builder createBuilderFromPersistableBundle(PersistableBundle persistableBundle) {
        Builder builder = new Builder();
        builder.setMinVersion(persistableBundle.getInt("android.app.extra.PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE"));
        builder.setLocation(persistableBundle.getString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"));
        builder.setCookieHeader(persistableBundle.getString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER"));
        builder.setPackageChecksum(StoreUtils.stringToByteArray(persistableBundle.getString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM")));
        builder.setSignatureChecksum(StoreUtils.stringToByteArray(persistableBundle.getString("android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM")));
        builder.setPackageChecksumSupportsSha1(persistableBundle.getBoolean("supports-sha1-checksum"));
        return builder;
    }

    @Override
    public PersistableBundle toPersistableBundle() {
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putInt("android.app.extra.PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE", this.minVersion);
        persistableBundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION", this.location);
        persistableBundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER", this.cookieHeader);
        persistableBundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM", StoreUtils.byteArrayToString(this.packageChecksum));
        persistableBundle.putString("android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM", StoreUtils.byteArrayToString(this.signatureChecksum));
        persistableBundle.putBoolean("supports-sha1-checksum", this.packageChecksumSupportsSha1);
        return persistableBundle;
    }

    public static final class Builder {
        private String mCookieHeader;
        private String mLocation;
        private byte[] mPackageChecksum = PackageDownloadInfo.DEFAULT_PACKAGE_CHECKSUM;
        private byte[] mSignatureChecksum = PackageDownloadInfo.DEFAULT_SIGNATURE_CHECKSUM;
        private int mMinVersion = Integer.MAX_VALUE;
        private boolean mPackageChecksumSupportsSha1 = false;

        public Builder setLocation(String str) {
            this.mLocation = str;
            return this;
        }

        public Builder setCookieHeader(String str) {
            this.mCookieHeader = str;
            return this;
        }

        public Builder setPackageChecksum(byte[] bArr) {
            this.mPackageChecksum = bArr;
            return this;
        }

        public Builder setSignatureChecksum(byte[] bArr) {
            this.mSignatureChecksum = bArr;
            return this;
        }

        public Builder setMinVersion(int i) {
            this.mMinVersion = i;
            return this;
        }

        public Builder setPackageChecksumSupportsSha1(boolean z) {
            this.mPackageChecksumSupportsSha1 = z;
            return this;
        }

        public PackageDownloadInfo build() {
            return new PackageDownloadInfo(this);
        }

        public static Builder builder() {
            return new Builder();
        }
    }
}

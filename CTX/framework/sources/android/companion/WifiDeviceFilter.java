package android.companion;

import android.annotation.SuppressLint;
import android.net.wifi.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.OneTimeUseBuilder;
import java.util.Objects;
import java.util.regex.Pattern;

public final class WifiDeviceFilter implements DeviceFilter<ScanResult> {
    public static final Parcelable.Creator<WifiDeviceFilter> CREATOR = new Parcelable.Creator<WifiDeviceFilter>() {
        @Override
        public WifiDeviceFilter createFromParcel(Parcel parcel) {
            return new WifiDeviceFilter(parcel);
        }

        @Override
        public WifiDeviceFilter[] newArray(int i) {
            return new WifiDeviceFilter[i];
        }
    };
    private final Pattern mNamePattern;

    private WifiDeviceFilter(Pattern pattern) {
        this.mNamePattern = pattern;
    }

    @SuppressLint({"ParcelClassLoader"})
    private WifiDeviceFilter(Parcel parcel) {
        this(BluetoothDeviceFilterUtils.patternFromString(parcel.readString()));
    }

    public Pattern getNamePattern() {
        return this.mNamePattern;
    }

    @Override
    public boolean matches(ScanResult scanResult) {
        return BluetoothDeviceFilterUtils.matchesName(getNamePattern(), scanResult);
    }

    @Override
    public String getDeviceDisplayName(ScanResult scanResult) {
        return BluetoothDeviceFilterUtils.getDeviceDisplayNameInternal(scanResult);
    }

    @Override
    public int getMediumType() {
        return 2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.mNamePattern, ((WifiDeviceFilter) obj).mNamePattern);
    }

    public int hashCode() {
        return Objects.hash(this.mNamePattern);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(BluetoothDeviceFilterUtils.patternToString(getNamePattern()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final class Builder extends OneTimeUseBuilder<WifiDeviceFilter> {
        private Pattern mNamePattern;

        public Builder setNamePattern(Pattern pattern) {
            checkNotUsed();
            this.mNamePattern = pattern;
            return this;
        }

        @Override
        public WifiDeviceFilter build() {
            markUsed();
            return new WifiDeviceFilter(this.mNamePattern);
        }
    }
}

package android.hardware.location;

import android.annotation.SystemApi;
import android.hardware.contexthub.V1_0.ContextHub;
import android.net.wifi.WifiScanner;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

@SystemApi
public class ContextHubInfo implements Parcelable {
    public static final Parcelable.Creator<ContextHubInfo> CREATOR = new Parcelable.Creator<ContextHubInfo>() {
        @Override
        public ContextHubInfo createFromParcel(Parcel parcel) {
            return new ContextHubInfo(parcel);
        }

        @Override
        public ContextHubInfo[] newArray(int i) {
            return new ContextHubInfo[i];
        }
    };
    private byte mChreApiMajorVersion;
    private byte mChreApiMinorVersion;
    private short mChrePatchVersion;
    private long mChrePlatformId;
    private int mId;
    private int mMaxPacketLengthBytes;
    private MemoryRegion[] mMemoryRegions;
    private String mName;
    private float mPeakMips;
    private float mPeakPowerDrawMw;
    private int mPlatformVersion;
    private float mSleepPowerDrawMw;
    private float mStoppedPowerDrawMw;
    private int[] mSupportedSensors;
    private String mToolchain;
    private int mToolchainVersion;
    private String mVendor;

    public ContextHubInfo() {
    }

    public ContextHubInfo(ContextHub contextHub) {
        this.mId = contextHub.hubId;
        this.mName = contextHub.name;
        this.mVendor = contextHub.vendor;
        this.mToolchain = contextHub.toolchain;
        this.mPlatformVersion = contextHub.platformVersion;
        this.mToolchainVersion = contextHub.toolchainVersion;
        this.mPeakMips = contextHub.peakMips;
        this.mStoppedPowerDrawMw = contextHub.stoppedPowerDrawMw;
        this.mSleepPowerDrawMw = contextHub.sleepPowerDrawMw;
        this.mPeakPowerDrawMw = contextHub.peakPowerDrawMw;
        this.mMaxPacketLengthBytes = contextHub.maxSupportedMsgLen;
        this.mChrePlatformId = contextHub.chrePlatformId;
        this.mChreApiMajorVersion = contextHub.chreApiMajorVersion;
        this.mChreApiMinorVersion = contextHub.chreApiMinorVersion;
        this.mChrePatchVersion = contextHub.chrePatchVersion;
        this.mSupportedSensors = new int[0];
        this.mMemoryRegions = new MemoryRegion[0];
    }

    public int getMaxPacketLengthBytes() {
        return this.mMaxPacketLengthBytes;
    }

    public int getId() {
        return this.mId;
    }

    public String getName() {
        return this.mName;
    }

    public String getVendor() {
        return this.mVendor;
    }

    public String getToolchain() {
        return this.mToolchain;
    }

    public int getPlatformVersion() {
        return this.mPlatformVersion;
    }

    public int getStaticSwVersion() {
        return (this.mChreApiMajorVersion << 24) | (this.mChreApiMinorVersion << WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK) | this.mChrePatchVersion;
    }

    public int getToolchainVersion() {
        return this.mToolchainVersion;
    }

    public float getPeakMips() {
        return this.mPeakMips;
    }

    public float getStoppedPowerDrawMw() {
        return this.mStoppedPowerDrawMw;
    }

    public float getSleepPowerDrawMw() {
        return this.mSleepPowerDrawMw;
    }

    public float getPeakPowerDrawMw() {
        return this.mPeakPowerDrawMw;
    }

    public int[] getSupportedSensors() {
        return Arrays.copyOf(this.mSupportedSensors, this.mSupportedSensors.length);
    }

    public MemoryRegion[] getMemoryRegions() {
        return (MemoryRegion[]) Arrays.copyOf(this.mMemoryRegions, this.mMemoryRegions.length);
    }

    public long getChrePlatformId() {
        return this.mChrePlatformId;
    }

    public byte getChreApiMajorVersion() {
        return this.mChreApiMajorVersion;
    }

    public byte getChreApiMinorVersion() {
        return this.mChreApiMinorVersion;
    }

    public short getChrePatchVersion() {
        return this.mChrePatchVersion;
    }

    public String toString() {
        return ((((((((((("ID/handle : " + this.mId) + ", Name : " + this.mName) + "\n\tVendor : " + this.mVendor) + ", Toolchain : " + this.mToolchain) + ", Toolchain version: 0x" + Integer.toHexString(this.mToolchainVersion)) + "\n\tPlatformVersion : 0x" + Integer.toHexString(this.mPlatformVersion)) + ", SwVersion : " + ((int) this.mChreApiMajorVersion) + "." + ((int) this.mChreApiMinorVersion) + "." + ((int) this.mChrePatchVersion)) + ", CHRE platform ID: 0x" + Long.toHexString(this.mChrePlatformId)) + "\n\tPeakMips : " + this.mPeakMips) + ", StoppedPowerDraw : " + this.mStoppedPowerDrawMw + " mW") + ", PeakPowerDraw : " + this.mPeakPowerDrawMw + " mW") + ", MaxPacketLength : " + this.mMaxPacketLengthBytes + " Bytes";
    }

    private ContextHubInfo(Parcel parcel) {
        this.mId = parcel.readInt();
        this.mName = parcel.readString();
        this.mVendor = parcel.readString();
        this.mToolchain = parcel.readString();
        this.mPlatformVersion = parcel.readInt();
        this.mToolchainVersion = parcel.readInt();
        this.mPeakMips = parcel.readFloat();
        this.mStoppedPowerDrawMw = parcel.readFloat();
        this.mSleepPowerDrawMw = parcel.readFloat();
        this.mPeakPowerDrawMw = parcel.readFloat();
        this.mMaxPacketLengthBytes = parcel.readInt();
        this.mChrePlatformId = parcel.readLong();
        this.mChreApiMajorVersion = parcel.readByte();
        this.mChreApiMinorVersion = parcel.readByte();
        this.mChrePatchVersion = (short) parcel.readInt();
        this.mSupportedSensors = new int[parcel.readInt()];
        parcel.readIntArray(this.mSupportedSensors);
        this.mMemoryRegions = (MemoryRegion[]) parcel.createTypedArray(MemoryRegion.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mId);
        parcel.writeString(this.mName);
        parcel.writeString(this.mVendor);
        parcel.writeString(this.mToolchain);
        parcel.writeInt(this.mPlatformVersion);
        parcel.writeInt(this.mToolchainVersion);
        parcel.writeFloat(this.mPeakMips);
        parcel.writeFloat(this.mStoppedPowerDrawMw);
        parcel.writeFloat(this.mSleepPowerDrawMw);
        parcel.writeFloat(this.mPeakPowerDrawMw);
        parcel.writeInt(this.mMaxPacketLengthBytes);
        parcel.writeLong(this.mChrePlatformId);
        parcel.writeByte(this.mChreApiMajorVersion);
        parcel.writeByte(this.mChreApiMinorVersion);
        parcel.writeInt(this.mChrePatchVersion);
        parcel.writeInt(this.mSupportedSensors.length);
        parcel.writeIntArray(this.mSupportedSensors);
        parcel.writeTypedArray(this.mMemoryRegions, i);
    }
}

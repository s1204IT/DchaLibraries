package com.android.packageinstaller;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import libcore.io.IoUtils;

public class InstallFlowAnalytics implements Parcelable {
    public static final Parcelable.Creator<InstallFlowAnalytics> CREATOR = new Parcelable.Creator<InstallFlowAnalytics>() {
        @Override
        public InstallFlowAnalytics createFromParcel(Parcel in) {
            return new InstallFlowAnalytics(in);
        }

        @Override
        public InstallFlowAnalytics[] newArray(int size) {
            return new InstallFlowAnalytics[size];
        }
    };
    private Context mContext;
    private long mEndTimestampMillis;
    private int mFlags;
    private long mInstallButtonClickTimestampMillis;
    private boolean mLogged;
    private long mPackageInfoObtainedTimestampMillis;
    private int mPackageManagerInstallResult;
    private String mPackageUri;
    private byte mResult;
    private long mStartTimestampMillis;

    public InstallFlowAnalytics() {
        this.mResult = (byte) -1;
    }

    public InstallFlowAnalytics(Parcel in) {
        this.mResult = (byte) -1;
        this.mFlags = in.readInt();
        this.mResult = in.readByte();
        this.mPackageManagerInstallResult = in.readInt();
        this.mStartTimestampMillis = in.readLong();
        this.mPackageInfoObtainedTimestampMillis = in.readLong();
        this.mInstallButtonClickTimestampMillis = in.readLong();
        this.mEndTimestampMillis = in.readLong();
        this.mPackageUri = in.readString();
        this.mLogged = readBoolean(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mFlags);
        dest.writeByte(this.mResult);
        dest.writeInt(this.mPackageManagerInstallResult);
        dest.writeLong(this.mStartTimestampMillis);
        dest.writeLong(this.mPackageInfoObtainedTimestampMillis);
        dest.writeLong(this.mInstallButtonClickTimestampMillis);
        dest.writeLong(this.mEndTimestampMillis);
        dest.writeString(this.mPackageUri);
        writeBoolean(dest, this.mLogged);
    }

    private static void writeBoolean(Parcel dest, boolean value) {
        dest.writeByte((byte) (value ? 1 : 0));
    }

    private static boolean readBoolean(Parcel dest) {
        return dest.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    void setContext(Context context) {
        this.mContext = context;
    }

    void setInstallsFromUnknownSourcesPermitted(boolean permitted) {
        setFlagState(1, permitted);
    }

    private boolean isInstallsFromUnknownSourcesPermitted() {
        return isFlagSet(1);
    }

    void setInstallRequestFromUnknownSource(boolean unknownSource) {
        setFlagState(2, unknownSource);
    }

    private boolean isInstallRequestFromUnknownSource() {
        return isFlagSet(2);
    }

    void setVerifyAppsEnabled(boolean enabled) {
        setFlagState(4, enabled);
    }

    private boolean isVerifyAppsEnabled() {
        return isFlagSet(4);
    }

    void setAppVerifierInstalled(boolean installed) {
        setFlagState(8, installed);
    }

    private boolean isAppVerifierInstalled() {
        return isFlagSet(8);
    }

    void setFileUri(boolean fileUri) {
        setFlagState(16, fileUri);
    }

    void setPackageUri(String packageUri) {
        this.mPackageUri = packageUri;
    }

    private boolean isFileUri() {
        return isFlagSet(16);
    }

    void setReplace(boolean replace) {
        setFlagState(32, replace);
    }

    private boolean isReplace() {
        return isFlagSet(32);
    }

    void setSystemApp(boolean systemApp) {
        setFlagState(64, systemApp);
    }

    private boolean isSystemApp() {
        return isFlagSet(64);
    }

    void setNewPermissionsFound(boolean found) {
        setFlagState(512, found);
    }

    private boolean isNewPermissionsFound() {
        return isFlagSet(512);
    }

    void setPermissionsDisplayed(boolean displayed) {
        setFlagState(1024, displayed);
    }

    private boolean isPermissionsDisplayed() {
        return isFlagSet(1024);
    }

    void setNewPermissionsDisplayed(boolean displayed) {
        setFlagState(2048, displayed);
    }

    private boolean isNewPermissionsDisplayed() {
        return isFlagSet(2048);
    }

    void setAllPermissionsDisplayed(boolean displayed) {
        setFlagState(4096, displayed);
    }

    private boolean isAllPermissionsDisplayed() {
        return isFlagSet(4096);
    }

    void setStartTimestampMillis(long timestampMillis) {
        this.mStartTimestampMillis = timestampMillis;
    }

    void setPackageInfoObtained() {
        setFlagState(128, true);
        this.mPackageInfoObtainedTimestampMillis = SystemClock.elapsedRealtime();
    }

    private boolean isPackageInfoObtained() {
        return isFlagSet(128);
    }

    void setInstallButtonClicked() {
        setFlagState(256, true);
        this.mInstallButtonClickTimestampMillis = SystemClock.elapsedRealtime();
    }

    private boolean isInstallButtonClicked() {
        return isFlagSet(256);
    }

    void setFlowFinishedWithPackageManagerResult(int packageManagerResult) {
        this.mPackageManagerInstallResult = packageManagerResult;
        if (packageManagerResult == 1) {
            setFlowFinished((byte) 0);
        } else {
            setFlowFinished((byte) 6);
        }
    }

    void setFlowFinished(byte result) {
        if (!this.mLogged) {
            this.mResult = result;
            this.mEndTimestampMillis = SystemClock.elapsedRealtime();
            writeToEventLog();
        }
    }

    private void writeToEventLog() {
        byte packageManagerInstallResultByte = 0;
        if (this.mResult == 6) {
            packageManagerInstallResultByte = clipUnsignedValueToUnsignedByte(-this.mPackageManagerInstallResult);
        }
        final int resultAndFlags = (this.mResult & 255) | ((packageManagerInstallResultByte & 255) << 8) | ((this.mFlags & 65535) << 16);
        final int totalElapsedTime = clipUnsignedLongToUnsignedInt(this.mEndTimestampMillis - this.mStartTimestampMillis);
        final int elapsedTimeTillPackageInfoObtained = isPackageInfoObtained() ? clipUnsignedLongToUnsignedInt(this.mPackageInfoObtainedTimestampMillis - this.mStartTimestampMillis) : 0;
        final int elapsedTimeTillInstallButtonClick = isInstallButtonClicked() ? clipUnsignedLongToUnsignedInt(this.mInstallButtonClickTimestampMillis - this.mStartTimestampMillis) : 0;
        if ((this.mFlags & 16) != 0 && (this.mFlags & 4) != 0 && isUserConsentToVerifyAppsGranted()) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    boolean z = 0;
                    z = 0;
                    byte[] packageContentsDigest = null;
                    try {
                        try {
                            packageContentsDigest = InstallFlowAnalytics.this.getPackageContentsDigest();
                            String strBytesToHexString = packageContentsDigest != null ? IntegralToString.bytesToHexString(packageContentsDigest, false) : "";
                            int i = resultAndFlags;
                            int i2 = totalElapsedTime;
                            int i3 = elapsedTimeTillPackageInfoObtained;
                            EventLogTags.writeInstallPackageAttempt(i, i2, i3, elapsedTimeTillInstallButtonClick, strBytesToHexString);
                            z = i3;
                        } catch (IOException e) {
                            Log.w("InstallFlowAnalytics", "Failed to hash APK contents", e);
                            String strBytesToHexString2 = 0 != 0 ? IntegralToString.bytesToHexString((byte[]) null, false) : "";
                            int i4 = resultAndFlags;
                            int i5 = totalElapsedTime;
                            int i6 = elapsedTimeTillPackageInfoObtained;
                            EventLogTags.writeInstallPackageAttempt(i4, i5, i6, elapsedTimeTillInstallButtonClick, strBytesToHexString2);
                            z = i6;
                        }
                    } catch (Throwable th) {
                        EventLogTags.writeInstallPackageAttempt(resultAndFlags, totalElapsedTime, elapsedTimeTillPackageInfoObtained, elapsedTimeTillInstallButtonClick, packageContentsDigest != null ? IntegralToString.bytesToHexString(packageContentsDigest, z) : "");
                        throw th;
                    }
                }
            });
        } else {
            EventLogTags.writeInstallPackageAttempt(resultAndFlags, totalElapsedTime, elapsedTimeTillPackageInfoObtained, elapsedTimeTillInstallButtonClick, "");
        }
        this.mLogged = true;
        if (Log.isLoggable("InstallFlowAnalytics", 2)) {
            Log.v("InstallFlowAnalytics", "Analytics:\n\tinstallsFromUnknownSourcesPermitted: " + isInstallsFromUnknownSourcesPermitted() + "\n\tinstallRequestFromUnknownSource: " + isInstallRequestFromUnknownSource() + "\n\tverifyAppsEnabled: " + isVerifyAppsEnabled() + "\n\tappVerifierInstalled: " + isAppVerifierInstalled() + "\n\tfileUri: " + isFileUri() + "\n\treplace: " + isReplace() + "\n\tsystemApp: " + isSystemApp() + "\n\tpackageInfoObtained: " + isPackageInfoObtained() + "\n\tinstallButtonClicked: " + isInstallButtonClicked() + "\n\tpermissionsDisplayed: " + isPermissionsDisplayed() + "\n\tnewPermissionsDisplayed: " + isNewPermissionsDisplayed() + "\n\tallPermissionsDisplayed: " + isAllPermissionsDisplayed() + "\n\tnewPermissionsFound: " + isNewPermissionsFound() + "\n\tresult: " + ((int) this.mResult) + "\n\tpackageManagerInstallResult: " + this.mPackageManagerInstallResult + "\n\ttotalDuration: " + (this.mEndTimestampMillis - this.mStartTimestampMillis) + " ms\n\ttimeTillPackageInfoObtained: " + (isPackageInfoObtained() ? (this.mPackageInfoObtainedTimestampMillis - this.mStartTimestampMillis) + " ms" : "n/a") + "\n\ttimeTillInstallButtonClick: " + (isInstallButtonClicked() ? (this.mInstallButtonClickTimestampMillis - this.mStartTimestampMillis) + " ms" : "n/a"));
            Log.v("InstallFlowAnalytics", "Wrote to Event Log: 0x" + Long.toString(((long) resultAndFlags) & 4294967295L, 16) + ", " + totalElapsedTime + ", " + elapsedTimeTillPackageInfoObtained + ", " + elapsedTimeTillInstallButtonClick);
        }
    }

    private static final byte clipUnsignedValueToUnsignedByte(long value) {
        if (value < 0) {
            return (byte) 0;
        }
        if (value > 255) {
            return (byte) -1;
        }
        return (byte) value;
    }

    private static final int clipUnsignedLongToUnsignedInt(long value) {
        if (value < 0) {
            return 0;
        }
        if (value > 4294967295L) {
            return -1;
        }
        return (int) value;
    }

    private void setFlagState(int flag, boolean set) {
        if (set) {
            this.mFlags |= flag;
        } else {
            this.mFlags &= flag ^ (-1);
        }
    }

    private boolean isFlagSet(int flag) {
        return (this.mFlags & flag) == flag;
    }

    private boolean isUserConsentToVerifyAppsGranted() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "package_verifier_user_consent", 0) != 0;
    }

    private byte[] getPackageContentsDigest() throws IOException {
        File file = new File(Uri.parse(this.mPackageUri).getPath());
        return getSha256ContentsDigest(file);
    }

    private static byte[] getSha256ContentsDigest(File file) throws Throwable {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            InputStream in = null;
            try {
                InputStream in2 = new BufferedInputStream(new FileInputStream(file), buf.length);
                while (true) {
                    try {
                        int chunkSize = in2.read(buf);
                        if (chunkSize != -1) {
                            digest.update(buf, 0, chunkSize);
                        } else {
                            IoUtils.closeQuietly(in2);
                            return digest.digest();
                        }
                    } catch (Throwable th) {
                        th = th;
                        in = in2;
                        IoUtils.closeQuietly(in);
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

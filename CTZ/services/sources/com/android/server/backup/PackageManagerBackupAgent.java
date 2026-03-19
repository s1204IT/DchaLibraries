package com.android.server.backup;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.backup.utils.AppBackupUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PackageManagerBackupAgent extends BackupAgent {
    private static final String ANCESTRAL_RECORD_KEY = "@ancestral_record@";
    private static final int ANCESTRAL_RECORD_VERSION = 1;
    private static final boolean DEBUG = false;
    private static final String DEFAULT_HOME_KEY = "@home@";
    private static final String GLOBAL_METADATA_KEY = "@meta@";
    private static final String STATE_FILE_HEADER = "=state=";
    private static final int STATE_FILE_VERSION = 2;
    private static final String TAG = "PMBA";
    private static final int UNDEFINED_ANCESTRAL_RECORD_VERSION = -1;
    private List<PackageInfo> mAllPackages;
    private boolean mHasMetadata;
    private PackageManager mPackageManager;
    private ComponentName mRestoredHome;
    private String mRestoredHomeInstaller;
    private ArrayList<byte[]> mRestoredHomeSigHashes;
    private long mRestoredHomeVersion;
    private HashMap<String, Metadata> mRestoredSignatures;
    private ComponentName mStoredHomeComponent;
    private ArrayList<byte[]> mStoredHomeSigHashes;
    private long mStoredHomeVersion;
    private String mStoredIncrementalVersion;
    private int mStoredSdkVersion;
    private HashMap<String, Metadata> mStateVersions = new HashMap<>();
    private final HashSet<String> mExisting = new HashSet<>();

    interface RestoreDataConsumer {
        void consumeRestoreData(BackupDataInput backupDataInput) throws IOException;
    }

    public class Metadata {
        public ArrayList<byte[]> sigHashes;
        public long versionCode;

        Metadata(long j, ArrayList<byte[]> arrayList) {
            this.versionCode = j;
            this.sigHashes = arrayList;
        }
    }

    public PackageManagerBackupAgent(PackageManager packageManager, List<PackageInfo> list) {
        init(packageManager, list);
    }

    public PackageManagerBackupAgent(PackageManager packageManager) {
        init(packageManager, null);
        evaluateStorablePackages();
    }

    private void init(PackageManager packageManager, List<PackageInfo> list) {
        this.mPackageManager = packageManager;
        this.mAllPackages = list;
        this.mRestoredSignatures = null;
        this.mHasMetadata = false;
        this.mStoredSdkVersion = Build.VERSION.SDK_INT;
        this.mStoredIncrementalVersion = Build.VERSION.INCREMENTAL;
    }

    public void evaluateStorablePackages() {
        this.mAllPackages = getStorableApplications(this.mPackageManager);
    }

    public static List<PackageInfo> getStorableApplications(PackageManager packageManager) {
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(134217728);
        for (int size = installedPackages.size() - 1; size >= 0; size--) {
            if (!AppBackupUtils.appIsEligibleForBackup(installedPackages.get(size).applicationInfo, packageManager)) {
                installedPackages.remove(size);
            }
        }
        return installedPackages;
    }

    public boolean hasMetadata() {
        return this.mHasMetadata;
    }

    public Metadata getRestoredMetadata(String str) {
        if (this.mRestoredSignatures == null) {
            Slog.w(TAG, "getRestoredMetadata() before metadata read!");
            return null;
        }
        return this.mRestoredSignatures.get(str);
    }

    public Set<String> getRestoredPackages() {
        if (this.mRestoredSignatures == null) {
            Slog.w(TAG, "getRestoredPackages() before metadata read!");
            return null;
        }
        return this.mRestoredSignatures.keySet();
    }

    @Override
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) {
        PackageInfo packageInfo;
        String installerPackageName;
        ArrayList<byte[]> arrayList;
        long longVersionCode;
        ArrayList<byte[]> arrayListHashSignatureArray;
        ComponentName componentName;
        PackageInfo packageInfo2;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        parseStateFile(parcelFileDescriptor);
        if (this.mStoredIncrementalVersion == null || !this.mStoredIncrementalVersion.equals(Build.VERSION.INCREMENTAL)) {
            Slog.i(TAG, "Previous metadata " + this.mStoredIncrementalVersion + " mismatch vs " + Build.VERSION.INCREMENTAL + " - rewriting");
            this.mExisting.clear();
        }
        boolean z = true;
        try {
            dataOutputStream.writeInt(1);
            writeEntity(backupDataOutput, ANCESTRAL_RECORD_KEY, byteArrayOutputStream.toByteArray());
            long j = 0;
            ComponentName preferredHomeComponent = getPreferredHomeComponent();
            int i = 134217728;
            ComponentName componentName2 = null;
            if (preferredHomeComponent != null) {
                try {
                    packageInfo = this.mPackageManager.getPackageInfo(preferredHomeComponent.getPackageName(), 134217728);
                    try {
                        installerPackageName = this.mPackageManager.getInstallerPackageName(preferredHomeComponent.getPackageName());
                        try {
                            longVersionCode = packageInfo.getLongVersionCode();
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                    } catch (PackageManager.NameNotFoundException e2) {
                        installerPackageName = null;
                    }
                } catch (PackageManager.NameNotFoundException e3) {
                    packageInfo = null;
                    installerPackageName = null;
                }
                try {
                    SigningInfo signingInfo = packageInfo.signingInfo;
                    if (signingInfo == null) {
                        Slog.e(TAG, "Home app has no signing information");
                        arrayListHashSignatureArray = null;
                    } else {
                        arrayListHashSignatureArray = BackupUtils.hashSignatureArray(signingInfo.getApkContentsSigners());
                    }
                    componentName2 = preferredHomeComponent;
                    arrayList = arrayListHashSignatureArray;
                    j = longVersionCode;
                } catch (PackageManager.NameNotFoundException e4) {
                    j = longVersionCode;
                    Slog.w(TAG, "Can't access preferred home info");
                    arrayList = null;
                }
            } else {
                packageInfo = null;
                installerPackageName = null;
                arrayList = null;
                componentName2 = preferredHomeComponent;
            }
            try {
                PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                if (j == this.mStoredHomeVersion && Objects.equals(componentName2, this.mStoredHomeComponent) && (componentName2 == null || BackupUtils.signaturesMatch(this.mStoredHomeSigHashes, packageInfo, packageManagerInternal))) {
                    z = false;
                }
                if (z) {
                    if (componentName2 != null) {
                        byteArrayOutputStream.reset();
                        dataOutputStream.writeUTF(componentName2.flattenToString());
                        dataOutputStream.writeLong(j);
                        if (installerPackageName == null) {
                            installerPackageName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        }
                        dataOutputStream.writeUTF(installerPackageName);
                        writeSignatureHashArray(dataOutputStream, arrayList);
                        writeEntity(backupDataOutput, DEFAULT_HOME_KEY, byteArrayOutputStream.toByteArray());
                    } else {
                        backupDataOutput.writeEntityHeader(DEFAULT_HOME_KEY, -1);
                    }
                }
                byteArrayOutputStream.reset();
                if (!this.mExisting.contains(GLOBAL_METADATA_KEY)) {
                    dataOutputStream.writeInt(Build.VERSION.SDK_INT);
                    dataOutputStream.writeUTF(Build.VERSION.INCREMENTAL);
                    writeEntity(backupDataOutput, GLOBAL_METADATA_KEY, byteArrayOutputStream.toByteArray());
                } else {
                    this.mExisting.remove(GLOBAL_METADATA_KEY);
                }
                Iterator<PackageInfo> it = this.mAllPackages.iterator();
                while (it.hasNext()) {
                    String str = it.next().packageName;
                    if (!str.equals(GLOBAL_METADATA_KEY)) {
                        try {
                            packageInfo2 = this.mPackageManager.getPackageInfo(str, i);
                        } catch (PackageManager.NameNotFoundException e5) {
                            componentName = componentName2;
                            this.mExisting.add(str);
                        }
                        if (this.mExisting.contains(str)) {
                            this.mExisting.remove(str);
                            componentName = componentName2;
                            if (packageInfo2.getLongVersionCode() == this.mStateVersions.get(str).versionCode) {
                            }
                            componentName2 = componentName;
                            i = 134217728;
                        } else {
                            componentName = componentName2;
                        }
                        SigningInfo signingInfo2 = packageInfo2.signingInfo;
                        if (signingInfo2 == null) {
                            Slog.w(TAG, "Not backing up package " + str + " since it appears to have no signatures.");
                        } else {
                            byteArrayOutputStream.reset();
                            if (packageInfo2.versionCodeMajor != 0) {
                                dataOutputStream.writeInt(Integer.MIN_VALUE);
                                dataOutputStream.writeLong(packageInfo2.getLongVersionCode());
                            } else {
                                dataOutputStream.writeInt(packageInfo2.versionCode);
                            }
                            writeSignatureHashArray(dataOutputStream, BackupUtils.hashSignatureArray(signingInfo2.getApkContentsSigners()));
                            writeEntity(backupDataOutput, str, byteArrayOutputStream.toByteArray());
                        }
                        componentName2 = componentName;
                        i = 134217728;
                    }
                }
                writeStateFile(this.mAllPackages, componentName2, j, arrayList, parcelFileDescriptor2);
            } catch (IOException e6) {
                Slog.e(TAG, "Unable to write package backup data file!");
            }
        } catch (IOException e7) {
            Slog.e(TAG, "Unable to write package backup data file!");
        }
    }

    private static void writeEntity(BackupDataOutput backupDataOutput, String str, byte[] bArr) throws IOException {
        backupDataOutput.writeEntityHeader(str, bArr.length);
        backupDataOutput.writeEntityData(bArr, bArr.length);
    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        RestoreDataConsumer restoreDataConsumer = getRestoreDataConsumer(getAncestralRecordVersionValue(backupDataInput));
        if (restoreDataConsumer == null) {
            Slog.w(TAG, "Ancestral restore set version is unknown to this Android version; not restoring");
        } else {
            restoreDataConsumer.consumeRestoreData(backupDataInput);
        }
    }

    private int getAncestralRecordVersionValue(BackupDataInput backupDataInput) throws IOException {
        if (backupDataInput.readNextHeader()) {
            String key = backupDataInput.getKey();
            int dataSize = backupDataInput.getDataSize();
            if (ANCESTRAL_RECORD_KEY.equals(key)) {
                byte[] bArr = new byte[dataSize];
                backupDataInput.readEntityData(bArr, 0, dataSize);
                return new DataInputStream(new ByteArrayInputStream(bArr)).readInt();
            }
        }
        return -1;
    }

    private RestoreDataConsumer getRestoreDataConsumer(int i) {
        if (i == -1) {
            return new LegacyRestoreDataConsumer();
        }
        if (i == 1) {
            return new AncestralVersion1RestoreDataConsumer();
        }
        Slog.e(TAG, "Unrecognized ANCESTRAL_RECORD_VERSION: " + i);
        return null;
    }

    private static void writeSignatureHashArray(DataOutputStream dataOutputStream, ArrayList<byte[]> arrayList) throws IOException {
        dataOutputStream.writeInt(arrayList.size());
        for (byte[] bArr : arrayList) {
            dataOutputStream.writeInt(bArr.length);
            dataOutputStream.write(bArr);
        }
    }

    private static ArrayList<byte[]> readSignatureHashArray(DataInputStream dataInputStream) {
        try {
            try {
                int i = dataInputStream.readInt();
                if (i > 20) {
                    Slog.e(TAG, "Suspiciously large sig count in restore data; aborting");
                    throw new IllegalStateException("Bad restore state");
                }
                ArrayList<byte[]> arrayList = new ArrayList<>(i);
                boolean z = false;
                for (int i2 = 0; i2 < i; i2++) {
                    int i3 = dataInputStream.readInt();
                    byte[] bArr = new byte[i3];
                    dataInputStream.read(bArr);
                    arrayList.add(bArr);
                    if (i3 != 32) {
                        z = true;
                    }
                }
                if (z) {
                    return BackupUtils.hashSignatureArray(arrayList);
                }
                return arrayList;
            } catch (EOFException e) {
                Slog.w(TAG, "Read empty signature block");
                return null;
            }
        } catch (IOException e2) {
            Slog.e(TAG, "Unable to read signatures");
            return null;
        }
    }

    private void parseStateFile(ParcelFileDescriptor parcelFileDescriptor) {
        long j;
        this.mExisting.clear();
        this.mStateVersions.clear();
        boolean z = false;
        this.mStoredSdkVersion = 0;
        this.mStoredIncrementalVersion = null;
        this.mStoredHomeComponent = null;
        this.mStoredHomeVersion = 0L;
        this.mStoredHomeSigHashes = null;
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor())));
        try {
            String utf = dataInputStream.readUTF();
            if (utf.equals(STATE_FILE_HEADER)) {
                int i = dataInputStream.readInt();
                if (i > 2) {
                    Slog.w(TAG, "Unsupported state file version " + i + ", redoing from start");
                    return;
                }
                utf = dataInputStream.readUTF();
            } else {
                Slog.i(TAG, "Older version of saved state - rewriting");
                z = true;
            }
            if (utf.equals(DEFAULT_HOME_KEY)) {
                this.mStoredHomeComponent = ComponentName.unflattenFromString(dataInputStream.readUTF());
                this.mStoredHomeVersion = dataInputStream.readLong();
                this.mStoredHomeSigHashes = readSignatureHashArray(dataInputStream);
                utf = dataInputStream.readUTF();
            }
            if (utf.equals(GLOBAL_METADATA_KEY)) {
                this.mStoredSdkVersion = dataInputStream.readInt();
                this.mStoredIncrementalVersion = dataInputStream.readUTF();
                if (!z) {
                    this.mExisting.add(GLOBAL_METADATA_KEY);
                }
                while (true) {
                    String utf2 = dataInputStream.readUTF();
                    int i2 = dataInputStream.readInt();
                    if (i2 == Integer.MIN_VALUE) {
                        j = dataInputStream.readLong();
                    } else {
                        j = i2;
                    }
                    if (!z) {
                        this.mExisting.add(utf2);
                    }
                    this.mStateVersions.put(utf2, new Metadata(j, null));
                }
            } else {
                Slog.e(TAG, "No global metadata in state file!");
            }
        } catch (EOFException e) {
        } catch (IOException e2) {
            Slog.e(TAG, "Unable to read Package Manager state file: " + e2);
        }
    }

    private ComponentName getPreferredHomeComponent() {
        return this.mPackageManager.getHomeActivities(new ArrayList());
    }

    private void writeStateFile(List<PackageInfo> list, ComponentName componentName, long j, ArrayList<byte[]> arrayList, ParcelFileDescriptor parcelFileDescriptor) {
        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(parcelFileDescriptor.getFileDescriptor())));
        try {
            dataOutputStream.writeUTF(STATE_FILE_HEADER);
            dataOutputStream.writeInt(2);
            if (componentName != null) {
                dataOutputStream.writeUTF(DEFAULT_HOME_KEY);
                dataOutputStream.writeUTF(componentName.flattenToString());
                dataOutputStream.writeLong(j);
                writeSignatureHashArray(dataOutputStream, arrayList);
            }
            dataOutputStream.writeUTF(GLOBAL_METADATA_KEY);
            dataOutputStream.writeInt(Build.VERSION.SDK_INT);
            dataOutputStream.writeUTF(Build.VERSION.INCREMENTAL);
            for (PackageInfo packageInfo : list) {
                dataOutputStream.writeUTF(packageInfo.packageName);
                if (packageInfo.versionCodeMajor != 0) {
                    dataOutputStream.writeInt(Integer.MIN_VALUE);
                    dataOutputStream.writeLong(packageInfo.getLongVersionCode());
                } else {
                    dataOutputStream.writeInt(packageInfo.versionCode);
                }
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            Slog.e(TAG, "Unable to write package manager state file!");
        }
    }

    private class LegacyRestoreDataConsumer implements RestoreDataConsumer {
        private LegacyRestoreDataConsumer() {
        }

        @Override
        public void consumeRestoreData(BackupDataInput backupDataInput) throws IOException {
            long j;
            ArrayList arrayList = new ArrayList();
            HashMap map = new HashMap();
            while (true) {
                String key = backupDataInput.getKey();
                int dataSize = backupDataInput.getDataSize();
                byte[] bArr = new byte[dataSize];
                backupDataInput.readEntityData(bArr, 0, dataSize);
                DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
                if (key.equals(PackageManagerBackupAgent.GLOBAL_METADATA_KEY)) {
                    PackageManagerBackupAgent.this.mStoredSdkVersion = dataInputStream.readInt();
                    PackageManagerBackupAgent.this.mStoredIncrementalVersion = dataInputStream.readUTF();
                    PackageManagerBackupAgent.this.mHasMetadata = true;
                } else if (key.equals(PackageManagerBackupAgent.DEFAULT_HOME_KEY)) {
                    PackageManagerBackupAgent.this.mRestoredHome = ComponentName.unflattenFromString(dataInputStream.readUTF());
                    PackageManagerBackupAgent.this.mRestoredHomeVersion = dataInputStream.readLong();
                    PackageManagerBackupAgent.this.mRestoredHomeInstaller = dataInputStream.readUTF();
                    PackageManagerBackupAgent.this.mRestoredHomeSigHashes = PackageManagerBackupAgent.readSignatureHashArray(dataInputStream);
                } else {
                    int i = dataInputStream.readInt();
                    if (i == Integer.MIN_VALUE) {
                        j = dataInputStream.readLong();
                    } else {
                        j = i;
                    }
                    ArrayList signatureHashArray = PackageManagerBackupAgent.readSignatureHashArray(dataInputStream);
                    if (signatureHashArray == null || signatureHashArray.size() == 0) {
                        Slog.w(PackageManagerBackupAgent.TAG, "Not restoring package " + key + " since it appears to have no signatures.");
                    } else {
                        ApplicationInfo applicationInfo = new ApplicationInfo();
                        applicationInfo.packageName = key;
                        arrayList.add(applicationInfo);
                        map.put(key, PackageManagerBackupAgent.this.new Metadata(j, signatureHashArray));
                    }
                }
                if (!backupDataInput.readNextHeader()) {
                    PackageManagerBackupAgent.this.mRestoredSignatures = map;
                    return;
                }
            }
        }
    }

    private class AncestralVersion1RestoreDataConsumer implements RestoreDataConsumer {
        private AncestralVersion1RestoreDataConsumer() {
        }

        @Override
        public void consumeRestoreData(BackupDataInput backupDataInput) throws IOException {
            long j;
            ArrayList arrayList = new ArrayList();
            HashMap map = new HashMap();
            while (backupDataInput.readNextHeader()) {
                String key = backupDataInput.getKey();
                int dataSize = backupDataInput.getDataSize();
                byte[] bArr = new byte[dataSize];
                backupDataInput.readEntityData(bArr, 0, dataSize);
                DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
                if (key.equals(PackageManagerBackupAgent.GLOBAL_METADATA_KEY)) {
                    PackageManagerBackupAgent.this.mStoredSdkVersion = dataInputStream.readInt();
                    PackageManagerBackupAgent.this.mStoredIncrementalVersion = dataInputStream.readUTF();
                    PackageManagerBackupAgent.this.mHasMetadata = true;
                } else if (key.equals(PackageManagerBackupAgent.DEFAULT_HOME_KEY)) {
                    PackageManagerBackupAgent.this.mRestoredHome = ComponentName.unflattenFromString(dataInputStream.readUTF());
                    PackageManagerBackupAgent.this.mRestoredHomeVersion = dataInputStream.readLong();
                    PackageManagerBackupAgent.this.mRestoredHomeInstaller = dataInputStream.readUTF();
                    PackageManagerBackupAgent.this.mRestoredHomeSigHashes = PackageManagerBackupAgent.readSignatureHashArray(dataInputStream);
                } else {
                    int i = dataInputStream.readInt();
                    if (i == Integer.MIN_VALUE) {
                        j = dataInputStream.readLong();
                    } else {
                        j = i;
                    }
                    ArrayList signatureHashArray = PackageManagerBackupAgent.readSignatureHashArray(dataInputStream);
                    if (signatureHashArray == null || signatureHashArray.size() == 0) {
                        Slog.w(PackageManagerBackupAgent.TAG, "Not restoring package " + key + " since it appears to have no signatures.");
                    } else {
                        ApplicationInfo applicationInfo = new ApplicationInfo();
                        applicationInfo.packageName = key;
                        arrayList.add(applicationInfo);
                        map.put(key, PackageManagerBackupAgent.this.new Metadata(j, signatureHashArray));
                    }
                }
            }
            PackageManagerBackupAgent.this.mRestoredSignatures = map;
        }
    }
}

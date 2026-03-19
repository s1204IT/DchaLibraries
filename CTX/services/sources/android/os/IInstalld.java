package android.os;

import java.io.FileDescriptor;

public interface IInstalld extends IInterface {
    void assertFsverityRootHashMatches(String str, byte[] bArr) throws RemoteException;

    void clearAppData(String str, String str2, int i, int i2, long j) throws RemoteException;

    void clearAppProfiles(String str, String str2) throws RemoteException;

    boolean copySystemProfile(String str, int i, String str2, String str3) throws RemoteException;

    long createAppData(String str, String str2, int i, int i2, int i3, String str3, int i4) throws RemoteException;

    void createOatDir(String str, String str2) throws RemoteException;

    boolean createProfileSnapshot(int i, String str, String str2, String str3) throws RemoteException;

    void createUserData(String str, int i, int i2, int i3) throws RemoteException;

    void deleteOdex(String str, String str2, String str3) throws RemoteException;

    void destroyAppData(String str, String str2, int i, int i2, long j) throws RemoteException;

    void destroyAppProfiles(String str) throws RemoteException;

    void destroyProfileSnapshot(String str, String str2) throws RemoteException;

    void destroyUserData(String str, int i, int i2) throws RemoteException;

    void dexopt(String str, int i, String str2, String str3, int i2, String str4, int i3, String str5, String str6, String str7, String str8, boolean z, int i4, String str9, String str10, String str11) throws RemoteException;

    boolean dumpProfiles(int i, String str, String str2, String str3) throws RemoteException;

    void fixupAppData(String str, int i) throws RemoteException;

    void freeCache(String str, long j, long j2, int i) throws RemoteException;

    long[] getAppSize(String str, String[] strArr, int i, int i2, int i3, long[] jArr, String[] strArr2) throws RemoteException;

    long[] getExternalSize(String str, int i, int i2, int[] iArr) throws RemoteException;

    long[] getUserSize(String str, int i, int i2, int[] iArr) throws RemoteException;

    byte[] hashSecondaryDexFile(String str, String str2, int i, String str3, int i2) throws RemoteException;

    void idmap(String str, String str2, int i) throws RemoteException;

    void installApkVerity(String str, FileDescriptor fileDescriptor, int i) throws RemoteException;

    void invalidateMounts() throws RemoteException;

    boolean isQuotaSupported(String str) throws RemoteException;

    void linkFile(String str, String str2, String str3) throws RemoteException;

    void linkNativeLibraryDirectory(String str, String str2, String str3, int i) throws RemoteException;

    void markBootComplete(String str) throws RemoteException;

    boolean mergeProfiles(int i, String str, String str2) throws RemoteException;

    void migrateAppData(String str, String str2, int i, int i2) throws RemoteException;

    void moveAb(String str, String str2, String str3) throws RemoteException;

    void moveCompleteApp(String str, String str2, String str3, String str4, int i, String str5, int i2) throws RemoteException;

    boolean prepareAppProfile(String str, int i, int i2, String str2, String str3, String str4) throws RemoteException;

    boolean reconcileSecondaryDexFile(String str, String str2, int i, String[] strArr, String str3, int i2) throws RemoteException;

    void removeIdmap(String str) throws RemoteException;

    void restoreconAppData(String str, String str2, int i, int i2, int i3, String str3) throws RemoteException;

    void rmPackageDir(String str) throws RemoteException;

    void rmdex(String str, String str2) throws RemoteException;

    void setAppQuota(String str, int i, int i2, long j) throws RemoteException;

    public static abstract class Stub extends Binder implements IInstalld {
        private static final String DESCRIPTOR = "android.os.IInstalld";
        static final int TRANSACTION_assertFsverityRootHashMatches = 34;
        static final int TRANSACTION_clearAppData = 6;
        static final int TRANSACTION_clearAppProfiles = 19;
        static final int TRANSACTION_copySystemProfile = 18;
        static final int TRANSACTION_createAppData = 3;
        static final int TRANSACTION_createOatDir = 29;
        static final int TRANSACTION_createProfileSnapshot = 21;
        static final int TRANSACTION_createUserData = 1;
        static final int TRANSACTION_deleteOdex = 32;
        static final int TRANSACTION_destroyAppData = 7;
        static final int TRANSACTION_destroyAppProfiles = 20;
        static final int TRANSACTION_destroyProfileSnapshot = 22;
        static final int TRANSACTION_destroyUserData = 2;
        static final int TRANSACTION_dexopt = 14;
        static final int TRANSACTION_dumpProfiles = 17;
        static final int TRANSACTION_fixupAppData = 8;
        static final int TRANSACTION_freeCache = 27;
        static final int TRANSACTION_getAppSize = 9;
        static final int TRANSACTION_getExternalSize = 11;
        static final int TRANSACTION_getUserSize = 10;
        static final int TRANSACTION_hashSecondaryDexFile = 36;
        static final int TRANSACTION_idmap = 23;
        static final int TRANSACTION_installApkVerity = 33;
        static final int TRANSACTION_invalidateMounts = 37;
        static final int TRANSACTION_isQuotaSupported = 38;
        static final int TRANSACTION_linkFile = 30;
        static final int TRANSACTION_linkNativeLibraryDirectory = 28;
        static final int TRANSACTION_markBootComplete = 26;
        static final int TRANSACTION_mergeProfiles = 16;
        static final int TRANSACTION_migrateAppData = 5;
        static final int TRANSACTION_moveAb = 31;
        static final int TRANSACTION_moveCompleteApp = 13;
        static final int TRANSACTION_prepareAppProfile = 39;
        static final int TRANSACTION_reconcileSecondaryDexFile = 35;
        static final int TRANSACTION_removeIdmap = 24;
        static final int TRANSACTION_restoreconAppData = 4;
        static final int TRANSACTION_rmPackageDir = 25;
        static final int TRANSACTION_rmdex = 15;
        static final int TRANSACTION_setAppQuota = 12;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IInstalld asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IInstalld)) {
                return (IInstalld) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            boolean z;
            if (i != 1598968902) {
                switch (i) {
                    case 1:
                        parcel.enforceInterface(DESCRIPTOR);
                        createUserData(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                        parcel2.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(DESCRIPTOR);
                        destroyUserData(parcel.readString(), parcel.readInt(), parcel.readInt());
                        parcel2.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(DESCRIPTOR);
                        long jCreateAppData = createAppData(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeLong(jCreateAppData);
                        return true;
                    case 4:
                        parcel.enforceInterface(DESCRIPTOR);
                        restoreconAppData(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(DESCRIPTOR);
                        migrateAppData(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt());
                        parcel2.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(DESCRIPTOR);
                        clearAppData(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readLong());
                        parcel2.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(DESCRIPTOR);
                        destroyAppData(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readLong());
                        parcel2.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(DESCRIPTOR);
                        fixupAppData(parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(DESCRIPTOR);
                        long[] appSize = getAppSize(parcel.readString(), parcel.createStringArray(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createLongArray(), parcel.createStringArray());
                        parcel2.writeNoException();
                        parcel2.writeLongArray(appSize);
                        return true;
                    case 10:
                        parcel.enforceInterface(DESCRIPTOR);
                        long[] userSize = getUserSize(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.createIntArray());
                        parcel2.writeNoException();
                        parcel2.writeLongArray(userSize);
                        return true;
                    case 11:
                        parcel.enforceInterface(DESCRIPTOR);
                        long[] externalSize = getExternalSize(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.createIntArray());
                        parcel2.writeNoException();
                        parcel2.writeLongArray(externalSize);
                        return true;
                    case 12:
                        parcel.enforceInterface(DESCRIPTOR);
                        setAppQuota(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readLong());
                        parcel2.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(DESCRIPTOR);
                        moveCompleteApp(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(DESCRIPTOR);
                        String string = parcel.readString();
                        int i3 = parcel.readInt();
                        String string2 = parcel.readString();
                        String string3 = parcel.readString();
                        int i4 = parcel.readInt();
                        String string4 = parcel.readString();
                        int i5 = parcel.readInt();
                        String string5 = parcel.readString();
                        String string6 = parcel.readString();
                        String string7 = parcel.readString();
                        String string8 = parcel.readString();
                        if (parcel.readInt() == 0) {
                            z = false;
                        } else {
                            z = true;
                        }
                        dexopt(string, i3, string2, string3, i4, string4, i5, string5, string6, string7, string8, z, parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 15:
                        parcel.enforceInterface(DESCRIPTOR);
                        rmdex(parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean zMergeProfiles = mergeProfiles(parcel.readInt(), parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(zMergeProfiles ? 1 : 0);
                        return true;
                    case 17:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean zDumpProfiles = dumpProfiles(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(zDumpProfiles ? 1 : 0);
                        return true;
                    case 18:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean zCopySystemProfile = copySystemProfile(parcel.readString(), parcel.readInt(), parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(zCopySystemProfile ? 1 : 0);
                        return true;
                    case 19:
                        parcel.enforceInterface(DESCRIPTOR);
                        clearAppProfiles(parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(DESCRIPTOR);
                        destroyAppProfiles(parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean zCreateProfileSnapshot = createProfileSnapshot(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(zCreateProfileSnapshot ? 1 : 0);
                        return true;
                    case 22:
                        parcel.enforceInterface(DESCRIPTOR);
                        destroyProfileSnapshot(parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(DESCRIPTOR);
                        idmap(parcel.readString(), parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(DESCRIPTOR);
                        removeIdmap(parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(DESCRIPTOR);
                        rmPackageDir(parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 26:
                        parcel.enforceInterface(DESCRIPTOR);
                        markBootComplete(parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case TRANSACTION_freeCache:
                        parcel.enforceInterface(DESCRIPTOR);
                        freeCache(parcel.readString(), parcel.readLong(), parcel.readLong(), parcel.readInt());
                        parcel2.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(DESCRIPTOR);
                        linkNativeLibraryDirectory(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(DESCRIPTOR);
                        createOatDir(parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 30:
                        parcel.enforceInterface(DESCRIPTOR);
                        linkFile(parcel.readString(), parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 31:
                        parcel.enforceInterface(DESCRIPTOR);
                        moveAb(parcel.readString(), parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 32:
                        parcel.enforceInterface(DESCRIPTOR);
                        deleteOdex(parcel.readString(), parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(DESCRIPTOR);
                        installApkVerity(parcel.readString(), parcel.readRawFileDescriptor(), parcel.readInt());
                        parcel2.writeNoException();
                        return true;
                    case 34:
                        parcel.enforceInterface(DESCRIPTOR);
                        assertFsverityRootHashMatches(parcel.readString(), parcel.createByteArray());
                        parcel2.writeNoException();
                        return true;
                    case 35:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean zReconcileSecondaryDexFile = reconcileSecondaryDexFile(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.createStringArray(), parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(zReconcileSecondaryDexFile ? 1 : 0);
                        return true;
                    case 36:
                        parcel.enforceInterface(DESCRIPTOR);
                        byte[] bArrHashSecondaryDexFile = hashSecondaryDexFile(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeByteArray(bArrHashSecondaryDexFile);
                        return true;
                    case 37:
                        parcel.enforceInterface(DESCRIPTOR);
                        invalidateMounts();
                        parcel2.writeNoException();
                        return true;
                    case 38:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean zIsQuotaSupported = isQuotaSupported(parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(zIsQuotaSupported ? 1 : 0);
                        return true;
                    case 39:
                        parcel.enforceInterface(DESCRIPTOR);
                        boolean zPrepareAppProfile = prepareAppProfile(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(zPrepareAppProfile ? 1 : 0);
                        return true;
                    default:
                        return super.onTransact(i, parcel, parcel2, i2);
                }
            }
            parcel2.writeString(DESCRIPTOR);
            return true;
        }

        private static class Proxy implements IInstalld {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void createUserData(String str, int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void destroyUserData(String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long createAppData(String str, String str2, int i, int i2, int i3, String str3, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void restoreconAppData(String str, String str2, int i, int i2, int i3, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void migrateAppData(String str, String str2, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearAppData(String str, String str2, int i, int i2, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void destroyAppData(String str, String str2, int i, int i2, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void fixupAppData(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long[] getAppSize(String str, String[] strArr, int i, int i2, int i3, long[] jArr, String[] strArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeLongArray(jArr);
                    parcelObtain.writeStringArray(strArr2);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createLongArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long[] getUserSize(String str, int i, int i2, int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createLongArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long[] getExternalSize(String str, int i, int i2, int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createLongArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAppQuota(String str, int i, int i2, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void moveCompleteApp(String str, String str2, String str3, String str4, int i, String str5, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeString(str4);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str5);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void dexopt(String str, int i, String str2, String str3, int i2, String str4, int i3, String str5, String str6, String str7, String str8, boolean z, int i4, String str9, String str10, String str11) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str4);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str5);
                    parcelObtain.writeString(str6);
                    parcelObtain.writeString(str7);
                    parcelObtain.writeString(str8);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeString(str9);
                    parcelObtain.writeString(str10);
                    parcelObtain.writeString(str11);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void rmdex(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean mergeProfiles(int i, String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean dumpProfiles(int i, String str, String str2, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean copySystemProfile(String str, int i, String str2, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearAppProfiles(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void destroyAppProfiles(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean createProfileSnapshot(int i, String str, String str2, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void destroyProfileSnapshot(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void idmap(String str, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeIdmap(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void rmPackageDir(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void markBootComplete(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void freeCache(String str, long j, long j2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeLong(j2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_freeCache, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void linkNativeLibraryDirectory(String str, String str2, String str3, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void createOatDir(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void linkFile(String str, String str2, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void moveAb(String str, String str2, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void deleteOdex(String str, String str2, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void installApkVerity(String str, FileDescriptor fileDescriptor, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeRawFileDescriptor(fileDescriptor);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void assertFsverityRootHashMatches(String str, byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean reconcileSecondaryDexFile(String str, String str2, int i, String[] strArr, String str3, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(35, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] hashSecondaryDexFile(String str, String str2, int i, String str3, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(36, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void invalidateMounts() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isQuotaSupported(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(38, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean prepareAppProfile(String str, int i, int i2, String str2, String str3, String str4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeString(str4);
                    this.mRemote.transact(39, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}

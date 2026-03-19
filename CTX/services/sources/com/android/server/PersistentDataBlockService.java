package com.android.server;

import android.R;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.persistentdata.IPersistentDataBlockService;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import com.android.server.pm.DumpState;
import com.android.server.slice.SliceClientPermissions;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class PersistentDataBlockService extends SystemService {
    public static final int DIGEST_SIZE_BYTES = 32;
    private static final String FLASH_LOCK_LOCKED = "1";
    private static final String FLASH_LOCK_PROP = "ro.boot.flash.locked";
    private static final String FLASH_LOCK_UNLOCKED = "0";
    private static final int FRP_CREDENTIAL_RESERVED_SIZE = 1000;
    private static final int HEADER_SIZE = 8;
    private static final int MAX_DATA_BLOCK_SIZE = 102400;
    private static final int MAX_FRP_CREDENTIAL_HANDLE_SIZE = 996;
    private static final String OEM_UNLOCK_PROP = "sys.oem_unlock_allowed";
    private static final int PARTITION_TYPE_MARKER = 428873843;
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String TAG = PersistentDataBlockService.class.getSimpleName();
    private int mAllowedUid;
    private long mBlockDeviceSize;
    private final Context mContext;
    private final String mDataBlockFile;
    private final CountDownLatch mInitDoneSignal;
    private PersistentDataBlockManagerInternal mInternalService;

    @GuardedBy("mLock")
    private boolean mIsWritable;
    private final Object mLock;
    private final IBinder mService;

    private native long nativeGetBlockDeviceSize(String str);

    private native int nativeWipe(String str);

    public PersistentDataBlockService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mInitDoneSignal = new CountDownLatch(1);
        this.mAllowedUid = -1;
        this.mIsWritable = true;
        this.mService = new IPersistentDataBlockService.Stub() {
            public int write(byte[] bArr) throws RemoteException {
                PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
                long jDoGetMaximumDataBlockSize = PersistentDataBlockService.this.doGetMaximumDataBlockSize();
                if (bArr.length > jDoGetMaximumDataBlockSize) {
                    return (int) (-jDoGetMaximumDataBlockSize);
                }
                try {
                    DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                    ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArr.length + 8);
                    byteBufferAllocate.putInt(PersistentDataBlockService.PARTITION_TYPE_MARKER);
                    byteBufferAllocate.putInt(bArr.length);
                    byteBufferAllocate.put(bArr);
                    synchronized (PersistentDataBlockService.this.mLock) {
                        if (!PersistentDataBlockService.this.mIsWritable) {
                            return -1;
                        }
                        try {
                            try {
                                dataOutputStream.write(new byte[32], 0, 32);
                                dataOutputStream.write(byteBufferAllocate.array());
                                dataOutputStream.flush();
                                IoUtils.closeQuietly(dataOutputStream);
                                if (!PersistentDataBlockService.this.computeAndWriteDigestLocked()) {
                                    return -1;
                                }
                                return bArr.length;
                            } finally {
                                IoUtils.closeQuietly(dataOutputStream);
                            }
                        } catch (IOException e) {
                            Slog.e(PersistentDataBlockService.TAG, "failed writing to the persistent data block", e);
                            return -1;
                        }
                    }
                } catch (FileNotFoundException e2) {
                    Slog.e(PersistentDataBlockService.TAG, "partition not available?", e2);
                    return -1;
                }
            }

            public byte[] read() {
                PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
                if (!PersistentDataBlockService.this.enforceChecksumValidity()) {
                    return new byte[0];
                }
                try {
                    DataInputStream dataInputStream = new DataInputStream(new FileInputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                    try {
                        try {
                            synchronized (PersistentDataBlockService.this.mLock) {
                                int totalDataSizeLocked = PersistentDataBlockService.this.getTotalDataSizeLocked(dataInputStream);
                                if (totalDataSizeLocked == 0) {
                                    return new byte[0];
                                }
                                byte[] bArr = new byte[totalDataSizeLocked];
                                int i = dataInputStream.read(bArr, 0, totalDataSizeLocked);
                                if (i >= totalDataSizeLocked) {
                                    try {
                                        dataInputStream.close();
                                    } catch (IOException e) {
                                        Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                                    }
                                    return bArr;
                                }
                                Slog.e(PersistentDataBlockService.TAG, "failed to read entire data block. bytes read: " + i + SliceClientPermissions.SliceAuthority.DELIMITER + totalDataSizeLocked);
                                try {
                                    dataInputStream.close();
                                } catch (IOException e2) {
                                    Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                                }
                                return null;
                            }
                        } catch (IOException e3) {
                            Slog.e(PersistentDataBlockService.TAG, "failed to read data", e3);
                            try {
                                dataInputStream.close();
                            } catch (IOException e4) {
                                Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                            }
                            return null;
                        }
                    } finally {
                        try {
                            dataInputStream.close();
                        } catch (IOException e5) {
                            Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                        }
                    }
                } catch (FileNotFoundException e6) {
                    Slog.e(PersistentDataBlockService.TAG, "partition not available?", e6);
                    return null;
                }
            }

            public void wipe() {
                PersistentDataBlockService.this.enforceOemUnlockWritePermission();
                synchronized (PersistentDataBlockService.this.mLock) {
                    if (PersistentDataBlockService.this.nativeWipe(PersistentDataBlockService.this.mDataBlockFile) < 0) {
                        Slog.e(PersistentDataBlockService.TAG, "failed to wipe persistent partition");
                    } else {
                        PersistentDataBlockService.this.mIsWritable = false;
                        Slog.i(PersistentDataBlockService.TAG, "persistent partition now wiped and unwritable");
                    }
                }
            }

            public void setOemUnlockEnabled(boolean z) throws SecurityException {
                if (!ActivityManager.isUserAMonkey()) {
                    PersistentDataBlockService.this.enforceOemUnlockWritePermission();
                    PersistentDataBlockService.this.enforceIsAdmin();
                    if (z) {
                        PersistentDataBlockService.this.enforceUserRestriction("no_oem_unlock");
                        PersistentDataBlockService.this.enforceUserRestriction("no_factory_reset");
                    }
                    synchronized (PersistentDataBlockService.this.mLock) {
                        PersistentDataBlockService.this.doSetOemUnlockEnabledLocked(z);
                        PersistentDataBlockService.this.computeAndWriteDigestLocked();
                    }
                }
            }

            public boolean getOemUnlockEnabled() {
                PersistentDataBlockService.this.enforceOemUnlockReadPermission();
                return PersistentDataBlockService.this.doGetOemUnlockEnabled();
            }

            public int getFlashLockState() {
                PersistentDataBlockService.this.enforceOemUnlockReadPermission();
                switch (SystemProperties.get(PersistentDataBlockService.FLASH_LOCK_PROP)) {
                    case "1":
                        return 1;
                    case "0":
                        return 0;
                    default:
                        return -1;
                }
            }

            public int getDataBlockSize() {
                int totalDataSizeLocked;
                enforcePersistentDataBlockAccess();
                try {
                    DataInputStream dataInputStream = new DataInputStream(new FileInputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                    try {
                        synchronized (PersistentDataBlockService.this.mLock) {
                            totalDataSizeLocked = PersistentDataBlockService.this.getTotalDataSizeLocked(dataInputStream);
                        }
                        return totalDataSizeLocked;
                    } catch (IOException e) {
                        Slog.e(PersistentDataBlockService.TAG, "error reading data block size");
                        return 0;
                    } finally {
                        IoUtils.closeQuietly(dataInputStream);
                    }
                } catch (FileNotFoundException e2) {
                    Slog.e(PersistentDataBlockService.TAG, "partition not available");
                    return 0;
                }
            }

            private void enforcePersistentDataBlockAccess() {
                if (PersistentDataBlockService.this.mContext.checkCallingPermission("android.permission.ACCESS_PDB_STATE") != 0) {
                    PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
                }
            }

            public long getMaximumDataBlockSize() {
                PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
                return PersistentDataBlockService.this.doGetMaximumDataBlockSize();
            }

            public boolean hasFrpCredentialHandle() {
                enforcePersistentDataBlockAccess();
                try {
                    return PersistentDataBlockService.this.mInternalService.getFrpCredentialHandle() != null;
                } catch (IllegalStateException e) {
                    Slog.e(PersistentDataBlockService.TAG, "error reading frp handle", e);
                    throw new UnsupportedOperationException("cannot read frp credential");
                }
            }
        };
        this.mInternalService = new PersistentDataBlockManagerInternal() {
            @Override
            public void setFrpCredentialHandle(byte[] bArr) {
                boolean z = true;
                Preconditions.checkArgument(bArr == null || bArr.length > 0, "handle must be null or non-empty");
                if (bArr != null && bArr.length > PersistentDataBlockService.MAX_FRP_CREDENTIAL_HANDLE_SIZE) {
                    z = false;
                }
                Preconditions.checkArgument(z, "handle must not be longer than 996");
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(new File(PersistentDataBlockService.this.mDataBlockFile));
                    ByteBuffer byteBufferAllocate = ByteBuffer.allocate(1000);
                    byteBufferAllocate.putInt(bArr != null ? bArr.length : 0);
                    if (bArr != null) {
                        byteBufferAllocate.put(bArr);
                    }
                    byteBufferAllocate.flip();
                    synchronized (PersistentDataBlockService.this.mLock) {
                        if (PersistentDataBlockService.this.mIsWritable) {
                            try {
                                try {
                                    FileChannel channel = fileOutputStream.getChannel();
                                    channel.position((PersistentDataBlockService.this.getBlockDeviceSize() - 1) - 1000);
                                    channel.write(byteBufferAllocate);
                                    fileOutputStream.flush();
                                    IoUtils.closeQuietly(fileOutputStream);
                                    PersistentDataBlockService.this.computeAndWriteDigestLocked();
                                } catch (IOException e) {
                                    Slog.e(PersistentDataBlockService.TAG, "unable to access persistent partition", e);
                                }
                            } finally {
                                IoUtils.closeQuietly(fileOutputStream);
                            }
                        }
                    }
                } catch (FileNotFoundException e2) {
                    Slog.e(PersistentDataBlockService.TAG, "partition not available", e2);
                }
            }

            @Override
            public byte[] getFrpCredentialHandle() {
                ?? EnforceChecksumValidity = PersistentDataBlockService.this.enforceChecksumValidity();
                if (EnforceChecksumValidity == 0) {
                    throw new IllegalStateException("invalid checksum");
                }
                try {
                    try {
                        EnforceChecksumValidity = new DataInputStream(new FileInputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                        try {
                            synchronized (PersistentDataBlockService.this.mLock) {
                                EnforceChecksumValidity.skip((PersistentDataBlockService.this.getBlockDeviceSize() - 1) - 1000);
                                int i = EnforceChecksumValidity.readInt();
                                if (i > 0 && i <= PersistentDataBlockService.MAX_FRP_CREDENTIAL_HANDLE_SIZE) {
                                    byte[] bArr = new byte[i];
                                    EnforceChecksumValidity.readFully(bArr);
                                    return bArr;
                                }
                                return null;
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException("frp handle not readable", e);
                        }
                    } catch (FileNotFoundException e2) {
                        throw new IllegalStateException("frp partition not available");
                    }
                } finally {
                    IoUtils.closeQuietly((AutoCloseable) EnforceChecksumValidity);
                }
            }

            @Override
            public void forceOemUnlockEnabled(boolean z) {
                synchronized (PersistentDataBlockService.this.mLock) {
                    PersistentDataBlockService.this.doSetOemUnlockEnabledLocked(z);
                    PersistentDataBlockService.this.computeAndWriteDigestLocked();
                }
            }
        };
        this.mContext = context;
        this.mDataBlockFile = SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP);
        this.mBlockDeviceSize = -1L;
    }

    private int getAllowedUid(int i) {
        String string = this.mContext.getResources().getString(R.string.anr_process);
        try {
            return this.mContext.getPackageManager().getPackageUidAsUser(string, DumpState.DUMP_DEXOPT, i);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "not able to find package " + string, e);
            return -1;
        }
    }

    @Override
    public void onStart() {
        SystemServerInitThreadPool.get().submit(new Runnable() {
            @Override
            public final void run() {
                PersistentDataBlockService.lambda$onStart$0(this.f$0);
            }
        }, TAG + ".onStart");
    }

    public static void lambda$onStart$0(PersistentDataBlockService persistentDataBlockService) {
        persistentDataBlockService.mAllowedUid = persistentDataBlockService.getAllowedUid(0);
        persistentDataBlockService.enforceChecksumValidity();
        persistentDataBlockService.formatIfOemUnlockEnabled();
        persistentDataBlockService.publishBinderService("persistent_data_block", persistentDataBlockService.mService);
        persistentDataBlockService.mInitDoneSignal.countDown();
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            try {
                if (!this.mInitDoneSignal.await(10L, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Service " + TAG + " init timeout");
                }
                LocalServices.addService(PersistentDataBlockManagerInternal.class, this.mInternalService);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Service " + TAG + " init interrupted", e);
            }
        }
        super.onBootPhase(i);
    }

    private void formatIfOemUnlockEnabled() {
        boolean zDoGetOemUnlockEnabled = doGetOemUnlockEnabled();
        if (zDoGetOemUnlockEnabled) {
            synchronized (this.mLock) {
                formatPartitionLocked(true);
            }
        }
        SystemProperties.set(OEM_UNLOCK_PROP, zDoGetOemUnlockEnabled ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
    }

    private void enforceOemUnlockReadPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_OEM_UNLOCK_STATE") == -1 && this.mContext.checkCallingOrSelfPermission("android.permission.OEM_UNLOCK_STATE") == -1) {
            throw new SecurityException("Can't access OEM unlock state. Requires READ_OEM_UNLOCK_STATE or OEM_UNLOCK_STATE permission.");
        }
    }

    private void enforceOemUnlockWritePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.OEM_UNLOCK_STATE", "Can't modify OEM unlock state");
    }

    private void enforceUid(int i) {
        if (i != this.mAllowedUid) {
            throw new SecurityException("uid " + i + " not allowed to access PST");
        }
    }

    private void enforceIsAdmin() {
        if (!UserManager.get(this.mContext).isUserAdmin(UserHandle.getCallingUserId())) {
            throw new SecurityException("Only the Admin user is allowed to change OEM unlock state");
        }
    }

    private void enforceUserRestriction(String str) {
        if (UserManager.get(this.mContext).hasUserRestriction(str)) {
            throw new SecurityException("OEM unlock is disallowed by user restriction: " + str);
        }
    }

    private int getTotalDataSizeLocked(DataInputStream dataInputStream) throws IOException {
        dataInputStream.skipBytes(32);
        if (dataInputStream.readInt() == PARTITION_TYPE_MARKER) {
            return dataInputStream.readInt();
        }
        return 0;
    }

    private long getBlockDeviceSize() {
        synchronized (this.mLock) {
            if (this.mBlockDeviceSize == -1) {
                this.mBlockDeviceSize = nativeGetBlockDeviceSize(this.mDataBlockFile);
            }
        }
        return this.mBlockDeviceSize;
    }

    private boolean enforceChecksumValidity() {
        byte[] bArr = new byte[32];
        synchronized (this.mLock) {
            byte[] bArrComputeDigestLocked = computeDigestLocked(bArr);
            if (bArrComputeDigestLocked != null && Arrays.equals(bArr, bArrComputeDigestLocked)) {
                return true;
            }
            Slog.i(TAG, "Formatting FRP partition...");
            formatPartitionLocked(false);
            return false;
        }
    }

    private boolean computeAndWriteDigestLocked() {
        byte[] bArrComputeDigestLocked = computeDigestLocked(null);
        if (bArrComputeDigestLocked == null) {
            return false;
        }
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(this.mDataBlockFile)));
            try {
                try {
                    dataOutputStream.write(bArrComputeDigestLocked, 0, 32);
                    dataOutputStream.flush();
                    IoUtils.closeQuietly(dataOutputStream);
                    return true;
                } catch (IOException e) {
                    Slog.e(TAG, "failed to write block checksum", e);
                    IoUtils.closeQuietly(dataOutputStream);
                    return false;
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(dataOutputStream);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available?", e2);
            return false;
        }
    }

    private byte[] computeDigestLocked(byte[] bArr) {
        try {
            DataInputStream dataInputStream = new DataInputStream(new FileInputStream(new File(this.mDataBlockFile)));
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                if (bArr != null) {
                    try {
                        try {
                            if (bArr.length == 32) {
                                dataInputStream.read(bArr);
                            } else {
                                dataInputStream.skipBytes(32);
                            }
                        } catch (IOException e) {
                            Slog.e(TAG, "failed to read partition", e);
                            IoUtils.closeQuietly(dataInputStream);
                            return null;
                        }
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(dataInputStream);
                        throw th;
                    }
                }
                byte[] bArr2 = new byte[1024];
                messageDigest.update(bArr2, 0, 32);
                while (true) {
                    int i = dataInputStream.read(bArr2);
                    if (i == -1) {
                        IoUtils.closeQuietly(dataInputStream);
                        return messageDigest.digest();
                    }
                    messageDigest.update(bArr2, 0, i);
                }
            } catch (NoSuchAlgorithmException e2) {
                Slog.e(TAG, "SHA-256 not supported?", e2);
                IoUtils.closeQuietly(dataInputStream);
                return null;
            }
        } catch (FileNotFoundException e3) {
            Slog.e(TAG, "partition not available?", e3);
            return null;
        }
    }

    private void formatPartitionLocked(boolean z) {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(this.mDataBlockFile)));
            try {
                try {
                    dataOutputStream.write(new byte[32], 0, 32);
                    dataOutputStream.writeInt(PARTITION_TYPE_MARKER);
                    dataOutputStream.writeInt(0);
                    dataOutputStream.flush();
                    IoUtils.closeQuietly(dataOutputStream);
                    doSetOemUnlockEnabledLocked(z);
                    computeAndWriteDigestLocked();
                } catch (IOException e) {
                    Slog.e(TAG, "failed to format block", e);
                    IoUtils.closeQuietly(dataOutputStream);
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(dataOutputStream);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available?", e2);
        }
    }

    private void doSetOemUnlockEnabledLocked(boolean z) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(this.mDataBlockFile));
            try {
                try {
                    FileChannel channel = fileOutputStream.getChannel();
                    channel.position(getBlockDeviceSize() - 1);
                    ByteBuffer byteBufferAllocate = ByteBuffer.allocate(1);
                    byteBufferAllocate.put(z ? (byte) 1 : (byte) 0);
                    byteBufferAllocate.flip();
                    channel.write(byteBufferAllocate);
                    fileOutputStream.flush();
                    SystemProperties.set(OEM_UNLOCK_PROP, z ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
                    IoUtils.closeQuietly(fileOutputStream);
                } catch (IOException e) {
                    Slog.e(TAG, "unable to access persistent partition", e);
                    SystemProperties.set(OEM_UNLOCK_PROP, z ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
                    IoUtils.closeQuietly(fileOutputStream);
                }
            } catch (Throwable th) {
                SystemProperties.set(OEM_UNLOCK_PROP, z ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
                IoUtils.closeQuietly(fileOutputStream);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available", e2);
        }
    }

    private boolean doGetOemUnlockEnabled() {
        boolean z;
        try {
            DataInputStream dataInputStream = new DataInputStream(new FileInputStream(new File(this.mDataBlockFile)));
            try {
                synchronized (this.mLock) {
                    dataInputStream.skip(getBlockDeviceSize() - 1);
                    z = dataInputStream.readByte() != 0;
                }
                return z;
            } catch (IOException e) {
                Slog.e(TAG, "unable to access persistent partition", e);
                return false;
            } finally {
                IoUtils.closeQuietly(dataInputStream);
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available");
            return false;
        }
    }

    private long doGetMaximumDataBlockSize() {
        long blockDeviceSize = (((getBlockDeviceSize() - 8) - 32) - 1000) - 1;
        if (blockDeviceSize <= 102400) {
            return blockDeviceSize;
        }
        return 102400L;
    }
}

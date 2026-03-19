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
import com.android.server.pm.PackageManagerService;
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
import libcore.io.IoUtils;

public class PersistentDataBlockService extends SystemService {
    public static final int DIGEST_SIZE_BYTES = 32;
    private static final String FLASH_LOCK_LOCKED = "1";
    private static final String FLASH_LOCK_PROP = "ro.boot.flash.locked";
    private static final String FLASH_LOCK_UNLOCKED = "0";
    private static final int HEADER_SIZE = 8;
    private static final int MAX_DATA_BLOCK_SIZE = 102400;
    private static final String OEM_UNLOCK_PROP = "sys.oem_unlock_allowed";
    private static final int PARTITION_TYPE_MARKER = 428873843;
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String TAG = PersistentDataBlockService.class.getSimpleName();
    private int mAllowedUid;
    private long mBlockDeviceSize;
    private final Context mContext;
    private final String mDataBlockFile;
    private boolean mIsWritable;
    private final Object mLock;
    private final IBinder mService;

    private native long nativeGetBlockDeviceSize(String str);

    private native int nativeWipe(String str);

    public PersistentDataBlockService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mAllowedUid = -1;
        this.mIsWritable = true;
        this.mService = new IPersistentDataBlockService.Stub() {
            public int write(byte[] data) throws RemoteException {
                Slog.i(PersistentDataBlockService.TAG, "mService::write data");
                PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
                long maxBlockSize = (PersistentDataBlockService.this.getBlockDeviceSize() - 8) - 1;
                if (data.length > maxBlockSize) {
                    return (int) (-maxBlockSize);
                }
                try {
                    DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                    ByteBuffer headerAndData = ByteBuffer.allocate(data.length + 8);
                    headerAndData.putInt(PersistentDataBlockService.PARTITION_TYPE_MARKER);
                    headerAndData.putInt(data.length);
                    headerAndData.put(data);
                    synchronized (PersistentDataBlockService.this.mLock) {
                        if (!PersistentDataBlockService.this.mIsWritable) {
                            return -1;
                        }
                        try {
                            try {
                                byte[] checksum = new byte[32];
                                outputStream.write(checksum, 0, 32);
                                outputStream.write(headerAndData.array());
                                outputStream.flush();
                                Slog.i(PersistentDataBlockService.TAG, "mService::write flush");
                                IoUtils.closeQuietly(outputStream);
                                if (!PersistentDataBlockService.this.computeAndWriteDigestLocked()) {
                                    return -1;
                                }
                                Slog.i(PersistentDataBlockService.TAG, "mService::write return " + data.length);
                                return data.length;
                            } finally {
                                IoUtils.closeQuietly(outputStream);
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
                DataInputStream inputStream;
                Slog.i(PersistentDataBlockService.TAG, "mService::read");
                PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
                if (!PersistentDataBlockService.this.enforceChecksumValidity()) {
                    Slog.i(PersistentDataBlockService.TAG, "mService::read, enforceChecksumValidity is false, return byte[0]");
                    return new byte[0];
                }
                try {
                    try {
                        inputStream = new DataInputStream(new FileInputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                        try {
                            synchronized (PersistentDataBlockService.this.mLock) {
                                int totalDataSize = PersistentDataBlockService.this.getTotalDataSizeLocked(inputStream);
                                if (totalDataSize == 0) {
                                    Slog.i(PersistentDataBlockService.TAG, "mService::read, totalDataSize == 0, return byte[0]");
                                    return new byte[0];
                                }
                                byte[] data = new byte[totalDataSize];
                                int read = inputStream.read(data, 0, totalDataSize);
                                if (read < totalDataSize) {
                                    Slog.e(PersistentDataBlockService.TAG, "failed to read entire data block. bytes read: " + read + "/" + totalDataSize);
                                    try {
                                        inputStream.close();
                                    } catch (IOException e) {
                                        Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                                    }
                                    return null;
                                }
                                Slog.i(PersistentDataBlockService.TAG, "mService::read out");
                                try {
                                    inputStream.close();
                                } catch (IOException e2) {
                                    Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                                }
                                return data;
                            }
                        } catch (IOException e3) {
                            Slog.e(PersistentDataBlockService.TAG, "failed to read data", e3);
                            try {
                                inputStream.close();
                            } catch (IOException e4) {
                                Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                            }
                            return null;
                        }
                    } catch (FileNotFoundException e5) {
                        Slog.e(PersistentDataBlockService.TAG, "partition not available?", e5);
                        return null;
                    }
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e6) {
                        Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                    }
                }
            }

            public void wipe() {
                Slog.i(PersistentDataBlockService.TAG, "mService::wipe");
                PersistentDataBlockService.this.enforceOemUnlockWritePermission();
                synchronized (PersistentDataBlockService.this.mLock) {
                    int ret = PersistentDataBlockService.this.nativeWipe(PersistentDataBlockService.this.mDataBlockFile);
                    if (ret < 0) {
                        Slog.e(PersistentDataBlockService.TAG, "failed to wipe persistent partition");
                    } else {
                        PersistentDataBlockService.this.mIsWritable = false;
                        Slog.i(PersistentDataBlockService.TAG, "persistent partition now wiped and unwritable");
                    }
                }
            }

            public void setOemUnlockEnabled(boolean enabled) {
                Slog.i(PersistentDataBlockService.TAG, "mService::setOemUnlockEnabled, enabled=" + enabled);
                if (ActivityManager.isUserAMonkey()) {
                    return;
                }
                PersistentDataBlockService.this.enforceOemUnlockWritePermission();
                PersistentDataBlockService.this.enforceIsAdmin();
                synchronized (PersistentDataBlockService.this.mLock) {
                    PersistentDataBlockService.this.doSetOemUnlockEnabledLocked(enabled);
                    PersistentDataBlockService.this.computeAndWriteDigestLocked();
                }
            }

            public boolean getOemUnlockEnabled() {
                Slog.i(PersistentDataBlockService.TAG, "mService::getOemUnlockEnabled");
                PersistentDataBlockService.this.enforceOemUnlockReadPermission();
                return PersistentDataBlockService.this.doGetOemUnlockEnabled();
            }

            public int getFlashLockState() {
                PersistentDataBlockService.this.enforceOemUnlockReadPermission();
                String locked = SystemProperties.get(PersistentDataBlockService.FLASH_LOCK_PROP);
                Slog.i(PersistentDataBlockService.TAG, "getFlashLockState, ro.boot.flash.locked=" + locked);
                if (!locked.equals(PersistentDataBlockService.FLASH_LOCK_LOCKED)) {
                    if (locked.equals(PersistentDataBlockService.FLASH_LOCK_UNLOCKED)) {
                        return 0;
                    }
                    return -1;
                }
                return 1;
            }

            public int getDataBlockSize() {
                int totalDataSizeLocked;
                enforcePersistentDataBlockAccess();
                try {
                    DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                    try {
                        synchronized (PersistentDataBlockService.this.mLock) {
                            Slog.i(PersistentDataBlockService.TAG, "mService::getDataBlockSize, call getTotalDataSizeLocked");
                            totalDataSizeLocked = PersistentDataBlockService.this.getTotalDataSizeLocked(inputStream);
                        }
                        return totalDataSizeLocked;
                    } catch (IOException e) {
                        Slog.e(PersistentDataBlockService.TAG, "error reading data block size");
                        return 0;
                    } finally {
                        IoUtils.closeQuietly(inputStream);
                    }
                } catch (FileNotFoundException e2) {
                    Slog.e(PersistentDataBlockService.TAG, "partition not available");
                    return 0;
                }
            }

            private void enforcePersistentDataBlockAccess() {
                if (PersistentDataBlockService.this.mContext.checkCallingPermission("android.permission.ACCESS_PDB_STATE") == 0) {
                    return;
                }
                PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
            }

            public long getMaximumDataBlockSize() {
                long actualSize = (PersistentDataBlockService.this.getBlockDeviceSize() - 8) - 1;
                if (actualSize <= 102400) {
                    return actualSize;
                }
                return 102400L;
            }
        };
        Slog.i(TAG, "PersistentDataBlockService init");
        this.mContext = context;
        this.mDataBlockFile = SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP);
        this.mBlockDeviceSize = -1L;
        this.mAllowedUid = getAllowedUid(0);
        Slog.i(TAG, "PersistentDataBlockService, mDataBlockFile=" + this.mDataBlockFile + ", mAllowedUid=" + this.mAllowedUid);
    }

    private int getAllowedUid(int userHandle) {
        Slog.i(TAG, "getAllowedUid, userHandle=" + userHandle);
        String allowedPackage = this.mContext.getResources().getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_PUK_ENTRY);
        PackageManager pm = this.mContext.getPackageManager();
        int allowedUid = -1;
        try {
            allowedUid = pm.getPackageUidAsUser(allowedPackage, PackageManagerService.DumpState.DUMP_DEXOPT, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "not able to find package " + allowedPackage, e);
        }
        Slog.i(TAG, "getAllowedUid, allowedUid=" + allowedUid);
        return allowedUid;
    }

    @Override
    public void onStart() {
        Slog.i(TAG, "onStart");
        enforceChecksumValidity();
        formatIfOemUnlockEnabled();
        publishBinderService("persistent_data_block", this.mService);
    }

    private void formatIfOemUnlockEnabled() {
        Slog.i(TAG, "formatIfOemUnlockEnabled");
        boolean enabled = doGetOemUnlockEnabled();
        if (enabled) {
            synchronized (this.mLock) {
                formatPartitionLocked(true);
            }
        }
        SystemProperties.set(OEM_UNLOCK_PROP, enabled ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
    }

    private void enforceOemUnlockReadPermission() {
        Slog.i(TAG, "enforceOemUnlockReadPermission");
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_OEM_UNLOCK_STATE") != -1 || this.mContext.checkCallingOrSelfPermission("android.permission.OEM_UNLOCK_STATE") != -1) {
        } else {
            throw new SecurityException("Can't access OEM unlock state. Requires READ_OEM_UNLOCK_STATE or OEM_UNLOCK_STATE permission.");
        }
    }

    private void enforceOemUnlockWritePermission() {
        Slog.i(TAG, "enforceOemUnlockWritePermission");
        this.mContext.enforceCallingOrSelfPermission("android.permission.OEM_UNLOCK_STATE", "Can't modify OEM unlock state");
    }

    private void enforceUid(int callingUid) {
        if (callingUid == this.mAllowedUid) {
        } else {
            throw new SecurityException("uid " + callingUid + " not allowed to access PST");
        }
    }

    private void enforceIsAdmin() {
        int userId = UserHandle.getCallingUserId();
        boolean isAdmin = UserManager.get(this.mContext).isUserAdmin(userId);
        if (isAdmin) {
        } else {
            throw new SecurityException("Only the Admin user is allowed to change OEM unlock state");
        }
    }

    private int getTotalDataSizeLocked(DataInputStream inputStream) throws IOException {
        int totalDataSize;
        Slog.i(TAG, "getTotalDataSizeLocked");
        inputStream.skipBytes(32);
        int blockId = inputStream.readInt();
        Slog.i(TAG, "getTotalDataSizeLocked, blockId=" + blockId + ", PARTITION_TYPE_MARKER=" + PARTITION_TYPE_MARKER);
        if (blockId == PARTITION_TYPE_MARKER) {
            totalDataSize = inputStream.readInt();
        } else {
            totalDataSize = 0;
        }
        Slog.i(TAG, "getTotalDataSizeLocked, totalDataSize=" + totalDataSize);
        return totalDataSize;
    }

    private long getBlockDeviceSize() {
        Slog.i(TAG, "getBlockDeviceSize");
        synchronized (this.mLock) {
            if (this.mBlockDeviceSize == -1) {
                this.mBlockDeviceSize = nativeGetBlockDeviceSize(this.mDataBlockFile);
            }
        }
        Slog.i(TAG, "getBlockDeviceSize, mBlockDeviceSize=" + this.mBlockDeviceSize);
        return this.mBlockDeviceSize;
    }

    private boolean enforceChecksumValidity() {
        byte[] storedDigest = new byte[32];
        synchronized (this.mLock) {
            byte[] digest = computeDigestLocked(storedDigest);
            if (digest == null || !Arrays.equals(storedDigest, digest)) {
                Slog.i(TAG, "Formatting FRP partition...");
                formatPartitionLocked(false);
                Slog.i(TAG, "enforceChecksumValidity, return false");
                return false;
            }
            Slog.i(TAG, "enforceChecksumValidity, return true");
            return true;
        }
    }

    private boolean computeAndWriteDigestLocked() {
        Slog.i(TAG, "computeAndWriteDigestLocked");
        byte[] digest = computeDigestLocked(null);
        if (digest == null) {
            return false;
        }
        try {
            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(new File(this.mDataBlockFile)));
            try {
                try {
                    outputStream.write(digest, 0, 32);
                    outputStream.flush();
                    IoUtils.closeQuietly(outputStream);
                    Slog.i(TAG, "computeAndWriteDigestLocked, return true");
                    return true;
                } catch (IOException e) {
                    Slog.e(TAG, "failed to write block checksum", e);
                    IoUtils.closeQuietly(outputStream);
                    return false;
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(outputStream);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available?", e2);
            return false;
        }
    }

    private byte[] computeDigestLocked(byte[] storedDigest) {
        Slog.i(TAG, "computeDigestLocked in");
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(this.mDataBlockFile)));
            Slog.i(TAG, "computeDigestLocked, get MessageDigest isnstance");
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                if (storedDigest != null) {
                    try {
                        if (storedDigest.length == 32) {
                            inputStream.read(storedDigest);
                        } else {
                            inputStream.skipBytes(32);
                        }
                    } catch (IOException e) {
                        Slog.e(TAG, "failed to read partition", e);
                        return null;
                    } finally {
                        IoUtils.closeQuietly(inputStream);
                    }
                }
                Slog.i(TAG, "computeDigestLocked, start read partition");
                byte[] data = new byte[PackageManagerService.DumpState.DUMP_PREFERRED_XML];
                md.update(data, 0, 32);
                while (true) {
                    int read = inputStream.read(data);
                    if (read != -1) {
                        md.update(data, 0, read);
                    } else {
                        Slog.i(TAG, "computeDigestLocked, end read partition");
                        IoUtils.closeQuietly(inputStream);
                        byte[] returnValue = md.digest();
                        Slog.i(TAG, "computeDigestLocked out");
                        return returnValue;
                    }
                }
            } catch (NoSuchAlgorithmException e2) {
                Slog.e(TAG, "SHA-256 not supported?", e2);
                return null;
            }
        } catch (FileNotFoundException e3) {
            Slog.e(TAG, "partition not available?", e3);
            return null;
        }
    }

    private void formatPartitionLocked(boolean setOemUnlockEnabled) {
        Slog.i(TAG, "formatPartitionLocked");
        try {
            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(new File(this.mDataBlockFile)));
            byte[] data = new byte[32];
            try {
                try {
                    outputStream.write(data, 0, 32);
                    outputStream.writeInt(PARTITION_TYPE_MARKER);
                    outputStream.writeInt(0);
                    outputStream.flush();
                    Slog.i(TAG, "formatPartitionLocked init data, PARTITION_TYPE_MARKER=428873843, size=0 ");
                    IoUtils.closeQuietly(outputStream);
                    doSetOemUnlockEnabledLocked(setOemUnlockEnabled);
                    computeAndWriteDigestLocked();
                } catch (IOException e) {
                    Slog.e(TAG, "failed to format block", e);
                    IoUtils.closeQuietly(outputStream);
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(outputStream);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available?", e2);
        }
    }

    private void doSetOemUnlockEnabledLocked(boolean enabled) {
        FileOutputStream outputStream;
        Slog.i(TAG, "doSetOemUnlockEnabledLocked, enabled=" + enabled);
        try {
            try {
                outputStream = new FileOutputStream(new File(this.mDataBlockFile));
                try {
                    FileChannel channel = outputStream.getChannel();
                    channel.position(getBlockDeviceSize() - 1);
                    ByteBuffer data = ByteBuffer.allocate(1);
                    data.put(enabled ? (byte) 1 : (byte) 0);
                    data.flip();
                    channel.write(data);
                    outputStream.flush();
                    Slog.i(TAG, "doSetOemUnlockEnabledLocked out");
                    SystemProperties.set(OEM_UNLOCK_PROP, enabled ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
                    IoUtils.closeQuietly(outputStream);
                } catch (IOException e) {
                    Slog.e(TAG, "unable to access persistent partition", e);
                    SystemProperties.set(OEM_UNLOCK_PROP, enabled ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
                    IoUtils.closeQuietly(outputStream);
                }
            } catch (FileNotFoundException e2) {
                Slog.e(TAG, "partition not available", e2);
            }
        } catch (Throwable th) {
            SystemProperties.set(OEM_UNLOCK_PROP, enabled ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
            IoUtils.closeQuietly(outputStream);
            throw th;
        }
    }

    private boolean doGetOemUnlockEnabled() {
        boolean returnValue;
        Slog.i(TAG, "doGetOemUnlockEnabled in");
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(this.mDataBlockFile)));
            try {
                synchronized (this.mLock) {
                    inputStream.skip(getBlockDeviceSize() - 1);
                    returnValue = inputStream.readByte() != 0;
                    Slog.i(TAG, "doGetOemUnlockEnabled, return " + returnValue);
                }
                return returnValue;
            } catch (IOException e) {
                Slog.e(TAG, "unable to access persistent partition", e);
                return false;
            } finally {
                IoUtils.closeQuietly(inputStream);
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available");
            return false;
        }
    }
}

package com.android.server;

import android.R;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.service.persistentdata.IPersistentDataBlockService;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
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
    private static final int HEADER_SIZE = 8;
    private static final int MAX_DATA_BLOCK_SIZE = 102400;
    private static final int PARTITION_TYPE_MARKER = 428873843;
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String TAG = PersistentDataBlockService.class.getSimpleName();
    private int mAllowedUid;
    private long mBlockDeviceSize;
    private final Context mContext;
    private final String mDataBlockFile;

    @GuardedBy("mLock")
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
                int length = -1;
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
                            byte[] checksum = new byte[32];
                            outputStream.write(checksum, 0, 32);
                            outputStream.write(headerAndData.array());
                            outputStream.flush();
                            IoUtils.closeQuietly(outputStream);
                            if (PersistentDataBlockService.this.computeAndWriteDigestLocked()) {
                                length = data.length;
                            }
                        } catch (IOException e) {
                            Slog.e(PersistentDataBlockService.TAG, "failed writing to the persistent data block", e);
                        } finally {
                            IoUtils.closeQuietly(outputStream);
                        }
                        return length;
                    }
                } catch (FileNotFoundException e2) {
                    Slog.e(PersistentDataBlockService.TAG, "partition not available?", e2);
                    return -1;
                }
            }

            public byte[] read() {
                byte[] data;
                String str = null;
                PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
                if (!PersistentDataBlockService.this.enforceChecksumValidity()) {
                    return new byte[0];
                }
                try {
                    DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                    try {
                        try {
                            synchronized (PersistentDataBlockService.this.mLock) {
                                int totalDataSize = PersistentDataBlockService.this.getTotalDataSizeLocked(inputStream);
                                if (totalDataSize == 0) {
                                    data = new byte[0];
                                } else {
                                    data = new byte[totalDataSize];
                                    int read = inputStream.read(data, 0, totalDataSize);
                                    if (read < totalDataSize) {
                                        Slog.e(PersistentDataBlockService.TAG, "failed to read entire data block. bytes read: " + read + "/" + totalDataSize);
                                        try {
                                            inputStream.close();
                                        } catch (IOException e) {
                                            Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                                        }
                                        data = null;
                                    } else {
                                        try {
                                            inputStream.close();
                                        } catch (IOException e2) {
                                            str = PersistentDataBlockService.TAG;
                                            Slog.e(str, "failed to close OutputStream");
                                        }
                                    }
                                }
                            }
                            return data;
                        } finally {
                            try {
                                inputStream.close();
                            } catch (IOException e3) {
                                Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                            }
                        }
                    } catch (IOException e4) {
                        Slog.e(PersistentDataBlockService.TAG, "failed to read data", e4);
                        try {
                            inputStream.close();
                        } catch (IOException e5) {
                            Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                        }
                        return str;
                    }
                } catch (FileNotFoundException e6) {
                    Slog.e(PersistentDataBlockService.TAG, "partition not available?", e6);
                    return null;
                }
            }

            public void wipe() {
                PersistentDataBlockService.this.enforceOemUnlockPermission();
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
                if (!ActivityManager.isUserAMonkey()) {
                    PersistentDataBlockService.this.enforceOemUnlockPermission();
                    PersistentDataBlockService.this.enforceIsOwner();
                    synchronized (PersistentDataBlockService.this.mLock) {
                        PersistentDataBlockService.this.doSetOemUnlockEnabledLocked(enabled);
                        PersistentDataBlockService.this.computeAndWriteDigestLocked();
                    }
                }
            }

            public boolean getOemUnlockEnabled() {
                PersistentDataBlockService.this.enforceOemUnlockPermission();
                return PersistentDataBlockService.this.doGetOemUnlockEnabled();
            }

            public int getDataBlockSize() {
                DataInputStream inputStream;
                int totalDataSizeLocked;
                if (PersistentDataBlockService.this.mContext.checkCallingPermission("android.permission.ACCESS_PDB_STATE") != 0) {
                    PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
                }
                try {
                    try {
                        inputStream = new DataInputStream(new FileInputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                        synchronized (PersistentDataBlockService.this.mLock) {
                            totalDataSizeLocked = PersistentDataBlockService.this.getTotalDataSizeLocked(inputStream);
                        }
                        return totalDataSizeLocked;
                    } catch (FileNotFoundException e) {
                        Slog.e(PersistentDataBlockService.TAG, "partition not available");
                        return 0;
                    }
                } catch (IOException e2) {
                    Slog.e(PersistentDataBlockService.TAG, "error reading data block size");
                    return 0;
                } finally {
                    IoUtils.closeQuietly(inputStream);
                }
            }

            public long getMaximumDataBlockSize() {
                long actualSize = (PersistentDataBlockService.this.getBlockDeviceSize() - 8) - 1;
                if (actualSize <= 102400) {
                    return actualSize;
                }
                return 102400L;
            }
        };
        this.mContext = context;
        this.mDataBlockFile = SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP);
        this.mBlockDeviceSize = -1L;
        this.mAllowedUid = getAllowedUid(0);
    }

    private int getAllowedUid(int userHandle) {
        String allowedPackage = this.mContext.getResources().getString(R.string.config_systemCallStreaming);
        PackageManager pm = this.mContext.getPackageManager();
        try {
            int allowedUid = pm.getPackageUid(allowedPackage, userHandle);
            return allowedUid;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "not able to find package " + allowedPackage, e);
            return -1;
        }
    }

    @Override
    public void onStart() {
        enforceChecksumValidity();
        formatIfOemUnlockEnabled();
        publishBinderService("persistent_data_block", this.mService);
    }

    private void formatIfOemUnlockEnabled() {
        if (doGetOemUnlockEnabled()) {
            synchronized (this.mLock) {
                formatPartitionLocked();
                doSetOemUnlockEnabledLocked(true);
            }
        }
    }

    private void enforceOemUnlockPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.OEM_UNLOCK_STATE", "Can't access OEM unlock state");
    }

    private void enforceUid(int callingUid) {
        if (callingUid != this.mAllowedUid) {
            throw new SecurityException("uid " + callingUid + " not allowed to access PST");
        }
    }

    private void enforceIsOwner() {
        if (!Binder.getCallingUserHandle().isOwner()) {
            throw new SecurityException("Only the Owner is allowed to change OEM unlock state");
        }
    }

    private int getTotalDataSizeLocked(DataInputStream inputStream) throws IOException {
        inputStream.skipBytes(32);
        int blockId = inputStream.readInt();
        if (blockId == PARTITION_TYPE_MARKER) {
            int totalDataSize = inputStream.readInt();
            return totalDataSize;
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
        byte[] storedDigest = new byte[32];
        synchronized (this.mLock) {
            byte[] digest = computeDigestLocked(storedDigest);
            if (digest == null || !Arrays.equals(storedDigest, digest)) {
                Slog.i(TAG, "Formatting FRP partition...");
                formatPartitionLocked();
                return false;
            }
            return true;
        }
    }

    private boolean computeAndWriteDigestLocked() {
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
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(this.mDataBlockFile)));
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
                byte[] data = new byte[1024];
                md.update(data, 0, 32);
                while (true) {
                    int read = inputStream.read(data);
                    if (read != -1) {
                        md.update(data, 0, read);
                    } else {
                        IoUtils.closeQuietly(inputStream);
                        return md.digest();
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

    private void formatPartitionLocked() {
        try {
            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(new File(this.mDataBlockFile)));
            byte[] data = new byte[32];
            try {
                try {
                    outputStream.write(data, 0, 32);
                    outputStream.writeInt(PARTITION_TYPE_MARKER);
                    outputStream.writeInt(0);
                    outputStream.flush();
                    IoUtils.closeQuietly(outputStream);
                    doSetOemUnlockEnabledLocked(false);
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
        try {
            try {
                outputStream = new FileOutputStream(new File(this.mDataBlockFile));
                FileChannel channel = outputStream.getChannel();
                channel.position(getBlockDeviceSize() - 1);
                ByteBuffer data = ByteBuffer.allocate(1);
                data.put(enabled ? (byte) 1 : (byte) 0);
                data.flip();
                channel.write(data);
                outputStream.flush();
            } catch (FileNotFoundException e) {
                Slog.e(TAG, "partition not available", e);
            }
        } catch (IOException e2) {
            Slog.e(TAG, "unable to access persistent partition", e2);
        } finally {
            IoUtils.closeQuietly(outputStream);
        }
    }

    private boolean doGetOemUnlockEnabled() {
        boolean z;
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(this.mDataBlockFile)));
            try {
                synchronized (this.mLock) {
                    inputStream.skip(getBlockDeviceSize() - 1);
                    z = inputStream.readByte() != 0;
                }
                return z;
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

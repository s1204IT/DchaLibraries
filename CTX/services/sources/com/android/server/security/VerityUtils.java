package com.android.server.security;

import android.os.SharedMemory;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Pair;
import android.util.Slog;
import android.util.apk.ApkSignatureVerifier;
import android.util.apk.ByteBufferFactory;
import android.util.apk.SignatureNotFoundException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public abstract class VerityUtils {
    private static final boolean DEBUG = false;
    private static final String TAG = "VerityUtils";

    public static SetupResult generateApkVeritySetupData(String str) throws Throwable {
        SharedMemory sharedMemory = null;
        try {
            try {
                byte[] verityRootHash = ApkSignatureVerifier.getVerityRootHash(str);
                if (verityRootHash == null) {
                    return SetupResult.skipped();
                }
                Pair<SharedMemory, Integer> pairGenerateApkVerityIntoSharedMemory = generateApkVerityIntoSharedMemory(str, verityRootHash);
                SharedMemory sharedMemory2 = (SharedMemory) pairGenerateApkVerityIntoSharedMemory.first;
                try {
                    try {
                        int iIntValue = ((Integer) pairGenerateApkVerityIntoSharedMemory.second).intValue();
                        FileDescriptor fileDescriptor = sharedMemory2.getFileDescriptor();
                        if (fileDescriptor != null && fileDescriptor.valid()) {
                            SetupResult setupResultOk = SetupResult.ok(Os.dup(fileDescriptor), iIntValue);
                            if (sharedMemory2 != null) {
                                sharedMemory2.close();
                            }
                            return setupResultOk;
                        }
                        SetupResult setupResultFailed = SetupResult.failed();
                        if (sharedMemory2 != null) {
                            sharedMemory2.close();
                        }
                        return setupResultFailed;
                    } catch (Throwable th) {
                        th = th;
                        sharedMemory = sharedMemory2;
                        if (sharedMemory != null) {
                            sharedMemory.close();
                        }
                        throw th;
                    }
                } catch (IOException | SecurityException | DigestException | NoSuchAlgorithmException | SignatureNotFoundException | ErrnoException e) {
                    e = e;
                    sharedMemory = sharedMemory2;
                    Slog.e(TAG, "Failed to set up apk verity: ", e);
                    SetupResult setupResultFailed2 = SetupResult.failed();
                    if (sharedMemory != null) {
                        sharedMemory.close();
                    }
                    return setupResultFailed2;
                }
            } catch (IOException | SecurityException | DigestException | NoSuchAlgorithmException | SignatureNotFoundException | ErrnoException e2) {
                e = e2;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public static byte[] generateFsverityRootHash(String str) throws NoSuchAlgorithmException, DigestException, IOException {
        return ApkSignatureVerifier.generateFsverityRootHash(str);
    }

    public static byte[] getVerityRootHash(String str) throws SignatureNotFoundException, IOException, SecurityException {
        return ApkSignatureVerifier.getVerityRootHash(str);
    }

    private static Pair<SharedMemory, Integer> generateApkVerityIntoSharedMemory(String str, byte[] bArr) throws SignatureNotFoundException, NoSuchAlgorithmException, DigestException, IOException, SecurityException {
        TrackedShmBufferFactory trackedShmBufferFactory = new TrackedShmBufferFactory();
        if (!Arrays.equals(bArr, ApkSignatureVerifier.generateApkVerity(str, trackedShmBufferFactory))) {
            throw new SecurityException("Locally generated verity root hash does not match");
        }
        int bufferLimit = trackedShmBufferFactory.getBufferLimit();
        SharedMemory sharedMemoryReleaseSharedMemory = trackedShmBufferFactory.releaseSharedMemory();
        if (sharedMemoryReleaseSharedMemory == null) {
            throw new IllegalStateException("Failed to generate verity tree into shared memory");
        }
        if (!sharedMemoryReleaseSharedMemory.setProtect(OsConstants.PROT_READ)) {
            throw new SecurityException("Failed to set up shared memory correctly");
        }
        return Pair.create(sharedMemoryReleaseSharedMemory, Integer.valueOf(bufferLimit));
    }

    public static class SetupResult {
        private static final int RESULT_FAILED = 3;
        private static final int RESULT_OK = 1;
        private static final int RESULT_SKIPPED = 2;
        private final int mCode;
        private final int mContentSize;
        private final FileDescriptor mFileDescriptor;

        public static SetupResult ok(FileDescriptor fileDescriptor, int i) {
            return new SetupResult(1, fileDescriptor, i);
        }

        public static SetupResult skipped() {
            return new SetupResult(2, null, -1);
        }

        public static SetupResult failed() {
            return new SetupResult(3, null, -1);
        }

        private SetupResult(int i, FileDescriptor fileDescriptor, int i2) {
            this.mCode = i;
            this.mFileDescriptor = fileDescriptor;
            this.mContentSize = i2;
        }

        public boolean isFailed() {
            return this.mCode == 3;
        }

        public boolean isOk() {
            return this.mCode == 1;
        }

        public FileDescriptor getUnownedFileDescriptor() {
            return this.mFileDescriptor;
        }

        public int getContentSize() {
            return this.mContentSize;
        }
    }

    private static class TrackedShmBufferFactory implements ByteBufferFactory {
        private ByteBuffer mBuffer;
        private SharedMemory mShm;

        private TrackedShmBufferFactory() {
        }

        public ByteBuffer create(int i) throws SecurityException {
            try {
                if (this.mBuffer != null) {
                    throw new IllegalStateException("Multiple instantiation from this factory");
                }
                this.mShm = SharedMemory.create("apkverity", i);
                if (!this.mShm.setProtect(OsConstants.PROT_READ | OsConstants.PROT_WRITE)) {
                    throw new SecurityException("Failed to set protection");
                }
                this.mBuffer = this.mShm.mapReadWrite();
                return this.mBuffer;
            } catch (ErrnoException e) {
                throw new SecurityException("Failed to set protection", e);
            }
        }

        public SharedMemory releaseSharedMemory() {
            if (this.mBuffer != null) {
                SharedMemory.unmap(this.mBuffer);
                this.mBuffer = null;
            }
            SharedMemory sharedMemory = this.mShm;
            this.mShm = null;
            return sharedMemory;
        }

        public int getBufferLimit() {
            if (this.mBuffer == null) {
                return -1;
            }
            return this.mBuffer.limit();
        }
    }
}

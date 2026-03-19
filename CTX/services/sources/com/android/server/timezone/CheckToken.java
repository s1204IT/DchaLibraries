package com.android.server.timezone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class CheckToken {
    final int mOptimisticLockId;
    final PackageVersions mPackageVersions;

    CheckToken(int i, PackageVersions packageVersions) {
        this.mOptimisticLockId = i;
        if (packageVersions == null) {
            throw new NullPointerException("packageVersions == null");
        }
        this.mPackageVersions = packageVersions;
    }

    byte[] toByteArray() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(12);
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            Throwable th = null;
            try {
                try {
                    dataOutputStream.writeInt(this.mOptimisticLockId);
                    dataOutputStream.writeLong(this.mPackageVersions.mUpdateAppVersion);
                    dataOutputStream.writeLong(this.mPackageVersions.mDataAppVersion);
                    return byteArrayOutputStream.toByteArray();
                } finally {
                }
            } finally {
                $closeResource(th, dataOutputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to write into a ByteArrayOutputStream", e);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    static CheckToken fromByteArray(byte[] bArr) throws Exception {
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
        try {
            return new CheckToken(dataInputStream.readInt(), new PackageVersions(dataInputStream.readLong(), dataInputStream.readLong()));
        } finally {
            $closeResource(null, dataInputStream);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CheckToken checkToken = (CheckToken) obj;
        if (this.mOptimisticLockId != checkToken.mOptimisticLockId) {
            return false;
        }
        return this.mPackageVersions.equals(checkToken.mPackageVersions);
    }

    public int hashCode() {
        return (31 * this.mOptimisticLockId) + this.mPackageVersions.hashCode();
    }

    public String toString() {
        return "Token{mOptimisticLockId=" + this.mOptimisticLockId + ", mPackageVersions=" + this.mPackageVersions + '}';
    }
}

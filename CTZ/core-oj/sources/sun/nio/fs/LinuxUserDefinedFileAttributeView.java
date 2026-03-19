package sun.nio.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

class LinuxUserDefinedFileAttributeView extends AbstractUserDefinedFileAttributeView {
    static final boolean $assertionsDisabled = false;
    private static final String USER_NAMESPACE = "user.";
    private static final int XATTR_NAME_MAX = 255;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private final UnixPath file;
    private final boolean followLinks;

    private byte[] nameAsBytes(UnixPath unixPath, String str) throws IOException {
        if (str == null) {
            throw new NullPointerException("'name' is null");
        }
        String str2 = USER_NAMESPACE + str;
        byte[] bytes = Util.toBytes(str2);
        if (bytes.length > XATTR_NAME_MAX) {
            throw new FileSystemException(unixPath.getPathForExceptionMessage(), null, "'" + str2 + "' is too big");
        }
        return bytes;
    }

    private List<String> asList(long j, int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            if (unsafe.getByte(((long) i3) + j) == 0) {
                int i4 = i3 - i2;
                byte[] bArr = new byte[i4];
                for (int i5 = 0; i5 < i4; i5++) {
                    bArr[i5] = unsafe.getByte(((long) i2) + j + ((long) i5));
                }
                String string = Util.toString(bArr);
                if (string.startsWith(USER_NAMESPACE)) {
                    arrayList.add(string.substring(USER_NAMESPACE.length()));
                }
                i2 = i3 + 1;
            }
        }
        return arrayList;
    }

    LinuxUserDefinedFileAttributeView(UnixPath unixPath, boolean z) {
        this.file = unixPath;
        this.followLinks = z;
    }

    @Override
    public List<String> list() throws Throwable {
        NativeBuffer nativeBuffer;
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), true, $assertionsDisabled);
        }
        int iOpenForAttributeAccess = this.file.openForAttributeAccess(this.followLinks);
        int i = 1024;
        try {
            nativeBuffer = NativeBuffers.getNativeBuffer(1024);
            while (true) {
                try {
                    try {
                        List<String> listUnmodifiableList = Collections.unmodifiableList(asList(nativeBuffer.address(), LinuxNativeDispatcher.flistxattr(iOpenForAttributeAccess, nativeBuffer.address(), i)));
                        if (nativeBuffer != null) {
                            nativeBuffer.release();
                        }
                        LinuxNativeDispatcher.close(iOpenForAttributeAccess);
                        return listUnmodifiableList;
                    } catch (UnixException e) {
                        if (e.errno() == UnixConstants.ERANGE && i < 32768) {
                            nativeBuffer.release();
                            i *= 2;
                            nativeBuffer = NativeBuffers.getNativeBuffer(i);
                        } else {
                            throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Unable to get list of extended attributes: " + e.getMessage());
                        }
                        if (nativeBuffer != null) {
                            nativeBuffer.release();
                        }
                        LinuxNativeDispatcher.close(iOpenForAttributeAccess);
                        throw th;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (nativeBuffer != null) {
                    }
                    LinuxNativeDispatcher.close(iOpenForAttributeAccess);
                    throw th;
                }
            }
        } catch (Throwable th2) {
            th = th2;
            nativeBuffer = null;
            if (nativeBuffer != null) {
            }
            LinuxNativeDispatcher.close(iOpenForAttributeAccess);
            throw th;
        }
    }

    @Override
    public int size(String str) throws IOException {
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), true, $assertionsDisabled);
        }
        int iOpenForAttributeAccess = this.file.openForAttributeAccess(this.followLinks);
        try {
            try {
                return LinuxNativeDispatcher.fgetxattr(iOpenForAttributeAccess, nameAsBytes(this.file, str), 0L, 0);
            } catch (UnixException e) {
                throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Unable to get size of extended attribute '" + str + "': " + e.getMessage());
            }
        } finally {
            LinuxNativeDispatcher.close(iOpenForAttributeAccess);
        }
    }

    @Override
    public int read(String str, ByteBuffer byteBuffer) throws IOException {
        NativeBuffer nativeBuffer;
        long jAddress;
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), true, $assertionsDisabled);
        }
        if (byteBuffer.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit();
        int i = iPosition <= iLimit ? iLimit - iPosition : 0;
        if (byteBuffer instanceof DirectBuffer) {
            jAddress = ((DirectBuffer) byteBuffer).address() + ((long) iPosition);
            nativeBuffer = null;
        } else {
            nativeBuffer = NativeBuffers.getNativeBuffer(i);
            jAddress = nativeBuffer.address();
        }
        int iOpenForAttributeAccess = this.file.openForAttributeAccess(this.followLinks);
        try {
            try {
                try {
                    int iFgetxattr = LinuxNativeDispatcher.fgetxattr(iOpenForAttributeAccess, nameAsBytes(this.file, str), jAddress, i);
                    if (i == 0) {
                        if (iFgetxattr <= 0) {
                            return 0;
                        }
                        throw new UnixException(UnixConstants.ERANGE);
                    }
                    if (nativeBuffer != null) {
                        for (int i2 = 0; i2 < iFgetxattr; i2++) {
                            byteBuffer.put(unsafe.getByte(((long) i2) + jAddress));
                        }
                    }
                    byteBuffer.position(iPosition + iFgetxattr);
                    if (nativeBuffer != null) {
                        nativeBuffer.release();
                    }
                    return iFgetxattr;
                } catch (UnixException e) {
                    String message = e.errno() == UnixConstants.ERANGE ? "Insufficient space in buffer" : e.getMessage();
                    throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Error reading extended attribute '" + str + "': " + message);
                }
            } finally {
                if (nativeBuffer != null) {
                    nativeBuffer.release();
                }
            }
        } finally {
            LinuxNativeDispatcher.close(iOpenForAttributeAccess);
        }
    }

    @Override
    public int write(String str, ByteBuffer byteBuffer) throws IOException {
        NativeBuffer nativeBuffer;
        long jAddress;
        int i = 0;
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), $assertionsDisabled, true);
        }
        int iPosition = byteBuffer.position();
        int iLimit = byteBuffer.limit();
        int i2 = iPosition <= iLimit ? iLimit - iPosition : 0;
        if (byteBuffer instanceof DirectBuffer) {
            jAddress = ((DirectBuffer) byteBuffer).address() + ((long) iPosition);
            nativeBuffer = null;
        } else {
            nativeBuffer = NativeBuffers.getNativeBuffer(i2);
            jAddress = nativeBuffer.address();
            if (byteBuffer.hasArray()) {
                while (i < i2) {
                    unsafe.putByte(((long) i) + jAddress, byteBuffer.get());
                    i++;
                }
            } else {
                byte[] bArr = new byte[i2];
                byteBuffer.get(bArr);
                byteBuffer.position(iPosition);
                while (i < i2) {
                    unsafe.putByte(((long) i) + jAddress, bArr[i]);
                    i++;
                }
            }
        }
        int iOpenForAttributeAccess = this.file.openForAttributeAccess(this.followLinks);
        try {
            try {
                try {
                    LinuxNativeDispatcher.fsetxattr(iOpenForAttributeAccess, nameAsBytes(this.file, str), jAddress, i2);
                    byteBuffer.position(iPosition + i2);
                    return i2;
                } catch (UnixException e) {
                    throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Error writing extended attribute '" + str + "': " + e.getMessage());
                }
            } finally {
                if (nativeBuffer != null) {
                    nativeBuffer.release();
                }
            }
        } finally {
            LinuxNativeDispatcher.close(iOpenForAttributeAccess);
        }
    }

    @Override
    public void delete(String str) throws IOException {
        if (System.getSecurityManager() != null) {
            checkAccess(this.file.getPathForPermissionCheck(), $assertionsDisabled, true);
        }
        int iOpenForAttributeAccess = this.file.openForAttributeAccess(this.followLinks);
        try {
            try {
                LinuxNativeDispatcher.fremovexattr(iOpenForAttributeAccess, nameAsBytes(this.file, str));
            } catch (UnixException e) {
                throw new FileSystemException(this.file.getPathForExceptionMessage(), null, "Unable to delete extended attribute '" + str + "': " + e.getMessage());
            }
        } finally {
            LinuxNativeDispatcher.close(iOpenForAttributeAccess);
        }
    }

    static void copyExtendedAttributes(int i, int i2) throws Throwable {
        NativeBuffer nativeBuffer;
        int i3;
        try {
            nativeBuffer = NativeBuffers.getNativeBuffer(1024);
            int i4 = 1024;
            while (true) {
                try {
                    try {
                        int iFlistxattr = LinuxNativeDispatcher.flistxattr(i, nativeBuffer.address(), i4);
                        break;
                    } catch (UnixException e) {
                        if (e.errno() == UnixConstants.ERANGE && i4 < 32768) {
                            nativeBuffer.release();
                            i4 *= 2;
                            nativeBuffer = NativeBuffers.getNativeBuffer(i4);
                        } else if (nativeBuffer == null) {
                        }
                        if (nativeBuffer != null) {
                        }
                        throw th;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (nativeBuffer != null) {
                        nativeBuffer.release();
                    }
                    throw th;
                }
            }
            if (nativeBuffer == null) {
                nativeBuffer.release();
                return;
            }
            return;
            int i5 = i3 + 1;
            i3++;
        } catch (Throwable th2) {
            th = th2;
            nativeBuffer = null;
        }
    }

    private static void copyExtendedAttribute(int i, byte[] bArr, int i2) throws UnixException {
        int iFgetxattr = LinuxNativeDispatcher.fgetxattr(i, bArr, 0L, 0);
        NativeBuffer nativeBuffer = NativeBuffers.getNativeBuffer(iFgetxattr);
        try {
            long jAddress = nativeBuffer.address();
            LinuxNativeDispatcher.fsetxattr(i2, bArr, jAddress, LinuxNativeDispatcher.fgetxattr(i, bArr, jAddress, iFgetxattr));
        } finally {
            nativeBuffer.release();
        }
    }
}

package sun.nio.fs;

import java.io.IOException;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Set;
import sun.misc.Unsafe;
import sun.nio.fs.AbstractBasicFileAttributeView;
import sun.nio.fs.UnixFileAttributeViews;

class LinuxDosFileAttributeView extends UnixFileAttributeViews.Basic implements DosFileAttributeView {
    private static final int DOS_XATTR_ARCHIVE = 32;
    private static final int DOS_XATTR_HIDDEN = 2;
    private static final int DOS_XATTR_READONLY = 1;
    private static final int DOS_XATTR_SYSTEM = 4;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final String DOS_XATTR_NAME = "user.DOSATTRIB";
    private static final byte[] DOS_XATTR_NAME_AS_BYTES = Util.toBytes(DOS_XATTR_NAME);
    private static final String READONLY_NAME = "readonly";
    private static final String ARCHIVE_NAME = "archive";
    private static final String SYSTEM_NAME = "system";
    private static final String HIDDEN_NAME = "hidden";
    private static final Set<String> dosAttributeNames = Util.newSet(basicAttributeNames, READONLY_NAME, ARCHIVE_NAME, SYSTEM_NAME, HIDDEN_NAME);

    LinuxDosFileAttributeView(UnixPath unixPath, boolean z) {
        super(unixPath, z);
    }

    @Override
    public String name() {
        return "dos";
    }

    @Override
    public void setAttribute(String str, Object obj) throws IOException {
        if (str.equals(READONLY_NAME)) {
            setReadOnly(((Boolean) obj).booleanValue());
            return;
        }
        if (str.equals(ARCHIVE_NAME)) {
            setArchive(((Boolean) obj).booleanValue());
            return;
        }
        if (str.equals(SYSTEM_NAME)) {
            setSystem(((Boolean) obj).booleanValue());
        } else if (str.equals(HIDDEN_NAME)) {
            setHidden(((Boolean) obj).booleanValue());
        } else {
            super.setAttribute(str, obj);
        }
    }

    @Override
    public Map<String, Object> readAttributes(String[] strArr) throws IOException {
        AbstractBasicFileAttributeView.AttributesBuilder attributesBuilderCreate = AbstractBasicFileAttributeView.AttributesBuilder.create(dosAttributeNames, strArr);
        DosFileAttributes attributes = readAttributes();
        addRequestedBasicAttributes(attributes, attributesBuilderCreate);
        if (attributesBuilderCreate.match(READONLY_NAME)) {
            attributesBuilderCreate.add(READONLY_NAME, Boolean.valueOf(attributes.isReadOnly()));
        }
        if (attributesBuilderCreate.match(ARCHIVE_NAME)) {
            attributesBuilderCreate.add(ARCHIVE_NAME, Boolean.valueOf(attributes.isArchive()));
        }
        if (attributesBuilderCreate.match(SYSTEM_NAME)) {
            attributesBuilderCreate.add(SYSTEM_NAME, Boolean.valueOf(attributes.isSystem()));
        }
        if (attributesBuilderCreate.match(HIDDEN_NAME)) {
            attributesBuilderCreate.add(HIDDEN_NAME, Boolean.valueOf(attributes.isHidden()));
        }
        return attributesBuilderCreate.unmodifiableMap();
    }

    @Override
    public DosFileAttributes readAttributes() throws IOException {
        this.file.checkRead();
        int iOpenForAttributeAccess = this.file.openForAttributeAccess(this.followLinks);
        try {
            final UnixFileAttributes unixFileAttributes = UnixFileAttributes.get(iOpenForAttributeAccess);
            final int dosAttribute = getDosAttribute(iOpenForAttributeAccess);
            return new DosFileAttributes() {
                @Override
                public FileTime lastModifiedTime() {
                    return unixFileAttributes.lastModifiedTime();
                }

                @Override
                public FileTime lastAccessTime() {
                    return unixFileAttributes.lastAccessTime();
                }

                @Override
                public FileTime creationTime() {
                    return unixFileAttributes.creationTime();
                }

                @Override
                public boolean isRegularFile() {
                    return unixFileAttributes.isRegularFile();
                }

                @Override
                public boolean isDirectory() {
                    return unixFileAttributes.isDirectory();
                }

                @Override
                public boolean isSymbolicLink() {
                    return unixFileAttributes.isSymbolicLink();
                }

                @Override
                public boolean isOther() {
                    return unixFileAttributes.isOther();
                }

                @Override
                public long size() {
                    return unixFileAttributes.size();
                }

                @Override
                public Object fileKey() {
                    return unixFileAttributes.fileKey();
                }

                @Override
                public boolean isReadOnly() {
                    return (dosAttribute & 1) != 0;
                }

                @Override
                public boolean isHidden() {
                    return (dosAttribute & 2) != 0;
                }

                @Override
                public boolean isArchive() {
                    return (dosAttribute & 32) != 0;
                }

                @Override
                public boolean isSystem() {
                    return (dosAttribute & 4) != 0;
                }
            };
        } catch (UnixException e) {
            e.rethrowAsIOException(this.file);
            return null;
        } finally {
            UnixNativeDispatcher.close(iOpenForAttributeAccess);
        }
    }

    @Override
    public void setReadOnly(boolean z) throws IOException {
        updateDosAttribute(1, z);
    }

    @Override
    public void setHidden(boolean z) throws IOException {
        updateDosAttribute(2, z);
    }

    @Override
    public void setArchive(boolean z) throws IOException {
        updateDosAttribute(32, z);
    }

    @Override
    public void setSystem(boolean z) throws IOException {
        updateDosAttribute(4, z);
    }

    private int getDosAttribute(int i) throws UnixException {
        NativeBuffer nativeBuffer = NativeBuffers.getNativeBuffer(24);
        try {
            int iFgetxattr = LinuxNativeDispatcher.fgetxattr(i, DOS_XATTR_NAME_AS_BYTES, nativeBuffer.address(), 24);
            if (iFgetxattr > 0) {
                if (unsafe.getByte((nativeBuffer.address() + ((long) iFgetxattr)) - 1) == 0) {
                    iFgetxattr--;
                }
                byte[] bArr = new byte[iFgetxattr];
                for (int i2 = 0; i2 < iFgetxattr; i2++) {
                    bArr[i2] = unsafe.getByte(nativeBuffer.address() + ((long) i2));
                }
                String string = Util.toString(bArr);
                if (string.length() >= 3 && string.startsWith("0x")) {
                    try {
                        return Integer.parseInt(string.substring(2), 16);
                    } catch (NumberFormatException e) {
                    }
                }
            }
            throw new UnixException("Value of user.DOSATTRIB attribute is invalid");
        } catch (UnixException e2) {
            if (e2.errno() == UnixConstants.ENODATA) {
                return 0;
            }
            throw e2;
        } finally {
            nativeBuffer.release();
        }
    }

    private void updateDosAttribute(int i, boolean z) throws IOException {
        int i2;
        this.file.checkWrite();
        int iOpenForAttributeAccess = this.file.openForAttributeAccess(this.followLinks);
        try {
            try {
                int dosAttribute = getDosAttribute(iOpenForAttributeAccess);
                if (z) {
                    i2 = i | dosAttribute;
                } else {
                    i2 = (~i) & dosAttribute;
                }
                if (i2 != dosAttribute) {
                    byte[] bytes = Util.toBytes("0x" + Integer.toHexString(i2));
                    NativeBuffer nativeBufferAsNativeBuffer = NativeBuffers.asNativeBuffer(bytes);
                    try {
                        LinuxNativeDispatcher.fsetxattr(iOpenForAttributeAccess, DOS_XATTR_NAME_AS_BYTES, nativeBufferAsNativeBuffer.address(), bytes.length + 1);
                        nativeBufferAsNativeBuffer.release();
                    } catch (Throwable th) {
                        nativeBufferAsNativeBuffer.release();
                        throw th;
                    }
                }
            } catch (UnixException e) {
                e.rethrowAsIOException(this.file);
            }
        } finally {
            UnixNativeDispatcher.close(iOpenForAttributeAccess);
        }
    }
}

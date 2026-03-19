package sun.nio.fs;

import java.io.IOException;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Arrays;
import sun.nio.fs.UnixFileStore;

class LinuxFileStore extends UnixFileStore {
    private volatile boolean xattrChecked;
    private volatile boolean xattrEnabled;

    LinuxFileStore(UnixPath unixPath) throws IOException {
        super(unixPath);
    }

    LinuxFileStore(UnixFileSystem unixFileSystem, UnixMountEntry unixMountEntry) throws IOException {
        super(unixFileSystem, unixMountEntry);
    }

    @Override
    UnixMountEntry findMountEntry() throws IOException {
        UnixPath unixPath;
        UnixPath unixPath2;
        UnixFileAttributes unixFileAttributes;
        LinuxFileSystem linuxFileSystem = (LinuxFileSystem) file().getFileSystem();
        try {
            unixPath = new UnixPath(linuxFileSystem, UnixNativeDispatcher.realpath(file()));
        } catch (UnixException e) {
            e.rethrowAsIOException(file());
            unixPath = null;
        }
        UnixPath parent = unixPath.getParent();
        while (true) {
            unixPath2 = unixPath;
            unixPath = parent;
            if (unixPath == null) {
                break;
            }
            try {
                unixFileAttributes = UnixFileAttributes.get(unixPath, true);
            } catch (UnixException e2) {
                e2.rethrowAsIOException(unixPath);
                unixFileAttributes = null;
            }
            if (unixFileAttributes.dev() != dev()) {
                break;
            }
            parent = unixPath.getParent();
        }
        byte[] bArrAsByteArray = unixPath2.asByteArray();
        for (UnixMountEntry unixMountEntry : linuxFileSystem.getMountEntries("/proc/mounts")) {
            if (Arrays.equals(bArrAsByteArray, unixMountEntry.dir())) {
                return unixMountEntry;
            }
        }
        throw new IOException("Mount point not found");
    }

    private boolean isExtendedAttributesEnabled(UnixPath unixPath) {
        try {
            int iOpenForAttributeAccess = unixPath.openForAttributeAccess(false);
            try {
                LinuxNativeDispatcher.fgetxattr(iOpenForAttributeAccess, Util.toBytes("user.java"), 0L, 0);
                return true;
            } catch (UnixException e) {
                if (e.errno() == UnixConstants.ENODATA) {
                    return true;
                }
                UnixNativeDispatcher.close(iOpenForAttributeAccess);
                return false;
            } finally {
                UnixNativeDispatcher.close(iOpenForAttributeAccess);
            }
        } catch (IOException e2) {
            return false;
        }
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> cls) {
        if (cls == DosFileAttributeView.class || cls == UserDefinedFileAttributeView.class) {
            UnixFileStore.FeatureStatus featureStatusCheckIfFeaturePresent = checkIfFeaturePresent("user_xattr");
            if (featureStatusCheckIfFeaturePresent == UnixFileStore.FeatureStatus.PRESENT) {
                return true;
            }
            if (featureStatusCheckIfFeaturePresent == UnixFileStore.FeatureStatus.NOT_PRESENT) {
                return false;
            }
            if (entry().hasOption("user_xattr")) {
                return true;
            }
            if (entry().fstype().equals("ext3") || entry().fstype().equals("ext4")) {
                return false;
            }
            if (!this.xattrChecked) {
                this.xattrEnabled = isExtendedAttributesEnabled(new UnixPath(file().getFileSystem(), entry().dir()));
                this.xattrChecked = true;
            }
            return this.xattrEnabled;
        }
        if (cls == PosixFileAttributeView.class && entry().fstype().equals("vfat")) {
            return false;
        }
        return super.supportsFileAttributeView(cls);
    }

    @Override
    public boolean supportsFileAttributeView(String str) {
        if (str.equals("dos")) {
            return supportsFileAttributeView(DosFileAttributeView.class);
        }
        if (str.equals("user")) {
            return supportsFileAttributeView(UserDefinedFileAttributeView.class);
        }
        return super.supportsFileAttributeView(str);
    }
}

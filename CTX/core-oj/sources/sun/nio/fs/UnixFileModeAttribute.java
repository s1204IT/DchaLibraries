package sun.nio.fs;

import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

class UnixFileModeAttribute {
    static final int ALL_PERMISSIONS = (((((((UnixConstants.S_IRUSR | UnixConstants.S_IWUSR) | UnixConstants.S_IXUSR) | UnixConstants.S_IRGRP) | UnixConstants.S_IWGRP) | UnixConstants.S_IXGRP) | UnixConstants.S_IROTH) | UnixConstants.S_IWOTH) | UnixConstants.S_IXOTH;
    static final int ALL_READWRITE = ((((UnixConstants.S_IRUSR | UnixConstants.S_IWUSR) | UnixConstants.S_IRGRP) | UnixConstants.S_IWGRP) | UnixConstants.S_IROTH) | UnixConstants.S_IWOTH;
    static final int TEMPFILE_PERMISSIONS = (UnixConstants.S_IRUSR | UnixConstants.S_IWUSR) | UnixConstants.S_IXUSR;

    private UnixFileModeAttribute() {
    }

    static int toUnixMode(Set<PosixFilePermission> set) {
        int i = 0;
        for (PosixFilePermission posixFilePermission : set) {
            if (posixFilePermission == null) {
                throw new NullPointerException();
            }
            switch (posixFilePermission) {
                case OWNER_READ:
                    i |= UnixConstants.S_IRUSR;
                    break;
                case OWNER_WRITE:
                    i |= UnixConstants.S_IWUSR;
                    break;
                case OWNER_EXECUTE:
                    i |= UnixConstants.S_IXUSR;
                    break;
                case GROUP_READ:
                    i |= UnixConstants.S_IRGRP;
                    break;
                case GROUP_WRITE:
                    i |= UnixConstants.S_IWGRP;
                    break;
                case GROUP_EXECUTE:
                    i |= UnixConstants.S_IXGRP;
                    break;
                case OTHERS_READ:
                    i |= UnixConstants.S_IROTH;
                    break;
                case OTHERS_WRITE:
                    i |= UnixConstants.S_IWOTH;
                    break;
                case OTHERS_EXECUTE:
                    i |= UnixConstants.S_IXOTH;
                    break;
            }
        }
        return i;
    }

    static int toUnixMode(int i, FileAttribute<?>... fileAttributeArr) {
        for (FileAttribute<?> fileAttribute : fileAttributeArr) {
            String strName = fileAttribute.name();
            if (!strName.equals("posix:permissions") && !strName.equals("unix:permissions")) {
                throw new UnsupportedOperationException("'" + fileAttribute.name() + "' not supported as initial attribute");
            }
            i = toUnixMode((Set) fileAttribute.value());
        }
        return i;
    }
}

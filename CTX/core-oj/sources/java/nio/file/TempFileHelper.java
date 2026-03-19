package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.AccessController;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.Set;
import sun.security.action.GetPropertyAction;

class TempFileHelper {
    private static final Path tmpdir = Paths.get((String) AccessController.doPrivileged(new GetPropertyAction("java.io.tmpdir")), new String[0]);
    private static final boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    private static final SecureRandom random = new SecureRandom();

    private TempFileHelper() {
    }

    private static Path generatePath(String str, String str2, Path path) {
        long jNextLong = random.nextLong();
        long jAbs = jNextLong == Long.MIN_VALUE ? 0L : Math.abs(jNextLong);
        Path path2 = path.getFileSystem().getPath(str + Long.toString(jAbs) + str2, new String[0]);
        if (path2.getParent() != null) {
            throw new IllegalArgumentException("Invalid prefix or suffix");
        }
        return path.resolve(path2);
    }

    private static class PosixPermissions {
        static final FileAttribute<Set<PosixFilePermission>> filePermissions = PosixFilePermissions.asFileAttribute(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        static final FileAttribute<Set<PosixFilePermission>> dirPermissions = PosixFilePermissions.asFileAttribute(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));

        private PosixPermissions() {
        }
    }

    private static Path create(Path path, String str, String str2, boolean z, FileAttribute<?>[] fileAttributeArr) throws IOException {
        boolean z2;
        FileAttribute<Set<PosixFilePermission>> fileAttribute;
        if (str == null) {
            str = "";
        }
        if (str2 == null) {
            str2 = z ? "" : ".tmp";
        }
        if (path == null) {
            path = tmpdir;
        }
        if (isPosix && path.getFileSystem() == FileSystems.getDefault()) {
            if (fileAttributeArr.length == 0) {
                fileAttributeArr = new FileAttribute[1];
                fileAttributeArr[0] = z ? PosixPermissions.dirPermissions : PosixPermissions.filePermissions;
            } else {
                int i = 0;
                while (true) {
                    if (i < fileAttributeArr.length) {
                        if (!fileAttributeArr[i].name().equals("posix:permissions")) {
                            i++;
                        } else {
                            z2 = true;
                            break;
                        }
                    } else {
                        z2 = false;
                        break;
                    }
                }
                if (!z2) {
                    FileAttribute<?>[] fileAttributeArr2 = new FileAttribute[fileAttributeArr.length + 1];
                    System.arraycopy(fileAttributeArr, 0, fileAttributeArr2, 0, fileAttributeArr.length);
                    int length = fileAttributeArr2.length - 1;
                    if (z) {
                        fileAttribute = PosixPermissions.dirPermissions;
                    } else {
                        fileAttribute = PosixPermissions.filePermissions;
                    }
                    fileAttributeArr2[length] = fileAttribute;
                    fileAttributeArr = fileAttributeArr2;
                }
            }
        }
        SecurityManager securityManager = System.getSecurityManager();
        while (true) {
            try {
                Path pathGeneratePath = generatePath(str, str2, path);
                try {
                    if (z) {
                        return Files.createDirectory(pathGeneratePath, fileAttributeArr);
                    }
                    return Files.createFile(pathGeneratePath, fileAttributeArr);
                } catch (SecurityException e) {
                    if (path == tmpdir && securityManager != null) {
                        throw new SecurityException("Unable to create temporary file or directory");
                    }
                    throw e;
                } catch (FileAlreadyExistsException e2) {
                }
            } catch (InvalidPathException e3) {
                if (securityManager != null) {
                    throw new IllegalArgumentException("Invalid prefix or suffix");
                }
                throw e3;
            }
        }
    }

    static Path createTempFile(Path path, String str, String str2, FileAttribute<?>[] fileAttributeArr) throws IOException {
        return create(path, str, str2, false, fileAttributeArr);
    }

    static Path createTempDirectory(Path path, String str, FileAttribute<?>[] fileAttributeArr) throws IOException {
        return create(path, str, null, true, fileAttributeArr);
    }
}

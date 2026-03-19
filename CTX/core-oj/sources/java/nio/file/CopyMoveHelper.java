package java.nio.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;

class CopyMoveHelper {
    private CopyMoveHelper() {
    }

    private static class CopyOptions {
        boolean replaceExisting = false;
        boolean copyAttributes = false;
        boolean followLinks = true;

        private CopyOptions() {
        }

        static CopyOptions parse(CopyOption... copyOptionArr) {
            CopyOptions copyOptions = new CopyOptions();
            for (CopyOption copyOption : copyOptionArr) {
                if (copyOption == StandardCopyOption.REPLACE_EXISTING) {
                    copyOptions.replaceExisting = true;
                } else if (copyOption == LinkOption.NOFOLLOW_LINKS) {
                    copyOptions.followLinks = false;
                } else if (copyOption == StandardCopyOption.COPY_ATTRIBUTES) {
                    copyOptions.copyAttributes = true;
                } else {
                    if (copyOption == null) {
                        throw new NullPointerException();
                    }
                    throw new UnsupportedOperationException("'" + ((Object) copyOption) + "' is not a recognized copy option");
                }
            }
            return copyOptions;
        }
    }

    private static CopyOption[] convertMoveToCopyOptions(CopyOption... copyOptionArr) throws AtomicMoveNotSupportedException {
        int length = copyOptionArr.length;
        CopyOption[] copyOptionArr2 = new CopyOption[length + 2];
        for (int i = 0; i < length; i++) {
            CopyOption copyOption = copyOptionArr[i];
            if (copyOption == StandardCopyOption.ATOMIC_MOVE) {
                throw new AtomicMoveNotSupportedException(null, null, "Atomic move between providers is not supported");
            }
            copyOptionArr2[i] = copyOption;
        }
        copyOptionArr2[length] = LinkOption.NOFOLLOW_LINKS;
        copyOptionArr2[length + 1] = StandardCopyOption.COPY_ATTRIBUTES;
        return copyOptionArr2;
    }

    static void copyToForeignTarget(Path path, Path path2, CopyOption... copyOptionArr) throws IOException {
        CopyOptions copyOptions = CopyOptions.parse(copyOptionArr);
        BasicFileAttributes attributes = Files.readAttributes(path, (Class<BasicFileAttributes>) BasicFileAttributes.class, copyOptions.followLinks ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
        if (attributes.isSymbolicLink()) {
            throw new IOException("Copying of symbolic links not supported");
        }
        if (copyOptions.replaceExisting) {
            Files.deleteIfExists(path2);
        } else if (Files.exists(path2, new LinkOption[0])) {
            throw new FileAlreadyExistsException(path2.toString());
        }
        if (attributes.isDirectory()) {
            Files.createDirectory(path2, new FileAttribute[0]);
        } else {
            InputStream inputStreamNewInputStream = Files.newInputStream(path, new OpenOption[0]);
            Throwable th = null;
            try {
                Files.copy(inputStreamNewInputStream, path2, new CopyOption[0]);
                if (inputStreamNewInputStream != null) {
                    inputStreamNewInputStream.close();
                }
            } catch (Throwable th2) {
                if (inputStreamNewInputStream != null) {
                    if (0 != 0) {
                        try {
                            inputStreamNewInputStream.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        inputStreamNewInputStream.close();
                    }
                }
                throw th2;
            }
        }
        if (copyOptions.copyAttributes) {
            try {
                ((BasicFileAttributeView) Files.getFileAttributeView(path2, BasicFileAttributeView.class, new LinkOption[0])).setTimes(attributes.lastModifiedTime(), attributes.lastAccessTime(), attributes.creationTime());
            } catch (Throwable th4) {
                try {
                    Files.delete(path2);
                } catch (Throwable th5) {
                    th4.addSuppressed(th5);
                }
                throw th4;
            }
        }
    }

    static void moveToForeignTarget(Path path, Path path2, CopyOption... copyOptionArr) throws IOException {
        copyToForeignTarget(path, path2, convertMoveToCopyOptions(copyOptionArr));
        Files.delete(path);
    }
}

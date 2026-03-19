package sun.nio.fs;

import java.io.IOException;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import sun.nio.fs.AbstractBasicFileAttributeView;
import sun.nio.fs.UnixUserPrincipals;

class UnixFileAttributeViews {
    UnixFileAttributeViews() {
    }

    static class Basic extends AbstractBasicFileAttributeView {
        protected final UnixPath file;
        protected final boolean followLinks;

        Basic(UnixPath unixPath, boolean z) {
            this.file = unixPath;
            this.followLinks = z;
        }

        @Override
        public BasicFileAttributes readAttributes() throws IOException {
            this.file.checkRead();
            try {
                return UnixFileAttributes.get(this.file, this.followLinks).asBasicFileAttributes();
            } catch (UnixException e) {
                e.rethrowAsIOException(this.file);
                return null;
            }
        }

        @Override
        public void setTimes(FileTime fileTime, FileTime fileTime2, FileTime fileTime3) throws IOException {
            if (fileTime == null && fileTime2 == null) {
                return;
            }
            this.file.checkWrite();
            int iOpenForAttributeAccess = this.file.openForAttributeAccess(this.followLinks);
            if (fileTime == null || fileTime2 == null) {
                try {
                    try {
                        UnixFileAttributes unixFileAttributes = UnixFileAttributes.get(iOpenForAttributeAccess);
                        if (fileTime == null) {
                            fileTime = unixFileAttributes.lastModifiedTime();
                        }
                        if (fileTime2 == null) {
                            fileTime2 = unixFileAttributes.lastAccessTime();
                        }
                    } catch (UnixException e) {
                        e.rethrowAsIOException(this.file);
                    }
                } finally {
                    UnixNativeDispatcher.close(iOpenForAttributeAccess);
                }
            }
            long j = fileTime.to(TimeUnit.MICROSECONDS);
            long j2 = fileTime2.to(TimeUnit.MICROSECONDS);
            boolean z = false;
            try {
                if (UnixNativeDispatcher.futimesSupported()) {
                    UnixNativeDispatcher.futimes(iOpenForAttributeAccess, j2, j);
                } else {
                    UnixNativeDispatcher.utimes(this.file, j2, j);
                }
            } catch (UnixException e2) {
                if (e2.errno() == UnixConstants.EINVAL && (j < 0 || j2 < 0)) {
                    z = true;
                } else {
                    e2.rethrowAsIOException(this.file);
                }
            }
            if (z) {
                if (j < 0) {
                    j = 0;
                }
                if (j2 < 0) {
                    j2 = 0;
                }
                try {
                    if (UnixNativeDispatcher.futimesSupported()) {
                        UnixNativeDispatcher.futimes(iOpenForAttributeAccess, j2, j);
                    } else {
                        UnixNativeDispatcher.utimes(this.file, j2, j);
                    }
                } catch (UnixException e3) {
                    e3.rethrowAsIOException(this.file);
                }
            }
        }
    }

    private static class Posix extends Basic implements PosixFileAttributeView {
        private static final String PERMISSIONS_NAME = "permissions";
        private static final String OWNER_NAME = "owner";
        private static final String GROUP_NAME = "group";
        static final Set<String> posixAttributeNames = Util.newSet(basicAttributeNames, PERMISSIONS_NAME, OWNER_NAME, GROUP_NAME);

        Posix(UnixPath unixPath, boolean z) {
            super(unixPath, z);
        }

        final void checkReadExtended() {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                this.file.checkRead();
                securityManager.checkPermission(new RuntimePermission("accessUserInformation"));
            }
        }

        final void checkWriteExtended() {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                this.file.checkWrite();
                securityManager.checkPermission(new RuntimePermission("accessUserInformation"));
            }
        }

        @Override
        public String name() {
            return "posix";
        }

        @Override
        public void setAttribute(String str, Object obj) throws IOException {
            if (str.equals(PERMISSIONS_NAME)) {
                setPermissions((Set) obj);
                return;
            }
            if (str.equals(OWNER_NAME)) {
                setOwner((UserPrincipal) obj);
            } else if (str.equals(GROUP_NAME)) {
                setGroup((GroupPrincipal) obj);
            } else {
                super.setAttribute(str, obj);
            }
        }

        final void addRequestedPosixAttributes(PosixFileAttributes posixFileAttributes, AbstractBasicFileAttributeView.AttributesBuilder attributesBuilder) {
            addRequestedBasicAttributes(posixFileAttributes, attributesBuilder);
            if (attributesBuilder.match(PERMISSIONS_NAME)) {
                attributesBuilder.add(PERMISSIONS_NAME, posixFileAttributes.permissions());
            }
            if (attributesBuilder.match(OWNER_NAME)) {
                attributesBuilder.add(OWNER_NAME, posixFileAttributes.owner());
            }
            if (attributesBuilder.match(GROUP_NAME)) {
                attributesBuilder.add(GROUP_NAME, posixFileAttributes.group());
            }
        }

        @Override
        public Map<String, Object> readAttributes(String[] strArr) throws IOException {
            AbstractBasicFileAttributeView.AttributesBuilder attributesBuilderCreate = AbstractBasicFileAttributeView.AttributesBuilder.create(posixAttributeNames, strArr);
            addRequestedPosixAttributes(readAttributes(), attributesBuilderCreate);
            return attributesBuilderCreate.unmodifiableMap();
        }

        @Override
        public UnixFileAttributes readAttributes() throws IOException {
            checkReadExtended();
            try {
                return UnixFileAttributes.get(this.file, this.followLinks);
            } catch (UnixException e) {
                e.rethrowAsIOException(this.file);
                return null;
            }
        }

        final void setMode(int i) throws IOException {
            checkWriteExtended();
            try {
                if (this.followLinks) {
                    UnixNativeDispatcher.chmod(this.file, i);
                } else {
                    int iOpenForAttributeAccess = this.file.openForAttributeAccess(false);
                    try {
                        UnixNativeDispatcher.fchmod(iOpenForAttributeAccess, i);
                    } finally {
                        UnixNativeDispatcher.close(iOpenForAttributeAccess);
                    }
                }
            } catch (UnixException e) {
                e.rethrowAsIOException(this.file);
            }
        }

        final void setOwners(int i, int i2) throws IOException {
            checkWriteExtended();
            try {
                if (this.followLinks) {
                    UnixNativeDispatcher.chown(this.file, i, i2);
                } else {
                    UnixNativeDispatcher.lchown(this.file, i, i2);
                }
            } catch (UnixException e) {
                e.rethrowAsIOException(this.file);
            }
        }

        @Override
        public void setPermissions(Set<PosixFilePermission> set) throws IOException {
            setMode(UnixFileModeAttribute.toUnixMode(set));
        }

        @Override
        public void setOwner(UserPrincipal userPrincipal) throws IOException {
            if (userPrincipal == null) {
                throw new NullPointerException("'owner' is null");
            }
            if (!(userPrincipal instanceof UnixUserPrincipals.User)) {
                throw new ProviderMismatchException();
            }
            if (userPrincipal instanceof UnixUserPrincipals.Group) {
                throw new IOException("'owner' parameter can't be a group");
            }
            setOwners(((UnixUserPrincipals.User) userPrincipal).uid(), -1);
        }

        @Override
        public UserPrincipal getOwner() throws IOException {
            return readAttributes().owner();
        }

        @Override
        public void setGroup(GroupPrincipal groupPrincipal) throws IOException {
            if (groupPrincipal == null) {
                throw new NullPointerException("'owner' is null");
            }
            if (!(groupPrincipal instanceof UnixUserPrincipals.Group)) {
                throw new ProviderMismatchException();
            }
            setOwners(-1, ((UnixUserPrincipals.Group) groupPrincipal).gid());
        }
    }

    private static class Unix extends Posix {
        private static final String MODE_NAME = "mode";
        private static final String INO_NAME = "ino";
        private static final String DEV_NAME = "dev";
        private static final String RDEV_NAME = "rdev";
        private static final String NLINK_NAME = "nlink";
        private static final String UID_NAME = "uid";
        private static final String GID_NAME = "gid";
        private static final String CTIME_NAME = "ctime";
        static final Set<String> unixAttributeNames = Util.newSet(posixAttributeNames, MODE_NAME, INO_NAME, DEV_NAME, RDEV_NAME, NLINK_NAME, UID_NAME, GID_NAME, CTIME_NAME);

        Unix(UnixPath unixPath, boolean z) {
            super(unixPath, z);
        }

        @Override
        public String name() {
            return "unix";
        }

        @Override
        public void setAttribute(String str, Object obj) throws IOException {
            if (str.equals(MODE_NAME)) {
                setMode(((Integer) obj).intValue());
                return;
            }
            if (str.equals(UID_NAME)) {
                setOwners(((Integer) obj).intValue(), -1);
            } else if (str.equals(GID_NAME)) {
                setOwners(-1, ((Integer) obj).intValue());
            } else {
                super.setAttribute(str, obj);
            }
        }

        @Override
        public Map<String, Object> readAttributes(String[] strArr) throws IOException {
            AbstractBasicFileAttributeView.AttributesBuilder attributesBuilderCreate = AbstractBasicFileAttributeView.AttributesBuilder.create(unixAttributeNames, strArr);
            UnixFileAttributes attributes = readAttributes();
            addRequestedPosixAttributes(attributes, attributesBuilderCreate);
            if (attributesBuilderCreate.match(MODE_NAME)) {
                attributesBuilderCreate.add(MODE_NAME, Integer.valueOf(attributes.mode()));
            }
            if (attributesBuilderCreate.match(INO_NAME)) {
                attributesBuilderCreate.add(INO_NAME, Long.valueOf(attributes.ino()));
            }
            if (attributesBuilderCreate.match(DEV_NAME)) {
                attributesBuilderCreate.add(DEV_NAME, Long.valueOf(attributes.dev()));
            }
            if (attributesBuilderCreate.match(RDEV_NAME)) {
                attributesBuilderCreate.add(RDEV_NAME, Long.valueOf(attributes.rdev()));
            }
            if (attributesBuilderCreate.match(NLINK_NAME)) {
                attributesBuilderCreate.add(NLINK_NAME, Integer.valueOf(attributes.nlink()));
            }
            if (attributesBuilderCreate.match(UID_NAME)) {
                attributesBuilderCreate.add(UID_NAME, Integer.valueOf(attributes.uid()));
            }
            if (attributesBuilderCreate.match(GID_NAME)) {
                attributesBuilderCreate.add(GID_NAME, Integer.valueOf(attributes.gid()));
            }
            if (attributesBuilderCreate.match(CTIME_NAME)) {
                attributesBuilderCreate.add(CTIME_NAME, attributes.ctime());
            }
            return attributesBuilderCreate.unmodifiableMap();
        }
    }

    static Basic createBasicView(UnixPath unixPath, boolean z) {
        return new Basic(unixPath, z);
    }

    static Posix createPosixView(UnixPath unixPath, boolean z) {
        return new Posix(unixPath, z);
    }

    static Unix createUnixView(UnixPath unixPath, boolean z) {
        return new Unix(unixPath, z);
    }

    static FileOwnerAttributeViewImpl createOwnerView(UnixPath unixPath, boolean z) {
        return new FileOwnerAttributeViewImpl(createPosixView(unixPath, z));
    }
}

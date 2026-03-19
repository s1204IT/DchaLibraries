package sun.nio.fs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class LinuxFileSystem extends UnixFileSystem {
    LinuxFileSystem(UnixFileSystemProvider unixFileSystemProvider, String str) {
        super(unixFileSystemProvider, str);
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return new LinuxWatchService(this);
    }

    private static class SupportedFileFileAttributeViewsHolder {
        static final Set<String> supportedFileAttributeViews = supportedFileAttributeViews();

        private SupportedFileFileAttributeViewsHolder() {
        }

        private static Set<String> supportedFileAttributeViews() {
            HashSet hashSet = new HashSet();
            hashSet.addAll(UnixFileSystem.standardFileAttributeViews());
            hashSet.add("dos");
            hashSet.add("user");
            return Collections.unmodifiableSet(hashSet);
        }
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return SupportedFileFileAttributeViewsHolder.supportedFileAttributeViews;
    }

    @Override
    void copyNonPosixAttributes(int i, int i2) throws Throwable {
        LinuxUserDefinedFileAttributeView.copyExtendedAttributes(i, i2);
    }

    Iterable<UnixMountEntry> getMountEntries(String str) {
        ArrayList arrayList = new ArrayList();
        try {
            long j = LinuxNativeDispatcher.setmntent(Util.toBytes(str), Util.toBytes("r"));
            while (true) {
                try {
                    UnixMountEntry unixMountEntry = new UnixMountEntry();
                    if (LinuxNativeDispatcher.getmntent(j, unixMountEntry) < 0) {
                        break;
                    }
                    arrayList.add(unixMountEntry);
                } finally {
                    LinuxNativeDispatcher.endmntent(j);
                }
            }
        } catch (UnixException e) {
        }
        return arrayList;
    }

    @Override
    Iterable<UnixMountEntry> getMountEntries() {
        return getMountEntries("/proc/mounts");
    }

    @Override
    FileStore getFileStore(UnixMountEntry unixMountEntry) throws IOException {
        return new LinuxFileStore(this, unixMountEntry);
    }
}

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.channels.Channel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class SharedFileLockTable extends FileLockTable {
    static final boolean $assertionsDisabled = false;
    private static ConcurrentHashMap<FileKey, List<FileLockReference>> lockMap = new ConcurrentHashMap<>();
    private static ReferenceQueue<FileLock> queue = new ReferenceQueue<>();
    private final Channel channel;
    private final FileKey fileKey;

    private static class FileLockReference extends WeakReference<FileLock> {
        private FileKey fileKey;

        FileLockReference(FileLock fileLock, ReferenceQueue<FileLock> referenceQueue, FileKey fileKey) {
            super(fileLock, referenceQueue);
            this.fileKey = fileKey;
        }

        FileKey fileKey() {
            return this.fileKey;
        }
    }

    SharedFileLockTable(Channel channel, FileDescriptor fileDescriptor) throws IOException {
        this.channel = channel;
        this.fileKey = FileKey.create(fileDescriptor);
    }

    @Override
    public void add(FileLock fileLock) throws OverlappingFileLockException {
        List<FileLockReference> list;
        List<FileLockReference> listPutIfAbsent = lockMap.get(this.fileKey);
        while (true) {
            if (listPutIfAbsent == null) {
                ArrayList arrayList = new ArrayList(2);
                synchronized (arrayList) {
                    listPutIfAbsent = lockMap.putIfAbsent(this.fileKey, arrayList);
                    if (listPutIfAbsent != null) {
                        synchronized (listPutIfAbsent) {
                            list = lockMap.get(this.fileKey);
                            if (listPutIfAbsent == list) {
                                break;
                            }
                        }
                        break;
                    }
                    break;
                }
            }
            synchronized (listPutIfAbsent) {
            }
            listPutIfAbsent = list;
        }
        removeStaleEntries();
    }

    private void removeKeyIfEmpty(FileKey fileKey, List<FileLockReference> list) {
        if (list.isEmpty()) {
            lockMap.remove(fileKey);
        }
    }

    @Override
    public void remove(FileLock fileLock) {
        List<FileLockReference> list = lockMap.get(this.fileKey);
        if (list == null) {
            return;
        }
        synchronized (list) {
            int i = 0;
            while (true) {
                if (i >= list.size()) {
                    break;
                }
                FileLockReference fileLockReference = list.get(i);
                if (fileLockReference.get() == fileLock) {
                    fileLockReference.clear();
                    list.remove(i);
                    break;
                }
                i++;
            }
        }
    }

    @Override
    public List<FileLock> removeAll() {
        ArrayList arrayList = new ArrayList();
        List<FileLockReference> list = lockMap.get(this.fileKey);
        if (list != null) {
            synchronized (list) {
                int i = 0;
                while (i < list.size()) {
                    FileLockReference fileLockReference = list.get(i);
                    FileLock fileLock = fileLockReference.get();
                    if (fileLock != null && fileLock.acquiredBy() == this.channel) {
                        fileLockReference.clear();
                        list.remove(i);
                        arrayList.add(fileLock);
                    } else {
                        i++;
                    }
                }
                removeKeyIfEmpty(this.fileKey, list);
            }
        }
        return arrayList;
    }

    @Override
    public void replace(FileLock fileLock, FileLock fileLock2) {
        List<FileLockReference> list = lockMap.get(this.fileKey);
        synchronized (list) {
            int i = 0;
            while (true) {
                if (i >= list.size()) {
                    break;
                }
                FileLockReference fileLockReference = list.get(i);
                if (fileLockReference.get() != fileLock) {
                    i++;
                } else {
                    fileLockReference.clear();
                    list.set(i, new FileLockReference(fileLock2, queue, this.fileKey));
                    break;
                }
            }
        }
    }

    private void checkList(List<FileLockReference> list, long j, long j2) throws OverlappingFileLockException {
        Iterator<FileLockReference> it = list.iterator();
        while (it.hasNext()) {
            FileLock fileLock = it.next().get();
            if (fileLock != null && fileLock.overlaps(j, j2)) {
                throw new OverlappingFileLockException();
            }
        }
    }

    private void removeStaleEntries() {
        while (true) {
            FileLockReference fileLockReference = (FileLockReference) queue.poll();
            if (fileLockReference != null) {
                FileKey fileKey = fileLockReference.fileKey();
                List<FileLockReference> list = lockMap.get(fileKey);
                if (list != null) {
                    synchronized (list) {
                        list.remove(fileLockReference);
                        removeKeyIfEmpty(fileKey, list);
                    }
                }
            } else {
                return;
            }
        }
    }
}

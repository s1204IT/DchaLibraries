package java.nio.file;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileTreeWalker;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

class FileTreeIterator implements Iterator<FileTreeWalker.Event>, Closeable {
    static final boolean $assertionsDisabled = false;
    private FileTreeWalker.Event next;
    private final FileTreeWalker walker;

    FileTreeIterator(Path path, int i, FileVisitOption... fileVisitOptionArr) throws IOException {
        this.walker = new FileTreeWalker(Arrays.asList(fileVisitOptionArr), i);
        this.next = this.walker.walk(path);
        IOException iOExceptionIoeException = this.next.ioeException();
        if (iOExceptionIoeException != null) {
            throw iOExceptionIoeException;
        }
    }

    private void fetchNextIfNeeded() {
        if (this.next == null) {
            FileTreeWalker.Event next = this.walker.next();
            while (next != null) {
                IOException iOExceptionIoeException = next.ioeException();
                if (iOExceptionIoeException != null) {
                    throw new UncheckedIOException(iOExceptionIoeException);
                }
                if (next.type() != FileTreeWalker.EventType.END_DIRECTORY) {
                    this.next = next;
                    return;
                }
                next = this.walker.next();
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (!this.walker.isOpen()) {
            throw new IllegalStateException();
        }
        fetchNextIfNeeded();
        return this.next != null;
    }

    @Override
    public FileTreeWalker.Event next() {
        if (!this.walker.isOpen()) {
            throw new IllegalStateException();
        }
        fetchNextIfNeeded();
        if (this.next == null) {
            throw new NoSuchElementException();
        }
        FileTreeWalker.Event event = this.next;
        this.next = null;
        return event;
    }

    @Override
    public void close() {
        this.walker.close();
    }
}

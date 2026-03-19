package java.nio.file;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import sun.nio.fs.BasicFileAttributesHolder;

class FileTreeWalker implements Closeable {
    static final boolean $assertionsDisabled = false;
    private boolean closed;
    private final boolean followLinks;
    private final LinkOption[] linkOptions;
    private final int maxDepth;
    private final ArrayDeque<DirectoryNode> stack = new ArrayDeque<>();

    enum EventType {
        START_DIRECTORY,
        END_DIRECTORY,
        ENTRY
    }

    private static class DirectoryNode {
        private final Path dir;
        private final Iterator<Path> iterator;
        private final Object key;
        private boolean skipped;
        private final DirectoryStream<Path> stream;

        DirectoryNode(Path path, Object obj, DirectoryStream<Path> directoryStream) {
            this.dir = path;
            this.key = obj;
            this.stream = directoryStream;
            this.iterator = directoryStream.iterator();
        }

        Path directory() {
            return this.dir;
        }

        Object key() {
            return this.key;
        }

        DirectoryStream<Path> stream() {
            return this.stream;
        }

        Iterator<Path> iterator() {
            return this.iterator;
        }

        void skip() {
            this.skipped = true;
        }

        boolean skipped() {
            return this.skipped;
        }
    }

    static class Event {
        private final BasicFileAttributes attrs;
        private final Path file;
        private final IOException ioe;
        private final EventType type;

        private Event(EventType eventType, Path path, BasicFileAttributes basicFileAttributes, IOException iOException) {
            this.type = eventType;
            this.file = path;
            this.attrs = basicFileAttributes;
            this.ioe = iOException;
        }

        Event(EventType eventType, Path path, BasicFileAttributes basicFileAttributes) {
            this(eventType, path, basicFileAttributes, null);
        }

        Event(EventType eventType, Path path, IOException iOException) {
            this(eventType, path, null, iOException);
        }

        EventType type() {
            return this.type;
        }

        Path file() {
            return this.file;
        }

        BasicFileAttributes attributes() {
            return this.attrs;
        }

        IOException ioeException() {
            return this.ioe;
        }
    }

    FileTreeWalker(Collection<FileVisitOption> collection, int i) {
        Iterator<FileVisitOption> it = collection.iterator();
        boolean z = false;
        while (it.hasNext()) {
            if (AnonymousClass1.$SwitchMap$java$nio$file$FileVisitOption[it.next().ordinal()] == 1) {
                z = true;
            } else {
                throw new AssertionError((Object) "Should not get here");
            }
        }
        if (i < 0) {
            throw new IllegalArgumentException("'maxDepth' is negative");
        }
        this.followLinks = z;
        this.linkOptions = z ? new LinkOption[0] : new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
        this.maxDepth = i;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$java$nio$file$FileVisitOption = new int[FileVisitOption.values().length];

        static {
            try {
                $SwitchMap$java$nio$file$FileVisitOption[FileVisitOption.FOLLOW_LINKS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    private BasicFileAttributes getAttributes(Path path, boolean z) throws IOException {
        BasicFileAttributes basicFileAttributes;
        if (z && (path instanceof BasicFileAttributesHolder) && System.getSecurityManager() == null && (basicFileAttributes = ((BasicFileAttributesHolder) path).get()) != null && (!this.followLinks || !basicFileAttributes.isSymbolicLink())) {
            return basicFileAttributes;
        }
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, this.linkOptions);
        } catch (IOException e) {
            if (!this.followLinks) {
                throw e;
            }
            return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }
    }

    private boolean wouldLoop(Path path, Object obj) {
        for (DirectoryNode directoryNode : this.stack) {
            Object objKey = directoryNode.key();
            if (obj != null && objKey != null) {
                if (obj.equals(objKey)) {
                    return true;
                }
            } else {
                try {
                    if (Files.isSameFile(path, directoryNode.directory())) {
                        return true;
                    }
                } catch (IOException | SecurityException e) {
                }
            }
        }
        return false;
    }

    private Event visit(Path path, boolean z, boolean z2) {
        try {
            BasicFileAttributes attributes = getAttributes(path, z2);
            if (this.stack.size() >= this.maxDepth || !attributes.isDirectory()) {
                return new Event(EventType.ENTRY, path, attributes);
            }
            if (this.followLinks && wouldLoop(path, attributes.fileKey())) {
                return new Event(EventType.ENTRY, path, new FileSystemLoopException(path.toString()));
            }
            try {
                this.stack.push(new DirectoryNode(path, attributes.fileKey(), Files.newDirectoryStream(path)));
                return new Event(EventType.START_DIRECTORY, path, attributes);
            } catch (IOException e) {
                return new Event(EventType.ENTRY, path, e);
            } catch (SecurityException e2) {
                if (z) {
                    return null;
                }
                throw e2;
            }
        } catch (IOException e3) {
            return new Event(EventType.ENTRY, path, e3);
        } catch (SecurityException e4) {
            if (z) {
                return null;
            }
            throw e4;
        }
    }

    Event walk(Path path) {
        if (this.closed) {
            throw new IllegalStateException("Closed");
        }
        return visit(path, false, false);
    }

    Event next() {
        Path next;
        IOException cause;
        Event eventVisit;
        DirectoryNode directoryNodePeek = this.stack.peek();
        if (directoryNodePeek == null) {
            return null;
        }
        do {
            if (directoryNodePeek.skipped()) {
                next = null;
                cause = null;
            } else {
                Iterator<Path> it = directoryNodePeek.iterator();
                try {
                    if (it.hasNext()) {
                        next = it.next();
                    } else {
                        next = null;
                    }
                    cause = null;
                } catch (DirectoryIteratorException e) {
                    cause = e.getCause();
                    next = null;
                }
            }
            if (next == null) {
                try {
                    directoryNodePeek.stream().close();
                } catch (IOException e2) {
                    e = e2;
                    if (cause == null) {
                        cause.addSuppressed(e);
                    }
                    this.stack.pop();
                    return new Event(EventType.END_DIRECTORY, directoryNodePeek.directory(), e);
                }
                e = cause;
                this.stack.pop();
                return new Event(EventType.END_DIRECTORY, directoryNodePeek.directory(), e);
            }
            eventVisit = visit(next, true, true);
        } while (eventVisit == null);
        return eventVisit;
    }

    void pop() {
        if (!this.stack.isEmpty()) {
            try {
                this.stack.pop().stream().close();
            } catch (IOException e) {
            }
        }
    }

    void skipRemainingSiblings() {
        if (!this.stack.isEmpty()) {
            this.stack.peek().skip();
        }
    }

    boolean isOpen() {
        return !this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            while (!this.stack.isEmpty()) {
                pop();
            }
            this.closed = true;
        }
    }
}

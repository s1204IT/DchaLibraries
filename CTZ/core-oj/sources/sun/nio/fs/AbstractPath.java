package sun.nio.fs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.NoSuchElementException;

abstract class AbstractPath implements Path {
    protected AbstractPath() {
    }

    @Override
    public final boolean startsWith(String str) {
        return startsWith(getFileSystem().getPath(str, new String[0]));
    }

    @Override
    public final boolean endsWith(String str) {
        return endsWith(getFileSystem().getPath(str, new String[0]));
    }

    @Override
    public final Path resolve(String str) {
        return resolve(getFileSystem().getPath(str, new String[0]));
    }

    @Override
    public final Path resolveSibling(Path path) {
        if (path == null) {
            throw new NullPointerException();
        }
        Path parent = getParent();
        return parent == null ? path : parent.resolve(path);
    }

    @Override
    public final Path resolveSibling(String str) {
        return resolveSibling(getFileSystem().getPath(str, new String[0]));
    }

    @Override
    public final Iterator<Path> iterator() {
        return new Iterator<Path>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return this.i < AbstractPath.this.getNameCount();
            }

            @Override
            public Path next() {
                if (this.i < AbstractPath.this.getNameCount()) {
                    Path name = AbstractPath.this.getName(this.i);
                    this.i++;
                    return name;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public final File toFile() {
        return new File(toString());
    }

    @Override
    public final WatchKey register(WatchService watchService, WatchEvent.Kind<?>... kindArr) throws IOException {
        return register(watchService, kindArr, new WatchEvent.Modifier[0]);
    }
}

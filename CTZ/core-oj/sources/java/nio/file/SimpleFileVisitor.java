package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class SimpleFileVisitor<T> implements FileVisitor<T> {
    protected SimpleFileVisitor() {
    }

    @Override
    public FileVisitResult preVisitDirectory(T t, BasicFileAttributes basicFileAttributes) throws IOException {
        Objects.requireNonNull(t);
        Objects.requireNonNull(basicFileAttributes);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(T t, BasicFileAttributes basicFileAttributes) throws IOException {
        Objects.requireNonNull(t);
        Objects.requireNonNull(basicFileAttributes);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(T t, IOException iOException) throws IOException {
        Objects.requireNonNull(t);
        throw iOException;
    }

    @Override
    public FileVisitResult postVisitDirectory(T t, IOException iOException) throws IOException {
        Objects.requireNonNull(t);
        if (iOException != null) {
            throw iOException;
        }
        return FileVisitResult.CONTINUE;
    }
}

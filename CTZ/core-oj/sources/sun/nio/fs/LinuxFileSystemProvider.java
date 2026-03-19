package sun.nio.fs;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileTypeDetector;

public class LinuxFileSystemProvider extends UnixFileSystemProvider {
    @Override
    LinuxFileSystem newFileSystem(String str) {
        return new LinuxFileSystem(this, str);
    }

    @Override
    LinuxFileStore getFileStore(UnixPath unixPath) throws IOException {
        throw new SecurityException("getFileStore");
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> cls, LinkOption... linkOptionArr) {
        return (V) super.getFileAttributeView(path, cls, linkOptionArr);
    }

    @Override
    public DynamicFileAttributeView getFileAttributeView(Path path, String str, LinkOption... linkOptionArr) {
        return super.getFileAttributeView(path, str, linkOptionArr);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> cls, LinkOption... linkOptionArr) throws IOException {
        return (A) super.readAttributes(path, cls, linkOptionArr);
    }

    @Override
    FileTypeDetector getFileTypeDetector() {
        return new MimeTypesFileTypeDetector();
    }
}

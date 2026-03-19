package java.nio.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileTreeWalker;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.nio.file.spi.FileTypeDetector;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import sun.nio.fs.DefaultFileTypeDetector;

public final class Files {
    static final boolean $assertionsDisabled = false;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = 2147483639;

    private Files() {
    }

    private static FileSystemProvider provider(Path path) {
        return path.getFileSystem().provider();
    }

    private static Runnable asUncheckedRunnable(final Closeable closeable) {
        return new Runnable() {
            @Override
            public final void run() {
                Files.lambda$asUncheckedRunnable$0(closeable);
            }
        };
    }

    static void lambda$asUncheckedRunnable$0(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static InputStream newInputStream(Path path, OpenOption... openOptionArr) throws IOException {
        return provider(path).newInputStream(path, openOptionArr);
    }

    public static OutputStream newOutputStream(Path path, OpenOption... openOptionArr) throws IOException {
        return provider(path).newOutputStream(path, openOptionArr);
    }

    public static SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> set, FileAttribute<?>... fileAttributeArr) throws IOException {
        return provider(path).newByteChannel(path, set, fileAttributeArr);
    }

    public static SeekableByteChannel newByteChannel(Path path, OpenOption... openOptionArr) throws IOException {
        HashSet hashSet = new HashSet(openOptionArr.length);
        Collections.addAll(hashSet, openOptionArr);
        return newByteChannel(path, hashSet, new FileAttribute[0]);
    }

    private static class AcceptAllFilter implements DirectoryStream.Filter<Path> {
        static final AcceptAllFilter FILTER = new AcceptAllFilter();

        private AcceptAllFilter() {
        }

        @Override
        public boolean accept(Path path) {
            return true;
        }
    }

    public static DirectoryStream<Path> newDirectoryStream(Path path) throws IOException {
        return provider(path).newDirectoryStream(path, AcceptAllFilter.FILTER);
    }

    public static DirectoryStream<Path> newDirectoryStream(Path path, String str) throws IOException {
        if (str.equals("*")) {
            return newDirectoryStream(path);
        }
        FileSystem fileSystem = path.getFileSystem();
        final PathMatcher pathMatcher = fileSystem.getPathMatcher("glob:" + str);
        return fileSystem.provider().newDirectoryStream(path, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path path2) {
                return pathMatcher.matches(path2.getFileName());
            }
        });
    }

    public static DirectoryStream<Path> newDirectoryStream(Path path, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return provider(path).newDirectoryStream(path, filter);
    }

    public static Path createFile(Path path, FileAttribute<?>... fileAttributeArr) throws IOException {
        newByteChannel(path, EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), fileAttributeArr).close();
        return path;
    }

    public static Path createDirectory(Path path, FileAttribute<?>... fileAttributeArr) throws IOException {
        provider(path).createDirectory(path, fileAttributeArr);
        return path;
    }

    public static Path createDirectories(Path path, FileAttribute<?>... fileAttributeArr) throws IOException {
        try {
            createAndCheckIsDirectory(path, fileAttributeArr);
            return path;
        } catch (FileAlreadyExistsException e) {
            throw e;
        } catch (IOException e2) {
            try {
                path = path.toAbsolutePath();
                e = null;
            } catch (SecurityException e3) {
                e = e3;
            }
            Path parent = path.getParent();
            while (parent != null) {
                try {
                    provider(parent).checkAccess(parent, new AccessMode[0]);
                    break;
                } catch (NoSuchFileException e4) {
                    parent = parent.getParent();
                }
            }
            if (parent == null) {
                if (e == null) {
                    throw new FileSystemException(path.toString(), null, "Unable to determine if root directory exists");
                }
                throw e;
            }
            Iterator<Path> it = parent.relativize(path).iterator();
            while (it.hasNext()) {
                parent = parent.resolve(it.next());
                createAndCheckIsDirectory(parent, fileAttributeArr);
            }
            return path;
        }
    }

    private static void createAndCheckIsDirectory(Path path, FileAttribute<?>... fileAttributeArr) throws IOException {
        try {
            createDirectory(path, fileAttributeArr);
        } catch (FileAlreadyExistsException e) {
            if (!isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                throw e;
            }
        }
    }

    public static Path createTempFile(Path path, String str, String str2, FileAttribute<?>... fileAttributeArr) throws IOException {
        return TempFileHelper.createTempFile((Path) Objects.requireNonNull(path), str, str2, fileAttributeArr);
    }

    public static Path createTempFile(String str, String str2, FileAttribute<?>... fileAttributeArr) throws IOException {
        return TempFileHelper.createTempFile(null, str, str2, fileAttributeArr);
    }

    public static Path createTempDirectory(Path path, String str, FileAttribute<?>... fileAttributeArr) throws IOException {
        return TempFileHelper.createTempDirectory((Path) Objects.requireNonNull(path), str, fileAttributeArr);
    }

    public static Path createTempDirectory(String str, FileAttribute<?>... fileAttributeArr) throws IOException {
        return TempFileHelper.createTempDirectory(null, str, fileAttributeArr);
    }

    public static Path createSymbolicLink(Path path, Path path2, FileAttribute<?>... fileAttributeArr) throws IOException {
        provider(path).createSymbolicLink(path, path2, fileAttributeArr);
        return path;
    }

    public static Path createLink(Path path, Path path2) throws IOException {
        provider(path).createLink(path, path2);
        return path;
    }

    public static void delete(Path path) throws IOException {
        provider(path).delete(path);
    }

    public static boolean deleteIfExists(Path path) throws IOException {
        return provider(path).deleteIfExists(path);
    }

    public static Path copy(Path path, Path path2, CopyOption... copyOptionArr) throws IOException {
        FileSystemProvider fileSystemProviderProvider = provider(path);
        if (provider(path2) == fileSystemProviderProvider) {
            fileSystemProviderProvider.copy(path, path2, copyOptionArr);
        } else {
            CopyMoveHelper.copyToForeignTarget(path, path2, copyOptionArr);
        }
        return path2;
    }

    public static Path move(Path path, Path path2, CopyOption... copyOptionArr) throws IOException {
        FileSystemProvider fileSystemProviderProvider = provider(path);
        if (provider(path2) == fileSystemProviderProvider) {
            fileSystemProviderProvider.move(path, path2, copyOptionArr);
        } else {
            CopyMoveHelper.moveToForeignTarget(path, path2, copyOptionArr);
        }
        return path2;
    }

    public static Path readSymbolicLink(Path path) throws IOException {
        return provider(path).readSymbolicLink(path);
    }

    public static FileStore getFileStore(Path path) throws IOException {
        return provider(path).getFileStore(path);
    }

    public static boolean isSameFile(Path path, Path path2) throws IOException {
        return provider(path).isSameFile(path, path2);
    }

    public static boolean isHidden(Path path) throws IOException {
        return provider(path).isHidden(path);
    }

    private static class FileTypeDetectors {
        static final FileTypeDetector defaultFileTypeDetector = createDefaultFileTypeDetector();
        static final List<FileTypeDetector> installeDetectors = loadInstalledDetectors();

        private FileTypeDetectors() {
        }

        private static FileTypeDetector createDefaultFileTypeDetector() {
            return (FileTypeDetector) AccessController.doPrivileged(new PrivilegedAction<FileTypeDetector>() {
                @Override
                public FileTypeDetector run() {
                    return DefaultFileTypeDetector.create();
                }
            });
        }

        private static List<FileTypeDetector> loadInstalledDetectors() {
            return (List) AccessController.doPrivileged(new PrivilegedAction<List<FileTypeDetector>>() {
                @Override
                public List<FileTypeDetector> run() {
                    ArrayList arrayList = new ArrayList();
                    Iterator it = ServiceLoader.load(FileTypeDetector.class, ClassLoader.getSystemClassLoader()).iterator();
                    while (it.hasNext()) {
                        arrayList.add((FileTypeDetector) it.next());
                    }
                    return arrayList;
                }
            });
        }
    }

    public static String probeContentType(Path path) throws IOException {
        Iterator<FileTypeDetector> it = FileTypeDetectors.installeDetectors.iterator();
        while (it.hasNext()) {
            String strProbeContentType = it.next().probeContentType(path);
            if (strProbeContentType != null) {
                return strProbeContentType;
            }
        }
        return FileTypeDetectors.defaultFileTypeDetector.probeContentType(path);
    }

    public static <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> cls, LinkOption... linkOptionArr) {
        return (V) provider(path).getFileAttributeView(path, cls, linkOptionArr);
    }

    public static <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> cls, LinkOption... linkOptionArr) throws IOException {
        return (A) provider(path).readAttributes(path, cls, linkOptionArr);
    }

    public static Path setAttribute(Path path, String str, Object obj, LinkOption... linkOptionArr) throws IOException {
        provider(path).setAttribute(path, str, obj, linkOptionArr);
        return path;
    }

    public static Object getAttribute(Path path, String str, LinkOption... linkOptionArr) throws IOException {
        if (str.indexOf(42) >= 0 || str.indexOf(44) >= 0) {
            throw new IllegalArgumentException(str);
        }
        Map<String, Object> attributes = readAttributes(path, str, linkOptionArr);
        int iIndexOf = str.indexOf(58);
        if (iIndexOf != -1) {
            str = iIndexOf == str.length() ? "" : str.substring(iIndexOf + 1);
        }
        return attributes.get(str);
    }

    public static Map<String, Object> readAttributes(Path path, String str, LinkOption... linkOptionArr) throws IOException {
        return provider(path).readAttributes(path, str, linkOptionArr);
    }

    public static Set<PosixFilePermission> getPosixFilePermissions(Path path, LinkOption... linkOptionArr) throws IOException {
        return ((PosixFileAttributes) readAttributes(path, PosixFileAttributes.class, linkOptionArr)).permissions();
    }

    public static Path setPosixFilePermissions(Path path, Set<PosixFilePermission> set) throws IOException {
        PosixFileAttributeView posixFileAttributeView = (PosixFileAttributeView) getFileAttributeView(path, PosixFileAttributeView.class, new LinkOption[0]);
        if (posixFileAttributeView == null) {
            throw new UnsupportedOperationException();
        }
        posixFileAttributeView.setPermissions(set);
        return path;
    }

    public static UserPrincipal getOwner(Path path, LinkOption... linkOptionArr) throws IOException {
        FileOwnerAttributeView fileOwnerAttributeView = (FileOwnerAttributeView) getFileAttributeView(path, FileOwnerAttributeView.class, linkOptionArr);
        if (fileOwnerAttributeView == null) {
            throw new UnsupportedOperationException();
        }
        return fileOwnerAttributeView.getOwner();
    }

    public static Path setOwner(Path path, UserPrincipal userPrincipal) throws IOException {
        FileOwnerAttributeView fileOwnerAttributeView = (FileOwnerAttributeView) getFileAttributeView(path, FileOwnerAttributeView.class, new LinkOption[0]);
        if (fileOwnerAttributeView == null) {
            throw new UnsupportedOperationException();
        }
        fileOwnerAttributeView.setOwner(userPrincipal);
        return path;
    }

    public static boolean isSymbolicLink(Path path) {
        try {
            return readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isSymbolicLink();
        } catch (IOException e) {
            return $assertionsDisabled;
        }
    }

    public static boolean isDirectory(Path path, LinkOption... linkOptionArr) {
        try {
            return readAttributes(path, BasicFileAttributes.class, linkOptionArr).isDirectory();
        } catch (IOException e) {
            return $assertionsDisabled;
        }
    }

    public static boolean isRegularFile(Path path, LinkOption... linkOptionArr) {
        try {
            return readAttributes(path, BasicFileAttributes.class, linkOptionArr).isRegularFile();
        } catch (IOException e) {
            return $assertionsDisabled;
        }
    }

    public static FileTime getLastModifiedTime(Path path, LinkOption... linkOptionArr) throws IOException {
        return readAttributes(path, BasicFileAttributes.class, linkOptionArr).lastModifiedTime();
    }

    public static Path setLastModifiedTime(Path path, FileTime fileTime) throws IOException {
        ((BasicFileAttributeView) getFileAttributeView(path, BasicFileAttributeView.class, new LinkOption[0])).setTimes(fileTime, null, null);
        return path;
    }

    public static long size(Path path) throws IOException {
        return readAttributes(path, BasicFileAttributes.class, new LinkOption[0]).size();
    }

    private static boolean followLinks(LinkOption... linkOptionArr) {
        int length = linkOptionArr.length;
        boolean z = true;
        int i = 0;
        while (i < length) {
            LinkOption linkOption = linkOptionArr[i];
            if (linkOption == LinkOption.NOFOLLOW_LINKS) {
                i++;
                z = false;
            } else {
                if (linkOption == null) {
                    throw new NullPointerException();
                }
                throw new AssertionError((Object) "Should not get here");
            }
        }
        return z;
    }

    public static boolean exists(Path path, LinkOption... linkOptionArr) {
        try {
            if (followLinks(linkOptionArr)) {
                provider(path).checkAccess(path, new AccessMode[0]);
            } else {
                readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            }
            return true;
        } catch (IOException e) {
            return $assertionsDisabled;
        }
    }

    public static boolean notExists(Path path, LinkOption... linkOptionArr) {
        try {
            if (followLinks(linkOptionArr)) {
                provider(path).checkAccess(path, new AccessMode[0]);
            } else {
                readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            }
            return $assertionsDisabled;
        } catch (NoSuchFileException e) {
            return true;
        } catch (IOException e2) {
            return $assertionsDisabled;
        }
    }

    private static boolean isAccessible(Path path, AccessMode... accessModeArr) {
        try {
            provider(path).checkAccess(path, accessModeArr);
            return true;
        } catch (IOException e) {
            return $assertionsDisabled;
        }
    }

    public static boolean isReadable(Path path) {
        return isAccessible(path, AccessMode.READ);
    }

    public static boolean isWritable(Path path) {
        return isAccessible(path, AccessMode.WRITE);
    }

    public static boolean isExecutable(Path path) {
        return isAccessible(path, AccessMode.EXECUTE);
    }

    public static Path walkFileTree(Path path, Set<FileVisitOption> set, int i, FileVisitor<? super Path> fileVisitor) throws Exception {
        FileVisitResult fileVisitResultVisitFile;
        FileTreeWalker fileTreeWalker = new FileTreeWalker(set, i);
        Throwable th = null;
        try {
            try {
                FileTreeWalker.Event eventWalk = fileTreeWalker.walk(path);
                do {
                    switch (eventWalk.type()) {
                        case ENTRY:
                            IOException iOExceptionIoeException = eventWalk.ioeException();
                            fileVisitResultVisitFile = iOExceptionIoeException == null ? fileVisitor.visitFile(eventWalk.file(), eventWalk.attributes()) : fileVisitor.visitFileFailed(eventWalk.file(), iOExceptionIoeException);
                            if (Objects.requireNonNull(fileVisitResultVisitFile) != FileVisitResult.CONTINUE) {
                                if (fileVisitResultVisitFile != FileVisitResult.TERMINATE) {
                                    if (fileVisitResultVisitFile == FileVisitResult.SKIP_SIBLINGS) {
                                        fileTreeWalker.skipRemainingSiblings();
                                    }
                                    eventWalk = fileTreeWalker.next();
                                    break;
                                }
                            } else {
                                eventWalk = fileTreeWalker.next();
                                break;
                            }
                            return path;
                        case START_DIRECTORY:
                            fileVisitResultVisitFile = fileVisitor.preVisitDirectory(eventWalk.file(), eventWalk.attributes());
                            if (fileVisitResultVisitFile == FileVisitResult.SKIP_SUBTREE || fileVisitResultVisitFile == FileVisitResult.SKIP_SIBLINGS) {
                                fileTreeWalker.pop();
                            }
                            if (Objects.requireNonNull(fileVisitResultVisitFile) != FileVisitResult.CONTINUE) {
                            }
                            return path;
                        case END_DIRECTORY:
                            fileVisitResultVisitFile = fileVisitor.postVisitDirectory(eventWalk.file(), eventWalk.ioeException());
                            if (fileVisitResultVisitFile == FileVisitResult.SKIP_SIBLINGS) {
                                fileVisitResultVisitFile = FileVisitResult.CONTINUE;
                            }
                            if (Objects.requireNonNull(fileVisitResultVisitFile) != FileVisitResult.CONTINUE) {
                            }
                            return path;
                        default:
                            throw new AssertionError((Object) "Should not get here");
                    }
                } while (eventWalk != null);
                return path;
            } finally {
            }
        } finally {
            $closeResource(th, fileTreeWalker);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public static Path walkFileTree(Path path, FileVisitor<? super Path> fileVisitor) throws IOException {
        return walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, fileVisitor);
    }

    public static BufferedReader newBufferedReader(Path path, Charset charset) throws IOException {
        return new BufferedReader(new InputStreamReader(newInputStream(path, new OpenOption[0]), charset.newDecoder()));
    }

    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return newBufferedReader(path, StandardCharsets.UTF_8);
    }

    public static BufferedWriter newBufferedWriter(Path path, Charset charset, OpenOption... openOptionArr) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(newOutputStream(path, openOptionArr), charset.newEncoder()));
    }

    public static BufferedWriter newBufferedWriter(Path path, OpenOption... openOptionArr) throws IOException {
        return newBufferedWriter(path, StandardCharsets.UTF_8, openOptionArr);
    }

    private static long copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[8192];
        long j = 0;
        while (true) {
            int i = inputStream.read(bArr);
            if (i > 0) {
                outputStream.write(bArr, 0, i);
                j += (long) i;
            } else {
                return j;
            }
        }
    }

    public static long copy(InputStream inputStream, Path path, CopyOption... copyOptionArr) throws Exception {
        Objects.requireNonNull(inputStream);
        int length = copyOptionArr.length;
        int i = 0;
        boolean z = false;
        while (i < length) {
            CopyOption copyOption = copyOptionArr[i];
            if (copyOption == StandardCopyOption.REPLACE_EXISTING) {
                i++;
                z = true;
            } else {
                if (copyOption == null) {
                    throw new NullPointerException("options contains 'null'");
                }
                throw new UnsupportedOperationException(((Object) copyOption) + " not supported");
            }
        }
        Throwable th = null;
        if (z) {
            try {
                deleteIfExists(path);
                e = null;
            } catch (SecurityException e) {
                e = e;
            }
        } else {
            e = null;
        }
        try {
            OutputStream outputStreamNewOutputStream = newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try {
                return copy(inputStream, outputStreamNewOutputStream);
            } finally {
                if (outputStreamNewOutputStream != null) {
                    $closeResource(th, outputStreamNewOutputStream);
                }
            }
        } catch (FileAlreadyExistsException e2) {
            if (e != null) {
                throw e;
            }
            throw e2;
        }
    }

    public static long copy(Path path, OutputStream outputStream) throws Exception {
        Objects.requireNonNull(outputStream);
        InputStream inputStreamNewInputStream = newInputStream(path, new OpenOption[0]);
        try {
            return copy(inputStreamNewInputStream, outputStream);
        } finally {
            if (inputStreamNewInputStream != null) {
                $closeResource(null, inputStreamNewInputStream);
            }
        }
    }

    private static byte[] read(InputStream inputStream, int i) throws IOException {
        int i2;
        byte[] bArrCopyOf = new byte[i];
        int i3 = 0;
        while (true) {
            int i4 = inputStream.read(bArrCopyOf, i3, i - i3);
            if (i4 > 0) {
                i3 += i4;
            } else {
                if (i4 < 0 || (i2 = inputStream.read()) < 0) {
                    break;
                }
                if (i <= MAX_BUFFER_SIZE - i) {
                    i = Math.max(i << 1, 8192);
                } else {
                    if (i == MAX_BUFFER_SIZE) {
                        throw new OutOfMemoryError("Required array size too large");
                    }
                    i = MAX_BUFFER_SIZE;
                }
                bArrCopyOf = Arrays.copyOf(bArrCopyOf, i);
                bArrCopyOf[i3] = (byte) i2;
                i3++;
            }
        }
    }

    public static byte[] readAllBytes(Path path) throws Exception {
        Throwable th;
        Throwable th2;
        SeekableByteChannel seekableByteChannelNewByteChannel = newByteChannel(path, new OpenOption[0]);
        try {
            InputStream inputStreamNewInputStream = Channels.newInputStream(seekableByteChannelNewByteChannel);
            try {
                long size = seekableByteChannelNewByteChannel.size();
                if (size > 2147483639) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                byte[] bArr = read(inputStreamNewInputStream, (int) size);
                if (inputStreamNewInputStream != null) {
                    $closeResource(null, inputStreamNewInputStream);
                }
                return bArr;
            } catch (Throwable th3) {
                try {
                    throw th3;
                } catch (Throwable th4) {
                    th = th3;
                    th2 = th4;
                    if (inputStreamNewInputStream != null) {
                        throw th2;
                    }
                    $closeResource(th, inputStreamNewInputStream);
                    throw th2;
                }
            }
        } finally {
            if (seekableByteChannelNewByteChannel != null) {
                $closeResource(null, seekableByteChannelNewByteChannel);
            }
        }
    }

    public static List<String> readAllLines(Path path, Charset charset) throws Exception {
        BufferedReader bufferedReaderNewBufferedReader = newBufferedReader(path, charset);
        Throwable th = null;
        try {
            ArrayList arrayList = new ArrayList();
            while (true) {
                String line = bufferedReaderNewBufferedReader.readLine();
                if (line == null) {
                    break;
                }
                arrayList.add(line);
            }
            return arrayList;
        } finally {
            if (bufferedReaderNewBufferedReader != null) {
                $closeResource(th, bufferedReaderNewBufferedReader);
            }
        }
    }

    public static List<String> readAllLines(Path path) throws IOException {
        return readAllLines(path, StandardCharsets.UTF_8);
    }

    public static Path write(Path path, byte[] bArr, OpenOption... openOptionArr) throws Exception {
        Objects.requireNonNull(bArr);
        OutputStream outputStreamNewOutputStream = newOutputStream(path, openOptionArr);
        try {
            int length = bArr.length;
            int i = length;
            while (i > 0) {
                int iMin = Math.min(i, 8192);
                outputStreamNewOutputStream.write(bArr, length - i, iMin);
                i -= iMin;
            }
            return path;
        } finally {
            if (outputStreamNewOutputStream != null) {
                $closeResource(null, outputStreamNewOutputStream);
            }
        }
    }

    public static Path write(Path path, Iterable<? extends CharSequence> iterable, Charset charset, OpenOption... openOptionArr) throws Exception {
        Objects.requireNonNull(iterable);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(newOutputStream(path, openOptionArr), charset.newEncoder()));
        try {
            Iterator<? extends CharSequence> it = iterable.iterator();
            while (it.hasNext()) {
                bufferedWriter.append(it.next());
                bufferedWriter.newLine();
            }
            return path;
        } finally {
            $closeResource(null, bufferedWriter);
        }
    }

    public static Path write(Path path, Iterable<? extends CharSequence> iterable, OpenOption... openOptionArr) throws IOException {
        return write(path, iterable, StandardCharsets.UTF_8, openOptionArr);
    }

    public static Stream<Path> list(Path path) throws IOException {
        DirectoryStream<Path> directoryStreamNewDirectoryStream = newDirectoryStream(path);
        try {
            final Iterator<Path> it = directoryStreamNewDirectoryStream.iterator();
            return (Stream) StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Path>() {
                @Override
                public boolean hasNext() {
                    try {
                        return it.hasNext();
                    } catch (DirectoryIteratorException e) {
                        throw new UncheckedIOException(e.getCause());
                    }
                }

                @Override
                public Path next() {
                    try {
                        return (Path) it.next();
                    } catch (DirectoryIteratorException e) {
                        throw new UncheckedIOException(e.getCause());
                    }
                }
            }, 1), $assertionsDisabled).onClose(asUncheckedRunnable(directoryStreamNewDirectoryStream));
        } catch (Error | RuntimeException e) {
            try {
                directoryStreamNewDirectoryStream.close();
            } catch (IOException e2) {
                try {
                    e.addSuppressed(e2);
                } catch (Throwable th) {
                }
            }
            throw e;
        }
    }

    public static Stream<Path> walk(Path path, int i, FileVisitOption... fileVisitOptionArr) throws IOException {
        FileTreeIterator fileTreeIterator = new FileTreeIterator(path, i, fileVisitOptionArr);
        try {
            Stream stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(fileTreeIterator, 1), $assertionsDisabled);
            Objects.requireNonNull(fileTreeIterator);
            return stream.onClose(new $$Lambda$sYbGIj22XbOmrXSY16DZsES4BAM(fileTreeIterator)).map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return ((FileTreeWalker.Event) obj).file();
                }
            });
        } catch (Error | RuntimeException e) {
            fileTreeIterator.close();
            throw e;
        }
    }

    public static Stream<Path> walk(Path path, FileVisitOption... fileVisitOptionArr) throws IOException {
        return walk(path, Integer.MAX_VALUE, fileVisitOptionArr);
    }

    public static Stream<Path> find(Path path, int i, final BiPredicate<Path, BasicFileAttributes> biPredicate, FileVisitOption... fileVisitOptionArr) throws IOException {
        FileTreeIterator fileTreeIterator = new FileTreeIterator(path, i, fileVisitOptionArr);
        try {
            Stream stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(fileTreeIterator, 1), $assertionsDisabled);
            Objects.requireNonNull(fileTreeIterator);
            return stream.onClose(new $$Lambda$sYbGIj22XbOmrXSY16DZsES4BAM(fileTreeIterator)).filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    FileTreeWalker.Event event = (FileTreeWalker.Event) obj;
                    return biPredicate.test(event.file(), event.attributes());
                }
            }).map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return ((FileTreeWalker.Event) obj).file();
                }
            });
        } catch (Error | RuntimeException e) {
            fileTreeIterator.close();
            throw e;
        }
    }

    public static Stream<String> lines(Path path, Charset charset) throws IOException {
        BufferedReader bufferedReaderNewBufferedReader = newBufferedReader(path, charset);
        try {
            return (Stream) bufferedReaderNewBufferedReader.lines().onClose(asUncheckedRunnable(bufferedReaderNewBufferedReader));
        } catch (Error | RuntimeException e) {
            try {
                bufferedReaderNewBufferedReader.close();
            } catch (IOException e2) {
                try {
                    e.addSuppressed(e2);
                } catch (Throwable th) {
                }
            }
            throw e;
        }
    }

    public static Stream<String> lines(Path path) throws IOException {
        return lines(path, StandardCharsets.UTF_8);
    }
}

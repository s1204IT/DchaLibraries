package sun.nio.fs;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.FileSystemException;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Objects;

class UnixPath extends AbstractPath {
    static final boolean $assertionsDisabled = false;
    private static ThreadLocal<SoftReference<CharsetEncoder>> encoder = new ThreadLocal<>();
    private final UnixFileSystem fs;
    private int hash;
    private volatile int[] offsets;
    private final byte[] path;
    private volatile String stringValue;

    UnixPath(UnixFileSystem unixFileSystem, byte[] bArr) {
        this.fs = unixFileSystem;
        this.path = bArr;
    }

    UnixPath(UnixFileSystem unixFileSystem, String str) {
        this(unixFileSystem, encode(unixFileSystem, normalizeAndCheck(str)));
    }

    static String normalizeAndCheck(String str) {
        int length = str.length();
        int i = 0;
        char c = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '/' && c == '/') {
                return normalize(str, length, i - 1);
            }
            checkNotNul(str, cCharAt);
            i++;
            c = cCharAt;
        }
        if (c == '/') {
            return normalize(str, length, length - 1);
        }
        return str;
    }

    private static void checkNotNul(String str, char c) {
        if (c == 0) {
            throw new InvalidPathException(str, "Nul character not allowed");
        }
    }

    private static String normalize(String str, int i, int i2) {
        if (i == 0) {
            return str;
        }
        while (i > 0 && str.charAt(i - 1) == '/') {
            i--;
        }
        if (i == 0) {
            return "/";
        }
        StringBuilder sb = new StringBuilder(str.length());
        char c = 0;
        if (i2 > 0) {
            sb.append(str.substring(0, i2));
        }
        while (i2 < i) {
            char cCharAt = str.charAt(i2);
            if (cCharAt != '/' || c != '/') {
                checkNotNul(str, cCharAt);
                sb.append(cCharAt);
                c = cCharAt;
            }
            i2++;
        }
        return sb.toString();
    }

    private static byte[] encode(UnixFileSystem unixFileSystem, String str) {
        SoftReference<CharsetEncoder> softReference = encoder.get();
        CharsetEncoder charsetEncoderOnUnmappableCharacter = softReference != null ? softReference.get() : null;
        if (charsetEncoderOnUnmappableCharacter == null) {
            charsetEncoderOnUnmappableCharacter = Util.jnuEncoding().newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            encoder.set(new SoftReference<>(charsetEncoderOnUnmappableCharacter));
        }
        char[] cArrNormalizeNativePath = unixFileSystem.normalizeNativePath(str.toCharArray());
        byte[] bArr = new byte[(int) (((double) cArrNormalizeNativePath.length) * ((double) charsetEncoderOnUnmappableCharacter.maxBytesPerChar()))];
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        CharBuffer charBufferWrap = CharBuffer.wrap(cArrNormalizeNativePath);
        charsetEncoderOnUnmappableCharacter.reset();
        boolean zIsUnderflow = true;
        if (charsetEncoderOnUnmappableCharacter.encode(charBufferWrap, byteBufferWrap, true).isUnderflow()) {
            zIsUnderflow = true ^ charsetEncoderOnUnmappableCharacter.flush(byteBufferWrap).isUnderflow();
        }
        if (zIsUnderflow) {
            throw new InvalidPathException(str, "Malformed input or input contains unmappable characters");
        }
        int iPosition = byteBufferWrap.position();
        if (iPosition != bArr.length) {
            return Arrays.copyOf(bArr, iPosition);
        }
        return bArr;
    }

    byte[] asByteArray() {
        return this.path;
    }

    byte[] getByteArrayForSysCalls() {
        if (getFileSystem().needToResolveAgainstDefaultDirectory()) {
            return resolve(getFileSystem().defaultDirectory(), this.path);
        }
        if (!isEmpty()) {
            return this.path;
        }
        return new byte[]{46};
    }

    String getPathForExceptionMessage() {
        return toString();
    }

    String getPathForPermissionCheck() {
        if (getFileSystem().needToResolveAgainstDefaultDirectory()) {
            return Util.toString(getByteArrayForSysCalls());
        }
        return toString();
    }

    static UnixPath toUnixPath(Path path) {
        if (path == null) {
            throw new NullPointerException();
        }
        if (!(path instanceof UnixPath)) {
            throw new ProviderMismatchException();
        }
        return (UnixPath) path;
    }

    private void initOffsets() {
        int i;
        int i2;
        if (this.offsets == null) {
            int i3 = 0;
            if (!isEmpty()) {
                int i4 = 0;
                for (int i5 = 0; i5 < this.path.length; i5 = i2) {
                    i2 = i5 + 1;
                    if (this.path[i5] != 47) {
                        i4++;
                        while (i2 < this.path.length && this.path[i2] != 47) {
                            i2++;
                        }
                    }
                }
                i = i4;
            } else {
                i = 1;
            }
            int[] iArr = new int[i];
            int i6 = 0;
            while (i3 < this.path.length) {
                if (this.path[i3] == 47) {
                    i3++;
                } else {
                    int i7 = i6 + 1;
                    int i8 = i3 + 1;
                    iArr[i6] = i3;
                    while (i8 < this.path.length && this.path[i8] != 47) {
                        i8++;
                    }
                    i6 = i7;
                    i3 = i8;
                }
            }
            synchronized (this) {
                if (this.offsets == null) {
                    this.offsets = iArr;
                }
            }
        }
    }

    private boolean isEmpty() {
        return this.path.length == 0;
    }

    private UnixPath emptyPath() {
        return new UnixPath(getFileSystem(), new byte[0]);
    }

    @Override
    public UnixFileSystem getFileSystem() {
        return this.fs;
    }

    @Override
    public UnixPath getRoot() {
        if (this.path.length > 0 && this.path[0] == 47) {
            return getFileSystem().rootDirectory();
        }
        return null;
    }

    @Override
    public UnixPath getFileName() {
        initOffsets();
        int length = this.offsets.length;
        if (length == 0) {
            return null;
        }
        if (length == 1 && this.path.length > 0 && this.path[0] != 47) {
            return this;
        }
        int i = this.offsets[length - 1];
        int length2 = this.path.length - i;
        byte[] bArr = new byte[length2];
        System.arraycopy(this.path, i, bArr, 0, length2);
        return new UnixPath(getFileSystem(), bArr);
    }

    @Override
    public UnixPath getParent() {
        initOffsets();
        int length = this.offsets.length;
        if (length == 0) {
            return null;
        }
        int i = this.offsets[length - 1] - 1;
        if (i <= 0) {
            return getRoot();
        }
        byte[] bArr = new byte[i];
        System.arraycopy(this.path, 0, bArr, 0, i);
        return new UnixPath(getFileSystem(), bArr);
    }

    @Override
    public int getNameCount() {
        initOffsets();
        return this.offsets.length;
    }

    @Override
    public UnixPath getName(int i) {
        int length;
        initOffsets();
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        if (i >= this.offsets.length) {
            throw new IllegalArgumentException();
        }
        int i2 = this.offsets[i];
        if (i == this.offsets.length - 1) {
            length = this.path.length - i2;
        } else {
            length = (this.offsets[i + 1] - i2) - 1;
        }
        byte[] bArr = new byte[length];
        System.arraycopy(this.path, i2, bArr, 0, length);
        return new UnixPath(getFileSystem(), bArr);
    }

    @Override
    public UnixPath subpath(int i, int i2) {
        int length;
        initOffsets();
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        if (i >= this.offsets.length) {
            throw new IllegalArgumentException();
        }
        if (i2 > this.offsets.length) {
            throw new IllegalArgumentException();
        }
        if (i >= i2) {
            throw new IllegalArgumentException();
        }
        int i3 = this.offsets[i];
        if (i2 == this.offsets.length) {
            length = this.path.length - i3;
        } else {
            length = (this.offsets[i2] - i3) - 1;
        }
        byte[] bArr = new byte[length];
        System.arraycopy(this.path, i3, bArr, 0, length);
        return new UnixPath(getFileSystem(), bArr);
    }

    @Override
    public boolean isAbsolute() {
        return this.path.length > 0 && this.path[0] == 47;
    }

    private static byte[] resolve(byte[] bArr, byte[] bArr2) {
        int length = bArr.length;
        int length2 = bArr2.length;
        if (length2 == 0) {
            return bArr;
        }
        if (length == 0 || bArr2[0] == 47) {
            return bArr2;
        }
        if (length == 1 && bArr[0] == 47) {
            byte[] bArr3 = new byte[length2 + 1];
            bArr3[0] = 47;
            System.arraycopy(bArr2, 0, bArr3, 1, length2);
            return bArr3;
        }
        int i = length + 1;
        byte[] bArr4 = new byte[i + length2];
        System.arraycopy(bArr, 0, bArr4, 0, length);
        bArr4[bArr.length] = 47;
        System.arraycopy(bArr2, 0, bArr4, i, length2);
        return bArr4;
    }

    @Override
    public UnixPath resolve(Path path) {
        byte[] bArr = toUnixPath(path).path;
        if (bArr.length > 0 && bArr[0] == 47) {
            return (UnixPath) path;
        }
        return new UnixPath(getFileSystem(), resolve(this.path, bArr));
    }

    UnixPath resolve(byte[] bArr) {
        return resolve((Path) new UnixPath(getFileSystem(), bArr));
    }

    @Override
    public UnixPath relativize(Path path) {
        UnixPath unixPath = toUnixPath(path);
        if (unixPath.equals(this)) {
            return emptyPath();
        }
        if (isAbsolute() != unixPath.isAbsolute()) {
            throw new IllegalArgumentException("'other' is different type of Path");
        }
        if (isEmpty()) {
            return unixPath;
        }
        int nameCount = getNameCount();
        int nameCount2 = unixPath.getNameCount();
        int i = nameCount > nameCount2 ? nameCount2 : nameCount;
        int i2 = 0;
        int i3 = 0;
        while (i3 < i && getName(i3).equals(unixPath.getName(i3))) {
            i3++;
        }
        int i4 = nameCount - i3;
        if (i3 < nameCount2) {
            UnixPath unixPathSubpath = unixPath.subpath(i3, nameCount2);
            if (i4 == 0) {
                return unixPathSubpath;
            }
            boolean zIsEmpty = unixPath.isEmpty();
            int length = (i4 * 3) + unixPathSubpath.path.length;
            if (zIsEmpty) {
                length--;
            }
            byte[] bArr = new byte[length];
            int i5 = 0;
            while (i4 > 0) {
                int i6 = i5 + 1;
                bArr[i5] = 46;
                i5 = i6 + 1;
                bArr[i6] = 46;
                if (!zIsEmpty || i4 > 1) {
                    int i7 = i5 + 1;
                    bArr[i5] = 47;
                    i5 = i7;
                    i4--;
                } else {
                    i4--;
                }
            }
            System.arraycopy(unixPathSubpath.path, 0, bArr, i5, unixPathSubpath.path.length);
            return new UnixPath(getFileSystem(), bArr);
        }
        byte[] bArr2 = new byte[(i4 * 3) - 1];
        while (i4 > 0) {
            int i8 = i2 + 1;
            bArr2[i2] = 46;
            i2 = i8 + 1;
            bArr2[i8] = 46;
            if (i4 > 1) {
                bArr2[i2] = 47;
                i2++;
            }
            i4--;
        }
        return new UnixPath(getFileSystem(), bArr2);
    }

    @Override
    public Path normalize() {
        int i;
        int i2;
        boolean z;
        int length;
        int nameCount = getNameCount();
        if (nameCount == 0 || isEmpty()) {
            return this;
        }
        boolean[] zArr = new boolean[nameCount];
        int[] iArr = new int[nameCount];
        boolean zIsAbsolute = isAbsolute();
        int i3 = nameCount;
        int i4 = 0;
        boolean z2 = false;
        while (true) {
            i = 1;
            if (i4 >= nameCount) {
                break;
            }
            int i5 = this.offsets[i4];
            if (i4 == this.offsets.length - 1) {
                length = this.path.length - i5;
            } else {
                length = (this.offsets[i4 + 1] - i5) - 1;
            }
            iArr[i4] = length;
            if (this.path[i5] == 46) {
                if (length == 1) {
                    zArr[i4] = true;
                    i3--;
                } else if (this.path[i5 + 1] == 46) {
                    z2 = true;
                }
            }
            i4++;
        }
        if (z2) {
            while (true) {
                int i6 = -1;
                i2 = i3;
                for (int i7 = 0; i7 < nameCount; i7++) {
                    if (!zArr[i7]) {
                        if (iArr[i7] != 2) {
                            i6 = i7;
                        } else {
                            int i8 = this.offsets[i7];
                            if (this.path[i8] == 46 && this.path[i8 + 1] == 46) {
                                if (i6 >= 0) {
                                    zArr[i6] = true;
                                    zArr[i7] = true;
                                    i2 -= 2;
                                    i6 = -1;
                                } else if (zIsAbsolute) {
                                    int i9 = 0;
                                    while (true) {
                                        if (i9 < i7) {
                                            if (zArr[i9]) {
                                                i9++;
                                            } else {
                                                z = true;
                                                break;
                                            }
                                        } else {
                                            z = false;
                                            break;
                                        }
                                    }
                                    if (!z) {
                                        zArr[i7] = true;
                                        i2--;
                                    }
                                }
                            }
                        }
                    }
                }
                if (i3 <= i2) {
                    break;
                }
                i3 = i2;
            }
            i3 = i2;
        }
        if (i3 == nameCount) {
            return this;
        }
        if (i3 == 0) {
            return zIsAbsolute ? getFileSystem().rootDirectory() : emptyPath();
        }
        int i10 = i3 - 1;
        if (zIsAbsolute) {
            i10++;
        }
        int i11 = i10;
        for (int i12 = 0; i12 < nameCount; i12++) {
            if (!zArr[i12]) {
                i11 += iArr[i12];
            }
        }
        byte[] bArr = new byte[i11];
        if (zIsAbsolute) {
            bArr[0] = 47;
        } else {
            i = 0;
        }
        for (int i13 = 0; i13 < nameCount; i13++) {
            if (!zArr[i13]) {
                System.arraycopy(this.path, this.offsets[i13], bArr, i, iArr[i13]);
                i += iArr[i13];
                i3--;
                if (i3 > 0) {
                    bArr[i] = 47;
                    i++;
                }
            }
        }
        return new UnixPath(getFileSystem(), bArr);
    }

    @Override
    public boolean startsWith(Path path) {
        if (!(Objects.requireNonNull(path) instanceof UnixPath)) {
            return false;
        }
        UnixPath unixPath = (UnixPath) path;
        if (unixPath.path.length > this.path.length) {
            return false;
        }
        int nameCount = getNameCount();
        int nameCount2 = unixPath.getNameCount();
        if (nameCount2 == 0 && isAbsolute()) {
            return !unixPath.isEmpty();
        }
        if (nameCount2 > nameCount) {
            return false;
        }
        if (nameCount2 == nameCount && this.path.length != unixPath.path.length) {
            return false;
        }
        for (int i = 0; i < nameCount2; i++) {
            if (!Integer.valueOf(this.offsets[i]).equals(Integer.valueOf(unixPath.offsets[i]))) {
                return false;
            }
        }
        int i2 = 0;
        while (i2 < unixPath.path.length) {
            if (this.path[i2] != unixPath.path[i2]) {
                return false;
            }
            i2++;
        }
        return i2 >= this.path.length || this.path[i2] == 47;
    }

    @Override
    public boolean endsWith(Path path) {
        int nameCount;
        int nameCount2;
        int i;
        if (!(Objects.requireNonNull(path) instanceof UnixPath)) {
            return false;
        }
        UnixPath unixPath = (UnixPath) path;
        int length = this.path.length;
        int length2 = unixPath.path.length;
        if (length2 > length) {
            return false;
        }
        if (length > 0 && length2 == 0) {
            return false;
        }
        if ((unixPath.isAbsolute() && !isAbsolute()) || (nameCount2 = unixPath.getNameCount()) > (nameCount = getNameCount())) {
            return false;
        }
        if (nameCount2 == nameCount) {
            if (nameCount == 0) {
                return true;
            }
            if (isAbsolute() && !unixPath.isAbsolute()) {
                i = length - 1;
            } else {
                i = length;
            }
            if (length2 != i) {
                return false;
            }
        } else if (unixPath.isAbsolute()) {
            return false;
        }
        int i2 = this.offsets[nameCount - nameCount2];
        int i3 = unixPath.offsets[0];
        if (length2 - i3 != length - i2) {
            return false;
        }
        while (i3 < length2) {
            int i4 = i2 + 1;
            int i5 = i3 + 1;
            if (this.path[i2] != unixPath.path[i3]) {
                return false;
            }
            i2 = i4;
            i3 = i5;
        }
        return true;
    }

    @Override
    public int compareTo(Path path) {
        int length = this.path.length;
        UnixPath unixPath = (UnixPath) path;
        int length2 = unixPath.path.length;
        int iMin = Math.min(length, length2);
        byte[] bArr = this.path;
        byte[] bArr2 = unixPath.path;
        for (int i = 0; i < iMin; i++) {
            int i2 = bArr[i] & Character.DIRECTIONALITY_UNDEFINED;
            int i3 = bArr2[i] & Character.DIRECTIONALITY_UNDEFINED;
            if (i2 != i3) {
                return i2 - i3;
            }
        }
        return length - length2;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && (obj instanceof UnixPath) && compareTo((Path) obj) == 0;
    }

    @Override
    public int hashCode() {
        int i = this.hash;
        if (i == 0) {
            for (int i2 = 0; i2 < this.path.length; i2++) {
                i = (this.path[i2] & Character.DIRECTIONALITY_UNDEFINED) + (31 * i);
            }
            this.hash = i;
        }
        return i;
    }

    @Override
    public String toString() {
        if (this.stringValue == null) {
            this.stringValue = this.fs.normalizeJavaPath(Util.toString(this.path));
        }
        return this.stringValue;
    }

    int openForAttributeAccess(boolean z) throws IOException {
        int i = UnixConstants.O_RDONLY;
        if (!z) {
            if (UnixConstants.O_NOFOLLOW == 0) {
                throw new IOException("NOFOLLOW_LINKS is not supported on this platform");
            }
            i |= UnixConstants.O_NOFOLLOW;
        }
        try {
            return UnixNativeDispatcher.open(this, i, 0);
        } catch (UnixException e) {
            if (getFileSystem().isSolaris() && e.errno() == UnixConstants.EINVAL) {
                e.setError(UnixConstants.ELOOP);
            }
            if (e.errno() == UnixConstants.ELOOP) {
                throw new FileSystemException(getPathForExceptionMessage(), null, e.getMessage() + " or unable to access attributes of symbolic link");
            }
            e.rethrowAsIOException(this);
            return -1;
        }
    }

    void checkRead() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(getPathForPermissionCheck());
        }
    }

    void checkWrite() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(getPathForPermissionCheck());
        }
    }

    void checkDelete() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkDelete(getPathForPermissionCheck());
        }
    }

    @Override
    public UnixPath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPropertyAccess("user.dir");
        }
        return new UnixPath(getFileSystem(), resolve(getFileSystem().defaultDirectory(), this.path));
    }

    @Override
    public Path toRealPath(LinkOption... linkOptionArr) throws IOException, UnixException {
        checkRead();
        UnixPath absolutePath = toAbsolutePath();
        if (Util.followLinks(linkOptionArr)) {
            try {
                return new UnixPath(getFileSystem(), UnixNativeDispatcher.realpath(absolutePath));
            } catch (UnixException e) {
                e.rethrowAsIOException(this);
            }
        }
        UnixPath unixPathRootDirectory = this.fs.rootDirectory();
        for (int i = 0; i < absolutePath.getNameCount(); i++) {
            UnixPath name = absolutePath.getName(i);
            if (name.asByteArray().length != 1 || name.asByteArray()[0] != 46) {
                if (name.asByteArray().length == 2 && name.asByteArray()[0] == 46 && name.asByteArray()[1] == 46) {
                    UnixFileAttributes unixFileAttributes = null;
                    try {
                        unixFileAttributes = UnixFileAttributes.get(unixPathRootDirectory, false);
                    } catch (UnixException e2) {
                        e2.rethrowAsIOException(unixPathRootDirectory);
                    }
                    if (!unixFileAttributes.isSymbolicLink()) {
                        unixPathRootDirectory = unixPathRootDirectory.getParent();
                        if (unixPathRootDirectory == null) {
                            unixPathRootDirectory = this.fs.rootDirectory();
                        }
                    }
                } else {
                    unixPathRootDirectory = unixPathRootDirectory.resolve((Path) name);
                }
            }
        }
        try {
            UnixFileAttributes.get(unixPathRootDirectory, false);
        } catch (UnixException e3) {
            e3.rethrowAsIOException(unixPathRootDirectory);
        }
        return unixPathRootDirectory;
    }

    @Override
    public URI toUri() {
        return UnixUriUtils.toUri(this);
    }

    @Override
    public WatchKey register(WatchService watchService, WatchEvent.Kind<?>[] kindArr, WatchEvent.Modifier... modifierArr) throws IOException {
        if (watchService == null) {
            throw new NullPointerException();
        }
        if (!(watchService instanceof AbstractWatchService)) {
            throw new ProviderMismatchException();
        }
        checkRead();
        return ((AbstractWatchService) watchService).register(this, kindArr, modifierArr);
    }
}

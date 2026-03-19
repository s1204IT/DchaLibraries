package dalvik.system;

import android.system.ErrnoException;
import android.system.OsConstants;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import libcore.io.ClassPathURLStreamHandler;
import libcore.io.IoUtils;
import libcore.io.Libcore;

final class DexPathList {
    private static final String DEX_SUFFIX = ".dex";
    private static final String zipSeparator = "!/";
    private final ClassLoader definingContext;
    private Element[] dexElements;
    private IOException[] dexElementsSuppressedExceptions;
    private final List<File> nativeLibraryDirectories;
    NativeLibraryElement[] nativeLibraryPathElements;
    private final List<File> systemNativeLibraryDirectories;

    public DexPathList(ClassLoader classLoader, ByteBuffer[] byteBufferArr) {
        if (classLoader == null) {
            throw new NullPointerException("definingContext == null");
        }
        if (byteBufferArr == null) {
            throw new NullPointerException("dexFiles == null");
        }
        if (Arrays.stream(byteBufferArr).anyMatch(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DexPathList.lambda$new$0((ByteBuffer) obj);
            }
        })) {
            throw new NullPointerException("dexFiles contains a null Buffer!");
        }
        this.definingContext = classLoader;
        this.nativeLibraryDirectories = Collections.emptyList();
        this.systemNativeLibraryDirectories = splitPaths(System.getProperty("java.library.path"), true);
        this.nativeLibraryPathElements = makePathElements(this.systemNativeLibraryDirectories);
        ArrayList arrayList = new ArrayList();
        this.dexElements = makeInMemoryDexElements(byteBufferArr, arrayList);
        if (arrayList.size() > 0) {
            this.dexElementsSuppressedExceptions = (IOException[]) arrayList.toArray(new IOException[arrayList.size()]);
        } else {
            this.dexElementsSuppressedExceptions = null;
        }
    }

    static boolean lambda$new$0(ByteBuffer byteBuffer) {
        return byteBuffer == null;
    }

    public DexPathList(ClassLoader classLoader, String str, String str2, File file) {
        this(classLoader, str, str2, file, false);
    }

    DexPathList(ClassLoader classLoader, String str, String str2, File file, boolean z) {
        if (classLoader == null) {
            throw new NullPointerException("definingContext == null");
        }
        if (str == null) {
            throw new NullPointerException("dexPath == null");
        }
        if (file != null) {
            if (!file.exists()) {
                throw new IllegalArgumentException("optimizedDirectory doesn't exist: " + file);
            }
            if (!file.canRead() || !file.canWrite()) {
                throw new IllegalArgumentException("optimizedDirectory not readable/writable: " + file);
            }
        }
        this.definingContext = classLoader;
        ArrayList arrayList = new ArrayList();
        this.dexElements = makeDexElements(splitDexPath(str), file, arrayList, classLoader, z);
        this.nativeLibraryDirectories = splitPaths(str2, false);
        this.systemNativeLibraryDirectories = splitPaths(System.getProperty("java.library.path"), true);
        ArrayList arrayList2 = new ArrayList(this.nativeLibraryDirectories);
        arrayList2.addAll(this.systemNativeLibraryDirectories);
        this.nativeLibraryPathElements = makePathElements(arrayList2);
        if (arrayList.size() > 0) {
            this.dexElementsSuppressedExceptions = (IOException[]) arrayList.toArray(new IOException[arrayList.size()]);
        } else {
            this.dexElementsSuppressedExceptions = null;
        }
    }

    public String toString() {
        ArrayList arrayList = new ArrayList(this.nativeLibraryDirectories);
        arrayList.addAll(this.systemNativeLibraryDirectories);
        return "DexPathList[" + Arrays.toString(this.dexElements) + ",nativeLibraryDirectories=" + Arrays.toString((File[]) arrayList.toArray(new File[arrayList.size()])) + "]";
    }

    public List<File> getNativeLibraryDirectories() {
        return this.nativeLibraryDirectories;
    }

    public void addDexPath(String str, File file) {
        addDexPath(str, file, false);
    }

    public void addDexPath(String str, File file, boolean z) {
        ArrayList arrayList = new ArrayList();
        Element[] elementArrMakeDexElements = makeDexElements(splitDexPath(str), file, arrayList, this.definingContext, z);
        if (elementArrMakeDexElements != null && elementArrMakeDexElements.length > 0) {
            Element[] elementArr = this.dexElements;
            this.dexElements = new Element[elementArr.length + elementArrMakeDexElements.length];
            System.arraycopy(elementArr, 0, this.dexElements, 0, elementArr.length);
            System.arraycopy(elementArrMakeDexElements, 0, this.dexElements, elementArr.length, elementArrMakeDexElements.length);
        }
        if (arrayList.size() > 0) {
            IOException[] iOExceptionArr = (IOException[]) arrayList.toArray(new IOException[arrayList.size()]);
            if (this.dexElementsSuppressedExceptions != null) {
                IOException[] iOExceptionArr2 = this.dexElementsSuppressedExceptions;
                this.dexElementsSuppressedExceptions = new IOException[iOExceptionArr2.length + iOExceptionArr.length];
                System.arraycopy(iOExceptionArr2, 0, this.dexElementsSuppressedExceptions, 0, iOExceptionArr2.length);
                System.arraycopy(iOExceptionArr, 0, this.dexElementsSuppressedExceptions, iOExceptionArr2.length, iOExceptionArr.length);
                return;
            }
            this.dexElementsSuppressedExceptions = iOExceptionArr;
        }
    }

    private static List<File> splitDexPath(String str) {
        return splitPaths(str, false);
    }

    private static List<File> splitPaths(String str, boolean z) {
        ArrayList arrayList = new ArrayList();
        if (str != null) {
            for (String str2 : str.split(File.pathSeparator)) {
                if (z) {
                    try {
                        if (OsConstants.S_ISDIR(Libcore.os.stat(str2).st_mode)) {
                            arrayList.add(new File(str2));
                        }
                    } catch (ErrnoException e) {
                    }
                }
            }
        }
        return arrayList;
    }

    private static Element[] makeInMemoryDexElements(ByteBuffer[] byteBufferArr, List<IOException> list) {
        int i;
        IOException e;
        Element[] elementArr = new Element[byteBufferArr.length];
        int i2 = 0;
        for (ByteBuffer byteBuffer : byteBufferArr) {
            try {
                i = i2 + 1;
                try {
                    elementArr[i2] = new Element(new DexFile(byteBuffer));
                } catch (IOException e2) {
                    e = e2;
                    System.logE("Unable to load dex file: " + byteBuffer, e);
                    list.add(e);
                }
            } catch (IOException e3) {
                i = i2;
                e = e3;
            }
            i2 = i;
        }
        if (i2 != elementArr.length) {
            return (Element[]) Arrays.copyOf(elementArr, i2);
        }
        return elementArr;
    }

    private static Element[] makeDexElements(List<File> list, File file, List<IOException> list2, ClassLoader classLoader) {
        return makeDexElements(list, file, list2, classLoader, false);
    }

    private static Element[] makeDexElements(List<File> list, File file, List<IOException> list2, ClassLoader classLoader, boolean z) {
        int i;
        IOException e;
        DexFile dexFileLoadDexFile;
        int i2;
        Element[] elementArr = new Element[list.size()];
        int i3 = 0;
        for (File file2 : list) {
            if (file2.isDirectory()) {
                elementArr[i3] = new Element(file2);
                i3++;
            } else if (file2.isFile()) {
                if (file2.getName().endsWith(DEX_SUFFIX)) {
                    try {
                        dexFileLoadDexFile = loadDexFile(file2, file, classLoader, elementArr);
                        if (dexFileLoadDexFile != null) {
                            i = i3 + 1;
                            try {
                                elementArr[i3] = new Element(dexFileLoadDexFile, null);
                                i3 = i;
                            } catch (IOException e2) {
                                e = e2;
                                System.logE("Unable to load dex file: " + file2, e);
                                list2.add(e);
                                i2 = i;
                            }
                        }
                        i2 = i3;
                    } catch (IOException e3) {
                        i = i3;
                        e = e3;
                        dexFileLoadDexFile = null;
                    }
                } else {
                    try {
                        dexFileLoadDexFile = loadDexFile(file2, file, classLoader, elementArr);
                    } catch (IOException e4) {
                        list2.add(e4);
                        dexFileLoadDexFile = null;
                    }
                    if (dexFileLoadDexFile == null) {
                        i2 = i3 + 1;
                        elementArr[i3] = new Element(file2);
                    } else {
                        i2 = i3 + 1;
                        elementArr[i3] = new Element(dexFileLoadDexFile, file2);
                    }
                }
                if (dexFileLoadDexFile != null && z) {
                    dexFileLoadDexFile.setTrusted();
                }
                i3 = i2;
            } else {
                System.logW("ClassLoader referenced unknown path: " + file2);
            }
        }
        if (i3 != elementArr.length) {
            return (Element[]) Arrays.copyOf(elementArr, i3);
        }
        return elementArr;
    }

    private static DexFile loadDexFile(File file, File file2, ClassLoader classLoader, Element[] elementArr) throws IOException {
        if (file2 == null) {
            return new DexFile(file, classLoader, elementArr);
        }
        return DexFile.loadDex(file.getPath(), optimizedPathFor(file, file2), 0, classLoader, elementArr);
    }

    private static String optimizedPathFor(File file, File file2) {
        String name = file.getName();
        if (!name.endsWith(DEX_SUFFIX)) {
            int iLastIndexOf = name.lastIndexOf(".");
            if (iLastIndexOf < 0) {
                name = name + DEX_SUFFIX;
            } else {
                StringBuilder sb = new StringBuilder(iLastIndexOf + 4);
                sb.append((CharSequence) name, 0, iLastIndexOf);
                sb.append(DEX_SUFFIX);
                name = sb.toString();
            }
        }
        return new File(file2, name).getPath();
    }

    private static Element[] makePathElements(List<File> list, File file, List<IOException> list2) {
        return makeDexElements(list, file, list2, null);
    }

    private static NativeLibraryElement[] makePathElements(List<File> list) {
        NativeLibraryElement[] nativeLibraryElementArr = new NativeLibraryElement[list.size()];
        int i = 0;
        for (File file : list) {
            String path = file.getPath();
            if (path.contains(zipSeparator)) {
                String[] strArrSplit = path.split(zipSeparator, 2);
                nativeLibraryElementArr[i] = new NativeLibraryElement(new File(strArrSplit[0]), strArrSplit[1]);
                i++;
            } else if (file.isDirectory()) {
                nativeLibraryElementArr[i] = new NativeLibraryElement(file);
                i++;
            }
        }
        if (i != nativeLibraryElementArr.length) {
            return (NativeLibraryElement[]) Arrays.copyOf(nativeLibraryElementArr, i);
        }
        return nativeLibraryElementArr;
    }

    public Class<?> findClass(String str, List<Throwable> list) {
        for (Element element : this.dexElements) {
            Class<?> clsFindClass = element.findClass(str, this.definingContext, list);
            if (clsFindClass != null) {
                return clsFindClass;
            }
        }
        if (this.dexElementsSuppressedExceptions != null) {
            list.addAll(Arrays.asList(this.dexElementsSuppressedExceptions));
            return null;
        }
        return null;
    }

    public URL findResource(String str) {
        for (Element element : this.dexElements) {
            URL urlFindResource = element.findResource(str);
            if (urlFindResource != null) {
                return urlFindResource;
            }
        }
        return null;
    }

    public Enumeration<URL> findResources(String str) {
        ArrayList arrayList = new ArrayList();
        for (Element element : this.dexElements) {
            URL urlFindResource = element.findResource(str);
            if (urlFindResource != null) {
                arrayList.add(urlFindResource);
            }
        }
        return Collections.enumeration(arrayList);
    }

    public String findLibrary(String str) {
        String strMapLibraryName = System.mapLibraryName(str);
        for (NativeLibraryElement nativeLibraryElement : this.nativeLibraryPathElements) {
            String strFindNativeLibrary = nativeLibraryElement.findNativeLibrary(strMapLibraryName);
            if (strFindNativeLibrary != null) {
                return strFindNativeLibrary;
            }
        }
        return null;
    }

    List<String> getDexPaths() {
        ArrayList arrayList = new ArrayList();
        for (Element element : this.dexElements) {
            String dexPath = element.getDexPath();
            if (dexPath != null) {
                arrayList.add(dexPath);
            }
        }
        return arrayList;
    }

    public void addNativePath(Collection<String> collection) {
        if (collection.isEmpty()) {
            return;
        }
        ArrayList arrayList = new ArrayList(collection.size());
        Iterator<String> it = collection.iterator();
        while (it.hasNext()) {
            arrayList.add(new File(it.next()));
        }
        ArrayList arrayList2 = new ArrayList(this.nativeLibraryPathElements.length + collection.size());
        arrayList2.addAll(Arrays.asList(this.nativeLibraryPathElements));
        for (NativeLibraryElement nativeLibraryElement : makePathElements(arrayList)) {
            if (!arrayList2.contains(nativeLibraryElement)) {
                arrayList2.add(nativeLibraryElement);
            }
        }
        this.nativeLibraryPathElements = (NativeLibraryElement[]) arrayList2.toArray(new NativeLibraryElement[arrayList2.size()]);
    }

    static class Element {
        private final DexFile dexFile;
        private boolean initialized;
        private final File path;
        private final Boolean pathIsDirectory;
        private ClassPathURLStreamHandler urlHandler;

        public Element(DexFile dexFile, File file) {
            this.dexFile = dexFile;
            this.path = file;
            this.pathIsDirectory = this.path == null ? null : Boolean.valueOf(this.path.isDirectory());
        }

        public Element(DexFile dexFile) {
            this(dexFile, null);
        }

        public Element(File file) {
            this(null, file);
        }

        @Deprecated
        public Element(File file, boolean z, File file2, DexFile dexFile) {
            System.err.println("Warning: Using deprecated Element constructor. Do not use internal APIs, this constructor will be removed in the future.");
            if (file != null && (file2 != null || dexFile != null)) {
                throw new IllegalArgumentException("Using dir and zip|dexFile no longer supported.");
            }
            if (z && (file2 != null || dexFile != null)) {
                throw new IllegalArgumentException("Unsupported argument combination.");
            }
            if (file != null) {
                this.path = file;
                this.dexFile = null;
            } else {
                this.path = file2;
                this.dexFile = dexFile;
            }
            this.pathIsDirectory = this.path != null ? Boolean.valueOf(this.path.isDirectory()) : null;
        }

        private String getDexPath() {
            if (this.path != null) {
                if (this.path.isDirectory()) {
                    return null;
                }
                return this.path.getAbsolutePath();
            }
            if (this.dexFile != null) {
                return this.dexFile.getName();
            }
            return null;
        }

        public String toString() {
            if (this.dexFile == null) {
                StringBuilder sb = new StringBuilder();
                sb.append(this.pathIsDirectory.booleanValue() ? "directory \"" : "zip file \"");
                sb.append(this.path);
                sb.append("\"");
                return sb.toString();
            }
            if (this.path == null) {
                return "dex file \"" + this.dexFile + "\"";
            }
            return "zip file \"" + this.path + "\"";
        }

        public synchronized void maybeInit() {
            if (this.initialized) {
                return;
            }
            if (this.path == null || this.pathIsDirectory.booleanValue()) {
                this.initialized = true;
                return;
            }
            try {
                this.urlHandler = new ClassPathURLStreamHandler(this.path.getPath());
            } catch (IOException e) {
                System.logE("Unable to open zip file: " + this.path, e);
                this.urlHandler = null;
            }
            this.initialized = true;
        }

        public Class<?> findClass(String str, ClassLoader classLoader, List<Throwable> list) {
            if (this.dexFile != null) {
                return this.dexFile.loadClassBinaryName(str, classLoader, list);
            }
            return null;
        }

        public URL findResource(String str) {
            maybeInit();
            if (this.urlHandler != null) {
                return this.urlHandler.getEntryUrlOrNull(str);
            }
            if (this.path != null && this.path.isDirectory()) {
                File file = new File(this.path, str);
                if (file.exists()) {
                    try {
                        return file.toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }
            return null;
        }
    }

    static class NativeLibraryElement {
        private boolean initialized;
        private final File path;
        private ClassPathURLStreamHandler urlHandler;
        private final String zipDir;

        public NativeLibraryElement(File file) {
            this.path = file;
            this.zipDir = null;
        }

        public NativeLibraryElement(File file, String str) {
            this.path = file;
            this.zipDir = str;
            if (str == null) {
                throw new IllegalArgumentException();
            }
        }

        public String toString() {
            String str;
            if (this.zipDir == null) {
                return "directory \"" + this.path + "\"";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("zip file \"");
            sb.append(this.path);
            sb.append("\"");
            if (this.zipDir.isEmpty()) {
                str = "";
            } else {
                str = ", dir \"" + this.zipDir + "\"";
            }
            sb.append(str);
            return sb.toString();
        }

        public synchronized void maybeInit() {
            if (this.initialized) {
                return;
            }
            if (this.zipDir == null) {
                this.initialized = true;
                return;
            }
            try {
                this.urlHandler = new ClassPathURLStreamHandler(this.path.getPath());
            } catch (IOException e) {
                System.logE("Unable to open zip file: " + this.path, e);
                this.urlHandler = null;
            }
            this.initialized = true;
        }

        public String findNativeLibrary(String str) {
            maybeInit();
            if (this.zipDir == null) {
                String path = new File(this.path, str).getPath();
                if (IoUtils.canOpenReadOnly(path)) {
                    return path;
                }
                return null;
            }
            if (this.urlHandler != null) {
                String str2 = this.zipDir + '/' + str;
                if (this.urlHandler.isEntryStored(str2)) {
                    return this.path.getPath() + DexPathList.zipSeparator + str2;
                }
                return null;
            }
            return null;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NativeLibraryElement)) {
                return false;
            }
            NativeLibraryElement nativeLibraryElement = (NativeLibraryElement) obj;
            return Objects.equals(this.path, nativeLibraryElement.path) && Objects.equals(this.zipDir, nativeLibraryElement.zipDir);
        }

        public int hashCode() {
            return Objects.hash(this.path, this.zipDir);
        }
    }
}

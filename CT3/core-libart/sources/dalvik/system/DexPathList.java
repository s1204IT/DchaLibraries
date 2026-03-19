package dalvik.system;

import android.system.ErrnoException;
import android.system.OsConstants;
import android.system.StructStat;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
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
    private final Element[] nativeLibraryPathElements;
    private final List<File> systemNativeLibraryDirectories;

    public DexPathList(ClassLoader definingContext, String dexPath, String librarySearchPath, File optimizedDirectory) {
        if (definingContext == null) {
            throw new NullPointerException("definingContext == null");
        }
        if (dexPath == null) {
            throw new NullPointerException("dexPath == null");
        }
        if (optimizedDirectory != null) {
            if (!optimizedDirectory.exists()) {
                throw new IllegalArgumentException("optimizedDirectory doesn't exist: " + optimizedDirectory);
            }
            if (!(optimizedDirectory.canRead() ? optimizedDirectory.canWrite() : false)) {
                throw new IllegalArgumentException("optimizedDirectory not readable/writable: " + optimizedDirectory);
            }
        }
        this.definingContext = definingContext;
        ArrayList<IOException> suppressedExceptions = new ArrayList<>();
        this.dexElements = makeDexElements(splitDexPath(dexPath), optimizedDirectory, suppressedExceptions, definingContext);
        this.nativeLibraryDirectories = splitPaths(librarySearchPath, false);
        this.systemNativeLibraryDirectories = splitPaths(System.getProperty("java.library.path"), true);
        List<File> allNativeLibraryDirectories = new ArrayList<>(this.nativeLibraryDirectories);
        allNativeLibraryDirectories.addAll(this.systemNativeLibraryDirectories);
        this.nativeLibraryPathElements = makePathElements(allNativeLibraryDirectories, suppressedExceptions, definingContext);
        if (suppressedExceptions.size() > 0) {
            this.dexElementsSuppressedExceptions = (IOException[]) suppressedExceptions.toArray(new IOException[suppressedExceptions.size()]);
        } else {
            this.dexElementsSuppressedExceptions = null;
        }
    }

    public String toString() {
        List<File> allNativeLibraryDirectories = new ArrayList<>(this.nativeLibraryDirectories);
        allNativeLibraryDirectories.addAll(this.systemNativeLibraryDirectories);
        File[] nativeLibraryDirectoriesArray = (File[]) allNativeLibraryDirectories.toArray(new File[allNativeLibraryDirectories.size()]);
        return "DexPathList[" + Arrays.toString(this.dexElements) + ",nativeLibraryDirectories=" + Arrays.toString(nativeLibraryDirectoriesArray) + "]";
    }

    public List<File> getNativeLibraryDirectories() {
        return this.nativeLibraryDirectories;
    }

    public void addDexPath(String dexPath, File optimizedDirectory) {
        List<IOException> suppressedExceptionList = new ArrayList<>();
        Element[] newElements = makeDexElements(splitDexPath(dexPath), optimizedDirectory, suppressedExceptionList, this.definingContext);
        if (newElements != null && newElements.length > 0) {
            Element[] oldElements = this.dexElements;
            this.dexElements = new Element[oldElements.length + newElements.length];
            System.arraycopy(oldElements, 0, this.dexElements, 0, oldElements.length);
            System.arraycopy(newElements, 0, this.dexElements, oldElements.length, newElements.length);
        }
        if (suppressedExceptionList.size() <= 0) {
            return;
        }
        IOException[] newSuppressedExceptions = (IOException[]) suppressedExceptionList.toArray(new IOException[suppressedExceptionList.size()]);
        if (this.dexElementsSuppressedExceptions != null) {
            IOException[] oldSuppressedExceptions = this.dexElementsSuppressedExceptions;
            int suppressedExceptionsLength = oldSuppressedExceptions.length + newSuppressedExceptions.length;
            this.dexElementsSuppressedExceptions = new IOException[suppressedExceptionsLength];
            System.arraycopy(oldSuppressedExceptions, 0, this.dexElementsSuppressedExceptions, 0, oldSuppressedExceptions.length);
            System.arraycopy(newSuppressedExceptions, 0, this.dexElementsSuppressedExceptions, oldSuppressedExceptions.length, newSuppressedExceptions.length);
            return;
        }
        this.dexElementsSuppressedExceptions = newSuppressedExceptions;
    }

    private static List<File> splitDexPath(String path) {
        return splitPaths(path, false);
    }

    private static List<File> splitPaths(String searchPath, boolean directoriesOnly) {
        List<File> result = new ArrayList<>();
        if (searchPath != null) {
            for (String path : searchPath.split(File.pathSeparator)) {
                if (directoriesOnly) {
                    try {
                        StructStat sb = Libcore.os.stat(path);
                        if (OsConstants.S_ISDIR(sb.st_mode)) {
                            result.add(new File(path));
                        }
                    } catch (ErrnoException e) {
                    }
                }
            }
        }
        return result;
    }

    private static Element[] makeDexElements(List<File> files, File optimizedDirectory, List<IOException> suppressedExceptions, ClassLoader loader) {
        return makeElements(files, optimizedDirectory, suppressedExceptions, false, loader);
    }

    private static Element[] makePathElements(List<File> files, List<IOException> suppressedExceptions, ClassLoader loader) {
        return makeElements(files, null, suppressedExceptions, true, loader);
    }

    private static Element[] makePathElements(List<File> files, File optimizedDirectory, List<IOException> suppressedExceptions) {
        return makeElements(files, optimizedDirectory, suppressedExceptions, false, null);
    }

    private static Element[] makeElements(List<File> files, File optimizedDirectory, List<IOException> suppressedExceptions, boolean ignoreDexFiles, ClassLoader loader) {
        int elementsPos;
        Element[] elements = new Element[files.size()];
        int elementsPos2 = 0;
        for (File file : files) {
            File zip = null;
            File dir = new File("");
            DexFile dex = null;
            String path = file.getPath();
            String name = file.getName();
            if (path.contains(zipSeparator)) {
                String[] split = path.split(zipSeparator, 2);
                zip = new File(split[0]);
                dir = new File(split[1]);
                elementsPos = elementsPos2;
            } else if (file.isDirectory()) {
                elementsPos = elementsPos2 + 1;
                elements[elementsPos2] = new Element(file, true, null, null);
            } else if (file.isFile()) {
                if (!ignoreDexFiles && name.endsWith(DEX_SUFFIX)) {
                    try {
                        dex = loadDexFile(file, optimizedDirectory, loader, elements);
                        elementsPos = elementsPos2;
                    } catch (IOException suppressed) {
                        System.logE("Unable to load dex file: " + file, suppressed);
                        suppressedExceptions.add(suppressed);
                        elementsPos = elementsPos2;
                    }
                } else {
                    zip = file;
                    if (ignoreDexFiles) {
                        elementsPos = elementsPos2;
                    } else {
                        try {
                            dex = loadDexFile(file, optimizedDirectory, loader, elements);
                            elementsPos = elementsPos2;
                        } catch (IOException suppressed2) {
                            suppressedExceptions.add(suppressed2);
                            elementsPos = elementsPos2;
                        }
                    }
                }
            } else {
                System.logW("ClassLoader referenced unknown path: " + file);
                elementsPos = elementsPos2;
            }
            if (zip == null && dex == null) {
                elementsPos2 = elementsPos;
            } else {
                elementsPos2 = elementsPos + 1;
                elements[elementsPos] = new Element(dir, false, zip, dex);
            }
        }
        if (elementsPos2 != elements.length) {
            return (Element[]) Arrays.copyOf(elements, elementsPos2);
        }
        return elements;
    }

    private static DexFile loadDexFile(File file, File optimizedDirectory, ClassLoader loader, Element[] elements) throws IOException {
        if (optimizedDirectory == null) {
            return new DexFile(file, loader, elements);
        }
        String optimizedPath = optimizedPathFor(file, optimizedDirectory);
        return DexFile.loadDex(file.getPath(), optimizedPath, 0, loader, elements);
    }

    private static String optimizedPathFor(File path, File optimizedDirectory) {
        String fileName = path.getName();
        if (!fileName.endsWith(DEX_SUFFIX)) {
            int lastDot = fileName.lastIndexOf(".");
            if (lastDot < 0) {
                fileName = fileName + DEX_SUFFIX;
            } else {
                StringBuilder sb = new StringBuilder(lastDot + 4);
                sb.append((CharSequence) fileName, 0, lastDot);
                sb.append(DEX_SUFFIX);
                fileName = sb.toString();
            }
        }
        File result = new File(optimizedDirectory, fileName);
        return result.getPath();
    }

    public Class findClass(String name, List<Throwable> suppressed) {
        Class clazz;
        for (Element element : this.dexElements) {
            DexFile dex = element.dexFile;
            if (dex != null && (clazz = dex.loadClassBinaryName(name, this.definingContext, suppressed)) != null) {
                return clazz;
            }
        }
        if (this.dexElementsSuppressedExceptions != null) {
            suppressed.addAll(Arrays.asList(this.dexElementsSuppressedExceptions));
        }
        return null;
    }

    public URL findResource(String name) {
        for (Element element : this.dexElements) {
            URL url = element.findResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    public Enumeration<URL> findResources(String name) {
        ArrayList<URL> result = new ArrayList<>();
        for (Element element : this.dexElements) {
            URL url = element.findResource(name);
            if (url != null) {
                result.add(url);
            }
        }
        return Collections.enumeration(result);
    }

    public String findLibrary(String libraryName) {
        String fileName = System.mapLibraryName(libraryName);
        for (Element element : this.nativeLibraryPathElements) {
            String path = element.findNativeLibrary(fileName);
            if (path != null) {
                return path;
            }
        }
        return null;
    }

    static class Element {
        private final DexFile dexFile;
        private final File dir;
        private boolean initialized;
        private final boolean isDirectory;
        private ClassPathURLStreamHandler urlHandler;
        private final File zip;

        public Element(File dir, boolean isDirectory, File zip, DexFile dexFile) {
            this.dir = dir;
            this.isDirectory = isDirectory;
            this.zip = zip;
            this.dexFile = dexFile;
        }

        public String toString() {
            if (this.isDirectory) {
                return "directory \"" + this.dir + "\"";
            }
            if (this.zip != null) {
                return "zip file \"" + this.zip + "\"" + ((this.dir == null || this.dir.getPath().isEmpty()) ? "" : ", dir \"" + this.dir + "\"");
            }
            return "dex file \"" + this.dexFile + "\"";
        }

        public synchronized void maybeInit() {
            if (this.initialized) {
                return;
            }
            this.initialized = true;
            if (this.isDirectory || this.zip == null) {
                return;
            }
            try {
                this.urlHandler = new ClassPathURLStreamHandler(this.zip.getPath());
            } catch (IOException ioe) {
                System.logE("Unable to open zip file: " + this.zip, ioe);
                this.urlHandler = null;
            }
        }

        public String findNativeLibrary(String name) {
            maybeInit();
            if (this.isDirectory) {
                String path = new File(this.dir, name).getPath();
                if (IoUtils.canOpenReadOnly(path)) {
                    return path;
                }
            } else if (this.urlHandler != null) {
                String entryName = new File(this.dir, name).getPath();
                if (this.urlHandler.isEntryStored(entryName)) {
                    return this.zip.getPath() + DexPathList.zipSeparator + entryName;
                }
            }
            return null;
        }

        public URL findResource(String name) {
            maybeInit();
            if (this.isDirectory) {
                File resourceFile = new File(this.dir, name);
                if (resourceFile.exists()) {
                    try {
                        return resourceFile.toURI().toURL();
                    } catch (MalformedURLException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            if (this.urlHandler == null) {
                return null;
            }
            return this.urlHandler.getEntryUrlOrNull(name);
        }
    }
}

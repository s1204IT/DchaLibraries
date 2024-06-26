package android.support.v4.content;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class FileProvider extends ContentProvider {
    private static final String[] COLUMNS = {"_display_name", "_size"};
    private static final File DEVICE_ROOT = new File("/");
    private static HashMap<String, PathStrategy> sCache = new HashMap<>();
    private PathStrategy mStrategy;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface PathStrategy {
        File getFileForUri(Uri uri);

        Uri getUriForFile(File file);
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        return true;
    }

    @Override // android.content.ContentProvider
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        if (info.exported) {
            throw new SecurityException("Provider must not be exported");
        }
        if (!info.grantUriPermissions) {
            throw new SecurityException("Provider must grant uri permissions");
        }
        this.mStrategy = getPathStrategy(context, info.authority);
    }

    public static Uri getUriForFile(Context context, String authority, File file) {
        PathStrategy strategy = getPathStrategy(context, authority);
        return strategy.getUriForFile(file);
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int i;
        File file = this.mStrategy.getFileForUri(uri);
        if (projection == null) {
            projection = COLUMNS;
        }
        String[] cols = new String[projection.length];
        Object[] values = new Object[projection.length];
        int i2 = 0;
        int length = projection.length;
        int i3 = 0;
        while (i2 < length) {
            String col = projection[i2];
            if ("_display_name".equals(col)) {
                cols[i3] = "_display_name";
                i = i3 + 1;
                values[i3] = file.getName();
            } else if ("_size".equals(col)) {
                cols[i3] = "_size";
                i = i3 + 1;
                values[i3] = Long.valueOf(file.length());
            } else {
                i = i3;
            }
            i2++;
            i3 = i;
        }
        String[] cols2 = copyOf(cols, i3);
        Object[] values2 = copyOf(values, i3);
        MatrixCursor cursor = new MatrixCursor(cols2, 1);
        cursor.addRow(values2);
        return cursor;
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        File file = this.mStrategy.getFileForUri(uri);
        int lastDot = file.getName().lastIndexOf(46);
        if (lastDot >= 0) {
            String extension = file.getName().substring(lastDot + 1);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
            return "application/octet-stream";
        }
        return "application/octet-stream";
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("No external inserts");
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("No external updates");
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        File file = this.mStrategy.getFileForUri(uri);
        return file.delete() ? 1 : 0;
    }

    @Override // android.content.ContentProvider
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = this.mStrategy.getFileForUri(uri);
        int fileMode = modeToMode(mode);
        return ParcelFileDescriptor.open(file, fileMode);
    }

    private static PathStrategy getPathStrategy(Context context, String authority) {
        PathStrategy strat;
        synchronized (sCache) {
            strat = sCache.get(authority);
            if (strat == null) {
                try {
                    try {
                        strat = parsePathStrategy(context, authority);
                        sCache.put(authority, strat);
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Failed to parse android.support.FILE_PROVIDER_PATHS meta-data", e);
                    }
                } catch (XmlPullParserException e2) {
                    throw new IllegalArgumentException("Failed to parse android.support.FILE_PROVIDER_PATHS meta-data", e2);
                }
            }
        }
        return strat;
    }

    private static PathStrategy parsePathStrategy(Context context, String authority) throws IOException, XmlPullParserException {
        SimplePathStrategy strat = new SimplePathStrategy(authority);
        ProviderInfo info = context.getPackageManager().resolveContentProvider(authority, 128);
        XmlResourceParser in = info.loadXmlMetaData(context.getPackageManager(), "android.support.FILE_PROVIDER_PATHS");
        if (in == null) {
            throw new IllegalArgumentException("Missing android.support.FILE_PROVIDER_PATHS meta-data");
        }
        while (true) {
            int type = in.next();
            if (type != 1) {
                if (type == 2) {
                    String tag = in.getName();
                    String name = in.getAttributeValue(null, "name");
                    String path = in.getAttributeValue(null, "path");
                    File target = null;
                    if ("root-path".equals(tag)) {
                        target = buildPath(DEVICE_ROOT, path);
                    } else if ("files-path".equals(tag)) {
                        target = buildPath(context.getFilesDir(), path);
                    } else if ("cache-path".equals(tag)) {
                        target = buildPath(context.getCacheDir(), path);
                    } else if ("external-path".equals(tag)) {
                        target = buildPath(Environment.getExternalStorageDirectory(), path);
                    }
                    if (target != null) {
                        strat.addRoot(name, target);
                    }
                }
            } else {
                return strat;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class SimplePathStrategy implements PathStrategy {
        private final String mAuthority;
        private final HashMap<String, File> mRoots = new HashMap<>();

        public SimplePathStrategy(String authority) {
            this.mAuthority = authority;
        }

        public void addRoot(String name, File root) {
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("Name must not be empty");
            }
            try {
                this.mRoots.put(name, root.getCanonicalFile());
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + root, e);
            }
        }

        @Override // android.support.v4.content.FileProvider.PathStrategy
        public Uri getUriForFile(File file) {
            String path;
            try {
                String path2 = file.getCanonicalPath();
                Map.Entry<String, File> mostSpecific = null;
                for (Map.Entry<String, File> root : this.mRoots.entrySet()) {
                    String rootPath = root.getValue().getPath();
                    if (path2.startsWith(rootPath) && (mostSpecific == null || rootPath.length() > mostSpecific.getValue().getPath().length())) {
                        mostSpecific = root;
                    }
                }
                if (mostSpecific == null) {
                    throw new IllegalArgumentException("Failed to find configured root that contains " + path2);
                }
                String rootPath2 = mostSpecific.getValue().getPath();
                if (rootPath2.endsWith("/")) {
                    path = path2.substring(rootPath2.length());
                } else {
                    path = path2.substring(rootPath2.length() + 1);
                }
                return new Uri.Builder().scheme("content").authority(this.mAuthority).encodedPath(Uri.encode(mostSpecific.getKey()) + '/' + Uri.encode(path, "/")).build();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
            }
        }

        @Override // android.support.v4.content.FileProvider.PathStrategy
        public File getFileForUri(Uri uri) {
            String path = uri.getEncodedPath();
            int splitIndex = path.indexOf(47, 1);
            String tag = Uri.decode(path.substring(1, splitIndex));
            String path2 = Uri.decode(path.substring(splitIndex + 1));
            File root = this.mRoots.get(tag);
            if (root == null) {
                throw new IllegalArgumentException("Unable to find configured root for " + uri);
            }
            File file = new File(root, path2);
            try {
                File file2 = file.getCanonicalFile();
                if (!file2.getPath().startsWith(root.getPath())) {
                    throw new SecurityException("Resolved path jumped beyond configured root");
                }
                return file2;
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
            }
        }
    }

    private static int modeToMode(String mode) {
        if ("r".equals(mode)) {
            return 268435456;
        }
        if ("w".equals(mode) || "wt".equals(mode)) {
            return 738197504;
        }
        if ("wa".equals(mode)) {
            return 704643072;
        }
        if ("rw".equals(mode)) {
            return 939524096;
        }
        if ("rwt".equals(mode)) {
            return 1006632960;
        }
        throw new IllegalArgumentException("Invalid mode: " + mode);
    }

    private static File buildPath(File base, String... segments) {
        int i = 0;
        int length = segments.length;
        File cur = base;
        while (i < length) {
            String segment = segments[i];
            i++;
            cur = segment != null ? new File(cur, segment) : cur;
        }
        return cur;
    }

    private static String[] copyOf(String[] original, int newLength) {
        String[] result = new String[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }

    private static Object[] copyOf(Object[] original, int newLength) {
        Object[] result = new Object[newLength];
        System.arraycopy(original, 0, result, 0, newLength);
        return result;
    }
}

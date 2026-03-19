package android.provider;

import android.app.backup.FullBackup;
import android.app.job.JobInfo;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.Signature;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.fonts.FontVariationAxis;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.LruCache;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FontsContract {
    private static final long SYNC_FONT_FETCH_TIMEOUT_MS = 500;
    private static final String TAG = "FontsContract";
    private static final int THREAD_RENEWAL_THRESHOLD_MS = 10000;
    private static volatile Context sContext;

    @GuardedBy("sLock")
    private static Handler sHandler;

    @GuardedBy("sLock")
    private static Set<String> sInQueueSet;

    @GuardedBy("sLock")
    private static HandlerThread sThread;
    private static final Object sLock = new Object();
    private static final LruCache<String, Typeface> sTypefaceCache = new LruCache<>(16);
    private static final Runnable sReplaceDispatcherThreadRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (FontsContract.sLock) {
                if (FontsContract.sThread != null) {
                    FontsContract.sThread.quitSafely();
                    HandlerThread unused = FontsContract.sThread = null;
                    Handler unused2 = FontsContract.sHandler = null;
                }
            }
        }
    };
    private static final Comparator<byte[]> sByteArrayComparator = new Comparator() {
        @Override
        public final int compare(Object obj, Object obj2) {
            return FontsContract.lambda$static$13((byte[]) obj, (byte[]) obj2);
        }
    };

    public static final class Columns implements BaseColumns {
        public static final String FILE_ID = "file_id";
        public static final String ITALIC = "font_italic";
        public static final String RESULT_CODE = "result_code";
        public static final int RESULT_CODE_FONT_NOT_FOUND = 1;
        public static final int RESULT_CODE_FONT_UNAVAILABLE = 2;
        public static final int RESULT_CODE_MALFORMED_QUERY = 3;
        public static final int RESULT_CODE_OK = 0;
        public static final String TTC_INDEX = "font_ttc_index";
        public static final String VARIATION_SETTINGS = "font_variation_settings";
        public static final String WEIGHT = "font_weight";

        private Columns() {
        }
    }

    private FontsContract() {
    }

    public static void setApplicationContextForResources(Context context) {
        sContext = context.getApplicationContext();
    }

    public static class FontInfo {
        private final FontVariationAxis[] mAxes;
        private final boolean mItalic;
        private final int mResultCode;
        private final int mTtcIndex;
        private final Uri mUri;
        private final int mWeight;

        public FontInfo(Uri uri, int i, FontVariationAxis[] fontVariationAxisArr, int i2, boolean z, int i3) {
            this.mUri = (Uri) Preconditions.checkNotNull(uri);
            this.mTtcIndex = i;
            this.mAxes = fontVariationAxisArr;
            this.mWeight = i2;
            this.mItalic = z;
            this.mResultCode = i3;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public int getTtcIndex() {
            return this.mTtcIndex;
        }

        public FontVariationAxis[] getAxes() {
            return this.mAxes;
        }

        public int getWeight() {
            return this.mWeight;
        }

        public boolean isItalic() {
            return this.mItalic;
        }

        public int getResultCode() {
            return this.mResultCode;
        }
    }

    public static class FontFamilyResult {
        public static final int STATUS_OK = 0;
        public static final int STATUS_REJECTED = 3;
        public static final int STATUS_UNEXPECTED_DATA_PROVIDED = 2;
        public static final int STATUS_WRONG_CERTIFICATES = 1;
        private final FontInfo[] mFonts;
        private final int mStatusCode;

        @Retention(RetentionPolicy.SOURCE)
        @interface FontResultStatus {
        }

        public FontFamilyResult(int i, FontInfo[] fontInfoArr) {
            this.mStatusCode = i;
            this.mFonts = fontInfoArr;
        }

        public int getStatusCode() {
            return this.mStatusCode;
        }

        public FontInfo[] getFonts() {
            return this.mFonts;
        }
    }

    public static Typeface getFontSync(final FontRequest fontRequest) {
        final String identifier = fontRequest.getIdentifier();
        Typeface typeface = sTypefaceCache.get(identifier);
        if (typeface != null) {
            return typeface;
        }
        synchronized (sLock) {
            if (sHandler == null) {
                sThread = new HandlerThread("fonts", 10);
                sThread.start();
                sHandler = new Handler(sThread.getLooper());
            }
            final ReentrantLock reentrantLock = new ReentrantLock();
            final Condition conditionNewCondition = reentrantLock.newCondition();
            final AtomicReference atomicReference = new AtomicReference();
            final AtomicBoolean atomicBoolean = new AtomicBoolean(true);
            final AtomicBoolean atomicBoolean2 = new AtomicBoolean(false);
            sHandler.post(new Runnable() {
                @Override
                public final void run() throws Exception {
                    FontsContract.lambda$getFontSync$0(fontRequest, identifier, atomicReference, reentrantLock, atomicBoolean2, atomicBoolean, conditionNewCondition);
                }
            });
            sHandler.removeCallbacks(sReplaceDispatcherThreadRunnable);
            sHandler.postDelayed(sReplaceDispatcherThreadRunnable, JobInfo.MIN_BACKOFF_MILLIS);
            long nanos = TimeUnit.MILLISECONDS.toNanos(SYNC_FONT_FETCH_TIMEOUT_MS);
            reentrantLock.lock();
            try {
                if (!atomicBoolean.get()) {
                    return (Typeface) atomicReference.get();
                }
                long jAwaitNanos = nanos;
                do {
                    try {
                        jAwaitNanos = conditionNewCondition.awaitNanos(jAwaitNanos);
                    } catch (InterruptedException e) {
                    }
                    if (!atomicBoolean.get()) {
                        return (Typeface) atomicReference.get();
                    }
                } while (jAwaitNanos > 0);
                atomicBoolean2.set(true);
                Log.w(TAG, "Remote font fetch timed out: " + fontRequest.getProviderAuthority() + "/" + fontRequest.getQuery());
                return null;
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    static void lambda$getFontSync$0(FontRequest fontRequest, String str, AtomicReference atomicReference, Lock lock, AtomicBoolean atomicBoolean, AtomicBoolean atomicBoolean2, Condition condition) throws Exception {
        try {
            FontFamilyResult fontFamilyResultFetchFonts = fetchFonts(sContext, null, fontRequest);
            if (fontFamilyResultFetchFonts.getStatusCode() == 0) {
                Typeface typefaceBuildTypeface = buildTypeface(sContext, null, fontFamilyResultFetchFonts.getFonts());
                if (typefaceBuildTypeface != null) {
                    sTypefaceCache.put(str, typefaceBuildTypeface);
                }
                atomicReference.set(typefaceBuildTypeface);
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        lock.lock();
        try {
            if (!atomicBoolean.get()) {
                atomicBoolean2.set(false);
                condition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    public static class FontRequestCallback {
        public static final int FAIL_REASON_FONT_LOAD_ERROR = -3;
        public static final int FAIL_REASON_FONT_NOT_FOUND = 1;
        public static final int FAIL_REASON_FONT_UNAVAILABLE = 2;
        public static final int FAIL_REASON_MALFORMED_QUERY = 3;
        public static final int FAIL_REASON_PROVIDER_NOT_FOUND = -1;
        public static final int FAIL_REASON_WRONG_CERTIFICATES = -2;

        @Retention(RetentionPolicy.SOURCE)
        @interface FontRequestFailReason {
        }

        public void onTypefaceRetrieved(Typeface typeface) {
        }

        public void onTypefaceRequestFailed(int i) {
        }
    }

    public static void requestFonts(final Context context, final FontRequest fontRequest, Handler handler, final CancellationSignal cancellationSignal, final FontRequestCallback fontRequestCallback) {
        final Handler handler2 = new Handler();
        final Typeface typeface = sTypefaceCache.get(fontRequest.getIdentifier());
        if (typeface != null) {
            handler2.post(new Runnable() {
                @Override
                public final void run() {
                    fontRequestCallback.onTypefaceRetrieved(typeface);
                }
            });
        } else {
            handler.post(new Runnable() {
                @Override
                public final void run() throws Exception {
                    FontsContract.lambda$requestFonts$12(context, cancellationSignal, fontRequest, handler2, fontRequestCallback);
                }
            });
        }
    }

    static void lambda$requestFonts$12(Context context, CancellationSignal cancellationSignal, FontRequest fontRequest, Handler handler, final FontRequestCallback fontRequestCallback) throws Exception {
        try {
            FontFamilyResult fontFamilyResultFetchFonts = fetchFonts(context, cancellationSignal, fontRequest);
            final Typeface typeface = sTypefaceCache.get(fontRequest.getIdentifier());
            if (typeface != null) {
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        fontRequestCallback.onTypefaceRetrieved(typeface);
                    }
                });
                return;
            }
            if (fontFamilyResultFetchFonts.getStatusCode() != 0) {
                switch (fontFamilyResultFetchFonts.getStatusCode()) {
                    case 1:
                        handler.post(new Runnable() {
                            @Override
                            public final void run() {
                                fontRequestCallback.onTypefaceRequestFailed(-2);
                            }
                        });
                        break;
                    case 2:
                        handler.post(new Runnable() {
                            @Override
                            public final void run() {
                                fontRequestCallback.onTypefaceRequestFailed(-3);
                            }
                        });
                        break;
                    default:
                        handler.post(new Runnable() {
                            @Override
                            public final void run() {
                                fontRequestCallback.onTypefaceRequestFailed(-3);
                            }
                        });
                        break;
                }
                return;
            }
            FontInfo[] fonts = fontFamilyResultFetchFonts.getFonts();
            if (fonts == null || fonts.length == 0) {
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        fontRequestCallback.onTypefaceRequestFailed(1);
                    }
                });
                return;
            }
            for (FontInfo fontInfo : fonts) {
                if (fontInfo.getResultCode() != 0) {
                    final int resultCode = fontInfo.getResultCode();
                    if (resultCode < 0) {
                        handler.post(new Runnable() {
                            @Override
                            public final void run() {
                                fontRequestCallback.onTypefaceRequestFailed(-3);
                            }
                        });
                        return;
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public final void run() {
                                fontRequestCallback.onTypefaceRequestFailed(resultCode);
                            }
                        });
                        return;
                    }
                }
            }
            final Typeface typefaceBuildTypeface = buildTypeface(context, cancellationSignal, fonts);
            if (typefaceBuildTypeface == null) {
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        fontRequestCallback.onTypefaceRequestFailed(-3);
                    }
                });
            } else {
                sTypefaceCache.put(fontRequest.getIdentifier(), typefaceBuildTypeface);
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        fontRequestCallback.onTypefaceRetrieved(typefaceBuildTypeface);
                    }
                });
            }
        } catch (PackageManager.NameNotFoundException e) {
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    fontRequestCallback.onTypefaceRequestFailed(-1);
                }
            });
        }
    }

    public static FontFamilyResult fetchFonts(Context context, CancellationSignal cancellationSignal, FontRequest fontRequest) throws PackageManager.NameNotFoundException {
        if (context.isRestricted()) {
            return new FontFamilyResult(3, null);
        }
        ProviderInfo provider = getProvider(context.getPackageManager(), fontRequest);
        if (provider == null) {
            return new FontFamilyResult(1, null);
        }
        try {
            return new FontFamilyResult(0, getFontFromProvider(context, fontRequest, provider.authority, cancellationSignal));
        } catch (IllegalArgumentException e) {
            return new FontFamilyResult(2, null);
        }
    }

    public static Typeface buildTypeface(Context context, CancellationSignal cancellationSignal, FontInfo[] fontInfoArr) throws Exception {
        if (context.isRestricted()) {
            return null;
        }
        Map<Uri, ByteBuffer> mapPrepareFontData = prepareFontData(context, fontInfoArr, cancellationSignal);
        if (mapPrepareFontData.isEmpty()) {
            return null;
        }
        return new Typeface.Builder(fontInfoArr, mapPrepareFontData).build();
    }

    private static Map<Uri, ByteBuffer> prepareFontData(Context context, FontInfo[] fontInfoArr, CancellationSignal cancellationSignal) throws Exception {
        MappedByteBuffer map;
        Throwable th;
        HashMap map2 = new HashMap();
        ContentResolver contentResolver = context.getContentResolver();
        for (FontInfo fontInfo : fontInfoArr) {
            if (fontInfo.getResultCode() == 0) {
                Uri uri = fontInfo.getUri();
                if (!map2.containsKey(uri)) {
                    Throwable th2 = null;
                    try {
                        ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor = contentResolver.openFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN, cancellationSignal);
                        if (parcelFileDescriptorOpenFileDescriptor != null) {
                            try {
                                FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptorOpenFileDescriptor.getFileDescriptor());
                                try {
                                    FileChannel channel = fileInputStream.getChannel();
                                    map = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
                                    try {
                                        try {
                                            $closeResource(null, fileInputStream);
                                        } catch (Throwable th3) {
                                            th = th3;
                                            if (parcelFileDescriptorOpenFileDescriptor != null) {
                                                $closeResource(th2, parcelFileDescriptorOpenFileDescriptor);
                                            }
                                            throw th;
                                        }
                                    } catch (IOException e) {
                                    } catch (Throwable th4) {
                                        th = th4;
                                        th2 = th;
                                        throw th2;
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                    th = null;
                                    $closeResource(th, fileInputStream);
                                    throw th;
                                }
                            } catch (IOException e2) {
                                map = null;
                            } catch (Throwable th6) {
                                th = th6;
                                map = null;
                                if (parcelFileDescriptorOpenFileDescriptor != null) {
                                }
                                throw th;
                            }
                        } else {
                            map = null;
                        }
                        if (parcelFileDescriptorOpenFileDescriptor != null) {
                            try {
                                $closeResource(null, parcelFileDescriptorOpenFileDescriptor);
                            } catch (IOException e3) {
                            }
                        }
                    } catch (IOException e4) {
                        map = null;
                    }
                    map2.put(uri, map);
                }
            }
        }
        return Collections.unmodifiableMap(map2);
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

    @VisibleForTesting
    public static ProviderInfo getProvider(PackageManager packageManager, FontRequest fontRequest) throws PackageManager.NameNotFoundException {
        String providerAuthority = fontRequest.getProviderAuthority();
        ProviderInfo providerInfoResolveContentProvider = packageManager.resolveContentProvider(providerAuthority, 0);
        if (providerInfoResolveContentProvider == null) {
            throw new PackageManager.NameNotFoundException("No package found for authority: " + providerAuthority);
        }
        if (!providerInfoResolveContentProvider.packageName.equals(fontRequest.getProviderPackage())) {
            throw new PackageManager.NameNotFoundException("Found content provider " + providerAuthority + ", but package was not " + fontRequest.getProviderPackage());
        }
        if (providerInfoResolveContentProvider.applicationInfo.isSystemApp()) {
            return providerInfoResolveContentProvider;
        }
        List<byte[]> listConvertToByteArrayList = convertToByteArrayList(packageManager.getPackageInfo(providerInfoResolveContentProvider.packageName, 64).signatures);
        Collections.sort(listConvertToByteArrayList, sByteArrayComparator);
        List<List<byte[]>> certificates = fontRequest.getCertificates();
        for (int i = 0; i < certificates.size(); i++) {
            ArrayList arrayList = new ArrayList(certificates.get(i));
            Collections.sort(arrayList, sByteArrayComparator);
            if (equalsByteArrayList(listConvertToByteArrayList, arrayList)) {
                return providerInfoResolveContentProvider;
            }
        }
        return null;
    }

    static int lambda$static$13(byte[] bArr, byte[] bArr2) {
        if (bArr.length != bArr2.length) {
            return bArr.length - bArr2.length;
        }
        for (int i = 0; i < bArr.length; i++) {
            if (bArr[i] != bArr2[i]) {
                return bArr[i] - bArr2[i];
            }
        }
        return 0;
    }

    private static boolean equalsByteArrayList(List<byte[]> list, List<byte[]> list2) {
        if (list.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            if (!Arrays.equals(list.get(i), list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<byte[]> convertToByteArrayList(Signature[] signatureArr) {
        ArrayList arrayList = new ArrayList();
        for (Signature signature : signatureArr) {
            arrayList.add(signature.toByteArray());
        }
        return arrayList;
    }

    @VisibleForTesting
    public static FontInfo[] getFontFromProvider(Context context, FontRequest fontRequest, String str, CancellationSignal cancellationSignal) throws Exception {
        Throwable th;
        int i;
        int i2;
        String string;
        ArrayList arrayList;
        Uri uriWithAppendedId;
        int i3;
        boolean z;
        ArrayList arrayList2 = new ArrayList();
        Uri uriBuild = new Uri.Builder().scheme("content").authority(str).build();
        Uri uriBuild2 = new Uri.Builder().scheme("content").authority(str).appendPath(ContentResolver.SCHEME_FILE).build();
        Cursor cursorQuery = context.getContentResolver().query(uriBuild, new String[]{"_id", Columns.FILE_ID, Columns.TTC_INDEX, Columns.VARIATION_SETTINGS, Columns.WEIGHT, Columns.ITALIC, Columns.RESULT_CODE}, "query = ?", new String[]{fontRequest.getQuery()}, null, cancellationSignal);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() > 0) {
                    int columnIndex = cursorQuery.getColumnIndex(Columns.RESULT_CODE);
                    ArrayList arrayList3 = new ArrayList();
                    int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("_id");
                    int columnIndex2 = cursorQuery.getColumnIndex(Columns.FILE_ID);
                    int columnIndex3 = cursorQuery.getColumnIndex(Columns.TTC_INDEX);
                    int columnIndex4 = cursorQuery.getColumnIndex(Columns.VARIATION_SETTINGS);
                    int columnIndex5 = cursorQuery.getColumnIndex(Columns.WEIGHT);
                    int columnIndex6 = cursorQuery.getColumnIndex(Columns.ITALIC);
                    while (cursorQuery.moveToNext()) {
                        if (columnIndex != -1) {
                            i = cursorQuery.getInt(columnIndex);
                        } else {
                            i = 0;
                        }
                        if (columnIndex3 != -1) {
                            i2 = cursorQuery.getInt(columnIndex3);
                        } else {
                            i2 = 0;
                        }
                        if (columnIndex4 != -1) {
                            string = cursorQuery.getString(columnIndex4);
                        } else {
                            string = null;
                        }
                        if (columnIndex2 == -1) {
                            arrayList = arrayList3;
                            uriWithAppendedId = ContentUris.withAppendedId(uriBuild, cursorQuery.getLong(columnIndexOrThrow));
                        } else {
                            arrayList = arrayList3;
                            uriWithAppendedId = ContentUris.withAppendedId(uriBuild2, cursorQuery.getLong(columnIndex2));
                        }
                        Uri uri = uriWithAppendedId;
                        if (columnIndex5 != -1 && columnIndex6 != -1) {
                            i3 = cursorQuery.getInt(columnIndex5);
                            z = cursorQuery.getInt(columnIndex6) == 1;
                        } else {
                            i3 = 400;
                            z = false;
                        }
                        arrayList3 = arrayList;
                        arrayList3.add(new FontInfo(uri, i2, FontVariationAxis.fromFontVariationSettings(string), i3, z, i));
                    }
                    arrayList2 = arrayList3;
                }
            } catch (Throwable th2) {
                th = th2;
                th = null;
                if (cursorQuery != null) {
                }
                throw th;
            }
        }
        if (cursorQuery != null) {
            $closeResource(null, cursorQuery);
        }
        return (FontInfo[]) arrayList2.toArray(new FontInfo[0]);
    }
}

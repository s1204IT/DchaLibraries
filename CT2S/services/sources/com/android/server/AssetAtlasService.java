package com.android.server;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Atlas;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.GraphicBuffer;
import android.view.IAssetAtlas;
import com.android.server.voiceinteraction.SoundTriggerHelper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AssetAtlasService extends IAssetAtlas.Stub {
    public static final String ASSET_ATLAS_SERVICE = "assetatlas";
    private static final int ATLAS_MAP_ENTRY_FIELD_COUNT = 4;
    private static final boolean DEBUG_ATLAS = true;
    private static final boolean DEBUG_ATLAS_TEXTURE = false;
    private static final int GRAPHIC_BUFFER_USAGE = 256;
    private static final String LOG_TAG = "AssetAtlas";
    private static final int MAX_SIZE = 2048;
    private static final int MIN_SIZE = 768;
    private static final float PACKING_THRESHOLD = 0.8f;
    private static final int STEP = 64;
    private long[] mAtlasMap;
    private final AtomicBoolean mAtlasReady = new AtomicBoolean(DEBUG_ATLAS_TEXTURE);
    private GraphicBuffer mBuffer;
    private final Context mContext;
    private final String mVersionName;

    private static native long nAcquireAtlasCanvas(Canvas canvas, int i, int i2);

    private static native void nReleaseAtlasCanvas(Canvas canvas, long j);

    private static native boolean nUploadAtlas(GraphicBuffer graphicBuffer, long j);

    public AssetAtlasService(Context context) {
        this.mContext = context;
        this.mVersionName = queryVersionName(context);
        Collection<Bitmap> bitmaps = new HashSet<>(300);
        int totalPixelCount = 0;
        Resources resources = context.getResources();
        LongSparseArray<Drawable.ConstantState> drawables = resources.getPreloadedDrawables();
        int count = drawables.size();
        for (int i = 0; i < count; i++) {
            try {
                totalPixelCount += drawables.valueAt(i).addAtlasableBitmaps(bitmaps);
            } catch (Throwable t) {
                Log.e(LOG_TAG, "Failed to fetch preloaded drawable state", t);
                throw t;
            }
        }
        ArrayList<Bitmap> sortedBitmaps = new ArrayList<>(bitmaps);
        Collections.sort(sortedBitmaps, new Comparator<Bitmap>() {
            @Override
            public int compare(Bitmap b1, Bitmap b2) {
                return b1.getWidth() == b2.getWidth() ? b2.getHeight() - b1.getHeight() : b2.getWidth() - b1.getWidth();
            }
        });
        new Thread(new Renderer(sortedBitmaps, totalPixelCount)).start();
    }

    private static String queryVersionName(Context context) {
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOG_TAG, "Could not get package info", e);
            return null;
        }
    }

    public void systemRunning() {
    }

    private class Renderer implements Runnable {
        private Bitmap mAtlasBitmap;
        private final ArrayList<Bitmap> mBitmaps;
        private long mNativeBitmap;
        private final int mPixelCount;

        Renderer(ArrayList<Bitmap> bitmaps, int pixelCount) {
            this.mBitmaps = bitmaps;
            this.mPixelCount = pixelCount;
        }

        @Override
        public void run() throws Throwable {
            Configuration config = AssetAtlasService.this.chooseConfiguration(this.mBitmaps, this.mPixelCount, AssetAtlasService.this.mVersionName);
            Log.d(AssetAtlasService.LOG_TAG, "Loaded configuration: " + config);
            if (config != null) {
                AssetAtlasService.this.mBuffer = GraphicBuffer.create(config.width, config.height, 1, 256);
                if (AssetAtlasService.this.mBuffer != null) {
                    Atlas atlas = new Atlas(config.type, config.width, config.height, config.flags);
                    if (renderAtlas(AssetAtlasService.this.mBuffer, atlas, config.count)) {
                        AssetAtlasService.this.mAtlasReady.set(AssetAtlasService.DEBUG_ATLAS);
                    }
                }
            }
        }

        private boolean renderAtlas(GraphicBuffer buffer, Atlas atlas, int packCount) throws Throwable {
            long startRender;
            int count;
            int i;
            int mapIndex;
            Bitmap bitmap;
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            Canvas canvas = acquireCanvas(buffer.getWidth(), buffer.getHeight());
            if (canvas == null) {
                return AssetAtlasService.DEBUG_ATLAS_TEXTURE;
            }
            Atlas.Entry entry = new Atlas.Entry();
            AssetAtlasService.this.mAtlasMap = new long[packCount * 4];
            long[] atlasMap = AssetAtlasService.this.mAtlasMap;
            boolean result = AssetAtlasService.DEBUG_ATLAS_TEXTURE;
            try {
                startRender = System.nanoTime();
                count = this.mBitmaps.size();
                i = 0;
                mapIndex = 0;
            } catch (Throwable th) {
                th = th;
            }
            while (true) {
                if (i >= count) {
                    break;
                }
                try {
                    bitmap = this.mBitmaps.get(i);
                } catch (Throwable th2) {
                    th = th2;
                }
                if (atlas.pack(bitmap.getWidth(), bitmap.getHeight(), entry) != null) {
                    if (mapIndex >= AssetAtlasService.this.mAtlasMap.length) {
                        AssetAtlasService.deleteDataFile();
                        break;
                    }
                    canvas.save();
                    canvas.translate(entry.x, entry.y);
                    if (entry.rotated) {
                        canvas.translate(bitmap.getHeight(), 0.0f);
                        canvas.rotate(90.0f);
                    }
                    canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
                    canvas.restore();
                    int mapIndex2 = mapIndex + 1;
                    atlasMap[mapIndex] = bitmap.mNativeBitmap;
                    int mapIndex3 = mapIndex2 + 1;
                    atlasMap[mapIndex2] = entry.x;
                    int mapIndex4 = mapIndex3 + 1;
                    atlasMap[mapIndex3] = entry.y;
                    mapIndex = mapIndex4 + 1;
                    atlasMap[mapIndex4] = entry.rotated ? 1L : 0L;
                    releaseCanvas(canvas);
                    throw th;
                }
                i++;
                mapIndex = mapIndex;
            }
            long endRender = System.nanoTime();
            if (this.mNativeBitmap != 0) {
                result = AssetAtlasService.nUploadAtlas(buffer, this.mNativeBitmap);
            }
            long endUpload = System.nanoTime();
            float renderDuration = ((endRender - startRender) / 1000.0f) / 1000.0f;
            float uploadDuration = ((endUpload - endRender) / 1000.0f) / 1000.0f;
            Log.d(AssetAtlasService.LOG_TAG, String.format("Rendered atlas in %.2fms (%.2f+%.2fms)", Float.valueOf(renderDuration + uploadDuration), Float.valueOf(renderDuration), Float.valueOf(uploadDuration)));
            releaseCanvas(canvas);
            return result;
        }

        private Canvas acquireCanvas(int width, int height) {
            Canvas canvas = new Canvas();
            this.mNativeBitmap = AssetAtlasService.nAcquireAtlasCanvas(canvas, width, height);
            return canvas;
        }

        private void releaseCanvas(Canvas canvas) {
            AssetAtlasService.nReleaseAtlasCanvas(canvas, this.mNativeBitmap);
        }
    }

    public boolean isCompatible(int ppid) {
        return ppid == Process.myPpid() ? DEBUG_ATLAS : DEBUG_ATLAS_TEXTURE;
    }

    public GraphicBuffer getBuffer() throws RemoteException {
        if (this.mAtlasReady.get()) {
            return this.mBuffer;
        }
        return null;
    }

    public long[] getMap() throws RemoteException {
        if (this.mAtlasReady.get()) {
            return this.mAtlasMap;
        }
        return null;
    }

    private static Configuration computeBestConfiguration(ArrayList<Bitmap> bitmaps, int pixelCount) {
        Log.d(LOG_TAG, "Computing best atlas configuration...");
        long begin = System.nanoTime();
        List<WorkerResult> results = Collections.synchronizedList(new ArrayList());
        int cpuCount = Runtime.getRuntime().availableProcessors();
        if (cpuCount == 1) {
            new ComputeWorker(MIN_SIZE, 2048, 64, bitmaps, pixelCount, results, null).run();
        } else {
            int start = MIN_SIZE;
            int end = 2048 - ((cpuCount - 1) * 64);
            int step = cpuCount * 64;
            CountDownLatch signal = new CountDownLatch(cpuCount);
            int i = 0;
            while (i < cpuCount) {
                ComputeWorker worker = new ComputeWorker(start, end, step, bitmaps, pixelCount, results, signal);
                new Thread(worker, "Atlas Worker #" + (i + 1)).start();
                i++;
                start += 64;
                end += 64;
            }
            try {
                signal.await(10L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "Could not complete configuration computation");
                return null;
            }
        }
        Collections.sort(results, new Comparator<WorkerResult>() {
            @Override
            public int compare(WorkerResult r1, WorkerResult r2) {
                int delta = r2.count - r1.count;
                return delta != 0 ? delta : (r1.width * r1.height) - (r2.width * r2.height);
            }
        });
        float delay = (((System.nanoTime() - begin) / 1000.0f) / 1000.0f) / 1000.0f;
        Log.d(LOG_TAG, String.format("Found best atlas configuration in %.2fs", Float.valueOf(delay)));
        WorkerResult result = results.get(0);
        return new Configuration(result.type, result.width, result.height, result.count);
    }

    private static File getDataFile() {
        File systemDirectory = new File(Environment.getDataDirectory(), "system");
        return new File(systemDirectory, "framework_atlas.config");
    }

    private static void deleteDataFile() {
        Log.w(LOG_TAG, "Current configuration inconsistent with assets list");
        if (!getDataFile().delete()) {
            Log.w(LOG_TAG, "Could not delete the current configuration");
        }
    }

    private File getFrameworkResourcesFile() {
        return new File(this.mContext.getApplicationInfo().sourceDir);
    }

    private Configuration chooseConfiguration(ArrayList<Bitmap> bitmaps, int pixelCount, String versionName) throws Throwable {
        Configuration config = null;
        File dataFile = getDataFile();
        if (dataFile.exists()) {
            config = readConfiguration(dataFile, versionName);
        }
        if (config == null && (config = computeBestConfiguration(bitmaps, pixelCount)) != null) {
            writeConfiguration(config, dataFile, versionName);
        }
        return config;
    }

    private void writeConfiguration(Configuration config, File file, String versionName) throws Throwable {
        BufferedWriter writer;
        BufferedWriter writer2 = null;
        try {
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
            e = e;
        } catch (IOException e2) {
            e = e2;
        }
        try {
            writer.write(getBuildIdentifier(versionName));
            writer.newLine();
            writer.write(config.type.toString());
            writer.newLine();
            writer.write(String.valueOf(config.width));
            writer.newLine();
            writer.write(String.valueOf(config.height));
            writer.newLine();
            writer.write(String.valueOf(config.count));
            writer.newLine();
            writer.write(String.valueOf(config.flags));
            writer.newLine();
            if (writer != null) {
                try {
                    writer.close();
                    writer2 = writer;
                } catch (IOException e3) {
                    writer2 = writer;
                }
            } else {
                writer2 = writer;
            }
        } catch (FileNotFoundException e4) {
            e = e4;
            writer2 = writer;
            Log.w(LOG_TAG, "Could not write " + file, e);
            if (writer2 != null) {
                try {
                    writer2.close();
                } catch (IOException e5) {
                }
            }
        } catch (IOException e6) {
            e = e6;
            writer2 = writer;
            Log.w(LOG_TAG, "Could not write " + file, e);
            if (writer2 != null) {
                try {
                    writer2.close();
                } catch (IOException e7) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            writer2 = writer;
            if (writer2 != null) {
                try {
                    writer2.close();
                } catch (IOException e8) {
                }
            }
            throw th;
        }
    }

    private Configuration readConfiguration(File file, String versionName) throws Throwable {
        Configuration config;
        BufferedReader reader;
        BufferedReader reader2 = null;
        try {
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
            e = e;
        } catch (IOException e2) {
            e = e2;
        } catch (IllegalArgumentException e3) {
            e = e3;
        }
        try {
            if (checkBuildIdentifier(reader, versionName)) {
                Atlas.Type type = Atlas.Type.valueOf(reader.readLine());
                int width = readInt(reader, MIN_SIZE, 2048);
                int height = readInt(reader, MIN_SIZE, 2048);
                int count = readInt(reader, 0, Integer.MAX_VALUE);
                int flags = readInt(reader, SoundTriggerHelper.STATUS_ERROR, Integer.MAX_VALUE);
                config = new Configuration(type, width, height, count, flags);
            } else {
                config = null;
            }
            if (reader != null) {
                try {
                    reader.close();
                    reader2 = reader;
                } catch (IOException e4) {
                    reader2 = reader;
                }
            } else {
                reader2 = reader;
            }
        } catch (FileNotFoundException e5) {
            e = e5;
            reader2 = reader;
            Log.w(LOG_TAG, "Could not read " + file, e);
            if (reader2 != null) {
                try {
                    reader2.close();
                    config = null;
                } catch (IOException e6) {
                    config = null;
                }
            } else {
                config = null;
            }
        } catch (IOException e7) {
            e = e7;
            reader2 = reader;
            Log.w(LOG_TAG, "Could not read " + file, e);
            if (reader2 != null) {
                try {
                    reader2.close();
                    config = null;
                } catch (IOException e8) {
                    config = null;
                }
            }
        } catch (IllegalArgumentException e9) {
            e = e9;
            reader2 = reader;
            Log.w(LOG_TAG, "Invalid parameter value in " + file, e);
            if (reader2 != null) {
                try {
                    reader2.close();
                    config = null;
                } catch (IOException e10) {
                    config = null;
                }
            }
        } catch (Throwable th2) {
            th = th2;
            reader2 = reader;
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e11) {
                }
            }
            throw th;
        }
        return config;
    }

    private static int readInt(BufferedReader reader, int min, int max) throws IOException {
        return Math.max(min, Math.min(max, Integer.parseInt(reader.readLine())));
    }

    private boolean checkBuildIdentifier(BufferedReader reader, String versionName) throws IOException {
        String deviceBuildId = getBuildIdentifier(versionName);
        String buildId = reader.readLine();
        return deviceBuildId.equals(buildId);
    }

    private String getBuildIdentifier(String versionName) {
        return SystemProperties.get("ro.build.fingerprint", "") + '/' + versionName + '/' + String.valueOf(getFrameworkResourcesFile().length());
    }

    private static class Configuration {
        final int count;
        final int flags;
        final int height;
        final Atlas.Type type;
        final int width;

        Configuration(Atlas.Type type, int width, int height, int count) {
            this(type, width, height, count, 2);
        }

        Configuration(Atlas.Type type, int width, int height, int count, int flags) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.count = count;
            this.flags = flags;
        }

        public String toString() {
            return this.type.toString() + " (" + this.width + "x" + this.height + ") flags=0x" + Integer.toHexString(this.flags) + " count=" + this.count;
        }
    }

    private static class WorkerResult {
        int count;
        int height;
        Atlas.Type type;
        int width;

        WorkerResult(Atlas.Type type, int width, int height, int count) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.count = count;
        }

        public String toString() {
            return String.format("%s %dx%d", this.type.toString(), Integer.valueOf(this.width), Integer.valueOf(this.height));
        }
    }

    private static class ComputeWorker implements Runnable {
        private final List<Bitmap> mBitmaps;
        private final int mEnd;
        private final List<WorkerResult> mResults;
        private final CountDownLatch mSignal;
        private final int mStart;
        private final int mStep;
        private final int mThreshold;

        ComputeWorker(int start, int end, int step, List<Bitmap> bitmaps, int pixelCount, List<WorkerResult> results, CountDownLatch signal) {
            this.mStart = start;
            this.mEnd = end;
            this.mStep = step;
            this.mBitmaps = bitmaps;
            this.mResults = results;
            this.mSignal = signal;
            int threshold = (int) (pixelCount * 0.8f);
            while (threshold > 4194304) {
                threshold >>= 1;
            }
            this.mThreshold = threshold;
        }

        @Override
        public void run() {
            int count;
            Log.d(AssetAtlasService.LOG_TAG, "Running " + Thread.currentThread().getName());
            Atlas.Entry entry = new Atlas.Entry();
            Atlas.Type[] arr$ = Atlas.Type.values();
            for (Atlas.Type type : arr$) {
                int width = this.mStart;
                while (width < this.mEnd) {
                    for (int height = AssetAtlasService.MIN_SIZE; height < 2048; height += 64) {
                        if (width * height > this.mThreshold && (count = packBitmaps(type, width, height, entry)) > 0) {
                            this.mResults.add(new WorkerResult(type, width, height, count));
                            if (count == this.mBitmaps.size()) {
                                break;
                            }
                        }
                    }
                    width += this.mStep;
                }
            }
            if (this.mSignal != null) {
                this.mSignal.countDown();
            }
        }

        private int packBitmaps(Atlas.Type type, int width, int height, Atlas.Entry entry) {
            int total = 0;
            Atlas atlas = new Atlas(type, width, height);
            int count = this.mBitmaps.size();
            for (int i = 0; i < count; i++) {
                Bitmap bitmap = this.mBitmaps.get(i);
                if (atlas.pack(bitmap.getWidth(), bitmap.getHeight(), entry) != null) {
                    total++;
                }
            }
            return total;
        }
    }
}

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
    private static final int ATLAS_MAP_ENTRY_FIELD_COUNT = 3;
    private static final boolean DEBUG_ATLAS = true;
    private static final boolean DEBUG_ATLAS_TEXTURE = false;
    private static final int GRAPHIC_BUFFER_USAGE = 256;
    private static final String LOG_TAG = "AssetAtlasService";
    private static final int MAX_SIZE = 2048;
    private static final int MIN_SIZE = 512;
    private static final float PACKING_THRESHOLD = 0.8f;
    private static final int STEP = 64;
    private long[] mAtlasMap;
    private final AtomicBoolean mAtlasReady = new AtomicBoolean(false);
    private GraphicBuffer mBuffer;
    private final Context mContext;
    private final String mVersionName;

    private static native boolean nUploadAtlas(GraphicBuffer graphicBuffer, Bitmap bitmap);

    public AssetAtlasService(Context context) {
        this.mContext = context;
        this.mVersionName = queryVersionName(context);
        Collection<Bitmap> bitmaps = new HashSet<>(300);
        int totalPixelCount = 0;
        Resources resources = context.getResources();
        LongSparseArray<Drawable.ConstantState> drawables = resources.getPreloadedDrawables();
        Log.d(LOG_TAG, "AssetAtlasService: size of preload drawables is " + drawables.size());
        int count = drawables.size();
        for (int i = 0; i < count; i++) {
            try {
                totalPixelCount += drawables.valueAt(i).addAtlasableBitmaps(bitmaps);
            } catch (Throwable t) {
                Log.e("AssetAtlas", "Failed to fetch preloaded drawable state", t);
                throw t;
            }
        }
        ArrayList<Bitmap> sortedBitmaps = new ArrayList<>(bitmaps);
        Collections.sort(sortedBitmaps, new Comparator<Bitmap>() {
            @Override
            public int compare(Bitmap b1, Bitmap b2) {
                if (b1.getWidth() == b2.getWidth()) {
                    return b2.getHeight() - b1.getHeight();
                }
                return b2.getWidth() - b1.getWidth();
            }
        });
        new Thread(new Renderer(sortedBitmaps, totalPixelCount)).start();
    }

    private static String queryVersionName(Context context) {
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 268435456);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOG_TAG, "Could not get package info", e);
            return null;
        }
    }

    public void systemRunning() {
    }

    private class Renderer implements Runnable {
        private final ArrayList<Bitmap> mBitmaps;
        private final int mPixelCount;

        Renderer(ArrayList<Bitmap> bitmaps, int pixelCount) {
            this.mBitmaps = bitmaps;
            this.mPixelCount = pixelCount;
        }

        @Override
        public void run() throws Throwable {
            Configuration config = AssetAtlasService.this.chooseConfiguration(this.mBitmaps, this.mPixelCount, AssetAtlasService.this.mVersionName);
            Log.d(AssetAtlasService.LOG_TAG, "Loaded configuration: " + config);
            if (config == null) {
                return;
            }
            AssetAtlasService.this.mBuffer = GraphicBuffer.create(config.width, config.height, 1, 256);
            if (AssetAtlasService.this.mBuffer == null) {
                return;
            }
            Atlas atlas = new Atlas(config.type, config.width, config.height, config.flags);
            if (!renderAtlas(AssetAtlasService.this.mBuffer, atlas, config.count)) {
                return;
            }
            AssetAtlasService.this.mAtlasReady.set(true);
        }

        private boolean renderAtlas(GraphicBuffer buffer, Atlas atlas, int packCount) {
            int mapIndex;
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            Bitmap atlasBitmap = Bitmap.createBitmap(buffer.getWidth(), buffer.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(atlasBitmap);
            Atlas.Entry entry = new Atlas.Entry();
            AssetAtlasService.this.mAtlasMap = new long[packCount * 3];
            long[] atlasMap = AssetAtlasService.this.mAtlasMap;
            long startRender = System.nanoTime();
            int count = this.mBitmaps.size();
            int i = 0;
            int mapIndex2 = 0;
            while (true) {
                if (i >= count) {
                    break;
                }
                Bitmap bitmap = this.mBitmaps.get(i);
                if (atlas.pack(bitmap.getWidth(), bitmap.getHeight(), entry) == null) {
                    mapIndex = mapIndex2;
                } else {
                    if (mapIndex2 >= AssetAtlasService.this.mAtlasMap.length) {
                        AssetAtlasService.deleteDataFile();
                        break;
                    }
                    canvas.save();
                    canvas.translate(entry.x, entry.y);
                    canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
                    canvas.restore();
                    int mapIndex3 = mapIndex2 + 1;
                    atlasMap[mapIndex2] = bitmap.refSkPixelRef();
                    int mapIndex4 = mapIndex3 + 1;
                    atlasMap[mapIndex3] = entry.x;
                    mapIndex = mapIndex4 + 1;
                    atlasMap[mapIndex4] = entry.y;
                }
                i++;
                mapIndex2 = mapIndex;
            }
            long endRender = System.nanoTime();
            releaseCanvas(canvas, atlasBitmap);
            boolean result = AssetAtlasService.nUploadAtlas(buffer, atlasBitmap);
            atlasBitmap.recycle();
            long endUpload = System.nanoTime();
            float renderDuration = ((endRender - startRender) / 1000.0f) / 1000.0f;
            float uploadDuration = ((endUpload - endRender) / 1000.0f) / 1000.0f;
            Log.d(AssetAtlasService.LOG_TAG, String.format("Rendered atlas in %.2fms (%.2f+%.2fms)", Float.valueOf(renderDuration + uploadDuration), Float.valueOf(renderDuration), Float.valueOf(uploadDuration)));
            return result;
        }

        private void releaseCanvas(Canvas canvas, Bitmap atlasBitmap) {
            canvas.setBitmap(null);
        }
    }

    public boolean isCompatible(int ppid) {
        return ppid == Process.myPpid();
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
            new ComputeWorker(512, 2048, 64, bitmaps, pixelCount, results, null).run();
        } else {
            int start = ((cpuCount - 1) * 64) + 512;
            int end = 2048;
            int step = cpuCount * 64;
            CountDownLatch signal = new CountDownLatch(cpuCount);
            int i = 0;
            while (i < cpuCount) {
                ComputeWorker worker = new ComputeWorker(start, end, step, bitmaps, pixelCount, results, signal);
                new Thread(worker, "Atlas Worker #" + (i + 1)).start();
                i++;
                start -= 64;
                end -= 64;
            }
            try {
                boolean isAllWorkerFinished = signal.await(15L, TimeUnit.SECONDS);
                if (!isAllWorkerFinished) {
                    Log.w(LOG_TAG, "Could not complete configuration computation before timeout.");
                    return null;
                }
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "Could not complete configuration computation");
                return null;
            }
        }
        synchronized (results) {
            Collections.sort(results, new Comparator<WorkerResult>() {
                @Override
                public int compare(WorkerResult r1, WorkerResult r2) {
                    int delta = r2.count - r1.count;
                    return delta != 0 ? delta : (r1.width * r1.height) - (r2.width * r2.height);
                }
            });
        }
        float delay = (((System.nanoTime() - begin) / 1000.0f) / 1000.0f) / 1000.0f;
        Log.d(LOG_TAG, String.format("Found best atlas configuration (out of %d) in %.2fs", Integer.valueOf(results.size()), Float.valueOf(delay)));
        WorkerResult result = results.get(0);
        return new Configuration(result.type, result.width, result.height, result.count);
    }

    private static File getDataFile() {
        File systemDirectory = new File(Environment.getDataDirectory(), "system");
        return new File(systemDirectory, "framework_atlas.config");
    }

    private static void deleteDataFile() {
        Log.w(LOG_TAG, "Current configuration inconsistent with assets list");
        if (getDataFile().delete()) {
            return;
        }
        Log.w(LOG_TAG, "Could not delete the current configuration");
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
                } catch (IOException e3) {
                }
            }
            writer2 = writer;
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
        BufferedReader reader;
        Configuration config;
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
                int width = readInt(reader, 512, 2048);
                int height = readInt(reader, 512, 2048);
                int count = readInt(reader, 0, Integer.MAX_VALUE);
                int flags = readInt(reader, Integer.MIN_VALUE, Integer.MAX_VALUE);
                config = new Configuration(type, width, height, count, flags);
            } else {
                config = null;
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e4) {
                }
            }
            return config;
        } catch (FileNotFoundException e5) {
            e = e5;
            reader2 = reader;
            Log.w(LOG_TAG, "Could not read " + file, e);
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e6) {
                }
            }
            return null;
        } catch (IOException e7) {
            e = e7;
            reader2 = reader;
            Log.w(LOG_TAG, "Could not read " + file, e);
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e8) {
                }
            }
            return null;
        } catch (IllegalArgumentException e9) {
            e = e9;
            reader2 = reader;
            Log.w(LOG_TAG, "Invalid parameter value in " + file, e);
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e10) {
                }
            }
            return null;
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
            int threshold = (int) (pixelCount * AssetAtlasService.PACKING_THRESHOLD);
            while (threshold > 4194304) {
                threshold >>= 1;
            }
            this.mThreshold = threshold;
        }

        @Override
        public void run() {
            Log.d(AssetAtlasService.LOG_TAG, "Running " + Thread.currentThread().getName());
            Atlas.Entry entry = new Atlas.Entry();
            int width = this.mEnd;
            while (width > this.mStart) {
                for (int height = 2048; height > 512; height -= 64) {
                    if (width * height > this.mThreshold) {
                        boolean packSuccess = false;
                        Atlas.Type[] typeArrValues = Atlas.Type.values();
                        int length = typeArrValues.length;
                        int i = 0;
                        while (true) {
                            if (i >= length) {
                                break;
                            }
                            Atlas.Type type = typeArrValues[i];
                            int count = packBitmaps(type, width, height, entry);
                            if (count > 0) {
                                this.mResults.add(new WorkerResult(type, width, height, count));
                                if (count == this.mBitmaps.size()) {
                                    packSuccess = true;
                                    break;
                                }
                            }
                            i++;
                        }
                        if (!packSuccess) {
                            break;
                        }
                    }
                }
                width -= this.mStep;
            }
            if (this.mSignal == null) {
                return;
            }
            this.mSignal.countDown();
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

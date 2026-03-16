package com.android.gallery3d.filtershow.pipeline;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import java.util.Vector;

public class CachingPipeline implements PipelineInterface {
    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
    private static volatile RenderScript sRS = null;
    private FiltersManager mFiltersManager;
    protected volatile Allocation mInPixelsAllocation;
    private volatile String mName;
    protected volatile Allocation mOutPixelsAllocation;
    private boolean DEBUG = false;
    private volatile Bitmap mOriginalBitmap = null;
    private volatile Bitmap mResizedOriginalBitmap = null;
    private FilterEnvironment mEnvironment = new FilterEnvironment();
    private CacheProcessing mCachedProcessing = new CacheProcessing();
    private volatile Allocation mOriginalAllocation = null;
    private volatile Allocation mFiltersOnlyOriginalAllocation = null;
    private volatile int mWidth = 0;
    private volatile int mHeight = 0;
    private volatile float mPreviewScaleFactor = 1.0f;
    private volatile float mHighResPreviewScaleFactor = 1.0f;

    public CachingPipeline(FiltersManager filtersManager, String name) {
        this.mFiltersManager = null;
        this.mName = "";
        this.mFiltersManager = filtersManager;
        this.mName = name;
    }

    public static synchronized RenderScript getRenderScriptContext() {
        return sRS;
    }

    public static synchronized void createRenderscriptContext(Context context) {
        if (sRS != null) {
            Log.w("CachingPipeline", "A prior RS context exists when calling setRenderScriptContext");
            destroyRenderScriptContext();
        }
        sRS = RenderScript.create(context);
    }

    public static synchronized void destroyRenderScriptContext() {
        if (sRS != null) {
            sRS.destroy();
        }
        sRS = null;
    }

    public void stop() {
        this.mEnvironment.setStop(true);
    }

    @Override
    public Resources getResources() {
        return sRS.getApplicationContext().getResources();
    }

    private synchronized void destroyPixelAllocations() {
        if (this.DEBUG) {
            Log.v("CachingPipeline", "destroyPixelAllocations in " + getName());
        }
        if (this.mInPixelsAllocation != null) {
            this.mInPixelsAllocation.destroy();
            this.mInPixelsAllocation = null;
        }
        if (this.mOutPixelsAllocation != null) {
            this.mOutPixelsAllocation.destroy();
            this.mOutPixelsAllocation = null;
        }
        this.mWidth = 0;
        this.mHeight = 0;
    }

    private String getType(RenderingRequest request) {
        if (request.getType() == 3) {
            return "ICON_RENDERING";
        }
        if (request.getType() == 1) {
            return "FILTERS_RENDERING";
        }
        if (request.getType() == 0) {
            return "FULL_RENDERING";
        }
        if (request.getType() == 2) {
            return "GEOMETRY_RENDERING";
        }
        if (request.getType() == 4) {
            return "PARTIAL_RENDERING";
        }
        if (request.getType() == 5) {
            return "HIGHRES_RENDERING";
        }
        return "UNKNOWN TYPE!";
    }

    private void setupEnvironment(ImagePreset preset, boolean highResPreview) {
        this.mEnvironment.setPipeline(this);
        this.mEnvironment.setFiltersManager(this.mFiltersManager);
        this.mEnvironment.setBitmapCache(MasterImage.getImage().getBitmapCache());
        if (highResPreview) {
            this.mEnvironment.setScaleFactor(this.mHighResPreviewScaleFactor);
        } else {
            this.mEnvironment.setScaleFactor(this.mPreviewScaleFactor);
        }
        this.mEnvironment.setQuality(1);
        this.mEnvironment.setImagePreset(preset);
        this.mEnvironment.setStop(false);
    }

    public void setOriginal(Bitmap bitmap) {
        this.mOriginalBitmap = bitmap;
        Log.v("CachingPipeline", "setOriginal, size " + bitmap.getWidth() + " x " + bitmap.getHeight());
        ImagePreset preset = MasterImage.getImage().getPreset();
        setupEnvironment(preset, false);
        updateOriginalAllocation(preset);
    }

    private synchronized boolean updateOriginalAllocation(ImagePreset preset) {
        boolean z = false;
        synchronized (this) {
            if (preset != null) {
                Bitmap originalBitmap = this.mOriginalBitmap;
                if (originalBitmap != null) {
                    RenderScript RS = getRenderScriptContext();
                    Allocation filtersOnlyOriginalAllocation = this.mFiltersOnlyOriginalAllocation;
                    this.mFiltersOnlyOriginalAllocation = Allocation.createFromBitmap(RS, originalBitmap, Allocation.MipmapControl.MIPMAP_NONE, 1);
                    if (filtersOnlyOriginalAllocation != null) {
                        filtersOnlyOriginalAllocation.destroy();
                    }
                    Allocation originalAllocation = this.mOriginalAllocation;
                    this.mResizedOriginalBitmap = preset.applyGeometry(originalBitmap, this.mEnvironment);
                    this.mOriginalAllocation = Allocation.createFromBitmap(RS, this.mResizedOriginalBitmap, Allocation.MipmapControl.MIPMAP_NONE, 1);
                    if (originalAllocation != null) {
                        originalAllocation.destroy();
                    }
                    z = true;
                }
            }
        }
        return z;
    }

    public void renderHighres(RenderingRequest request) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() != null) {
                ImagePreset preset = request.getImagePreset();
                setupEnvironment(preset, false);
                Bitmap bitmap = MasterImage.getImage().getOriginalBitmapHighres();
                if (bitmap != null) {
                    Bitmap bitmap2 = preset.applyGeometry(this.mEnvironment.getBitmapCopy(bitmap, 6), this.mEnvironment);
                    this.mEnvironment.setQuality(1);
                    Bitmap bmp = preset.apply(bitmap2, this.mEnvironment);
                    if (!this.mEnvironment.needsStop()) {
                        request.setBitmap(bmp);
                    } else {
                        this.mEnvironment.cache(bmp);
                    }
                    this.mFiltersManager.freeFilterResources(preset);
                }
            }
        }
    }

    public void renderGeometry(RenderingRequest request) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() != null) {
                ImagePreset preset = request.getImagePreset();
                setupEnvironment(preset, false);
                Bitmap bitmap = MasterImage.getImage().getOriginalBitmapHighres();
                if (bitmap != null) {
                    Bitmap bitmap2 = preset.applyGeometry(this.mEnvironment.getBitmapCopy(bitmap, 5), this.mEnvironment);
                    if (!this.mEnvironment.needsStop()) {
                        request.setBitmap(bitmap2);
                    } else {
                        this.mEnvironment.cache(bitmap2);
                    }
                    this.mFiltersManager.freeFilterResources(preset);
                }
            }
        }
    }

    public void renderFilters(RenderingRequest request) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() != null) {
                ImagePreset preset = request.getImagePreset();
                setupEnvironment(preset, false);
                Bitmap bitmap = MasterImage.getImage().getOriginalBitmapHighres();
                if (bitmap != null) {
                    Bitmap bitmap2 = preset.apply(this.mEnvironment.getBitmapCopy(bitmap, 4), this.mEnvironment);
                    if (!this.mEnvironment.needsStop()) {
                        request.setBitmap(bitmap2);
                    } else {
                        this.mEnvironment.cache(bitmap2);
                    }
                    this.mFiltersManager.freeFilterResources(preset);
                }
            }
        }
    }

    public synchronized void render(RenderingRequest request) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() != null) {
                if ((request.getType() == 4 || request.getType() == 3 || request.getBitmap() != null) && request.getImagePreset() != null) {
                    if (this.DEBUG) {
                        Log.v("CachingPipeline", "render image of type " + getType(request));
                    }
                    Bitmap bitmap = request.getBitmap();
                    ImagePreset preset = request.getImagePreset();
                    setupEnvironment(preset, true);
                    this.mFiltersManager.freeFilterResources(preset);
                    if (request.getType() == 4) {
                        MasterImage master = MasterImage.getImage();
                        bitmap = ImageLoader.getScaleOneImageForPreset(master.getActivity(), this.mEnvironment.getBimapCache(), master.getUri(), request.getBounds(), request.getDestination());
                        if (bitmap == null) {
                            Log.w("CachingPipeline", "could not get bitmap for: " + getType(request));
                        } else {
                            if (request.getType() == 0 || request.getType() == 2 || request.getType() == 1) {
                                updateOriginalAllocation(preset);
                            }
                            if (this.DEBUG && bitmap != null) {
                                Log.v("CachingPipeline", "after update, req bitmap (" + bitmap.getWidth() + "x" + bitmap.getHeight() + " ? resizeOriginal (" + this.mResizedOriginalBitmap.getWidth() + "x" + this.mResizedOriginalBitmap.getHeight());
                            }
                            if (request.getType() == 0 || request.getType() == 2) {
                                this.mOriginalAllocation.copyTo(bitmap);
                            } else if (request.getType() == 1) {
                                this.mFiltersOnlyOriginalAllocation.copyTo(bitmap);
                            }
                            if (request.getType() == 0 || request.getType() == 1 || request.getType() == 3 || request.getType() == 4 || request.getType() == 6) {
                                if (request.getType() == 3) {
                                    this.mEnvironment.setQuality(0);
                                } else {
                                    this.mEnvironment.setQuality(1);
                                }
                                if (request.getType() == 3) {
                                    Rect iconBounds = request.getIconBounds();
                                    Bitmap source = MasterImage.getImage().getThumbnailBitmap();
                                    if (iconBounds.width() > source.getWidth() * 2) {
                                        source = MasterImage.getImage().getLargeThumbnailBitmap();
                                    }
                                    if (iconBounds != null) {
                                        bitmap = this.mEnvironment.getBitmap(iconBounds.width(), iconBounds.height(), 3);
                                        Canvas canvas = new Canvas(bitmap);
                                        Matrix m = new Matrix();
                                        float minSize = Math.min(source.getWidth(), source.getHeight());
                                        float maxSize = Math.max(iconBounds.width(), iconBounds.height());
                                        float scale = maxSize / minSize;
                                        m.setScale(scale, scale);
                                        float dx = (iconBounds.width() - (source.getWidth() * scale)) / 2.0f;
                                        float dy = (iconBounds.height() - (source.getHeight() * scale)) / 2.0f;
                                        m.postTranslate(dx, dy);
                                        canvas.drawBitmap(source, m, new Paint(2));
                                    } else {
                                        bitmap = this.mEnvironment.getBitmapCopy(source, 3);
                                    }
                                }
                                Bitmap bmp = preset.apply(bitmap, this.mEnvironment);
                                if (!this.mEnvironment.needsStop()) {
                                    request.setBitmap(bmp);
                                }
                                this.mFiltersManager.freeFilterResources(preset);
                            }
                        }
                    }
                }
            }
        }
    }

    public synchronized Bitmap renderFinalImage(Bitmap bitmap, ImagePreset preset) {
        Bitmap bitmap2;
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                bitmap2 = bitmap;
            } else {
                setupEnvironment(preset, false);
                this.mEnvironment.setQuality(2);
                this.mEnvironment.setScaleFactor(1.0f);
                this.mFiltersManager.freeFilterResources(preset);
                bitmap2 = preset.apply(preset.applyGeometry(bitmap, this.mEnvironment), this.mEnvironment);
            }
        }
        return bitmap2;
    }

    public Bitmap renderGeometryIcon(Bitmap bitmap, ImagePreset preset) {
        return GeometryMathUtils.applyGeometryRepresentations(preset.getGeometryFilters(), bitmap);
    }

    public void compute(SharedBuffer buffer, ImagePreset preset, int type) {
        if (getRenderScriptContext() != null) {
            setupEnvironment(preset, false);
            Vector<FilterRepresentation> filters = preset.getFilters();
            Bitmap result = this.mCachedProcessing.process(this.mOriginalBitmap, filters, this.mEnvironment);
            buffer.setProducer(result);
            this.mEnvironment.cache(result);
        }
    }

    public void setPreviewScaleFactor(float previewScaleFactor) {
        this.mPreviewScaleFactor = previewScaleFactor;
    }

    public void setHighResPreviewScaleFactor(float highResPreviewScaleFactor) {
        this.mHighResPreviewScaleFactor = highResPreviewScaleFactor;
    }

    @Override
    public boolean prepareRenderscriptAllocations(Bitmap bitmap) {
        RenderScript RS = getRenderScriptContext();
        boolean needsUpdate = false;
        if (this.mOutPixelsAllocation == null || this.mInPixelsAllocation == null || bitmap.getWidth() != this.mWidth || bitmap.getHeight() != this.mHeight) {
            destroyPixelAllocations();
            Bitmap bitmapBuffer = bitmap;
            if (bitmap.getConfig() == null || bitmap.getConfig() != BITMAP_CONFIG) {
                bitmapBuffer = bitmap.copy(BITMAP_CONFIG, true);
            }
            this.mOutPixelsAllocation = Allocation.createFromBitmap(RS, bitmapBuffer, Allocation.MipmapControl.MIPMAP_NONE, 1);
            this.mInPixelsAllocation = Allocation.createTyped(RS, this.mOutPixelsAllocation.getType());
            needsUpdate = true;
        }
        if (RS != null) {
            this.mInPixelsAllocation.copyFrom(bitmap);
        }
        if (bitmap.getWidth() != this.mWidth || bitmap.getHeight() != this.mHeight) {
            this.mWidth = bitmap.getWidth();
            this.mHeight = bitmap.getHeight();
            needsUpdate = true;
        }
        if (this.DEBUG) {
            Log.v("CachingPipeline", "prepareRenderscriptAllocations: " + needsUpdate + " in " + getName());
        }
        return needsUpdate;
    }

    @Override
    public synchronized Allocation getInPixelsAllocation() {
        return this.mInPixelsAllocation;
    }

    @Override
    public synchronized Allocation getOutPixelsAllocation() {
        return this.mOutPixelsAllocation;
    }

    @Override
    public String getName() {
        return this.mName;
    }

    @Override
    public RenderScript getRSContext() {
        return getRenderScriptContext();
    }
}

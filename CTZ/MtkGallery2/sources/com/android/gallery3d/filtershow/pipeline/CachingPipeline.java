package com.android.gallery3d.filtershow.pipeline;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallerybasic.util.BitmapUtils;

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

    public CachingPipeline(FiltersManager filtersManager, String str) {
        this.mFiltersManager = null;
        this.mName = "";
        this.mFiltersManager = filtersManager;
        this.mName = str;
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
        if (sRS == null || sRS.getApplicationContext() == null) {
            return null;
        }
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

    private String getType(RenderingRequest renderingRequest) {
        if (renderingRequest.getType() == 3) {
            return "ICON_RENDERING";
        }
        if (renderingRequest.getType() == 1) {
            return "FILTERS_RENDERING";
        }
        if (renderingRequest.getType() == 0) {
            return "FULL_RENDERING";
        }
        if (renderingRequest.getType() == 2) {
            return "GEOMETRY_RENDERING";
        }
        if (renderingRequest.getType() == 4) {
            return "PARTIAL_RENDERING";
        }
        if (renderingRequest.getType() == 5) {
            return "HIGHRES_RENDERING";
        }
        return "UNKNOWN TYPE!";
    }

    private void setupEnvironment(ImagePreset imagePreset, boolean z) {
        this.mEnvironment.setPipeline(this);
        this.mEnvironment.setFiltersManager(this.mFiltersManager);
        this.mEnvironment.setBitmapCache(MasterImage.getImage().getBitmapCache());
        if (z) {
            this.mEnvironment.setScaleFactor(this.mHighResPreviewScaleFactor);
        } else {
            this.mEnvironment.setScaleFactor(this.mPreviewScaleFactor);
        }
        this.mEnvironment.setQuality(1);
        this.mEnvironment.setImagePreset(imagePreset);
        this.mEnvironment.setStop(false);
    }

    public void setOriginal(Bitmap bitmap) {
        this.mOriginalBitmap = bitmap;
        Log.v("CachingPipeline", "setOriginal, size " + bitmap.getWidth() + " x " + bitmap.getHeight());
        ImagePreset preset = MasterImage.getImage().getPreset();
        setupEnvironment(preset, false);
        updateOriginalAllocation(preset);
    }

    private synchronized boolean updateOriginalAllocation(ImagePreset imagePreset) {
        if (imagePreset == null) {
            return false;
        }
        Bitmap bitmap = this.mOriginalBitmap;
        if (bitmap == null) {
            return false;
        }
        RenderScript renderScriptContext = getRenderScriptContext();
        Allocation allocation = this.mFiltersOnlyOriginalAllocation;
        this.mFiltersOnlyOriginalAllocation = Allocation.createFromBitmap(renderScriptContext, bitmap, Allocation.MipmapControl.MIPMAP_NONE, 1);
        if (allocation != null) {
            allocation.destroy();
        }
        Allocation allocation2 = this.mOriginalAllocation;
        this.mResizedOriginalBitmap = imagePreset.applyGeometry(bitmap, this.mEnvironment);
        this.mOriginalAllocation = Allocation.createFromBitmap(renderScriptContext, this.mResizedOriginalBitmap, Allocation.MipmapControl.MIPMAP_NONE, 1);
        if (allocation2 != null) {
            allocation2.destroy();
        }
        return true;
    }

    public void renderHighres(RenderingRequest renderingRequest) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return;
            }
            ImagePreset imagePreset = renderingRequest.getImagePreset();
            setupEnvironment(imagePreset, false);
            Bitmap originalBitmapHighres = MasterImage.getImage().getOriginalBitmapHighres();
            if (originalBitmapHighres == null) {
                return;
            }
            Bitmap bitmapApplyGeometry = imagePreset.applyGeometry(this.mEnvironment.getBitmapCopy(originalBitmapHighres, 6), this.mEnvironment);
            this.mEnvironment.setQuality(1);
            Bitmap bitmapApply = imagePreset.apply(bitmapApplyGeometry, this.mEnvironment);
            if (!this.mEnvironment.needsStop()) {
                renderingRequest.setBitmap(bitmapApply);
            } else {
                this.mEnvironment.cache(bitmapApply);
            }
            this.mFiltersManager.freeFilterResources(imagePreset);
        }
    }

    public void renderGeometry(RenderingRequest renderingRequest) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return;
            }
            ImagePreset imagePreset = renderingRequest.getImagePreset();
            setupEnvironment(imagePreset, false);
            Bitmap originalBitmapHighres = MasterImage.getImage().getOriginalBitmapHighres();
            if (originalBitmapHighres == null) {
                return;
            }
            Bitmap bitmapApplyGeometry = imagePreset.applyGeometry(this.mEnvironment.getBitmapCopy(originalBitmapHighres, 5), this.mEnvironment);
            if (!this.mEnvironment.needsStop()) {
                renderingRequest.setBitmap(bitmapApplyGeometry);
            } else {
                this.mEnvironment.cache(bitmapApplyGeometry);
            }
            this.mFiltersManager.freeFilterResources(imagePreset);
        }
    }

    public void renderFilters(RenderingRequest renderingRequest) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return;
            }
            ImagePreset imagePreset = renderingRequest.getImagePreset();
            setupEnvironment(imagePreset, false);
            Bitmap originalBitmapHighres = MasterImage.getImage().getOriginalBitmapHighres();
            if (originalBitmapHighres == null) {
                return;
            }
            Bitmap bitmapCopy = this.mEnvironment.getBitmapCopy(originalBitmapHighres, 4);
            MasterImage.sIsRenderFilters = true;
            Bitmap bitmapApply = imagePreset.apply(bitmapCopy, this.mEnvironment);
            MasterImage.sIsRenderFilters = false;
            if (!this.mEnvironment.needsStop()) {
                renderingRequest.setBitmap(bitmapApply);
            } else {
                this.mEnvironment.cache(bitmapApply);
            }
            this.mFiltersManager.freeFilterResources(imagePreset);
        }
    }

    public synchronized void render(RenderingRequest renderingRequest) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return;
            }
            if ((renderingRequest.getType() == 4 || renderingRequest.getType() == 3 || renderingRequest.getBitmap() != null) && renderingRequest.getImagePreset() != null) {
                if (this.DEBUG) {
                    Log.v("CachingPipeline", "render image of type " + getType(renderingRequest));
                }
                Bitmap bitmap = renderingRequest.getBitmap();
                ImagePreset imagePreset = renderingRequest.getImagePreset();
                setupEnvironment(imagePreset, true);
                this.mFiltersManager.freeFilterResources(imagePreset);
                if (renderingRequest.getType() == 4) {
                    MasterImage image = MasterImage.getImage();
                    bitmap = BitmapUtils.replaceBackgroundColor(ImageLoader.getScaleOneImageForPreset(image.getActivity(), this.mEnvironment.getBimapCache(), image.getUri(), renderingRequest.getBounds(), renderingRequest.getDestination()), true);
                    if (bitmap == null) {
                        Log.w("CachingPipeline", "could not get bitmap for: " + getType(renderingRequest));
                        return;
                    }
                }
                if (renderingRequest.getType() == 0 || renderingRequest.getType() == 2 || renderingRequest.getType() == 1) {
                    updateOriginalAllocation(imagePreset);
                }
                if (this.DEBUG && bitmap != null) {
                    Log.v("CachingPipeline", "after update, req bitmap (" + bitmap.getWidth() + "x" + bitmap.getHeight() + " ? resizeOriginal (" + this.mResizedOriginalBitmap.getWidth() + "x" + this.mResizedOriginalBitmap.getHeight());
                }
                if (renderingRequest.getType() != 0 && renderingRequest.getType() != 2) {
                    if (renderingRequest.getType() == 1) {
                        this.mFiltersOnlyOriginalAllocation.copyTo(bitmap);
                    }
                } else {
                    this.mOriginalAllocation.copyTo(bitmap);
                }
                if (renderingRequest.getType() == 0 || renderingRequest.getType() == 1 || renderingRequest.getType() == 3 || renderingRequest.getType() == 4 || renderingRequest.getType() == 6) {
                    if (renderingRequest.getType() != 3) {
                        this.mEnvironment.setQuality(1);
                    } else {
                        this.mEnvironment.setQuality(0);
                    }
                    if (renderingRequest.getType() == 3) {
                        Rect iconBounds = renderingRequest.getIconBounds();
                        Bitmap thumbnailBitmap = MasterImage.getImage().getThumbnailBitmap();
                        if (thumbnailBitmap == null) {
                            return;
                        }
                        if (iconBounds.width() > thumbnailBitmap.getWidth() * 2) {
                            thumbnailBitmap = MasterImage.getImage().getLargeThumbnailBitmap();
                        }
                        if (iconBounds != null) {
                            Bitmap bitmap2 = this.mEnvironment.getBitmap(iconBounds.width(), iconBounds.height(), 3);
                            if (bitmap2 == null) {
                                return;
                            }
                            Canvas canvas = new Canvas(bitmap2);
                            Matrix matrix = new Matrix();
                            float fMax = Math.max(iconBounds.width(), iconBounds.height()) / Math.min(thumbnailBitmap.getWidth(), thumbnailBitmap.getHeight());
                            matrix.setScale(fMax, fMax);
                            matrix.postTranslate((iconBounds.width() - (thumbnailBitmap.getWidth() * fMax)) / 2.0f, (iconBounds.height() - (thumbnailBitmap.getHeight() * fMax)) / 2.0f);
                            canvas.drawBitmap(thumbnailBitmap, matrix, new Paint(2));
                            bitmap = bitmap2;
                        } else {
                            bitmap = this.mEnvironment.getBitmapCopy(thumbnailBitmap, 3);
                        }
                    }
                    Bitmap bitmapApply = imagePreset.apply(bitmap, this.mEnvironment);
                    if (!this.mEnvironment.needsStop()) {
                        renderingRequest.setBitmap(bitmapApply);
                    }
                    this.mFiltersManager.freeFilterResources(imagePreset);
                }
            }
        }
    }

    public synchronized Bitmap renderFinalImage(Bitmap bitmap, ImagePreset imagePreset) {
        synchronized (CachingPipeline.class) {
            if (getRenderScriptContext() == null) {
                return bitmap;
            }
            setupEnvironment(imagePreset, false);
            this.mEnvironment.setQuality(2);
            this.mEnvironment.setScaleFactor(1.0f);
            this.mFiltersManager.freeFilterResources(imagePreset);
            return imagePreset.apply(imagePreset.applyGeometry(bitmap, this.mEnvironment), this.mEnvironment);
        }
    }

    public Bitmap renderGeometryIcon(Bitmap bitmap, ImagePreset imagePreset) {
        return GeometryMathUtils.applyGeometryRepresentations(imagePreset.getGeometryFilters(), bitmap);
    }

    public void compute(SharedBuffer sharedBuffer, ImagePreset imagePreset, int i) {
        if (getRenderScriptContext() == null) {
            return;
        }
        setupEnvironment(imagePreset, false);
        Bitmap bitmapProcess = this.mCachedProcessing.process(this.mOriginalBitmap, imagePreset.getFilters(), this.mEnvironment);
        sharedBuffer.setProducer(bitmapProcess);
        this.mEnvironment.cache(bitmapProcess);
    }

    public void setPreviewScaleFactor(float f) {
        this.mPreviewScaleFactor = f;
    }

    public void setHighResPreviewScaleFactor(float f) {
        this.mHighResPreviewScaleFactor = f;
    }

    @Override
    public boolean prepareRenderscriptAllocations(Bitmap bitmap) {
        Bitmap bitmapCopy;
        boolean z;
        RenderScript renderScriptContext = getRenderScriptContext();
        if (this.mOutPixelsAllocation == null || this.mInPixelsAllocation == null || bitmap.getWidth() != this.mWidth || bitmap.getHeight() != this.mHeight) {
            destroyPixelAllocations();
            if (bitmap.getConfig() == null || bitmap.getConfig() != BITMAP_CONFIG) {
                bitmapCopy = bitmap.copy(BITMAP_CONFIG, true);
            } else {
                bitmapCopy = bitmap;
            }
            this.mOutPixelsAllocation = Allocation.createFromBitmap(renderScriptContext, bitmapCopy, Allocation.MipmapControl.MIPMAP_NONE, 1);
            this.mInPixelsAllocation = Allocation.createTyped(renderScriptContext, this.mOutPixelsAllocation.getType());
            z = true;
        } else {
            z = false;
        }
        if (renderScriptContext != null) {
            this.mInPixelsAllocation.copyFrom(bitmap);
        }
        if (bitmap.getWidth() != this.mWidth || bitmap.getHeight() != this.mHeight) {
            this.mWidth = bitmap.getWidth();
            this.mHeight = bitmap.getHeight();
            z = true;
        }
        if (this.DEBUG) {
            Log.v("CachingPipeline", "prepareRenderscriptAllocations: " + z + " in " + getName());
        }
        return z;
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

package com.android.printspooler.model;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import com.android.internal.annotations.GuardedBy;
import com.android.printspooler.renderer.IPdfRenderer;
import com.android.printspooler.renderer.PdfManipulationService;
import com.android.printspooler.util.BitmapSerializeUtils;
import com.android.printspooler.util.PageRangeUtils;
import dalvik.system.CloseGuard;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import libcore.io.IoUtils;

public final class PageContentRepository {
    private RenderSpec mLastRenderSpec;
    private final AsyncRenderer mRenderer;
    private PageRange[] mScheduledPreloadSelectedPages;
    private PageRange mScheduledPreloadVisiblePages;
    private PageRange[] mScheduledPreloadWrittenPages;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private int mState = 0;

    public interface OnPageContentAvailableCallback {
        void onPageContentAvailable(BitmapDrawable bitmapDrawable);
    }

    public PageContentRepository(Context context) {
        this.mRenderer = new AsyncRenderer(context);
        this.mCloseGuard.open("destroy");
    }

    public void open(ParcelFileDescriptor parcelFileDescriptor, OpenDocumentCallback openDocumentCallback) {
        throwIfNotClosed();
        this.mState = 1;
        this.mRenderer.open(parcelFileDescriptor, openDocumentCallback);
    }

    public void close(Runnable runnable) {
        throwIfNotOpened();
        this.mState = 0;
        this.mRenderer.close(runnable);
    }

    public void destroy(final Runnable runnable) {
        if (this.mState == 1) {
            close(new Runnable() {
                @Override
                public void run() {
                    PageContentRepository.this.destroy(runnable);
                }
            });
            return;
        }
        this.mCloseGuard.close();
        this.mState = 2;
        this.mRenderer.destroy();
        if (runnable != null) {
            runnable.run();
        }
    }

    public void startPreload(PageRange pageRange, PageRange[] pageRangeArr, PageRange[] pageRangeArr2) {
        if (this.mLastRenderSpec == null) {
            this.mScheduledPreloadVisiblePages = pageRange;
            this.mScheduledPreloadSelectedPages = pageRangeArr;
            this.mScheduledPreloadWrittenPages = pageRangeArr2;
        } else if (this.mState == 1) {
            this.mRenderer.startPreload(pageRange, pageRangeArr, pageRangeArr2, this.mLastRenderSpec);
        }
    }

    public void stopPreload() {
        this.mRenderer.stopPreload();
    }

    public int getFilePageCount() {
        return this.mRenderer.getPageCount();
    }

    public PageContentProvider acquirePageContentProvider(int i, View view) {
        throwIfDestroyed();
        return new PageContentProvider(i, view);
    }

    public void releasePageContentProvider(PageContentProvider pageContentProvider) {
        throwIfDestroyed();
        pageContentProvider.cancelLoad();
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            if (this.mState != 2) {
                destroy(null);
            }
        } finally {
            super.finalize();
        }
    }

    private void throwIfNotOpened() {
        if (this.mState != 1) {
            throw new IllegalStateException("Not opened");
        }
    }

    private void throwIfNotClosed() {
        if (this.mState != 0) {
            throw new IllegalStateException("Not closed");
        }
    }

    private void throwIfDestroyed() {
        if (this.mState == 2) {
            throw new IllegalStateException("Destroyed");
        }
    }

    public final class PageContentProvider {
        private View mOwner;
        private final int mPageIndex;

        public PageContentProvider(int i, View view) {
            this.mPageIndex = i;
            this.mOwner = view;
        }

        public void getPageContent(RenderSpec renderSpec, OnPageContentAvailableCallback onPageContentAvailableCallback) {
            PageContentRepository.this.throwIfDestroyed();
            PageContentRepository.this.mLastRenderSpec = renderSpec;
            if (PageContentRepository.this.mScheduledPreloadVisiblePages != null) {
                PageContentRepository.this.startPreload(PageContentRepository.this.mScheduledPreloadVisiblePages, PageContentRepository.this.mScheduledPreloadSelectedPages, PageContentRepository.this.mScheduledPreloadWrittenPages);
                PageContentRepository.this.mScheduledPreloadVisiblePages = null;
                PageContentRepository.this.mScheduledPreloadSelectedPages = null;
                PageContentRepository.this.mScheduledPreloadWrittenPages = null;
            }
            if (PageContentRepository.this.mState == 1) {
                PageContentRepository.this.mRenderer.renderPage(this.mPageIndex, renderSpec, onPageContentAvailableCallback);
            } else {
                PageContentRepository.this.mRenderer.getCachedPage(this.mPageIndex, renderSpec, onPageContentAvailableCallback);
            }
        }

        void cancelLoad() {
            PageContentRepository.this.throwIfDestroyed();
            if (PageContentRepository.this.mState == 1) {
                PageContentRepository.this.mRenderer.cancelRendering(this.mPageIndex);
            }
        }
    }

    private static final class PageContentLruCache {
        private final int mMaxSizeInBytes;
        private final LinkedHashMap<Integer, RenderedPage> mRenderedPages = new LinkedHashMap<>();
        private int mSizeInBytes;

        public PageContentLruCache(int i) {
            this.mMaxSizeInBytes = i;
        }

        public RenderedPage getRenderedPage(int i) {
            return this.mRenderedPages.get(Integer.valueOf(i));
        }

        public RenderedPage removeRenderedPage(int i) {
            RenderedPage renderedPageRemove = this.mRenderedPages.remove(Integer.valueOf(i));
            if (renderedPageRemove != null) {
                this.mSizeInBytes -= renderedPageRemove.getSizeInBytes();
            }
            return renderedPageRemove;
        }

        public RenderedPage putRenderedPage(int i, RenderedPage renderedPage) {
            RenderedPage renderedPageRemove = this.mRenderedPages.remove(Integer.valueOf(i));
            if (renderedPageRemove != null) {
                if (!renderedPageRemove.renderSpec.equals(renderedPage.renderSpec)) {
                    throw new IllegalStateException("Wrong page size");
                }
            } else {
                int sizeInBytes = renderedPage.getSizeInBytes();
                if (this.mSizeInBytes + sizeInBytes > this.mMaxSizeInBytes) {
                    throw new IllegalStateException("Client didn't free space");
                }
                this.mSizeInBytes += sizeInBytes;
            }
            return this.mRenderedPages.put(Integer.valueOf(i), renderedPage);
        }

        public void invalidate() {
            Iterator<Map.Entry<Integer, RenderedPage>> it = this.mRenderedPages.entrySet().iterator();
            while (it.hasNext()) {
                it.next().getValue().state = 2;
            }
        }

        public RenderedPage removeLeastNeeded() {
            if (this.mRenderedPages.isEmpty()) {
                return null;
            }
            for (Map.Entry<Integer, RenderedPage> entry : this.mRenderedPages.entrySet()) {
                RenderedPage value = entry.getValue();
                if (value.state == 2) {
                    this.mRenderedPages.remove(entry.getKey());
                    this.mSizeInBytes -= value.getSizeInBytes();
                    return value;
                }
            }
            RenderedPage renderedPageRemove = this.mRenderedPages.remove(Integer.valueOf(((Integer) this.mRenderedPages.eldest().getKey()).intValue()));
            this.mSizeInBytes -= renderedPageRemove.getSizeInBytes();
            return renderedPageRemove;
        }

        public int getSizeInBytes() {
            return this.mSizeInBytes;
        }

        public int getMaxSizeInBytes() {
            return this.mMaxSizeInBytes;
        }

        public void clear() {
            Iterator<Map.Entry<Integer, RenderedPage>> it = this.mRenderedPages.entrySet().iterator();
            while (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    public static final class RenderSpec {
        final int bitmapHeight;
        final int bitmapWidth;
        final PrintAttributes printAttributes = new PrintAttributes.Builder().build();

        public RenderSpec(int i, int i2, PrintAttributes.MediaSize mediaSize, PrintAttributes.Margins margins) {
            this.bitmapWidth = i;
            this.bitmapHeight = i2;
            this.printAttributes.setMediaSize(mediaSize);
            this.printAttributes.setMinMargins(margins);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            RenderSpec renderSpec = (RenderSpec) obj;
            if (this.bitmapHeight != renderSpec.bitmapHeight || this.bitmapWidth != renderSpec.bitmapWidth) {
                return false;
            }
            if (this.printAttributes != null) {
                if (!this.printAttributes.equals(renderSpec.printAttributes)) {
                    return false;
                }
            } else if (renderSpec.printAttributes != null) {
                return false;
            }
            return true;
        }

        public boolean hasSameSize(RenderedPage renderedPage) {
            Bitmap bitmap = renderedPage.content.getBitmap();
            return bitmap.getWidth() == this.bitmapWidth && bitmap.getHeight() == this.bitmapHeight;
        }

        public int hashCode() {
            return (31 * ((this.bitmapWidth * 31) + this.bitmapHeight)) + (this.printAttributes != null ? this.printAttributes.hashCode() : 0);
        }
    }

    private static final class RenderedPage {
        final BitmapDrawable content;
        RenderSpec renderSpec;
        int state = 2;

        RenderedPage(BitmapDrawable bitmapDrawable) {
            this.content = bitmapDrawable;
        }

        public int getSizeInBytes() {
            return this.content.getBitmap().getByteCount();
        }

        public void erase() {
            this.content.getBitmap().eraseColor(-1);
        }
    }

    private static final class AsyncRenderer implements ServiceConnection {
        private boolean mBoundToService;
        private final Context mContext;
        private boolean mDestroyed;
        private OpenTask mOpenTask;
        private final PageContentLruCache mPageContentCache;

        @GuardedBy("mLock")
        private IPdfRenderer mRenderer;
        private final Object mLock = new Object();
        private final ArrayMap<Integer, RenderPageTask> mPageToRenderTaskMap = new ArrayMap<>();
        private int mPageCount = -1;

        public AsyncRenderer(Context context) {
            this.mContext = context;
            this.mPageContentCache = new PageContentLruCache((((ActivityManager) this.mContext.getSystemService("activity")).getMemoryClass() * 1048576) / 4);
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (this.mLock) {
                this.mRenderer = IPdfRenderer.Stub.asInterface(iBinder);
                this.mLock.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (this.mLock) {
                this.mRenderer = null;
            }
        }

        public void open(ParcelFileDescriptor parcelFileDescriptor, OpenDocumentCallback openDocumentCallback) {
            this.mPageContentCache.invalidate();
            this.mOpenTask = new OpenTask(parcelFileDescriptor, openDocumentCallback);
            this.mOpenTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Void[0]);
        }

        public void close(final Runnable runnable) {
            cancelAllRendering();
            if (this.mOpenTask != null) {
                this.mOpenTask.cancel();
            }
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected void onPreExecute() {
                    if (AsyncRenderer.this.mDestroyed) {
                        cancel(true);
                    }
                }

                @Override
                protected Void doInBackground(Void... voidArr) {
                    synchronized (AsyncRenderer.this.mLock) {
                        try {
                            if (AsyncRenderer.this.mRenderer != null) {
                                AsyncRenderer.this.mRenderer.closeDocument();
                            }
                        } catch (RemoteException e) {
                        }
                    }
                    return null;
                }

                @Override
                public void onPostExecute(Void r2) {
                    AsyncRenderer.this.mPageCount = -1;
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Void[0]);
        }

        public void destroy() {
            if (this.mBoundToService) {
                this.mBoundToService = false;
                try {
                    this.mContext.unbindService(this);
                } catch (IllegalArgumentException e) {
                    Log.e("PageContentRepository", "Cannot unbind service", e);
                }
            }
            this.mPageContentCache.invalidate();
            this.mPageContentCache.clear();
            this.mDestroyed = true;
        }

        private int findIndexOfPage(int i, PageRange[] pageRangeArr) {
            int size = 0;
            for (int i2 = 0; i2 < pageRangeArr.length; i2++) {
                if (pageRangeArr[i2].contains(i)) {
                    return (size + i) - pageRangeArr[i2].getStart();
                }
                size += pageRangeArr[i2].getSize();
            }
            return -1;
        }

        void startPreload(PageRange pageRange, PageRange[] pageRangeArr, PageRange[] pageRangeArr2, RenderSpec renderSpec) {
            if (PageRangeUtils.isAllPages(pageRangeArr)) {
                pageRangeArr = new PageRange[]{new PageRange(0, this.mPageCount - 1)};
            }
            int iFindIndexOfPage = findIndexOfPage(pageRange.getStart(), pageRangeArr);
            int iFindIndexOfPage2 = findIndexOfPage(pageRange.getEnd(), pageRangeArr);
            if (iFindIndexOfPage == -1 || iFindIndexOfPage2 == -1) {
                return;
            }
            int maxSizeInBytes = (((this.mPageContentCache.getMaxSizeInBytes() / ((renderSpec.bitmapWidth * renderSpec.bitmapHeight) * 4)) - (iFindIndexOfPage2 - iFindIndexOfPage)) / 2) - 1;
            int iMax = Math.max(iFindIndexOfPage - maxSizeInBytes, 0);
            int i = iFindIndexOfPage2 + maxSizeInBytes;
            int size = 0;
            for (PageRange pageRange2 : pageRangeArr) {
                int iMin = Math.min(pageRange2.getSize(), (i - size) + 1);
                for (int iMax2 = Math.max(0, iMax - size); iMax2 < iMin; iMax2++) {
                    if (PageRangeUtils.contains(pageRangeArr2, pageRange2.getStart() + iMax2)) {
                        renderPage(pageRange2.getStart() + iMax2, renderSpec, null);
                    }
                }
                size += pageRange2.getSize();
            }
        }

        public void stopPreload() {
            int size = this.mPageToRenderTaskMap.size();
            for (int i = 0; i < size; i++) {
                RenderPageTask renderPageTaskValueAt = this.mPageToRenderTaskMap.valueAt(i);
                if (renderPageTaskValueAt.isPreload() && !renderPageTaskValueAt.isCancelled()) {
                    renderPageTaskValueAt.cancel(true);
                }
            }
        }

        public int getPageCount() {
            return this.mPageCount;
        }

        public void getCachedPage(int i, RenderSpec renderSpec, OnPageContentAvailableCallback onPageContentAvailableCallback) {
            RenderedPage renderedPage = this.mPageContentCache.getRenderedPage(i);
            if (renderedPage != null && renderedPage.state == 0 && renderedPage.renderSpec.equals(renderSpec) && onPageContentAvailableCallback != null) {
                onPageContentAvailableCallback.onPageContentAvailable(renderedPage.content);
            }
        }

        public void renderPage(int i, RenderSpec renderSpec, OnPageContentAvailableCallback onPageContentAvailableCallback) {
            RenderedPage renderedPage = this.mPageContentCache.getRenderedPage(i);
            if (renderedPage != null && renderedPage.state == 0) {
                if (renderedPage.renderSpec.equals(renderSpec)) {
                    if (onPageContentAvailableCallback != null) {
                        onPageContentAvailableCallback.onPageContentAvailable(renderedPage.content);
                        return;
                    }
                    return;
                }
                renderedPage.state = 2;
            }
            RenderPageTask renderPageTask = this.mPageToRenderTaskMap.get(Integer.valueOf(i));
            if (renderPageTask != null && !renderPageTask.isCancelled()) {
                if (renderPageTask.mRenderSpec.equals(renderSpec)) {
                    if (renderPageTask.mCallback != null) {
                        if (onPageContentAvailableCallback != null && renderPageTask.mCallback != onPageContentAvailableCallback) {
                            throw new IllegalStateException("Page rendering not cancelled");
                        }
                        return;
                    }
                    renderPageTask.mCallback = onPageContentAvailableCallback;
                    return;
                }
                renderPageTask.cancel(true);
            }
            RenderPageTask renderPageTask2 = new RenderPageTask(i, renderSpec, onPageContentAvailableCallback);
            this.mPageToRenderTaskMap.put(Integer.valueOf(i), renderPageTask2);
            renderPageTask2.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Void[0]);
        }

        public void cancelRendering(int i) {
            RenderPageTask renderPageTask = this.mPageToRenderTaskMap.get(Integer.valueOf(i));
            if (renderPageTask != null && !renderPageTask.isCancelled()) {
                renderPageTask.cancel(true);
            }
        }

        private void cancelAllRendering() {
            int size = this.mPageToRenderTaskMap.size();
            for (int i = 0; i < size; i++) {
                RenderPageTask renderPageTaskValueAt = this.mPageToRenderTaskMap.valueAt(i);
                if (!renderPageTaskValueAt.isCancelled()) {
                    renderPageTaskValueAt.cancel(true);
                }
            }
        }

        private final class OpenTask extends AsyncTask<Void, Void, Integer> {
            private final OpenDocumentCallback mCallback;
            private final ParcelFileDescriptor mSource;

            public OpenTask(ParcelFileDescriptor parcelFileDescriptor, OpenDocumentCallback openDocumentCallback) {
                this.mSource = parcelFileDescriptor;
                this.mCallback = openDocumentCallback;
            }

            @Override
            protected void onPreExecute() {
                if (AsyncRenderer.this.mDestroyed) {
                    cancel(true);
                    return;
                }
                Intent intent = new Intent("com.android.printspooler.renderer.ACTION_GET_RENDERER");
                intent.setClass(AsyncRenderer.this.mContext, PdfManipulationService.class);
                intent.setData(Uri.fromParts("fake-scheme", String.valueOf(AsyncRenderer.this.hashCode()), null));
                AsyncRenderer.this.mContext.bindService(intent, AsyncRenderer.this, 1);
                AsyncRenderer.this.mBoundToService = true;
            }

            @Override
            protected Integer doInBackground(Void... voidArr) {
                Integer numValueOf;
                synchronized (AsyncRenderer.this.mLock) {
                    while (AsyncRenderer.this.mRenderer == null && !isCancelled()) {
                        try {
                            AsyncRenderer.this.mLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    try {
                        try {
                            numValueOf = Integer.valueOf(AsyncRenderer.this.mRenderer.openDocument(this.mSource));
                        } finally {
                            IoUtils.closeQuietly(this.mSource);
                        }
                    } catch (RemoteException e2) {
                        Log.e("PageContentRepository", "Cannot open PDF document");
                        return -2;
                    }
                }
                return numValueOf;
            }

            @Override
            public void onPostExecute(Integer num) {
                switch (num.intValue()) {
                    case -3:
                        AsyncRenderer.this.mPageCount = -1;
                        if (this.mCallback != null) {
                            this.mCallback.onFailure(-2);
                        }
                        break;
                    case -2:
                        AsyncRenderer.this.mPageCount = -1;
                        if (this.mCallback != null) {
                            this.mCallback.onFailure(-1);
                        }
                        break;
                    default:
                        AsyncRenderer.this.mPageCount = num.intValue();
                        if (this.mCallback != null) {
                            this.mCallback.onSuccess();
                        }
                        break;
                }
                AsyncRenderer.this.mOpenTask = null;
            }

            @Override
            protected void onCancelled(Integer num) {
                AsyncRenderer.this.mOpenTask = null;
            }

            public void cancel() {
                cancel(true);
                synchronized (AsyncRenderer.this.mLock) {
                    AsyncRenderer.this.mLock.notifyAll();
                }
            }
        }

        private final class RenderPageTask extends AsyncTask<Void, Void, RenderedPage> {
            OnPageContentAvailableCallback mCallback;
            private boolean mIsFailed;
            final int mPageIndex;
            final RenderSpec mRenderSpec;
            RenderedPage mRenderedPage;

            public RenderPageTask(int i, RenderSpec renderSpec, OnPageContentAvailableCallback onPageContentAvailableCallback) {
                this.mPageIndex = i;
                this.mRenderSpec = renderSpec;
                this.mCallback = onPageContentAvailableCallback;
            }

            @Override
            protected void onPreExecute() {
                this.mRenderedPage = AsyncRenderer.this.mPageContentCache.getRenderedPage(this.mPageIndex);
                if (this.mRenderedPage != null && this.mRenderedPage.state == 0) {
                    throw new IllegalStateException("Trying to render a rendered page");
                }
                if (this.mRenderedPage != null && !this.mRenderSpec.hasSameSize(this.mRenderedPage)) {
                    AsyncRenderer.this.mPageContentCache.removeRenderedPage(this.mPageIndex);
                    this.mRenderedPage = null;
                }
                int i = this.mRenderSpec.bitmapWidth * this.mRenderSpec.bitmapHeight * 4;
                while (true) {
                    if (this.mRenderedPage == null && AsyncRenderer.this.mPageContentCache.getSizeInBytes() > 0 && AsyncRenderer.this.mPageContentCache.getSizeInBytes() + i > AsyncRenderer.this.mPageContentCache.getMaxSizeInBytes()) {
                        RenderedPage renderedPageRemoveLeastNeeded = AsyncRenderer.this.mPageContentCache.removeLeastNeeded();
                        if (this.mRenderSpec.hasSameSize(renderedPageRemoveLeastNeeded)) {
                            this.mRenderedPage = renderedPageRemoveLeastNeeded;
                            renderedPageRemoveLeastNeeded.erase();
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (this.mRenderedPage == null) {
                    Bitmap bitmapCreateBitmap = Bitmap.createBitmap(this.mRenderSpec.bitmapWidth, this.mRenderSpec.bitmapHeight, Bitmap.Config.ARGB_8888);
                    bitmapCreateBitmap.eraseColor(-1);
                    this.mRenderedPage = new RenderedPage(new BitmapDrawable(AsyncRenderer.this.mContext.getResources(), bitmapCreateBitmap));
                }
                this.mRenderedPage.renderSpec = this.mRenderSpec;
                this.mRenderedPage.state = 1;
                AsyncRenderer.this.mPageContentCache.putRenderedPage(this.mPageIndex, this.mRenderedPage);
            }

            @Override
            protected RenderedPage doInBackground(Void... voidArr) {
                ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe;
                ParcelFileDescriptor parcelFileDescriptor;
                Throwable th;
                if (isCancelled()) {
                    return this.mRenderedPage;
                }
                Bitmap bitmap = this.mRenderedPage.content.getBitmap();
                try {
                    parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
                    parcelFileDescriptor = parcelFileDescriptorArrCreatePipe[0];
                } catch (RemoteException | IOException | IllegalStateException e) {
                    Log.e("PageContentRepository", "Error rendering page " + this.mPageIndex, e);
                    this.mIsFailed = true;
                }
                try {
                    ParcelFileDescriptor parcelFileDescriptor2 = parcelFileDescriptorArrCreatePipe[1];
                    try {
                        synchronized (AsyncRenderer.this.mLock) {
                            if (AsyncRenderer.this.mRenderer != null) {
                                AsyncRenderer.this.mRenderer.renderPage(this.mPageIndex, bitmap.getWidth(), bitmap.getHeight(), this.mRenderSpec.printAttributes, parcelFileDescriptor2);
                            } else {
                                throw new IllegalStateException("Renderer is disconnected");
                            }
                        }
                        if (parcelFileDescriptor2 != null) {
                            $closeResource(null, parcelFileDescriptor2);
                        }
                        BitmapSerializeUtils.readBitmapPixels(bitmap, parcelFileDescriptor);
                        this.mIsFailed = false;
                        return this.mRenderedPage;
                    } catch (Throwable th2) {
                        th = th2;
                        th = null;
                        if (parcelFileDescriptor2 != null) {
                        }
                    }
                } finally {
                    if (parcelFileDescriptor != null) {
                        $closeResource(null, parcelFileDescriptor);
                    }
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

            @Override
            public void onPostExecute(RenderedPage renderedPage) {
                AsyncRenderer.this.mPageToRenderTaskMap.remove(Integer.valueOf(this.mPageIndex));
                if (this.mIsFailed) {
                    renderedPage.state = 2;
                } else {
                    renderedPage.state = 0;
                }
                this.mRenderedPage.content.invalidateSelf();
                if (this.mCallback != null) {
                    if (this.mIsFailed) {
                        this.mCallback.onPageContentAvailable(null);
                    } else {
                        this.mCallback.onPageContentAvailable(renderedPage.content);
                    }
                }
            }

            @Override
            protected void onCancelled(RenderedPage renderedPage) {
                AsyncRenderer.this.mPageToRenderTaskMap.remove(Integer.valueOf(this.mPageIndex));
                if (renderedPage == null) {
                    return;
                }
                renderedPage.state = 2;
            }

            public boolean isPreload() {
                return this.mCallback == null;
            }
        }
    }
}

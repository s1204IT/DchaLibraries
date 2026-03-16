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
import android.print.PrintAttributes;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import com.android.internal.annotations.GuardedBy;
import com.android.printspooler.renderer.IPdfRenderer;
import com.android.printspooler.renderer.PdfManipulationService;
import com.android.printspooler.util.BitmapSerializeUtils;
import dalvik.system.CloseGuard;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import libcore.io.IoUtils;

public final class PageContentRepository {
    private RenderSpec mLastRenderSpec;
    private final AsyncRenderer mRenderer;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private int mScheduledPreloadFirstShownPage = -1;
    private int mScheduledPreloadLastShownPage = -1;
    private int mState = 0;

    public interface OnPageContentAvailableCallback {
        void onPageContentAvailable(BitmapDrawable bitmapDrawable);
    }

    public PageContentRepository(Context context) {
        this.mRenderer = new AsyncRenderer(context);
        this.mCloseGuard.open("destroy");
    }

    public void open(ParcelFileDescriptor source, OpenDocumentCallback callback) {
        throwIfNotClosed();
        this.mState = 1;
        this.mRenderer.open(source, callback);
    }

    public void close(Runnable callback) {
        throwIfNotOpened();
        this.mState = 0;
        this.mRenderer.close(callback);
    }

    public void destroy(final Runnable callback) {
        if (this.mState == 1) {
            close(new Runnable() {
                @Override
                public void run() {
                    PageContentRepository.this.destroy(callback);
                }
            });
            return;
        }
        this.mState = 2;
        this.mRenderer.destroy();
        if (callback != null) {
            callback.run();
        }
    }

    public void startPreload(int firstShownPage, int lastShownPage) {
        if (this.mLastRenderSpec == null) {
            this.mScheduledPreloadFirstShownPage = firstShownPage;
            this.mScheduledPreloadLastShownPage = lastShownPage;
        } else if (this.mState == 1) {
            this.mRenderer.startPreload(firstShownPage, lastShownPage, this.mLastRenderSpec);
        }
    }

    public void stopPreload() {
        this.mRenderer.stopPreload();
    }

    public int getFilePageCount() {
        return this.mRenderer.getPageCount();
    }

    public PageContentProvider acquirePageContentProvider(int pageIndex, View owner) {
        throwIfDestroyed();
        return new PageContentProvider(pageIndex, owner);
    }

    public void releasePageContentProvider(PageContentProvider provider) {
        throwIfDestroyed();
        provider.cancelLoad();
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mState != 2) {
                this.mCloseGuard.warnIfOpen();
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

        public PageContentProvider(int pageIndex, View owner) {
            this.mPageIndex = pageIndex;
            this.mOwner = owner;
        }

        public void getPageContent(RenderSpec renderSpec, OnPageContentAvailableCallback callback) {
            PageContentRepository.this.throwIfDestroyed();
            PageContentRepository.this.mLastRenderSpec = renderSpec;
            if (PageContentRepository.this.mScheduledPreloadFirstShownPage != -1 && PageContentRepository.this.mScheduledPreloadLastShownPage != -1) {
                PageContentRepository.this.startPreload(PageContentRepository.this.mScheduledPreloadFirstShownPage, PageContentRepository.this.mScheduledPreloadLastShownPage);
                PageContentRepository.this.mScheduledPreloadFirstShownPage = -1;
                PageContentRepository.this.mScheduledPreloadLastShownPage = -1;
            }
            if (PageContentRepository.this.mState == 1) {
                PageContentRepository.this.mRenderer.renderPage(this.mPageIndex, renderSpec, callback);
            } else {
                PageContentRepository.this.mRenderer.getCachedPage(this.mPageIndex, renderSpec, callback);
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

        public PageContentLruCache(int maxSizeInBytes) {
            this.mMaxSizeInBytes = maxSizeInBytes;
        }

        public RenderedPage getRenderedPage(int pageIndex) {
            return this.mRenderedPages.get(Integer.valueOf(pageIndex));
        }

        public RenderedPage removeRenderedPage(int pageIndex) {
            RenderedPage page = this.mRenderedPages.remove(Integer.valueOf(pageIndex));
            if (page != null) {
                this.mSizeInBytes -= page.getSizeInBytes();
            }
            return page;
        }

        public RenderedPage putRenderedPage(int pageIndex, RenderedPage renderedPage) {
            RenderedPage oldRenderedPage = this.mRenderedPages.remove(Integer.valueOf(pageIndex));
            if (oldRenderedPage != null) {
                if (!oldRenderedPage.renderSpec.equals(renderedPage.renderSpec)) {
                    throw new IllegalStateException("Wrong page size");
                }
            } else {
                int contentSizeInBytes = renderedPage.getSizeInBytes();
                if (this.mSizeInBytes + contentSizeInBytes > this.mMaxSizeInBytes) {
                    throw new IllegalStateException("Client didn't free space");
                }
                this.mSizeInBytes += contentSizeInBytes;
            }
            return this.mRenderedPages.put(Integer.valueOf(pageIndex), renderedPage);
        }

        public void invalidate() {
            for (Map.Entry<Integer, RenderedPage> entry : this.mRenderedPages.entrySet()) {
                entry.getValue().state = 2;
            }
        }

        public RenderedPage removeLeastNeeded() {
            if (this.mRenderedPages.isEmpty()) {
                return null;
            }
            for (Map.Entry<Integer, RenderedPage> entry : this.mRenderedPages.entrySet()) {
                RenderedPage renderedPage = entry.getValue();
                if (renderedPage.state == 2) {
                    Integer pageIndex = entry.getKey();
                    this.mRenderedPages.remove(pageIndex);
                    this.mSizeInBytes -= renderedPage.getSizeInBytes();
                    return renderedPage;
                }
            }
            int pageIndex2 = ((Integer) this.mRenderedPages.eldest().getKey()).intValue();
            RenderedPage renderedPage2 = this.mRenderedPages.remove(Integer.valueOf(pageIndex2));
            this.mSizeInBytes -= renderedPage2.getSizeInBytes();
            return renderedPage2;
        }

        public int getSizeInBytes() {
            return this.mSizeInBytes;
        }

        public int getMaxSizeInBytes() {
            return this.mMaxSizeInBytes;
        }

        public void clear() {
            Iterator<Map.Entry<Integer, RenderedPage>> iterator = this.mRenderedPages.entrySet().iterator();
            while (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    public static final class RenderSpec {
        final int bitmapHeight;
        final int bitmapWidth;
        final PrintAttributes printAttributes = new PrintAttributes.Builder().build();

        public RenderSpec(int bitmapWidth, int bitmapHeight, PrintAttributes.MediaSize mediaSize, PrintAttributes.Margins minMargins) {
            this.bitmapWidth = bitmapWidth;
            this.bitmapHeight = bitmapHeight;
            this.printAttributes.setMediaSize(mediaSize);
            this.printAttributes.setMinMargins(minMargins);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                RenderSpec other = (RenderSpec) obj;
                if (this.bitmapHeight == other.bitmapHeight && this.bitmapWidth == other.bitmapWidth) {
                    return this.printAttributes != null ? this.printAttributes.equals(other.printAttributes) : other.printAttributes == null;
                }
                return false;
            }
            return false;
        }

        public boolean hasSameSize(RenderedPage page) {
            Bitmap bitmap = page.content.getBitmap();
            return bitmap.getWidth() == this.bitmapWidth && bitmap.getHeight() == this.bitmapHeight;
        }

        public int hashCode() {
            int result = this.bitmapWidth;
            return (((result * 31) + this.bitmapHeight) * 31) + (this.printAttributes != null ? this.printAttributes.hashCode() : 0);
        }
    }

    private static final class RenderedPage {
        final BitmapDrawable content;
        RenderSpec renderSpec;
        int state = 2;

        RenderedPage(BitmapDrawable content) {
            this.content = content;
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
            ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
            int cacheSizeInBytes = (activityManager.getMemoryClass() * 1048576) / 4;
            this.mPageContentCache = new PageContentLruCache(cacheSizeInBytes);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (this.mLock) {
                this.mRenderer = IPdfRenderer.Stub.asInterface(service);
                this.mLock.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (this.mLock) {
                this.mRenderer = null;
            }
        }

        public void open(ParcelFileDescriptor source, OpenDocumentCallback callback) {
            this.mPageContentCache.invalidate();
            this.mOpenTask = new OpenTask(source, callback);
            this.mOpenTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Void[0]);
        }

        public void close(final Runnable callback) {
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
                protected Void doInBackground(Void... params) {
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
                public void onPostExecute(Void result) {
                    AsyncRenderer.this.mPageCount = -1;
                    if (callback != null) {
                        callback.run();
                    }
                }
            }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Void[0]);
        }

        public void destroy() {
            if (this.mBoundToService) {
                this.mBoundToService = false;
                this.mContext.unbindService(this);
            }
            this.mPageContentCache.invalidate();
            this.mPageContentCache.clear();
            this.mDestroyed = true;
        }

        public void startPreload(int firstShownPage, int lastShownPage, RenderSpec renderSpec) {
            int excessFromStart;
            int excessFromEnd;
            int bitmapSizeInBytes = renderSpec.bitmapWidth * renderSpec.bitmapHeight * 4;
            int maxCachedPageCount = this.mPageContentCache.getMaxSizeInBytes() / bitmapSizeInBytes;
            int halfPreloadCount = ((maxCachedPageCount - (lastShownPage - firstShownPage)) / 2) - 1;
            if (firstShownPage - halfPreloadCount < 0) {
                excessFromStart = halfPreloadCount - firstShownPage;
            } else {
                excessFromStart = 0;
            }
            if (lastShownPage + halfPreloadCount >= this.mPageCount) {
                excessFromEnd = (lastShownPage + halfPreloadCount) - this.mPageCount;
            } else {
                excessFromEnd = 0;
            }
            int fromIndex = Math.max((firstShownPage - halfPreloadCount) - excessFromEnd, 0);
            int toIndex = Math.min(lastShownPage + halfPreloadCount + excessFromStart, this.mPageCount - 1);
            for (int i = fromIndex; i <= toIndex; i++) {
                renderPage(i, renderSpec, null);
            }
        }

        public void stopPreload() {
            int taskCount = this.mPageToRenderTaskMap.size();
            for (int i = 0; i < taskCount; i++) {
                RenderPageTask task = this.mPageToRenderTaskMap.valueAt(i);
                if (task.isPreload() && !task.isCancelled()) {
                    task.cancel(true);
                }
            }
        }

        public int getPageCount() {
            return this.mPageCount;
        }

        public void getCachedPage(int pageIndex, RenderSpec renderSpec, OnPageContentAvailableCallback callback) {
            RenderedPage renderedPage = this.mPageContentCache.getRenderedPage(pageIndex);
            if (renderedPage != null && renderedPage.state == 0 && renderedPage.renderSpec.equals(renderSpec) && callback != null) {
                callback.onPageContentAvailable(renderedPage.content);
            }
        }

        public void renderPage(int pageIndex, RenderSpec renderSpec, OnPageContentAvailableCallback callback) {
            RenderedPage renderedPage = this.mPageContentCache.getRenderedPage(pageIndex);
            if (renderedPage != null && renderedPage.state == 0) {
                if (renderedPage.renderSpec.equals(renderSpec)) {
                    if (callback != null) {
                        callback.onPageContentAvailable(renderedPage.content);
                        return;
                    }
                    return;
                }
                renderedPage.state = 2;
            }
            RenderPageTask renderTask = this.mPageToRenderTaskMap.get(Integer.valueOf(pageIndex));
            if (renderTask != null && !renderTask.isCancelled()) {
                if (renderTask.mRenderSpec.equals(renderSpec)) {
                    if (renderTask.mCallback != null) {
                        if (callback != null && renderTask.mCallback != callback) {
                            throw new IllegalStateException("Page rendering not cancelled");
                        }
                        return;
                    }
                    renderTask.mCallback = callback;
                    return;
                }
                renderTask.cancel(true);
            }
            RenderPageTask renderTask2 = new RenderPageTask(pageIndex, renderSpec, callback);
            this.mPageToRenderTaskMap.put(Integer.valueOf(pageIndex), renderTask2);
            renderTask2.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Void[0]);
        }

        public void cancelRendering(int pageIndex) {
            RenderPageTask task = this.mPageToRenderTaskMap.get(Integer.valueOf(pageIndex));
            if (task != null && !task.isCancelled()) {
                task.cancel(true);
            }
        }

        private void cancelAllRendering() {
            int taskCount = this.mPageToRenderTaskMap.size();
            for (int i = 0; i < taskCount; i++) {
                RenderPageTask task = this.mPageToRenderTaskMap.valueAt(i);
                if (!task.isCancelled()) {
                    task.cancel(true);
                }
            }
        }

        private final class OpenTask extends AsyncTask<Void, Void, Integer> {
            private final OpenDocumentCallback mCallback;
            private final ParcelFileDescriptor mSource;

            public OpenTask(ParcelFileDescriptor source, OpenDocumentCallback callback) {
                this.mSource = source;
                this.mCallback = callback;
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
            protected Integer doInBackground(Void... params) {
                int iValueOf;
                synchronized (AsyncRenderer.this.mLock) {
                    while (AsyncRenderer.this.mRenderer == null && !isCancelled()) {
                        try {
                            AsyncRenderer.this.mLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    try {
                        try {
                            iValueOf = Integer.valueOf(AsyncRenderer.this.mRenderer.openDocument(this.mSource));
                        } catch (RemoteException e2) {
                            Log.e("PageContentRepository", "Cannot open PDF document");
                            iValueOf = -2;
                        }
                    } finally {
                        IoUtils.closeQuietly(this.mSource);
                    }
                }
                return iValueOf;
            }

            @Override
            public void onPostExecute(Integer pageCount) {
                switch (pageCount.intValue()) {
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
                        AsyncRenderer.this.mPageCount = pageCount.intValue();
                        if (this.mCallback != null) {
                            this.mCallback.onSuccess();
                        }
                        break;
                }
                AsyncRenderer.this.mOpenTask = null;
            }

            @Override
            protected void onCancelled(Integer integer) {
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
            final int mPageIndex;
            final RenderSpec mRenderSpec;
            RenderedPage mRenderedPage;

            public RenderPageTask(int pageIndex, RenderSpec renderSpec, OnPageContentAvailableCallback callback) {
                this.mPageIndex = pageIndex;
                this.mRenderSpec = renderSpec;
                this.mCallback = callback;
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
                int bitmapSizeInBytes = this.mRenderSpec.bitmapWidth * this.mRenderSpec.bitmapHeight * 4;
                while (true) {
                    if (this.mRenderedPage != null || AsyncRenderer.this.mPageContentCache.getSizeInBytes() <= 0 || AsyncRenderer.this.mPageContentCache.getSizeInBytes() + bitmapSizeInBytes <= AsyncRenderer.this.mPageContentCache.getMaxSizeInBytes()) {
                        break;
                    }
                    RenderedPage renderedPage = AsyncRenderer.this.mPageContentCache.removeLeastNeeded();
                    if (this.mRenderSpec.hasSameSize(renderedPage)) {
                        this.mRenderedPage = renderedPage;
                        renderedPage.erase();
                        break;
                    }
                }
                if (this.mRenderedPage == null) {
                    Bitmap bitmap = Bitmap.createBitmap(this.mRenderSpec.bitmapWidth, this.mRenderSpec.bitmapHeight, Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(-1);
                    BitmapDrawable content = new BitmapDrawable(AsyncRenderer.this.mContext.getResources(), bitmap);
                    this.mRenderedPage = new RenderedPage(content);
                }
                this.mRenderedPage.renderSpec = this.mRenderSpec;
                this.mRenderedPage.state = 1;
                AsyncRenderer.this.mPageContentCache.putRenderedPage(this.mPageIndex, this.mRenderedPage);
            }

            @Override
            protected RenderedPage doInBackground(Void... params) {
                Exception e;
                if (isCancelled()) {
                    return this.mRenderedPage;
                }
                Bitmap bitmap = this.mRenderedPage.content.getBitmap();
                ParcelFileDescriptor[] pipe = null;
                try {
                    try {
                        pipe = ParcelFileDescriptor.createPipe();
                        ParcelFileDescriptor source = pipe[0];
                        ParcelFileDescriptor destination = pipe[1];
                        AsyncRenderer.this.mRenderer.renderPage(this.mPageIndex, bitmap.getWidth(), bitmap.getHeight(), this.mRenderSpec.printAttributes, destination);
                        destination.close();
                        BitmapSerializeUtils.readBitmapPixels(bitmap, source);
                    } finally {
                        IoUtils.closeQuietly(pipe[0]);
                        IoUtils.closeQuietly(pipe[1]);
                    }
                } catch (RemoteException e2) {
                    e = e2;
                    Log.e("PageContentRepository", "Error rendering page:" + this.mPageIndex, e);
                    IoUtils.closeQuietly(pipe[0]);
                    IoUtils.closeQuietly(pipe[1]);
                } catch (IOException e3) {
                    e = e3;
                    Log.e("PageContentRepository", "Error rendering page:" + this.mPageIndex, e);
                    IoUtils.closeQuietly(pipe[0]);
                    IoUtils.closeQuietly(pipe[1]);
                }
                return this.mRenderedPage;
            }

            @Override
            public void onPostExecute(RenderedPage renderedPage) {
                AsyncRenderer.this.mPageToRenderTaskMap.remove(Integer.valueOf(this.mPageIndex));
                renderedPage.state = 0;
                if (this.mCallback != null) {
                    this.mCallback.onPageContentAvailable(renderedPage.content);
                }
            }

            @Override
            protected void onCancelled(RenderedPage renderedPage) {
                AsyncRenderer.this.mPageToRenderTaskMap.remove(Integer.valueOf(this.mPageIndex));
                if (renderedPage != null) {
                    renderedPage.state = 2;
                }
            }

            public boolean isPreload() {
                return this.mCallback == null;
            }
        }
    }
}

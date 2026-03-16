package com.android.contacts.common;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.util.BitmapUtil;
import com.android.contacts.common.util.UriUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class ContactPhotoManagerImpl extends ContactPhotoManager implements Handler.Callback {
    private static int mThumbnailSize;
    private final LruCache<Object, Bitmap> mBitmapCache;
    private final LruCache<Object, BitmapHolder> mBitmapHolderCache;
    private final int mBitmapHolderCacheRedZoneBytes;
    private final Context mContext;
    private LoaderThread mLoaderThread;
    private boolean mLoadingRequested;
    private boolean mPaused;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String[] COLUMNS = {"_id", "data15"};
    private volatile boolean mBitmapHolderCacheAllUnfresh = true;
    private final ConcurrentHashMap<ImageView, Request> mPendingRequests = new ConcurrentHashMap<>();
    private final Handler mMainThreadHandler = new Handler(this);
    private final AtomicInteger mStaleCacheOverwrite = new AtomicInteger();
    private final AtomicInteger mFreshCacheOverwrite = new AtomicInteger();

    private static class BitmapHolder {
        Bitmap bitmap;
        Reference<Bitmap> bitmapRef;
        final byte[] bytes;
        int decodedSampleSize;
        volatile boolean fresh = true;
        final int originalSmallerExtent;

        public BitmapHolder(byte[] bytes, int originalSmallerExtent) {
            this.bytes = bytes;
            this.originalSmallerExtent = originalSmallerExtent;
        }
    }

    public ContactPhotoManagerImpl(Context context) {
        this.mContext = context;
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        float cacheSizeAdjustment = am.isLowRamDevice() ? 0.5f : 1.0f;
        int bitmapCacheSize = (int) (1769472.0f * cacheSizeAdjustment);
        this.mBitmapCache = new LruCache<Object, Bitmap>(bitmapCacheSize) {
            @Override
            protected int sizeOf(Object key, Bitmap value) {
                return value.getByteCount();
            }

            @Override
            protected void entryRemoved(boolean evicted, Object key, Bitmap oldValue, Bitmap newValue) {
            }
        };
        int holderCacheSize = (int) (2000000.0f * cacheSizeAdjustment);
        this.mBitmapHolderCache = new LruCache<Object, BitmapHolder>(holderCacheSize) {
            @Override
            protected int sizeOf(Object key, BitmapHolder value) {
                if (value.bytes != null) {
                    return value.bytes.length;
                }
                return 0;
            }

            @Override
            protected void entryRemoved(boolean evicted, Object key, BitmapHolder oldValue, BitmapHolder newValue) {
            }
        };
        this.mBitmapHolderCacheRedZoneBytes = (int) (((double) holderCacheSize) * 0.75d);
        Log.i("ContactPhotoManager", "Cache adj: " + cacheSizeAdjustment);
        mThumbnailSize = context.getResources().getDimensionPixelSize(com.android.contacts.R.dimen.contact_browser_list_item_photo_size);
    }

    @Override
    public void onTrimMemory(int level) {
        if (level >= 60) {
            clear();
        }
    }

    @Override
    public void preloadPhotosInBackground() {
        ensureLoaderThread();
        this.mLoaderThread.requestPreloading();
    }

    @Override
    public void loadThumbnail(ImageView view, long photoId, boolean darkTheme, boolean isCircular, ContactPhotoManager.DefaultImageRequest defaultImageRequest, ContactPhotoManager.DefaultImageProvider defaultProvider) {
        if (photoId == 0) {
            defaultProvider.applyDefaultImage(view, -1, darkTheme, defaultImageRequest);
            this.mPendingRequests.remove(view);
        } else {
            loadPhotoByIdOrUri(view, Request.createFromThumbnailId(photoId, darkTheme, isCircular, defaultProvider));
        }
    }

    @Override
    public void loadPhoto(ImageView view, Uri photoUri, int requestedExtent, boolean darkTheme, boolean isCircular, ContactPhotoManager.DefaultImageRequest defaultImageRequest, ContactPhotoManager.DefaultImageProvider defaultProvider) {
        if (photoUri == null) {
            defaultProvider.applyDefaultImage(view, requestedExtent, darkTheme, defaultImageRequest);
            this.mPendingRequests.remove(view);
        } else if (isDefaultImageUri(photoUri)) {
            createAndApplyDefaultImageForUri(view, photoUri, requestedExtent, darkTheme, isCircular, defaultProvider);
        } else {
            loadPhotoByIdOrUri(view, Request.createFromUri(photoUri, requestedExtent, darkTheme, isCircular, defaultProvider));
        }
    }

    private void createAndApplyDefaultImageForUri(ImageView view, Uri uri, int requestedExtent, boolean darkTheme, boolean isCircular, ContactPhotoManager.DefaultImageProvider defaultProvider) {
        ContactPhotoManager.DefaultImageRequest request = getDefaultImageRequestFromUri(uri);
        request.isCircular = isCircular;
        defaultProvider.applyDefaultImage(view, requestedExtent, darkTheme, request);
    }

    private void loadPhotoByIdOrUri(ImageView view, Request request) {
        boolean loaded = loadCachedPhoto(view, request, false);
        if (loaded) {
            this.mPendingRequests.remove(view);
            return;
        }
        this.mPendingRequests.put(view, request);
        if (!this.mPaused) {
            requestLoading();
        }
    }

    @Override
    public void cancelPendingRequests(View fragmentRootView) {
        if (fragmentRootView == null) {
            this.mPendingRequests.clear();
            return;
        }
        ImageView[] requestSetCopy = (ImageView[]) this.mPendingRequests.keySet().toArray(new ImageView[this.mPendingRequests.size()]);
        for (ImageView imageView : requestSetCopy) {
            if (imageView.getParent() == null || isChildView(fragmentRootView, imageView)) {
                this.mPendingRequests.remove(imageView);
            }
        }
    }

    private static boolean isChildView(View parent, View potentialChild) {
        return potentialChild.getParent() != null && (potentialChild.getParent() == parent || ((potentialChild.getParent() instanceof ViewGroup) && isChildView(parent, (ViewGroup) potentialChild.getParent())));
    }

    @Override
    public void refreshCache() {
        if (!this.mBitmapHolderCacheAllUnfresh) {
            this.mBitmapHolderCacheAllUnfresh = true;
            for (BitmapHolder holder : this.mBitmapHolderCache.snapshot().values()) {
                holder.fresh = false;
            }
        }
    }

    private boolean loadCachedPhoto(ImageView view, Request request, boolean fadeIn) {
        BitmapHolder holder = this.mBitmapHolderCache.get(request.getKey());
        if (holder != null) {
            if (holder.bytes != null) {
                Bitmap cachedBitmap = holder.bitmapRef == null ? null : holder.bitmapRef.get();
                if (cachedBitmap == null) {
                    if (holder.bytes.length < 8192) {
                        inflateBitmap(holder, request.getRequestedExtent());
                        cachedBitmap = holder.bitmap;
                        if (cachedBitmap == null) {
                            return false;
                        }
                    } else {
                        request.applyDefaultImage(view, request.mIsCircular);
                        return false;
                    }
                }
                Drawable previousDrawable = view.getDrawable();
                if (fadeIn && previousDrawable != null) {
                    Drawable[] layers = new Drawable[2];
                    if (previousDrawable instanceof TransitionDrawable) {
                        TransitionDrawable previousTransitionDrawable = (TransitionDrawable) previousDrawable;
                        layers[0] = previousTransitionDrawable.getDrawable(previousTransitionDrawable.getNumberOfLayers() - 1);
                    } else {
                        layers[0] = previousDrawable;
                    }
                    layers[1] = getDrawableForBitmap(this.mContext.getResources(), cachedBitmap, request);
                    TransitionDrawable drawable = new TransitionDrawable(layers);
                    view.setImageDrawable(drawable);
                    drawable.startTransition(200);
                } else {
                    view.setImageDrawable(getDrawableForBitmap(this.mContext.getResources(), cachedBitmap, request));
                }
                if (cachedBitmap.getByteCount() < this.mBitmapCache.maxSize() / 6) {
                    this.mBitmapCache.put(request.getKey(), cachedBitmap);
                }
                holder.bitmap = null;
                return holder.fresh;
            }
            request.applyDefaultImage(view, request.mIsCircular);
            return holder.fresh;
        }
        request.applyDefaultImage(view, request.mIsCircular);
        return false;
    }

    private Drawable getDrawableForBitmap(Resources resources, Bitmap bitmap, Request request) {
        if (!request.mIsCircular) {
            return new BitmapDrawable(resources, bitmap);
        }
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(resources, bitmap);
        drawable.setAntiAlias(true);
        drawable.setCornerRadius(bitmap.getHeight() / 2);
        return drawable;
    }

    private static void inflateBitmap(BitmapHolder holder, int requestedExtent) {
        int sampleSize = BitmapUtil.findOptimalSampleSize(holder.originalSmallerExtent, requestedExtent);
        byte[] bytes = holder.bytes;
        if (bytes != null && bytes.length != 0) {
            if (sampleSize == holder.decodedSampleSize && holder.bitmapRef != null) {
                holder.bitmap = holder.bitmapRef.get();
                if (holder.bitmap != null) {
                    return;
                }
            }
            try {
                Bitmap bitmap = BitmapUtil.decodeBitmapFromBytes(bytes, sampleSize);
                int height = bitmap.getHeight();
                int width = bitmap.getWidth();
                if (height != width && Math.min(height, width) <= mThumbnailSize * 2) {
                    int dimension = Math.min(height, width);
                    bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
                }
                holder.decodedSampleSize = sampleSize;
                holder.bitmap = bitmap;
                holder.bitmapRef = new SoftReference(bitmap);
            } catch (OutOfMemoryError e) {
            }
        }
    }

    public void clear() {
        this.mPendingRequests.clear();
        this.mBitmapHolderCache.evictAll();
        this.mBitmapCache.evictAll();
    }

    @Override
    public void pause() {
        this.mPaused = true;
    }

    @Override
    public void resume() {
        this.mPaused = false;
        if (!this.mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }

    private void requestLoading() {
        if (!this.mLoadingRequested) {
            this.mLoadingRequested = true;
            this.mMainThreadHandler.sendEmptyMessage(1);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                this.mLoadingRequested = false;
                if (this.mPaused) {
                    return true;
                }
                ensureLoaderThread();
                this.mLoaderThread.requestLoading();
                return true;
            case 2:
                if (this.mPaused) {
                    return true;
                }
                processLoadedImages();
                return true;
            default:
                return false;
        }
    }

    public void ensureLoaderThread() {
        if (this.mLoaderThread == null) {
            this.mLoaderThread = new LoaderThread(this.mContext.getContentResolver());
            this.mLoaderThread.start();
        }
    }

    private void processLoadedImages() {
        Iterator<ImageView> iterator = this.mPendingRequests.keySet().iterator();
        while (iterator.hasNext()) {
            ImageView view = iterator.next();
            Request key = this.mPendingRequests.get(view);
            boolean loaded = loadCachedPhoto(view, key, false);
            if (loaded) {
                iterator.remove();
            }
        }
        softenCache();
        if (!this.mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }

    private void softenCache() {
        for (BitmapHolder holder : this.mBitmapHolderCache.snapshot().values()) {
            holder.bitmap = null;
        }
    }

    private void cacheBitmap(Object key, byte[] bytes, boolean preloading, int requestedExtent) {
        BitmapHolder holder = new BitmapHolder(bytes, bytes == null ? -1 : BitmapUtil.getSmallerExtentFromBytes(bytes));
        if (!preloading) {
            inflateBitmap(holder, requestedExtent);
        }
        this.mBitmapHolderCache.put(key, holder);
        this.mBitmapHolderCacheAllUnfresh = false;
    }

    private void obtainPhotoIdsAndUrisToLoad(Set<Long> photoIds, Set<String> photoIdsAsStrings, Set<Request> uris) {
        photoIds.clear();
        photoIdsAsStrings.clear();
        uris.clear();
        boolean jpegsDecoded = false;
        for (Request request : this.mPendingRequests.values()) {
            BitmapHolder holder = this.mBitmapHolderCache.get(request.getKey());
            if (holder != null && holder.bytes != null && holder.fresh && (holder.bitmapRef == null || holder.bitmapRef.get() == null)) {
                inflateBitmap(holder, request.getRequestedExtent());
                jpegsDecoded = true;
            } else if (holder == null || !holder.fresh) {
                if (request.isUriRequest()) {
                    uris.add(request);
                } else {
                    photoIds.add(Long.valueOf(request.getId()));
                    photoIdsAsStrings.add(String.valueOf(request.mId));
                }
            }
        }
        if (jpegsDecoded) {
            this.mMainThreadHandler.sendEmptyMessage(2);
        }
    }

    private class LoaderThread extends HandlerThread implements Handler.Callback {
        private byte[] mBuffer;
        private Handler mLoaderThreadHandler;
        private final Set<Long> mPhotoIds;
        private final Set<String> mPhotoIdsAsStrings;
        private final Set<Request> mPhotoUris;
        private final List<Long> mPreloadPhotoIds;
        private int mPreloadStatus;
        private final ContentResolver mResolver;
        private final StringBuilder mStringBuilder;

        public LoaderThread(ContentResolver resolver) {
            super("ContactPhotoLoader");
            this.mStringBuilder = new StringBuilder();
            this.mPhotoIds = Sets.newHashSet();
            this.mPhotoIdsAsStrings = Sets.newHashSet();
            this.mPhotoUris = Sets.newHashSet();
            this.mPreloadPhotoIds = Lists.newArrayList();
            this.mPreloadStatus = 0;
            this.mResolver = resolver;
        }

        public void ensureHandler() {
            if (this.mLoaderThreadHandler == null) {
                this.mLoaderThreadHandler = new Handler(getLooper(), this);
            }
        }

        public void requestPreloading() {
            if (this.mPreloadStatus != 2) {
                ensureHandler();
                if (!this.mLoaderThreadHandler.hasMessages(1)) {
                    this.mLoaderThreadHandler.sendEmptyMessageDelayed(0, 1000L);
                }
            }
        }

        public void requestLoading() {
            ensureHandler();
            this.mLoaderThreadHandler.removeMessages(0);
            this.mLoaderThreadHandler.sendEmptyMessage(1);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    preloadPhotosInBackground();
                    break;
                case 1:
                    loadPhotosInBackground();
                    break;
            }
            return true;
        }

        private void preloadPhotosInBackground() {
            if (this.mPreloadStatus != 2) {
                if (this.mPreloadStatus != 0) {
                    if (ContactPhotoManagerImpl.this.mBitmapHolderCache.size() > ContactPhotoManagerImpl.this.mBitmapHolderCacheRedZoneBytes) {
                        this.mPreloadStatus = 2;
                        return;
                    }
                    this.mPhotoIds.clear();
                    this.mPhotoIdsAsStrings.clear();
                    int count = 0;
                    int preloadSize = this.mPreloadPhotoIds.size();
                    while (preloadSize > 0 && this.mPhotoIds.size() < 25) {
                        preloadSize--;
                        count++;
                        Long photoId = this.mPreloadPhotoIds.get(preloadSize);
                        this.mPhotoIds.add(photoId);
                        this.mPhotoIdsAsStrings.add(photoId.toString());
                        this.mPreloadPhotoIds.remove(preloadSize);
                    }
                    loadThumbnails(true);
                    if (preloadSize == 0) {
                        this.mPreloadStatus = 2;
                    }
                    Log.v("ContactPhotoManager", "Preloaded " + count + " photos.  Cached bytes: " + ContactPhotoManagerImpl.this.mBitmapHolderCache.size());
                    requestPreloading();
                    return;
                }
                queryPhotosForPreload();
                if (this.mPreloadPhotoIds.isEmpty()) {
                    this.mPreloadStatus = 2;
                } else {
                    this.mPreloadStatus = 1;
                }
                requestPreloading();
            }
        }

        private void queryPhotosForPreload() {
            Cursor cursor = null;
            try {
                Uri uri = ContactsContract.Contacts.CONTENT_URI.buildUpon().appendQueryParameter("directory", String.valueOf(0L)).appendQueryParameter("limit", String.valueOf(100)).build();
                cursor = this.mResolver.query(uri, new String[]{"photo_id"}, "photo_id NOT NULL AND photo_id!=0", null, "starred DESC, last_time_contacted DESC");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        this.mPreloadPhotoIds.add(0, Long.valueOf(cursor.getLong(0)));
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        private void loadPhotosInBackground() {
            ContactPhotoManagerImpl.this.obtainPhotoIdsAndUrisToLoad(this.mPhotoIds, this.mPhotoIdsAsStrings, this.mPhotoUris);
            loadThumbnails(false);
            loadUriBasedPhotos();
            requestPreloading();
        }

        private void loadThumbnails(boolean preloading) {
            if (!this.mPhotoIds.isEmpty()) {
                if (!preloading && this.mPreloadStatus == 1) {
                    Iterator<Long> it = this.mPhotoIds.iterator();
                    while (it.hasNext()) {
                        this.mPreloadPhotoIds.remove(it.next());
                    }
                    if (this.mPreloadPhotoIds.isEmpty()) {
                        this.mPreloadStatus = 2;
                    }
                }
                this.mStringBuilder.setLength(0);
                this.mStringBuilder.append("_id IN(");
                for (int i = 0; i < this.mPhotoIds.size(); i++) {
                    if (i != 0) {
                        this.mStringBuilder.append(',');
                    }
                    this.mStringBuilder.append('?');
                }
                this.mStringBuilder.append(')');
                Cursor profileCursor = null;
                try {
                    profileCursor = this.mResolver.query(ContactsContract.Data.CONTENT_URI, ContactPhotoManagerImpl.COLUMNS, this.mStringBuilder.toString(), (String[]) this.mPhotoIdsAsStrings.toArray(ContactPhotoManagerImpl.EMPTY_STRING_ARRAY), null);
                    if (profileCursor != null) {
                        while (profileCursor.moveToNext()) {
                            Long id = Long.valueOf(profileCursor.getLong(0));
                            byte[] bytes = profileCursor.getBlob(1);
                            ContactPhotoManagerImpl.this.cacheBitmap(id, bytes, preloading, -1);
                            this.mPhotoIds.remove(id);
                        }
                    }
                    if (profileCursor != null) {
                        profileCursor.close();
                    }
                    for (Long id2 : this.mPhotoIds) {
                        if (!ContactsContract.isProfileId(id2.longValue())) {
                            ContactPhotoManagerImpl.this.cacheBitmap(id2, null, preloading, -1);
                        } else {
                            Cursor profileCursor2 = null;
                            try {
                                profileCursor2 = this.mResolver.query(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id2.longValue()), ContactPhotoManagerImpl.COLUMNS, null, null, null);
                                if (profileCursor2 == null || !profileCursor2.moveToFirst()) {
                                    ContactPhotoManagerImpl.this.cacheBitmap(id2, null, preloading, -1);
                                } else {
                                    ContactPhotoManagerImpl.this.cacheBitmap(Long.valueOf(profileCursor2.getLong(0)), profileCursor2.getBlob(1), preloading, -1);
                                }
                                if (profileCursor2 != null) {
                                    profileCursor2.close();
                                }
                            } finally {
                            }
                        }
                    }
                    ContactPhotoManagerImpl.this.mMainThreadHandler.sendEmptyMessage(2);
                } finally {
                }
            }
        }

        private void loadUriBasedPhotos() {
            InputStream is;
            for (Request uriRequest : this.mPhotoUris) {
                Uri originalUri = uriRequest.getUri();
                Uri uri = ContactPhotoManager.removeContactType(originalUri);
                if (this.mBuffer == null) {
                    this.mBuffer = new byte[16384];
                }
                try {
                    String scheme = uri.getScheme();
                    if (scheme.equals("http") || scheme.equals("https")) {
                        is = new URL(uri.toString()).openStream();
                    } else {
                        is = this.mResolver.openInputStream(uri);
                    }
                    if (is != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        while (true) {
                            try {
                                int size = is.read(this.mBuffer);
                                if (size == -1) {
                                    break;
                                } else {
                                    baos.write(this.mBuffer, 0, size);
                                }
                            } catch (Throwable th) {
                                is.close();
                                throw th;
                            }
                        }
                        is.close();
                        ContactPhotoManagerImpl.this.cacheBitmap(originalUri, baos.toByteArray(), false, uriRequest.getRequestedExtent());
                        ContactPhotoManagerImpl.this.mMainThreadHandler.sendEmptyMessage(2);
                    } else {
                        Log.v("ContactPhotoManager", "Cannot load photo " + uri);
                        ContactPhotoManagerImpl.this.cacheBitmap(originalUri, null, false, uriRequest.getRequestedExtent());
                    }
                } catch (Exception e) {
                    ex = e;
                    Log.v("ContactPhotoManager", "Cannot load photo " + uri, ex);
                    ContactPhotoManagerImpl.this.cacheBitmap(originalUri, null, false, uriRequest.getRequestedExtent());
                } catch (OutOfMemoryError e2) {
                    ex = e2;
                    Log.v("ContactPhotoManager", "Cannot load photo " + uri, ex);
                    ContactPhotoManagerImpl.this.cacheBitmap(originalUri, null, false, uriRequest.getRequestedExtent());
                }
            }
        }
    }

    private static final class Request {
        private final boolean mDarkTheme;
        private final ContactPhotoManager.DefaultImageProvider mDefaultProvider;
        private final long mId;
        private final boolean mIsCircular;
        private final int mRequestedExtent;
        private final Uri mUri;

        private Request(long id, Uri uri, int requestedExtent, boolean darkTheme, boolean isCircular, ContactPhotoManager.DefaultImageProvider defaultProvider) {
            this.mId = id;
            this.mUri = uri;
            this.mDarkTheme = darkTheme;
            this.mIsCircular = isCircular;
            this.mRequestedExtent = requestedExtent;
            this.mDefaultProvider = defaultProvider;
        }

        public static Request createFromThumbnailId(long id, boolean darkTheme, boolean isCircular, ContactPhotoManager.DefaultImageProvider defaultProvider) {
            return new Request(id, null, -1, darkTheme, isCircular, defaultProvider);
        }

        public static Request createFromUri(Uri uri, int requestedExtent, boolean darkTheme, boolean isCircular, ContactPhotoManager.DefaultImageProvider defaultProvider) {
            return new Request(0L, uri, requestedExtent, darkTheme, isCircular, defaultProvider);
        }

        public boolean isUriRequest() {
            return this.mUri != null;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public long getId() {
            return this.mId;
        }

        public int getRequestedExtent() {
            return this.mRequestedExtent;
        }

        public int hashCode() {
            int result = ((int) (this.mId ^ (this.mId >>> 32))) + 31;
            return (((result * 31) + this.mRequestedExtent) * 31) + (this.mUri == null ? 0 : this.mUri.hashCode());
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                Request that = (Request) obj;
                return this.mId == that.mId && this.mRequestedExtent == that.mRequestedExtent && UriUtils.areEqual(this.mUri, that.mUri);
            }
            return false;
        }

        public Object getKey() {
            return this.mUri == null ? Long.valueOf(this.mId) : this.mUri;
        }

        public void applyDefaultImage(ImageView view, boolean isCircular) {
            ContactPhotoManager.DefaultImageRequest request;
            if (isCircular) {
                request = ContactPhotoManager.isBusinessContactUri(this.mUri) ? ContactPhotoManager.DefaultImageRequest.EMPTY_CIRCULAR_BUSINESS_IMAGE_REQUEST : ContactPhotoManager.DefaultImageRequest.EMPTY_CIRCULAR_DEFAULT_IMAGE_REQUEST;
            } else {
                request = ContactPhotoManager.isBusinessContactUri(this.mUri) ? ContactPhotoManager.DefaultImageRequest.EMPTY_DEFAULT_BUSINESS_IMAGE_REQUEST : ContactPhotoManager.DefaultImageRequest.EMPTY_DEFAULT_IMAGE_REQUEST;
            }
            this.mDefaultProvider.applyDefaultImage(view, this.mRequestedExtent, this.mDarkTheme, request);
        }
    }
}

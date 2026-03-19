package com.android.contacts;

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
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.util.BitmapUtil;
import com.android.contacts.util.PermissionsUtil;
import com.android.contacts.util.UriUtils;
import com.android.contactsbind.util.UserAgentGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mediatek.contacts.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private String mUserAgent;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String[] COLUMNS = {"_id", "data15"};
    private static final BitmapHolder BITMAP_UNAVAILABLE = new BitmapHolder(new byte[0], 0);
    private volatile boolean mBitmapHolderCacheAllUnfresh = true;
    private final ConcurrentHashMap<ImageView, Request> mPendingRequests = new ConcurrentHashMap<>();
    private final Handler mMainThreadHandler = new Handler(this);
    private final AtomicInteger mStaleCacheOverwrite = new AtomicInteger();
    private final AtomicInteger mFreshCacheOverwrite = new AtomicInteger();

    static {
        BITMAP_UNAVAILABLE.bitmapRef = new SoftReference(null);
    }

    private static class BitmapHolder {
        Bitmap bitmap;
        Reference<Bitmap> bitmapRef;
        final byte[] bytes;
        int decodedSampleSize;
        volatile boolean fresh = true;
        final int originalSmallerExtent;

        public BitmapHolder(byte[] bArr, int i) {
            this.bytes = bArr;
            this.originalSmallerExtent = i;
        }
    }

    public ContactPhotoManagerImpl(Context context) {
        this.mContext = context;
        float f = ((ActivityManager) context.getSystemService("activity")).isLowRamDevice() ? 0.5f : 1.0f;
        this.mBitmapCache = new LruCache<Object, Bitmap>((int) (1769472.0f * f)) {
            @Override
            protected int sizeOf(Object obj, Bitmap bitmap) {
                return bitmap.getByteCount();
            }

            @Override
            protected void entryRemoved(boolean z, Object obj, Bitmap bitmap, Bitmap bitmap2) {
            }
        };
        int i = (int) (2000000.0f * f);
        this.mBitmapHolderCache = new LruCache<Object, BitmapHolder>(i) {
            @Override
            protected int sizeOf(Object obj, BitmapHolder bitmapHolder) {
                if (bitmapHolder.bytes != null) {
                    return bitmapHolder.bytes.length;
                }
                return 0;
            }

            @Override
            protected void entryRemoved(boolean z, Object obj, BitmapHolder bitmapHolder, BitmapHolder bitmapHolder2) {
            }
        };
        this.mBitmapHolderCacheRedZoneBytes = (int) (((double) i) * 0.75d);
        Log.i("ContactPhotoManager", "Cache adj: " + f);
        mThumbnailSize = context.getResources().getDimensionPixelSize(R.dimen.contact_browser_list_item_photo_size);
        this.mUserAgent = UserAgentGenerator.getUserAgent(context);
        if (this.mUserAgent == null) {
            this.mUserAgent = "";
        }
    }

    @Override
    public void onTrimMemory(int i) {
        if (i >= 60) {
            clear();
        }
    }

    @Override
    public void preloadPhotosInBackground() {
        ensureLoaderThread();
        this.mLoaderThread.requestPreloading();
    }

    @Override
    public void loadThumbnail(ImageView imageView, long j, boolean z, boolean z2, ContactPhotoManager.DefaultImageRequest defaultImageRequest, ContactPhotoManager.DefaultImageProvider defaultImageProvider) {
        if (j == 0) {
            defaultImageProvider.applyDefaultImage(imageView, -1, z, defaultImageRequest);
            this.mPendingRequests.remove(imageView);
        } else {
            loadPhotoByIdOrUri(imageView, Request.createFromThumbnailId(j, z, z2, defaultImageProvider, defaultImageRequest));
        }
    }

    @Override
    public void loadPhoto(ImageView imageView, Uri uri, int i, boolean z, boolean z2, ContactPhotoManager.DefaultImageRequest defaultImageRequest, ContactPhotoManager.DefaultImageProvider defaultImageProvider) {
        if (uri == null) {
            defaultImageProvider.applyDefaultImage(imageView, i, z, defaultImageRequest);
            this.mPendingRequests.remove(imageView);
        } else if (isDefaultImageUri(uri)) {
            createAndApplyDefaultImageForUri(imageView, uri, i, z, z2, defaultImageProvider);
        } else {
            loadPhotoByIdOrUri(imageView, Request.createFromUri(uri, i, z, z2, defaultImageProvider, defaultImageRequest));
        }
    }

    private void createAndApplyDefaultImageForUri(ImageView imageView, Uri uri, int i, boolean z, boolean z2, ContactPhotoManager.DefaultImageProvider defaultImageProvider) {
        ContactPhotoManager.DefaultImageRequest defaultImageRequestFromUri = getDefaultImageRequestFromUri(uri);
        defaultImageRequestFromUri.isCircular = z2;
        defaultImageProvider.applyDefaultImage(imageView, i, z, defaultImageRequestFromUri);
    }

    private void loadPhotoByIdOrUri(ImageView imageView, Request request) {
        if (loadCachedPhoto(imageView, request, false)) {
            this.mPendingRequests.remove(imageView);
            return;
        }
        this.mPendingRequests.put(imageView, request);
        if (!this.mPaused) {
            requestLoading();
        }
    }

    @Override
    public void removePhoto(ImageView imageView) {
        imageView.setImageDrawable(null);
        this.mPendingRequests.remove(imageView);
    }

    @Override
    public void cancelPendingRequests(View view) {
        if (view == null) {
            this.mPendingRequests.clear();
            return;
        }
        Iterator<Map.Entry<ImageView, Request>> it = this.mPendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            ImageView key = it.next().getKey();
            if (key.getParent() == null || isChildView(view, key)) {
                it.remove();
            }
        }
    }

    private static boolean isChildView(View view, View view2) {
        return view2.getParent() != null && (view2.getParent() == view || ((view2.getParent() instanceof ViewGroup) && isChildView(view, (ViewGroup) view2.getParent())));
    }

    @Override
    public void refreshCache() {
        if (this.mBitmapHolderCacheAllUnfresh) {
            return;
        }
        this.mBitmapHolderCacheAllUnfresh = true;
        for (BitmapHolder bitmapHolder : this.mBitmapHolderCache.snapshot().values()) {
            if (bitmapHolder != BITMAP_UNAVAILABLE) {
                bitmapHolder.fresh = false;
            }
        }
    }

    @Override
    public void refreshCacheByKey(Object obj) {
        BitmapHolder bitmapHolder;
        Log.d("ContactPhotoManager", "[refreshCacheByKey] key = " + obj);
        if (!this.mBitmapHolderCacheAllUnfresh && (bitmapHolder = this.mBitmapHolderCache.get(obj)) != null && bitmapHolder != BITMAP_UNAVAILABLE) {
            Log.d("ContactPhotoManager", "[refreshCacheByKey] refresh done");
            bitmapHolder.fresh = false;
        }
    }

    private boolean loadCachedPhoto(ImageView imageView, Request request, boolean z) {
        Bitmap bitmap;
        BitmapHolder bitmapHolder = this.mBitmapHolderCache.get(request.getKey());
        if (bitmapHolder != null) {
            if (bitmapHolder.bytes != null && bitmapHolder.bytes.length != 0) {
                if (bitmapHolder.bitmapRef != null) {
                    bitmap = bitmapHolder.bitmapRef.get();
                } else {
                    bitmap = null;
                }
                if (bitmap != null) {
                    Drawable drawable = imageView.getDrawable();
                    if (z && drawable != null) {
                        Drawable[] drawableArr = new Drawable[2];
                        if (drawable instanceof TransitionDrawable) {
                            TransitionDrawable transitionDrawable = (TransitionDrawable) drawable;
                            drawableArr[0] = transitionDrawable.getDrawable(transitionDrawable.getNumberOfLayers() - 1);
                        } else {
                            drawableArr[0] = drawable;
                        }
                        drawableArr[1] = getDrawableForBitmap(this.mContext.getResources(), bitmap, request);
                        TransitionDrawable transitionDrawable2 = new TransitionDrawable(drawableArr);
                        imageView.setImageDrawable(transitionDrawable2);
                        transitionDrawable2.startTransition(200);
                    } else {
                        imageView.setImageDrawable(getDrawableForBitmap(this.mContext.getResources(), bitmap, request));
                    }
                    if (bitmap.getByteCount() < this.mBitmapCache.maxSize() / 6) {
                        this.mBitmapCache.put(request.getKey(), bitmap);
                    }
                    bitmapHolder.bitmap = null;
                    return bitmapHolder.fresh;
                }
                request.applyDefaultImage(imageView, request.mIsCircular);
                Log.e("ContactPhotoManager", "[loadCachedPhoto]cachedBitmap == null");
                return false;
            }
            request.applyDefaultImage(imageView, request.mIsCircular);
            return bitmapHolder.fresh;
        }
        request.applyDefaultImage(imageView, request.mIsCircular);
        return false;
    }

    private Drawable getDrawableForBitmap(Resources resources, Bitmap bitmap, Request request) {
        if (request.mIsCircular) {
            RoundedBitmapDrawable roundedBitmapDrawableCreate = RoundedBitmapDrawableFactory.create(resources, bitmap);
            roundedBitmapDrawableCreate.setAntiAlias(true);
            roundedBitmapDrawableCreate.setCircular(true);
            return roundedBitmapDrawableCreate;
        }
        return new BitmapDrawable(resources, bitmap);
    }

    private static void inflateBitmap(BitmapHolder bitmapHolder, int i) {
        int iFindOptimalSampleSize = BitmapUtil.findOptimalSampleSize(bitmapHolder.originalSmallerExtent, i);
        byte[] bArr = bitmapHolder.bytes;
        if (bArr == null || bArr.length == 0) {
            return;
        }
        if (iFindOptimalSampleSize == bitmapHolder.decodedSampleSize && bitmapHolder.bitmapRef != null) {
            bitmapHolder.bitmap = bitmapHolder.bitmapRef.get();
            if (bitmapHolder.bitmap != null) {
                return;
            }
        }
        try {
            Bitmap bitmapDecodeBitmapFromBytes = BitmapUtil.decodeBitmapFromBytes(bArr, iFindOptimalSampleSize);
            int height = bitmapDecodeBitmapFromBytes.getHeight();
            int width = bitmapDecodeBitmapFromBytes.getWidth();
            if (height != width && Math.min(height, width) <= mThumbnailSize * 2) {
                int iMin = Math.min(height, width);
                bitmapDecodeBitmapFromBytes = ThumbnailUtils.extractThumbnail(bitmapDecodeBitmapFromBytes, iMin, iMin);
            }
            bitmapHolder.decodedSampleSize = iFindOptimalSampleSize;
            bitmapHolder.bitmap = bitmapDecodeBitmapFromBytes;
            bitmapHolder.bitmapRef = new SoftReference(bitmapDecodeBitmapFromBytes);
        } catch (OutOfMemoryError e) {
            Log.e("ContactPhotoManager", "[inflateBitmap]cached OutOfMemoryError", e);
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
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case 1:
                Log.d("ContactPhotoManager", "Main handleMessage MESSAGE_REQUEST_LOADING, paused: " + this.mPaused);
                this.mLoadingRequested = false;
                if (!this.mPaused) {
                    ensureLoaderThread();
                    this.mLoaderThread.requestLoading();
                }
                return true;
            case 2:
                Log.d("ContactPhotoManager", "Main handleMessage MESSAGE_PHOTOS_LOADED, paused: " + this.mPaused);
                if (!this.mPaused) {
                    processLoadedImages();
                } else {
                    Log.d("ContactPhotoManager", "[handleMessage] The loaded caches ignored, refresh them");
                    refreshCache();
                }
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
        Iterator<Map.Entry<ImageView, Request>> it = this.mPendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ImageView, Request> next = it.next();
            if (loadCachedPhoto(next.getKey(), next.getValue(), false)) {
                it.remove();
            }
        }
        softenCache();
        if (!this.mPendingRequests.isEmpty()) {
            Log.d("ContactPhotoManager", "[processLoadedImages]mPendingRequests is not empty, requestLoading again");
            requestLoading();
        }
    }

    private void softenCache() {
        Iterator<BitmapHolder> it = this.mBitmapHolderCache.snapshot().values().iterator();
        while (it.hasNext()) {
            it.next().bitmap = null;
        }
    }

    private void cacheBitmap(Object obj, byte[] bArr, boolean z, int i) {
        BitmapHolder bitmapHolder = new BitmapHolder(bArr, bArr == null ? -1 : BitmapUtil.getSmallerExtentFromBytes(bArr));
        if (!z) {
            inflateBitmap(bitmapHolder, i);
        }
        if (bArr != null) {
            this.mBitmapHolderCache.put(obj, bitmapHolder);
            if (this.mBitmapHolderCache.get(obj) != bitmapHolder) {
                Log.w("ContactPhotoManager", "Bitmap too big to fit in cache.");
                this.mBitmapHolderCache.put(obj, BITMAP_UNAVAILABLE);
            }
        } else {
            this.mBitmapHolderCache.put(obj, BITMAP_UNAVAILABLE);
        }
        this.mBitmapHolderCacheAllUnfresh = false;
    }

    @Override
    public void cacheBitmap(Uri uri, Bitmap bitmap, byte[] bArr) {
        int iMin = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Request requestCreateFromUri = Request.createFromUri(uri, iMin, false, false, DEFAULT_AVATAR);
        BitmapHolder bitmapHolder = new BitmapHolder(bArr, iMin);
        bitmapHolder.bitmapRef = new SoftReference(bitmap);
        this.mBitmapHolderCache.put(requestCreateFromUri.getKey(), bitmapHolder);
        this.mBitmapHolderCacheAllUnfresh = false;
        this.mBitmapCache.put(requestCreateFromUri.getKey(), bitmap);
    }

    private void obtainPhotoIdsAndUrisToLoad(Set<Long> set, Set<String> set2, Set<Request> set3) {
        set.clear();
        set2.clear();
        set3.clear();
        boolean z = false;
        for (Request request : this.mPendingRequests.values()) {
            BitmapHolder bitmapHolder = this.mBitmapHolderCache.get(request.getKey());
            if (bitmapHolder != BITMAP_UNAVAILABLE) {
                if (bitmapHolder != null && bitmapHolder.bytes != null && bitmapHolder.fresh && (bitmapHolder.bitmapRef == null || bitmapHolder.bitmapRef.get() == null)) {
                    inflateBitmap(bitmapHolder, request.getRequestedExtent());
                    z = true;
                } else if (bitmapHolder == null || !bitmapHolder.fresh) {
                    if (request.isUriRequest()) {
                        set3.add(request);
                    } else {
                        Log.d("ContactPhotoManager", "[obtainPhotoIdsAndUrisToLoad]id: " + request.getId());
                        set.add(Long.valueOf(request.getId()));
                        set2.add(String.valueOf(request.mId));
                    }
                } else {
                    Log.e("ContactPhotoManager", "[obtainPhotoIdsAndUrisToLoad]holder != null && holder.fresh");
                }
            }
        }
        if (z) {
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

        public LoaderThread(ContentResolver contentResolver) {
            super("ContactPhotoLoader");
            this.mStringBuilder = new StringBuilder();
            this.mPhotoIds = Sets.newHashSet();
            this.mPhotoIdsAsStrings = Sets.newHashSet();
            this.mPhotoUris = Sets.newHashSet();
            this.mPreloadPhotoIds = Lists.newArrayList();
            this.mPreloadStatus = 0;
            this.mResolver = contentResolver;
        }

        public void ensureHandler() {
            if (this.mLoaderThreadHandler == null) {
                this.mLoaderThreadHandler = new Handler(getLooper(), this);
            }
        }

        public void requestPreloading() {
            if (this.mPreloadStatus == 2) {
                return;
            }
            ensureHandler();
            if (this.mLoaderThreadHandler.hasMessages(1)) {
                return;
            }
            this.mLoaderThreadHandler.sendEmptyMessageDelayed(0, 1000L);
        }

        public void requestLoading() {
            ensureHandler();
            this.mLoaderThreadHandler.removeMessages(0);
            this.mLoaderThreadHandler.sendEmptyMessage(1);
        }

        @Override
        public boolean handleMessage(Message message) throws Throwable {
            switch (message.what) {
                case 0:
                    preloadPhotosInBackground();
                    break;
                case 1:
                    loadPhotosInBackground();
                    break;
            }
            return true;
        }

        private void preloadPhotosInBackground() throws Throwable {
            if (this.mPreloadStatus == 2) {
                return;
            }
            if (this.mPreloadStatus != 0) {
                if (ContactPhotoManagerImpl.this.mBitmapHolderCache.size() > ContactPhotoManagerImpl.this.mBitmapHolderCacheRedZoneBytes) {
                    this.mPreloadStatus = 2;
                    return;
                }
                this.mPhotoIds.clear();
                this.mPhotoIdsAsStrings.clear();
                int i = 0;
                int size = this.mPreloadPhotoIds.size();
                while (size > 0 && this.mPhotoIds.size() < 25) {
                    size--;
                    i++;
                    Long l = this.mPreloadPhotoIds.get(size);
                    this.mPhotoIds.add(l);
                    this.mPhotoIdsAsStrings.add(l.toString());
                    this.mPreloadPhotoIds.remove(size);
                }
                loadThumbnails(true);
                if (size == 0) {
                    this.mPreloadStatus = 2;
                }
                if (Log.isLoggable("ContactPhotoManager", 2)) {
                    Log.v("ContactPhotoManager", "Preloaded " + i + " photos.  Cached bytes: " + ContactPhotoManagerImpl.this.mBitmapHolderCache.size());
                }
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

        private void queryPhotosForPreload() throws Throwable {
            Cursor cursor = null;
            try {
                Cursor cursorQuery = this.mResolver.query(ContactsContract.Contacts.CONTENT_URI.buildUpon().appendQueryParameter("directory", String.valueOf(0L)).appendQueryParameter("limit", String.valueOf(100)).build(), new String[]{"photo_id"}, "photo_id NOT NULL AND photo_id!=0", null, "starred DESC, last_time_contacted DESC");
                if (cursorQuery != null) {
                    while (cursorQuery.moveToNext()) {
                        try {
                            this.mPreloadPhotoIds.add(0, Long.valueOf(cursorQuery.getLong(0)));
                        } catch (Throwable th) {
                            th = th;
                            cursor = cursorQuery;
                            if (cursor != null) {
                                cursor.close();
                            }
                            throw th;
                        }
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }

        private void loadPhotosInBackground() throws Throwable {
            if (PermissionsUtil.hasPermission(ContactPhotoManagerImpl.this.mContext, PermissionsUtil.CONTACTS)) {
                ContactPhotoManagerImpl.this.obtainPhotoIdsAndUrisToLoad(this.mPhotoIds, this.mPhotoIdsAsStrings, this.mPhotoUris);
                loadThumbnails(false);
                loadUriBasedPhotos();
                requestPreloading();
            }
        }

        private void loadThumbnails(boolean z) throws Throwable {
            Cursor cursorQuery;
            if (this.mPhotoIds.isEmpty()) {
                Log.e("ContactPhotoManager", "[loadThumbnails]mphotoIds is empty, preloading " + z);
                return;
            }
            if (!z && this.mPreloadStatus == 1) {
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
            Cursor cursor = null;
            try {
                Cursor cursorQuery2 = this.mResolver.query(ContactsContract.Data.CONTENT_URI, ContactPhotoManagerImpl.COLUMNS, this.mStringBuilder.toString(), (String[]) this.mPhotoIdsAsStrings.toArray(ContactPhotoManagerImpl.EMPTY_STRING_ARRAY), null);
                if (cursorQuery2 != null) {
                    while (cursorQuery2.moveToNext()) {
                        try {
                            Long lValueOf = Long.valueOf(cursorQuery2.getLong(0));
                            byte[] blob = cursorQuery2.getBlob(1);
                            if (blob == null) {
                                blob = new byte[0];
                            }
                            ContactPhotoManagerImpl.this.cacheBitmap(lValueOf, blob, z, -1);
                            this.mPhotoIds.remove(lValueOf);
                        } catch (Throwable th) {
                            th = th;
                            cursor = cursorQuery2;
                            if (cursor != null) {
                                cursor.close();
                            }
                            throw th;
                        }
                    }
                }
                if (cursorQuery2 != null) {
                    cursorQuery2.close();
                }
                for (Long l : this.mPhotoIds) {
                    if (ContactsContract.isProfileId(l.longValue())) {
                        try {
                            cursorQuery = this.mResolver.query(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, l.longValue()), ContactPhotoManagerImpl.COLUMNS, null, null, null);
                            if (cursorQuery != null) {
                                try {
                                    if (cursorQuery.moveToFirst()) {
                                        byte[] blob2 = cursorQuery.getBlob(1);
                                        if (blob2 == null) {
                                            blob2 = new byte[0];
                                        }
                                        ContactPhotoManagerImpl.this.cacheBitmap(Long.valueOf(cursorQuery.getLong(0)), blob2, z, -1);
                                    } else {
                                        Log.e("ContactPhotoManager", "[loadThumbnails]profileCursor query failed, id = " + l);
                                        ContactPhotoManagerImpl.this.cacheBitmap(l, null, z, -1);
                                    }
                                    if (cursorQuery != null) {
                                        cursorQuery.close();
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    if (cursorQuery != null) {
                                        cursorQuery.close();
                                    }
                                    throw th;
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            cursorQuery = null;
                        }
                    } else {
                        Log.d("ContactPhotoManager", "[loadThumbnails]Not a profile photo and not found");
                        ContactPhotoManagerImpl.this.cacheBitmap(l, null, z, -1);
                    }
                }
                Log.d("ContactPhotoManager", "[loadThumbnails]send message MESSAGE_PHOTOS_LOADED");
                ContactPhotoManagerImpl.this.mMainThreadHandler.sendEmptyMessage(2);
            } catch (Throwable th4) {
                th = th4;
            }
        }

        private void loadUriBasedPhotos() {
            InputStream inputStream;
            for (Request request : this.mPhotoUris) {
                Uri uri = request.getUri();
                Uri uriRemoveContactType = ContactPhotoManager.removeContactType(uri);
                if (this.mBuffer == null) {
                    this.mBuffer = new byte[16384];
                }
                try {
                    String scheme = uriRemoveContactType.getScheme();
                    if (scheme.equals("http") || scheme.equals("https")) {
                        TrafficStats.setThreadStatsTag(1);
                        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(uriRemoveContactType.toString()).openConnection();
                        if (!TextUtils.isEmpty(ContactPhotoManagerImpl.this.mUserAgent)) {
                            httpURLConnection.setRequestProperty("User-Agent", ContactPhotoManagerImpl.this.mUserAgent);
                        }
                        try {
                            inputStream = httpURLConnection.getInputStream();
                        } catch (IOException e) {
                            httpURLConnection.disconnect();
                            inputStream = null;
                        }
                        TrafficStats.clearThreadStatsTag();
                    } else {
                        inputStream = this.mResolver.openInputStream(uriRemoveContactType);
                    }
                    if (inputStream == null) {
                        if (Log.isLoggable("ContactPhotoManager", 2)) {
                            Log.v("ContactPhotoManager", "Cannot load photo " + uriRemoveContactType);
                        }
                        ContactPhotoManagerImpl.this.cacheBitmap(uri, null, false, request.getRequestedExtent());
                    } else {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        while (true) {
                            try {
                                int i = inputStream.read(this.mBuffer);
                                if (i == -1) {
                                    break;
                                } else {
                                    byteArrayOutputStream.write(this.mBuffer, 0, i);
                                }
                            } catch (Throwable th) {
                                inputStream.close();
                                throw th;
                            }
                        }
                        inputStream.close();
                        ContactPhotoManagerImpl.this.cacheBitmap(uri, byteArrayOutputStream.toByteArray(), false, request.getRequestedExtent());
                        ContactPhotoManagerImpl.this.mMainThreadHandler.sendEmptyMessage(2);
                    }
                } catch (Exception | OutOfMemoryError e2) {
                    if (Log.isLoggable("ContactPhotoManager", 2)) {
                        Log.v("ContactPhotoManager", "Cannot load photo " + uriRemoveContactType, e2);
                    }
                    ContactPhotoManagerImpl.this.cacheBitmap(uri, null, false, request.getRequestedExtent());
                }
            }
        }
    }

    private static final class Request {
        private final boolean mDarkTheme;
        private final ContactPhotoManager.DefaultImageProvider mDefaultProvider;
        private final ContactPhotoManager.DefaultImageRequest mDefaultRequest;
        private final long mId;
        private final boolean mIsCircular;
        private final int mRequestedExtent;
        private final Uri mUri;

        private Request(long j, Uri uri, int i, boolean z, boolean z2, ContactPhotoManager.DefaultImageProvider defaultImageProvider, ContactPhotoManager.DefaultImageRequest defaultImageRequest) {
            this.mId = j;
            this.mUri = uri;
            this.mDarkTheme = z;
            this.mIsCircular = z2;
            this.mRequestedExtent = i;
            this.mDefaultProvider = defaultImageProvider;
            this.mDefaultRequest = defaultImageRequest;
        }

        public static Request createFromThumbnailId(long j, boolean z, boolean z2, ContactPhotoManager.DefaultImageProvider defaultImageProvider, ContactPhotoManager.DefaultImageRequest defaultImageRequest) {
            return new Request(j, null, -1, z, z2, defaultImageProvider, defaultImageRequest);
        }

        public static Request createFromUri(Uri uri, int i, boolean z, boolean z2, ContactPhotoManager.DefaultImageProvider defaultImageProvider) {
            return createFromUri(uri, i, z, z2, defaultImageProvider, null);
        }

        public static Request createFromUri(Uri uri, int i, boolean z, boolean z2, ContactPhotoManager.DefaultImageProvider defaultImageProvider, ContactPhotoManager.DefaultImageRequest defaultImageRequest) {
            return new Request(0L, uri, i, z, z2, defaultImageProvider, defaultImageRequest);
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
            return (31 * (((((int) (this.mId ^ (this.mId >>> 32))) + 31) * 31) + this.mRequestedExtent)) + (this.mUri == null ? 0 : this.mUri.hashCode());
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Request request = (Request) obj;
            if (this.mId == request.mId && this.mRequestedExtent == request.mRequestedExtent && UriUtils.areEqual(this.mUri, request.mUri)) {
                return true;
            }
            return false;
        }

        public Object getKey() {
            return this.mUri == null ? Long.valueOf(this.mId) : this.mUri;
        }

        public void applyDefaultImage(ImageView imageView, boolean z) {
            ContactPhotoManager.DefaultImageRequest defaultImageRequest;
            if (this.mDefaultRequest == null) {
                if (z) {
                    if (ContactPhotoManager.isBusinessContactUri(this.mUri)) {
                        defaultImageRequest = ContactPhotoManager.DefaultImageRequest.EMPTY_CIRCULAR_BUSINESS_IMAGE_REQUEST;
                    } else {
                        defaultImageRequest = ContactPhotoManager.DefaultImageRequest.EMPTY_CIRCULAR_DEFAULT_IMAGE_REQUEST;
                    }
                } else if (ContactPhotoManager.isBusinessContactUri(this.mUri)) {
                    defaultImageRequest = ContactPhotoManager.DefaultImageRequest.EMPTY_DEFAULT_BUSINESS_IMAGE_REQUEST;
                } else {
                    defaultImageRequest = ContactPhotoManager.DefaultImageRequest.EMPTY_DEFAULT_IMAGE_REQUEST;
                }
            } else {
                defaultImageRequest = this.mDefaultRequest;
            }
            this.mDefaultProvider.applyDefaultImage(imageView, this.mRequestedExtent, this.mDarkTheme, defaultImageRequest);
        }
    }
}

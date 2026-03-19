package com.android.documentsui.dirlist;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.DocumentsFeatureOption;
import com.android.documentsui.IconUtils;
import com.android.documentsui.ProviderExecutor;
import com.android.documentsui.R;
import com.android.documentsui.ThumbnailCache;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.MimeTypes;
import com.android.documentsui.base.SharedMinimal;
import com.mediatek.omadrm.OmaDrmUtils;
import java.util.Arrays;
import java.util.function.BiConsumer;

public class IconHelper {
    private static final BiConsumer<View, View> ANIM_FADE_IN = new BiConsumer() {
        @Override
        public final void accept(Object obj, Object obj2) {
            IconHelper.lambda$static$0((View) obj, (View) obj2);
        }
    };
    private static final BiConsumer<View, View> ANIM_NO_OP = new BiConsumer() {
        @Override
        public final void accept(Object obj, Object obj2) {
            IconHelper.lambda$static$1((View) obj, (View) obj2);
        }
    };
    private final Context mContext;
    private Point mCurrentSize;
    private int mMode;
    private final ThumbnailCache mThumbnailCache;
    private boolean mThumbnailsEnabled = true;

    static void lambda$static$0(View view, View view2) {
        float alpha = view.getAlpha();
        view.animate().alpha(0.0f).start();
        view2.setAlpha(0.0f);
        view2.animate().alpha(alpha).start();
    }

    static void lambda$static$1(View view, View view2) {
    }

    public IconHelper(Context context, int i) {
        this.mContext = context;
        setViewMode(i);
        this.mThumbnailCache = DocumentsApplication.getThumbnailCache(context);
    }

    public void setThumbnailsEnabled(boolean z) {
        this.mThumbnailsEnabled = z;
    }

    public void setViewMode(int i) {
        this.mMode = i;
        int thumbSize = getThumbSize(i);
        this.mCurrentSize = new Point(thumbSize, thumbSize);
    }

    private int getThumbSize(int i) {
        switch (i) {
            case 1:
                return this.mContext.getResources().getDimensionPixelSize(R.dimen.list_item_thumbnail_size);
            case 2:
                return this.mContext.getResources().getDimensionPixelSize(R.dimen.grid_width);
            default:
                throw new IllegalArgumentException("Unsupported layout mode: " + i);
        }
    }

    public void stopLoading(ImageView imageView) {
        LoaderTask loaderTask = (LoaderTask) imageView.getTag();
        if (loaderTask != null) {
            loaderTask.preempt();
            imageView.setTag(null);
        }
    }

    private static class LoaderTask extends AsyncTask<Uri, Void, Bitmap> implements ProviderExecutor.Preemptable {
        private final ImageView mIconMime;
        private final ImageView mIconThumb;
        private final BiConsumer<View, View> mImageAnimator;
        private final long mLastModified;
        private final CancellationSignal mSignal = new CancellationSignal();
        private final Point mThumbSize;
        private final Uri mUri;

        public LoaderTask(Uri uri, ImageView imageView, ImageView imageView2, Point point, long j, BiConsumer<View, View> biConsumer) {
            this.mUri = uri;
            this.mIconMime = imageView;
            this.mIconThumb = imageView2;
            this.mThumbSize = point;
            this.mImageAnimator = biConsumer;
            this.mLastModified = j;
            if (SharedMinimal.VERBOSE) {
                Log.v("IconHelper", "Starting icon loader task for " + this.mUri);
            }
        }

        @Override
        public void preempt() {
            if (SharedMinimal.VERBOSE) {
                Log.v("IconHelper", "Icon loader task for " + this.mUri + " was cancelled.");
            }
            cancel(false);
            this.mSignal.cancel();
        }

        @Override
        protected Bitmap doInBackground(Uri... uriArr) throws Throwable {
            Bitmap documentThumbnail;
            ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow;
            ContentProviderClient contentProviderClient = null;
            if (isCancelled()) {
                return null;
            }
            Context context = this.mIconThumb.getContext();
            try {
                try {
                    contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(context.getContentResolver(), this.mUri.getAuthority());
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Exception e) {
                e = e;
                documentThumbnail = null;
            }
            try {
                try {
                    documentThumbnail = DocumentsContract.getDocumentThumbnail(contentProviderClientAcquireUnstableProviderOrThrow, this.mUri, this.mThumbSize, this.mSignal);
                    if (documentThumbnail != null) {
                        try {
                            DocumentsApplication.getThumbnailCache(context).putThumbnail(this.mUri, this.mThumbSize, documentThumbnail, this.mLastModified);
                        } catch (Exception e2) {
                            e = e2;
                            contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
                            if (!(e instanceof OperationCanceledException) && SharedMinimal.VERBOSE) {
                                Log.w("IconHelper", "Failed to load thumbnail for " + this.mUri + ": " + e);
                            }
                            ContentProviderClient.releaseQuietly(contentProviderClient);
                        }
                    }
                    ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                } catch (Exception e3) {
                    e = e3;
                    documentThumbnail = null;
                }
                return documentThumbnail;
            } catch (Throwable th2) {
                th = th2;
                contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
                ContentProviderClient.releaseQuietly(contentProviderClient);
                throw th;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (SharedMinimal.VERBOSE) {
                Log.v("IconHelper", "Loader task for " + this.mUri + " completed");
            }
            if (this.mIconThumb.getTag() == this && bitmap != null) {
                this.mIconThumb.setTag(null);
                this.mIconThumb.setImageBitmap(bitmap);
                this.mImageAnimator.accept(this.mIconMime, this.mIconThumb);
            }
        }
    }

    public void load(DocumentInfo documentInfo, ImageView imageView, ImageView imageView2, ImageView imageView3, Cursor cursor, ImageView imageView4) {
        load(documentInfo.derivedUri, documentInfo.mimeType, documentInfo.flags, documentInfo.icon, documentInfo.lastModified, imageView, imageView2, imageView3, cursor, imageView4);
    }

    public void load(Uri uri, String str, int i, int i2, long j, ImageView imageView, ImageView imageView2, ImageView imageView3, Cursor cursor, ImageView imageView4) {
        boolean z;
        String str2;
        boolean z2;
        boolean zLoadThumbnail;
        int iCheckRightsStatus;
        String authority = uri.getAuthority();
        boolean z3 = DocumentInfo.getCursorInt(cursor, "is_drm") > 0;
        int cursorInt = DocumentInfo.getCursorInt(cursor, "drm_method");
        if (SharedMinimal.VERBOSE) {
            Log.d("IconHelper", "DRM isDRM = " + z3 + " drmMethod = " + cursorInt + " support DRM = " + DocumentsFeatureOption.IS_SUPPORT_DRM);
        }
        if (DocumentsFeatureOption.IS_SUPPORT_DRM && z3 && cursorInt > 0) {
            int actionByMimetype = OmaDrmUtils.getActionByMimetype(str);
            String cursorString = DocumentInfo.getCursorString(cursor, "_data");
            if (cursorString != null) {
                iCheckRightsStatus = DocumentsApplication.getDrmClient(this.mContext).checkRightsStatus(cursorString, actionByMimetype);
            } else {
                iCheckRightsStatus = 1;
            }
            int i3 = 134348872;
            if (iCheckRightsStatus == 0) {
                i3 = 134348871;
                z = true;
            } else {
                z = false;
            }
            if (SharedMinimal.VERBOSE) {
                Log.d("IconHelper", "DRM icon displayed");
            }
            if (imageView4 != null) {
                imageView4.setVisibility(0);
                imageView4.setImageResource(i3);
            }
        } else {
            if (SharedMinimal.VERBOSE) {
                Log.d("IconHelper", "DRM icon not displayed");
            }
            if (imageView4 != null) {
                imageView4.setVisibility(8);
            }
            z = true;
        }
        boolean z4 = (i & 1) != 0;
        if (this.mMode != 2) {
            str2 = str;
            if (!MimeTypes.mimeMatches(MimeTypes.VISUAL_MIMES, str2)) {
                z2 = false;
            }
            zLoadThumbnail = !z4 && z2 && this.mThumbnailsEnabled && z ? loadThumbnail(uri, authority, j, imageView, imageView2) : false;
            Drawable documentIcon = getDocumentIcon(this.mContext, authority, DocumentsContract.getDocumentId(uri), str2, i2);
            if (imageView3 != null) {
                setMimeIcon(imageView3, documentIcon);
            }
            if (!zLoadThumbnail) {
                hideImageView(imageView2);
                return;
            } else {
                setMimeIcon(imageView2, documentIcon);
                hideImageView(imageView);
                return;
            }
        }
        str2 = str;
        z2 = true;
        zLoadThumbnail = !z4 && z2 && this.mThumbnailsEnabled && z ? loadThumbnail(uri, authority, j, imageView, imageView2) : false;
        Drawable documentIcon2 = getDocumentIcon(this.mContext, authority, DocumentsContract.getDocumentId(uri), str2, i2);
        if (imageView3 != null) {
        }
        if (!zLoadThumbnail) {
        }
    }

    private boolean loadThumbnail(Uri uri, String str, long j, ImageView imageView, ImageView imageView2) {
        boolean z;
        Cursor cursorQuery;
        boolean z2;
        ThumbnailCache.Result thumbnail = this.mThumbnailCache.getThumbnail(uri, this.mCurrentSize);
        try {
            Bitmap thumbnail2 = thumbnail.getThumbnail();
            imageView.setImageBitmap(thumbnail2);
            boolean z3 = j > thumbnail.getLastModified();
            if (SharedMinimal.VERBOSE) {
                Log.v("IconHelper", String.format("Load thumbnail for %s, got result %d and stale %b.", uri.toString(), Integer.valueOf(thumbnail.getStatus()), Boolean.valueOf(z3)));
            }
            if (SharedMinimal.VERBOSE) {
                Log.v("IconHelper", "loadThumbnail uri = " + uri);
            }
            String[] strArr = {"total_bytes", "current_bytes"};
            String strTrim = uri.toString().substring(uri.toString().lastIndexOf(47) + 1).trim();
            if (SharedMinimal.VERBOSE) {
                Log.v("IconHelper", "loadThumbnail id = " + strTrim);
            }
            try {
                z = z3;
            } catch (Exception e) {
                z = z3;
            }
            try {
                cursorQuery = this.mContext.getContentResolver().query(ContentUris.withAppendedId(Uri.parse("content://downloads/all_downloads"), Long.parseLong(strTrim)), strArr, null, null, null);
                z2 = false;
            } catch (Exception e2) {
                Log.d("IconHelper", "Exception occurred, hence load thumbnail");
                cursorQuery = null;
                z2 = true;
            }
            if (!z2) {
                if (cursorQuery != null && cursorQuery.moveToFirst()) {
                    String[] columnNames = cursorQuery.getColumnNames();
                    Log.v("IconHelper", "loadThumbnail cursor.getCount() = " + cursorQuery.getCount());
                    Log.v("IconHelper", "loadThumbnail columnNames = " + Arrays.toString(columnNames));
                    long cursorLong = DocumentInfo.getCursorLong(cursorQuery, "total_bytes");
                    long cursorLong2 = DocumentInfo.getCursorLong(cursorQuery, "current_bytes");
                    Log.v("IconHelper", "loadThumbnail total_size = " + cursorLong);
                    Log.v("IconHelper", "loadThumbnail current_bytes = " + cursorLong2);
                    if (cursorLong != cursorLong2) {
                        return false;
                    }
                }
                Log.v("IconHelper", "Missing details for uri = " + uri);
                return false;
            }
            if (!thumbnail.isExactHit() || z) {
                LoaderTask loaderTask = new LoaderTask(uri, imageView2, imageView, this.mCurrentSize, j, thumbnail2 == null ? ANIM_FADE_IN : ANIM_NO_OP);
                imageView.setTag(loaderTask);
                ProviderExecutor.forAuthority(str).execute(loaderTask, new Uri[0]);
            }
            return thumbnail.isHit();
        } finally {
            thumbnail.recycle();
        }
    }

    private void setMimeIcon(ImageView imageView, Drawable drawable) {
        imageView.setImageDrawable(drawable);
        imageView.setAlpha(1.0f);
    }

    private void hideImageView(ImageView imageView) {
        imageView.setImageDrawable(null);
        imageView.setAlpha(0.0f);
    }

    private Drawable getDocumentIcon(Context context, String str, String str2, String str3, int i) {
        if (i != 0) {
            return IconUtils.loadPackageIcon(context, str, i);
        }
        return IconUtils.loadMimeIcon(context, str3, str, str2, this.mMode);
    }

    public Drawable getDocumentIcon(Context context, DocumentInfo documentInfo) {
        return getDocumentIcon(context, documentInfo.authority, documentInfo.documentId, documentInfo.mimeType, documentInfo.icon);
    }
}

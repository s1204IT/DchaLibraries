package com.android.documentsui;

import android.content.ContentProviderClient;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.documentsui.ProviderExecutor;
import com.android.documentsui.base.SharedMinimal;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ThumbnailLoader extends AsyncTask<Uri, Void, Bitmap> implements ProviderExecutor.Preemptable {
    private final boolean mAddToCache;
    private final Consumer<Bitmap> mCallback;
    private final ImageView mIconThumb;
    private final long mLastModified;
    private final CancellationSignal mSignal = new CancellationSignal();
    private final Point mThumbSize;
    private final Uri mUri;
    private static final String TAG = ThumbnailLoader.class.getCanonicalName();
    public static final BiConsumer<View, View> ANIM_FADE_IN = new BiConsumer() {
        @Override
        public final void accept(Object obj, Object obj2) {
            ThumbnailLoader.lambda$static$0((View) obj, (View) obj2);
        }
    };
    public static final BiConsumer<View, View> ANIM_NO_OP = new BiConsumer() {
        @Override
        public final void accept(Object obj, Object obj2) {
            ThumbnailLoader.lambda$static$1((View) obj, (View) obj2);
        }
    };

    static void lambda$static$0(View view, View view2) {
        float alpha = view.getAlpha();
        view.animate().alpha(0.0f).start();
        view2.setAlpha(0.0f);
        view2.animate().alpha(alpha).start();
    }

    static void lambda$static$1(View view, View view2) {
    }

    public ThumbnailLoader(Uri uri, ImageView imageView, Point point, long j, Consumer<Bitmap> consumer, boolean z) {
        this.mUri = uri;
        this.mIconThumb = imageView;
        this.mThumbSize = point;
        this.mLastModified = j;
        this.mCallback = consumer;
        this.mAddToCache = z;
        this.mIconThumb.setTag(this);
        if (SharedMinimal.VERBOSE) {
            Log.v(TAG, "Starting icon loader task for " + this.mUri);
        }
    }

    @Override
    public void preempt() {
        if (SharedMinimal.VERBOSE) {
            Log.v(TAG, "Icon loader task for " + this.mUri + " was cancelled.");
        }
        cancel(false);
        this.mSignal.cancel();
    }

    @Override
    protected Bitmap doInBackground(Uri... uriArr) throws Throwable {
        Bitmap documentThumbnail;
        ContentProviderClient contentProviderClient = null;
        if (isCancelled()) {
            return null;
        }
        Context context = this.mIconThumb.getContext();
        try {
            try {
                ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(context.getContentResolver(), this.mUri.getAuthority());
                try {
                    try {
                        documentThumbnail = DocumentsContract.getDocumentThumbnail(contentProviderClientAcquireUnstableProviderOrThrow, this.mUri, this.mThumbSize, this.mSignal);
                        if (documentThumbnail != null) {
                            try {
                                if (this.mAddToCache) {
                                    DocumentsApplication.getThumbnailCache(context).putThumbnail(this.mUri, this.mThumbSize, documentThumbnail, this.mLastModified);
                                }
                            } catch (Exception e) {
                                e = e;
                                contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
                                if (!(e instanceof OperationCanceledException)) {
                                    Log.w(TAG, "Failed to load thumbnail for " + this.mUri + ": " + e);
                                }
                                ContentProviderClient.releaseQuietly(contentProviderClient);
                            }
                        }
                        ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                    } catch (Throwable th) {
                        th = th;
                        contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
                        ContentProviderClient.releaseQuietly(contentProviderClient);
                        throw th;
                    }
                } catch (Exception e2) {
                    e = e2;
                    documentThumbnail = null;
                }
            } catch (Exception e3) {
                e = e3;
                documentThumbnail = null;
            }
            return documentThumbnail;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (SharedMinimal.VERBOSE) {
            Log.v(TAG, "Loader task for " + this.mUri + " completed");
        }
        if (this.mIconThumb.getTag() == this) {
            this.mIconThumb.setTag(null);
            this.mCallback.accept(bitmap);
        }
    }
}

package com.android.documentsui.inspector;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.inspector.InspectorController;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class RuntimeDataSupplier implements InspectorController.DataSupplier {
    private final List<Integer> loaderIds = new ArrayList();
    private final Context mContext;
    private Callbacks mDirCallbacks;
    private Callbacks mDocCallbacks;
    private final LoaderManager mLoaderMgr;
    private LoaderManager.LoaderCallbacks<Bundle> mMetadataCallbacks;

    public RuntimeDataSupplier(Context context, LoaderManager loaderManager) {
        Preconditions.checkArgument(context != null);
        Preconditions.checkArgument(loaderManager != null);
        this.mContext = context;
        this.mLoaderMgr = loaderManager;
    }

    @Override
    public void loadDocInfo(final Uri uri, final Consumer<DocumentInfo> consumer) {
        Preconditions.checkArgument(uri.getScheme().equals("content"));
        this.mDocCallbacks = new Callbacks(this.mContext, uri, new Consumer<Cursor>() {
            @Override
            public void accept(Cursor cursor) {
                if (cursor == null || !cursor.moveToFirst()) {
                    consumer.accept(null);
                } else {
                    consumer.accept(DocumentInfo.fromCursor(cursor, uri.getAuthority()));
                }
            }
        });
        this.mLoaderMgr.restartLoader(getNextLoaderId(), null, this.mDocCallbacks);
    }

    @Override
    public void loadDirCount(DocumentInfo documentInfo, final Consumer<Integer> consumer) {
        Preconditions.checkArgument(documentInfo.isDirectory());
        this.mDirCallbacks = new Callbacks(this.mContext, DocumentsContract.buildChildDocumentsUri(documentInfo.authority, documentInfo.documentId), new Consumer<Cursor>() {
            @Override
            public void accept(Cursor cursor) {
                if (cursor != null && cursor.moveToFirst()) {
                    consumer.accept(Integer.valueOf(cursor.getCount()));
                }
            }
        });
        this.mLoaderMgr.restartLoader(getNextLoaderId(), null, this.mDirCallbacks);
    }

    @Override
    public void getDocumentMetadata(final Uri uri, final Consumer<Bundle> consumer) {
        this.mMetadataCallbacks = new LoaderManager.LoaderCallbacks<Bundle>() {
            @Override
            public Loader<Bundle> onCreateLoader(int i, Bundle bundle) {
                return new MetadataLoader(RuntimeDataSupplier.this.mContext, uri);
            }

            @Override
            public void onLoadFinished(Loader<Bundle> loader, Bundle bundle) {
                consumer.accept(bundle);
            }

            @Override
            public void onLoaderReset(Loader<Bundle> loader) {
            }
        };
        this.mLoaderMgr.restartLoader(getNextLoaderId(), null, this.mMetadataCallbacks);
    }

    @Override
    public void reset() {
        Iterator<Integer> it = this.loaderIds.iterator();
        while (it.hasNext()) {
            this.mLoaderMgr.destroyLoader(it.next().intValue());
        }
        this.loaderIds.clear();
        if (this.mDocCallbacks != null && this.mDocCallbacks.getObserver() != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mDocCallbacks.getObserver());
        }
        if (this.mDirCallbacks != null && this.mDirCallbacks.getObserver() != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mDirCallbacks.getObserver());
        }
    }

    private int getNextLoaderId() {
        int i = 0;
        while (this.mLoaderMgr.getLoader(i) != null) {
            i++;
            Preconditions.checkArgument(i <= Integer.MAX_VALUE);
        }
        this.loaderIds.add(Integer.valueOf(i));
        return i;
    }

    static final class Callbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        private final Consumer<Cursor> mCallback;
        private final Context mContext;
        private ContentObserver mObserver;
        private final Uri mUri;

        Callbacks(Context context, Uri uri, Consumer<Cursor> consumer) {
            Preconditions.checkArgument(context != null);
            Preconditions.checkArgument(uri != null);
            Preconditions.checkArgument(consumer != null);
            this.mContext = context;
            this.mUri = uri;
            this.mCallback = consumer;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(this.mContext, this.mUri, null, null, null, null);
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null) {
                Objects.requireNonNull(loader);
                this.mObserver = new InspectorContentObserver(new Runnable() {
                    @Override
                    public final void run() {
                        loader.onContentChanged();
                    }
                });
                cursor.registerContentObserver(this.mObserver);
            }
            this.mCallback.accept(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (this.mObserver != null) {
                this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
            }
        }

        public ContentObserver getObserver() {
            return this.mObserver;
        }
    }

    private static final class InspectorContentObserver extends ContentObserver {
        private final Runnable mContentChangedCallback;

        public InspectorContentObserver(Runnable runnable) {
            super(new Handler(Looper.getMainLooper()));
            this.mContentChangedCallback = runnable;
        }

        @Override
        public void onChange(boolean z) {
            this.mContentChangedCallback.run();
        }
    }
}

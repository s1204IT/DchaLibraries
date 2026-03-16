package com.bumptech.glide.load.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.bumptech.glide.Priority;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;

public abstract class LocalUriFetcher<T extends Closeable> implements DataFetcher<T> {
    private static final String TAG = "LocalUriFetcher";
    private final WeakReference<Context> contextRef;
    private T data;
    private final Uri uri;

    protected abstract T loadResource(Uri uri, ContentResolver contentResolver) throws FileNotFoundException;

    public LocalUriFetcher(Context context, Uri uri) {
        this.contextRef = new WeakReference<>(context);
        this.uri = uri;
    }

    @Override
    public final T loadData(Priority priority) throws Exception {
        Context context = this.contextRef.get();
        if (context == null) {
            throw new NullPointerException("Context has been cleared in LocalUriFetcher uri: " + this.uri);
        }
        this.data = (T) loadResource(this.uri, context.getContentResolver());
        return this.data;
    }

    @Override
    public void cleanup() {
        if (this.data != null) {
            try {
                this.data.close();
            } catch (IOException e) {
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, "failed to close data", e);
                }
            }
        }
    }

    @Override
    public void cancel() {
    }

    @Override
    public String getId() {
        return this.uri.toString();
    }
}

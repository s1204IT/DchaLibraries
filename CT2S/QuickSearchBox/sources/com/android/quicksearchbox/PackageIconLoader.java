package com.android.quicksearchbox;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.android.quicksearchbox.util.CachedLater;
import com.android.quicksearchbox.util.NamedTask;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.Now;
import com.android.quicksearchbox.util.NowOrLater;
import com.android.quicksearchbox.util.Util;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PackageIconLoader implements IconLoader {
    private final Context mContext;
    private final NamedTaskExecutor mIconLoaderExecutor;
    private Context mPackageContext;
    private final String mPackageName;
    private final Handler mUiThread;

    public PackageIconLoader(Context context, String packageName, Handler uiThread, NamedTaskExecutor iconLoaderExecutor) {
        this.mContext = context;
        this.mPackageName = packageName;
        this.mUiThread = uiThread;
        this.mIconLoaderExecutor = iconLoaderExecutor;
    }

    private boolean ensurePackageContext() {
        if (this.mPackageContext == null) {
            try {
                this.mPackageContext = this.mContext.createPackageContext(this.mPackageName, 4);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("QSB.PackageIconLoader", "Application not found " + this.mPackageName);
                return false;
            }
        }
        return true;
    }

    @Override
    public NowOrLater<Drawable> getIcon(String drawableId) {
        if (TextUtils.isEmpty(drawableId) || "0".equals(drawableId)) {
            return new Now(null);
        }
        if (!ensurePackageContext()) {
            return new Now(null);
        }
        try {
            int resourceId = Integer.parseInt(drawableId);
            Drawable icon = this.mPackageContext.getResources().getDrawable(resourceId);
            return new Now(icon);
        } catch (Resources.NotFoundException e) {
            Log.w("QSB.PackageIconLoader", "Icon resource not found: " + drawableId);
            return new Now(null);
        } catch (NumberFormatException e2) {
            Uri uri = Uri.parse(drawableId);
            if ("android.resource".equals(uri.getScheme())) {
                return new Now(getDrawable(uri));
            }
            return new IconLaterTask(uri);
        }
    }

    @Override
    public Uri getIconUri(String drawableId) {
        if (TextUtils.isEmpty(drawableId) || "0".equals(drawableId) || !ensurePackageContext()) {
            return null;
        }
        try {
            int resourceId = Integer.parseInt(drawableId);
            return Util.getResourceUri(this.mPackageContext, resourceId);
        } catch (NumberFormatException e) {
            return Uri.parse(drawableId);
        }
    }

    public Drawable getDrawable(Uri uri) {
        try {
            String scheme = uri.getScheme();
            if ("android.resource".equals(scheme)) {
                OpenResourceIdResult r = getResourceId(uri);
                try {
                    return r.r.getDrawable(r.id);
                } catch (Resources.NotFoundException e) {
                    throw new FileNotFoundException("Resource does not exist: " + uri);
                }
            }
            InputStream stream = this.mPackageContext.getContentResolver().openInputStream(uri);
            if (stream == null) {
                throw new FileNotFoundException("Failed to open " + uri);
            }
            try {
                Drawable drawableCreateFromStream = Drawable.createFromStream(stream, null);
                try {
                    stream.close();
                    return drawableCreateFromStream;
                } catch (IOException ex) {
                    Log.e("QSB.PackageIconLoader", "Error closing icon stream for " + uri, ex);
                    return drawableCreateFromStream;
                }
            } finally {
            }
        } catch (FileNotFoundException fnfe) {
            Log.w("QSB.PackageIconLoader", "Icon not found: " + uri + ", " + fnfe.getMessage());
            return null;
        }
        Log.w("QSB.PackageIconLoader", "Icon not found: " + uri + ", " + fnfe.getMessage());
        return null;
    }

    private class OpenResourceIdResult {
        public int id;
        public Resources r;

        private OpenResourceIdResult() {
        }
    }

    private OpenResourceIdResult getResourceId(Uri uri) throws FileNotFoundException {
        int id;
        String authority = uri.getAuthority();
        if (TextUtils.isEmpty(authority)) {
            throw new FileNotFoundException("No authority: " + uri);
        }
        try {
            Resources r = this.mPackageContext.getPackageManager().getResourcesForApplication(authority);
            List<String> path = uri.getPathSegments();
            if (path == null) {
                throw new FileNotFoundException("No path: " + uri);
            }
            int len = path.size();
            if (len == 1) {
                try {
                    id = Integer.parseInt(path.get(0));
                } catch (NumberFormatException e) {
                    throw new FileNotFoundException("Single path segment is not a resource ID: " + uri);
                }
            } else if (len == 2) {
                id = r.getIdentifier(path.get(1), path.get(0), authority);
            } else {
                throw new FileNotFoundException("More than two path segments: " + uri);
            }
            if (id == 0) {
                throw new FileNotFoundException("No resource found for: " + uri);
            }
            OpenResourceIdResult res = new OpenResourceIdResult();
            res.r = r;
            res.id = id;
            return res;
        } catch (PackageManager.NameNotFoundException ex) {
            throw new FileNotFoundException("Failed to get resources: " + ex);
        }
    }

    private class IconLaterTask extends CachedLater<Drawable> implements NamedTask {
        private final Uri mUri;

        public IconLaterTask(Uri iconUri) {
            this.mUri = iconUri;
        }

        @Override
        protected void create() {
            PackageIconLoader.this.mIconLoaderExecutor.execute(this);
        }

        @Override
        public void run() {
            final Drawable icon = getIcon();
            PackageIconLoader.this.mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    IconLaterTask.this.store(icon);
                }
            });
        }

        @Override
        public String getName() {
            return PackageIconLoader.this.mPackageName;
        }

        private Drawable getIcon() {
            try {
                return PackageIconLoader.this.getDrawable(this.mUri);
            } catch (Throwable t) {
                Log.e("QSB.PackageIconLoader", "Failed to load icon " + this.mUri, t);
                return null;
            }
        }
    }
}

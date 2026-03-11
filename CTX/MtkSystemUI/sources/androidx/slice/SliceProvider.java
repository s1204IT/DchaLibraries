package androidx.slice;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Process;
import android.support.v4.app.CoreComponentFactory;
import android.support.v4.os.BuildCompat;
import androidx.slice.Slice;
import androidx.slice.compat.CompatPermissionManager;
import androidx.slice.compat.SliceProviderCompat;
import androidx.slice.compat.SliceProviderWrapperContainer;
import androidx.slice.core.R;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class SliceProvider extends ContentProvider implements CoreComponentFactory.CompatWrapped {
    private static Clock sClock;
    private static Set<SliceSpec> sSpecs;
    private final String[] mAutoGrantPermissions;
    private SliceProviderCompat mCompat;
    private List<Uri> mPinnedSliceUris;

    public abstract Slice onBindSlice(Uri uri);

    public abstract boolean onCreateSliceProvider();

    public SliceProvider(String... autoGrantPermissions) {
        this.mAutoGrantPermissions = autoGrantPermissions;
    }

    public SliceProvider() {
        this.mAutoGrantPermissions = new String[0];
    }

    @Override
    public Object getWrapper() {
        if (BuildCompat.isAtLeastP()) {
            return new SliceProviderWrapperContainer.SliceProviderWrapper(this, this.mAutoGrantPermissions);
        }
        return null;
    }

    @Override
    public final boolean onCreate() {
        this.mPinnedSliceUris = new ArrayList(SliceManager.getInstance(getContext()).getPinnedSlices());
        if (!BuildCompat.isAtLeastP()) {
            this.mCompat = new SliceProviderCompat(this, onCreatePermissionManager(this.mAutoGrantPermissions), getContext());
        }
        return onCreateSliceProvider();
    }

    protected CompatPermissionManager onCreatePermissionManager(String[] autoGrantPermissions) {
        return new CompatPermissionManager(getContext(), "slice_perms_" + getClass().getName(), Process.myUid(), autoGrantPermissions);
    }

    @Override
    public final String getType(Uri uri) {
        return "vnd.android.slice";
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (this.mCompat != null) {
            return this.mCompat.call(method, arg, extras);
        }
        return null;
    }

    public static Slice createPermissionSlice(Context context, Uri sliceUri, String callingPackage) {
        Slice.Builder parent = new Slice.Builder(sliceUri);
        Slice.Builder action = new Slice.Builder(parent).addHints("title", "shortcut").addAction(createPermissionIntent(context, sliceUri, callingPackage), new Slice.Builder(parent).build(), null);
        parent.addSubSlice(new Slice.Builder(sliceUri.buildUpon().appendPath("permission").build()).addText(getPermissionString(context, callingPackage), (String) null, new String[0]).addSubSlice(action.build()).build());
        return parent.addHints("permission_request").build();
    }

    public static PendingIntent createPermissionIntent(Context context, Uri sliceUri, String callingPackage) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context.getPackageName(), "androidx.slice.compat.SlicePermissionActivity"));
        intent.putExtra("slice_uri", sliceUri);
        intent.putExtra("pkg", callingPackage);
        intent.putExtra("provider_pkg", context.getPackageName());
        intent.setData(sliceUri.buildUpon().appendQueryParameter("package", callingPackage).build());
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    public static CharSequence getPermissionString(Context context, String callingPackage) {
        PackageManager pm = context.getPackageManager();
        try {
            return context.getString(R.string.abc_slices_permission_request, pm.getApplicationInfo(callingPackage, 0).loadLabel(pm), context.getApplicationInfo().loadLabel(pm));
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Unknown calling app", e);
        }
    }

    public void onSlicePinned(Uri sliceUri) {
    }

    public void onSliceUnpinned(Uri sliceUri) {
    }

    public void handleSlicePinned(Uri sliceUri) {
        if (!this.mPinnedSliceUris.contains(sliceUri)) {
            this.mPinnedSliceUris.add(sliceUri);
        }
    }

    public void handleSliceUnpinned(Uri sliceUri) {
        if (this.mPinnedSliceUris.contains(sliceUri)) {
            this.mPinnedSliceUris.remove(sliceUri);
        }
    }

    public Uri onMapIntentToUri(Intent intent) {
        throw new UnsupportedOperationException("This provider has not implemented intent to uri mapping");
    }

    public Collection<Uri> onGetSliceDescendants(Uri uri) {
        return Collections.emptyList();
    }

    @Override
    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public final Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal) {
        return null;
    }

    @Override
    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        return null;
    }

    @Override
    public final Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public final int bulkInsert(Uri uri, ContentValues[] values) {
        return 0;
    }

    @Override
    public final int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public final int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public final Uri canonicalize(Uri url) {
        return null;
    }

    public static void setSpecs(Set<SliceSpec> specs) {
        sSpecs = specs;
    }

    public static Set<SliceSpec> getCurrentSpecs() {
        return sSpecs;
    }

    public static Clock getClock() {
        return sClock;
    }
}

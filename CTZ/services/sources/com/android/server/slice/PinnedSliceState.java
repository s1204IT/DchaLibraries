package com.android.server.slice;

import android.app.slice.SliceSpec;
import android.content.ContentProviderClient;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class PinnedSliceState {
    private static final long SLICE_TIMEOUT = 5000;
    private static final String TAG = "PinnedSliceState";
    private final Object mLock;
    private final String mPkg;
    private final SliceManagerService mService;
    private boolean mSlicePinned;
    private final Uri mUri;

    @GuardedBy("mLock")
    private final ArraySet<String> mPinnedPkgs = new ArraySet<>();

    @GuardedBy("mLock")
    private final ArrayMap<IBinder, ListenerInfo> mListeners = new ArrayMap<>();

    @GuardedBy("mLock")
    private SliceSpec[] mSupportedSpecs = null;
    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public final void binderDied() {
            this.f$0.handleRecheckListeners();
        }
    };

    public PinnedSliceState(SliceManagerService sliceManagerService, Uri uri, String str) {
        this.mService = sliceManagerService;
        this.mUri = uri;
        this.mPkg = str;
        this.mLock = this.mService.getLock();
    }

    public String getPkg() {
        return this.mPkg;
    }

    public SliceSpec[] getSpecs() {
        return this.mSupportedSpecs;
    }

    public void mergeSpecs(final SliceSpec[] sliceSpecArr) {
        synchronized (this.mLock) {
            if (this.mSupportedSpecs == null) {
                this.mSupportedSpecs = sliceSpecArr;
            } else {
                this.mSupportedSpecs = (SliceSpec[]) Arrays.asList(this.mSupportedSpecs).stream().map(new Function() {
                    @Override
                    public final Object apply(Object obj) {
                        return PinnedSliceState.lambda$mergeSpecs$0(this.f$0, sliceSpecArr, (SliceSpec) obj);
                    }
                }).filter(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return PinnedSliceState.lambda$mergeSpecs$1((SliceSpec) obj);
                    }
                }).toArray(new IntFunction() {
                    @Override
                    public final Object apply(int i) {
                        return PinnedSliceState.lambda$mergeSpecs$2(i);
                    }
                });
            }
        }
    }

    public static SliceSpec lambda$mergeSpecs$0(PinnedSliceState pinnedSliceState, SliceSpec[] sliceSpecArr, SliceSpec sliceSpec) {
        SliceSpec sliceSpecFindSpec = pinnedSliceState.findSpec(sliceSpecArr, sliceSpec.getType());
        if (sliceSpecFindSpec == null) {
            return null;
        }
        if (sliceSpecFindSpec.getRevision() < sliceSpec.getRevision()) {
            return sliceSpecFindSpec;
        }
        return sliceSpec;
    }

    static boolean lambda$mergeSpecs$1(SliceSpec sliceSpec) {
        return sliceSpec != null;
    }

    static SliceSpec[] lambda$mergeSpecs$2(int i) {
        return new SliceSpec[i];
    }

    private SliceSpec findSpec(SliceSpec[] sliceSpecArr, String str) {
        for (SliceSpec sliceSpec : sliceSpecArr) {
            if (Objects.equals(sliceSpec.getType(), str)) {
                return sliceSpec;
            }
        }
        return null;
    }

    public Uri getUri() {
        return this.mUri;
    }

    public void destroy() {
        setSlicePinned(false);
    }

    private void setSlicePinned(boolean z) {
        synchronized (this.mLock) {
            if (this.mSlicePinned == z) {
                return;
            }
            this.mSlicePinned = z;
            if (z) {
                this.mService.getHandler().post(new Runnable() {
                    @Override
                    public final void run() throws Exception {
                        this.f$0.handleSendPinned();
                    }
                });
            } else {
                this.mService.getHandler().post(new Runnable() {
                    @Override
                    public final void run() throws Exception {
                        this.f$0.handleSendUnpinned();
                    }
                });
            }
        }
    }

    public void pin(String str, SliceSpec[] sliceSpecArr, IBinder iBinder) {
        synchronized (this.mLock) {
            this.mListeners.put(iBinder, new ListenerInfo(iBinder, str, true, Binder.getCallingUid(), Binder.getCallingPid()));
            try {
                iBinder.linkToDeath(this.mDeathRecipient, 0);
            } catch (RemoteException e) {
            }
            mergeSpecs(sliceSpecArr);
            setSlicePinned(true);
        }
    }

    public boolean unpin(String str, IBinder iBinder) {
        synchronized (this.mLock) {
            iBinder.unlinkToDeath(this.mDeathRecipient, 0);
            this.mListeners.remove(iBinder);
        }
        return !hasPinOrListener();
    }

    public boolean isListening() {
        boolean z;
        synchronized (this.mLock) {
            z = !this.mListeners.isEmpty();
        }
        return z;
    }

    @VisibleForTesting
    public boolean hasPinOrListener() {
        boolean z;
        synchronized (this.mLock) {
            z = (this.mPinnedPkgs.isEmpty() && this.mListeners.isEmpty()) ? false : true;
        }
        return z;
    }

    ContentProviderClient getClient() {
        ContentProviderClient contentProviderClientAcquireUnstableContentProviderClient = this.mService.getContext().getContentResolver().acquireUnstableContentProviderClient(this.mUri);
        if (contentProviderClientAcquireUnstableContentProviderClient == null) {
            return null;
        }
        contentProviderClientAcquireUnstableContentProviderClient.setDetectNotResponding(SLICE_TIMEOUT);
        return contentProviderClientAcquireUnstableContentProviderClient;
    }

    private void checkSelfRemove() {
        if (!hasPinOrListener()) {
            this.mService.removePinnedSlice(this.mUri);
        }
    }

    private void handleRecheckListeners() {
        if (hasPinOrListener()) {
            synchronized (this.mLock) {
                for (int size = this.mListeners.size() - 1; size >= 0; size--) {
                    if (!this.mListeners.valueAt(size).token.isBinderAlive()) {
                        this.mListeners.removeAt(size);
                    }
                }
                checkSelfRemove();
            }
        }
    }

    private void handleSendPinned() throws Exception {
        ContentProviderClient client = getClient();
        try {
            if (client == null) {
                if (client != null) {
                    return;
                } else {
                    return;
                }
            }
            Bundle bundle = new Bundle();
            bundle.putParcelable("slice_uri", this.mUri);
            try {
                client.call("pin", null, bundle);
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to contact " + this.mUri, e);
            }
            if (client != null) {
                $closeResource(null, client);
            }
        } finally {
            if (client != null) {
                $closeResource(null, client);
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

    private void handleSendUnpinned() throws Exception {
        ContentProviderClient client = getClient();
        try {
            if (client == null) {
                if (client != null) {
                    return;
                } else {
                    return;
                }
            }
            Bundle bundle = new Bundle();
            bundle.putParcelable("slice_uri", this.mUri);
            try {
                client.call("unpin", null, bundle);
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to contact " + this.mUri, e);
            }
            if (client != null) {
                $closeResource(null, client);
            }
        } finally {
            if (client != null) {
                $closeResource(null, client);
            }
        }
    }

    private class ListenerInfo {
        private int callingPid;
        private int callingUid;
        private boolean hasPermission;
        private String pkg;
        private IBinder token;

        public ListenerInfo(IBinder iBinder, String str, boolean z, int i, int i2) {
            this.token = iBinder;
            this.pkg = str;
            this.hasPermission = z;
            this.callingUid = i;
            this.callingPid = i2;
        }
    }
}

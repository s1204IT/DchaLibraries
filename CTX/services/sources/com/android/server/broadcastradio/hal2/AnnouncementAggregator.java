package com.android.server.broadcastradio.hal2;

import android.hardware.radio.Announcement;
import android.hardware.radio.IAnnouncementListener;
import android.hardware.radio.ICloseHandle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.broadcastradio.hal2.TunerCallback;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class AnnouncementAggregator extends ICloseHandle.Stub {
    private static final String TAG = "BcRadio2Srv.AnnAggr";
    private final IAnnouncementListener mListener;
    private final Object mLock = new Object();
    private final IBinder.DeathRecipient mDeathRecipient = new DeathRecipient();

    @GuardedBy("mLock")
    private final Collection<ModuleWatcher> mModuleWatchers = new ArrayList();

    @GuardedBy("mLock")
    private boolean mIsClosed = false;

    public AnnouncementAggregator(IAnnouncementListener iAnnouncementListener) {
        this.mListener = (IAnnouncementListener) Objects.requireNonNull(iAnnouncementListener);
        try {
            iAnnouncementListener.asBinder().linkToDeath(this.mDeathRecipient, 0);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    private class ModuleWatcher extends IAnnouncementListener.Stub {
        public List<Announcement> currentList;
        private ICloseHandle mCloseHandle;

        private ModuleWatcher() {
            this.currentList = new ArrayList();
        }

        public void onListUpdated(List<Announcement> list) {
            this.currentList = (List) Objects.requireNonNull(list);
            AnnouncementAggregator.this.onListUpdated();
        }

        public void setCloseHandle(ICloseHandle iCloseHandle) {
            this.mCloseHandle = (ICloseHandle) Objects.requireNonNull(iCloseHandle);
        }

        public void close() throws RemoteException {
            if (this.mCloseHandle != null) {
                this.mCloseHandle.close();
            }
        }
    }

    private class DeathRecipient implements IBinder.DeathRecipient {
        private DeathRecipient() {
        }

        @Override
        public void binderDied() {
            try {
                AnnouncementAggregator.this.close();
            } catch (RemoteException e) {
            }
        }
    }

    private void onListUpdated() {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                Slog.e(TAG, "Announcement aggregator is closed, it shouldn't receive callbacks");
                return;
            }
            final ArrayList arrayList = new ArrayList();
            Iterator<ModuleWatcher> it = this.mModuleWatchers.iterator();
            while (it.hasNext()) {
                arrayList.addAll(it.next().currentList);
            }
            TunerCallback.dispatch(new TunerCallback.RunnableThrowingRemoteException() {
                @Override
                public final void run() {
                    this.f$0.mListener.onListUpdated(arrayList);
                }
            });
        }
    }

    public void watchModule(RadioModule radioModule, int[] iArr) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                throw new IllegalStateException();
            }
            ModuleWatcher moduleWatcher = new ModuleWatcher();
            try {
                moduleWatcher.setCloseHandle(radioModule.addAnnouncementListener(iArr, moduleWatcher));
                this.mModuleWatchers.add(moduleWatcher);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to add announcement listener", e);
            }
        }
    }

    public void close() throws RemoteException {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mIsClosed = true;
            this.mListener.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
            Iterator<ModuleWatcher> it = this.mModuleWatchers.iterator();
            while (it.hasNext()) {
                it.next().close();
            }
            this.mModuleWatchers.clear();
        }
    }
}

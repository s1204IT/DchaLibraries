package com.android.systemui.recents;

import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.recents.IRecentsNonSystemUserCallbacks;
import com.android.systemui.recents.IRecentsSystemUserCallbacks;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.component.SetWaitingForTransitionStartEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.misc.ForegroundThread;

public class RecentsSystemUser extends IRecentsSystemUserCallbacks.Stub {
    private Context mContext;
    private RecentsImpl mImpl;
    private final SparseArray<IRecentsNonSystemUserCallbacks> mNonSystemUserRecents = new SparseArray<>();

    public RecentsSystemUser(Context context, RecentsImpl recentsImpl) {
        this.mContext = context;
        this.mImpl = recentsImpl;
    }

    @Override
    public void registerNonSystemUserCallbacks(IBinder iBinder, final int i) {
        try {
            final IRecentsNonSystemUserCallbacks iRecentsNonSystemUserCallbacksAsInterface = IRecentsNonSystemUserCallbacks.Stub.asInterface(iBinder);
            iBinder.linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    RecentsSystemUser.this.mNonSystemUserRecents.removeAt(RecentsSystemUser.this.mNonSystemUserRecents.indexOfValue(iRecentsNonSystemUserCallbacksAsInterface));
                    EventLog.writeEvent(36060, 5, Integer.valueOf(i));
                }
            }, 0);
            this.mNonSystemUserRecents.put(i, iRecentsNonSystemUserCallbacksAsInterface);
            EventLog.writeEvent(36060, 4, Integer.valueOf(i));
        } catch (RemoteException e) {
            Log.e("RecentsSystemUser", "Failed to register NonSystemUserCallbacks", e);
        }
    }

    public IRecentsNonSystemUserCallbacks getNonSystemUserRecentsForUser(int i) {
        return this.mNonSystemUserRecents.get(i);
    }

    @Override
    public void updateRecentsVisibility(final boolean z) {
        ForegroundThread.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                RecentsSystemUser recentsSystemUser = this.f$0;
                recentsSystemUser.mImpl.onVisibilityChanged(recentsSystemUser.mContext, z);
            }
        });
    }

    @Override
    public void startScreenPinning(final int i) {
        ForegroundThread.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                RecentsSystemUser recentsSystemUser = this.f$0;
                recentsSystemUser.mImpl.onStartScreenPinning(recentsSystemUser.mContext, i);
            }
        });
    }

    @Override
    public void sendRecentsDrawnEvent() {
        EventBus.getDefault().post(new RecentsDrawnEvent());
    }

    @Override
    public void sendDockingTopTaskEvent(int i, Rect rect) throws RemoteException {
        EventBus.getDefault().post(new DockedTopTaskEvent(i, rect));
    }

    @Override
    public void sendLaunchRecentsEvent() throws RemoteException {
        EventBus.getDefault().post(new RecentsActivityStartingEvent());
    }

    @Override
    public void sendDockedFirstAnimationFrameEvent() throws RemoteException {
        EventBus.getDefault().post(new DockedFirstAnimationFrameEvent());
    }

    @Override
    public void setWaitingForTransitionStartEvent(boolean z) {
        EventBus.getDefault().post(new SetWaitingForTransitionStartEvent(z));
    }
}

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

/* loaded from: classes.dex */
public class RecentsSystemUser extends IRecentsSystemUserCallbacks.Stub {
    private Context mContext;
    private RecentsImpl mImpl;
    private final SparseArray<IRecentsNonSystemUserCallbacks> mNonSystemUserRecents = new SparseArray<>();

    public RecentsSystemUser(Context context, RecentsImpl recentsImpl) {
        this.mContext = context;
        this.mImpl = recentsImpl;
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void registerNonSystemUserCallbacks(IBinder iBinder, final int i) throws RemoteException {
        try {
            final IRecentsNonSystemUserCallbacks iRecentsNonSystemUserCallbacksAsInterface = IRecentsNonSystemUserCallbacks.Stub.asInterface(iBinder);
            iBinder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.systemui.recents.RecentsSystemUser.1
                @Override // android.os.IBinder.DeathRecipient
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

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void updateRecentsVisibility(final boolean z) {
        ForegroundThread.getHandler().post(new Runnable() { // from class: com.android.systemui.recents.-$$Lambda$RecentsSystemUser$mq7gzWWE-rKCOgjCgOrRqm6b0eU
            @Override // java.lang.Runnable
            public final void run() {
                RecentsSystemUser recentsSystemUser = this.f$0;
                recentsSystemUser.mImpl.onVisibilityChanged(recentsSystemUser.mContext, z);
            }
        });
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void startScreenPinning(final int i) {
        ForegroundThread.getHandler().post(new Runnable() { // from class: com.android.systemui.recents.-$$Lambda$RecentsSystemUser$RuMGq01oJynKESbiTF6h02bxcQ4
            @Override // java.lang.Runnable
            public final void run() {
                RecentsSystemUser recentsSystemUser = this.f$0;
                recentsSystemUser.mImpl.onStartScreenPinning(recentsSystemUser.mContext, i);
            }
        });
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void sendRecentsDrawnEvent() {
        EventBus.getDefault().post(new RecentsDrawnEvent());
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void sendDockingTopTaskEvent(int i, Rect rect) throws RemoteException {
        EventBus.getDefault().post(new DockedTopTaskEvent(i, rect));
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void sendLaunchRecentsEvent() throws RemoteException {
        EventBus.getDefault().post(new RecentsActivityStartingEvent());
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void sendDockedFirstAnimationFrameEvent() throws RemoteException {
        EventBus.getDefault().post(new DockedFirstAnimationFrameEvent());
    }

    @Override // com.android.systemui.recents.IRecentsSystemUserCallbacks
    public void setWaitingForTransitionStartEvent(boolean z) {
        EventBus.getDefault().post(new SetWaitingForTransitionStartEvent(z));
    }
}

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
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.misc.ForegroundThread;

public class RecentsSystemUser extends IRecentsSystemUserCallbacks.Stub {
    private Context mContext;
    private RecentsImpl mImpl;
    private final SparseArray<IRecentsNonSystemUserCallbacks> mNonSystemUserRecents = new SparseArray<>();

    public RecentsSystemUser(Context context, RecentsImpl impl) {
        this.mContext = context;
        this.mImpl = impl;
    }

    @Override
    public void registerNonSystemUserCallbacks(IBinder nonSystemUserCallbacks, final int userId) {
        try {
            final IRecentsNonSystemUserCallbacks callback = IRecentsNonSystemUserCallbacks.Stub.asInterface(nonSystemUserCallbacks);
            nonSystemUserCallbacks.linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    RecentsSystemUser.this.mNonSystemUserRecents.removeAt(RecentsSystemUser.this.mNonSystemUserRecents.indexOfValue(callback));
                    EventLog.writeEvent(36060, 5, Integer.valueOf(userId));
                }
            }, 0);
            this.mNonSystemUserRecents.put(userId, callback);
            EventLog.writeEvent(36060, 4, Integer.valueOf(userId));
        } catch (RemoteException e) {
            Log.e("RecentsSystemUser", "Failed to register NonSystemUserCallbacks", e);
        }
    }

    public IRecentsNonSystemUserCallbacks getNonSystemUserRecentsForUser(int userId) {
        return this.mNonSystemUserRecents.get(userId);
    }

    @Override
    public void updateRecentsVisibility(final boolean visible) {
        ForegroundThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                RecentsSystemUser.this.m1031com_android_systemui_recents_RecentsSystemUser_lambda$1(visible);
            }
        });
    }

    void m1031com_android_systemui_recents_RecentsSystemUser_lambda$1(boolean visible) {
        this.mImpl.onVisibilityChanged(this.mContext, visible);
    }

    @Override
    public void startScreenPinning(final int taskId) {
        ForegroundThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                RecentsSystemUser.this.m1032com_android_systemui_recents_RecentsSystemUser_lambda$2(taskId);
            }
        });
    }

    void m1032com_android_systemui_recents_RecentsSystemUser_lambda$2(int taskId) {
        this.mImpl.onStartScreenPinning(this.mContext, taskId);
    }

    @Override
    public void sendRecentsDrawnEvent() {
        EventBus.getDefault().post(new RecentsDrawnEvent());
    }

    @Override
    public void sendDockingTopTaskEvent(int dragMode, Rect initialRect) throws RemoteException {
        EventBus.getDefault().post(new DockedTopTaskEvent(dragMode, initialRect));
    }

    @Override
    public void sendLaunchRecentsEvent() throws RemoteException {
        EventBus.getDefault().post(new RecentsActivityStartingEvent());
    }
}

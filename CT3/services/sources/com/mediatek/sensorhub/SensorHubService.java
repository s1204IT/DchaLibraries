package com.mediatek.sensorhub;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import com.mediatek.sensorhub.ISensorHubService;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class SensorHubService extends ISensorHubService.Stub {
    static final boolean LOG;
    private static final int POST_EVENT_ACTION_DATA = 1;
    private static final String TAG = "SensorHubService";
    private final Context mContext;
    private long mListenerContext;
    private long mNativeContext;
    private PowerManager.WakeLock mWakeLock;
    private int mBroadcastRefCount = 0;
    private final ResultReceiver mResultReceiver = new ResultReceiver();
    private Object mLock = new Object();
    private CopyOnWriteArrayList<ActionHolder> mActionIntents = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<MappingHolder> mIntent = new CopyOnWriteArrayList<>();

    private static class Holder {
        public final int pid = Binder.getCallingPid();
        public final int uid = Binder.getCallingUid();
    }

    private native void nativeAddConGesture(int i, int i2);

    private native boolean nativeCancelAction(int i);

    private native void nativeCancelConGesture(int i, int i2);

    private native boolean nativeEnableGestureWakeup(boolean z);

    private native void nativeFinalize();

    private static native void nativeInit();

    private native int nativeRequestAction(Condition condition, Action action);

    private native void nativeSetup(Object obj);

    private native boolean nativeUpdateCondition(int i, Condition condition);

    public native int[] nativeGetContextList();

    static {
        boolean z = false;
        if (!"user".equals(Build.TYPE) && !"userdebug".equals(Build.TYPE)) {
            z = true;
        }
        LOG = z;
        System.loadLibrary("sensorhub_jni");
        nativeInit();
    }

    class ResultReceiver implements PendingIntent.OnFinished {
        ResultReceiver() {
        }

        @Override
        public void onSendFinished(PendingIntent pi, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
            synchronized (SensorHubService.this.mLock) {
                SensorHubService sensorHubService = SensorHubService.this;
                sensorHubService.mBroadcastRefCount--;
                if (SensorHubService.LOG) {
                    Log.v(SensorHubService.TAG, "onSendFinished: wlCount=" + SensorHubService.this.mBroadcastRefCount);
                }
                if (SensorHubService.this.mBroadcastRefCount == 0) {
                    SensorHubService.this.mWakeLock.release();
                }
            }
        }
    }

    private static class ActionHolder extends Holder {
        public final PendingIntent intent;
        public final boolean repeat;
        public final int rid;

        public ActionHolder(int requestId, PendingIntent intent, boolean repeat) {
            this.rid = requestId;
            this.intent = intent;
            this.repeat = repeat;
        }
    }

    private static class MappingHolder extends Holder {
        public int cgesture;
        public int gesture;
        public int mCount;

        public MappingHolder(int gesture, int cgesture, int count) {
            this.gesture = gesture;
            this.cgesture = cgesture;
            this.mCount = count;
        }
    }

    public SensorHubService(Context context) {
        this.mContext = context;
        nativeSetup(new WeakReference(this));
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, TAG);
    }

    public ParcelableListInteger getContextList() throws RemoteException {
        return new ParcelableListInteger(nativeGetContextList());
    }

    public int requestAction(Condition condition, Action action) throws SensorHubPermissionException, RemoteException {
        int permission = this.mContext.checkCallingOrSelfPermission(SensorHubManager.WAKE_DEVICE_SENSORHUB);
        if (permission != 0) {
            throw new SensorHubPermissionException("Need permission " + SensorHubManager.WAKE_DEVICE_SENSORHUB);
        }
        long origId = Binder.clearCallingIdentity();
        int rid = nativeRequestAction(condition, action);
        Binder.restoreCallingIdentity(origId);
        if (LOG) {
            Log.v(TAG, "requestAction: rid=" + rid + ", " + action);
        }
        if (rid > 0) {
            ActionHolder ah = new ActionHolder(rid, action.getIntent(), action.isRepeatable());
            this.mActionIntents.add(ah);
            if (LOG) {
                Log.v(TAG, "requestAction: add client[rid=" + rid + ", pid=" + ah.pid + ", uid=" + ah.uid + "]");
            }
        }
        return rid;
    }

    public boolean cancelAction(int requestId) throws RemoteException {
        ActionHolder find = null;
        Iterator holder$iterator = this.mActionIntents.iterator();
        while (true) {
            if (!holder$iterator.hasNext()) {
                break;
            }
            ActionHolder holder = (ActionHolder) holder$iterator.next();
            if (holder.rid == requestId) {
                find = holder;
                break;
            }
        }
        if (find == null) {
            if (LOG) {
                Log.v(TAG, "cancelAction: succeed due to no client. rid=" + requestId);
            }
            return true;
        }
        if (find.pid != Binder.getCallingPid() || find.uid != Binder.getCallingUid()) {
            Log.w(TAG, "cancelAction: current[pid=" + Binder.getCallingPid() + ",uid=" + Binder.getCallingUid() + "], old[pid=" + find.pid + ",uid=" + find.uid + "]");
        }
        long origId = Binder.clearCallingIdentity();
        boolean removed = nativeCancelAction(requestId);
        Binder.restoreCallingIdentity(origId);
        if (LOG) {
            Log.v(TAG, "cancelAction: rid=" + requestId + (removed ? " succeed." : " failed!"));
        }
        if (!removed) {
            return false;
        }
        this.mActionIntents.remove(find);
        return true;
    }

    public boolean updateCondition(int requestId, Condition condition) throws SensorHubPermissionException, RemoteException {
        int permission = this.mContext.checkCallingOrSelfPermission(SensorHubManager.WAKE_DEVICE_SENSORHUB);
        if (permission != 0) {
            throw new SensorHubPermissionException("Need permission " + SensorHubManager.WAKE_DEVICE_SENSORHUB);
        }
        long origId = Binder.clearCallingIdentity();
        boolean result = nativeUpdateCondition(requestId, condition);
        Binder.restoreCallingIdentity(origId);
        if (LOG) {
            Log.v(TAG, "updateCondition: rid=" + requestId + (result ? " succeed." : " failed!"));
        }
        return result;
    }

    public boolean enableGestureWakeup(boolean enabled) throws SensorHubPermissionException, RemoteException {
        int permission = this.mContext.checkCallingOrSelfPermission(SensorHubManager.WAKE_DEVICE_SENSORHUB);
        if (permission != 0) {
            throw new SensorHubPermissionException("Need permission " + SensorHubManager.WAKE_DEVICE_SENSORHUB);
        }
        boolean result = nativeEnableGestureWakeup(enabled);
        return result;
    }

    public void addConGesture(int gesture, int cgesture) throws SensorHubPermissionException, RemoteException {
        int permission = this.mContext.checkCallingOrSelfPermission(SensorHubManager.WAKE_DEVICE_SENSORHUB);
        if (permission != 0) {
            throw new SensorHubPermissionException("Need permission " + SensorHubManager.WAKE_DEVICE_SENSORHUB);
        }
        MappingHolder mh = new MappingHolder(gesture, cgesture, 0);
        if (mh == null) {
            return;
        }
        if (this.mIntent.size() == 0) {
            mh.mCount = 1;
            this.mIntent.add(mh);
            nativeAddConGesture(gesture, cgesture);
            return;
        }
        Iterator holder$iterator = this.mIntent.iterator();
        if (!holder$iterator.hasNext()) {
            return;
        }
        MappingHolder holder = (MappingHolder) holder$iterator.next();
        if (holder.gesture == mh.gesture && holder.cgesture == mh.cgesture) {
            holder.mCount++;
            return;
        }
        mh.mCount = 1;
        this.mIntent.add(mh);
        nativeAddConGesture(gesture, cgesture);
    }

    public void cancelConGesture(int gesture, int cgesture) throws SensorHubPermissionException, RemoteException {
        int permission = this.mContext.checkCallingOrSelfPermission(SensorHubManager.WAKE_DEVICE_SENSORHUB);
        if (permission != 0) {
            throw new SensorHubPermissionException("Need permission " + SensorHubManager.WAKE_DEVICE_SENSORHUB);
        }
        if (this.mIntent.size() == 0) {
            return;
        }
        for (MappingHolder holder : this.mIntent) {
            if (holder.gesture == gesture && holder.cgesture == cgesture) {
                holder.mCount--;
                if (holder.mCount != 0) {
                    return;
                }
                nativeCancelConGesture(gesture, cgesture);
                this.mIntent.remove(holder);
                return;
            }
        }
    }

    private ArrayList<DataCell> buildData(Object[] data) {
        ArrayList<DataCell> list = new ArrayList<>();
        if (data != null) {
            DataCell previousClock = null;
            DataCell currentClock = null;
            DataCell previousActivityTime = null;
            DataCell currentActivityTime = null;
            for (Object obj : data) {
                DataCell item = (DataCell) obj;
                if (12 == item.getIndex()) {
                    if (item.isPrevious()) {
                        previousClock = item;
                    } else {
                        currentClock = item;
                    }
                } else if (33 == item.getIndex()) {
                    if (item.isPrevious()) {
                        previousActivityTime = item;
                    } else {
                        currentActivityTime = item;
                    }
                } else {
                    list.add(item);
                }
            }
            if (previousClock != null && previousActivityTime != null) {
                DataCell datacell = new DataCell(34, true, previousClock.getLongValue() - previousActivityTime.getLongValue());
                list.add(datacell);
            } else if (currentClock != null && currentActivityTime != null) {
                DataCell datacell2 = new DataCell(34, false, currentClock.getLongValue() - currentActivityTime.getLongValue());
                list.add(datacell2);
            } else {
                if (previousClock != null) {
                    list.add(previousClock);
                }
                if (currentClock != null) {
                    list.add(currentClock);
                }
                if (previousActivityTime != null) {
                    list.add(previousActivityTime);
                }
                if (currentActivityTime != null) {
                    list.add(currentActivityTime);
                }
            }
        }
        return list;
    }

    private void handleNativeMessage(int msg, int ext1, int ext2, Object[] data) {
        if (LOG) {
            Log.v(TAG, "handleNativeMessage: msg=" + msg + ",arg1=" + ext1 + ", arg2=" + ext2);
        }
        if (msg != 1) {
            return;
        }
        for (ActionHolder holder : this.mActionIntents) {
            if (holder.rid == ext1) {
                if (!holder.repeat) {
                    this.mActionIntents.remove(holder);
                }
                ArrayList<DataCell> list = buildData(data);
                try {
                    if (holder.intent == null) {
                        Log.w(TAG, "handleNativeMessage: null pendingintent!");
                        return;
                    }
                    synchronized (this.mLock) {
                        if (this.mBroadcastRefCount == 0) {
                            this.mWakeLock.acquire();
                        }
                        this.mBroadcastRefCount++;
                        if (LOG) {
                            Log.v(TAG, "handleNativeMessage: sending intent=" + holder.intent + ", wlCount=" + this.mBroadcastRefCount);
                        }
                    }
                    long elapsed = SystemClock.elapsedRealtime();
                    Parcelable actionDataResult = new ActionDataResult(ext1, list, elapsed);
                    Intent intent = new Intent();
                    intent.putExtra("com.mediatek.sensorhub.EXTRA_ACTION_DATA_RESULT", actionDataResult);
                    holder.intent.send(this.mContext, 0, intent, this.mResultReceiver, null);
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "handleNativeMessage: exception for rid " + ext1, e);
                }
            }
        }
    }

    private static void postEventFromNative(Object selfRef, int msg, int ext1, int ext2, Object[] data) {
        SensorHubService service = (SensorHubService) ((WeakReference) selfRef).get();
        if (service == null) {
            Log.e(TAG, "postEventFromNative: Null SensorHubService! msg=" + msg + ", arg1=" + ext1 + ", arg2=" + ext2);
        } else {
            service.handleNativeMessage(msg, ext1, ext2, data);
        }
    }
}

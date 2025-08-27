package com.android.systemui.recents;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import com.android.internal.os.SomeArgs;
import com.android.systemui.recents.IRecentsNonSystemUserCallbacks;

/* loaded from: classes.dex */
public class RecentsImplProxy extends IRecentsNonSystemUserCallbacks.Stub {
    private final Handler mHandler = new Handler() { // from class: com.android.systemui.recents.RecentsImplProxy.1
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    RecentsImplProxy.this.mImpl.preloadRecents();
                    break;
                case 2:
                    RecentsImplProxy.this.mImpl.cancelPreloadingRecents();
                    break;
                case 3:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    RecentsImplProxy.this.mImpl.showRecents(someArgs.argi1 != 0, someArgs.argi2 != 0, someArgs.argi3 != 0, someArgs.argi4);
                    break;
                case 4:
                    RecentsImplProxy.this.mImpl.hideRecents(message.arg1 != 0, message.arg2 != 0);
                    break;
                case 5:
                    RecentsImplProxy.this.mImpl.toggleRecents(((SomeArgs) message.obj).argi1);
                    break;
                case 6:
                    RecentsImplProxy.this.mImpl.onConfigurationChanged();
                    break;
                case 7:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    RecentsImpl recentsImpl = RecentsImplProxy.this.mImpl;
                    int i = someArgs2.argi1;
                    int i2 = someArgs2.argi2;
                    someArgs2.argi3 = 0;
                    recentsImpl.splitPrimaryTask(i, i2, 0, (Rect) someArgs2.arg1);
                    break;
                case 8:
                    RecentsImplProxy.this.mImpl.onDraggingInRecents(((Float) message.obj).floatValue());
                    break;
                case 9:
                    RecentsImplProxy.this.mImpl.onDraggingInRecentsEnded(((Float) message.obj).floatValue());
                    break;
                case 10:
                    RecentsImplProxy.this.mImpl.onShowCurrentUserToast(message.arg1, message.arg2);
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
            super.handleMessage(message);
        }
    };
    private RecentsImpl mImpl;

    public RecentsImplProxy(RecentsImpl recentsImpl) {
        this.mImpl = recentsImpl;
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void preloadRecents() throws RemoteException {
        this.mHandler.sendEmptyMessage(1);
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void cancelPreloadingRecents() throws RemoteException {
        this.mHandler.sendEmptyMessage(2);
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void showRecents(boolean z, boolean z2, boolean z3, int i) throws RemoteException {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.argi1 = z ? 1 : 0;
        someArgsObtain.argi2 = z2 ? 1 : 0;
        someArgsObtain.argi3 = z3 ? 1 : 0;
        someArgsObtain.argi4 = i;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3, someArgsObtain));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void hideRecents(boolean z, boolean z2) throws RemoteException {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4, z ? 1 : 0, z2 ? 1 : 0));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void toggleRecents(int i) throws RemoteException {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.argi1 = i;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5, someArgsObtain));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void onConfigurationChanged() throws RemoteException {
        this.mHandler.sendEmptyMessage(6);
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void splitPrimaryTask(int i, int i2, int i3, Rect rect) throws RemoteException {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.argi1 = i;
        someArgsObtain.argi2 = i2;
        someArgsObtain.argi3 = i3;
        someArgsObtain.arg1 = rect;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(7, someArgsObtain));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void onDraggingInRecents(float f) throws RemoteException {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(8, Float.valueOf(f)));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void onDraggingInRecentsEnded(float f) throws RemoteException {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9, Float.valueOf(f)));
    }

    @Override // com.android.systemui.recents.IRecentsNonSystemUserCallbacks
    public void showCurrentUserToast(int i, int i2) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(10, i, i2));
    }
}

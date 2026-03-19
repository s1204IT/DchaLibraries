package com.android.server.am;

import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IAssistDataReceiver;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.IWindowManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class AssistDataRequester extends IAssistDataReceiver.Stub {
    public static final String KEY_RECEIVER_EXTRA_COUNT = "count";
    public static final String KEY_RECEIVER_EXTRA_INDEX = "index";
    private AppOpsManager mAppOpsManager;
    private final ArrayList<Bundle> mAssistData = new ArrayList<>();
    private final ArrayList<Bitmap> mAssistScreenshot = new ArrayList<>();
    private AssistDataRequesterCallbacks mCallbacks;
    private Object mCallbacksLock;
    private boolean mCanceled;
    private Context mContext;
    private int mPendingDataCount;
    private int mPendingScreenshotCount;
    private int mRequestScreenshotAppOps;
    private int mRequestStructureAppOps;
    private IActivityManager mService;
    private IWindowManager mWindowManager;

    public interface AssistDataRequesterCallbacks {
        @GuardedBy("mCallbacksLock")
        boolean canHandleReceivedAssistDataLocked();

        @GuardedBy("mCallbacksLock")
        void onAssistDataReceivedLocked(Bundle bundle, int i, int i2);

        @GuardedBy("mCallbacksLock")
        void onAssistScreenshotReceivedLocked(Bitmap bitmap);

        @GuardedBy("mCallbacksLock")
        default void onAssistRequestCompleted() {
        }
    }

    public AssistDataRequester(Context context, IActivityManager iActivityManager, IWindowManager iWindowManager, AppOpsManager appOpsManager, AssistDataRequesterCallbacks assistDataRequesterCallbacks, Object obj, int i, int i2) {
        this.mCallbacks = assistDataRequesterCallbacks;
        this.mCallbacksLock = obj;
        this.mWindowManager = iWindowManager;
        this.mService = iActivityManager;
        this.mContext = context;
        this.mAppOpsManager = appOpsManager;
        this.mRequestStructureAppOps = i;
        this.mRequestScreenshotAppOps = i2;
    }

    public void requestAssistData(List<IBinder> list, boolean z, boolean z2, boolean z3, boolean z4, int i, String str) {
        boolean zIsAssistDataAllowedOnCurrentActivity;
        boolean z5;
        int i2;
        boolean z6;
        boolean z7;
        if (list.isEmpty()) {
            tryDispatchRequestComplete();
            return;
        }
        boolean z8 = false;
        try {
            zIsAssistDataAllowedOnCurrentActivity = this.mService.isAssistDataAllowedOnCurrentActivity();
        } catch (RemoteException e) {
            zIsAssistDataAllowedOnCurrentActivity = false;
        }
        boolean z9 = z3 & zIsAssistDataAllowedOnCurrentActivity;
        if (!z || !zIsAssistDataAllowedOnCurrentActivity || this.mRequestScreenshotAppOps == -1) {
            z5 = false;
        } else {
            z5 = true;
        }
        boolean z10 = z4 & z5;
        this.mCanceled = false;
        this.mPendingDataCount = 0;
        this.mPendingScreenshotCount = 0;
        this.mAssistData.clear();
        this.mAssistScreenshot.clear();
        if (z) {
            if (this.mAppOpsManager.checkOpNoThrow(this.mRequestStructureAppOps, i, str) == 0 && z9) {
                int size = list.size();
                int i3 = 0;
                while (true) {
                    if (i3 < size) {
                        IBinder iBinder = list.get(i3);
                        try {
                            MetricsLogger.count(this.mContext, "assist_with_context", 1);
                            Bundle bundle = new Bundle();
                            bundle.putInt(KEY_RECEIVER_EXTRA_INDEX, i3);
                            bundle.putInt(KEY_RECEIVER_EXTRA_COUNT, size);
                            IActivityManager iActivityManager = this.mService;
                            if (i3 != 0) {
                                z6 = false;
                            } else {
                                z6 = true;
                            }
                            if (i3 != 0) {
                                z7 = false;
                            } else {
                                z7 = true;
                            }
                            i2 = i3;
                            try {
                                if (iActivityManager.requestAssistContextExtras(1, this, bundle, iBinder, z6, z7)) {
                                    this.mPendingDataCount++;
                                } else if (i2 == 0) {
                                    if (!this.mCallbacks.canHandleReceivedAssistDataLocked()) {
                                        this.mAssistData.add(null);
                                    } else {
                                        dispatchAssistDataReceived(null);
                                    }
                                }
                            } catch (RemoteException e2) {
                            }
                        } catch (RemoteException e3) {
                            i2 = i3;
                        }
                        i3 = i2 + 1;
                    } else {
                        z8 = z10;
                        break;
                    }
                }
            } else if (!this.mCallbacks.canHandleReceivedAssistDataLocked()) {
                this.mAssistData.add(null);
            } else {
                dispatchAssistDataReceived(null);
            }
        } else {
            z8 = z10;
        }
        if (z2) {
            if (this.mAppOpsManager.checkOpNoThrow(this.mRequestScreenshotAppOps, i, str) == 0 && z8) {
                try {
                    MetricsLogger.count(this.mContext, "assist_with_screen", 1);
                    this.mPendingScreenshotCount++;
                    this.mWindowManager.requestAssistScreenshot(this);
                } catch (RemoteException e4) {
                }
            } else if (!this.mCallbacks.canHandleReceivedAssistDataLocked()) {
                this.mAssistScreenshot.add(null);
            } else {
                dispatchAssistScreenshotReceived(null);
            }
        }
        tryDispatchRequestComplete();
    }

    public void processPendingAssistData() {
        flushPendingAssistData();
        tryDispatchRequestComplete();
    }

    private void flushPendingAssistData() {
        int size = this.mAssistData.size();
        for (int i = 0; i < size; i++) {
            dispatchAssistDataReceived(this.mAssistData.get(i));
        }
        this.mAssistData.clear();
        int size2 = this.mAssistScreenshot.size();
        for (int i2 = 0; i2 < size2; i2++) {
            dispatchAssistScreenshotReceived(this.mAssistScreenshot.get(i2));
        }
        this.mAssistScreenshot.clear();
    }

    public int getPendingDataCount() {
        return this.mPendingDataCount;
    }

    public int getPendingScreenshotCount() {
        return this.mPendingScreenshotCount;
    }

    public void cancel() {
        this.mCanceled = true;
        this.mPendingDataCount = 0;
        this.mPendingScreenshotCount = 0;
        this.mAssistData.clear();
        this.mAssistScreenshot.clear();
    }

    public void onHandleAssistData(Bundle bundle) {
        synchronized (this.mCallbacksLock) {
            if (this.mCanceled) {
                return;
            }
            this.mPendingDataCount--;
            if (this.mCallbacks.canHandleReceivedAssistDataLocked()) {
                flushPendingAssistData();
                dispatchAssistDataReceived(bundle);
                tryDispatchRequestComplete();
            } else {
                this.mAssistData.add(bundle);
            }
        }
    }

    public void onHandleAssistScreenshot(Bitmap bitmap) {
        synchronized (this.mCallbacksLock) {
            if (this.mCanceled) {
                return;
            }
            this.mPendingScreenshotCount--;
            if (this.mCallbacks.canHandleReceivedAssistDataLocked()) {
                flushPendingAssistData();
                dispatchAssistScreenshotReceived(bitmap);
                tryDispatchRequestComplete();
            } else {
                this.mAssistScreenshot.add(bitmap);
            }
        }
    }

    private void dispatchAssistDataReceived(Bundle bundle) {
        int i;
        Bundle bundle2 = bundle != null ? bundle.getBundle("receiverExtras") : null;
        int i2 = 0;
        if (bundle2 != null) {
            i2 = bundle2.getInt(KEY_RECEIVER_EXTRA_INDEX);
            i = bundle2.getInt(KEY_RECEIVER_EXTRA_COUNT);
        } else {
            i = 0;
        }
        this.mCallbacks.onAssistDataReceivedLocked(bundle, i2, i);
    }

    private void dispatchAssistScreenshotReceived(Bitmap bitmap) {
        this.mCallbacks.onAssistScreenshotReceivedLocked(bitmap);
    }

    private void tryDispatchRequestComplete() {
        if (this.mPendingDataCount == 0 && this.mPendingScreenshotCount == 0 && this.mAssistData.isEmpty() && this.mAssistScreenshot.isEmpty()) {
            this.mCallbacks.onAssistRequestCompleted();
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("mPendingDataCount=");
        printWriter.println(this.mPendingDataCount);
        printWriter.print(str);
        printWriter.print("mAssistData=");
        printWriter.println(this.mAssistData);
        printWriter.print(str);
        printWriter.print("mPendingScreenshotCount=");
        printWriter.println(this.mPendingScreenshotCount);
        printWriter.print(str);
        printWriter.print("mAssistScreenshot=");
        printWriter.println(this.mAssistScreenshot);
    }
}

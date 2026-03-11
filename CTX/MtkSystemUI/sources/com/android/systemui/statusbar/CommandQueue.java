package com.android.systemui.statusbar;

import android.content.ComponentName;
import android.graphics.Rect;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.IStatusBar;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.SystemUI;
import java.util.ArrayList;

public class CommandQueue extends IStatusBar.Stub {
    private int mDisable1;
    private int mDisable2;
    private final Object mLock = new Object();
    private ArrayList<Callbacks> mCallbacks = new ArrayList<>();
    private Handler mHandler = new H(Looper.getMainLooper());

    public interface Callbacks {
        default void setIcon(String str, StatusBarIcon statusBarIcon) {
        }

        default void removeIcon(String str) {
        }

        default void disable(int i, int i2, boolean z) {
        }

        default void animateExpandNotificationsPanel() {
        }

        default void animateCollapsePanels(int i) {
        }

        default void togglePanel() {
        }

        default void animateExpandSettingsPanel(String str) {
        }

        default void setSystemUiVisibility(int i, int i2, int i3, int i4, Rect rect, Rect rect2) {
        }

        default void topAppWindowChanged(boolean z) {
        }

        default void setImeWindowStatus(IBinder iBinder, int i, int i2, boolean z) {
        }

        default void showRecentApps(boolean z) {
        }

        default void hideRecentApps(boolean z, boolean z2) {
        }

        default void toggleRecentApps() {
        }

        default void toggleSplitScreen() {
        }

        default void preloadRecentApps() {
        }

        default void dismissKeyboardShortcutsMenu() {
        }

        default void toggleKeyboardShortcutsMenu(int i) {
        }

        default void cancelPreloadRecentApps() {
        }

        default void setWindowState(int i, int i2) {
        }

        default void showScreenPinningRequest(int i) {
        }

        default void appTransitionPending(boolean z) {
        }

        default void appTransitionCancelled() {
        }

        default void appTransitionStarting(long j, long j2, boolean z) {
        }

        default void appTransitionFinished() {
        }

        default void showAssistDisclosure() {
        }

        default void startAssist(Bundle bundle) {
        }

        default void onCameraLaunchGestureDetected(int i) {
        }

        default void showPictureInPictureMenu() {
        }

        default void setTopAppHidesStatusBar(boolean z) {
        }

        default void addQsTile(ComponentName componentName) {
        }

        default void remQsTile(ComponentName componentName) {
        }

        default void clickTile(ComponentName componentName) {
        }

        default void handleSystemKey(int i) {
        }

        default void showPinningEnterExitToast(boolean z) {
        }

        default void showPinningEscapeToast() {
        }

        default void handleShowGlobalActionsMenu() {
        }

        default void handleShowShutdownUi(boolean z, String str) {
        }

        default void showWirelessChargingAnimation(int i) {
        }

        default void onRotationProposal(int i, boolean z) {
        }

        default void showFingerprintDialog(Bundle bundle, IBiometricPromptReceiver iBiometricPromptReceiver) {
        }

        default void onFingerprintAuthenticated() {
        }

        default void onFingerprintHelp(String str) {
        }

        default void onFingerprintError(String str) {
        }

        default void hideFingerprintDialog() {
        }
    }

    protected CommandQueue() {
    }

    public void addCallbacks(Callbacks callbacks) {
        this.mCallbacks.add(callbacks);
        callbacks.disable(this.mDisable1, this.mDisable2, false);
    }

    public void removeCallbacks(Callbacks callbacks) {
        this.mCallbacks.remove(callbacks);
    }

    public void setIcon(String str, StatusBarIcon statusBarIcon) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(65536, 1, 0, new Pair(str, statusBarIcon)).sendToTarget();
        }
    }

    public void removeIcon(String str) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(65536, 2, 0, str).sendToTarget();
        }
    }

    public void disable(int i, int i2, boolean z) {
        synchronized (this.mLock) {
            this.mDisable1 = i;
            this.mDisable2 = i2;
            this.mHandler.removeMessages(131072);
            Message messageObtainMessage = this.mHandler.obtainMessage(131072, i, i2, Boolean.valueOf(z));
            if (Looper.myLooper() == this.mHandler.getLooper()) {
                this.mHandler.handleMessage(messageObtainMessage);
                messageObtainMessage.recycle();
            } else {
                messageObtainMessage.sendToTarget();
            }
        }
    }

    public void disable(int i, int i2) {
        disable(i, i2, true);
    }

    public void recomputeDisableFlags(boolean z) {
        disable(this.mDisable1, this.mDisable2, z);
    }

    public void animateExpandNotificationsPanel() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(196608);
            this.mHandler.sendEmptyMessage(196608);
        }
    }

    public void animateCollapsePanels() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(262144);
            this.mHandler.obtainMessage(262144, 0, 0).sendToTarget();
        }
    }

    public void animateCollapsePanels(int i) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(262144);
            this.mHandler.obtainMessage(262144, i, 0).sendToTarget();
        }
    }

    public void togglePanel() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2293760);
            this.mHandler.obtainMessage(2293760, 0, 0).sendToTarget();
        }
    }

    public void animateExpandSettingsPanel(String str) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(327680);
            this.mHandler.obtainMessage(327680, str).sendToTarget();
        }
    }

    public void setSystemUiVisibility(int i, int i2, int i3, int i4, Rect rect, Rect rect2) {
        synchronized (this.mLock) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.argi1 = i;
            someArgsObtain.argi2 = i2;
            someArgsObtain.argi3 = i3;
            someArgsObtain.argi4 = i4;
            someArgsObtain.arg1 = rect;
            someArgsObtain.arg2 = rect2;
            this.mHandler.obtainMessage(393216, someArgsObtain).sendToTarget();
        }
    }

    public void topAppWindowChanged(boolean z) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(458752);
            this.mHandler.obtainMessage(458752, z ? 1 : 0, 0, null).sendToTarget();
        }
    }

    public void setImeWindowStatus(IBinder iBinder, int i, int i2, boolean z) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(524288);
            Message messageObtainMessage = this.mHandler.obtainMessage(524288, i, i2, iBinder);
            messageObtainMessage.getData().putBoolean("showImeSwitcherKey", z);
            messageObtainMessage.sendToTarget();
        }
    }

    public void showRecentApps(boolean z) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(851968);
            this.mHandler.obtainMessage(851968, z ? 1 : 0, 0, null).sendToTarget();
        }
    }

    public void hideRecentApps(boolean z, boolean z2) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(917504);
            this.mHandler.obtainMessage(917504, z ? 1 : 0, z2 ? 1 : 0, null).sendToTarget();
        }
    }

    public void toggleSplitScreen() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1966080);
            this.mHandler.obtainMessage(1966080, 0, 0, null).sendToTarget();
        }
    }

    public void toggleRecentApps() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(589824);
            Message messageObtainMessage = this.mHandler.obtainMessage(589824, 0, 0, null);
            messageObtainMessage.setAsynchronous(true);
            messageObtainMessage.sendToTarget();
        }
    }

    public void preloadRecentApps() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(655360);
            this.mHandler.obtainMessage(655360, 0, 0, null).sendToTarget();
        }
    }

    public void cancelPreloadRecentApps() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(720896);
            this.mHandler.obtainMessage(720896, 0, 0, null).sendToTarget();
        }
    }

    public void dismissKeyboardShortcutsMenu() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2097152);
            this.mHandler.obtainMessage(2097152).sendToTarget();
        }
    }

    public void toggleKeyboardShortcutsMenu(int i) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1638400);
            this.mHandler.obtainMessage(1638400, i, 0).sendToTarget();
        }
    }

    public void showPictureInPictureMenu() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1703936);
            this.mHandler.obtainMessage(1703936).sendToTarget();
        }
    }

    public void setWindowState(int i, int i2) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(786432, i, i2, null).sendToTarget();
        }
    }

    public void showScreenPinningRequest(int i) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1179648, i, 0, null).sendToTarget();
        }
    }

    public void appTransitionPending() {
        appTransitionPending(false);
    }

    public void appTransitionPending(boolean z) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1245184, z ? 1 : 0, 0).sendToTarget();
        }
    }

    public void appTransitionCancelled() {
        synchronized (this.mLock) {
            this.mHandler.sendEmptyMessage(1310720);
        }
    }

    public void appTransitionStarting(long j, long j2) {
        appTransitionStarting(j, j2, false);
    }

    public void appTransitionStarting(long j, long j2, boolean z) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1376256, z ? 1 : 0, 0, Pair.create(Long.valueOf(j), Long.valueOf(j2))).sendToTarget();
        }
    }

    public void appTransitionFinished() {
        synchronized (this.mLock) {
            this.mHandler.sendEmptyMessage(2031616);
        }
    }

    public void showAssistDisclosure() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1441792);
            this.mHandler.obtainMessage(1441792).sendToTarget();
        }
    }

    public void startAssist(Bundle bundle) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1507328);
            this.mHandler.obtainMessage(1507328, bundle).sendToTarget();
        }
    }

    public void onCameraLaunchGestureDetected(int i) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1572864);
            this.mHandler.obtainMessage(1572864, i, 0).sendToTarget();
        }
    }

    public void addQsTile(ComponentName componentName) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1769472, componentName).sendToTarget();
        }
    }

    public void remQsTile(ComponentName componentName) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1835008, componentName).sendToTarget();
        }
    }

    public void clickQsTile(ComponentName componentName) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1900544, componentName).sendToTarget();
        }
    }

    public void handleSystemKey(int i) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2162688, i, 0).sendToTarget();
        }
    }

    public void showPinningEnterExitToast(boolean z) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2949120, Boolean.valueOf(z)).sendToTarget();
        }
    }

    public void showPinningEscapeToast() {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(3014656).sendToTarget();
        }
    }

    public void showGlobalActionsMenu() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2228224);
            this.mHandler.obtainMessage(2228224).sendToTarget();
        }
    }

    public void setTopAppHidesStatusBar(boolean z) {
        this.mHandler.removeMessages(2424832);
        this.mHandler.obtainMessage(2424832, z ? 1 : 0, 0).sendToTarget();
    }

    public void showShutdownUi(boolean z, String str) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2359296);
            this.mHandler.obtainMessage(2359296, z ? 1 : 0, 0, str).sendToTarget();
        }
    }

    public void showWirelessChargingAnimation(int i) {
        this.mHandler.removeMessages(2883584);
        this.mHandler.obtainMessage(2883584, i, 0).sendToTarget();
    }

    public void onProposedRotationChanged(int i, boolean z) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2490368);
            this.mHandler.obtainMessage(2490368, i, z ? 1 : 0, null).sendToTarget();
        }
    }

    public void showFingerprintDialog(Bundle bundle, IBiometricPromptReceiver iBiometricPromptReceiver) {
        synchronized (this.mLock) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = bundle;
            someArgsObtain.arg2 = iBiometricPromptReceiver;
            this.mHandler.obtainMessage(2555904, someArgsObtain).sendToTarget();
        }
    }

    public void onFingerprintAuthenticated() {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2621440).sendToTarget();
        }
    }

    public void onFingerprintHelp(String str) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2686976, str).sendToTarget();
        }
    }

    public void onFingerprintError(String str) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2752512, str).sendToTarget();
        }
    }

    public void hideFingerprintDialog() {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2818048).sendToTarget();
        }
    }

    private final class H extends Handler {
        private H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            int i = 0;
            switch (message.what & (-65536)) {
                case 65536:
                    switch (message.arg1) {
                        case 1:
                            Pair pair = (Pair) message.obj;
                            while (i < CommandQueue.this.mCallbacks.size()) {
                                ((Callbacks) CommandQueue.this.mCallbacks.get(i)).setIcon((String) pair.first, (StatusBarIcon) pair.second);
                                i++;
                            }
                            break;
                    }
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).removeIcon((String) message.obj);
                        i++;
                    }
                    break;
                case 131072:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).disable(message.arg1, message.arg2, ((Boolean) message.obj).booleanValue());
                        i++;
                    }
                    break;
                case 196608:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).animateExpandNotificationsPanel();
                        i++;
                    }
                    break;
                case 262144:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).animateCollapsePanels(message.arg1);
                        i++;
                    }
                    break;
                case 327680:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).animateExpandSettingsPanel((String) message.obj);
                        i++;
                    }
                    break;
                case 393216:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).setSystemUiVisibility(someArgs.argi1, someArgs.argi2, someArgs.argi3, someArgs.argi4, (Rect) someArgs.arg1, (Rect) someArgs.arg2);
                        i++;
                    }
                    someArgs.recycle();
                    break;
                case 458752:
                    for (int i2 = 0; i2 < CommandQueue.this.mCallbacks.size(); i2++) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i2)).topAppWindowChanged(message.arg1 != 0);
                    }
                    break;
                case 524288:
                    for (int i3 = 0; i3 < CommandQueue.this.mCallbacks.size(); i3++) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i3)).setImeWindowStatus((IBinder) message.obj, message.arg1, message.arg2, message.getData().getBoolean("showImeSwitcherKey", false));
                    }
                    break;
                case 589824:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).toggleRecentApps();
                        i++;
                    }
                    break;
                case 655360:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).preloadRecentApps();
                        i++;
                    }
                    break;
                case 720896:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).cancelPreloadRecentApps();
                        i++;
                    }
                    break;
                case 786432:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).setWindowState(message.arg1, message.arg2);
                        i++;
                    }
                    break;
                case 851968:
                    for (int i4 = 0; i4 < CommandQueue.this.mCallbacks.size(); i4++) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i4)).showRecentApps(message.arg1 != 0);
                    }
                    break;
                case 917504:
                    for (int i5 = 0; i5 < CommandQueue.this.mCallbacks.size(); i5++) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i5)).hideRecentApps(message.arg1 != 0, message.arg2 != 0);
                    }
                    break;
                case 1179648:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).showScreenPinningRequest(message.arg1);
                        i++;
                    }
                    break;
                case 1245184:
                    for (int i6 = 0; i6 < CommandQueue.this.mCallbacks.size(); i6++) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i6)).appTransitionPending(message.arg1 != 0);
                    }
                    break;
                case 1310720:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).appTransitionCancelled();
                        i++;
                    }
                    break;
                case 1376256:
                    for (int i7 = 0; i7 < CommandQueue.this.mCallbacks.size(); i7++) {
                        Pair pair2 = (Pair) message.obj;
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i7)).appTransitionStarting(((Long) pair2.first).longValue(), ((Long) pair2.second).longValue(), message.arg1 != 0);
                    }
                    break;
                case 1441792:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).showAssistDisclosure();
                        i++;
                    }
                    break;
                case 1507328:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).startAssist((Bundle) message.obj);
                        i++;
                    }
                    break;
                case 1572864:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).onCameraLaunchGestureDetected(message.arg1);
                        i++;
                    }
                    break;
                case 1638400:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).toggleKeyboardShortcutsMenu(message.arg1);
                        i++;
                    }
                    break;
                case 1703936:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).showPictureInPictureMenu();
                        i++;
                    }
                    break;
                case 1769472:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).addQsTile((ComponentName) message.obj);
                        i++;
                    }
                    break;
                case 1835008:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).remQsTile((ComponentName) message.obj);
                        i++;
                    }
                    break;
                case 1900544:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).clickTile((ComponentName) message.obj);
                        i++;
                    }
                    break;
                case 1966080:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).toggleSplitScreen();
                        i++;
                    }
                    break;
                case 2031616:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).appTransitionFinished();
                        i++;
                    }
                    break;
                case 2097152:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).dismissKeyboardShortcutsMenu();
                        i++;
                    }
                    break;
                case 2162688:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).handleSystemKey(message.arg1);
                        i++;
                    }
                    break;
                case 2228224:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).handleShowGlobalActionsMenu();
                        i++;
                    }
                    break;
                case 2293760:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).togglePanel();
                        i++;
                    }
                    break;
                case 2359296:
                    for (int i8 = 0; i8 < CommandQueue.this.mCallbacks.size(); i8++) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i8)).handleShowShutdownUi(message.arg1 != 0, (String) message.obj);
                    }
                    break;
                case 2424832:
                    for (int i9 = 0; i9 < CommandQueue.this.mCallbacks.size(); i9++) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i9)).setTopAppHidesStatusBar(message.arg1 != 0);
                    }
                    break;
                case 2490368:
                    for (int i10 = 0; i10 < CommandQueue.this.mCallbacks.size(); i10++) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i10)).onRotationProposal(message.arg1, message.arg2 != 0);
                    }
                    break;
                case 2555904:
                    CommandQueue.this.mHandler.removeMessages(2752512);
                    CommandQueue.this.mHandler.removeMessages(2686976);
                    CommandQueue.this.mHandler.removeMessages(2621440);
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).showFingerprintDialog((Bundle) ((SomeArgs) message.obj).arg1, (IBiometricPromptReceiver) ((SomeArgs) message.obj).arg2);
                        i++;
                    }
                    break;
                case 2621440:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).onFingerprintAuthenticated();
                        i++;
                    }
                    break;
                case 2686976:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).onFingerprintHelp((String) message.obj);
                        i++;
                    }
                    break;
                case 2752512:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).onFingerprintError((String) message.obj);
                        i++;
                    }
                    break;
                case 2818048:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).hideFingerprintDialog();
                        i++;
                    }
                    break;
                case 2883584:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).showWirelessChargingAnimation(message.arg1);
                        i++;
                    }
                    break;
                case 2949120:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).showPinningEnterExitToast(((Boolean) message.obj).booleanValue());
                        i++;
                    }
                    break;
                case 3014656:
                    while (i < CommandQueue.this.mCallbacks.size()) {
                        ((Callbacks) CommandQueue.this.mCallbacks.get(i)).showPinningEscapeToast();
                        i++;
                    }
                    break;
            }
        }
    }

    public static class CommandQueueStart extends SystemUI {
        @Override
        public void start() {
            putComponent(CommandQueue.class, new CommandQueue());
        }
    }
}

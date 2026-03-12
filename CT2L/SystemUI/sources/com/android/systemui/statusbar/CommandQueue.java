package com.android.systemui.statusbar;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import com.android.internal.statusbar.IStatusBar;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarIconList;

public class CommandQueue extends IStatusBar.Stub {
    private Callbacks mCallbacks;
    private Handler mHandler = new H();
    private StatusBarIconList mList;

    public interface Callbacks {
        void addIcon(String str, int i, int i2, StatusBarIcon statusBarIcon);

        void animateCollapsePanels(int i);

        void animateExpandNotificationsPanel();

        void animateExpandSettingsPanel();

        void buzzBeepBlinked();

        void cancelPreloadRecentApps();

        void disable(int i, boolean z);

        void hideRecentApps(boolean z, boolean z2);

        void notificationLightOff();

        void notificationLightPulse(int i, int i2, int i3);

        void preloadRecentApps();

        void removeIcon(String str, int i, int i2);

        void setImeWindowStatus(IBinder iBinder, int i, int i2, boolean z);

        void setSystemUiVisibility(int i, int i2);

        void setWindowState(int i, int i2);

        void showRecentApps(boolean z);

        void showScreenPinningRequest();

        void toggleRecentApps();

        void topAppWindowChanged(boolean z);

        void updateIcon(String str, int i, int i2, StatusBarIcon statusBarIcon, StatusBarIcon statusBarIcon2);
    }

    public CommandQueue(Callbacks callbacks, StatusBarIconList list) {
        this.mCallbacks = callbacks;
        this.mList = list;
    }

    public void setIcon(int index, StatusBarIcon icon) {
        synchronized (this.mList) {
            int what = 65536 | index;
            this.mHandler.removeMessages(what);
            this.mHandler.obtainMessage(what, 1, 0, icon.clone()).sendToTarget();
        }
    }

    public void removeIcon(int index) {
        synchronized (this.mList) {
            int what = 65536 | index;
            this.mHandler.removeMessages(what);
            this.mHandler.obtainMessage(what, 2, 0, null).sendToTarget();
        }
    }

    public void disable(int state) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(131072);
            this.mHandler.obtainMessage(131072, state, 0, null).sendToTarget();
        }
    }

    public void animateExpandNotificationsPanel() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(196608);
            this.mHandler.sendEmptyMessage(196608);
        }
    }

    public void animateCollapsePanels() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(262144);
            this.mHandler.sendEmptyMessage(262144);
        }
    }

    public void animateExpandSettingsPanel() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(327680);
            this.mHandler.sendEmptyMessage(327680);
        }
    }

    public void setSystemUiVisibility(int vis, int mask) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(393216);
            this.mHandler.obtainMessage(393216, vis, mask, null).sendToTarget();
        }
    }

    public void topAppWindowChanged(boolean menuVisible) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(458752);
            this.mHandler.obtainMessage(458752, menuVisible ? 1 : 0, 0, null).sendToTarget();
        }
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(524288);
            Message m = this.mHandler.obtainMessage(524288, vis, backDisposition, token);
            m.getData().putBoolean("showImeSwitcherKey", showImeSwitcher);
            m.sendToTarget();
        }
    }

    public void showRecentApps(boolean triggeredFromAltTab) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(851968);
            this.mHandler.obtainMessage(851968, triggeredFromAltTab ? 1 : 0, 0, null).sendToTarget();
        }
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        synchronized (this.mList) {
            this.mHandler.removeMessages(917504);
            this.mHandler.obtainMessage(917504, triggeredFromAltTab ? 1 : 0, triggeredFromHomeKey ? 1 : 0, null).sendToTarget();
        }
    }

    public void toggleRecentApps() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(589824);
            this.mHandler.obtainMessage(589824, 0, 0, null).sendToTarget();
        }
    }

    public void preloadRecentApps() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(655360);
            this.mHandler.obtainMessage(655360, 0, 0, null).sendToTarget();
        }
    }

    public void cancelPreloadRecentApps() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(720896);
            this.mHandler.obtainMessage(720896, 0, 0, null).sendToTarget();
        }
    }

    public void setWindowState(int window, int state) {
        synchronized (this.mList) {
            this.mHandler.obtainMessage(786432, window, state, null).sendToTarget();
        }
    }

    public void buzzBeepBlinked() {
        synchronized (this.mList) {
            this.mHandler.removeMessages(983040);
            this.mHandler.sendEmptyMessage(983040);
        }
    }

    public void notificationLightOff() {
        synchronized (this.mList) {
            this.mHandler.sendEmptyMessage(1048576);
        }
    }

    public void notificationLightPulse(int argb, int onMillis, int offMillis) {
        synchronized (this.mList) {
            this.mHandler.obtainMessage(1114112, onMillis, offMillis, Integer.valueOf(argb)).sendToTarget();
        }
    }

    public void showScreenPinningRequest() {
        synchronized (this.mList) {
            this.mHandler.sendEmptyMessage(1179648);
        }
    }

    private final class H extends Handler {
        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what & (-65536);
            switch (what) {
                case 65536:
                    int index = msg.what & 65535;
                    int viewIndex = CommandQueue.this.mList.getViewIndex(index);
                    switch (msg.arg1) {
                        case 1:
                            StatusBarIcon icon = (StatusBarIcon) msg.obj;
                            StatusBarIcon old = CommandQueue.this.mList.getIcon(index);
                            if (old == null) {
                                CommandQueue.this.mList.setIcon(index, icon);
                                CommandQueue.this.mCallbacks.addIcon(CommandQueue.this.mList.getSlot(index), index, viewIndex, icon);
                            } else {
                                CommandQueue.this.mList.setIcon(index, icon);
                                CommandQueue.this.mCallbacks.updateIcon(CommandQueue.this.mList.getSlot(index), index, viewIndex, old, icon);
                            }
                            break;
                        case 2:
                            if (CommandQueue.this.mList.getIcon(index) != null) {
                                CommandQueue.this.mList.removeIcon(index);
                                CommandQueue.this.mCallbacks.removeIcon(CommandQueue.this.mList.getSlot(index), index, viewIndex);
                            }
                            break;
                    }
                    break;
                case 131072:
                    CommandQueue.this.mCallbacks.disable(msg.arg1, true);
                    break;
                case 196608:
                    CommandQueue.this.mCallbacks.animateExpandNotificationsPanel();
                    break;
                case 262144:
                    CommandQueue.this.mCallbacks.animateCollapsePanels(0);
                    break;
                case 327680:
                    CommandQueue.this.mCallbacks.animateExpandSettingsPanel();
                    break;
                case 393216:
                    CommandQueue.this.mCallbacks.setSystemUiVisibility(msg.arg1, msg.arg2);
                    break;
                case 458752:
                    CommandQueue.this.mCallbacks.topAppWindowChanged(msg.arg1 != 0);
                    break;
                case 524288:
                    CommandQueue.this.mCallbacks.setImeWindowStatus((IBinder) msg.obj, msg.arg1, msg.arg2, msg.getData().getBoolean("showImeSwitcherKey", false));
                    break;
                case 589824:
                    CommandQueue.this.mCallbacks.toggleRecentApps();
                    break;
                case 655360:
                    CommandQueue.this.mCallbacks.preloadRecentApps();
                    break;
                case 720896:
                    CommandQueue.this.mCallbacks.cancelPreloadRecentApps();
                    break;
                case 786432:
                    CommandQueue.this.mCallbacks.setWindowState(msg.arg1, msg.arg2);
                    break;
                case 851968:
                    CommandQueue.this.mCallbacks.showRecentApps(msg.arg1 != 0);
                    break;
                case 917504:
                    CommandQueue.this.mCallbacks.hideRecentApps(msg.arg1 != 0, msg.arg2 != 0);
                    break;
                case 983040:
                    CommandQueue.this.mCallbacks.buzzBeepBlinked();
                    break;
                case 1048576:
                    CommandQueue.this.mCallbacks.notificationLightOff();
                    break;
                case 1114112:
                    CommandQueue.this.mCallbacks.notificationLightPulse(((Integer) msg.obj).intValue(), msg.arg1, msg.arg2);
                    break;
                case 1179648:
                    CommandQueue.this.mCallbacks.showScreenPinningRequest();
                    break;
            }
        }
    }
}

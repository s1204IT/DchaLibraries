package com.android.systemui.pip.phone;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.pip.phone.PipMediaController;
import com.android.systemui.pip.phone.PipMenuActivityController;
import com.android.systemui.plugins.R;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.component.HidePipMenuEvent;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.shared.system.InputConsumerController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PipMenuActivityController {
    private IActivityManager mActivityManager;
    private ParceledListSlice mAppActions;
    private Context mContext;
    private InputConsumerController mInputConsumerController;
    private ParceledListSlice mMediaActions;
    private PipMediaController mMediaController;
    private int mMenuState;
    private ReferenceCountedTrigger mOnAttachDecrementTrigger;
    private boolean mStartActivityRequested;
    private long mStartActivityRequestedTime;
    private Messenger mToActivityMessenger;
    private ArrayList<Listener> mListeners = new ArrayList<>();
    private Bundle mTmpDismissFractionData = new Bundle();
    private Handler mHandler = new AnonymousClass1();
    private Messenger mMessenger = new Messenger(this.mHandler);
    private Runnable mStartActivityRequestedTimeoutRunnable = new Runnable() {
        @Override
        public final void run() {
            PipMenuActivityController.lambda$new$0(this.f$0);
        }
    };
    private PipMediaController.ActionListener mMediaActionListener = new PipMediaController.ActionListener() {
        @Override
        public void onMediaActionsChanged(List<RemoteAction> list) {
            PipMenuActivityController.this.mMediaActions = new ParceledListSlice(list);
            PipMenuActivityController.this.updateMenuActions();
        }
    };

    public interface Listener {
        void onPipDismiss();

        void onPipExpand();

        void onPipMenuStateChanged(int i, boolean z);

        void onPipMinimize();

        void onPipShowMenu();
    }

    class AnonymousClass1 extends Handler {
        AnonymousClass1() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case R.styleable.AppCompatTheme_textAppearancePopupMenuHeader:
                    PipMenuActivityController.this.onMenuStateChanged(message.arg1, true);
                    break;
                case R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle:
                    PipMenuActivityController.this.mListeners.forEach(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            ((PipMenuActivityController.Listener) obj).onPipExpand();
                        }
                    });
                    break;
                case R.styleable.AppCompatTheme_textAppearanceSearchResultTitle:
                    PipMenuActivityController.this.mListeners.forEach(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            ((PipMenuActivityController.Listener) obj).onPipMinimize();
                        }
                    });
                    break;
                case R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu:
                    PipMenuActivityController.this.mListeners.forEach(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            ((PipMenuActivityController.Listener) obj).onPipDismiss();
                        }
                    });
                    break;
                case R.styleable.AppCompatTheme_textColorAlertDialogListItem:
                    PipMenuActivityController.this.mToActivityMessenger = message.replyTo;
                    PipMenuActivityController.this.setStartActivityRequested(false);
                    if (PipMenuActivityController.this.mOnAttachDecrementTrigger != null) {
                        PipMenuActivityController.this.mOnAttachDecrementTrigger.decrement();
                        PipMenuActivityController.this.mOnAttachDecrementTrigger = null;
                    }
                    if (PipMenuActivityController.this.mToActivityMessenger == null) {
                        PipMenuActivityController.this.onMenuStateChanged(0, true);
                    }
                    break;
                case R.styleable.AppCompatTheme_textColorSearchUrl:
                    PipMenuActivityController.this.mInputConsumerController.registerInputConsumer();
                    break;
                case R.styleable.AppCompatTheme_toolbarNavigationButtonStyle:
                    PipMenuActivityController.this.mInputConsumerController.unregisterInputConsumer();
                    break;
                case R.styleable.AppCompatTheme_toolbarStyle:
                    PipMenuActivityController.this.mListeners.forEach(new Consumer() {
                        @Override
                        public final void accept(Object obj) {
                            ((PipMenuActivityController.Listener) obj).onPipShowMenu();
                        }
                    });
                    break;
            }
        }
    }

    public static void lambda$new$0(PipMenuActivityController pipMenuActivityController) {
        pipMenuActivityController.setStartActivityRequested(false);
        if (pipMenuActivityController.mOnAttachDecrementTrigger != null) {
            pipMenuActivityController.mOnAttachDecrementTrigger.decrement();
            pipMenuActivityController.mOnAttachDecrementTrigger = null;
        }
        Log.e("PipMenuActController", "Expected start menu activity request timed out");
    }

    public PipMenuActivityController(Context context, IActivityManager iActivityManager, PipMediaController pipMediaController, InputConsumerController inputConsumerController) {
        this.mContext = context;
        this.mActivityManager = iActivityManager;
        this.mMediaController = pipMediaController;
        this.mInputConsumerController = inputConsumerController;
        EventBus.getDefault().register(this);
    }

    public boolean isMenuActivityVisible() {
        return this.mToActivityMessenger != null;
    }

    public void onActivityPinned() {
        if (this.mMenuState == 0) {
            this.mInputConsumerController.registerInputConsumer();
        }
    }

    public void onActivityUnpinned() {
        hideMenu();
        setStartActivityRequested(false);
    }

    public void onPinnedStackAnimationEnded() {
        if (this.mToActivityMessenger != null) {
            Message messageObtain = Message.obtain();
            messageObtain.what = 6;
            try {
                this.mToActivityMessenger.send(messageObtain);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify menu pinned animation ended", e);
            }
        }
    }

    public void addListener(Listener listener) {
        if (!this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
        }
    }

    public void setDismissFraction(float f) {
        if (this.mToActivityMessenger == null) {
            if (!this.mStartActivityRequested || isStartActivityRequestedElapsed()) {
                startMenuActivity(0, null, null, false, false);
                return;
            }
            return;
        }
        this.mTmpDismissFractionData.clear();
        this.mTmpDismissFractionData.putFloat("dismiss_fraction", f);
        Message messageObtain = Message.obtain();
        messageObtain.what = 5;
        messageObtain.obj = this.mTmpDismissFractionData;
        try {
            this.mToActivityMessenger.send(messageObtain);
        } catch (RemoteException e) {
            Log.e("PipMenuActController", "Could not notify menu to update dismiss fraction", e);
        }
    }

    public void showMenu(int i, Rect rect, Rect rect2, boolean z, boolean z2) {
        if (this.mToActivityMessenger == null) {
            if (!this.mStartActivityRequested || isStartActivityRequestedElapsed()) {
                startMenuActivity(i, rect, rect2, z, z2);
                return;
            }
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putInt("menu_state", i);
        bundle.putParcelable("stack_bounds", rect);
        bundle.putParcelable("movement_bounds", rect2);
        bundle.putBoolean("allow_timeout", z);
        bundle.putBoolean("resize_menu_on_show", z2);
        Message messageObtain = Message.obtain();
        messageObtain.what = 1;
        messageObtain.obj = bundle;
        try {
            this.mToActivityMessenger.send(messageObtain);
        } catch (RemoteException e) {
            Log.e("PipMenuActController", "Could not notify menu to show", e);
        }
    }

    public void pokeMenu() {
        if (this.mToActivityMessenger != null) {
            Message messageObtain = Message.obtain();
            messageObtain.what = 2;
            try {
                this.mToActivityMessenger.send(messageObtain);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify poke menu", e);
            }
        }
    }

    public void hideMenu() {
        if (this.mToActivityMessenger != null) {
            Message messageObtain = Message.obtain();
            messageObtain.what = 3;
            try {
                this.mToActivityMessenger.send(messageObtain);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify menu to hide", e);
            }
        }
    }

    public void hideMenuWithoutResize() {
        onMenuStateChanged(0, false);
    }

    public void setAppActions(ParceledListSlice parceledListSlice) {
        this.mAppActions = parceledListSlice;
        updateMenuActions();
    }

    private ParceledListSlice resolveMenuActions() {
        if (isValidActions(this.mAppActions)) {
            return this.mAppActions;
        }
        return this.mMediaActions;
    }

    private void startMenuActivity(int i, Rect rect, Rect rect2, boolean z, boolean z2) {
        try {
            ActivityManager.StackInfo stackInfo = this.mActivityManager.getStackInfo(2, 0);
            if (stackInfo != null && stackInfo.taskIds != null && stackInfo.taskIds.length > 0) {
                Intent intent = new Intent(this.mContext, (Class<?>) PipMenuActivity.class);
                intent.putExtra("messenger", this.mMessenger);
                intent.putExtra("actions", (Parcelable) resolveMenuActions());
                if (rect != null) {
                    intent.putExtra("stack_bounds", rect);
                }
                if (rect2 != null) {
                    intent.putExtra("movement_bounds", rect2);
                }
                intent.putExtra("menu_state", i);
                intent.putExtra("allow_timeout", z);
                intent.putExtra("resize_menu_on_show", z2);
                ActivityOptions activityOptionsMakeCustomAnimation = ActivityOptions.makeCustomAnimation(this.mContext, 0, 0);
                activityOptionsMakeCustomAnimation.setLaunchTaskId(stackInfo.taskIds[stackInfo.taskIds.length - 1]);
                activityOptionsMakeCustomAnimation.setTaskOverlay(true, true);
                this.mContext.startActivityAsUser(intent, activityOptionsMakeCustomAnimation.toBundle(), UserHandle.CURRENT);
                setStartActivityRequested(true);
                return;
            }
            Log.e("PipMenuActController", "No PIP tasks found");
        } catch (RemoteException e) {
            setStartActivityRequested(false);
            Log.e("PipMenuActController", "Error showing PIP menu activity", e);
        }
    }

    public void updateMenuActions() {
        if (this.mToActivityMessenger != null) {
            Rect rect = null;
            try {
                ActivityManager.StackInfo stackInfo = this.mActivityManager.getStackInfo(2, 0);
                if (stackInfo != null) {
                    rect = stackInfo.bounds;
                }
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Error showing PIP menu activity", e);
            }
            Bundle bundle = new Bundle();
            bundle.putParcelable("stack_bounds", rect);
            bundle.putParcelable("actions", resolveMenuActions());
            Message messageObtain = Message.obtain();
            messageObtain.what = 4;
            messageObtain.obj = bundle;
            try {
                this.mToActivityMessenger.send(messageObtain);
            } catch (RemoteException e2) {
                Log.e("PipMenuActController", "Could not notify menu activity to update actions", e2);
            }
        }
    }

    private boolean isValidActions(ParceledListSlice parceledListSlice) {
        return parceledListSlice != null && parceledListSlice.getList().size() > 0;
    }

    private boolean isStartActivityRequestedElapsed() {
        return SystemClock.uptimeMillis() - this.mStartActivityRequestedTime >= 300;
    }

    public void onMenuStateChanged(final int i, final boolean z) {
        if (i == 0) {
            this.mInputConsumerController.registerInputConsumer();
        } else {
            this.mInputConsumerController.unregisterInputConsumer();
        }
        if (i != this.mMenuState) {
            this.mListeners.forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((PipMenuActivityController.Listener) obj).onPipMenuStateChanged(i, z);
                }
            });
            if (i == 2) {
                this.mMediaController.addListener(this.mMediaActionListener);
            } else {
                this.mMediaController.removeListener(this.mMediaActionListener);
            }
        }
        this.mMenuState = i;
    }

    public void setStartActivityRequested(boolean z) {
        this.mHandler.removeCallbacks(this.mStartActivityRequestedTimeoutRunnable);
        this.mStartActivityRequested = z;
        this.mStartActivityRequestedTime = z ? SystemClock.uptimeMillis() : 0L;
    }

    public final void onBusEvent(HidePipMenuEvent hidePipMenuEvent) {
        if (this.mStartActivityRequested) {
            this.mOnAttachDecrementTrigger = hidePipMenuEvent.getAnimationTrigger();
            this.mOnAttachDecrementTrigger.increment();
            this.mHandler.removeCallbacks(this.mStartActivityRequestedTimeoutRunnable);
            this.mHandler.postDelayed(this.mStartActivityRequestedTimeoutRunnable, 300L);
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        String str2 = str + "  ";
        printWriter.println(str + "PipMenuActController");
        printWriter.println(str2 + "mMenuState=" + this.mMenuState);
        printWriter.println(str2 + "mToActivityMessenger=" + this.mToActivityMessenger);
        printWriter.println(str2 + "mListeners=" + this.mListeners.size());
        printWriter.println(str2 + "mStartActivityRequested=" + this.mStartActivityRequested);
        printWriter.println(str2 + "mStartActivityRequestedTime=" + this.mStartActivityRequestedTime);
    }
}

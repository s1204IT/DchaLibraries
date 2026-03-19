package com.android.systemui.pip.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.component.HidePipMenuEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PipMenuActivity extends Activity {
    private LinearLayout mActionsGroup;
    private Drawable mBackgroundDrawable;
    private int mBetweenActionPaddingLand;
    private View mDismissButton;
    private ImageView mExpandButton;
    private View mMenuContainer;
    private AnimatorSet mMenuContainerAnimator;
    private int mMenuState;
    private View mSettingsButton;
    private Messenger mToControllerMessenger;
    private PipTouchState mTouchState;
    private ViewConfiguration mViewConfig;
    private View mViewRoot;
    private boolean mAllowMenuTimeout = true;
    private boolean mAllowTouches = true;
    private final List<RemoteAction> mActions = new ArrayList();
    private ValueAnimator.AnimatorUpdateListener mMenuBgUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            PipMenuActivity.this.mBackgroundDrawable.setAlpha((int) (0.3f * ((Float) valueAnimator.getAnimatedValue()).floatValue() * 255.0f));
        }
    };
    private PointF mDownPosition = new PointF();
    private PointF mDownDelta = new PointF();
    private Handler mHandler = new Handler();
    private Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    Bundle bundle = (Bundle) message.obj;
                    PipMenuActivity.this.showMenu(bundle.getInt("menu_state"), (Rect) bundle.getParcelable("stack_bounds"), (Rect) bundle.getParcelable("movement_bounds"), bundle.getBoolean("allow_timeout"), bundle.getBoolean("resize_menu_on_show"));
                    break;
                case 2:
                    PipMenuActivity.this.cancelDelayedFinish();
                    break;
                case 3:
                    PipMenuActivity.this.hideMenu();
                    break;
                case 4:
                    Bundle bundle2 = (Bundle) message.obj;
                    ParceledListSlice parcelable = bundle2.getParcelable("actions");
                    PipMenuActivity.this.setActions((Rect) bundle2.getParcelable("stack_bounds"), parcelable != null ? parcelable.getList() : Collections.EMPTY_LIST);
                    break;
                case 5:
                    PipMenuActivity.this.updateDismissFraction(((Bundle) message.obj).getFloat("dismiss_fraction"));
                    break;
                case 6:
                    PipMenuActivity.this.mAllowTouches = true;
                    break;
            }
        }
    });
    private final Runnable mFinishRunnable = new Runnable() {
        @Override
        public void run() {
            PipMenuActivity.this.hideMenu();
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        this.mViewConfig = ViewConfiguration.get(this);
        this.mTouchState = new PipTouchState(this.mViewConfig, this.mHandler, new Runnable() {
            @Override
            public final void run() {
                PipMenuActivity.lambda$onCreate$0(this.f$0);
            }
        });
        getWindow().addFlags(537133056);
        super.onCreate(bundle);
        setContentView(R.layout.pip_menu_activity);
        this.mBackgroundDrawable = new ColorDrawable(-16777216);
        this.mBackgroundDrawable.setAlpha(0);
        this.mViewRoot = findViewById(R.id.background);
        this.mViewRoot.setBackground(this.mBackgroundDrawable);
        this.mMenuContainer = findViewById(R.id.menu_container);
        this.mMenuContainer.setAlpha(0.0f);
        this.mMenuContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return PipMenuActivity.lambda$onCreate$1(this.f$0, view, motionEvent);
            }
        });
        this.mSettingsButton = findViewById(R.id.settings);
        this.mSettingsButton.setAlpha(0.0f);
        this.mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.showSettings();
            }
        });
        this.mDismissButton = findViewById(R.id.dismiss);
        this.mDismissButton.setAlpha(0.0f);
        this.mDismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.dismissPip();
            }
        });
        this.mActionsGroup = (LinearLayout) findViewById(R.id.actions_group);
        this.mBetweenActionPaddingLand = getResources().getDimensionPixelSize(R.dimen.pip_between_action_padding_land);
        this.mExpandButton = (ImageView) findViewById(R.id.expand_button);
        updateFromIntent(getIntent());
        setTitle(R.string.pip_menu_title);
        setDisablePreviewScreenshots(true);
    }

    public static void lambda$onCreate$0(PipMenuActivity pipMenuActivity) {
        if (pipMenuActivity.mMenuState == 1) {
            pipMenuActivity.showPipMenu();
        } else {
            pipMenuActivity.expandPip();
        }
    }

    public static boolean lambda$onCreate$1(PipMenuActivity pipMenuActivity, View view, MotionEvent motionEvent) {
        pipMenuActivity.mTouchState.onTouchEvent(motionEvent);
        if (motionEvent.getAction() == 1) {
            if (pipMenuActivity.mTouchState.isDoubleTap() || pipMenuActivity.mMenuState == 2) {
                pipMenuActivity.expandPip();
            } else if (!pipMenuActivity.mTouchState.isWaitingForDoubleTap()) {
                if (pipMenuActivity.mMenuState == 1) {
                    pipMenuActivity.showPipMenu();
                }
            } else {
                pipMenuActivity.mTouchState.scheduleDoubleTapTimeoutCallback();
            }
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateFromIntent(intent);
    }

    @Override
    public void onUserInteraction() {
        if (this.mAllowMenuTimeout) {
            repostDelayedFinish(2000L);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        hideMenu();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelDelayedFinish();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notifyActivityCallback(null);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean z) {
        if (!z) {
            finish();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (!this.mAllowTouches) {
            return super.dispatchTouchEvent(motionEvent);
        }
        int action = motionEvent.getAction();
        if (action == 0) {
            this.mDownPosition.set(motionEvent.getX(), motionEvent.getY());
            this.mDownDelta.set(0.0f, 0.0f);
        } else if (action == 2) {
            this.mDownDelta.set(motionEvent.getX() - this.mDownPosition.x, motionEvent.getY() - this.mDownPosition.y);
            if (this.mDownDelta.length() > this.mViewConfig.getScaledTouchSlop() && this.mMenuState != 0) {
                notifyRegisterInputConsumer();
                cancelDelayedFinish();
            }
        } else if (action == 4) {
            hideMenu();
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override
    public void finish() {
        notifyActivityCallback(null);
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
    }

    public final void onBusEvent(final HidePipMenuEvent hidePipMenuEvent) {
        if (this.mMenuState != 0) {
            hidePipMenuEvent.getAnimationTrigger().increment();
            hideMenu(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mHandler.post(new Runnable() {
                        @Override
                        public final void run() {
                            hidePipMenuEvent.getAnimationTrigger().decrement();
                        }
                    });
                }
            }, true, false);
        }
    }

    private void showMenu(int i, Rect rect, Rect rect2, boolean z, boolean z2) {
        this.mAllowMenuTimeout = z;
        if (this.mMenuState != i) {
            this.mAllowTouches = !(z2 && (this.mMenuState == 2 || i == 2));
            cancelDelayedFinish();
            updateActionViews(rect);
            if (this.mMenuContainerAnimator != null) {
                this.mMenuContainerAnimator.cancel();
            }
            notifyMenuStateChange(i);
            this.mMenuContainerAnimator = new AnimatorSet();
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mMenuContainer, (Property<View, Float>) View.ALPHA, this.mMenuContainer.getAlpha(), 1.0f);
            objectAnimatorOfFloat.addUpdateListener(this.mMenuBgUpdateListener);
            ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mSettingsButton, (Property<View, Float>) View.ALPHA, this.mSettingsButton.getAlpha(), 1.0f);
            ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(this.mDismissButton, (Property<View, Float>) View.ALPHA, this.mDismissButton.getAlpha(), 1.0f);
            if (i == 2) {
                this.mMenuContainerAnimator.playTogether(objectAnimatorOfFloat, objectAnimatorOfFloat2, objectAnimatorOfFloat3);
            } else {
                this.mMenuContainerAnimator.playTogether(objectAnimatorOfFloat3);
            }
            this.mMenuContainerAnimator.setInterpolator(Interpolators.ALPHA_IN);
            this.mMenuContainerAnimator.setDuration(125L);
            if (z) {
                this.mMenuContainerAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        PipMenuActivity.this.repostDelayedFinish(3500L);
                    }
                });
            }
            this.mMenuContainerAnimator.start();
            return;
        }
        if (z) {
            repostDelayedFinish(2000L);
        }
        notifyUnregisterInputConsumer();
    }

    private void hideMenu() {
        hideMenu(null, true, false);
    }

    private void hideMenu(final Runnable runnable, boolean z, final boolean z2) {
        if (this.mMenuState != 0) {
            cancelDelayedFinish();
            if (z) {
                notifyMenuStateChange(0);
            }
            this.mMenuContainerAnimator = new AnimatorSet();
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mMenuContainer, (Property<View, Float>) View.ALPHA, this.mMenuContainer.getAlpha(), 0.0f);
            objectAnimatorOfFloat.addUpdateListener(this.mMenuBgUpdateListener);
            this.mMenuContainerAnimator.playTogether(objectAnimatorOfFloat, ObjectAnimator.ofFloat(this.mSettingsButton, (Property<View, Float>) View.ALPHA, this.mSettingsButton.getAlpha(), 0.0f), ObjectAnimator.ofFloat(this.mDismissButton, (Property<View, Float>) View.ALPHA, this.mDismissButton.getAlpha(), 0.0f));
            this.mMenuContainerAnimator.setInterpolator(Interpolators.ALPHA_OUT);
            this.mMenuContainerAnimator.setDuration(125L);
            this.mMenuContainerAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    if (runnable != null) {
                        runnable.run();
                    }
                    if (!z2) {
                        PipMenuActivity.this.finish();
                    }
                }
            });
            this.mMenuContainerAnimator.start();
            return;
        }
        finish();
    }

    private void updateFromIntent(Intent intent) {
        this.mToControllerMessenger = (Messenger) intent.getParcelableExtra("messenger");
        if (this.mToControllerMessenger == null) {
            Log.w("PipMenuActivity", "Controller messenger is null. Stopping.");
            finish();
            return;
        }
        notifyActivityCallback(this.mMessenger);
        EventBus.getDefault().register(this);
        ParceledListSlice parcelableExtra = intent.getParcelableExtra("actions");
        if (parcelableExtra != null) {
            this.mActions.clear();
            this.mActions.addAll(parcelableExtra.getList());
        }
        int intExtra = intent.getIntExtra("menu_state", 0);
        if (intExtra != 0) {
            showMenu(intExtra, (Rect) intent.getParcelableExtra("stack_bounds"), (Rect) intent.getParcelableExtra("movement_bounds"), intent.getBooleanExtra("allow_timeout", true), intent.getBooleanExtra("resize_menu_on_show", false));
        }
    }

    private void setActions(Rect rect, List<RemoteAction> list) {
        this.mActions.clear();
        this.mActions.addAll(list);
        updateActionViews(rect);
    }

    private void updateActionViews(Rect rect) {
        int i;
        ViewGroup viewGroup = (ViewGroup) findViewById(R.id.expand_container);
        ViewGroup viewGroup2 = (ViewGroup) findViewById(R.id.actions_container);
        viewGroup2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return PipMenuActivity.lambda$updateActionViews$6(view, motionEvent);
            }
        });
        if (!this.mActions.isEmpty()) {
            if (this.mMenuState != 1) {
                viewGroup2.setVisibility(0);
                if (this.mActionsGroup != null) {
                    LayoutInflater layoutInflaterFrom = LayoutInflater.from(this);
                    while (this.mActionsGroup.getChildCount() < this.mActions.size()) {
                        this.mActionsGroup.addView((ImageView) layoutInflaterFrom.inflate(R.layout.pip_menu_action, (ViewGroup) this.mActionsGroup, false));
                    }
                    for (int i2 = 0; i2 < this.mActionsGroup.getChildCount(); i2++) {
                        View childAt = this.mActionsGroup.getChildAt(i2);
                        if (i2 < this.mActions.size()) {
                            i = 0;
                        } else {
                            i = 8;
                        }
                        childAt.setVisibility(i);
                    }
                    boolean z = rect != null && rect.width() > rect.height();
                    int i3 = 0;
                    while (i3 < this.mActions.size()) {
                        final RemoteAction remoteAction = this.mActions.get(i3);
                        final ImageView imageView = (ImageView) this.mActionsGroup.getChildAt(i3);
                        remoteAction.getIcon().loadDrawableAsync(this, new Icon.OnDrawableLoadedListener() {
                            @Override
                            public final void onDrawableLoaded(Drawable drawable) {
                                PipMenuActivity.lambda$updateActionViews$7(imageView, drawable);
                            }
                        }, this.mHandler);
                        imageView.setContentDescription(remoteAction.getContentDescription());
                        if (remoteAction.isEnabled()) {
                            imageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public final void onClick(View view) {
                                    PipMenuActivity.lambda$updateActionViews$8(remoteAction, view);
                                }
                            });
                        }
                        imageView.setEnabled(remoteAction.isEnabled());
                        imageView.setAlpha(remoteAction.isEnabled() ? 1.0f : 0.54f);
                        ((LinearLayout.LayoutParams) imageView.getLayoutParams()).leftMargin = (!z || i3 <= 0) ? 0 : this.mBetweenActionPaddingLand;
                        i3++;
                    }
                }
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) viewGroup.getLayoutParams();
                layoutParams.topMargin = getResources().getDimensionPixelSize(R.dimen.pip_action_padding);
                layoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.pip_expand_container_edge_margin);
                viewGroup.requestLayout();
                return;
            }
        }
        viewGroup2.setVisibility(4);
    }

    static boolean lambda$updateActionViews$6(View view, MotionEvent motionEvent) {
        return true;
    }

    static void lambda$updateActionViews$7(ImageView imageView, Drawable drawable) {
        drawable.setTint(-1);
        imageView.setImageDrawable(drawable);
    }

    static void lambda$updateActionViews$8(RemoteAction remoteAction, View view) {
        try {
            remoteAction.getActionIntent().send();
        } catch (PendingIntent.CanceledException e) {
            Log.w("PipMenuActivity", "Failed to send action", e);
        }
    }

    private void updateDismissFraction(float f) {
        int i;
        float f2 = 1.0f - f;
        if (this.mMenuState == 2) {
            this.mMenuContainer.setAlpha(f2);
            this.mSettingsButton.setAlpha(f2);
            this.mDismissButton.setAlpha(f2);
            i = (int) (((0.3f * f2) + (0.6f * f)) * 255.0f);
        } else {
            if (this.mMenuState == 1) {
                this.mDismissButton.setAlpha(f2);
            }
            i = (int) (f * 0.6f * 255.0f);
        }
        this.mBackgroundDrawable.setAlpha(i);
    }

    private void notifyRegisterInputConsumer() {
        Message messageObtain = Message.obtain();
        messageObtain.what = com.android.systemui.plugins.R.styleable.AppCompatTheme_textColorSearchUrl;
        sendMessage(messageObtain, "Could not notify controller to register input consumer");
    }

    private void notifyUnregisterInputConsumer() {
        Message messageObtain = Message.obtain();
        messageObtain.what = com.android.systemui.plugins.R.styleable.AppCompatTheme_toolbarNavigationButtonStyle;
        sendMessage(messageObtain, "Could not notify controller to unregister input consumer");
    }

    private void notifyMenuStateChange(int i) {
        this.mMenuState = i;
        Message messageObtain = Message.obtain();
        messageObtain.what = 100;
        messageObtain.arg1 = i;
        sendMessage(messageObtain, "Could not notify controller of PIP menu visibility");
    }

    private void expandPip() {
        hideMenu(new Runnable() {
            @Override
            public final void run() {
                this.f$0.sendEmptyMessage(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle, "Could not notify controller to expand PIP");
            }
        }, false, false);
    }

    private void dismissPip() {
        hideMenu(new Runnable() {
            @Override
            public final void run() {
                this.f$0.sendEmptyMessage(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu, "Could not notify controller to dismiss PIP");
            }
        }, false, true);
    }

    private void showPipMenu() {
        Message messageObtain = Message.obtain();
        messageObtain.what = com.android.systemui.plugins.R.styleable.AppCompatTheme_toolbarStyle;
        sendMessage(messageObtain, "Could not notify controller to show PIP menu");
    }

    private void showSettings() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Pair<ComponentName, Integer> topPinnedActivity = PipUtils.getTopPinnedActivity(this, ActivityManager.getService());
        if (topPinnedActivity.first != null) {
            UserHandle userHandleOf = UserHandle.of(((Integer) topPinnedActivity.second).intValue());
            Intent intent = new Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", Uri.fromParts("package", ((ComponentName) topPinnedActivity.first).getPackageName(), null));
            intent.putExtra("android.intent.extra.user_handle", userHandleOf);
            intent.setFlags(268468224);
            startActivity(intent);
        }
    }

    private void notifyActivityCallback(Messenger messenger) {
        Message messageObtain = Message.obtain();
        messageObtain.what = com.android.systemui.plugins.R.styleable.AppCompatTheme_textColorAlertDialogListItem;
        messageObtain.replyTo = messenger;
        sendMessage(messageObtain, "Could not notify controller of activity finished");
    }

    private void sendEmptyMessage(int i, String str) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        sendMessage(messageObtain, str);
    }

    private void sendMessage(Message message, String str) {
        if (this.mToControllerMessenger == null) {
            return;
        }
        try {
            this.mToControllerMessenger.send(message);
        } catch (RemoteException e) {
            Log.e("PipMenuActivity", str, e);
        }
    }

    private void cancelDelayedFinish() {
        this.mHandler.removeCallbacks(this.mFinishRunnable);
    }

    private void repostDelayedFinish(long j) {
        this.mHandler.removeCallbacks(this.mFinishRunnable);
        this.mHandler.postDelayed(this.mFinishRunnable, j);
    }
}

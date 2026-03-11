package com.android.quickstep.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.AnimationSuccessListener;
import com.android.launcher3.anim.RoundedRectRevealOutlineProvider;
import com.android.launcher3.shortcuts.DeepShortcutView;
import com.android.quickstep.TaskSystemShortcut;
import com.android.quickstep.TaskUtils;

public class TaskMenuView extends AbstractFloatingView {
    private static final long OPEN_CLOSE_DURATION = 220;
    private BaseDraggingActivity mActivity;
    private AnimatorSet mOpenCloseAnimator;
    private TextView mTaskIconAndName;
    private TaskView mTaskView;
    private static final Rect sTempRect = new Rect();
    public static final TaskSystemShortcut[] MENU_OPTIONS = {new TaskSystemShortcut.AppInfo(), new TaskSystemShortcut.SplitScreen(), new TaskSystemShortcut.Pin(), new TaskSystemShortcut.Install()};

    public TaskMenuView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskMenuView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mActivity = BaseDraggingActivity.fromContext(context);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), TaskMenuView.this.getResources().getDimensionPixelSize(R.dimen.task_menu_background_radius));
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTaskIconAndName = (TextView) findViewById(R.id.task_icon_and_name);
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0 && !this.mActivity.getDragLayer().isEventOverView(this, motionEvent)) {
            close(true);
            return true;
        }
        return false;
    }

    @Override
    protected void handleClose(boolean z) {
        if (z) {
            animateClose();
        } else {
            closeComplete();
        }
    }

    @Override
    public void logActionCommand(int i) {
    }

    @Override
    protected boolean isOfType(int i) {
        return (i & 128) != 0;
    }

    public static boolean showForTask(TaskView taskView) {
        BaseDraggingActivity baseDraggingActivityFromContext = BaseDraggingActivity.fromContext(taskView.getContext());
        return ((TaskMenuView) baseDraggingActivityFromContext.getLayoutInflater().inflate(R.layout.task_menu, (ViewGroup) baseDraggingActivityFromContext.getDragLayer(), false)).populateAndShowForTask(taskView);
    }

    private boolean populateAndShowForTask(TaskView taskView) {
        if (isAttachedToWindow()) {
            return false;
        }
        this.mActivity.getDragLayer().addView(this);
        this.mTaskView = taskView;
        addMenuOptions(this.mTaskView);
        orientAroundTaskView(this.mTaskView);
        post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.animateOpen();
            }
        });
        return true;
    }

    private void addMenuOptions(TaskView taskView) {
        Drawable drawableNewDrawable = taskView.getTask().icon.getConstantState().newDrawable();
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.task_thumbnail_icon_size);
        drawableNewDrawable.setBounds(0, 0, dimensionPixelSize, dimensionPixelSize);
        this.mTaskIconAndName.setCompoundDrawables(null, drawableNewDrawable, null, null);
        this.mTaskIconAndName.setText(TaskUtils.getTitle(getContext(), taskView.getTask()));
        this.mTaskIconAndName.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.close(true);
            }
        });
        for (TaskSystemShortcut taskSystemShortcut : MENU_OPTIONS) {
            View.OnClickListener onClickListener = taskSystemShortcut.getOnClickListener(this.mActivity, taskView);
            if (onClickListener != null) {
                addMenuOption(taskSystemShortcut, onClickListener);
            }
        }
    }

    private void addMenuOption(TaskSystemShortcut taskSystemShortcut, View.OnClickListener onClickListener) {
        DeepShortcutView deepShortcutView = (DeepShortcutView) this.mActivity.getLayoutInflater().inflate(R.layout.system_shortcut, (ViewGroup) this, false);
        deepShortcutView.getIconView().setBackgroundResource(taskSystemShortcut.iconResId);
        deepShortcutView.getBubbleText().setText(taskSystemShortcut.labelResId);
        deepShortcutView.setOnClickListener(onClickListener);
        addView(deepShortcutView);
    }

    private void orientAroundTaskView(TaskView taskView) {
        measure(0, 0);
        this.mActivity.getDragLayer().getDescendantRectRelativeToSelf(taskView, sTempRect);
        int iWidth = (sTempRect.left + ((sTempRect.width() - getMeasuredWidth()) / 2)) - this.mActivity.getDragLayer().getInsets().left;
        if (Utilities.isRtl(getResources())) {
            iWidth = -iWidth;
        }
        setX(iWidth);
        setY((sTempRect.top - this.mTaskIconAndName.getPaddingTop()) - r4.top);
    }

    public void animateOpen() {
        animateOpenOrClosed(false);
        this.mIsOpen = true;
    }

    private void animateClose() {
        animateOpenOrClosed(true);
    }

    private void animateOpenOrClosed(final boolean z) {
        if (this.mOpenCloseAnimator != null && this.mOpenCloseAnimator.isRunning()) {
            return;
        }
        this.mOpenCloseAnimator = LauncherAnimUtils.createAnimatorSet();
        this.mOpenCloseAnimator.play(createOpenCloseOutlineProvider().createRevealAnimator(this, z));
        this.mOpenCloseAnimator.addListener(new AnimationSuccessListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                TaskMenuView.this.setVisibility(0);
            }

            @Override
            public void onAnimationSuccess(Animator animator) {
                if (z) {
                    TaskMenuView.this.closeComplete();
                }
            }
        });
        AnimatorSet animatorSet = this.mOpenCloseAnimator;
        Property property = ALPHA;
        float[] fArr = new float[1];
        fArr[0] = z ? 0.0f : 1.0f;
        animatorSet.play(ObjectAnimator.ofFloat(this, (Property<TaskMenuView, Float>) property, fArr));
        this.mOpenCloseAnimator.setDuration(OPEN_CLOSE_DURATION);
        this.mOpenCloseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        this.mOpenCloseAnimator.start();
    }

    public void closeComplete() {
        this.mIsOpen = false;
        this.mActivity.getDragLayer().removeView(this);
    }

    private RoundedRectRevealOutlineProvider createOpenCloseOutlineProvider() {
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.task_thumbnail_icon_size) / 2;
        float dimensionPixelSize2 = getResources().getDimensionPixelSize(R.dimen.task_menu_background_radius);
        Point point = new Point(getWidth() / 2, this.mTaskIconAndName.getPaddingTop() + dimensionPixelSize);
        return new RoundedRectRevealOutlineProvider(dimensionPixelSize, dimensionPixelSize2, new Rect(point.x, point.y, point.x, point.y), new Rect(0, 0, getWidth(), getHeight())) {
            @Override
            public boolean shouldRemoveElevationDuringAnimation() {
                return true;
            }
        };
    }
}

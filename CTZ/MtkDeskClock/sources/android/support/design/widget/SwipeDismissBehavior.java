package android.support.design.widget;

import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class SwipeDismissBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    private static final float DEFAULT_ALPHA_END_DISTANCE = 0.5f;
    private static final float DEFAULT_ALPHA_START_DISTANCE = 0.0f;
    private static final float DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5f;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_SETTLING = 2;
    public static final int SWIPE_DIRECTION_ANY = 2;
    public static final int SWIPE_DIRECTION_END_TO_START = 1;
    public static final int SWIPE_DIRECTION_START_TO_END = 0;
    private boolean interceptingEvents;
    OnDismissListener listener;
    private boolean sensitivitySet;
    ViewDragHelper viewDragHelper;
    private float sensitivity = 0.0f;
    int swipeDirection = 2;
    float dragDismissThreshold = 0.5f;
    float alphaStartSwipeDistance = 0.0f;
    float alphaEndSwipeDistance = 0.5f;
    private final ViewDragHelper.Callback dragCallback = new ViewDragHelper.Callback() {
        private static final int INVALID_POINTER_ID = -1;
        private int activePointerId = -1;
        private int originalCapturedViewLeft;

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return this.activePointerId == -1 && SwipeDismissBehavior.this.canSwipeDismissView(child);
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            this.activePointerId = activePointerId;
            this.originalCapturedViewLeft = capturedChild.getLeft();
            ViewParent parent = capturedChild.getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (SwipeDismissBehavior.this.listener != null) {
                SwipeDismissBehavior.this.listener.onDragStateChanged(state);
            }
        }

        @Override
        public void onViewReleased(View child, float xvel, float yvel) {
            int targetLeft;
            this.activePointerId = -1;
            int childWidth = child.getWidth();
            boolean dismiss = false;
            if (shouldDismiss(child, xvel)) {
                targetLeft = child.getLeft() < this.originalCapturedViewLeft ? this.originalCapturedViewLeft - childWidth : this.originalCapturedViewLeft + childWidth;
                dismiss = true;
            } else {
                targetLeft = this.originalCapturedViewLeft;
            }
            if (SwipeDismissBehavior.this.viewDragHelper.settleCapturedViewAt(targetLeft, child.getTop())) {
                ViewCompat.postOnAnimation(child, new SettleRunnable(child, dismiss));
            } else if (dismiss && SwipeDismissBehavior.this.listener != null) {
                SwipeDismissBehavior.this.listener.onDismiss(child);
            }
        }

        private boolean shouldDismiss(View child, float xvel) {
            if (xvel != 0.0f) {
                boolean isRtl = ViewCompat.getLayoutDirection(child) == 1;
                if (SwipeDismissBehavior.this.swipeDirection == 2) {
                    return true;
                }
                if (SwipeDismissBehavior.this.swipeDirection == 0) {
                    if (isRtl) {
                        if (xvel >= 0.0f) {
                            return false;
                        }
                    } else if (xvel <= 0.0f) {
                        return false;
                    }
                    return true;
                }
                if (SwipeDismissBehavior.this.swipeDirection != 1) {
                    return false;
                }
                if (isRtl) {
                    if (xvel <= 0.0f) {
                        return false;
                    }
                } else if (xvel >= 0.0f) {
                    return false;
                }
                return true;
            }
            int distance = child.getLeft() - this.originalCapturedViewLeft;
            int thresholdDistance = Math.round(child.getWidth() * SwipeDismissBehavior.this.dragDismissThreshold);
            return Math.abs(distance) >= thresholdDistance;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return child.getWidth();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int min;
            int max;
            boolean isRtl = ViewCompat.getLayoutDirection(child) == 1;
            if (SwipeDismissBehavior.this.swipeDirection == 0) {
                if (isRtl) {
                    min = this.originalCapturedViewLeft - child.getWidth();
                    max = this.originalCapturedViewLeft;
                } else {
                    min = this.originalCapturedViewLeft;
                    max = this.originalCapturedViewLeft + child.getWidth();
                }
            } else if (SwipeDismissBehavior.this.swipeDirection == 1) {
                if (isRtl) {
                    min = this.originalCapturedViewLeft;
                    max = this.originalCapturedViewLeft + child.getWidth();
                } else {
                    int min2 = this.originalCapturedViewLeft;
                    min = min2 - child.getWidth();
                    max = this.originalCapturedViewLeft;
                }
            } else {
                int min3 = this.originalCapturedViewLeft;
                min = min3 - child.getWidth();
                max = this.originalCapturedViewLeft + child.getWidth();
            }
            return SwipeDismissBehavior.clamp(min, left, max);
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return child.getTop();
        }

        @Override
        public void onViewPositionChanged(View child, int left, int top, int dx, int dy) {
            float startAlphaDistance = this.originalCapturedViewLeft + (child.getWidth() * SwipeDismissBehavior.this.alphaStartSwipeDistance);
            float endAlphaDistance = this.originalCapturedViewLeft + (child.getWidth() * SwipeDismissBehavior.this.alphaEndSwipeDistance);
            if (left <= startAlphaDistance) {
                child.setAlpha(1.0f);
            } else if (left >= endAlphaDistance) {
                child.setAlpha(0.0f);
            } else {
                float distance = SwipeDismissBehavior.fraction(startAlphaDistance, endAlphaDistance, left);
                child.setAlpha(SwipeDismissBehavior.clamp(0.0f, 1.0f - distance, 1.0f));
            }
        }
    };

    public interface OnDismissListener {
        void onDismiss(View view);

        void onDragStateChanged(int i);
    }

    public void setListener(OnDismissListener listener) {
        this.listener = listener;
    }

    public void setSwipeDirection(int direction) {
        this.swipeDirection = direction;
    }

    public void setDragDismissDistance(float distance) {
        this.dragDismissThreshold = clamp(0.0f, distance, 1.0f);
    }

    public void setStartAlphaSwipeDistance(float fraction) {
        this.alphaStartSwipeDistance = clamp(0.0f, fraction, 1.0f);
    }

    public void setEndAlphaSwipeDistance(float fraction) {
        this.alphaEndSwipeDistance = clamp(0.0f, fraction, 1.0f);
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
        this.sensitivitySet = true;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        boolean dispatchEventToHelper = this.interceptingEvents;
        int actionMasked = event.getActionMasked();
        if (actionMasked != 3) {
            switch (actionMasked) {
                case 0:
                    this.interceptingEvents = parent.isPointInChildBounds(child, (int) event.getX(), (int) event.getY());
                    dispatchEventToHelper = this.interceptingEvents;
                    break;
                case 1:
                    this.interceptingEvents = false;
                    break;
            }
        }
        if (!dispatchEventToHelper) {
            return false;
        }
        ensureViewDragHelper(parent);
        return this.viewDragHelper.shouldInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (this.viewDragHelper != null) {
            this.viewDragHelper.processTouchEvent(event);
            return true;
        }
        return false;
    }

    public boolean canSwipeDismissView(@NonNull View view) {
        return true;
    }

    private void ensureViewDragHelper(ViewGroup parent) {
        ViewDragHelper viewDragHelperCreate;
        if (this.viewDragHelper == null) {
            if (this.sensitivitySet) {
                viewDragHelperCreate = ViewDragHelper.create(parent, this.sensitivity, this.dragCallback);
            } else {
                viewDragHelperCreate = ViewDragHelper.create(parent, this.dragCallback);
            }
            this.viewDragHelper = viewDragHelperCreate;
        }
    }

    private class SettleRunnable implements Runnable {
        private final boolean dismiss;
        private final View view;

        SettleRunnable(View view, boolean dismiss) {
            this.view = view;
            this.dismiss = dismiss;
        }

        @Override
        public void run() {
            if (SwipeDismissBehavior.this.viewDragHelper != null && SwipeDismissBehavior.this.viewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(this.view, this);
            } else if (this.dismiss && SwipeDismissBehavior.this.listener != null) {
                SwipeDismissBehavior.this.listener.onDismiss(this.view);
            }
        }
    }

    static float clamp(float min, float value, float max) {
        return Math.min(Math.max(min, value), max);
    }

    static int clamp(int min, int value, int max) {
        return Math.min(Math.max(min, value), max);
    }

    public int getDragState() {
        if (this.viewDragHelper != null) {
            return this.viewDragHelper.getViewDragState();
        }
        return 0;
    }

    static float fraction(float startValue, float endValue, float value) {
        return (value - startValue) / (endValue - startValue);
    }
}

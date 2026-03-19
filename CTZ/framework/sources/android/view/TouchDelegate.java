package android.view;

import android.graphics.Rect;

public class TouchDelegate {
    public static final int ABOVE = 1;
    public static final int BELOW = 2;
    public static final int TO_LEFT = 4;
    public static final int TO_RIGHT = 8;
    private Rect mBounds;
    private boolean mDelegateTargeted;
    private View mDelegateView;
    private int mSlop;
    private Rect mSlopBounds;

    public TouchDelegate(Rect rect, View view) {
        this.mBounds = rect;
        this.mSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        this.mSlopBounds = new Rect(rect);
        this.mSlopBounds.inset(-this.mSlop, -this.mSlop);
        this.mDelegateView = view;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z;
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        boolean z2 = true;
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.mDelegateTargeted = this.mBounds.contains(x, y);
                z = this.mDelegateTargeted;
                break;
            case 1:
            case 2:
            case 5:
            case 6:
                boolean z3 = this.mDelegateTargeted;
                if (z3 && !this.mSlopBounds.contains(x, y)) {
                    z2 = false;
                }
                z = z3;
                break;
            case 3:
                z = this.mDelegateTargeted;
                this.mDelegateTargeted = false;
                break;
            case 4:
            default:
                z = false;
                break;
        }
        if (!z) {
            return false;
        }
        View view = this.mDelegateView;
        if (z2) {
            motionEvent.setLocation(view.getWidth() / 2, view.getHeight() / 2);
        } else {
            float f = -(this.mSlop * 2);
            motionEvent.setLocation(f, f);
        }
        return view.dispatchTouchEvent(motionEvent);
    }
}

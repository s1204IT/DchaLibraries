package android.support.v7.widget;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;

public final class ViewStubCompat extends View {
    private OnInflateListener mInflateListener;
    private int mInflatedId;
    private WeakReference<View> mInflatedViewRef;
    private LayoutInflater mInflater;
    private int mLayoutResource;

    public interface OnInflateListener {
        void onInflate(ViewStubCompat viewStubCompat, View view);
    }

    public void setLayoutInflater(LayoutInflater inflater) {
        this.mInflater = inflater;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    @Override
    @SuppressLint({"MissingSuperCall"})
    public void draw(Canvas canvas) {
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
    }

    @Override
    public void setVisibility(int visibility) {
        if (this.mInflatedViewRef != null) {
            View view = this.mInflatedViewRef.get();
            if (view != null) {
                view.setVisibility(visibility);
                return;
            }
            throw new IllegalStateException("setVisibility called on un-referenced view");
        }
        super.setVisibility(visibility);
        if (visibility == 0 || visibility == 4) {
            inflate();
        }
    }

    public View inflate() {
        LayoutInflater layoutInflaterFrom;
        ?? parent = getParent();
        if (parent != 0 && (parent instanceof ViewGroup)) {
            if (this.mLayoutResource != 0) {
                if (this.mInflater != null) {
                    layoutInflaterFrom = this.mInflater;
                } else {
                    layoutInflaterFrom = LayoutInflater.from(getContext());
                }
                View viewInflate = layoutInflaterFrom.inflate(this.mLayoutResource, (ViewGroup) parent, false);
                if (this.mInflatedId != -1) {
                    viewInflate.setId(this.mInflatedId);
                }
                int iIndexOfChild = parent.indexOfChild(this);
                parent.removeViewInLayout(this);
                ViewGroup.LayoutParams layoutParams = getLayoutParams();
                if (layoutParams != null) {
                    parent.addView(viewInflate, iIndexOfChild, layoutParams);
                } else {
                    parent.addView(viewInflate, iIndexOfChild);
                }
                this.mInflatedViewRef = new WeakReference<>(viewInflate);
                if (this.mInflateListener != null) {
                    this.mInflateListener.onInflate(this, viewInflate);
                }
                return viewInflate;
            }
            throw new IllegalArgumentException("ViewStub must have a valid layoutResource");
        }
        throw new IllegalStateException("ViewStub must have a non-null ViewGroup viewParent");
    }
}

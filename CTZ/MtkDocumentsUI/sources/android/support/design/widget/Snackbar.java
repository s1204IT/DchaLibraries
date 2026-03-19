package android.support.design.widget;

import android.content.Context;
import android.support.design.internal.SnackbarContentLayout;
import android.support.design.snackbar.ContentViewCallback;
import android.support.design.widget.BaseTransientBottomBar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class Snackbar extends BaseTransientBottomBar<Snackbar> {
    private Snackbar(ViewGroup parent, View content, ContentViewCallback contentViewCallback) {
        super(parent, content, contentViewCallback);
    }

    @Override
    public void show() {
        super.show();
    }

    public static Snackbar make(View view, CharSequence text, int duration) {
        ViewGroup parent = findSuitableParent(view);
        if (parent == null) {
            throw new IllegalArgumentException("No suitable parent found from the given view. Please provide a valid view.");
        }
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        SnackbarContentLayout content = (SnackbarContentLayout) inflater.inflate(R.layout.design_layout_snackbar_include, parent, false);
        Snackbar snackbar = new Snackbar(parent, content, content);
        snackbar.setText(text);
        snackbar.setDuration(duration);
        return snackbar;
    }

    public static Snackbar make(View view, int resId, int duration) {
        return make(view, view.getResources().getText(resId), duration);
    }

    private static ViewGroup findSuitableParent(View view) {
        View view2 = view;
        ViewGroup fallback = null;
        while (!(view2 instanceof CoordinatorLayout)) {
            if (view2 instanceof FrameLayout) {
                if (view2.getId() == 16908290) {
                    return (ViewGroup) view2;
                }
                fallback = (ViewGroup) view2;
            }
            if (view2 != null) {
                Object parent = view2.getParent();
                view2 = parent instanceof View ? (View) parent : null;
            }
            if (view2 == null) {
                return fallback;
            }
        }
        return (ViewGroup) view2;
    }

    public Snackbar setText(CharSequence message) {
        SnackbarContentLayout contentLayout = (SnackbarContentLayout) this.view.getChildAt(0);
        TextView tv = contentLayout.getMessageView();
        tv.setText(message);
        return this;
    }

    public static final class SnackbarLayout extends BaseTransientBottomBar.SnackbarBaseLayout {
        public SnackbarLayout(Context context) {
            super(context);
        }

        public SnackbarLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int childCount = getChildCount();
            int availableWidth = (getMeasuredWidth() - getPaddingLeft()) - getPaddingRight();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child.getLayoutParams().width == -1) {
                    child.measure(View.MeasureSpec.makeMeasureSpec(availableWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(child.getMeasuredHeight(), 1073741824));
                }
            }
        }
    }
}

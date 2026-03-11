package com.android.launcher3.keyboard;

import android.graphics.Rect;
import android.view.View;
import com.android.launcher3.PagedView;

public class ViewGroupFocusHelper extends FocusIndicatorHelper {
    private final View mContainer;

    public ViewGroupFocusHelper(View view) {
        super(view);
        this.mContainer = view;
    }

    @Override
    public void viewToRect(View view, Rect rect) {
        rect.left = 0;
        rect.top = 0;
        computeLocationRelativeToContainer(view, rect);
        rect.left = (int) (rect.left + (((1.0f - view.getScaleX()) * view.getWidth()) / 2.0f));
        rect.top = (int) (rect.top + (((1.0f - view.getScaleY()) * view.getHeight()) / 2.0f));
        rect.right = rect.left + ((int) (view.getScaleX() * view.getWidth()));
        rect.bottom = rect.top + ((int) (view.getScaleY() * view.getHeight()));
    }

    private void computeLocationRelativeToContainer(View view, Rect rect) {
        View view2 = (View) view.getParent();
        rect.left = (int) (rect.left + view.getX());
        rect.top = (int) (rect.top + view.getY());
        if (view2 != this.mContainer) {
            if (view2 instanceof PagedView) {
                PagedView pagedView = (PagedView) view2;
                rect.left -= pagedView.getScrollForPage(pagedView.indexOfChild(view));
            }
            computeLocationRelativeToContainer(view2, rect);
        }
    }
}

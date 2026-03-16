package com.android.camera.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

public class TopRightWeightedLayout extends LinearLayout {
    public TopRightWeightedLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        Configuration configuration = getContext().getResources().getConfiguration();
        checkOrientation(configuration.orientation);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        checkOrientation(configuration.orientation);
    }

    private void checkOrientation(int orientation) {
        boolean isHorizontal = getOrientation() == 0;
        boolean isPortrait = 1 == orientation;
        if (isPortrait && !isHorizontal) {
            fixGravityAndPadding(0);
            setOrientation(0);
            reverseChildren();
            requestLayout();
            return;
        }
        if (!isPortrait && isHorizontal) {
            fixGravityAndPadding(1);
            setOrientation(1);
            reverseChildren();
            requestLayout();
        }
    }

    private void reverseChildren() {
        List<View> children = new ArrayList<>();
        for (int i = getChildCount() - 1; i >= 0; i--) {
            children.add(getChildAt(i));
        }
        for (View v : children) {
            bringChildToFront(v);
        }
    }

    private void fixGravityAndPadding(int direction) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) v.getLayoutParams();
            int gravity = layoutParams.gravity;
            if (direction == 1) {
                if ((gravity & 3) != 0) {
                    gravity = (gravity & (-4)) | 80;
                }
            } else if ((gravity & 80) != 0) {
                gravity = (gravity & (-81)) | 3;
            }
            if (direction == 1) {
                if ((gravity & 5) != 0) {
                    gravity = (gravity & (-6)) | 48;
                }
            } else if ((gravity & 48) != 0) {
                gravity = (gravity & (-49)) | 5;
            }
            if ((gravity & 17) != 17) {
                if (direction == 1) {
                    if ((gravity & 16) != 0) {
                        gravity = (gravity & (-17)) | 1;
                    }
                } else if ((gravity & 1) != 0) {
                    gravity = (gravity & (-2)) | 16;
                }
            }
            layoutParams.gravity = gravity;
            int paddingLeft = v.getPaddingLeft();
            int paddingTop = v.getPaddingTop();
            int paddingRight = v.getPaddingRight();
            int paddingBottom = v.getPaddingBottom();
            v.setPadding(paddingBottom, paddingRight, paddingTop, paddingLeft);
        }
    }
}

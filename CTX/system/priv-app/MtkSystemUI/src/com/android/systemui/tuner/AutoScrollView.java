package com.android.systemui.tuner;

import android.content.Context;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.widget.ScrollView;
/* loaded from: classes.dex */
public class AutoScrollView extends ScrollView {
    public AutoScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.view.View
    public boolean onDragEvent(DragEvent dragEvent) {
        if (dragEvent.getAction() == 2) {
            int y = (int) dragEvent.getY();
            int height = getHeight();
            int i = (int) (height * 0.1f);
            if (y < i) {
                scrollBy(0, y - i);
            } else if (y > height - i) {
                scrollBy(0, (y - height) + i);
            }
        }
        return false;
    }
}

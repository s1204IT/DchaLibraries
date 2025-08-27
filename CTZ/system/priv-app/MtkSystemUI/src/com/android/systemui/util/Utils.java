package com.android.systemui.util;

import android.view.View;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.CommandQueue;
import java.util.List;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class Utils {
    public static <T> void safeForeach(List<T> list, Consumer<T> consumer) {
        for (int size = list.size() - 1; size >= 0; size--) {
            consumer.accept(list.get(size));
        }
    }

    public static class DisableStateTracker implements View.OnAttachStateChangeListener, CommandQueue.Callbacks {
        private boolean mDisabled;
        private final int mMask1;
        private final int mMask2;
        private View mView;

        public DisableStateTracker(int i, int i2) {
            this.mMask1 = i;
            this.mMask2 = i2;
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewAttachedToWindow(View view) {
            this.mView = view;
            ((CommandQueue) SysUiServiceProvider.getComponent(view.getContext(), CommandQueue.class)).addCallbacks(this);
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewDetachedFromWindow(View view) {
            ((CommandQueue) SysUiServiceProvider.getComponent(this.mView.getContext(), CommandQueue.class)).removeCallbacks(this);
            this.mView = null;
        }

        @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
        public void disable(int i, int i2, boolean z) {
            boolean z2;
            if ((i & this.mMask1) != 0 || (this.mMask2 & i2) != 0) {
                z2 = true;
            } else {
                z2 = false;
            }
            if (z2 == this.mDisabled) {
                return;
            }
            this.mDisabled = z2;
            this.mView.setVisibility(z2 ? 8 : 0);
        }
    }
}

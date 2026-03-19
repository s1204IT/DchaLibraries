package android.view.accessibility;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

public abstract class AccessibilityRequestPreparer {
    public static final int REQUEST_TYPE_EXTRA_DATA = 1;
    private final int mRequestTypes;
    private final WeakReference<View> mViewRef;

    @Retention(RetentionPolicy.SOURCE)
    public @interface RequestTypes {
    }

    public abstract void onPrepareExtraData(int i, String str, Bundle bundle, Message message);

    public AccessibilityRequestPreparer(View view, int i) {
        if (!view.isAttachedToWindow()) {
            throw new IllegalStateException("View must be attached to a window");
        }
        this.mViewRef = new WeakReference<>(view);
        this.mRequestTypes = i;
        view.addOnAttachStateChangeListener(new ViewAttachStateListener());
    }

    public View getView() {
        return this.mViewRef.get();
    }

    private class ViewAttachStateListener implements View.OnAttachStateChangeListener {
        private ViewAttachStateListener() {
        }

        @Override
        public void onViewAttachedToWindow(View view) {
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            Context context = view.getContext();
            if (context != null) {
                ((AccessibilityManager) context.getSystemService(AccessibilityManager.class)).removeAccessibilityRequestPreparer(AccessibilityRequestPreparer.this);
            }
            view.removeOnAttachStateChangeListener(this);
        }
    }
}

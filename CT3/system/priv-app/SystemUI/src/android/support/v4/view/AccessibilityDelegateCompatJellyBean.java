package android.support.v4.view;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
/* loaded from: a.zip:android/support/v4/view/AccessibilityDelegateCompatJellyBean.class */
class AccessibilityDelegateCompatJellyBean {

    /* loaded from: a.zip:android/support/v4/view/AccessibilityDelegateCompatJellyBean$AccessibilityDelegateBridgeJellyBean.class */
    public interface AccessibilityDelegateBridgeJellyBean {
        boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent);

        Object getAccessibilityNodeProvider(View view);

        void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent);

        void onInitializeAccessibilityNodeInfo(View view, Object obj);

        void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent);

        boolean onRequestSendAccessibilityEvent(ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent);

        boolean performAccessibilityAction(View view, int i, Bundle bundle);

        void sendAccessibilityEvent(View view, int i);

        void sendAccessibilityEventUnchecked(View view, AccessibilityEvent accessibilityEvent);
    }

    AccessibilityDelegateCompatJellyBean() {
    }

    public static Object getAccessibilityNodeProvider(Object obj, View view) {
        return ((View.AccessibilityDelegate) obj).getAccessibilityNodeProvider(view);
    }

    public static Object newAccessibilityDelegateBridge(AccessibilityDelegateBridgeJellyBean accessibilityDelegateBridgeJellyBean) {
        return new View.AccessibilityDelegate(accessibilityDelegateBridgeJellyBean) { // from class: android.support.v4.view.AccessibilityDelegateCompatJellyBean.1
            final AccessibilityDelegateBridgeJellyBean val$bridge;

            {
                this.val$bridge = accessibilityDelegateBridgeJellyBean;
            }

            @Override // android.view.View.AccessibilityDelegate
            public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                return this.val$bridge.dispatchPopulateAccessibilityEvent(view, accessibilityEvent);
            }

            @Override // android.view.View.AccessibilityDelegate
            public AccessibilityNodeProvider getAccessibilityNodeProvider(View view) {
                return (AccessibilityNodeProvider) this.val$bridge.getAccessibilityNodeProvider(view);
            }

            @Override // android.view.View.AccessibilityDelegate
            public void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                this.val$bridge.onInitializeAccessibilityEvent(view, accessibilityEvent);
            }

            @Override // android.view.View.AccessibilityDelegate
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                this.val$bridge.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
            }

            @Override // android.view.View.AccessibilityDelegate
            public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                this.val$bridge.onPopulateAccessibilityEvent(view, accessibilityEvent);
            }

            @Override // android.view.View.AccessibilityDelegate
            public boolean onRequestSendAccessibilityEvent(ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
                return this.val$bridge.onRequestSendAccessibilityEvent(viewGroup, view, accessibilityEvent);
            }

            @Override // android.view.View.AccessibilityDelegate
            public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
                return this.val$bridge.performAccessibilityAction(view, i, bundle);
            }

            @Override // android.view.View.AccessibilityDelegate
            public void sendAccessibilityEvent(View view, int i) {
                this.val$bridge.sendAccessibilityEvent(view, i);
            }

            @Override // android.view.View.AccessibilityDelegate
            public void sendAccessibilityEventUnchecked(View view, AccessibilityEvent accessibilityEvent) {
                this.val$bridge.sendAccessibilityEventUnchecked(view, accessibilityEvent);
            }
        };
    }

    public static boolean performAccessibilityAction(Object obj, View view, int i, Bundle bundle) {
        return ((View.AccessibilityDelegate) obj).performAccessibilityAction(view, i, bundle);
    }
}

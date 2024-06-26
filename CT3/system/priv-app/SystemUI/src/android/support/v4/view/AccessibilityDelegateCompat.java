package android.support.v4.view;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.AccessibilityDelegateCompatIcs;
import android.support.v4.view.AccessibilityDelegateCompatJellyBean;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeProviderCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
/* loaded from: a.zip:android/support/v4/view/AccessibilityDelegateCompat.class */
public class AccessibilityDelegateCompat {
    private static final Object DEFAULT_DELEGATE;
    private static final AccessibilityDelegateImpl IMPL;
    final Object mBridge = IMPL.newAccessiblityDelegateBridge(this);

    /* loaded from: a.zip:android/support/v4/view/AccessibilityDelegateCompat$AccessibilityDelegateIcsImpl.class */
    static class AccessibilityDelegateIcsImpl extends AccessibilityDelegateStubImpl {
        AccessibilityDelegateIcsImpl() {
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public boolean dispatchPopulateAccessibilityEvent(Object obj, View view, AccessibilityEvent accessibilityEvent) {
            return AccessibilityDelegateCompatIcs.dispatchPopulateAccessibilityEvent(obj, view, accessibilityEvent);
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public Object newAccessiblityDelegateBridge(AccessibilityDelegateCompat accessibilityDelegateCompat) {
            return AccessibilityDelegateCompatIcs.newAccessibilityDelegateBridge(new AccessibilityDelegateCompatIcs.AccessibilityDelegateBridge(this, accessibilityDelegateCompat) { // from class: android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateIcsImpl.1
                final AccessibilityDelegateIcsImpl this$1;
                final AccessibilityDelegateCompat val$compat;

                {
                    this.this$1 = this;
                    this.val$compat = accessibilityDelegateCompat;
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatIcs.AccessibilityDelegateBridge
                public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                    return this.val$compat.dispatchPopulateAccessibilityEvent(view, accessibilityEvent);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatIcs.AccessibilityDelegateBridge
                public void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                    this.val$compat.onInitializeAccessibilityEvent(view, accessibilityEvent);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatIcs.AccessibilityDelegateBridge
                public void onInitializeAccessibilityNodeInfo(View view, Object obj) {
                    this.val$compat.onInitializeAccessibilityNodeInfo(view, new AccessibilityNodeInfoCompat(obj));
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatIcs.AccessibilityDelegateBridge
                public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                    this.val$compat.onPopulateAccessibilityEvent(view, accessibilityEvent);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatIcs.AccessibilityDelegateBridge
                public boolean onRequestSendAccessibilityEvent(ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
                    return this.val$compat.onRequestSendAccessibilityEvent(viewGroup, view, accessibilityEvent);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatIcs.AccessibilityDelegateBridge
                public void sendAccessibilityEvent(View view, int i) {
                    this.val$compat.sendAccessibilityEvent(view, i);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatIcs.AccessibilityDelegateBridge
                public void sendAccessibilityEventUnchecked(View view, AccessibilityEvent accessibilityEvent) {
                    this.val$compat.sendAccessibilityEventUnchecked(view, accessibilityEvent);
                }
            });
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public Object newAccessiblityDelegateDefaultImpl() {
            return AccessibilityDelegateCompatIcs.newAccessibilityDelegateDefaultImpl();
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public void onInitializeAccessibilityEvent(Object obj, View view, AccessibilityEvent accessibilityEvent) {
            AccessibilityDelegateCompatIcs.onInitializeAccessibilityEvent(obj, view, accessibilityEvent);
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public void onInitializeAccessibilityNodeInfo(Object obj, View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
            AccessibilityDelegateCompatIcs.onInitializeAccessibilityNodeInfo(obj, view, accessibilityNodeInfoCompat.getInfo());
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public void onPopulateAccessibilityEvent(Object obj, View view, AccessibilityEvent accessibilityEvent) {
            AccessibilityDelegateCompatIcs.onPopulateAccessibilityEvent(obj, view, accessibilityEvent);
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public boolean onRequestSendAccessibilityEvent(Object obj, ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
            return AccessibilityDelegateCompatIcs.onRequestSendAccessibilityEvent(obj, viewGroup, view, accessibilityEvent);
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public void sendAccessibilityEvent(Object obj, View view, int i) {
            AccessibilityDelegateCompatIcs.sendAccessibilityEvent(obj, view, i);
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public void sendAccessibilityEventUnchecked(Object obj, View view, AccessibilityEvent accessibilityEvent) {
            AccessibilityDelegateCompatIcs.sendAccessibilityEventUnchecked(obj, view, accessibilityEvent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: a.zip:android/support/v4/view/AccessibilityDelegateCompat$AccessibilityDelegateImpl.class */
    public interface AccessibilityDelegateImpl {
        boolean dispatchPopulateAccessibilityEvent(Object obj, View view, AccessibilityEvent accessibilityEvent);

        AccessibilityNodeProviderCompat getAccessibilityNodeProvider(Object obj, View view);

        Object newAccessiblityDelegateBridge(AccessibilityDelegateCompat accessibilityDelegateCompat);

        Object newAccessiblityDelegateDefaultImpl();

        void onInitializeAccessibilityEvent(Object obj, View view, AccessibilityEvent accessibilityEvent);

        void onInitializeAccessibilityNodeInfo(Object obj, View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat);

        void onPopulateAccessibilityEvent(Object obj, View view, AccessibilityEvent accessibilityEvent);

        boolean onRequestSendAccessibilityEvent(Object obj, ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent);

        boolean performAccessibilityAction(Object obj, View view, int i, Bundle bundle);

        void sendAccessibilityEvent(Object obj, View view, int i);

        void sendAccessibilityEventUnchecked(Object obj, View view, AccessibilityEvent accessibilityEvent);
    }

    /* loaded from: a.zip:android/support/v4/view/AccessibilityDelegateCompat$AccessibilityDelegateJellyBeanImpl.class */
    static class AccessibilityDelegateJellyBeanImpl extends AccessibilityDelegateIcsImpl {
        AccessibilityDelegateJellyBeanImpl() {
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(Object obj, View view) {
            Object accessibilityNodeProvider = AccessibilityDelegateCompatJellyBean.getAccessibilityNodeProvider(obj, view);
            if (accessibilityNodeProvider != null) {
                return new AccessibilityNodeProviderCompat(accessibilityNodeProvider);
            }
            return null;
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateIcsImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public Object newAccessiblityDelegateBridge(AccessibilityDelegateCompat accessibilityDelegateCompat) {
            return AccessibilityDelegateCompatJellyBean.newAccessibilityDelegateBridge(new AccessibilityDelegateCompatJellyBean.AccessibilityDelegateBridgeJellyBean(this, accessibilityDelegateCompat) { // from class: android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateJellyBeanImpl.1
                final AccessibilityDelegateJellyBeanImpl this$1;
                final AccessibilityDelegateCompat val$compat;

                {
                    this.this$1 = this;
                    this.val$compat = accessibilityDelegateCompat;
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatJellyBean.AccessibilityDelegateBridgeJellyBean
                public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                    return this.val$compat.dispatchPopulateAccessibilityEvent(view, accessibilityEvent);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatJellyBean.AccessibilityDelegateBridgeJellyBean
                public Object getAccessibilityNodeProvider(View view) {
                    AccessibilityNodeProviderCompat accessibilityNodeProvider = this.val$compat.getAccessibilityNodeProvider(view);
                    Object obj = null;
                    if (accessibilityNodeProvider != null) {
                        obj = accessibilityNodeProvider.getProvider();
                    }
                    return obj;
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatJellyBean.AccessibilityDelegateBridgeJellyBean
                public void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                    this.val$compat.onInitializeAccessibilityEvent(view, accessibilityEvent);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatJellyBean.AccessibilityDelegateBridgeJellyBean
                public void onInitializeAccessibilityNodeInfo(View view, Object obj) {
                    this.val$compat.onInitializeAccessibilityNodeInfo(view, new AccessibilityNodeInfoCompat(obj));
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatJellyBean.AccessibilityDelegateBridgeJellyBean
                public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                    this.val$compat.onPopulateAccessibilityEvent(view, accessibilityEvent);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatJellyBean.AccessibilityDelegateBridgeJellyBean
                public boolean onRequestSendAccessibilityEvent(ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
                    return this.val$compat.onRequestSendAccessibilityEvent(viewGroup, view, accessibilityEvent);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatJellyBean.AccessibilityDelegateBridgeJellyBean
                public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
                    return this.val$compat.performAccessibilityAction(view, i, bundle);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatJellyBean.AccessibilityDelegateBridgeJellyBean
                public void sendAccessibilityEvent(View view, int i) {
                    this.val$compat.sendAccessibilityEvent(view, i);
                }

                @Override // android.support.v4.view.AccessibilityDelegateCompatJellyBean.AccessibilityDelegateBridgeJellyBean
                public void sendAccessibilityEventUnchecked(View view, AccessibilityEvent accessibilityEvent) {
                    this.val$compat.sendAccessibilityEventUnchecked(view, accessibilityEvent);
                }
            });
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateStubImpl, android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public boolean performAccessibilityAction(Object obj, View view, int i, Bundle bundle) {
            return AccessibilityDelegateCompatJellyBean.performAccessibilityAction(obj, view, i, bundle);
        }
    }

    /* loaded from: a.zip:android/support/v4/view/AccessibilityDelegateCompat$AccessibilityDelegateStubImpl.class */
    static class AccessibilityDelegateStubImpl implements AccessibilityDelegateImpl {
        AccessibilityDelegateStubImpl() {
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public boolean dispatchPopulateAccessibilityEvent(Object obj, View view, AccessibilityEvent accessibilityEvent) {
            return false;
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(Object obj, View view) {
            return null;
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public Object newAccessiblityDelegateBridge(AccessibilityDelegateCompat accessibilityDelegateCompat) {
            return null;
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public Object newAccessiblityDelegateDefaultImpl() {
            return null;
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public void onInitializeAccessibilityEvent(Object obj, View view, AccessibilityEvent accessibilityEvent) {
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public void onInitializeAccessibilityNodeInfo(Object obj, View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public void onPopulateAccessibilityEvent(Object obj, View view, AccessibilityEvent accessibilityEvent) {
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public boolean onRequestSendAccessibilityEvent(Object obj, ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
            return true;
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public boolean performAccessibilityAction(Object obj, View view, int i, Bundle bundle) {
            return false;
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public void sendAccessibilityEvent(Object obj, View view, int i) {
        }

        @Override // android.support.v4.view.AccessibilityDelegateCompat.AccessibilityDelegateImpl
        public void sendAccessibilityEventUnchecked(Object obj, View view, AccessibilityEvent accessibilityEvent) {
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 16) {
            IMPL = new AccessibilityDelegateJellyBeanImpl();
        } else if (Build.VERSION.SDK_INT >= 14) {
            IMPL = new AccessibilityDelegateIcsImpl();
        } else {
            IMPL = new AccessibilityDelegateStubImpl();
        }
        DEFAULT_DELEGATE = IMPL.newAccessiblityDelegateDefaultImpl();
    }

    public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        return IMPL.dispatchPopulateAccessibilityEvent(DEFAULT_DELEGATE, view, accessibilityEvent);
    }

    public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View view) {
        return IMPL.getAccessibilityNodeProvider(DEFAULT_DELEGATE, view);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Object getBridge() {
        return this.mBridge;
    }

    public void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        IMPL.onInitializeAccessibilityEvent(DEFAULT_DELEGATE, view, accessibilityEvent);
    }

    public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
        IMPL.onInitializeAccessibilityNodeInfo(DEFAULT_DELEGATE, view, accessibilityNodeInfoCompat);
    }

    public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        IMPL.onPopulateAccessibilityEvent(DEFAULT_DELEGATE, view, accessibilityEvent);
    }

    public boolean onRequestSendAccessibilityEvent(ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
        return IMPL.onRequestSendAccessibilityEvent(DEFAULT_DELEGATE, viewGroup, view, accessibilityEvent);
    }

    public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
        return IMPL.performAccessibilityAction(DEFAULT_DELEGATE, view, i, bundle);
    }

    public void sendAccessibilityEvent(View view, int i) {
        IMPL.sendAccessibilityEvent(DEFAULT_DELEGATE, view, i);
    }

    public void sendAccessibilityEventUnchecked(View view, AccessibilityEvent accessibilityEvent) {
        IMPL.sendAccessibilityEventUnchecked(DEFAULT_DELEGATE, view, accessibilityEvent);
    }
}

package android.support.v4.view;

import android.os.Build;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
/* loaded from: a.zip:android/support/v4/view/ViewParentCompat.class */
public final class ViewParentCompat {
    static final ViewParentCompatImpl IMPL;

    /* loaded from: a.zip:android/support/v4/view/ViewParentCompat$ViewParentCompatICSImpl.class */
    static class ViewParentCompatICSImpl extends ViewParentCompatStubImpl {
        ViewParentCompatICSImpl() {
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatStubImpl, android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public boolean requestSendAccessibilityEvent(ViewParent viewParent, View view, AccessibilityEvent accessibilityEvent) {
            return ViewParentCompatICS.requestSendAccessibilityEvent(viewParent, view, accessibilityEvent);
        }
    }

    /* loaded from: a.zip:android/support/v4/view/ViewParentCompat$ViewParentCompatImpl.class */
    interface ViewParentCompatImpl {
        boolean onNestedFling(ViewParent viewParent, View view, float f, float f2, boolean z);

        boolean onNestedPreFling(ViewParent viewParent, View view, float f, float f2);

        void onNestedPreScroll(ViewParent viewParent, View view, int i, int i2, int[] iArr);

        void onNestedScroll(ViewParent viewParent, View view, int i, int i2, int i3, int i4);

        void onNestedScrollAccepted(ViewParent viewParent, View view, View view2, int i);

        boolean onStartNestedScroll(ViewParent viewParent, View view, View view2, int i);

        void onStopNestedScroll(ViewParent viewParent, View view);

        boolean requestSendAccessibilityEvent(ViewParent viewParent, View view, AccessibilityEvent accessibilityEvent);
    }

    /* loaded from: a.zip:android/support/v4/view/ViewParentCompat$ViewParentCompatKitKatImpl.class */
    static class ViewParentCompatKitKatImpl extends ViewParentCompatICSImpl {
        ViewParentCompatKitKatImpl() {
        }
    }

    /* loaded from: a.zip:android/support/v4/view/ViewParentCompat$ViewParentCompatLollipopImpl.class */
    static class ViewParentCompatLollipopImpl extends ViewParentCompatKitKatImpl {
        ViewParentCompatLollipopImpl() {
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatStubImpl, android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public boolean onNestedFling(ViewParent viewParent, View view, float f, float f2, boolean z) {
            return ViewParentCompatLollipop.onNestedFling(viewParent, view, f, f2, z);
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatStubImpl, android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public boolean onNestedPreFling(ViewParent viewParent, View view, float f, float f2) {
            return ViewParentCompatLollipop.onNestedPreFling(viewParent, view, f, f2);
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatStubImpl, android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public void onNestedPreScroll(ViewParent viewParent, View view, int i, int i2, int[] iArr) {
            ViewParentCompatLollipop.onNestedPreScroll(viewParent, view, i, i2, iArr);
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatStubImpl, android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public void onNestedScroll(ViewParent viewParent, View view, int i, int i2, int i3, int i4) {
            ViewParentCompatLollipop.onNestedScroll(viewParent, view, i, i2, i3, i4);
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatStubImpl, android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public void onNestedScrollAccepted(ViewParent viewParent, View view, View view2, int i) {
            ViewParentCompatLollipop.onNestedScrollAccepted(viewParent, view, view2, i);
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatStubImpl, android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public boolean onStartNestedScroll(ViewParent viewParent, View view, View view2, int i) {
            return ViewParentCompatLollipop.onStartNestedScroll(viewParent, view, view2, i);
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatStubImpl, android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public void onStopNestedScroll(ViewParent viewParent, View view) {
            ViewParentCompatLollipop.onStopNestedScroll(viewParent, view);
        }
    }

    /* loaded from: a.zip:android/support/v4/view/ViewParentCompat$ViewParentCompatStubImpl.class */
    static class ViewParentCompatStubImpl implements ViewParentCompatImpl {
        ViewParentCompatStubImpl() {
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public boolean onNestedFling(ViewParent viewParent, View view, float f, float f2, boolean z) {
            if (viewParent instanceof NestedScrollingParent) {
                return ((NestedScrollingParent) viewParent).onNestedFling(view, f, f2, z);
            }
            return false;
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public boolean onNestedPreFling(ViewParent viewParent, View view, float f, float f2) {
            if (viewParent instanceof NestedScrollingParent) {
                return ((NestedScrollingParent) viewParent).onNestedPreFling(view, f, f2);
            }
            return false;
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public void onNestedPreScroll(ViewParent viewParent, View view, int i, int i2, int[] iArr) {
            if (viewParent instanceof NestedScrollingParent) {
                ((NestedScrollingParent) viewParent).onNestedPreScroll(view, i, i2, iArr);
            }
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public void onNestedScroll(ViewParent viewParent, View view, int i, int i2, int i3, int i4) {
            if (viewParent instanceof NestedScrollingParent) {
                ((NestedScrollingParent) viewParent).onNestedScroll(view, i, i2, i3, i4);
            }
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public void onNestedScrollAccepted(ViewParent viewParent, View view, View view2, int i) {
            if (viewParent instanceof NestedScrollingParent) {
                ((NestedScrollingParent) viewParent).onNestedScrollAccepted(view, view2, i);
            }
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public boolean onStartNestedScroll(ViewParent viewParent, View view, View view2, int i) {
            if (viewParent instanceof NestedScrollingParent) {
                return ((NestedScrollingParent) viewParent).onStartNestedScroll(view, view2, i);
            }
            return false;
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public void onStopNestedScroll(ViewParent viewParent, View view) {
            if (viewParent instanceof NestedScrollingParent) {
                ((NestedScrollingParent) viewParent).onStopNestedScroll(view);
            }
        }

        @Override // android.support.v4.view.ViewParentCompat.ViewParentCompatImpl
        public boolean requestSendAccessibilityEvent(ViewParent viewParent, View view, AccessibilityEvent accessibilityEvent) {
            if (view == null) {
                return false;
            }
            ((AccessibilityManager) view.getContext().getSystemService("accessibility")).sendAccessibilityEvent(accessibilityEvent);
            return true;
        }
    }

    static {
        int i = Build.VERSION.SDK_INT;
        if (i >= 21) {
            IMPL = new ViewParentCompatLollipopImpl();
        } else if (i >= 19) {
            IMPL = new ViewParentCompatKitKatImpl();
        } else if (i >= 14) {
            IMPL = new ViewParentCompatICSImpl();
        } else {
            IMPL = new ViewParentCompatStubImpl();
        }
    }

    private ViewParentCompat() {
    }

    public static boolean onNestedFling(ViewParent viewParent, View view, float f, float f2, boolean z) {
        return IMPL.onNestedFling(viewParent, view, f, f2, z);
    }

    public static boolean onNestedPreFling(ViewParent viewParent, View view, float f, float f2) {
        return IMPL.onNestedPreFling(viewParent, view, f, f2);
    }

    public static void onNestedPreScroll(ViewParent viewParent, View view, int i, int i2, int[] iArr) {
        IMPL.onNestedPreScroll(viewParent, view, i, i2, iArr);
    }

    public static void onNestedScroll(ViewParent viewParent, View view, int i, int i2, int i3, int i4) {
        IMPL.onNestedScroll(viewParent, view, i, i2, i3, i4);
    }

    public static void onNestedScrollAccepted(ViewParent viewParent, View view, View view2, int i) {
        IMPL.onNestedScrollAccepted(viewParent, view, view2, i);
    }

    public static boolean onStartNestedScroll(ViewParent viewParent, View view, View view2, int i) {
        return IMPL.onStartNestedScroll(viewParent, view, view2, i);
    }

    public static void onStopNestedScroll(ViewParent viewParent, View view) {
        IMPL.onStopNestedScroll(viewParent, view);
    }

    public static boolean requestSendAccessibilityEvent(ViewParent viewParent, View view, AccessibilityEvent accessibilityEvent) {
        return IMPL.requestSendAccessibilityEvent(viewParent, view, accessibilityEvent);
    }
}

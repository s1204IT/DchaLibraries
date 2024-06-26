package android.support.v13.app;

import android.app.Fragment;
import android.os.Build;
import android.support.v4.os.BuildCompat;
/* loaded from: classes.dex */
public class FragmentCompat {
    static final FragmentCompatImpl IMPL;

    /* loaded from: classes.dex */
    interface FragmentCompatImpl {
        void setMenuVisibility(Fragment fragment, boolean z);

        void setUserVisibleHint(Fragment fragment, boolean z);
    }

    /* loaded from: classes.dex */
    static class BaseFragmentCompatImpl implements FragmentCompatImpl {
        BaseFragmentCompatImpl() {
        }

        @Override // android.support.v13.app.FragmentCompat.FragmentCompatImpl
        public void setMenuVisibility(Fragment f, boolean visible) {
        }

        @Override // android.support.v13.app.FragmentCompat.FragmentCompatImpl
        public void setUserVisibleHint(Fragment f, boolean deferStart) {
        }
    }

    /* loaded from: classes.dex */
    static class ICSFragmentCompatImpl extends BaseFragmentCompatImpl {
        ICSFragmentCompatImpl() {
        }

        @Override // android.support.v13.app.FragmentCompat.BaseFragmentCompatImpl, android.support.v13.app.FragmentCompat.FragmentCompatImpl
        public void setMenuVisibility(Fragment f, boolean visible) {
            FragmentCompatICS.setMenuVisibility(f, visible);
        }
    }

    /* loaded from: classes.dex */
    static class ICSMR1FragmentCompatImpl extends ICSFragmentCompatImpl {
        ICSMR1FragmentCompatImpl() {
        }

        @Override // android.support.v13.app.FragmentCompat.BaseFragmentCompatImpl, android.support.v13.app.FragmentCompat.FragmentCompatImpl
        public void setUserVisibleHint(Fragment f, boolean deferStart) {
            FragmentCompatICSMR1.setUserVisibleHint(f, deferStart);
        }
    }

    /* loaded from: classes.dex */
    static class MncFragmentCompatImpl extends ICSMR1FragmentCompatImpl {
        MncFragmentCompatImpl() {
        }
    }

    /* loaded from: classes.dex */
    static class NFragmentCompatImpl extends MncFragmentCompatImpl {
        NFragmentCompatImpl() {
        }

        @Override // android.support.v13.app.FragmentCompat.ICSMR1FragmentCompatImpl, android.support.v13.app.FragmentCompat.BaseFragmentCompatImpl, android.support.v13.app.FragmentCompat.FragmentCompatImpl
        public void setUserVisibleHint(Fragment f, boolean deferStart) {
            FragmentCompatApi24.setUserVisibleHint(f, deferStart);
        }
    }

    static {
        if (BuildCompat.isAtLeastN()) {
            IMPL = new NFragmentCompatImpl();
        } else if (Build.VERSION.SDK_INT >= 23) {
            IMPL = new MncFragmentCompatImpl();
        } else if (Build.VERSION.SDK_INT >= 15) {
            IMPL = new ICSMR1FragmentCompatImpl();
        } else if (Build.VERSION.SDK_INT >= 14) {
            IMPL = new ICSFragmentCompatImpl();
        } else {
            IMPL = new BaseFragmentCompatImpl();
        }
    }

    public static void setMenuVisibility(Fragment f, boolean visible) {
        IMPL.setMenuVisibility(f, visible);
    }

    public static void setUserVisibleHint(Fragment f, boolean deferStart) {
        IMPL.setUserVisibleHint(f, deferStart);
    }
}

package android.support.v13.app;

import android.app.Fragment;
import android.os.Build;

@Deprecated
public class FragmentCompat {
    static final FragmentCompatImpl IMPL;

    interface FragmentCompatImpl {
        void setUserVisibleHint(Fragment fragment, boolean z);
    }

    static class FragmentCompatBaseImpl implements FragmentCompatImpl {
        FragmentCompatBaseImpl() {
        }

        @Override
        public void setUserVisibleHint(Fragment f, boolean deferStart) {
        }
    }

    static class FragmentCompatApi15Impl extends FragmentCompatBaseImpl {
        FragmentCompatApi15Impl() {
        }

        @Override
        public void setUserVisibleHint(Fragment f, boolean deferStart) {
            f.setUserVisibleHint(deferStart);
        }
    }

    static class FragmentCompatApi23Impl extends FragmentCompatApi15Impl {
        FragmentCompatApi23Impl() {
        }
    }

    static class FragmentCompatApi24Impl extends FragmentCompatApi23Impl {
        FragmentCompatApi24Impl() {
        }

        @Override
        public void setUserVisibleHint(Fragment f, boolean deferStart) {
            f.setUserVisibleHint(deferStart);
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 24) {
            IMPL = new FragmentCompatApi24Impl();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            IMPL = new FragmentCompatApi23Impl();
        } else if (Build.VERSION.SDK_INT >= 15) {
            IMPL = new FragmentCompatApi15Impl();
        } else {
            IMPL = new FragmentCompatBaseImpl();
        }
    }

    @Deprecated
    public static void setUserVisibleHint(Fragment f, boolean deferStart) {
        IMPL.setUserVisibleHint(f, deferStart);
    }
}

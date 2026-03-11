package android.support.v17.leanback.widget;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

final class ForegroundHelper {
    static final ForegroundHelper sInstance = new ForegroundHelper();
    ForegroundHelperVersionImpl mImpl;

    interface ForegroundHelperVersionImpl {
        void setForeground(View view, Drawable drawable);
    }

    private static final class ForegroundHelperApi23Impl implements ForegroundHelperVersionImpl {
        ForegroundHelperApi23Impl(ForegroundHelperApi23Impl foregroundHelperApi23Impl) {
            this();
        }

        private ForegroundHelperApi23Impl() {
        }

        @Override
        public void setForeground(View view, Drawable drawable) {
            ForegroundHelperApi23.setForeground(view, drawable);
        }
    }

    private static final class ForegroundHelperStubImpl implements ForegroundHelperVersionImpl {
        ForegroundHelperStubImpl(ForegroundHelperStubImpl foregroundHelperStubImpl) {
            this();
        }

        private ForegroundHelperStubImpl() {
        }

        @Override
        public void setForeground(View view, Drawable drawable) {
        }
    }

    private ForegroundHelper() {
        ForegroundHelperApi23Impl foregroundHelperApi23Impl = null;
        Object[] objArr = 0;
        if (supportsForeground()) {
            this.mImpl = new ForegroundHelperApi23Impl(foregroundHelperApi23Impl);
        } else {
            this.mImpl = new ForegroundHelperStubImpl(objArr == true ? 1 : 0);
        }
    }

    public static ForegroundHelper getInstance() {
        return sInstance;
    }

    public static boolean supportsForeground() {
        return Build.VERSION.SDK_INT >= 23;
    }

    public void setForeground(View view, Drawable drawable) {
        this.mImpl.setForeground(view, drawable);
    }
}

package android.support.v17.leanback.widget;

import android.os.Build;
import android.view.View;

final class ShadowHelper {
    static final ShadowHelper sInstance = new ShadowHelper();
    ShadowHelperVersionImpl mImpl;
    boolean mSupportsDynamicShadow;

    interface ShadowHelperVersionImpl {
        void setZ(View view, float f);
    }

    private static final class ShadowHelperStubImpl implements ShadowHelperVersionImpl {
        ShadowHelperStubImpl(ShadowHelperStubImpl shadowHelperStubImpl) {
            this();
        }

        private ShadowHelperStubImpl() {
        }

        @Override
        public void setZ(View view, float z) {
        }
    }

    private static final class ShadowHelperApi21Impl implements ShadowHelperVersionImpl {
        ShadowHelperApi21Impl(ShadowHelperApi21Impl shadowHelperApi21Impl) {
            this();
        }

        private ShadowHelperApi21Impl() {
        }

        @Override
        public void setZ(View view, float z) {
            ShadowHelperApi21.setZ(view, z);
        }
    }

    private ShadowHelper() {
        ShadowHelperApi21Impl shadowHelperApi21Impl = null;
        Object[] objArr = 0;
        if (Build.VERSION.SDK_INT >= 21) {
            this.mSupportsDynamicShadow = true;
            this.mImpl = new ShadowHelperApi21Impl(shadowHelperApi21Impl);
        } else {
            this.mImpl = new ShadowHelperStubImpl(objArr == true ? 1 : 0);
        }
    }

    public static ShadowHelper getInstance() {
        return sInstance;
    }

    public boolean supportsDynamicShadow() {
        return this.mSupportsDynamicShadow;
    }

    public void setZ(View view, float z) {
        this.mImpl.setZ(view, z);
    }
}

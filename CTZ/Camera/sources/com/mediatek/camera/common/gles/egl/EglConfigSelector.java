package com.mediatek.camera.common.gles.egl;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLDisplay;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

@TargetApi(18)
public class EglConfigSelector {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(EglConfigSelector.class.getSimpleName());
    private EGLConfigChooser mEGLConfigChooser;
    private int mSelectedPixelFormat = -1;
    private ArrayList<Integer> mSupportedOutputFormats = new ArrayList<>();

    private interface EGLConfigChooser {
        EGLConfig chooseConfigEGL14(EGLDisplay eGLDisplay, boolean z);
    }

    private enum EglConfigFormat {
        YUV,
        RGB,
        RGBA
    }

    public void setSupportedFormats(int[] iArr) {
        LogUtil.Tag tag = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("[setSupportedFormats] setSupportedFormats,format: ");
        sb.append(iArr == null ? "null" : Arrays.toString(iArr));
        LogHelper.d(tag, sb.toString());
        for (int i : iArr) {
            this.mSupportedOutputFormats.add(Integer.valueOf(i));
        }
    }

    public EGLConfig chooseConfigEGL14(EGLDisplay eGLDisplay, boolean z) {
        if (this.mEGLConfigChooser == null) {
            this.mEGLConfigChooser = new SimpleEGLConfigChooser();
        }
        if (this.mSupportedOutputFormats.size() <= 0) {
            this.mSupportedOutputFormats.add(1);
        }
        return this.mEGLConfigChooser.chooseConfigEGL14(eGLDisplay, z);
    }

    public int getSelectedPixelFormat() {
        return this.mSelectedPixelFormat;
    }

    private abstract class BaseConfigChooser implements EGLConfigChooser {
        protected int[] mConfigSpec;

        abstract EGLConfig chooseConfigEGL14(EGLDisplay eGLDisplay, EGLConfig[] eGLConfigArr, int i, boolean z);

        public BaseConfigChooser(int[] iArr) {
            this.mConfigSpec = iArr;
        }

        @Override
        public EGLConfig chooseConfigEGL14(EGLDisplay eGLDisplay, boolean z) {
            int i;
            EGLConfig[] eGLConfigArr = new EGLConfig[100];
            int[] iArr = new int[1];
            if (z) {
                this.mConfigSpec[this.mConfigSpec.length - 3] = 12610;
                this.mConfigSpec[this.mConfigSpec.length - 2] = 1;
                i = 4;
            } else {
                i = 5;
            }
            this.mConfigSpec[this.mConfigSpec.length - 5] = 12339;
            this.mConfigSpec[this.mConfigSpec.length - 4] = i;
            if (!EGL14.eglChooseConfig(eGLDisplay, this.mConfigSpec, 0, eGLConfigArr, 0, eGLConfigArr.length, iArr, 0)) {
                throw new RuntimeException("unable to find ES2 EGL config in EGL14");
            }
            EGLConfig eGLConfigChooseConfigEGL14 = chooseConfigEGL14(eGLDisplay, eGLConfigArr, iArr[0], z);
            if (eGLConfigChooseConfigEGL14 == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return eGLConfigChooseConfigEGL14;
        }
    }

    private class ComponentSizeChooser extends BaseConfigChooser {
        protected int mAlphaSize;
        protected int mBlueSize;
        protected int mDepthSize;
        protected int mGreenSize;
        protected int mRedSize;
        protected int mStencilSize;
        private int[] mValue;

        public ComponentSizeChooser(int i, int i2, int i3, int i4, int i5, int i6) {
            super(new int[]{12324, i, 12323, i2, 12322, i3, 12321, i4, 12325, i5, 12326, i6, 12352, 4, 12344, 0, 12344, 0, 12344});
            this.mValue = new int[1];
            this.mRedSize = i;
            this.mGreenSize = i2;
            this.mBlueSize = i3;
            this.mAlphaSize = i4;
            this.mDepthSize = i5;
            this.mStencilSize = i6;
            LogHelper.d(EglConfigSelector.TAG, "R:" + this.mRedSize + ",G:" + this.mGreenSize + ",B:" + this.mBlueSize + ",A:" + this.mAlphaSize + ",Depth:" + this.mDepthSize + ",Stencil:" + this.mStencilSize);
        }

        @Override
        EGLConfig chooseConfigEGL14(EGLDisplay eGLDisplay, EGLConfig[] eGLConfigArr, int i, boolean z) {
            EGLConfig eGLConfigFindClosestEglConfig;
            if (hasSpecifiedEglConfigFormat(EglConfigFormat.YUV, EglConfigSelector.this.mSupportedOutputFormats)) {
                eGLConfigFindClosestEglConfig = findClosestEglConfig(eGLDisplay, eGLConfigArr, i, EglConfigFormat.YUV);
            } else {
                eGLConfigFindClosestEglConfig = null;
            }
            if (hasSpecifiedEglConfigFormat(EglConfigFormat.RGB, EglConfigSelector.this.mSupportedOutputFormats) && eGLConfigFindClosestEglConfig == null) {
                eGLConfigFindClosestEglConfig = findClosestEglConfig(eGLDisplay, eGLConfigArr, i, EglConfigFormat.RGB);
            }
            if (hasSpecifiedEglConfigFormat(EglConfigFormat.RGBA, EglConfigSelector.this.mSupportedOutputFormats) && eGLConfigFindClosestEglConfig == null) {
                return findClosestEglConfig(eGLDisplay, eGLConfigArr, i, EglConfigFormat.RGBA);
            }
            return eGLConfigFindClosestEglConfig;
        }

        private int findConfigAttribute(EGLDisplay eGLDisplay, EGLConfig eGLConfig, int i, int i2) {
            if (EGL14.eglGetConfigAttrib(eGLDisplay, eGLConfig, i, this.mValue, 0)) {
                return this.mValue[0];
            }
            return i2;
        }

        private EGLConfig findClosestEglConfig(EGLDisplay eGLDisplay, EGLConfig[] eGLConfigArr, int i, EglConfigFormat eglConfigFormat) {
            EGLConfig eGLConfig;
            EGLDisplay eGLDisplay2 = eGLDisplay;
            int i2 = 0;
            int i3 = 1000;
            EGLConfig eGLConfig2 = null;
            int i4 = 0;
            while (i4 < i) {
                int iFindConfigAttribute = findConfigAttribute(eGLDisplay2, eGLConfigArr[i4], 12325, i2);
                int iFindConfigAttribute2 = findConfigAttribute(eGLDisplay2, eGLConfigArr[i4], 12326, i2);
                int iFindConfigAttribute3 = findConfigAttribute(eGLDisplay2, eGLConfigArr[i4], 12334, i2);
                int iFindConfigAttribute4 = findConfigAttribute(eGLDisplay2, eGLConfigArr[i4], 12339, i2);
                if (iFindConfigAttribute < this.mDepthSize || iFindConfigAttribute2 < this.mStencilSize) {
                    eGLConfig = eGLConfig2;
                } else {
                    int iFindConfigAttribute5 = findConfigAttribute(eGLDisplay2, eGLConfigArr[i4], 12324, i2);
                    int iFindConfigAttribute6 = findConfigAttribute(eGLDisplay2, eGLConfigArr[i4], 12323, i2);
                    int iFindConfigAttribute7 = findConfigAttribute(eGLDisplay2, eGLConfigArr[i4], 12322, i2);
                    eGLConfig = eGLConfig2;
                    int iFindConfigAttribute8 = findConfigAttribute(eGLDisplay2, eGLConfigArr[i4], 12321, i2);
                    int iAbs = Math.abs(iFindConfigAttribute5 - this.mRedSize) + Math.abs(iFindConfigAttribute6 - this.mGreenSize) + Math.abs(iFindConfigAttribute7 - this.mBlueSize) + Math.abs(iFindConfigAttribute8 - this.mAlphaSize);
                    LogHelper.d(EglConfigSelector.TAG, "Try to find EglConfig, want format:" + eglConfigFormat + " r: " + iFindConfigAttribute5 + " g: " + iFindConfigAttribute6 + " b: " + iFindConfigAttribute7 + " a: " + iFindConfigAttribute8 + " visual id = " + iFindConfigAttribute3 + " surfaceType = " + iFindConfigAttribute4 + " depth = " + iFindConfigAttribute + " stencil = " + iFindConfigAttribute2 + " distance = " + iAbs);
                    if (EglConfigSelector.this.isInSupportedFormats(iFindConfigAttribute3) && isVisualIdValidate(iFindConfigAttribute3, eglConfigFormat) && iAbs < i3) {
                        EGLConfig eGLConfig3 = eGLConfigArr[i4];
                        EglConfigSelector.this.mSelectedPixelFormat = iFindConfigAttribute3;
                        eGLConfig2 = eGLConfig3;
                        i3 = iAbs;
                    }
                    i4++;
                    eGLDisplay2 = eGLDisplay;
                    i2 = 0;
                }
                eGLConfig2 = eGLConfig;
                i4++;
                eGLDisplay2 = eGLDisplay;
                i2 = 0;
            }
            EGLConfig eGLConfig4 = eGLConfig2;
            LogHelper.d(EglConfigSelector.TAG, "Find format: " + EglConfigSelector.this.mSelectedPixelFormat);
            return eGLConfig4;
        }

        private boolean isVisualIdValidate(int i, EglConfigFormat eglConfigFormat) {
            switch (AnonymousClass1.$SwitchMap$com$mediatek$camera$common$gles$egl$EglConfigSelector$EglConfigFormat[eglConfigFormat.ordinal()]) {
                case Camera2Proxy.TEMPLATE_RECORD:
                    if (i != 1) {
                        break;
                    }
                    break;
            }
            return false;
        }

        private boolean hasSpecifiedEglConfigFormat(EglConfigFormat eglConfigFormat, ArrayList<Integer> arrayList) {
            Iterator<Integer> it = arrayList.iterator();
            while (it.hasNext()) {
                int iIntValue = it.next().intValue();
                switch (AnonymousClass1.$SwitchMap$com$mediatek$camera$common$gles$egl$EglConfigSelector$EglConfigFormat[eglConfigFormat.ordinal()]) {
                    case Camera2Proxy.TEMPLATE_PREVIEW:
                        if (isYuvFormat(iIntValue)) {
                            return true;
                        }
                        break;
                    case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                        if (isRGBFormat(iIntValue)) {
                            return true;
                        }
                        break;
                    case Camera2Proxy.TEMPLATE_RECORD:
                        if (iIntValue == 1) {
                            return true;
                        }
                        break;
                    default:
                        return false;
                }
            }
            return false;
        }

        private boolean isYuvFormat(int i) {
            if (i == 17 || i == 35 || i == 842094169) {
                return true;
            }
            return false;
        }

        private boolean isRGBFormat(int i) {
            switch (i) {
                case Camera2Proxy.TEMPLATE_RECORD:
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    return true;
                default:
                    return false;
            }
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$mediatek$camera$common$gles$egl$EglConfigSelector$EglConfigFormat = new int[EglConfigFormat.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$gles$egl$EglConfigSelector$EglConfigFormat[EglConfigFormat.YUV.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$gles$egl$EglConfigSelector$EglConfigFormat[EglConfigFormat.RGB.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$gles$egl$EglConfigSelector$EglConfigFormat[EglConfigFormat.RGBA.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser() {
            super(8, 8, 8, 0, 0, 0);
        }
    }

    private boolean isInSupportedFormats(int i) {
        return this.mSupportedOutputFormats.contains(Integer.valueOf(i));
    }
}

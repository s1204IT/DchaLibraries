package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.ComplexColor;
import android.content.res.GradientColor;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Trace;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.IntProperty;
import android.util.Log;
import android.util.PathParser;
import android.util.Property;
import android.util.Xml;
import com.android.internal.R;
import com.android.internal.util.VirtualRefBasePtr;
import dalvik.annotation.optimization.FastNative;
import dalvik.system.VMRuntime;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class VectorDrawable extends Drawable {
    private static final String LOGTAG = VectorDrawable.class.getSimpleName();
    private static final String SHAPE_CLIP_PATH = "clip-path";
    private static final String SHAPE_GROUP = "group";
    private static final String SHAPE_PATH = "path";
    private static final String SHAPE_VECTOR = "vector";
    private ColorFilter mColorFilter;
    private boolean mDpiScaledDirty;
    private int mDpiScaledHeight;
    private Insets mDpiScaledInsets;
    private int mDpiScaledWidth;
    private boolean mMutated;
    private int mTargetDensity;
    private PorterDuffColorFilter mTintFilter;
    private final Rect mTmpBounds;
    private VectorDrawableState mVectorState;

    @FastNative
    private static native void nAddChild(long j, long j2);

    @FastNative
    private static native long nCreateClipPath();

    @FastNative
    private static native long nCreateClipPath(long j);

    @FastNative
    private static native long nCreateFullPath();

    @FastNative
    private static native long nCreateFullPath(long j);

    @FastNative
    private static native long nCreateGroup();

    @FastNative
    private static native long nCreateGroup(long j);

    @FastNative
    private static native long nCreateTree(long j);

    @FastNative
    private static native long nCreateTreeFromCopy(long j, long j2);

    private static native int nDraw(long j, long j2, long j3, Rect rect, boolean z, boolean z2);

    @FastNative
    private static native float nGetFillAlpha(long j);

    @FastNative
    private static native int nGetFillColor(long j);

    private static native boolean nGetFullPathProperties(long j, byte[] bArr, int i);

    private static native boolean nGetGroupProperties(long j, float[] fArr, int i);

    @FastNative
    private static native float nGetPivotX(long j);

    @FastNative
    private static native float nGetPivotY(long j);

    @FastNative
    private static native float nGetRootAlpha(long j);

    @FastNative
    private static native float nGetRotation(long j);

    @FastNative
    private static native float nGetScaleX(long j);

    @FastNative
    private static native float nGetScaleY(long j);

    @FastNative
    private static native float nGetStrokeAlpha(long j);

    @FastNative
    private static native int nGetStrokeColor(long j);

    @FastNative
    private static native float nGetStrokeWidth(long j);

    @FastNative
    private static native float nGetTranslateX(long j);

    @FastNative
    private static native float nGetTranslateY(long j);

    @FastNative
    private static native float nGetTrimPathEnd(long j);

    @FastNative
    private static native float nGetTrimPathOffset(long j);

    @FastNative
    private static native float nGetTrimPathStart(long j);

    @FastNative
    private static native void nSetAllowCaching(long j, boolean z);

    @FastNative
    private static native void nSetAntiAlias(long j, boolean z);

    @FastNative
    private static native void nSetFillAlpha(long j, float f);

    @FastNative
    private static native void nSetFillColor(long j, int i);

    private static native void nSetName(long j, String str);

    @FastNative
    private static native void nSetPathData(long j, long j2);

    private static native void nSetPathString(long j, String str, int i);

    @FastNative
    private static native void nSetPivotX(long j, float f);

    @FastNative
    private static native void nSetPivotY(long j, float f);

    @FastNative
    private static native void nSetRendererViewportSize(long j, float f, float f2);

    @FastNative
    private static native boolean nSetRootAlpha(long j, float f);

    @FastNative
    private static native void nSetRotation(long j, float f);

    @FastNative
    private static native void nSetScaleX(long j, float f);

    @FastNative
    private static native void nSetScaleY(long j, float f);

    @FastNative
    private static native void nSetStrokeAlpha(long j, float f);

    @FastNative
    private static native void nSetStrokeColor(long j, int i);

    @FastNative
    private static native void nSetStrokeWidth(long j, float f);

    @FastNative
    private static native void nSetTranslateX(long j, float f);

    @FastNative
    private static native void nSetTranslateY(long j, float f);

    @FastNative
    private static native void nSetTrimPathEnd(long j, float f);

    @FastNative
    private static native void nSetTrimPathOffset(long j, float f);

    @FastNative
    private static native void nSetTrimPathStart(long j, float f);

    @FastNative
    private static native void nUpdateFullPathFillGradient(long j, long j2);

    @FastNative
    private static native void nUpdateFullPathProperties(long j, float f, int i, float f2, int i2, float f3, float f4, float f5, float f6, float f7, int i3, int i4, int i5);

    @FastNative
    private static native void nUpdateFullPathStrokeGradient(long j, long j2);

    @FastNative
    private static native void nUpdateGroupProperties(long j, float f, float f2, float f3, float f4, float f5, float f6, float f7);

    public VectorDrawable() {
        this(null, null);
    }

    private VectorDrawable(VectorDrawableState vectorDrawableState, Resources resources) {
        this.mDpiScaledWidth = 0;
        this.mDpiScaledHeight = 0;
        this.mDpiScaledInsets = Insets.NONE;
        this.mDpiScaledDirty = true;
        this.mTmpBounds = new Rect();
        this.mVectorState = new VectorDrawableState(vectorDrawableState);
        updateLocalState(resources);
    }

    private void updateLocalState(Resources resources) {
        int iResolveDensity = Drawable.resolveDensity(resources, this.mVectorState.mDensity);
        if (this.mTargetDensity != iResolveDensity) {
            this.mTargetDensity = iResolveDensity;
            this.mDpiScaledDirty = true;
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mVectorState.mTint, this.mVectorState.mTintMode);
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mVectorState = new VectorDrawableState(this.mVectorState);
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        this.mMutated = false;
    }

    Object getTargetByName(String str) {
        return this.mVectorState.mVGTargetsMap.get(str);
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        this.mVectorState.mChangingConfigurations = getChangingConfigurations();
        return this.mVectorState;
    }

    @Override
    public void draw(Canvas canvas) {
        int i;
        copyBounds(this.mTmpBounds);
        if (this.mTmpBounds.width() <= 0 || this.mTmpBounds.height() <= 0) {
            return;
        }
        ColorFilter colorFilter = this.mColorFilter == null ? this.mTintFilter : this.mColorFilter;
        int iNDraw = nDraw(this.mVectorState.getNativeRenderer(), canvas.getNativeCanvasWrapper(), colorFilter == null ? 0L : colorFilter.getNativeInstance(), this.mTmpBounds, needMirroring(), this.mVectorState.canReuseCache());
        if (iNDraw == 0) {
            return;
        }
        if (canvas.isHardwareAccelerated()) {
            i = (iNDraw - this.mVectorState.mLastHWCachePixelCount) * 4;
            this.mVectorState.mLastHWCachePixelCount = iNDraw;
        } else {
            i = (iNDraw - this.mVectorState.mLastSWCachePixelCount) * 4;
            this.mVectorState.mLastSWCachePixelCount = iNDraw;
        }
        if (i > 0) {
            VMRuntime.getRuntime().registerNativeAllocation(i);
        } else if (i < 0) {
            VMRuntime.getRuntime().registerNativeFree(-i);
        }
    }

    @Override
    public int getAlpha() {
        return (int) (this.mVectorState.getAlpha() * 255.0f);
    }

    @Override
    public void setAlpha(int i) {
        if (this.mVectorState.setAlpha(i / 255.0f)) {
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mColorFilter = colorFilter;
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter() {
        return this.mColorFilter;
    }

    @Override
    public void setTintList(ColorStateList colorStateList) {
        VectorDrawableState vectorDrawableState = this.mVectorState;
        if (vectorDrawableState.mTint != colorStateList) {
            vectorDrawableState.mTint = colorStateList;
            this.mTintFilter = updateTintFilter(this.mTintFilter, colorStateList, vectorDrawableState.mTintMode);
            invalidateSelf();
        }
    }

    @Override
    public void setTintMode(PorterDuff.Mode mode) {
        VectorDrawableState vectorDrawableState = this.mVectorState;
        if (vectorDrawableState.mTintMode != mode) {
            vectorDrawableState.mTintMode = mode;
            this.mTintFilter = updateTintFilter(this.mTintFilter, vectorDrawableState.mTint, mode);
            invalidateSelf();
        }
    }

    @Override
    public boolean isStateful() {
        return super.isStateful() || (this.mVectorState != null && this.mVectorState.isStateful());
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return this.mVectorState != null && this.mVectorState.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(int[] iArr) {
        boolean z;
        if (isStateful()) {
            mutate();
        }
        VectorDrawableState vectorDrawableState = this.mVectorState;
        if (vectorDrawableState.onStateChange(iArr)) {
            vectorDrawableState.mCacheDirty = true;
            z = true;
        } else {
            z = false;
        }
        if (vectorDrawableState.mTint == null || vectorDrawableState.mTintMode == null) {
            return z;
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, vectorDrawableState.mTint, vectorDrawableState.mTintMode);
        return true;
    }

    @Override
    public int getOpacity() {
        return getAlpha() == 0 ? -2 : -3;
    }

    @Override
    public int getIntrinsicWidth() {
        if (this.mDpiScaledDirty) {
            computeVectorSize();
        }
        return this.mDpiScaledWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        if (this.mDpiScaledDirty) {
            computeVectorSize();
        }
        return this.mDpiScaledHeight;
    }

    @Override
    public Insets getOpticalInsets() {
        if (this.mDpiScaledDirty) {
            computeVectorSize();
        }
        return this.mDpiScaledInsets;
    }

    void computeVectorSize() {
        Insets insets = this.mVectorState.mOpticalInsets;
        int i = this.mVectorState.mDensity;
        int i2 = this.mTargetDensity;
        if (i2 != i) {
            this.mDpiScaledWidth = Drawable.scaleFromDensity(this.mVectorState.mBaseWidth, i, i2, true);
            this.mDpiScaledHeight = Drawable.scaleFromDensity(this.mVectorState.mBaseHeight, i, i2, true);
            this.mDpiScaledInsets = Insets.of(Drawable.scaleFromDensity(insets.left, i, i2, false), Drawable.scaleFromDensity(insets.top, i, i2, false), Drawable.scaleFromDensity(insets.right, i, i2, false), Drawable.scaleFromDensity(insets.bottom, i, i2, false));
        } else {
            this.mDpiScaledWidth = this.mVectorState.mBaseWidth;
            this.mDpiScaledHeight = this.mVectorState.mBaseHeight;
            this.mDpiScaledInsets = insets;
        }
        this.mDpiScaledDirty = false;
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mVectorState != null && this.mVectorState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    public void applyTheme(Resources.Theme theme) {
        super.applyTheme(theme);
        VectorDrawableState vectorDrawableState = this.mVectorState;
        if (vectorDrawableState == null) {
            return;
        }
        this.mDpiScaledDirty = this.mVectorState.setDensity(Drawable.resolveDensity(theme.getResources(), 0)) | this.mDpiScaledDirty;
        if (vectorDrawableState.mThemeAttrs != null) {
            TypedArray typedArrayResolveAttributes = theme.resolveAttributes(vectorDrawableState.mThemeAttrs, R.styleable.VectorDrawable);
            try {
                try {
                    vectorDrawableState.mCacheDirty = true;
                    updateStateFromTypedArray(typedArrayResolveAttributes);
                    typedArrayResolveAttributes.recycle();
                    this.mDpiScaledDirty = true;
                } catch (XmlPullParserException e) {
                    throw new RuntimeException(e);
                }
            } catch (Throwable th) {
                typedArrayResolveAttributes.recycle();
                throw th;
            }
        }
        if (vectorDrawableState.mTint != null && vectorDrawableState.mTint.canApplyTheme()) {
            vectorDrawableState.mTint = vectorDrawableState.mTint.obtainForTheme(theme);
        }
        if (this.mVectorState != null && this.mVectorState.canApplyTheme()) {
            this.mVectorState.applyTheme(theme);
        }
        updateLocalState(theme.getResources());
    }

    public float getPixelSize() {
        if (this.mVectorState == null || this.mVectorState.mBaseWidth == 0 || this.mVectorState.mBaseHeight == 0 || this.mVectorState.mViewportHeight == 0.0f || this.mVectorState.mViewportWidth == 0.0f) {
            return 1.0f;
        }
        return Math.min(this.mVectorState.mViewportWidth / this.mVectorState.mBaseWidth, this.mVectorState.mViewportHeight / this.mVectorState.mBaseHeight);
    }

    public static VectorDrawable create(Resources resources, int i) {
        int next;
        try {
            XmlResourceParser xml = resources.getXml(i);
            AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xml);
            do {
                next = xml.next();
                if (next == 2) {
                    break;
                }
            } while (next != 1);
            if (next != 2) {
                throw new XmlPullParserException("No start tag found");
            }
            VectorDrawable vectorDrawable = new VectorDrawable();
            vectorDrawable.inflate(resources, xml, attributeSetAsAttributeSet);
            return vectorDrawable;
        } catch (IOException e) {
            Log.e(LOGTAG, "parser error", e);
            return null;
        } catch (XmlPullParserException e2) {
            Log.e(LOGTAG, "parser error", e2);
            return null;
        }
    }

    @Override
    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        try {
            Trace.traceBegin(8192L, "VectorDrawable#inflate");
            if (this.mVectorState.mRootGroup != null || this.mVectorState.mNativeTree != null) {
                if (this.mVectorState.mRootGroup != null) {
                    VMRuntime.getRuntime().registerNativeFree(this.mVectorState.mRootGroup.getNativeSize());
                    this.mVectorState.mRootGroup.setTree(null);
                }
                this.mVectorState.mRootGroup = new VGroup();
                if (this.mVectorState.mNativeTree != null) {
                    VMRuntime.getRuntime().registerNativeFree(316);
                    this.mVectorState.mNativeTree.release();
                }
                this.mVectorState.createNativeTree(this.mVectorState.mRootGroup);
            }
            VectorDrawableState vectorDrawableState = this.mVectorState;
            vectorDrawableState.setDensity(Drawable.resolveDensity(resources, 0));
            TypedArray typedArrayObtainAttributes = obtainAttributes(resources, theme, attributeSet, R.styleable.VectorDrawable);
            updateStateFromTypedArray(typedArrayObtainAttributes);
            typedArrayObtainAttributes.recycle();
            this.mDpiScaledDirty = true;
            vectorDrawableState.mCacheDirty = true;
            inflateChildElements(resources, xmlPullParser, attributeSet, theme);
            vectorDrawableState.onTreeConstructionFinished();
            updateLocalState(resources);
        } finally {
            Trace.traceEnd(8192L);
        }
    }

    private void updateStateFromTypedArray(TypedArray typedArray) throws XmlPullParserException {
        VectorDrawableState vectorDrawableState = this.mVectorState;
        vectorDrawableState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        vectorDrawableState.mThemeAttrs = typedArray.extractThemeAttrs();
        int i = typedArray.getInt(6, -1);
        if (i != -1) {
            vectorDrawableState.mTintMode = Drawable.parseTintMode(i, PorterDuff.Mode.SRC_IN);
        }
        ColorStateList colorStateList = typedArray.getColorStateList(1);
        if (colorStateList != null) {
            vectorDrawableState.mTint = colorStateList;
        }
        vectorDrawableState.mAutoMirrored = typedArray.getBoolean(5, vectorDrawableState.mAutoMirrored);
        vectorDrawableState.setViewportSize(typedArray.getFloat(7, vectorDrawableState.mViewportWidth), typedArray.getFloat(8, vectorDrawableState.mViewportHeight));
        if (vectorDrawableState.mViewportWidth <= 0.0f) {
            throw new XmlPullParserException(typedArray.getPositionDescription() + "<vector> tag requires viewportWidth > 0");
        }
        if (vectorDrawableState.mViewportHeight <= 0.0f) {
            throw new XmlPullParserException(typedArray.getPositionDescription() + "<vector> tag requires viewportHeight > 0");
        }
        vectorDrawableState.mBaseWidth = typedArray.getDimensionPixelSize(3, vectorDrawableState.mBaseWidth);
        vectorDrawableState.mBaseHeight = typedArray.getDimensionPixelSize(2, vectorDrawableState.mBaseHeight);
        if (vectorDrawableState.mBaseWidth <= 0) {
            throw new XmlPullParserException(typedArray.getPositionDescription() + "<vector> tag requires width > 0");
        }
        if (vectorDrawableState.mBaseHeight <= 0) {
            throw new XmlPullParserException(typedArray.getPositionDescription() + "<vector> tag requires height > 0");
        }
        vectorDrawableState.mOpticalInsets = Insets.of(typedArray.getDimensionPixelOffset(10, vectorDrawableState.mOpticalInsets.left), typedArray.getDimensionPixelOffset(12, vectorDrawableState.mOpticalInsets.top), typedArray.getDimensionPixelOffset(11, vectorDrawableState.mOpticalInsets.right), typedArray.getDimensionPixelOffset(9, vectorDrawableState.mOpticalInsets.bottom));
        vectorDrawableState.setAlpha(typedArray.getFloat(4, vectorDrawableState.getAlpha()));
        String string = typedArray.getString(0);
        if (string != null) {
            vectorDrawableState.mRootName = string;
            vectorDrawableState.mVGTargetsMap.put(string, vectorDrawableState);
        }
    }

    private void inflateChildElements(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        VectorDrawableState vectorDrawableState = this.mVectorState;
        Stack stack = new Stack();
        stack.push(vectorDrawableState.mRootGroup);
        int eventType = xmlPullParser.getEventType();
        int depth = xmlPullParser.getDepth() + 1;
        boolean z = true;
        while (eventType != 1 && (xmlPullParser.getDepth() >= depth || eventType != 3)) {
            if (eventType == 2) {
                String name = xmlPullParser.getName();
                VGroup vGroup = (VGroup) stack.peek();
                if (SHAPE_PATH.equals(name)) {
                    VFullPath vFullPath = new VFullPath();
                    vFullPath.inflate(resources, attributeSet, theme);
                    vGroup.addChild(vFullPath);
                    if (vFullPath.getPathName() != null) {
                        vectorDrawableState.mVGTargetsMap.put(vFullPath.getPathName(), vFullPath);
                    }
                    z = false;
                    vectorDrawableState.mChangingConfigurations = vFullPath.mChangingConfigurations | vectorDrawableState.mChangingConfigurations;
                } else if (SHAPE_CLIP_PATH.equals(name)) {
                    VClipPath vClipPath = new VClipPath();
                    vClipPath.inflate(resources, attributeSet, theme);
                    vGroup.addChild(vClipPath);
                    if (vClipPath.getPathName() != null) {
                        vectorDrawableState.mVGTargetsMap.put(vClipPath.getPathName(), vClipPath);
                    }
                    vectorDrawableState.mChangingConfigurations = vClipPath.mChangingConfigurations | vectorDrawableState.mChangingConfigurations;
                } else if ("group".equals(name)) {
                    VGroup vGroup2 = new VGroup();
                    vGroup2.inflate(resources, attributeSet, theme);
                    vGroup.addChild(vGroup2);
                    stack.push(vGroup2);
                    if (vGroup2.getGroupName() != null) {
                        vectorDrawableState.mVGTargetsMap.put(vGroup2.getGroupName(), vGroup2);
                    }
                    vectorDrawableState.mChangingConfigurations = vGroup2.mChangingConfigurations | vectorDrawableState.mChangingConfigurations;
                }
            } else if (eventType == 3 && "group".equals(xmlPullParser.getName())) {
                stack.pop();
            }
            eventType = xmlPullParser.next();
        }
        if (z) {
            StringBuffer stringBuffer = new StringBuffer();
            if (stringBuffer.length() > 0) {
                stringBuffer.append(" or ");
            }
            stringBuffer.append(SHAPE_PATH);
            throw new XmlPullParserException("no " + ((Object) stringBuffer) + " defined");
        }
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mVectorState.getChangingConfigurations();
    }

    void setAllowCaching(boolean z) {
        nSetAllowCaching(this.mVectorState.getNativeRenderer(), z);
    }

    private boolean needMirroring() {
        return isAutoMirrored() && getLayoutDirection() == 1;
    }

    @Override
    public void setAutoMirrored(boolean z) {
        if (this.mVectorState.mAutoMirrored != z) {
            this.mVectorState.mAutoMirrored = z;
            invalidateSelf();
        }
    }

    @Override
    public boolean isAutoMirrored() {
        return this.mVectorState.mAutoMirrored;
    }

    public long getNativeTree() {
        return this.mVectorState.getNativeRenderer();
    }

    public void setAntiAlias(boolean z) {
        nSetAntiAlias(this.mVectorState.mNativeTree.get(), z);
    }

    static class VectorDrawableState extends Drawable.ConstantState {
        static final Property<VectorDrawableState, Float> ALPHA = new FloatProperty<VectorDrawableState>("alpha") {
            @Override
            public void setValue(VectorDrawableState vectorDrawableState, float f) {
                vectorDrawableState.setAlpha(f);
            }

            @Override
            public Float get(VectorDrawableState vectorDrawableState) {
                return Float.valueOf(vectorDrawableState.getAlpha());
            }
        };
        private static final int NATIVE_ALLOCATION_SIZE = 316;
        boolean mAutoMirrored;
        int mBaseHeight;
        int mBaseWidth;
        boolean mCacheDirty;
        boolean mCachedAutoMirrored;
        int[] mCachedThemeAttrs;
        ColorStateList mCachedTint;
        PorterDuff.Mode mCachedTintMode;
        int mChangingConfigurations;
        int mDensity;
        Insets mOpticalInsets;
        VGroup mRootGroup;
        String mRootName;
        int[] mThemeAttrs;
        ColorStateList mTint;
        PorterDuff.Mode mTintMode;
        float mViewportWidth = 0.0f;
        float mViewportHeight = 0.0f;
        VirtualRefBasePtr mNativeTree = null;
        final ArrayMap<String, Object> mVGTargetsMap = new ArrayMap<>();
        int mLastSWCachePixelCount = 0;
        int mLastHWCachePixelCount = 0;
        private int mAllocationOfAllNodes = 0;

        Property getProperty(String str) {
            if (ALPHA.getName().equals(str)) {
                return ALPHA;
            }
            return null;
        }

        public VectorDrawableState(VectorDrawableState vectorDrawableState) {
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mBaseWidth = 0;
            this.mBaseHeight = 0;
            this.mOpticalInsets = Insets.NONE;
            this.mRootName = null;
            this.mDensity = 160;
            if (vectorDrawableState != null) {
                this.mThemeAttrs = vectorDrawableState.mThemeAttrs;
                this.mChangingConfigurations = vectorDrawableState.mChangingConfigurations;
                this.mTint = vectorDrawableState.mTint;
                this.mTintMode = vectorDrawableState.mTintMode;
                this.mAutoMirrored = vectorDrawableState.mAutoMirrored;
                this.mRootGroup = new VGroup(vectorDrawableState.mRootGroup, this.mVGTargetsMap);
                createNativeTreeFromCopy(vectorDrawableState, this.mRootGroup);
                this.mBaseWidth = vectorDrawableState.mBaseWidth;
                this.mBaseHeight = vectorDrawableState.mBaseHeight;
                setViewportSize(vectorDrawableState.mViewportWidth, vectorDrawableState.mViewportHeight);
                this.mOpticalInsets = vectorDrawableState.mOpticalInsets;
                this.mRootName = vectorDrawableState.mRootName;
                this.mDensity = vectorDrawableState.mDensity;
                if (vectorDrawableState.mRootName != null) {
                    this.mVGTargetsMap.put(vectorDrawableState.mRootName, this);
                }
            } else {
                this.mRootGroup = new VGroup();
                createNativeTree(this.mRootGroup);
            }
            onTreeConstructionFinished();
        }

        private void createNativeTree(VGroup vGroup) {
            this.mNativeTree = new VirtualRefBasePtr(VectorDrawable.nCreateTree(vGroup.mNativePtr));
            VMRuntime.getRuntime().registerNativeAllocation(316);
        }

        private void createNativeTreeFromCopy(VectorDrawableState vectorDrawableState, VGroup vGroup) {
            this.mNativeTree = new VirtualRefBasePtr(VectorDrawable.nCreateTreeFromCopy(vectorDrawableState.mNativeTree.get(), vGroup.mNativePtr));
            VMRuntime.getRuntime().registerNativeAllocation(316);
        }

        void onTreeConstructionFinished() {
            this.mRootGroup.setTree(this.mNativeTree);
            this.mAllocationOfAllNodes = this.mRootGroup.getNativeSize();
            VMRuntime.getRuntime().registerNativeAllocation(this.mAllocationOfAllNodes);
        }

        long getNativeRenderer() {
            if (this.mNativeTree == null) {
                return 0L;
            }
            return this.mNativeTree.get();
        }

        public boolean canReuseCache() {
            if (!this.mCacheDirty && this.mCachedThemeAttrs == this.mThemeAttrs && this.mCachedTint == this.mTint && this.mCachedTintMode == this.mTintMode && this.mCachedAutoMirrored == this.mAutoMirrored) {
                return true;
            }
            updateCacheStates();
            return false;
        }

        public void updateCacheStates() {
            this.mCachedThemeAttrs = this.mThemeAttrs;
            this.mCachedTint = this.mTint;
            this.mCachedTintMode = this.mTintMode;
            this.mCachedAutoMirrored = this.mAutoMirrored;
            this.mCacheDirty = false;
        }

        public void applyTheme(Resources.Theme theme) {
            this.mRootGroup.applyTheme(theme);
        }

        @Override
        public boolean canApplyTheme() {
            return this.mThemeAttrs != null || (this.mRootGroup != null && this.mRootGroup.canApplyTheme()) || ((this.mTint != null && this.mTint.canApplyTheme()) || super.canApplyTheme());
        }

        @Override
        public Drawable newDrawable() {
            return new VectorDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources resources) {
            return new VectorDrawable(this, resources);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations | (this.mTint != null ? this.mTint.getChangingConfigurations() : 0);
        }

        public boolean isStateful() {
            return (this.mTint != null && this.mTint.isStateful()) || (this.mRootGroup != null && this.mRootGroup.isStateful());
        }

        public boolean hasFocusStateSpecified() {
            return (this.mTint != null && this.mTint.hasFocusStateSpecified()) || (this.mRootGroup != null && this.mRootGroup.hasFocusStateSpecified());
        }

        void setViewportSize(float f, float f2) {
            this.mViewportWidth = f;
            this.mViewportHeight = f2;
            VectorDrawable.nSetRendererViewportSize(getNativeRenderer(), f, f2);
        }

        public final boolean setDensity(int i) {
            if (this.mDensity != i) {
                int i2 = this.mDensity;
                this.mDensity = i;
                applyDensityScaling(i2, i);
                return true;
            }
            return false;
        }

        private void applyDensityScaling(int i, int i2) {
            this.mBaseWidth = Drawable.scaleFromDensity(this.mBaseWidth, i, i2, true);
            this.mBaseHeight = Drawable.scaleFromDensity(this.mBaseHeight, i, i2, true);
            this.mOpticalInsets = Insets.of(Drawable.scaleFromDensity(this.mOpticalInsets.left, i, i2, false), Drawable.scaleFromDensity(this.mOpticalInsets.top, i, i2, false), Drawable.scaleFromDensity(this.mOpticalInsets.right, i, i2, false), Drawable.scaleFromDensity(this.mOpticalInsets.bottom, i, i2, false));
        }

        public boolean onStateChange(int[] iArr) {
            return this.mRootGroup.onStateChange(iArr);
        }

        public void finalize() throws Throwable {
            super.finalize();
            VMRuntime.getRuntime().registerNativeFree(316 + this.mAllocationOfAllNodes + (this.mLastHWCachePixelCount * 4) + (this.mLastSWCachePixelCount * 4));
        }

        public boolean setAlpha(float f) {
            return VectorDrawable.nSetRootAlpha(this.mNativeTree.get(), f);
        }

        public float getAlpha() {
            return VectorDrawable.nGetRootAlpha(this.mNativeTree.get());
        }
    }

    static class VGroup extends VObject {
        private static final int NATIVE_ALLOCATION_SIZE = 100;
        private static final int PIVOT_X_INDEX = 1;
        private static final int PIVOT_Y_INDEX = 2;
        private static final int ROTATION_INDEX = 0;
        private static final int SCALE_X_INDEX = 3;
        private static final int SCALE_Y_INDEX = 4;
        private static final int TRANSFORM_PROPERTY_COUNT = 7;
        private static final int TRANSLATE_X_INDEX = 5;
        private static final int TRANSLATE_Y_INDEX = 6;
        private int mChangingConfigurations;
        private final ArrayList<VObject> mChildren;
        private String mGroupName;
        private boolean mIsStateful;
        private final long mNativePtr;
        private int[] mThemeAttrs;
        private float[] mTransform;
        private static final HashMap<String, Integer> sPropertyIndexMap = new HashMap<String, Integer>() {
            {
                put("translateX", 5);
                put("translateY", 6);
                put("scaleX", 3);
                put("scaleY", 4);
                put("pivotX", 1);
                put("pivotY", 2);
                put("rotation", 0);
            }
        };
        private static final Property<VGroup, Float> TRANSLATE_X = new FloatProperty<VGroup>("translateX") {
            @Override
            public void setValue(VGroup vGroup, float f) {
                vGroup.setTranslateX(f);
            }

            @Override
            public Float get(VGroup vGroup) {
                return Float.valueOf(vGroup.getTranslateX());
            }
        };
        private static final Property<VGroup, Float> TRANSLATE_Y = new FloatProperty<VGroup>("translateY") {
            @Override
            public void setValue(VGroup vGroup, float f) {
                vGroup.setTranslateY(f);
            }

            @Override
            public Float get(VGroup vGroup) {
                return Float.valueOf(vGroup.getTranslateY());
            }
        };
        private static final Property<VGroup, Float> SCALE_X = new FloatProperty<VGroup>("scaleX") {
            @Override
            public void setValue(VGroup vGroup, float f) {
                vGroup.setScaleX(f);
            }

            @Override
            public Float get(VGroup vGroup) {
                return Float.valueOf(vGroup.getScaleX());
            }
        };
        private static final Property<VGroup, Float> SCALE_Y = new FloatProperty<VGroup>("scaleY") {
            @Override
            public void setValue(VGroup vGroup, float f) {
                vGroup.setScaleY(f);
            }

            @Override
            public Float get(VGroup vGroup) {
                return Float.valueOf(vGroup.getScaleY());
            }
        };
        private static final Property<VGroup, Float> PIVOT_X = new FloatProperty<VGroup>("pivotX") {
            @Override
            public void setValue(VGroup vGroup, float f) {
                vGroup.setPivotX(f);
            }

            @Override
            public Float get(VGroup vGroup) {
                return Float.valueOf(vGroup.getPivotX());
            }
        };
        private static final Property<VGroup, Float> PIVOT_Y = new FloatProperty<VGroup>("pivotY") {
            @Override
            public void setValue(VGroup vGroup, float f) {
                vGroup.setPivotY(f);
            }

            @Override
            public Float get(VGroup vGroup) {
                return Float.valueOf(vGroup.getPivotY());
            }
        };
        private static final Property<VGroup, Float> ROTATION = new FloatProperty<VGroup>("rotation") {
            @Override
            public void setValue(VGroup vGroup, float f) {
                vGroup.setRotation(f);
            }

            @Override
            public Float get(VGroup vGroup) {
                return Float.valueOf(vGroup.getRotation());
            }
        };
        private static final HashMap<String, Property> sPropertyMap = new HashMap<String, Property>() {
            {
                put("translateX", VGroup.TRANSLATE_X);
                put("translateY", VGroup.TRANSLATE_Y);
                put("scaleX", VGroup.SCALE_X);
                put("scaleY", VGroup.SCALE_Y);
                put("pivotX", VGroup.PIVOT_X);
                put("pivotY", VGroup.PIVOT_Y);
                put("rotation", VGroup.ROTATION);
            }
        };

        static int getPropertyIndex(String str) {
            if (sPropertyIndexMap.containsKey(str)) {
                return sPropertyIndexMap.get(str).intValue();
            }
            return -1;
        }

        public VGroup(VGroup vGroup, ArrayMap<String, Object> arrayMap) {
            VPath vClipPath;
            this.mChildren = new ArrayList<>();
            this.mGroupName = null;
            this.mIsStateful = vGroup.mIsStateful;
            this.mThemeAttrs = vGroup.mThemeAttrs;
            this.mGroupName = vGroup.mGroupName;
            this.mChangingConfigurations = vGroup.mChangingConfigurations;
            if (this.mGroupName != null) {
                arrayMap.put(this.mGroupName, this);
            }
            this.mNativePtr = VectorDrawable.nCreateGroup(vGroup.mNativePtr);
            ArrayList<VObject> arrayList = vGroup.mChildren;
            for (int i = 0; i < arrayList.size(); i++) {
                VObject vObject = arrayList.get(i);
                if (vObject instanceof VGroup) {
                    addChild(new VGroup((VGroup) vObject, arrayMap));
                } else {
                    if (vObject instanceof VFullPath) {
                        vClipPath = new VFullPath((VFullPath) vObject);
                    } else if (vObject instanceof VClipPath) {
                        vClipPath = new VClipPath((VClipPath) vObject);
                    } else {
                        throw new IllegalStateException("Unknown object in the tree!");
                    }
                    addChild(vClipPath);
                    if (vClipPath.mPathName != null) {
                        arrayMap.put(vClipPath.mPathName, vClipPath);
                    }
                }
            }
        }

        public VGroup() {
            this.mChildren = new ArrayList<>();
            this.mGroupName = null;
            this.mNativePtr = VectorDrawable.nCreateGroup();
        }

        @Override
        Property getProperty(String str) {
            if (sPropertyMap.containsKey(str)) {
                return sPropertyMap.get(str);
            }
            return null;
        }

        public String getGroupName() {
            return this.mGroupName;
        }

        public void addChild(VObject vObject) {
            VectorDrawable.nAddChild(this.mNativePtr, vObject.getNativePtr());
            this.mChildren.add(vObject);
            this.mIsStateful = vObject.isStateful() | this.mIsStateful;
        }

        @Override
        public void setTree(VirtualRefBasePtr virtualRefBasePtr) {
            super.setTree(virtualRefBasePtr);
            for (int i = 0; i < this.mChildren.size(); i++) {
                this.mChildren.get(i).setTree(virtualRefBasePtr);
            }
        }

        @Override
        public long getNativePtr() {
            return this.mNativePtr;
        }

        @Override
        public void inflate(Resources resources, AttributeSet attributeSet, Resources.Theme theme) {
            TypedArray typedArrayObtainAttributes = Drawable.obtainAttributes(resources, theme, attributeSet, R.styleable.VectorDrawableGroup);
            updateStateFromTypedArray(typedArrayObtainAttributes);
            typedArrayObtainAttributes.recycle();
        }

        void updateStateFromTypedArray(TypedArray typedArray) {
            this.mChangingConfigurations |= typedArray.getChangingConfigurations();
            this.mThemeAttrs = typedArray.extractThemeAttrs();
            if (this.mTransform == null) {
                this.mTransform = new float[7];
            }
            if (!VectorDrawable.nGetGroupProperties(this.mNativePtr, this.mTransform, 7)) {
                throw new RuntimeException("Error: inconsistent property count");
            }
            float f = typedArray.getFloat(5, this.mTransform[0]);
            float f2 = typedArray.getFloat(1, this.mTransform[1]);
            float f3 = typedArray.getFloat(2, this.mTransform[2]);
            float f4 = typedArray.getFloat(3, this.mTransform[3]);
            float f5 = typedArray.getFloat(4, this.mTransform[4]);
            float f6 = typedArray.getFloat(6, this.mTransform[5]);
            float f7 = typedArray.getFloat(7, this.mTransform[6]);
            String string = typedArray.getString(0);
            if (string != null) {
                this.mGroupName = string;
                VectorDrawable.nSetName(this.mNativePtr, this.mGroupName);
            }
            VectorDrawable.nUpdateGroupProperties(this.mNativePtr, f, f2, f3, f4, f5, f6, f7);
        }

        @Override
        public boolean onStateChange(int[] iArr) {
            ArrayList<VObject> arrayList = this.mChildren;
            int size = arrayList.size();
            boolean zOnStateChange = false;
            for (int i = 0; i < size; i++) {
                VObject vObject = arrayList.get(i);
                if (vObject.isStateful()) {
                    zOnStateChange |= vObject.onStateChange(iArr);
                }
            }
            return zOnStateChange;
        }

        @Override
        public boolean isStateful() {
            return this.mIsStateful;
        }

        @Override
        public boolean hasFocusStateSpecified() {
            ArrayList<VObject> arrayList = this.mChildren;
            int size = arrayList.size();
            boolean zHasFocusStateSpecified = false;
            for (int i = 0; i < size; i++) {
                VObject vObject = arrayList.get(i);
                if (vObject.isStateful()) {
                    zHasFocusStateSpecified |= vObject.hasFocusStateSpecified();
                }
            }
            return zHasFocusStateSpecified;
        }

        @Override
        int getNativeSize() {
            int nativeSize = 100;
            for (int i = 0; i < this.mChildren.size(); i++) {
                nativeSize += this.mChildren.get(i).getNativeSize();
            }
            return nativeSize;
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null) {
                return true;
            }
            ArrayList<VObject> arrayList = this.mChildren;
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                if (arrayList.get(i).canApplyTheme()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void applyTheme(Resources.Theme theme) {
            if (this.mThemeAttrs != null) {
                TypedArray typedArrayResolveAttributes = theme.resolveAttributes(this.mThemeAttrs, R.styleable.VectorDrawableGroup);
                updateStateFromTypedArray(typedArrayResolveAttributes);
                typedArrayResolveAttributes.recycle();
            }
            ArrayList<VObject> arrayList = this.mChildren;
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                VObject vObject = arrayList.get(i);
                if (vObject.canApplyTheme()) {
                    vObject.applyTheme(theme);
                    this.mIsStateful = vObject.isStateful() | this.mIsStateful;
                }
            }
        }

        public float getRotation() {
            if (isTreeValid()) {
                return VectorDrawable.nGetRotation(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setRotation(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetRotation(this.mNativePtr, f);
            }
        }

        public float getPivotX() {
            if (isTreeValid()) {
                return VectorDrawable.nGetPivotX(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setPivotX(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetPivotX(this.mNativePtr, f);
            }
        }

        public float getPivotY() {
            if (isTreeValid()) {
                return VectorDrawable.nGetPivotY(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setPivotY(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetPivotY(this.mNativePtr, f);
            }
        }

        public float getScaleX() {
            if (isTreeValid()) {
                return VectorDrawable.nGetScaleX(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setScaleX(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetScaleX(this.mNativePtr, f);
            }
        }

        public float getScaleY() {
            if (isTreeValid()) {
                return VectorDrawable.nGetScaleY(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setScaleY(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetScaleY(this.mNativePtr, f);
            }
        }

        public float getTranslateX() {
            if (isTreeValid()) {
                return VectorDrawable.nGetTranslateX(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setTranslateX(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetTranslateX(this.mNativePtr, f);
            }
        }

        public float getTranslateY() {
            if (isTreeValid()) {
                return VectorDrawable.nGetTranslateY(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setTranslateY(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetTranslateY(this.mNativePtr, f);
            }
        }
    }

    static abstract class VPath extends VObject {
        private static final Property<VPath, PathParser.PathData> PATH_DATA = new Property<VPath, PathParser.PathData>(PathParser.PathData.class, "pathData") {
            @Override
            public void set(VPath vPath, PathParser.PathData pathData) {
                vPath.setPathData(pathData);
            }

            @Override
            public PathParser.PathData get(VPath vPath) {
                return vPath.getPathData();
            }
        };
        int mChangingConfigurations;
        protected PathParser.PathData mPathData;
        String mPathName;

        @Override
        Property getProperty(String str) {
            if (PATH_DATA.getName().equals(str)) {
                return PATH_DATA;
            }
            return null;
        }

        public VPath() {
            this.mPathData = null;
        }

        public VPath(VPath vPath) {
            this.mPathData = null;
            this.mPathName = vPath.mPathName;
            this.mChangingConfigurations = vPath.mChangingConfigurations;
            this.mPathData = vPath.mPathData != null ? new PathParser.PathData(vPath.mPathData) : null;
        }

        public String getPathName() {
            return this.mPathName;
        }

        public PathParser.PathData getPathData() {
            return this.mPathData;
        }

        public void setPathData(PathParser.PathData pathData) {
            this.mPathData.setPathData(pathData);
            if (isTreeValid()) {
                VectorDrawable.nSetPathData(getNativePtr(), this.mPathData.getNativePtr());
            }
        }
    }

    private static class VClipPath extends VPath {
        private static final int NATIVE_ALLOCATION_SIZE = 120;
        private final long mNativePtr;

        public VClipPath() {
            this.mNativePtr = VectorDrawable.nCreateClipPath();
        }

        public VClipPath(VClipPath vClipPath) {
            super(vClipPath);
            this.mNativePtr = VectorDrawable.nCreateClipPath(vClipPath.mNativePtr);
        }

        @Override
        public long getNativePtr() {
            return this.mNativePtr;
        }

        @Override
        public void inflate(Resources resources, AttributeSet attributeSet, Resources.Theme theme) {
            TypedArray typedArrayObtainAttributes = Drawable.obtainAttributes(resources, theme, attributeSet, R.styleable.VectorDrawableClipPath);
            updateStateFromTypedArray(typedArrayObtainAttributes);
            typedArrayObtainAttributes.recycle();
        }

        @Override
        public boolean canApplyTheme() {
            return false;
        }

        @Override
        public void applyTheme(Resources.Theme theme) {
        }

        @Override
        public boolean onStateChange(int[] iArr) {
            return false;
        }

        @Override
        public boolean isStateful() {
            return false;
        }

        @Override
        public boolean hasFocusStateSpecified() {
            return false;
        }

        @Override
        int getNativeSize() {
            return 120;
        }

        private void updateStateFromTypedArray(TypedArray typedArray) {
            this.mChangingConfigurations |= typedArray.getChangingConfigurations();
            String string = typedArray.getString(0);
            if (string != null) {
                this.mPathName = string;
                VectorDrawable.nSetName(this.mNativePtr, this.mPathName);
            }
            String string2 = typedArray.getString(1);
            if (string2 != null) {
                this.mPathData = new PathParser.PathData(string2);
                VectorDrawable.nSetPathString(this.mNativePtr, string2, string2.length());
            }
        }
    }

    static class VFullPath extends VPath {
        private static final int FILL_ALPHA_INDEX = 4;
        private static final int FILL_COLOR_INDEX = 3;
        private static final int FILL_TYPE_INDEX = 11;
        private static final int NATIVE_ALLOCATION_SIZE = 264;
        private static final int STROKE_ALPHA_INDEX = 2;
        private static final int STROKE_COLOR_INDEX = 1;
        private static final int STROKE_LINE_CAP_INDEX = 8;
        private static final int STROKE_LINE_JOIN_INDEX = 9;
        private static final int STROKE_MITER_LIMIT_INDEX = 10;
        private static final int STROKE_WIDTH_INDEX = 0;
        private static final int TOTAL_PROPERTY_COUNT = 12;
        private static final int TRIM_PATH_END_INDEX = 6;
        private static final int TRIM_PATH_OFFSET_INDEX = 7;
        private static final int TRIM_PATH_START_INDEX = 5;
        ComplexColor mFillColors;
        private final long mNativePtr;
        private byte[] mPropertyData;
        ComplexColor mStrokeColors;
        private int[] mThemeAttrs;
        private static final HashMap<String, Integer> sPropertyIndexMap = new HashMap<String, Integer>() {
            {
                put("strokeWidth", 0);
                put("strokeColor", 1);
                put("strokeAlpha", 2);
                put("fillColor", 3);
                put("fillAlpha", 4);
                put("trimPathStart", 5);
                put("trimPathEnd", 6);
                put("trimPathOffset", 7);
            }
        };
        private static final Property<VFullPath, Float> STROKE_WIDTH = new FloatProperty<VFullPath>("strokeWidth") {
            @Override
            public void setValue(VFullPath vFullPath, float f) {
                vFullPath.setStrokeWidth(f);
            }

            @Override
            public Float get(VFullPath vFullPath) {
                return Float.valueOf(vFullPath.getStrokeWidth());
            }
        };
        private static final Property<VFullPath, Integer> STROKE_COLOR = new IntProperty<VFullPath>("strokeColor") {
            @Override
            public void setValue(VFullPath vFullPath, int i) {
                vFullPath.setStrokeColor(i);
            }

            @Override
            public Integer get(VFullPath vFullPath) {
                return Integer.valueOf(vFullPath.getStrokeColor());
            }
        };
        private static final Property<VFullPath, Float> STROKE_ALPHA = new FloatProperty<VFullPath>("strokeAlpha") {
            @Override
            public void setValue(VFullPath vFullPath, float f) {
                vFullPath.setStrokeAlpha(f);
            }

            @Override
            public Float get(VFullPath vFullPath) {
                return Float.valueOf(vFullPath.getStrokeAlpha());
            }
        };
        private static final Property<VFullPath, Integer> FILL_COLOR = new IntProperty<VFullPath>("fillColor") {
            @Override
            public void setValue(VFullPath vFullPath, int i) {
                vFullPath.setFillColor(i);
            }

            @Override
            public Integer get(VFullPath vFullPath) {
                return Integer.valueOf(vFullPath.getFillColor());
            }
        };
        private static final Property<VFullPath, Float> FILL_ALPHA = new FloatProperty<VFullPath>("fillAlpha") {
            @Override
            public void setValue(VFullPath vFullPath, float f) {
                vFullPath.setFillAlpha(f);
            }

            @Override
            public Float get(VFullPath vFullPath) {
                return Float.valueOf(vFullPath.getFillAlpha());
            }
        };
        private static final Property<VFullPath, Float> TRIM_PATH_START = new FloatProperty<VFullPath>("trimPathStart") {
            @Override
            public void setValue(VFullPath vFullPath, float f) {
                vFullPath.setTrimPathStart(f);
            }

            @Override
            public Float get(VFullPath vFullPath) {
                return Float.valueOf(vFullPath.getTrimPathStart());
            }
        };
        private static final Property<VFullPath, Float> TRIM_PATH_END = new FloatProperty<VFullPath>("trimPathEnd") {
            @Override
            public void setValue(VFullPath vFullPath, float f) {
                vFullPath.setTrimPathEnd(f);
            }

            @Override
            public Float get(VFullPath vFullPath) {
                return Float.valueOf(vFullPath.getTrimPathEnd());
            }
        };
        private static final Property<VFullPath, Float> TRIM_PATH_OFFSET = new FloatProperty<VFullPath>("trimPathOffset") {
            @Override
            public void setValue(VFullPath vFullPath, float f) {
                vFullPath.setTrimPathOffset(f);
            }

            @Override
            public Float get(VFullPath vFullPath) {
                return Float.valueOf(vFullPath.getTrimPathOffset());
            }
        };
        private static final HashMap<String, Property> sPropertyMap = new HashMap<String, Property>() {
            {
                put("strokeWidth", VFullPath.STROKE_WIDTH);
                put("strokeColor", VFullPath.STROKE_COLOR);
                put("strokeAlpha", VFullPath.STROKE_ALPHA);
                put("fillColor", VFullPath.FILL_COLOR);
                put("fillAlpha", VFullPath.FILL_ALPHA);
                put("trimPathStart", VFullPath.TRIM_PATH_START);
                put("trimPathEnd", VFullPath.TRIM_PATH_END);
                put("trimPathOffset", VFullPath.TRIM_PATH_OFFSET);
            }
        };

        public VFullPath() {
            this.mStrokeColors = null;
            this.mFillColors = null;
            this.mNativePtr = VectorDrawable.nCreateFullPath();
        }

        public VFullPath(VFullPath vFullPath) {
            super(vFullPath);
            this.mStrokeColors = null;
            this.mFillColors = null;
            this.mNativePtr = VectorDrawable.nCreateFullPath(vFullPath.mNativePtr);
            this.mThemeAttrs = vFullPath.mThemeAttrs;
            this.mStrokeColors = vFullPath.mStrokeColors;
            this.mFillColors = vFullPath.mFillColors;
        }

        @Override
        Property getProperty(String str) {
            Property property = super.getProperty(str);
            if (property != null) {
                return property;
            }
            if (sPropertyMap.containsKey(str)) {
                return sPropertyMap.get(str);
            }
            return null;
        }

        int getPropertyIndex(String str) {
            if (!sPropertyIndexMap.containsKey(str)) {
                return -1;
            }
            return sPropertyIndexMap.get(str).intValue();
        }

        @Override
        public boolean onStateChange(int[] iArr) {
            boolean z;
            if (this.mStrokeColors != null && (this.mStrokeColors instanceof ColorStateList)) {
                int strokeColor = getStrokeColor();
                int colorForState = ((ColorStateList) this.mStrokeColors).getColorForState(iArr, strokeColor);
                z = (strokeColor != colorForState) | false;
                if (strokeColor != colorForState) {
                    VectorDrawable.nSetStrokeColor(this.mNativePtr, colorForState);
                }
            } else {
                z = false;
            }
            if (this.mFillColors != null && (this.mFillColors instanceof ColorStateList)) {
                int fillColor = getFillColor();
                int colorForState2 = ((ColorStateList) this.mFillColors).getColorForState(iArr, fillColor);
                z |= fillColor != colorForState2;
                if (fillColor != colorForState2) {
                    VectorDrawable.nSetFillColor(this.mNativePtr, colorForState2);
                }
            }
            return z;
        }

        @Override
        public boolean isStateful() {
            return (this.mStrokeColors == null && this.mFillColors == null) ? false : true;
        }

        @Override
        public boolean hasFocusStateSpecified() {
            return this.mStrokeColors != null && (this.mStrokeColors instanceof ColorStateList) && ((ColorStateList) this.mStrokeColors).hasFocusStateSpecified() && this.mFillColors != null && (this.mFillColors instanceof ColorStateList) && ((ColorStateList) this.mFillColors).hasFocusStateSpecified();
        }

        @Override
        int getNativeSize() {
            return 264;
        }

        @Override
        public long getNativePtr() {
            return this.mNativePtr;
        }

        @Override
        public void inflate(Resources resources, AttributeSet attributeSet, Resources.Theme theme) {
            TypedArray typedArrayObtainAttributes = Drawable.obtainAttributes(resources, theme, attributeSet, R.styleable.VectorDrawablePath);
            updateStateFromTypedArray(typedArrayObtainAttributes);
            typedArrayObtainAttributes.recycle();
        }

        private void updateStateFromTypedArray(TypedArray typedArray) {
            int i;
            int i2;
            Shader shader;
            int defaultColor;
            int defaultColor2;
            if (this.mPropertyData == null) {
                this.mPropertyData = new byte[48];
            }
            if (!VectorDrawable.nGetFullPathProperties(this.mNativePtr, this.mPropertyData, 48)) {
                throw new RuntimeException("Error: inconsistent property count");
            }
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(this.mPropertyData);
            byteBufferWrap.order(ByteOrder.nativeOrder());
            float f = byteBufferWrap.getFloat(0);
            int i3 = byteBufferWrap.getInt(4);
            float f2 = byteBufferWrap.getFloat(8);
            int i4 = byteBufferWrap.getInt(12);
            float f3 = byteBufferWrap.getFloat(16);
            float f4 = byteBufferWrap.getFloat(20);
            float f5 = byteBufferWrap.getFloat(24);
            float f6 = byteBufferWrap.getFloat(28);
            int i5 = byteBufferWrap.getInt(32);
            int i6 = byteBufferWrap.getInt(36);
            float f7 = byteBufferWrap.getFloat(40);
            int i7 = byteBufferWrap.getInt(44);
            this.mChangingConfigurations |= typedArray.getChangingConfigurations();
            this.mThemeAttrs = typedArray.extractThemeAttrs();
            String string = typedArray.getString(0);
            if (string != null) {
                this.mPathName = string;
                i = i4;
                VectorDrawable.nSetName(this.mNativePtr, this.mPathName);
            } else {
                i = i4;
            }
            String string2 = typedArray.getString(2);
            if (string2 != null) {
                this.mPathData = new PathParser.PathData(string2);
                i2 = i3;
                VectorDrawable.nSetPathString(this.mNativePtr, string2, string2.length());
            } else {
                i2 = i3;
            }
            ComplexColor complexColor = typedArray.getComplexColor(1);
            Shader shader2 = null;
            if (complexColor == null) {
                shader = null;
                defaultColor = i;
            } else {
                if (complexColor instanceof GradientColor) {
                    this.mFillColors = complexColor;
                    shader = ((GradientColor) complexColor).getShader();
                } else {
                    if (complexColor.isStateful()) {
                        this.mFillColors = complexColor;
                    } else {
                        this.mFillColors = null;
                    }
                    shader = null;
                }
                defaultColor = complexColor.getDefaultColor();
            }
            ComplexColor complexColor2 = typedArray.getComplexColor(3);
            if (complexColor2 != null) {
                if (complexColor2 instanceof GradientColor) {
                    this.mStrokeColors = complexColor2;
                    shader2 = ((GradientColor) complexColor2).getShader();
                } else if (complexColor2.isStateful()) {
                    this.mStrokeColors = complexColor2;
                } else {
                    this.mStrokeColors = null;
                }
                defaultColor2 = complexColor2.getDefaultColor();
            } else {
                defaultColor2 = i2;
            }
            VectorDrawable.nUpdateFullPathFillGradient(this.mNativePtr, shader != null ? shader.getNativeInstance() : 0L);
            VectorDrawable.nUpdateFullPathStrokeGradient(this.mNativePtr, shader2 != null ? shader2.getNativeInstance() : 0L);
            float f8 = typedArray.getFloat(12, f3);
            int i8 = typedArray.getInt(8, i5);
            int i9 = typedArray.getInt(9, i6);
            float f9 = typedArray.getFloat(10, f7);
            VectorDrawable.nUpdateFullPathProperties(this.mNativePtr, typedArray.getFloat(4, f), defaultColor2, typedArray.getFloat(11, f2), defaultColor, f8, typedArray.getFloat(5, f4), typedArray.getFloat(6, f5), typedArray.getFloat(7, f6), f9, i8, i9, typedArray.getInt(13, i7));
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null) {
                return true;
            }
            return canComplexColorApplyTheme(this.mFillColors) || canComplexColorApplyTheme(this.mStrokeColors);
        }

        @Override
        public void applyTheme(Resources.Theme theme) {
            if (this.mThemeAttrs != null) {
                TypedArray typedArrayResolveAttributes = theme.resolveAttributes(this.mThemeAttrs, R.styleable.VectorDrawablePath);
                updateStateFromTypedArray(typedArrayResolveAttributes);
                typedArrayResolveAttributes.recycle();
            }
            boolean zCanComplexColorApplyTheme = canComplexColorApplyTheme(this.mFillColors);
            boolean zCanComplexColorApplyTheme2 = canComplexColorApplyTheme(this.mStrokeColors);
            if (zCanComplexColorApplyTheme) {
                this.mFillColors = this.mFillColors.obtainForTheme(theme);
                if (this.mFillColors instanceof GradientColor) {
                    VectorDrawable.nUpdateFullPathFillGradient(this.mNativePtr, ((GradientColor) this.mFillColors).getShader().getNativeInstance());
                } else if (this.mFillColors instanceof ColorStateList) {
                    VectorDrawable.nSetFillColor(this.mNativePtr, this.mFillColors.getDefaultColor());
                }
            }
            if (zCanComplexColorApplyTheme2) {
                this.mStrokeColors = this.mStrokeColors.obtainForTheme(theme);
                if (this.mStrokeColors instanceof GradientColor) {
                    VectorDrawable.nUpdateFullPathStrokeGradient(this.mNativePtr, ((GradientColor) this.mStrokeColors).getShader().getNativeInstance());
                } else if (this.mStrokeColors instanceof ColorStateList) {
                    VectorDrawable.nSetStrokeColor(this.mNativePtr, this.mStrokeColors.getDefaultColor());
                }
            }
        }

        private boolean canComplexColorApplyTheme(ComplexColor complexColor) {
            return complexColor != null && complexColor.canApplyTheme();
        }

        int getStrokeColor() {
            if (isTreeValid()) {
                return VectorDrawable.nGetStrokeColor(this.mNativePtr);
            }
            return 0;
        }

        void setStrokeColor(int i) {
            this.mStrokeColors = null;
            if (isTreeValid()) {
                VectorDrawable.nSetStrokeColor(this.mNativePtr, i);
            }
        }

        float getStrokeWidth() {
            if (isTreeValid()) {
                return VectorDrawable.nGetStrokeWidth(this.mNativePtr);
            }
            return 0.0f;
        }

        void setStrokeWidth(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetStrokeWidth(this.mNativePtr, f);
            }
        }

        float getStrokeAlpha() {
            if (isTreeValid()) {
                return VectorDrawable.nGetStrokeAlpha(this.mNativePtr);
            }
            return 0.0f;
        }

        void setStrokeAlpha(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetStrokeAlpha(this.mNativePtr, f);
            }
        }

        int getFillColor() {
            if (isTreeValid()) {
                return VectorDrawable.nGetFillColor(this.mNativePtr);
            }
            return 0;
        }

        void setFillColor(int i) {
            this.mFillColors = null;
            if (isTreeValid()) {
                VectorDrawable.nSetFillColor(this.mNativePtr, i);
            }
        }

        float getFillAlpha() {
            if (isTreeValid()) {
                return VectorDrawable.nGetFillAlpha(this.mNativePtr);
            }
            return 0.0f;
        }

        void setFillAlpha(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetFillAlpha(this.mNativePtr, f);
            }
        }

        float getTrimPathStart() {
            if (isTreeValid()) {
                return VectorDrawable.nGetTrimPathStart(this.mNativePtr);
            }
            return 0.0f;
        }

        void setTrimPathStart(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetTrimPathStart(this.mNativePtr, f);
            }
        }

        float getTrimPathEnd() {
            if (isTreeValid()) {
                return VectorDrawable.nGetTrimPathEnd(this.mNativePtr);
            }
            return 0.0f;
        }

        void setTrimPathEnd(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetTrimPathEnd(this.mNativePtr, f);
            }
        }

        float getTrimPathOffset() {
            if (isTreeValid()) {
                return VectorDrawable.nGetTrimPathOffset(this.mNativePtr);
            }
            return 0.0f;
        }

        void setTrimPathOffset(float f) {
            if (isTreeValid()) {
                VectorDrawable.nSetTrimPathOffset(this.mNativePtr, f);
            }
        }
    }

    static abstract class VObject {
        VirtualRefBasePtr mTreePtr = null;

        abstract void applyTheme(Resources.Theme theme);

        abstract boolean canApplyTheme();

        abstract long getNativePtr();

        abstract int getNativeSize();

        abstract Property getProperty(String str);

        abstract boolean hasFocusStateSpecified();

        abstract void inflate(Resources resources, AttributeSet attributeSet, Resources.Theme theme);

        abstract boolean isStateful();

        abstract boolean onStateChange(int[] iArr);

        VObject() {
        }

        boolean isTreeValid() {
            return (this.mTreePtr == null || this.mTreePtr.get() == 0) ? false : true;
        }

        void setTree(VirtualRefBasePtr virtualRefBasePtr) {
            this.mTreePtr = virtualRefBasePtr;
        }
    }
}

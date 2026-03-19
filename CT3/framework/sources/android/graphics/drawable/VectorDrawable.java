package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.ComplexColor;
import android.content.res.GradientColor;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.util.PathParser;
import android.util.Xml;
import com.android.internal.R;
import com.android.internal.util.VirtualRefBasePtr;
import dalvik.system.BlockGuard;
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

    VectorDrawable(VectorDrawableState state, Resources res, VectorDrawable vectorDrawable) {
        this(state, res);
    }

    private static native void nAddChild(long j, long j2);

    private static native long nCreateClipPath();

    private static native long nCreateClipPath(long j);

    private static native long nCreateFullPath();

    private static native long nCreateFullPath(long j);

    private static native long nCreateGroup();

    private static native long nCreateGroup(long j);

    private static native long nCreateTree(long j);

    private static native long nCreateTreeFromCopy(long j, long j2);

    private static native int nDraw(long j, long j2, long j3, Rect rect, boolean z, boolean z2);

    private static native float nGetFillAlpha(long j);

    private static native int nGetFillColor(long j);

    private static native boolean nGetFullPathProperties(long j, byte[] bArr, int i);

    private static native boolean nGetGroupProperties(long j, float[] fArr, int i);

    private static native float nGetPivotX(long j);

    private static native float nGetPivotY(long j);

    private static native float nGetRootAlpha(long j);

    private static native float nGetRotation(long j);

    private static native float nGetScaleX(long j);

    private static native float nGetScaleY(long j);

    private static native float nGetStrokeAlpha(long j);

    private static native int nGetStrokeColor(long j);

    private static native float nGetStrokeWidth(long j);

    private static native float nGetTranslateX(long j);

    private static native float nGetTranslateY(long j);

    private static native float nGetTrimPathEnd(long j);

    private static native float nGetTrimPathOffset(long j);

    private static native float nGetTrimPathStart(long j);

    private static native void nSetAllowCaching(long j, boolean z);

    private static native void nSetFillAlpha(long j, float f);

    private static native void nSetFillColor(long j, int i);

    private static native void nSetName(long j, String str);

    private static native void nSetPathData(long j, long j2);

    private static native void nSetPathString(long j, String str, int i);

    private static native void nSetPivotX(long j, float f);

    private static native void nSetPivotY(long j, float f);

    private static native void nSetRendererViewportSize(long j, float f, float f2);

    private static native boolean nSetRootAlpha(long j, float f);

    private static native void nSetRotation(long j, float f);

    private static native void nSetScaleX(long j, float f);

    private static native void nSetScaleY(long j, float f);

    private static native void nSetStrokeAlpha(long j, float f);

    private static native void nSetStrokeColor(long j, int i);

    private static native void nSetStrokeWidth(long j, float f);

    private static native void nSetTranslateX(long j, float f);

    private static native void nSetTranslateY(long j, float f);

    private static native void nSetTrimPathEnd(long j, float f);

    private static native void nSetTrimPathOffset(long j, float f);

    private static native void nSetTrimPathStart(long j, float f);

    private static native void nUpdateFullPathFillGradient(long j, long j2);

    private static native void nUpdateFullPathProperties(long j, float f, int i, float f2, int i2, float f3, float f4, float f5, float f6, float f7, int i3, int i4, int i5);

    private static native void nUpdateFullPathStrokeGradient(long j, long j2);

    private static native void nUpdateGroupProperties(long j, float f, float f2, float f3, float f4, float f5, float f6, float f7);

    public VectorDrawable() {
        this(new VectorDrawableState(), null);
    }

    private VectorDrawable(VectorDrawableState state, Resources res) {
        this.mDpiScaledWidth = 0;
        this.mDpiScaledHeight = 0;
        this.mDpiScaledInsets = Insets.NONE;
        this.mDpiScaledDirty = true;
        this.mTmpBounds = new Rect();
        this.mVectorState = state;
        updateLocalState(res);
    }

    private void updateLocalState(Resources res) {
        int density = Drawable.resolveDensity(res, this.mVectorState.mDensity);
        if (this.mTargetDensity != density) {
            this.mTargetDensity = density;
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

    Object getTargetByName(String name) {
        return this.mVectorState.mVGTargetsMap.get(name);
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        this.mVectorState.mChangingConfigurations = getChangingConfigurations();
        return this.mVectorState;
    }

    @Override
    public void draw(Canvas canvas) {
        int deltaInBytes;
        copyBounds(this.mTmpBounds);
        if (this.mTmpBounds.width() <= 0 || this.mTmpBounds.height() <= 0) {
            return;
        }
        ColorFilter colorFilter = this.mColorFilter == null ? this.mTintFilter : this.mColorFilter;
        long colorFilterNativeInstance = colorFilter == null ? 0L : colorFilter.native_instance;
        boolean canReuseCache = this.mVectorState.canReuseCache();
        int pixelCount = nDraw(this.mVectorState.getNativeRenderer(), canvas.getNativeCanvasWrapper(), colorFilterNativeInstance, this.mTmpBounds, needMirroring(), canReuseCache);
        if (pixelCount == 0) {
            return;
        }
        if (canvas.isHardwareAccelerated()) {
            deltaInBytes = (pixelCount - this.mVectorState.mLastHWCachePixelCount) * 4;
            this.mVectorState.mLastHWCachePixelCount = pixelCount;
        } else {
            deltaInBytes = (pixelCount - this.mVectorState.mLastSWCachePixelCount) * 4;
            this.mVectorState.mLastSWCachePixelCount = pixelCount;
        }
        if (deltaInBytes > 0) {
            VMRuntime.getRuntime().registerNativeAllocation(deltaInBytes);
            this.mVectorState.bt.allocate(deltaInBytes);
            this.mVectorState.allocatedOnDraw += deltaInBytes;
            return;
        }
        if (deltaInBytes >= 0) {
            return;
        }
        VMRuntime.getRuntime().registerNativeFree(-deltaInBytes);
        this.mVectorState.bt.free(-deltaInBytes);
        this.mVectorState.freedOnDraw += -deltaInBytes;
    }

    @Override
    public int getAlpha() {
        return (int) (this.mVectorState.getAlpha() * 255.0f);
    }

    @Override
    public void setAlpha(int alpha) {
        if (!this.mVectorState.setAlpha(alpha / 255.0f)) {
            return;
        }
        invalidateSelf();
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
    public void setTintList(ColorStateList tint) {
        VectorDrawableState state = this.mVectorState;
        if (state.mTint == tint) {
            return;
        }
        state.mTint = tint;
        this.mTintFilter = updateTintFilter(this.mTintFilter, tint, state.mTintMode);
        invalidateSelf();
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        VectorDrawableState state = this.mVectorState;
        if (state.mTintMode == tintMode) {
            return;
        }
        state.mTintMode = tintMode;
        this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, tintMode);
        invalidateSelf();
    }

    @Override
    public boolean isStateful() {
        if (super.isStateful()) {
            return true;
        }
        if (this.mVectorState != null) {
            return this.mVectorState.isStateful();
        }
        return false;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        boolean changed = false;
        if (isStateful()) {
            mutate();
        }
        VectorDrawableState state = this.mVectorState;
        if (state.onStateChange(stateSet)) {
            changed = true;
            state.mCacheDirty = true;
        }
        if (state.mTint != null && state.mTintMode != null) {
            this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
            return true;
        }
        return changed;
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
        Insets opticalInsets = this.mVectorState.mOpticalInsets;
        int sourceDensity = this.mVectorState.mDensity;
        int targetDensity = this.mTargetDensity;
        if (targetDensity != sourceDensity) {
            this.mDpiScaledWidth = Drawable.scaleFromDensity((int) this.mVectorState.mBaseWidth, sourceDensity, targetDensity, true);
            this.mDpiScaledHeight = Drawable.scaleFromDensity((int) this.mVectorState.mBaseHeight, sourceDensity, targetDensity, true);
            int left = Drawable.scaleFromDensity(opticalInsets.left, sourceDensity, targetDensity, false);
            int right = Drawable.scaleFromDensity(opticalInsets.right, sourceDensity, targetDensity, false);
            int top = Drawable.scaleFromDensity(opticalInsets.top, sourceDensity, targetDensity, false);
            int bottom = Drawable.scaleFromDensity(opticalInsets.bottom, sourceDensity, targetDensity, false);
            this.mDpiScaledInsets = Insets.of(left, top, right, bottom);
        } else {
            this.mDpiScaledWidth = (int) this.mVectorState.mBaseWidth;
            this.mDpiScaledHeight = (int) this.mVectorState.mBaseHeight;
            this.mDpiScaledInsets = opticalInsets;
        }
        this.mDpiScaledDirty = false;
    }

    @Override
    public boolean canApplyTheme() {
        if (this.mVectorState == null || !this.mVectorState.canApplyTheme()) {
            return super.canApplyTheme();
        }
        return true;
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        VectorDrawableState state = this.mVectorState;
        if (state == null) {
            return;
        }
        boolean changedDensity = this.mVectorState.setDensity(Drawable.resolveDensity(t.getResources(), 0));
        this.mDpiScaledDirty |= changedDensity;
        if (state.mThemeAttrs != null) {
            TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.VectorDrawable);
            try {
                try {
                    state.mCacheDirty = true;
                    updateStateFromTypedArray(a);
                    a.recycle();
                    this.mDpiScaledDirty = true;
                } catch (XmlPullParserException e) {
                    throw new RuntimeException(e);
                }
            } catch (Throwable th) {
                a.recycle();
                throw th;
            }
        }
        if (state.mTint != null && state.mTint.canApplyTheme()) {
            state.mTint = state.mTint.obtainForTheme(t);
        }
        if (this.mVectorState != null && this.mVectorState.canApplyTheme()) {
            this.mVectorState.applyTheme(t);
        }
        updateLocalState(t.getResources());
    }

    public float getPixelSize() {
        if (this.mVectorState == null || this.mVectorState.mBaseWidth == 0.0f || this.mVectorState.mBaseHeight == 0.0f || this.mVectorState.mViewportHeight == 0.0f || this.mVectorState.mViewportWidth == 0.0f) {
            return 1.0f;
        }
        float intrinsicWidth = this.mVectorState.mBaseWidth;
        float intrinsicHeight = this.mVectorState.mBaseHeight;
        float viewportWidth = this.mVectorState.mViewportWidth;
        float viewportHeight = this.mVectorState.mViewportHeight;
        float scaleX = viewportWidth / intrinsicWidth;
        float scaleY = viewportHeight / intrinsicHeight;
        return Math.min(scaleX, scaleY);
    }

    public static VectorDrawable create(Resources resources, int rid) {
        int type;
        try {
            XmlPullParser parser = resources.getXml(rid);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type != 2) {
                throw new XmlPullParserException("No start tag found");
            }
            VectorDrawable drawable = new VectorDrawable();
            drawable.inflate(resources, parser, attrs);
            return drawable;
        } catch (IOException e) {
            Log.e(LOGTAG, "parser error", e);
            return null;
        } catch (XmlPullParserException e2) {
            Log.e(LOGTAG, "parser error", e2);
            return null;
        }
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        if (this.mVectorState.mRootGroup != null || this.mVectorState.mNativeTree != null) {
            if (this.mVectorState.mRootGroup != null) {
                int nativeGroupChildrenSize = this.mVectorState.mRootGroup.getNativeGroupChildrenSize();
                VMRuntime.getRuntime().registerNativeFree(nativeGroupChildrenSize);
                this.mVectorState.bt.free(nativeGroupChildrenSize);
                this.mVectorState.freedRootGroupOnInflate += nativeGroupChildrenSize;
                this.mVectorState.mRootGroup.setTree(null);
            }
            this.mVectorState.mRootGroup = new VGroup();
            if (this.mVectorState.mNativeTree != null) {
                VMRuntime.getRuntime().registerNativeFree(316);
                this.mVectorState.bt.free(316L);
                this.mVectorState.freedNativeTreeOnInflate += 316;
                this.mVectorState.mNativeTree.release();
            }
            this.mVectorState.createNativeTree(this.mVectorState.mRootGroup);
        }
        VectorDrawableState state = this.mVectorState;
        state.setDensity(Drawable.resolveDensity(r, 0));
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.VectorDrawable);
        updateStateFromTypedArray(a);
        a.recycle();
        this.mDpiScaledDirty = true;
        state.mCacheDirty = true;
        inflateChildElements(r, parser, attrs, theme);
        state.onTreeConstructionFinished();
        updateLocalState(r);
    }

    private void updateStateFromTypedArray(TypedArray a) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException {
        VectorDrawableState state = this.mVectorState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        int tintMode = a.getInt(6, -1);
        if (tintMode != -1) {
            state.mTintMode = Drawable.parseTintMode(tintMode, PorterDuff.Mode.SRC_IN);
        }
        ColorStateList tint = a.getColorStateList(1);
        if (tint != null) {
            state.mTint = tint;
        }
        state.mAutoMirrored = a.getBoolean(5, state.mAutoMirrored);
        float viewportWidth = a.getFloat(7, state.mViewportWidth);
        float viewportHeight = a.getFloat(8, state.mViewportHeight);
        state.setViewportSize(viewportWidth, viewportHeight);
        if (state.mViewportWidth <= 0.0f) {
            throw new XmlPullParserException(a.getPositionDescription() + "<vector> tag requires viewportWidth > 0");
        }
        if (state.mViewportHeight <= 0.0f) {
            throw new XmlPullParserException(a.getPositionDescription() + "<vector> tag requires viewportHeight > 0");
        }
        state.mBaseWidth = a.getDimension(3, state.mBaseWidth);
        state.mBaseHeight = a.getDimension(2, state.mBaseHeight);
        if (state.mBaseWidth <= 0.0f) {
            throw new XmlPullParserException(a.getPositionDescription() + "<vector> tag requires width > 0");
        }
        if (state.mBaseHeight <= 0.0f) {
            throw new XmlPullParserException(a.getPositionDescription() + "<vector> tag requires height > 0");
        }
        int insetLeft = a.getDimensionPixelOffset(9, state.mOpticalInsets.left);
        int insetTop = a.getDimensionPixelOffset(10, state.mOpticalInsets.top);
        int insetRight = a.getDimensionPixelOffset(11, state.mOpticalInsets.right);
        int insetBottom = a.getDimensionPixelOffset(12, state.mOpticalInsets.bottom);
        state.mOpticalInsets = Insets.of(insetLeft, insetTop, insetRight, insetBottom);
        float alphaInFloat = a.getFloat(4, state.getAlpha());
        state.setAlpha(alphaInFloat);
        String name = a.getString(0);
        if (name == null) {
            return;
        }
        state.mRootName = name;
        state.mVGTargetsMap.put(name, state);
    }

    private void inflateChildElements(Resources res, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        VectorDrawableState state = this.mVectorState;
        boolean noPathTag = true;
        Stack<VGroup> groupStack = new Stack<>();
        groupStack.push(state.mRootGroup);
        int eventType = parser.getEventType();
        while (eventType != 1) {
            if (eventType == 2) {
                String tagName = parser.getName();
                VGroup currentGroup = groupStack.peek();
                if (SHAPE_PATH.equals(tagName)) {
                    VFullPath path = new VFullPath();
                    path.inflate(res, attrs, theme);
                    currentGroup.addChild(path);
                    if (path.getPathName() != null) {
                        state.mVGTargetsMap.put(path.getPathName(), path);
                    }
                    noPathTag = false;
                    state.mChangingConfigurations |= path.mChangingConfigurations;
                } else if (SHAPE_CLIP_PATH.equals(tagName)) {
                    VClipPath path2 = new VClipPath();
                    path2.inflate(res, attrs, theme);
                    currentGroup.addChild(path2);
                    if (path2.getPathName() != null) {
                        state.mVGTargetsMap.put(path2.getPathName(), path2);
                    }
                    state.mChangingConfigurations |= path2.mChangingConfigurations;
                } else if ("group".equals(tagName)) {
                    VGroup newChildGroup = new VGroup();
                    newChildGroup.inflate(res, attrs, theme);
                    currentGroup.addChild(newChildGroup);
                    groupStack.push(newChildGroup);
                    if (newChildGroup.getGroupName() != null) {
                        state.mVGTargetsMap.put(newChildGroup.getGroupName(), newChildGroup);
                    }
                    state.mChangingConfigurations |= newChildGroup.mChangingConfigurations;
                }
            } else if (eventType == 3 && "group".equals(parser.getName())) {
                groupStack.pop();
            }
            eventType = parser.next();
        }
        if (!noPathTag) {
            return;
        }
        StringBuffer tag = new StringBuffer();
        if (tag.length() > 0) {
            tag.append(" or ");
        }
        tag.append(SHAPE_PATH);
        throw new XmlPullParserException("no " + ((Object) tag) + " defined");
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mVectorState.getChangingConfigurations();
    }

    void setAllowCaching(boolean allowCaching) {
        nSetAllowCaching(this.mVectorState.getNativeRenderer(), allowCaching);
    }

    private boolean needMirroring() {
        return isAutoMirrored() && getLayoutDirection() == 1;
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        if (this.mVectorState.mAutoMirrored == mirrored) {
            return;
        }
        this.mVectorState.mAutoMirrored = mirrored;
        invalidateSelf();
    }

    @Override
    public boolean isAutoMirrored() {
        return this.mVectorState.mAutoMirrored;
    }

    static class BalanceTracker {
        private long balance = 0;

        BalanceTracker() {
        }

        public void allocate(long size) {
            this.balance += size;
        }

        public void free(long size) {
            this.balance -= size;
        }

        public long get() {
            return this.balance;
        }
    }

    static class VectorDrawableState extends Drawable.ConstantState {
        private static final int NATIVE_ALLOCATION_SIZE = 316;
        public int allocatedOnCreateNativeTree;
        public int allocatedOnCreateNativeTreeFromCopy;
        public int allocatedOnDraw;
        public int allocatedOnTreeConstructionFinished;
        public long allocatedSizeInTotal;
        public BalanceTracker bt;
        public int freedNativeTreeOnInflate;
        public int freedOnDraw;
        public int freedRootGroupOnInflate;
        public long freedSizeInFinalize;
        private int mAllocationOfAllNodes;
        boolean mAutoMirrored;
        float mBaseHeight;
        float mBaseWidth;
        boolean mCacheDirty;
        boolean mCachedAutoMirrored;
        int[] mCachedThemeAttrs;
        ColorStateList mCachedTint;
        PorterDuff.Mode mCachedTintMode;
        int mChangingConfigurations;
        int mDensity;
        int mLastHWCachePixelCount;
        int mLastSWCachePixelCount;
        VirtualRefBasePtr mNativeTree;
        Insets mOpticalInsets;
        VGroup mRootGroup;
        String mRootName;
        int[] mThemeAttrs;
        ColorStateList mTint;
        PorterDuff.Mode mTintMode;
        final ArrayMap<String, Object> mVGTargetsMap;
        float mViewportHeight;
        float mViewportWidth;

        public VectorDrawableState(VectorDrawableState copy) {
            this.allocatedOnCreateNativeTree = 0;
            this.allocatedOnCreateNativeTreeFromCopy = 0;
            this.allocatedOnTreeConstructionFinished = 0;
            this.allocatedOnDraw = 0;
            this.freedOnDraw = 0;
            this.freedRootGroupOnInflate = 0;
            this.freedNativeTreeOnInflate = 0;
            this.allocatedSizeInTotal = 0L;
            this.freedSizeInFinalize = 0L;
            this.bt = new BalanceTracker();
            this.mTint = null;
            this.mTintMode = VectorDrawable.DEFAULT_TINT_MODE;
            this.mBaseWidth = 0.0f;
            this.mBaseHeight = 0.0f;
            this.mViewportWidth = 0.0f;
            this.mViewportHeight = 0.0f;
            this.mOpticalInsets = Insets.NONE;
            this.mRootName = null;
            this.mNativeTree = null;
            this.mDensity = 160;
            this.mVGTargetsMap = new ArrayMap<>();
            this.mLastSWCachePixelCount = 0;
            this.mLastHWCachePixelCount = 0;
            this.mAllocationOfAllNodes = 0;
            if (copy == null) {
                return;
            }
            this.mThemeAttrs = copy.mThemeAttrs;
            this.mChangingConfigurations = copy.mChangingConfigurations;
            this.mTint = copy.mTint;
            this.mTintMode = copy.mTintMode;
            this.mAutoMirrored = copy.mAutoMirrored;
            this.mRootGroup = new VGroup(copy.mRootGroup, this.mVGTargetsMap);
            createNativeTreeFromCopy(copy, this.mRootGroup);
            this.mBaseWidth = copy.mBaseWidth;
            this.mBaseHeight = copy.mBaseHeight;
            setViewportSize(copy.mViewportWidth, copy.mViewportHeight);
            this.mOpticalInsets = copy.mOpticalInsets;
            this.mRootName = copy.mRootName;
            this.mDensity = copy.mDensity;
            if (copy.mRootName != null) {
                this.mVGTargetsMap.put(copy.mRootName, this);
            }
            onTreeConstructionFinished();
        }

        private void createNativeTree(VGroup rootGroup) {
            this.mNativeTree = new VirtualRefBasePtr(VectorDrawable.nCreateTree(rootGroup.mNativePtr));
            VMRuntime.getRuntime().registerNativeAllocation(NATIVE_ALLOCATION_SIZE);
            this.bt.allocate(316L);
            this.allocatedOnCreateNativeTree += NATIVE_ALLOCATION_SIZE;
        }

        private void createNativeTreeFromCopy(VectorDrawableState copy, VGroup rootGroup) {
            this.mNativeTree = new VirtualRefBasePtr(VectorDrawable.nCreateTreeFromCopy(copy.mNativeTree.get(), rootGroup.mNativePtr));
            VMRuntime.getRuntime().registerNativeAllocation(NATIVE_ALLOCATION_SIZE);
            this.bt.allocate(316L);
            this.allocatedOnCreateNativeTreeFromCopy += NATIVE_ALLOCATION_SIZE;
        }

        void onTreeConstructionFinished() {
            this.mRootGroup.setTree(this.mNativeTree);
            this.mAllocationOfAllNodes = this.mRootGroup.getNativeSize();
            VMRuntime.getRuntime().registerNativeAllocation(this.mAllocationOfAllNodes);
            this.bt.allocate(this.mAllocationOfAllNodes);
            this.allocatedOnTreeConstructionFinished += this.mAllocationOfAllNodes;
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

        public void applyTheme(Resources.Theme t) {
            this.mRootGroup.applyTheme(t);
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null || ((this.mRootGroup != null && this.mRootGroup.canApplyTheme()) || (this.mTint != null && this.mTint.canApplyTheme()))) {
                return true;
            }
            return super.canApplyTheme();
        }

        public VectorDrawableState() {
            this.allocatedOnCreateNativeTree = 0;
            this.allocatedOnCreateNativeTreeFromCopy = 0;
            this.allocatedOnTreeConstructionFinished = 0;
            this.allocatedOnDraw = 0;
            this.freedOnDraw = 0;
            this.freedRootGroupOnInflate = 0;
            this.freedNativeTreeOnInflate = 0;
            this.allocatedSizeInTotal = 0L;
            this.freedSizeInFinalize = 0L;
            this.bt = new BalanceTracker();
            this.mTint = null;
            this.mTintMode = VectorDrawable.DEFAULT_TINT_MODE;
            this.mBaseWidth = 0.0f;
            this.mBaseHeight = 0.0f;
            this.mViewportWidth = 0.0f;
            this.mViewportHeight = 0.0f;
            this.mOpticalInsets = Insets.NONE;
            this.mRootName = null;
            this.mNativeTree = null;
            this.mDensity = 160;
            this.mVGTargetsMap = new ArrayMap<>();
            this.mLastSWCachePixelCount = 0;
            this.mLastHWCachePixelCount = 0;
            this.mAllocationOfAllNodes = 0;
            this.mRootGroup = new VGroup();
            createNativeTree(this.mRootGroup);
        }

        @Override
        public Drawable newDrawable() {
            return new VectorDrawable(this, null, 0 == true ? 1 : 0);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new VectorDrawable(this, res, null);
        }

        @Override
        public int getChangingConfigurations() {
            return (this.mTint != null ? this.mTint.getChangingConfigurations() : 0) | this.mChangingConfigurations;
        }

        public boolean isStateful() {
            if (this.mTint != null && this.mTint.isStateful()) {
                return true;
            }
            if (this.mRootGroup != null) {
                return this.mRootGroup.isStateful();
            }
            return false;
        }

        void setViewportSize(float viewportWidth, float viewportHeight) {
            this.mViewportWidth = viewportWidth;
            this.mViewportHeight = viewportHeight;
            VectorDrawable.nSetRendererViewportSize(getNativeRenderer(), viewportWidth, viewportHeight);
        }

        public final boolean setDensity(int targetDensity) {
            if (this.mDensity != targetDensity) {
                int sourceDensity = this.mDensity;
                this.mDensity = targetDensity;
                applyDensityScaling(sourceDensity, targetDensity);
                return true;
            }
            return false;
        }

        private void applyDensityScaling(int sourceDensity, int targetDensity) {
            this.mBaseWidth = Drawable.scaleFromDensity(this.mBaseWidth, sourceDensity, targetDensity);
            this.mBaseHeight = Drawable.scaleFromDensity(this.mBaseHeight, sourceDensity, targetDensity);
            int insetLeft = Drawable.scaleFromDensity(this.mOpticalInsets.left, sourceDensity, targetDensity, false);
            int insetTop = Drawable.scaleFromDensity(this.mOpticalInsets.top, sourceDensity, targetDensity, false);
            int insetRight = Drawable.scaleFromDensity(this.mOpticalInsets.right, sourceDensity, targetDensity, false);
            int insetBottom = Drawable.scaleFromDensity(this.mOpticalInsets.bottom, sourceDensity, targetDensity, false);
            this.mOpticalInsets = Insets.of(insetLeft, insetTop, insetRight, insetBottom);
        }

        public boolean onStateChange(int[] stateSet) {
            return this.mRootGroup.onStateChange(stateSet);
        }

        public void finalize() throws Exception {
            super.finalize();
            int bitmapCacheSize = (this.mLastHWCachePixelCount * 4) + (this.mLastSWCachePixelCount * 4);
            this.allocatedSizeInTotal = this.allocatedOnCreateNativeTree + this.allocatedOnCreateNativeTreeFromCopy + this.allocatedOnTreeConstructionFinished + this.allocatedOnDraw;
            this.freedSizeInFinalize = this.mAllocationOfAllNodes + NATIVE_ALLOCATION_SIZE + bitmapCacheSize;
            this.bt.free(this.freedSizeInFinalize);
            try {
                VMRuntime.getRuntime().registerNativeFree(this.mAllocationOfAllNodes + NATIVE_ALLOCATION_SIZE + bitmapCacheSize);
            } catch (Exception e) {
                Log.e("VectorDrawable", "---balance tracker find: " + this.bt.get() + " bytes remained\n---Allocated size in total: " + this.allocatedSizeInTotal + " bytes\nallocated On Create Native Tree: " + this.allocatedOnCreateNativeTree + " bytes\nallocated On Create Native Tree From Copy: " + this.allocatedOnCreateNativeTreeFromCopy + " bytes\nallocated On Tree Construction Finished: " + this.allocatedOnTreeConstructionFinished + " bytes\nallocated On Draw: " + this.allocatedOnDraw + " bytes\n---vm will free size in finalize: " + this.freedSizeInFinalize + " bytes\n---before finalize, freed on draw: " + this.freedOnDraw + " bytes  freed RootGroup On Inflate: " + this.freedRootGroupOnInflate + " bytes\n  freed NativeTree On Inflate: " + this.freedNativeTreeOnInflate + " bytes\n---Last HWCache Pixel Count after HWUI draw: " + this.mLastHWCachePixelCount + "\n---Last SWCache Pixel Count after sw draw: " + this.mLastSWCachePixelCount + "\n");
                throw e;
            }
        }

        public boolean setAlpha(float alpha) {
            return VectorDrawable.nSetRootAlpha(this.mNativeTree.get(), alpha);
        }

        public float getAlpha() {
            return VectorDrawable.nGetRootAlpha(this.mNativeTree.get());
        }
    }

    static class VGroup extends VObject {
        private static final int NATIVE_ALLOCATION_SIZE = 100;
        private static final int PIVOT_X_INDEX = 1;
        private static final int PIVOT_Y_INDEX = 2;
        private static final int ROTATE_INDEX = 0;
        private static final int SCALE_X_INDEX = 3;
        private static final int SCALE_Y_INDEX = 4;
        private static final int TRANSFORM_PROPERTY_COUNT = 7;
        private static final int TRANSLATE_X_INDEX = 5;
        private static final int TRANSLATE_Y_INDEX = 6;
        private static final HashMap<String, Integer> sPropertyMap = new HashMap<String, Integer>() {
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
        private int mChangingConfigurations;
        private final ArrayList<VObject> mChildren;
        private String mGroupName;
        private boolean mIsStateful;
        private final long mNativePtr;
        private int[] mThemeAttrs;
        private float[] mTransform;

        static int getPropertyIndex(String propertyName) {
            if (sPropertyMap.containsKey(propertyName)) {
                return sPropertyMap.get(propertyName).intValue();
            }
            return -1;
        }

        public VGroup(VGroup copy, ArrayMap<String, Object> targetsMap) {
            VPath newPath;
            this.mChildren = new ArrayList<>();
            this.mGroupName = null;
            this.mIsStateful = copy.mIsStateful;
            this.mThemeAttrs = copy.mThemeAttrs;
            this.mGroupName = copy.mGroupName;
            this.mChangingConfigurations = copy.mChangingConfigurations;
            if (this.mGroupName != null) {
                targetsMap.put(this.mGroupName, this);
            }
            this.mNativePtr = VectorDrawable.nCreateGroup(copy.mNativePtr);
            ArrayList<VObject> children = copy.mChildren;
            for (int i = 0; i < children.size(); i++) {
                VObject copyChild = children.get(i);
                if (copyChild instanceof VGroup) {
                    VGroup copyGroup = (VGroup) copyChild;
                    addChild(new VGroup(copyGroup, targetsMap));
                } else {
                    if (copyChild instanceof VFullPath) {
                        newPath = new VFullPath((VFullPath) copyChild);
                    } else if (copyChild instanceof VClipPath) {
                        newPath = new VClipPath((VClipPath) copyChild);
                    } else {
                        throw new IllegalStateException("Unknown object in the tree!");
                    }
                    addChild(newPath);
                    if (newPath.mPathName != null) {
                        targetsMap.put(newPath.mPathName, newPath);
                    }
                }
            }
        }

        public VGroup() {
            this.mChildren = new ArrayList<>();
            this.mGroupName = null;
            this.mNativePtr = VectorDrawable.nCreateGroup();
        }

        public String getGroupName() {
            return this.mGroupName;
        }

        public void addChild(VObject child) {
            VectorDrawable.nAddChild(this.mNativePtr, child.getNativePtr());
            this.mChildren.add(child);
            this.mIsStateful |= child.isStateful();
        }

        @Override
        public void setTree(VirtualRefBasePtr treeRoot) {
            super.setTree(treeRoot);
            for (int i = 0; i < this.mChildren.size(); i++) {
                this.mChildren.get(i).setTree(treeRoot);
            }
        }

        @Override
        public long getNativePtr() {
            return this.mNativePtr;
        }

        @Override
        public void inflate(Resources res, AttributeSet attrs, Resources.Theme theme) {
            TypedArray a = VectorDrawable.obtainAttributes(res, theme, attrs, R.styleable.VectorDrawableGroup);
            updateStateFromTypedArray(a);
            a.recycle();
        }

        void updateStateFromTypedArray(TypedArray a) {
            this.mChangingConfigurations |= a.getChangingConfigurations();
            this.mThemeAttrs = a.extractThemeAttrs();
            if (this.mTransform == null) {
                this.mTransform = new float[7];
            }
            boolean success = VectorDrawable.nGetGroupProperties(this.mNativePtr, this.mTransform, 7);
            if (!success) {
                throw new RuntimeException("Error: inconsistent property count");
            }
            float rotate = a.getFloat(5, this.mTransform[0]);
            float pivotX = a.getFloat(1, this.mTransform[1]);
            float pivotY = a.getFloat(2, this.mTransform[2]);
            float scaleX = a.getFloat(3, this.mTransform[3]);
            float scaleY = a.getFloat(4, this.mTransform[4]);
            float translateX = a.getFloat(6, this.mTransform[5]);
            float translateY = a.getFloat(7, this.mTransform[6]);
            String groupName = a.getString(0);
            if (groupName != null) {
                this.mGroupName = groupName;
                VectorDrawable.nSetName(this.mNativePtr, this.mGroupName);
            }
            VectorDrawable.nUpdateGroupProperties(this.mNativePtr, rotate, pivotX, pivotY, scaleX, scaleY, translateX, translateY);
        }

        @Override
        public boolean onStateChange(int[] stateSet) {
            boolean changed = false;
            ArrayList<VObject> children = this.mChildren;
            int count = children.size();
            for (int i = 0; i < count; i++) {
                VObject child = children.get(i);
                if (child.isStateful()) {
                    changed |= child.onStateChange(stateSet);
                }
            }
            return changed;
        }

        @Override
        public boolean isStateful() {
            return this.mIsStateful;
        }

        @Override
        int getNativeSize() {
            int size = 100;
            for (int i = 0; i < this.mChildren.size(); i++) {
                size += this.mChildren.get(i).getNativeSize();
            }
            return size;
        }

        int getNativeGroupChildrenSize() {
            int size = 0;
            for (int i = 0; i < this.mChildren.size(); i++) {
                size += this.mChildren.get(i).getNativeSize();
            }
            return size;
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null) {
                return true;
            }
            ArrayList<VObject> children = this.mChildren;
            int count = children.size();
            for (int i = 0; i < count; i++) {
                VObject child = children.get(i);
                if (child.canApplyTheme()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void applyTheme(Resources.Theme t) {
            if (this.mThemeAttrs != null) {
                TypedArray a = t.resolveAttributes(this.mThemeAttrs, R.styleable.VectorDrawableGroup);
                updateStateFromTypedArray(a);
                a.recycle();
            }
            ArrayList<VObject> children = this.mChildren;
            int count = children.size();
            for (int i = 0; i < count; i++) {
                VObject child = children.get(i);
                if (child.canApplyTheme()) {
                    child.applyTheme(t);
                    this.mIsStateful |= child.isStateful();
                }
            }
        }

        public float getRotation() {
            if (isTreeValid()) {
                return VectorDrawable.nGetRotation(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setRotation(float rotation) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetRotation(this.mNativePtr, rotation);
        }

        public float getPivotX() {
            if (isTreeValid()) {
                return VectorDrawable.nGetPivotX(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setPivotX(float pivotX) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetPivotX(this.mNativePtr, pivotX);
        }

        public float getPivotY() {
            if (isTreeValid()) {
                return VectorDrawable.nGetPivotY(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setPivotY(float pivotY) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetPivotY(this.mNativePtr, pivotY);
        }

        public float getScaleX() {
            if (isTreeValid()) {
                return VectorDrawable.nGetScaleX(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setScaleX(float scaleX) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetScaleX(this.mNativePtr, scaleX);
        }

        public float getScaleY() {
            if (isTreeValid()) {
                return VectorDrawable.nGetScaleY(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setScaleY(float scaleY) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetScaleY(this.mNativePtr, scaleY);
        }

        public float getTranslateX() {
            if (isTreeValid()) {
                return VectorDrawable.nGetTranslateX(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setTranslateX(float translateX) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetTranslateX(this.mNativePtr, translateX);
        }

        public float getTranslateY() {
            if (isTreeValid()) {
                return VectorDrawable.nGetTranslateY(this.mNativePtr);
            }
            return 0.0f;
        }

        public void setTranslateY(float translateY) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetTranslateY(this.mNativePtr, translateY);
        }
    }

    static abstract class VPath extends VObject {
        int mChangingConfigurations;
        protected PathParser.PathData mPathData;
        String mPathName;

        public VPath() {
            this.mPathData = null;
        }

        public VPath(VPath copy) {
            this.mPathData = null;
            this.mPathName = copy.mPathName;
            this.mChangingConfigurations = copy.mChangingConfigurations;
            this.mPathData = copy.mPathData != null ? new PathParser.PathData(copy.mPathData) : null;
        }

        public String getPathName() {
            return this.mPathName;
        }

        public PathParser.PathData getPathData() {
            return this.mPathData;
        }

        public void setPathData(PathParser.PathData pathData) {
            this.mPathData.setPathData(pathData);
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetPathData(getNativePtr(), this.mPathData.getNativePtr());
        }
    }

    private static class VClipPath extends VPath {
        private static final int NATIVE_ALLOCATION_SIZE = 120;
        private final long mNativePtr;

        public VClipPath() {
            this.mNativePtr = VectorDrawable.nCreateClipPath();
        }

        public VClipPath(VClipPath copy) {
            super(copy);
            this.mNativePtr = VectorDrawable.nCreateClipPath(copy.mNativePtr);
        }

        @Override
        public long getNativePtr() {
            return this.mNativePtr;
        }

        @Override
        public void inflate(Resources r, AttributeSet attrs, Resources.Theme theme) {
            TypedArray a = VectorDrawable.obtainAttributes(r, theme, attrs, R.styleable.VectorDrawableClipPath);
            updateStateFromTypedArray(a);
            a.recycle();
        }

        @Override
        public boolean canApplyTheme() {
            return false;
        }

        @Override
        public void applyTheme(Resources.Theme theme) {
        }

        @Override
        public boolean onStateChange(int[] stateSet) {
            return false;
        }

        @Override
        public boolean isStateful() {
            return false;
        }

        @Override
        int getNativeSize() {
            return 120;
        }

        private void updateStateFromTypedArray(TypedArray a) {
            this.mChangingConfigurations |= a.getChangingConfigurations();
            String pathName = a.getString(0);
            if (pathName != null) {
                this.mPathName = pathName;
                VectorDrawable.nSetName(this.mNativePtr, this.mPathName);
            }
            String pathDataString = a.getString(1);
            if (pathDataString == null) {
                return;
            }
            this.mPathData = new PathParser.PathData(pathDataString);
            VectorDrawable.nSetPathString(this.mNativePtr, pathDataString, pathDataString.length());
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
        private static final HashMap<String, Integer> sPropertyMap = new HashMap<String, Integer>() {
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
        ComplexColor mFillColors;
        private final long mNativePtr;
        private byte[] mPropertyData;
        ComplexColor mStrokeColors;
        private int[] mThemeAttrs;

        public VFullPath() {
            this.mStrokeColors = null;
            this.mFillColors = null;
            this.mNativePtr = VectorDrawable.nCreateFullPath();
        }

        public VFullPath(VFullPath copy) {
            super(copy);
            this.mStrokeColors = null;
            this.mFillColors = null;
            this.mNativePtr = VectorDrawable.nCreateFullPath(copy.mNativePtr);
            this.mThemeAttrs = copy.mThemeAttrs;
            this.mStrokeColors = copy.mStrokeColors;
            this.mFillColors = copy.mFillColors;
        }

        int getPropertyIndex(String propertyName) {
            if (!sPropertyMap.containsKey(propertyName)) {
                return -1;
            }
            return sPropertyMap.get(propertyName).intValue();
        }

        @Override
        public boolean onStateChange(int[] stateSet) {
            boolean changed = false;
            if (this.mStrokeColors != null && (this.mStrokeColors instanceof ColorStateList)) {
                int oldStrokeColor = getStrokeColor();
                int newStrokeColor = ((ColorStateList) this.mStrokeColors).getColorForState(stateSet, oldStrokeColor);
                changed = oldStrokeColor != newStrokeColor;
                if (oldStrokeColor != newStrokeColor) {
                    VectorDrawable.nSetStrokeColor(this.mNativePtr, newStrokeColor);
                }
            }
            if (this.mFillColors != null && (this.mFillColors instanceof ColorStateList)) {
                int oldFillColor = getFillColor();
                int newFillColor = ((ColorStateList) this.mFillColors).getColorForState(stateSet, oldFillColor);
                changed |= oldFillColor != newFillColor;
                if (oldFillColor != newFillColor) {
                    VectorDrawable.nSetFillColor(this.mNativePtr, newFillColor);
                }
            }
            return changed;
        }

        @Override
        public boolean isStateful() {
            return (this.mStrokeColors == null && this.mFillColors == null) ? false : true;
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
        public void inflate(Resources r, AttributeSet attrs, Resources.Theme theme) throws BlockGuard.BlockGuardPolicyException {
            TypedArray a = VectorDrawable.obtainAttributes(r, theme, attrs, R.styleable.VectorDrawablePath);
            updateStateFromTypedArray(a);
            a.recycle();
        }

        private void updateStateFromTypedArray(TypedArray a) throws BlockGuard.BlockGuardPolicyException {
            if (this.mPropertyData == null) {
                this.mPropertyData = new byte[48];
            }
            boolean success = VectorDrawable.nGetFullPathProperties(this.mNativePtr, this.mPropertyData, 48);
            if (!success) {
                throw new RuntimeException("Error: inconsistent property count");
            }
            ByteBuffer properties = ByteBuffer.wrap(this.mPropertyData);
            properties.order(ByteOrder.nativeOrder());
            float strokeWidth = properties.getFloat(0);
            int strokeColor = properties.getInt(4);
            float strokeAlpha = properties.getFloat(8);
            int fillColor = properties.getInt(12);
            float fillAlpha = properties.getFloat(16);
            float trimPathStart = properties.getFloat(20);
            float trimPathEnd = properties.getFloat(24);
            float trimPathOffset = properties.getFloat(28);
            int strokeLineCap = properties.getInt(32);
            int strokeLineJoin = properties.getInt(36);
            float strokeMiterLimit = properties.getFloat(40);
            int fillType = properties.getInt(44);
            Shader fillGradient = null;
            Shader strokeGradient = null;
            this.mChangingConfigurations |= a.getChangingConfigurations();
            this.mThemeAttrs = a.extractThemeAttrs();
            String pathName = a.getString(0);
            if (pathName != null) {
                this.mPathName = pathName;
                VectorDrawable.nSetName(this.mNativePtr, this.mPathName);
            }
            String pathString = a.getString(2);
            if (pathString != null) {
                this.mPathData = new PathParser.PathData(pathString);
                VectorDrawable.nSetPathString(this.mNativePtr, pathString, pathString.length());
            }
            ComplexColor fillColors = a.getComplexColor(1);
            if (fillColors != null) {
                if (fillColors instanceof GradientColor) {
                    this.mFillColors = fillColors;
                    fillGradient = ((GradientColor) fillColors).getShader();
                } else if (fillColors.isStateful()) {
                    this.mFillColors = fillColors;
                } else {
                    this.mFillColors = null;
                }
                fillColor = fillColors.getDefaultColor();
            }
            ComplexColor strokeColors = a.getComplexColor(3);
            if (strokeColors != null) {
                if (strokeColors instanceof GradientColor) {
                    this.mStrokeColors = strokeColors;
                    strokeGradient = ((GradientColor) strokeColors).getShader();
                } else if (strokeColors.isStateful()) {
                    this.mStrokeColors = strokeColors;
                } else {
                    this.mStrokeColors = null;
                }
                strokeColor = strokeColors.getDefaultColor();
            }
            VectorDrawable.nUpdateFullPathFillGradient(this.mNativePtr, fillGradient != null ? fillGradient.getNativeInstance() : 0L);
            VectorDrawable.nUpdateFullPathStrokeGradient(this.mNativePtr, strokeGradient != null ? strokeGradient.getNativeInstance() : 0L);
            float fillAlpha2 = a.getFloat(12, fillAlpha);
            int strokeLineCap2 = a.getInt(8, strokeLineCap);
            int strokeLineJoin2 = a.getInt(9, strokeLineJoin);
            float strokeMiterLimit2 = a.getFloat(10, strokeMiterLimit);
            VectorDrawable.nUpdateFullPathProperties(this.mNativePtr, a.getFloat(4, strokeWidth), strokeColor, a.getFloat(11, strokeAlpha), fillColor, fillAlpha2, a.getFloat(5, trimPathStart), a.getFloat(6, trimPathEnd), a.getFloat(7, trimPathOffset), strokeMiterLimit2, strokeLineCap2, strokeLineJoin2, a.getInt(13, fillType));
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null) {
                return true;
            }
            boolean fillCanApplyTheme = canComplexColorApplyTheme(this.mFillColors);
            boolean strokeCanApplyTheme = canComplexColorApplyTheme(this.mStrokeColors);
            return fillCanApplyTheme || strokeCanApplyTheme;
        }

        @Override
        public void applyTheme(Resources.Theme t) throws BlockGuard.BlockGuardPolicyException {
            if (this.mThemeAttrs != null) {
                TypedArray a = t.resolveAttributes(this.mThemeAttrs, R.styleable.VectorDrawablePath);
                updateStateFromTypedArray(a);
                a.recycle();
            }
            boolean fillCanApplyTheme = canComplexColorApplyTheme(this.mFillColors);
            boolean strokeCanApplyTheme = canComplexColorApplyTheme(this.mStrokeColors);
            if (fillCanApplyTheme) {
                this.mFillColors = this.mFillColors.obtainForTheme(t);
                if (this.mFillColors instanceof GradientColor) {
                    VectorDrawable.nUpdateFullPathFillGradient(this.mNativePtr, ((GradientColor) this.mFillColors).getShader().getNativeInstance());
                } else if (this.mFillColors instanceof ColorStateList) {
                    VectorDrawable.nSetFillColor(this.mNativePtr, this.mFillColors.getDefaultColor());
                }
            }
            if (!strokeCanApplyTheme) {
                return;
            }
            this.mStrokeColors = this.mStrokeColors.obtainForTheme(t);
            if (this.mStrokeColors instanceof GradientColor) {
                VectorDrawable.nUpdateFullPathStrokeGradient(this.mNativePtr, ((GradientColor) this.mStrokeColors).getShader().getNativeInstance());
            } else {
                if (!(this.mStrokeColors instanceof ColorStateList)) {
                    return;
                }
                VectorDrawable.nSetStrokeColor(this.mNativePtr, this.mStrokeColors.getDefaultColor());
            }
        }

        private boolean canComplexColorApplyTheme(ComplexColor complexColor) {
            if (complexColor != null) {
                return complexColor.canApplyTheme();
            }
            return false;
        }

        int getStrokeColor() {
            if (isTreeValid()) {
                return VectorDrawable.nGetStrokeColor(this.mNativePtr);
            }
            return 0;
        }

        void setStrokeColor(int strokeColor) {
            this.mStrokeColors = null;
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetStrokeColor(this.mNativePtr, strokeColor);
        }

        float getStrokeWidth() {
            if (isTreeValid()) {
                return VectorDrawable.nGetStrokeWidth(this.mNativePtr);
            }
            return 0.0f;
        }

        void setStrokeWidth(float strokeWidth) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetStrokeWidth(this.mNativePtr, strokeWidth);
        }

        float getStrokeAlpha() {
            if (isTreeValid()) {
                return VectorDrawable.nGetStrokeAlpha(this.mNativePtr);
            }
            return 0.0f;
        }

        void setStrokeAlpha(float strokeAlpha) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetStrokeAlpha(this.mNativePtr, strokeAlpha);
        }

        int getFillColor() {
            if (isTreeValid()) {
                return VectorDrawable.nGetFillColor(this.mNativePtr);
            }
            return 0;
        }

        void setFillColor(int fillColor) {
            this.mFillColors = null;
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetFillColor(this.mNativePtr, fillColor);
        }

        float getFillAlpha() {
            if (isTreeValid()) {
                return VectorDrawable.nGetFillAlpha(this.mNativePtr);
            }
            return 0.0f;
        }

        void setFillAlpha(float fillAlpha) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetFillAlpha(this.mNativePtr, fillAlpha);
        }

        float getTrimPathStart() {
            if (isTreeValid()) {
                return VectorDrawable.nGetTrimPathStart(this.mNativePtr);
            }
            return 0.0f;
        }

        void setTrimPathStart(float trimPathStart) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetTrimPathStart(this.mNativePtr, trimPathStart);
        }

        float getTrimPathEnd() {
            if (isTreeValid()) {
                return VectorDrawable.nGetTrimPathEnd(this.mNativePtr);
            }
            return 0.0f;
        }

        void setTrimPathEnd(float trimPathEnd) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetTrimPathEnd(this.mNativePtr, trimPathEnd);
        }

        float getTrimPathOffset() {
            if (isTreeValid()) {
                return VectorDrawable.nGetTrimPathOffset(this.mNativePtr);
            }
            return 0.0f;
        }

        void setTrimPathOffset(float trimPathOffset) {
            if (!isTreeValid()) {
                return;
            }
            VectorDrawable.nSetTrimPathOffset(this.mNativePtr, trimPathOffset);
        }
    }

    static abstract class VObject {
        VirtualRefBasePtr mTreePtr = null;

        abstract void applyTheme(Resources.Theme theme);

        abstract boolean canApplyTheme();

        abstract long getNativePtr();

        abstract int getNativeSize();

        abstract void inflate(Resources resources, AttributeSet attributeSet, Resources.Theme theme);

        abstract boolean isStateful();

        abstract boolean onStateChange(int[] iArr);

        VObject() {
        }

        boolean isTreeValid() {
            return (this.mTreePtr == null || this.mTreePtr.get() == 0) ? false : true;
        }

        void setTree(VirtualRefBasePtr ptr) {
            this.mTreePtr = ptr;
        }
    }
}

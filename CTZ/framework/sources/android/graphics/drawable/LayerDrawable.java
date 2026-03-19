package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import com.android.ims.ImsConfig;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class LayerDrawable extends Drawable implements Drawable.Callback {
    public static final int INSET_UNDEFINED = Integer.MIN_VALUE;
    private static final String LOG_TAG = "LayerDrawable";
    public static final int PADDING_MODE_NEST = 0;
    public static final int PADDING_MODE_STACK = 1;
    private boolean mChildRequestedInvalidation;
    private Rect mHotspotBounds;
    LayerState mLayerState;
    private boolean mMutated;
    private int[] mPaddingB;
    private int[] mPaddingL;
    private int[] mPaddingR;
    private int[] mPaddingT;
    private boolean mSuspendChildInvalidation;
    private final Rect mTmpContainer;
    private final Rect mTmpOutRect;
    private final Rect mTmpRect;

    public LayerDrawable(Drawable[] drawableArr) {
        this(drawableArr, (LayerState) null);
    }

    LayerDrawable(Drawable[] drawableArr, LayerState layerState) {
        this(layerState, (Resources) null);
        if (drawableArr == null) {
            throw new IllegalArgumentException("layers must be non-null");
        }
        int length = drawableArr.length;
        ChildDrawable[] childDrawableArr = new ChildDrawable[length];
        for (int i = 0; i < length; i++) {
            childDrawableArr[i] = new ChildDrawable(this.mLayerState.mDensity);
            childDrawableArr[i].mDrawable = drawableArr[i];
            drawableArr[i].setCallback(this);
            this.mLayerState.mChildrenChangingConfigurations |= drawableArr[i].getChangingConfigurations();
        }
        this.mLayerState.mNumChildren = length;
        this.mLayerState.mChildren = childDrawableArr;
        ensurePadding();
        refreshPadding();
    }

    LayerDrawable() {
        this((LayerState) null, (Resources) null);
    }

    LayerDrawable(LayerState layerState, Resources resources) {
        this.mTmpRect = new Rect();
        this.mTmpOutRect = new Rect();
        this.mTmpContainer = new Rect();
        this.mLayerState = createConstantState(layerState, resources);
        if (this.mLayerState.mNumChildren > 0) {
            ensurePadding();
            refreshPadding();
        }
    }

    LayerState createConstantState(LayerState layerState, Resources resources) {
        return new LayerState(layerState, this, resources);
    }

    @Override
    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(resources, xmlPullParser, attributeSet, theme);
        LayerState layerState = this.mLayerState;
        int iResolveDensity = Drawable.resolveDensity(resources, 0);
        layerState.setDensity(iResolveDensity);
        TypedArray typedArrayObtainAttributes = obtainAttributes(resources, theme, attributeSet, R.styleable.LayerDrawable);
        updateStateFromTypedArray(typedArrayObtainAttributes);
        typedArrayObtainAttributes.recycle();
        ChildDrawable[] childDrawableArr = layerState.mChildren;
        int i = layerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            childDrawableArr[i2].setDensity(iResolveDensity);
        }
        inflateLayers(resources, xmlPullParser, attributeSet, theme);
        ensurePadding();
        refreshPadding();
    }

    @Override
    public void applyTheme(Resources.Theme theme) {
        super.applyTheme(theme);
        LayerState layerState = this.mLayerState;
        int iResolveDensity = Drawable.resolveDensity(theme.getResources(), 0);
        layerState.setDensity(iResolveDensity);
        if (layerState.mThemeAttrs != null) {
            TypedArray typedArrayResolveAttributes = theme.resolveAttributes(layerState.mThemeAttrs, R.styleable.LayerDrawable);
            updateStateFromTypedArray(typedArrayResolveAttributes);
            typedArrayResolveAttributes.recycle();
        }
        ChildDrawable[] childDrawableArr = layerState.mChildren;
        int i = layerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            ChildDrawable childDrawable = childDrawableArr[i2];
            childDrawable.setDensity(iResolveDensity);
            if (childDrawable.mThemeAttrs != null) {
                TypedArray typedArrayResolveAttributes2 = theme.resolveAttributes(childDrawable.mThemeAttrs, R.styleable.LayerDrawableItem);
                updateLayerFromTypedArray(childDrawable, typedArrayResolveAttributes2);
                typedArrayResolveAttributes2.recycle();
            }
            Drawable drawable = childDrawable.mDrawable;
            if (drawable != null && drawable.canApplyTheme()) {
                drawable.applyTheme(theme);
                layerState.mChildrenChangingConfigurations = drawable.getChangingConfigurations() | layerState.mChildrenChangingConfigurations;
            }
        }
    }

    private void inflateLayers(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        int next;
        LayerState layerState = this.mLayerState;
        int depth = xmlPullParser.getDepth() + 1;
        while (true) {
            int next2 = xmlPullParser.next();
            if (next2 != 1) {
                int depth2 = xmlPullParser.getDepth();
                if (depth2 >= depth || next2 != 3) {
                    if (next2 == 2 && depth2 <= depth && xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        ChildDrawable childDrawable = new ChildDrawable(layerState.mDensity);
                        TypedArray typedArrayObtainAttributes = obtainAttributes(resources, theme, attributeSet, R.styleable.LayerDrawableItem);
                        updateLayerFromTypedArray(childDrawable, typedArrayObtainAttributes);
                        typedArrayObtainAttributes.recycle();
                        if (childDrawable.mDrawable == null && (childDrawable.mThemeAttrs == null || childDrawable.mThemeAttrs[4] == 0)) {
                            do {
                                next = xmlPullParser.next();
                            } while (next == 4);
                            if (next != 2) {
                                throw new XmlPullParserException(xmlPullParser.getPositionDescription() + ": <item> tag requires a 'drawable' attribute or child tag defining a drawable");
                            }
                            childDrawable.mDrawable = Drawable.createFromXmlInner(resources, xmlPullParser, attributeSet, theme);
                            childDrawable.mDrawable.setCallback(this);
                            layerState.mChildrenChangingConfigurations |= childDrawable.mDrawable.getChangingConfigurations();
                        }
                        addLayer(childDrawable);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void updateStateFromTypedArray(TypedArray typedArray) {
        LayerState layerState = this.mLayerState;
        layerState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        layerState.mThemeAttrs = typedArray.extractThemeAttrs();
        int indexCount = typedArray.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            int index = typedArray.getIndex(i);
            switch (index) {
                case 0:
                    layerState.mPaddingLeft = typedArray.getDimensionPixelOffset(index, layerState.mPaddingLeft);
                    break;
                case 1:
                    layerState.mPaddingTop = typedArray.getDimensionPixelOffset(index, layerState.mPaddingTop);
                    break;
                case 2:
                    layerState.mPaddingRight = typedArray.getDimensionPixelOffset(index, layerState.mPaddingRight);
                    break;
                case 3:
                    layerState.mPaddingBottom = typedArray.getDimensionPixelOffset(index, layerState.mPaddingBottom);
                    break;
                case 4:
                    layerState.mOpacityOverride = typedArray.getInt(index, layerState.mOpacityOverride);
                    break;
                case 5:
                    layerState.mPaddingStart = typedArray.getDimensionPixelOffset(index, layerState.mPaddingStart);
                    break;
                case 6:
                    layerState.mPaddingEnd = typedArray.getDimensionPixelOffset(index, layerState.mPaddingEnd);
                    break;
                case 7:
                    layerState.mAutoMirrored = typedArray.getBoolean(index, layerState.mAutoMirrored);
                    break;
                case 8:
                    layerState.mPaddingMode = typedArray.getInteger(index, layerState.mPaddingMode);
                    break;
            }
        }
    }

    private void updateLayerFromTypedArray(ChildDrawable childDrawable, TypedArray typedArray) {
        LayerState layerState = this.mLayerState;
        layerState.mChildrenChangingConfigurations |= typedArray.getChangingConfigurations();
        childDrawable.mThemeAttrs = typedArray.extractThemeAttrs();
        int indexCount = typedArray.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            int index = typedArray.getIndex(i);
            switch (index) {
                case 0:
                    childDrawable.mGravity = typedArray.getInteger(index, childDrawable.mGravity);
                    break;
                case 1:
                    childDrawable.mId = typedArray.getResourceId(index, childDrawable.mId);
                    break;
                case 2:
                    childDrawable.mHeight = typedArray.getDimensionPixelSize(index, childDrawable.mHeight);
                    break;
                case 3:
                    childDrawable.mWidth = typedArray.getDimensionPixelSize(index, childDrawable.mWidth);
                    break;
                case 5:
                    childDrawable.mInsetL = typedArray.getDimensionPixelOffset(index, childDrawable.mInsetL);
                    break;
                case 6:
                    childDrawable.mInsetT = typedArray.getDimensionPixelOffset(index, childDrawable.mInsetT);
                    break;
                case 7:
                    childDrawable.mInsetR = typedArray.getDimensionPixelOffset(index, childDrawable.mInsetR);
                    break;
                case 8:
                    childDrawable.mInsetB = typedArray.getDimensionPixelOffset(index, childDrawable.mInsetB);
                    break;
                case 9:
                    childDrawable.mInsetS = typedArray.getDimensionPixelOffset(index, childDrawable.mInsetS);
                    break;
                case 10:
                    childDrawable.mInsetE = typedArray.getDimensionPixelOffset(index, childDrawable.mInsetE);
                    break;
            }
        }
        Drawable drawable = typedArray.getDrawable(4);
        if (drawable != null) {
            if (childDrawable.mDrawable != null) {
                childDrawable.mDrawable.setCallback(null);
            }
            childDrawable.mDrawable = drawable;
            childDrawable.mDrawable.setCallback(this);
            layerState.mChildrenChangingConfigurations = childDrawable.mDrawable.getChangingConfigurations() | layerState.mChildrenChangingConfigurations;
        }
    }

    @Override
    public boolean canApplyTheme() {
        return this.mLayerState.canApplyTheme() || super.canApplyTheme();
    }

    @Override
    public boolean isProjected() {
        if (super.isProjected()) {
            return true;
        }
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            if (childDrawableArr[i2].mDrawable.isProjected()) {
                return true;
            }
        }
        return false;
    }

    int addLayer(ChildDrawable childDrawable) {
        LayerState layerState = this.mLayerState;
        int length = layerState.mChildren != null ? layerState.mChildren.length : 0;
        int i = layerState.mNumChildren;
        if (i >= length) {
            ChildDrawable[] childDrawableArr = new ChildDrawable[length + 10];
            if (i > 0) {
                System.arraycopy(layerState.mChildren, 0, childDrawableArr, 0, i);
            }
            layerState.mChildren = childDrawableArr;
        }
        layerState.mChildren[i] = childDrawable;
        layerState.mNumChildren++;
        layerState.invalidateCache();
        return i;
    }

    ChildDrawable addLayer(Drawable drawable, int[] iArr, int i, int i2, int i3, int i4, int i5) {
        ChildDrawable childDrawableCreateLayer = createLayer(drawable);
        childDrawableCreateLayer.mId = i;
        childDrawableCreateLayer.mThemeAttrs = iArr;
        childDrawableCreateLayer.mDrawable.setAutoMirrored(isAutoMirrored());
        childDrawableCreateLayer.mInsetL = i2;
        childDrawableCreateLayer.mInsetT = i3;
        childDrawableCreateLayer.mInsetR = i4;
        childDrawableCreateLayer.mInsetB = i5;
        addLayer(childDrawableCreateLayer);
        this.mLayerState.mChildrenChangingConfigurations |= drawable.getChangingConfigurations();
        drawable.setCallback(this);
        return childDrawableCreateLayer;
    }

    private ChildDrawable createLayer(Drawable drawable) {
        ChildDrawable childDrawable = new ChildDrawable(this.mLayerState.mDensity);
        childDrawable.mDrawable = drawable;
        return childDrawable;
    }

    public int addLayer(Drawable drawable) {
        ChildDrawable childDrawableCreateLayer = createLayer(drawable);
        int iAddLayer = addLayer(childDrawableCreateLayer);
        ensurePadding();
        refreshChildPadding(iAddLayer, childDrawableCreateLayer);
        return iAddLayer;
    }

    public Drawable findDrawableByLayerId(int i) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        for (int i2 = this.mLayerState.mNumChildren - 1; i2 >= 0; i2--) {
            if (childDrawableArr[i2].mId == i) {
                return childDrawableArr[i2].mDrawable;
            }
        }
        return null;
    }

    public void setId(int i, int i2) {
        this.mLayerState.mChildren[i].mId = i2;
    }

    public int getId(int i) {
        if (i >= this.mLayerState.mNumChildren) {
            throw new IndexOutOfBoundsException();
        }
        return this.mLayerState.mChildren[i].mId;
    }

    public int getNumberOfLayers() {
        return this.mLayerState.mNumChildren;
    }

    public boolean setDrawableByLayerId(int i, Drawable drawable) {
        int iFindIndexByLayerId = findIndexByLayerId(i);
        if (iFindIndexByLayerId < 0) {
            return false;
        }
        setDrawable(iFindIndexByLayerId, drawable);
        return true;
    }

    public int findIndexByLayerId(int i) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i2 = this.mLayerState.mNumChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            if (childDrawableArr[i3].mId == i) {
                return i3;
            }
        }
        return -1;
    }

    public void setDrawable(int i, Drawable drawable) {
        if (i >= this.mLayerState.mNumChildren) {
            throw new IndexOutOfBoundsException();
        }
        ChildDrawable childDrawable = this.mLayerState.mChildren[i];
        if (childDrawable.mDrawable != null) {
            if (drawable != null) {
                drawable.setBounds(childDrawable.mDrawable.getBounds());
            }
            childDrawable.mDrawable.setCallback(null);
        }
        if (drawable != null) {
            drawable.setCallback(this);
        }
        childDrawable.mDrawable = drawable;
        this.mLayerState.invalidateCache();
        refreshChildPadding(i, childDrawable);
    }

    public Drawable getDrawable(int i) {
        if (i >= this.mLayerState.mNumChildren) {
            throw new IndexOutOfBoundsException();
        }
        return this.mLayerState.mChildren[i].mDrawable;
    }

    public void setLayerSize(int i, int i2, int i3) {
        ChildDrawable childDrawable = this.mLayerState.mChildren[i];
        childDrawable.mWidth = i2;
        childDrawable.mHeight = i3;
    }

    public void setLayerWidth(int i, int i2) {
        this.mLayerState.mChildren[i].mWidth = i2;
    }

    public int getLayerWidth(int i) {
        return this.mLayerState.mChildren[i].mWidth;
    }

    public void setLayerHeight(int i, int i2) {
        this.mLayerState.mChildren[i].mHeight = i2;
    }

    public int getLayerHeight(int i) {
        return this.mLayerState.mChildren[i].mHeight;
    }

    public void setLayerGravity(int i, int i2) {
        this.mLayerState.mChildren[i].mGravity = i2;
    }

    public int getLayerGravity(int i) {
        return this.mLayerState.mChildren[i].mGravity;
    }

    public void setLayerInset(int i, int i2, int i3, int i4, int i5) {
        setLayerInsetInternal(i, i2, i3, i4, i5, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public void setLayerInsetRelative(int i, int i2, int i3, int i4, int i5) {
        setLayerInsetInternal(i, 0, i3, 0, i5, i2, i4);
    }

    public void setLayerInsetLeft(int i, int i2) {
        this.mLayerState.mChildren[i].mInsetL = i2;
    }

    public int getLayerInsetLeft(int i) {
        return this.mLayerState.mChildren[i].mInsetL;
    }

    public void setLayerInsetRight(int i, int i2) {
        this.mLayerState.mChildren[i].mInsetR = i2;
    }

    public int getLayerInsetRight(int i) {
        return this.mLayerState.mChildren[i].mInsetR;
    }

    public void setLayerInsetTop(int i, int i2) {
        this.mLayerState.mChildren[i].mInsetT = i2;
    }

    public int getLayerInsetTop(int i) {
        return this.mLayerState.mChildren[i].mInsetT;
    }

    public void setLayerInsetBottom(int i, int i2) {
        this.mLayerState.mChildren[i].mInsetB = i2;
    }

    public int getLayerInsetBottom(int i) {
        return this.mLayerState.mChildren[i].mInsetB;
    }

    public void setLayerInsetStart(int i, int i2) {
        this.mLayerState.mChildren[i].mInsetS = i2;
    }

    public int getLayerInsetStart(int i) {
        return this.mLayerState.mChildren[i].mInsetS;
    }

    public void setLayerInsetEnd(int i, int i2) {
        this.mLayerState.mChildren[i].mInsetE = i2;
    }

    public int getLayerInsetEnd(int i) {
        return this.mLayerState.mChildren[i].mInsetE;
    }

    private void setLayerInsetInternal(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        ChildDrawable childDrawable = this.mLayerState.mChildren[i];
        childDrawable.mInsetL = i2;
        childDrawable.mInsetT = i3;
        childDrawable.mInsetR = i4;
        childDrawable.mInsetB = i5;
        childDrawable.mInsetS = i6;
        childDrawable.mInsetE = i7;
    }

    public void setPaddingMode(int i) {
        if (this.mLayerState.mPaddingMode == i) {
            return;
        }
        this.mLayerState.mPaddingMode = i;
    }

    public int getPaddingMode() {
        return this.mLayerState.mPaddingMode;
    }

    private void suspendChildInvalidation() {
        this.mSuspendChildInvalidation = true;
    }

    private void resumeChildInvalidation() {
        this.mSuspendChildInvalidation = false;
        if (this.mChildRequestedInvalidation) {
            this.mChildRequestedInvalidation = false;
            invalidateSelf();
        }
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        if (this.mSuspendChildInvalidation) {
            this.mChildRequestedInvalidation = true;
        } else {
            this.mLayerState.invalidateCache();
            invalidateSelf();
        }
    }

    @Override
    public void scheduleDrawable(Drawable drawable, Runnable runnable, long j) {
        scheduleSelf(runnable, j);
    }

    @Override
    public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
        unscheduleSelf(runnable);
    }

    @Override
    public void draw(Canvas canvas) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mLayerState.getChangingConfigurations();
    }

    @Override
    public boolean getPadding(Rect rect) {
        LayerState layerState = this.mLayerState;
        if (layerState.mPaddingMode == 0) {
            computeNestedPadding(rect);
        } else {
            computeStackedPadding(rect);
        }
        int i = layerState.mPaddingTop;
        int i2 = layerState.mPaddingBottom;
        boolean z = getLayoutDirection() == 1;
        int i3 = z ? layerState.mPaddingEnd : layerState.mPaddingStart;
        int i4 = z ? layerState.mPaddingStart : layerState.mPaddingEnd;
        if (i3 < 0) {
            i3 = layerState.mPaddingLeft;
        }
        if (i4 < 0) {
            i4 = layerState.mPaddingRight;
        }
        if (i3 >= 0) {
            rect.left = i3;
        }
        if (i >= 0) {
            rect.top = i;
        }
        if (i4 >= 0) {
            rect.right = i4;
        }
        if (i2 >= 0) {
            rect.bottom = i2;
        }
        return (rect.left == 0 && rect.top == 0 && rect.right == 0 && rect.bottom == 0) ? false : true;
    }

    public void setPadding(int i, int i2, int i3, int i4) {
        LayerState layerState = this.mLayerState;
        layerState.mPaddingLeft = i;
        layerState.mPaddingTop = i2;
        layerState.mPaddingRight = i3;
        layerState.mPaddingBottom = i4;
        layerState.mPaddingStart = -1;
        layerState.mPaddingEnd = -1;
    }

    public void setPaddingRelative(int i, int i2, int i3, int i4) {
        LayerState layerState = this.mLayerState;
        layerState.mPaddingStart = i;
        layerState.mPaddingTop = i2;
        layerState.mPaddingEnd = i3;
        layerState.mPaddingBottom = i4;
        layerState.mPaddingLeft = -1;
        layerState.mPaddingRight = -1;
    }

    public int getLeftPadding() {
        return this.mLayerState.mPaddingLeft;
    }

    public int getRightPadding() {
        return this.mLayerState.mPaddingRight;
    }

    public int getStartPadding() {
        return this.mLayerState.mPaddingStart;
    }

    public int getEndPadding() {
        return this.mLayerState.mPaddingEnd;
    }

    public int getTopPadding() {
        return this.mLayerState.mPaddingTop;
    }

    public int getBottomPadding() {
        return this.mLayerState.mPaddingBottom;
    }

    private void computeNestedPadding(Rect rect) {
        rect.left = 0;
        rect.top = 0;
        rect.right = 0;
        rect.bottom = 0;
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            refreshChildPadding(i2, childDrawableArr[i2]);
            rect.left += this.mPaddingL[i2];
            rect.top += this.mPaddingT[i2];
            rect.right += this.mPaddingR[i2];
            rect.bottom += this.mPaddingB[i2];
        }
    }

    private void computeStackedPadding(Rect rect) {
        rect.left = 0;
        rect.top = 0;
        rect.right = 0;
        rect.bottom = 0;
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            refreshChildPadding(i2, childDrawableArr[i2]);
            rect.left = Math.max(rect.left, this.mPaddingL[i2]);
            rect.top = Math.max(rect.top, this.mPaddingT[i2]);
            rect.right = Math.max(rect.right, this.mPaddingR[i2]);
            rect.bottom = Math.max(rect.bottom, this.mPaddingB[i2]);
        }
    }

    @Override
    public void getOutline(Outline outline) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.getOutline(outline);
                if (!outline.isEmpty()) {
                    return;
                }
            }
        }
    }

    @Override
    public void setHotspot(float f, float f2) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.setHotspot(f, f2);
            }
        }
    }

    @Override
    public void setHotspotBounds(int i, int i2, int i3, int i4) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i5 = this.mLayerState.mNumChildren;
        for (int i6 = 0; i6 < i5; i6++) {
            Drawable drawable = childDrawableArr[i6].mDrawable;
            if (drawable != null) {
                drawable.setHotspotBounds(i, i2, i3, i4);
            }
        }
        if (this.mHotspotBounds == null) {
            this.mHotspotBounds = new Rect(i, i2, i3, i4);
        } else {
            this.mHotspotBounds.set(i, i2, i3, i4);
        }
    }

    @Override
    public void getHotspotBounds(Rect rect) {
        if (this.mHotspotBounds != null) {
            rect.set(this.mHotspotBounds);
        } else {
            super.getHotspotBounds(rect);
        }
    }

    @Override
    public boolean setVisible(boolean z, boolean z2) {
        boolean visible = super.setVisible(z, z2);
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.setVisible(z, z2);
            }
        }
        return visible;
    }

    @Override
    public void setDither(boolean z) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.setDither(z);
            }
        }
    }

    @Override
    public void setAlpha(int i) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i2 = this.mLayerState.mNumChildren;
        for (int i3 = 0; i3 < i2; i3++) {
            Drawable drawable = childDrawableArr[i3].mDrawable;
            if (drawable != null) {
                drawable.setAlpha(i);
            }
        }
    }

    @Override
    public int getAlpha() {
        Drawable firstNonNullDrawable = getFirstNonNullDrawable();
        if (firstNonNullDrawable != null) {
            return firstNonNullDrawable.getAlpha();
        }
        return super.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.setColorFilter(colorFilter);
            }
        }
    }

    @Override
    public void setTintList(ColorStateList colorStateList) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.setTintList(colorStateList);
            }
        }
    }

    @Override
    public void setTintMode(PorterDuff.Mode mode) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.setTintMode(mode);
            }
        }
    }

    private Drawable getFirstNonNullDrawable() {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                return drawable;
            }
        }
        return null;
    }

    public void setOpacity(int i) {
        this.mLayerState.mOpacityOverride = i;
    }

    @Override
    public int getOpacity() {
        if (this.mLayerState.mOpacityOverride != 0) {
            return this.mLayerState.mOpacityOverride;
        }
        return this.mLayerState.getOpacity();
    }

    @Override
    public void setAutoMirrored(boolean z) {
        this.mLayerState.mAutoMirrored = z;
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.setAutoMirrored(z);
            }
        }
    }

    @Override
    public boolean isAutoMirrored() {
        return this.mLayerState.mAutoMirrored;
    }

    @Override
    public void jumpToCurrentState() {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.jumpToCurrentState();
            }
        }
    }

    @Override
    public boolean isStateful() {
        return this.mLayerState.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return this.mLayerState.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(int[] iArr) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        boolean z = false;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null && drawable.isStateful() && drawable.setState(iArr)) {
                refreshChildPadding(i2, childDrawableArr[i2]);
                z = true;
            }
        }
        if (z) {
            updateLayerBounds(getBounds());
        }
        return z;
    }

    @Override
    protected boolean onLevelChange(int i) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i2 = this.mLayerState.mNumChildren;
        boolean z = false;
        for (int i3 = 0; i3 < i2; i3++) {
            Drawable drawable = childDrawableArr[i3].mDrawable;
            if (drawable != null && drawable.setLevel(i)) {
                refreshChildPadding(i3, childDrawableArr[i3]);
                z = true;
            }
        }
        if (z) {
            updateLayerBounds(getBounds());
        }
        return z;
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        updateLayerBounds(rect);
    }

    private void updateLayerBounds(Rect rect) {
        try {
            suspendChildInvalidation();
            updateLayerBoundsInternal(rect);
        } finally {
            resumeChildInvalidation();
        }
    }

    private void updateLayerBoundsInternal(Rect rect) {
        boolean z;
        ChildDrawable[] childDrawableArr;
        Rect rect2 = this.mTmpOutRect;
        int layoutDirection = getLayoutDirection();
        boolean z2 = layoutDirection == 1;
        boolean z3 = this.mLayerState.mPaddingMode == 0;
        ChildDrawable[] childDrawableArr2 = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        while (i2 < i) {
            ChildDrawable childDrawable = childDrawableArr2[i2];
            Drawable drawable = childDrawable.mDrawable;
            if (drawable == null) {
                z = z2;
                childDrawableArr = childDrawableArr2;
            } else {
                int i7 = childDrawable.mInsetT;
                int i8 = childDrawable.mInsetB;
                int i9 = z2 ? childDrawable.mInsetE : childDrawable.mInsetS;
                int i10 = z2 ? childDrawable.mInsetS : childDrawable.mInsetE;
                z = z2;
                if (i9 == Integer.MIN_VALUE) {
                    i9 = childDrawable.mInsetL;
                }
                if (i10 == Integer.MIN_VALUE) {
                    i10 = childDrawable.mInsetR;
                }
                Rect rect3 = this.mTmpContainer;
                childDrawableArr = childDrawableArr2;
                rect3.set(rect.left + i9 + i3, rect.top + i7 + i4, (rect.right - i10) - i5, (rect.bottom - i8) - i6);
                int intrinsicWidth = drawable.getIntrinsicWidth();
                int intrinsicHeight = drawable.getIntrinsicHeight();
                int i11 = childDrawable.mWidth;
                int i12 = childDrawable.mHeight;
                int iResolveGravity = resolveGravity(childDrawable.mGravity, i11, i12, intrinsicWidth, intrinsicHeight);
                if (i11 >= 0) {
                    intrinsicWidth = i11;
                }
                if (i12 >= 0) {
                    intrinsicHeight = i12;
                }
                Gravity.apply(iResolveGravity, intrinsicWidth, intrinsicHeight, rect3, rect2, layoutDirection);
                drawable.setBounds(rect2);
                if (z3) {
                    i3 += this.mPaddingL[i2];
                    i5 += this.mPaddingR[i2];
                    i4 += this.mPaddingT[i2];
                    i6 += this.mPaddingB[i2];
                }
            }
            i2++;
            z2 = z;
            childDrawableArr2 = childDrawableArr;
        }
    }

    private static int resolveGravity(int i, int i2, int i3, int i4, int i5) {
        if (!Gravity.isHorizontal(i)) {
            if (i2 < 0) {
                i |= 7;
            } else {
                i |= Gravity.START;
            }
        }
        if (!Gravity.isVertical(i)) {
            if (i3 < 0) {
                i |= 112;
            } else {
                i |= 48;
            }
        }
        if (i2 < 0 && i4 < 0) {
            i |= 7;
        }
        if (i3 < 0 && i5 < 0) {
            return i | 112;
        }
        return i;
    }

    @Override
    public int getIntrinsicWidth() {
        int i;
        boolean z = this.mLayerState.mPaddingMode == 0;
        boolean z2 = getLayoutDirection() == 1;
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i2 = this.mLayerState.mNumChildren;
        int i3 = 0;
        int i4 = 0;
        int i5 = -1;
        for (int i6 = 0; i6 < i2; i6++) {
            ChildDrawable childDrawable = childDrawableArr[i6];
            if (childDrawable.mDrawable != null) {
                int i7 = z2 ? childDrawable.mInsetE : childDrawable.mInsetS;
                int i8 = z2 ? childDrawable.mInsetS : childDrawable.mInsetE;
                if (i7 == Integer.MIN_VALUE) {
                    i7 = childDrawable.mInsetL;
                }
                if (i8 == Integer.MIN_VALUE) {
                    i8 = childDrawable.mInsetR;
                }
                int intrinsicWidth = childDrawable.mWidth < 0 ? childDrawable.mDrawable.getIntrinsicWidth() : childDrawable.mWidth;
                if (intrinsicWidth >= 0) {
                    i = intrinsicWidth + i7 + i8 + i3 + i4;
                } else {
                    i = -1;
                }
                if (i > i5) {
                    i5 = i;
                }
                if (z) {
                    i3 += this.mPaddingL[i6];
                    i4 += this.mPaddingR[i6];
                }
            }
        }
        return i5;
    }

    @Override
    public int getIntrinsicHeight() {
        int i;
        boolean z = this.mLayerState.mPaddingMode == 0;
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i2 = this.mLayerState.mNumChildren;
        int i3 = 0;
        int i4 = 0;
        int i5 = -1;
        for (int i6 = 0; i6 < i2; i6++) {
            ChildDrawable childDrawable = childDrawableArr[i6];
            if (childDrawable.mDrawable != null) {
                int intrinsicHeight = childDrawable.mHeight < 0 ? childDrawable.mDrawable.getIntrinsicHeight() : childDrawable.mHeight;
                if (intrinsicHeight >= 0) {
                    i = intrinsicHeight + childDrawable.mInsetT + childDrawable.mInsetB + i3 + i4;
                } else {
                    i = -1;
                }
                if (i > i5) {
                    i5 = i;
                }
                if (z) {
                    i3 += this.mPaddingT[i6];
                    i4 += this.mPaddingB[i6];
                }
            }
        }
        return i5;
    }

    private boolean refreshChildPadding(int i, ChildDrawable childDrawable) {
        if (childDrawable.mDrawable != null) {
            Rect rect = this.mTmpRect;
            childDrawable.mDrawable.getPadding(rect);
            if (rect.left != this.mPaddingL[i] || rect.top != this.mPaddingT[i] || rect.right != this.mPaddingR[i] || rect.bottom != this.mPaddingB[i]) {
                this.mPaddingL[i] = rect.left;
                this.mPaddingT[i] = rect.top;
                this.mPaddingR[i] = rect.right;
                this.mPaddingB[i] = rect.bottom;
                return true;
            }
            return false;
        }
        return false;
    }

    void ensurePadding() {
        int i = this.mLayerState.mNumChildren;
        if (this.mPaddingL != null && this.mPaddingL.length >= i) {
            return;
        }
        this.mPaddingL = new int[i];
        this.mPaddingT = new int[i];
        this.mPaddingR = new int[i];
        this.mPaddingB = new int[i];
    }

    void refreshPadding() {
        int i = this.mLayerState.mNumChildren;
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        for (int i2 = 0; i2 < i; i2++) {
            refreshChildPadding(i2, childDrawableArr[i2]);
        }
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        if (this.mLayerState.canConstantState()) {
            this.mLayerState.mChangingConfigurations = getChangingConfigurations();
            return this.mLayerState;
        }
        return null;
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mLayerState = createConstantState(this.mLayerState, null);
            ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
            int i = this.mLayerState.mNumChildren;
            for (int i2 = 0; i2 < i; i2++) {
                Drawable drawable = childDrawableArr[i2].mDrawable;
                if (drawable != null) {
                    drawable.mutate();
                }
            }
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i = this.mLayerState.mNumChildren;
        for (int i2 = 0; i2 < i; i2++) {
            Drawable drawable = childDrawableArr[i2].mDrawable;
            if (drawable != null) {
                drawable.clearMutated();
            }
        }
        this.mMutated = false;
    }

    @Override
    public boolean onLayoutDirectionChanged(int i) {
        ChildDrawable[] childDrawableArr = this.mLayerState.mChildren;
        int i2 = this.mLayerState.mNumChildren;
        boolean layoutDirection = false;
        for (int i3 = 0; i3 < i2; i3++) {
            Drawable drawable = childDrawableArr[i3].mDrawable;
            if (drawable != null) {
                layoutDirection |= drawable.setLayoutDirection(i);
            }
        }
        updateLayerBounds(getBounds());
        return layoutDirection;
    }

    static class ChildDrawable {
        public int mDensity;
        public Drawable mDrawable;
        public int mGravity;
        public int mHeight;
        public int mId;
        public int mInsetB;
        public int mInsetE;
        public int mInsetL;
        public int mInsetR;
        public int mInsetS;
        public int mInsetT;
        public int[] mThemeAttrs;
        public int mWidth;

        ChildDrawable(int i) {
            this.mDensity = 160;
            this.mInsetS = Integer.MIN_VALUE;
            this.mInsetE = Integer.MIN_VALUE;
            this.mWidth = -1;
            this.mHeight = -1;
            this.mGravity = 0;
            this.mId = -1;
            this.mDensity = i;
        }

        ChildDrawable(ChildDrawable childDrawable, LayerDrawable layerDrawable, Resources resources) {
            Drawable drawableNewDrawable;
            this.mDensity = 160;
            this.mInsetS = Integer.MIN_VALUE;
            this.mInsetE = Integer.MIN_VALUE;
            this.mWidth = -1;
            this.mHeight = -1;
            this.mGravity = 0;
            this.mId = -1;
            Drawable drawable = childDrawable.mDrawable;
            if (drawable != null) {
                Drawable.ConstantState constantState = drawable.getConstantState();
                if (constantState == null) {
                    if (drawable.getCallback() != null) {
                        Log.w(LayerDrawable.LOG_TAG, "Invalid drawable added to LayerDrawable! Drawable already belongs to another owner but does not expose a constant state.", new RuntimeException());
                    }
                    drawableNewDrawable = drawable;
                } else if (resources != null) {
                    drawableNewDrawable = constantState.newDrawable(resources);
                } else {
                    drawableNewDrawable = constantState.newDrawable();
                }
                drawableNewDrawable.setLayoutDirection(drawable.getLayoutDirection());
                drawableNewDrawable.setBounds(drawable.getBounds());
                drawableNewDrawable.setLevel(drawable.getLevel());
                drawableNewDrawable.setCallback(layerDrawable);
            } else {
                drawableNewDrawable = null;
            }
            this.mDrawable = drawableNewDrawable;
            this.mThemeAttrs = childDrawable.mThemeAttrs;
            this.mInsetL = childDrawable.mInsetL;
            this.mInsetT = childDrawable.mInsetT;
            this.mInsetR = childDrawable.mInsetR;
            this.mInsetB = childDrawable.mInsetB;
            this.mInsetS = childDrawable.mInsetS;
            this.mInsetE = childDrawable.mInsetE;
            this.mWidth = childDrawable.mWidth;
            this.mHeight = childDrawable.mHeight;
            this.mGravity = childDrawable.mGravity;
            this.mId = childDrawable.mId;
            this.mDensity = Drawable.resolveDensity(resources, childDrawable.mDensity);
            if (childDrawable.mDensity != this.mDensity) {
                applyDensityScaling(childDrawable.mDensity, this.mDensity);
            }
        }

        public boolean canApplyTheme() {
            return this.mThemeAttrs != null || (this.mDrawable != null && this.mDrawable.canApplyTheme());
        }

        public final void setDensity(int i) {
            if (this.mDensity != i) {
                int i2 = this.mDensity;
                this.mDensity = i;
                applyDensityScaling(i2, i);
            }
        }

        private void applyDensityScaling(int i, int i2) {
            this.mInsetL = Drawable.scaleFromDensity(this.mInsetL, i, i2, false);
            this.mInsetT = Drawable.scaleFromDensity(this.mInsetT, i, i2, false);
            this.mInsetR = Drawable.scaleFromDensity(this.mInsetR, i, i2, false);
            this.mInsetB = Drawable.scaleFromDensity(this.mInsetB, i, i2, false);
            if (this.mInsetS != Integer.MIN_VALUE) {
                this.mInsetS = Drawable.scaleFromDensity(this.mInsetS, i, i2, false);
            }
            if (this.mInsetE != Integer.MIN_VALUE) {
                this.mInsetE = Drawable.scaleFromDensity(this.mInsetE, i, i2, false);
            }
            if (this.mWidth > 0) {
                this.mWidth = Drawable.scaleFromDensity(this.mWidth, i, i2, true);
            }
            if (this.mHeight > 0) {
                this.mHeight = Drawable.scaleFromDensity(this.mHeight, i, i2, true);
            }
        }
    }

    static class LayerState extends Drawable.ConstantState {
        private boolean mAutoMirrored;
        int mChangingConfigurations;
        private boolean mCheckedOpacity;
        private boolean mCheckedStateful;
        ChildDrawable[] mChildren;
        int mChildrenChangingConfigurations;
        int mDensity;
        private boolean mIsStateful;
        int mNumChildren;
        private int mOpacity;
        int mOpacityOverride;
        int mPaddingBottom;
        int mPaddingEnd;
        int mPaddingLeft;
        private int mPaddingMode;
        int mPaddingRight;
        int mPaddingStart;
        int mPaddingTop;
        private int[] mThemeAttrs;

        LayerState(LayerState layerState, LayerDrawable layerDrawable, Resources resources) {
            this.mPaddingTop = -1;
            this.mPaddingBottom = -1;
            this.mPaddingLeft = -1;
            this.mPaddingRight = -1;
            this.mPaddingStart = -1;
            this.mPaddingEnd = -1;
            this.mOpacityOverride = 0;
            this.mAutoMirrored = false;
            this.mPaddingMode = 0;
            this.mDensity = Drawable.resolveDensity(resources, layerState != null ? layerState.mDensity : 0);
            if (layerState != null) {
                ChildDrawable[] childDrawableArr = layerState.mChildren;
                int i = layerState.mNumChildren;
                this.mNumChildren = i;
                this.mChildren = new ChildDrawable[i];
                this.mChangingConfigurations = layerState.mChangingConfigurations;
                this.mChildrenChangingConfigurations = layerState.mChildrenChangingConfigurations;
                for (int i2 = 0; i2 < i; i2++) {
                    this.mChildren[i2] = new ChildDrawable(childDrawableArr[i2], layerDrawable, resources);
                }
                this.mCheckedOpacity = layerState.mCheckedOpacity;
                this.mOpacity = layerState.mOpacity;
                this.mCheckedStateful = layerState.mCheckedStateful;
                this.mIsStateful = layerState.mIsStateful;
                this.mAutoMirrored = layerState.mAutoMirrored;
                this.mPaddingMode = layerState.mPaddingMode;
                this.mThemeAttrs = layerState.mThemeAttrs;
                this.mPaddingTop = layerState.mPaddingTop;
                this.mPaddingBottom = layerState.mPaddingBottom;
                this.mPaddingLeft = layerState.mPaddingLeft;
                this.mPaddingRight = layerState.mPaddingRight;
                this.mPaddingStart = layerState.mPaddingStart;
                this.mPaddingEnd = layerState.mPaddingEnd;
                this.mOpacityOverride = layerState.mOpacityOverride;
                if (layerState.mDensity != this.mDensity) {
                    applyDensityScaling(layerState.mDensity, this.mDensity);
                    return;
                }
                return;
            }
            this.mNumChildren = 0;
            this.mChildren = null;
        }

        public final void setDensity(int i) {
            if (this.mDensity != i) {
                int i2 = this.mDensity;
                this.mDensity = i;
                onDensityChanged(i2, i);
            }
        }

        protected void onDensityChanged(int i, int i2) {
            applyDensityScaling(i, i2);
        }

        private void applyDensityScaling(int i, int i2) {
            if (this.mPaddingLeft > 0) {
                this.mPaddingLeft = Drawable.scaleFromDensity(this.mPaddingLeft, i, i2, false);
            }
            if (this.mPaddingTop > 0) {
                this.mPaddingTop = Drawable.scaleFromDensity(this.mPaddingTop, i, i2, false);
            }
            if (this.mPaddingRight > 0) {
                this.mPaddingRight = Drawable.scaleFromDensity(this.mPaddingRight, i, i2, false);
            }
            if (this.mPaddingBottom > 0) {
                this.mPaddingBottom = Drawable.scaleFromDensity(this.mPaddingBottom, i, i2, false);
            }
            if (this.mPaddingStart > 0) {
                this.mPaddingStart = Drawable.scaleFromDensity(this.mPaddingStart, i, i2, false);
            }
            if (this.mPaddingEnd > 0) {
                this.mPaddingEnd = Drawable.scaleFromDensity(this.mPaddingEnd, i, i2, false);
            }
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null || super.canApplyTheme()) {
                return true;
            }
            ChildDrawable[] childDrawableArr = this.mChildren;
            int i = this.mNumChildren;
            for (int i2 = 0; i2 < i; i2++) {
                if (childDrawableArr[i2].canApplyTheme()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Drawable newDrawable() {
            return new LayerDrawable(this, (Resources) null);
        }

        @Override
        public Drawable newDrawable(Resources resources) {
            return new LayerDrawable(this, resources);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations | this.mChildrenChangingConfigurations;
        }

        public final int getOpacity() {
            int iResolveOpacity;
            if (this.mCheckedOpacity) {
                return this.mOpacity;
            }
            int i = this.mNumChildren;
            ChildDrawable[] childDrawableArr = this.mChildren;
            int i2 = -1;
            int i3 = 0;
            while (true) {
                if (i3 >= i) {
                    break;
                }
                if (childDrawableArr[i3].mDrawable == null) {
                    i3++;
                } else {
                    i2 = i3;
                    break;
                }
            }
            if (i2 >= 0) {
                iResolveOpacity = childDrawableArr[i2].mDrawable.getOpacity();
            } else {
                iResolveOpacity = -2;
            }
            for (int i4 = i2 + 1; i4 < i; i4++) {
                Drawable drawable = childDrawableArr[i4].mDrawable;
                if (drawable != null) {
                    iResolveOpacity = Drawable.resolveOpacity(iResolveOpacity, drawable.getOpacity());
                }
            }
            this.mOpacity = iResolveOpacity;
            this.mCheckedOpacity = true;
            return iResolveOpacity;
        }

        public final boolean isStateful() {
            if (this.mCheckedStateful) {
                return this.mIsStateful;
            }
            int i = this.mNumChildren;
            ChildDrawable[] childDrawableArr = this.mChildren;
            boolean z = false;
            int i2 = 0;
            while (true) {
                if (i2 < i) {
                    Drawable drawable = childDrawableArr[i2].mDrawable;
                    if (drawable == null || !drawable.isStateful()) {
                        i2++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            this.mIsStateful = z;
            this.mCheckedStateful = true;
            return z;
        }

        public final boolean hasFocusStateSpecified() {
            int i = this.mNumChildren;
            ChildDrawable[] childDrawableArr = this.mChildren;
            for (int i2 = 0; i2 < i; i2++) {
                Drawable drawable = childDrawableArr[i2].mDrawable;
                if (drawable != null && drawable.hasFocusStateSpecified()) {
                    return true;
                }
            }
            return false;
        }

        public final boolean canConstantState() {
            ChildDrawable[] childDrawableArr = this.mChildren;
            int i = this.mNumChildren;
            for (int i2 = 0; i2 < i; i2++) {
                Drawable drawable = childDrawableArr[i2].mDrawable;
                if (drawable != null && drawable.getConstantState() == null) {
                    return false;
                }
            }
            return true;
        }

        void invalidateCache() {
            this.mCheckedOpacity = false;
            this.mCheckedStateful = false;
        }
    }
}

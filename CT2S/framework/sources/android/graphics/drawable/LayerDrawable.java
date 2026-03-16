package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.android.internal.R;
import java.io.IOException;
import java.util.Collection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class LayerDrawable extends Drawable implements Drawable.Callback {
    public static final int PADDING_MODE_NEST = 0;
    public static final int PADDING_MODE_STACK = 1;
    private Rect mHotspotBounds;
    LayerState mLayerState;
    private boolean mMutated;
    private int mOpacityOverride;
    private int[] mPaddingB;
    private int[] mPaddingL;
    private int[] mPaddingR;
    private int[] mPaddingT;
    private final Rect mTmpRect;

    public LayerDrawable(Drawable[] layers) {
        this(layers, (LayerState) null);
    }

    LayerDrawable(Drawable[] layers, LayerState state) {
        this(state, (Resources) null);
        int length = layers.length;
        ChildDrawable[] r = new ChildDrawable[length];
        for (int i = 0; i < length; i++) {
            r[i] = new ChildDrawable();
            r[i].mDrawable = layers[i];
            layers[i].setCallback(this);
            this.mLayerState.mChildrenChangingConfigurations |= layers[i].getChangingConfigurations();
        }
        this.mLayerState.mNum = length;
        this.mLayerState.mChildren = r;
        ensurePadding();
    }

    LayerDrawable() {
        this((LayerState) null, (Resources) null);
    }

    LayerDrawable(LayerState state, Resources res) {
        this.mOpacityOverride = 0;
        this.mTmpRect = new Rect();
        this.mLayerState = createConstantState(state, res);
        if (this.mLayerState.mNum > 0) {
            ensurePadding();
        }
    }

    LayerState createConstantState(LayerState state, Resources res) {
        return new LayerState(state, this, res);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.LayerDrawable);
        updateStateFromTypedArray(a);
        a.recycle();
        inflateLayers(r, parser, attrs, theme);
        ensurePadding();
        onStateChange(getState());
    }

    private void updateStateFromTypedArray(TypedArray a) {
        LayerState state = this.mLayerState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        this.mOpacityOverride = a.getInt(0, this.mOpacityOverride);
        state.mAutoMirrored = a.getBoolean(1, state.mAutoMirrored);
        state.mPaddingMode = a.getInteger(2, state.mPaddingMode);
    }

    private void inflateLayers(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        int type;
        LayerState state = this.mLayerState;
        int innerDepth = parser.getDepth() + 1;
        while (true) {
            int type2 = parser.next();
            if (type2 == 1) {
                return;
            }
            int depth = parser.getDepth();
            if (depth >= innerDepth || type2 != 3) {
                if (type2 == 2 && depth <= innerDepth && parser.getName().equals("item")) {
                    ChildDrawable layer = new ChildDrawable();
                    TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.LayerDrawableItem);
                    updateLayerFromTypedArray(layer, a);
                    a.recycle();
                    if (layer.mDrawable == null) {
                        do {
                            type = parser.next();
                        } while (type == 4);
                        if (type != 2) {
                            throw new XmlPullParserException(parser.getPositionDescription() + ": <item> tag requires a 'drawable' attribute or child tag defining a drawable");
                        }
                        layer.mDrawable = Drawable.createFromXmlInner(r, parser, attrs, theme);
                    }
                    if (layer.mDrawable != null) {
                        state.mChildrenChangingConfigurations |= layer.mDrawable.getChangingConfigurations();
                        layer.mDrawable.setCallback(this);
                    }
                    addLayer(layer);
                }
            } else {
                return;
            }
        }
    }

    private void updateLayerFromTypedArray(ChildDrawable layer, TypedArray a) {
        LayerState state = this.mLayerState;
        state.mChildrenChangingConfigurations |= a.getChangingConfigurations();
        layer.mThemeAttrs = a.extractThemeAttrs();
        layer.mInsetL = a.getDimensionPixelOffset(2, layer.mInsetL);
        layer.mInsetT = a.getDimensionPixelOffset(3, layer.mInsetT);
        layer.mInsetR = a.getDimensionPixelOffset(4, layer.mInsetR);
        layer.mInsetB = a.getDimensionPixelOffset(5, layer.mInsetB);
        layer.mId = a.getResourceId(0, layer.mId);
        Drawable dr = a.getDrawable(1);
        if (dr != null) {
            layer.mDrawable = dr;
        }
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        LayerState state = this.mLayerState;
        if (state != null) {
            if (state.mThemeAttrs != null) {
                TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.LayerDrawable);
                updateStateFromTypedArray(a);
                a.recycle();
            }
            ChildDrawable[] array = state.mChildren;
            int N = state.mNum;
            for (int i = 0; i < N; i++) {
                ChildDrawable layer = array[i];
                if (layer.mThemeAttrs != null) {
                    TypedArray a2 = t.resolveAttributes(layer.mThemeAttrs, R.styleable.LayerDrawableItem);
                    updateLayerFromTypedArray(layer, a2);
                    a2.recycle();
                }
                Drawable d = layer.mDrawable;
                if (d.canApplyTheme()) {
                    d.applyTheme(t);
                }
            }
            ensurePadding();
            onStateChange(getState());
        }
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mLayerState != null && this.mLayerState.canApplyTheme()) || super.canApplyTheme();
    }

    @Override
    public boolean isProjected() {
        if (super.isProjected()) {
            return true;
        }
        ChildDrawable[] layers = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            if (layers[i].mDrawable.isProjected()) {
                return true;
            }
        }
        return false;
    }

    void addLayer(ChildDrawable layer) {
        LayerState st = this.mLayerState;
        int N = st.mChildren != null ? st.mChildren.length : 0;
        int i = st.mNum;
        if (i >= N) {
            ChildDrawable[] nu = new ChildDrawable[N + 10];
            if (i > 0) {
                System.arraycopy(st.mChildren, 0, nu, 0, i);
            }
            st.mChildren = nu;
        }
        st.mChildren[i] = layer;
        st.mNum++;
        st.invalidateCache();
    }

    ChildDrawable addLayer(Drawable layer, int[] themeAttrs, int id, int left, int top, int right, int bottom) {
        ChildDrawable childDrawable = new ChildDrawable();
        childDrawable.mId = id;
        childDrawable.mThemeAttrs = themeAttrs;
        childDrawable.mDrawable = layer;
        childDrawable.mDrawable.setAutoMirrored(isAutoMirrored());
        childDrawable.mInsetL = left;
        childDrawable.mInsetT = top;
        childDrawable.mInsetR = right;
        childDrawable.mInsetB = bottom;
        addLayer(childDrawable);
        this.mLayerState.mChildrenChangingConfigurations |= layer.getChangingConfigurations();
        layer.setCallback(this);
        return childDrawable;
    }

    public Drawable findDrawableByLayerId(int id) {
        ChildDrawable[] layers = this.mLayerState.mChildren;
        for (int i = this.mLayerState.mNum - 1; i >= 0; i--) {
            if (layers[i].mId == id) {
                return layers[i].mDrawable;
            }
        }
        return null;
    }

    public void setId(int index, int id) {
        this.mLayerState.mChildren[index].mId = id;
    }

    public int getNumberOfLayers() {
        return this.mLayerState.mNum;
    }

    public Drawable getDrawable(int index) {
        return this.mLayerState.mChildren[index].mDrawable;
    }

    public int getId(int index) {
        return this.mLayerState.mChildren[index].mId;
    }

    public boolean setDrawableByLayerId(int id, Drawable drawable) {
        ChildDrawable[] layers = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            ChildDrawable childDrawable = layers[i];
            if (childDrawable.mId == id) {
                if (childDrawable.mDrawable != null) {
                    if (drawable != null) {
                        Rect bounds = childDrawable.mDrawable.getBounds();
                        drawable.setBounds(bounds);
                    }
                    childDrawable.mDrawable.setCallback(null);
                }
                if (drawable != null) {
                    drawable.setCallback(this);
                }
                childDrawable.mDrawable = drawable;
                this.mLayerState.invalidateCache();
                return true;
            }
        }
        return false;
    }

    public void setLayerInset(int index, int l, int t, int r, int b) {
        ChildDrawable childDrawable = this.mLayerState.mChildren[index];
        childDrawable.mInsetL = l;
        childDrawable.mInsetT = t;
        childDrawable.mInsetR = r;
        childDrawable.mInsetB = b;
    }

    public void setPaddingMode(int mode) {
        if (this.mLayerState.mPaddingMode == mode) {
            return;
        }
        this.mLayerState.mPaddingMode = mode;
    }

    public int getPaddingMode() {
        return this.mLayerState.mPaddingMode;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    @Override
    public void draw(Canvas canvas) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.draw(canvas);
        }
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mLayerState.mChangingConfigurations | this.mLayerState.mChildrenChangingConfigurations;
    }

    @Override
    public boolean getPadding(Rect padding) {
        if (this.mLayerState.mPaddingMode == 0) {
            computeNestedPadding(padding);
        } else {
            computeStackedPadding(padding);
        }
        return (padding.left == 0 && padding.top == 0 && padding.right == 0 && padding.bottom == 0) ? false : true;
    }

    private void computeNestedPadding(Rect padding) {
        padding.left = 0;
        padding.top = 0;
        padding.right = 0;
        padding.bottom = 0;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            refreshChildPadding(i, array[i]);
            padding.left += this.mPaddingL[i];
            padding.top += this.mPaddingT[i];
            padding.right += this.mPaddingR[i];
            padding.bottom += this.mPaddingB[i];
        }
    }

    private void computeStackedPadding(Rect padding) {
        padding.left = 0;
        padding.top = 0;
        padding.right = 0;
        padding.bottom = 0;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            refreshChildPadding(i, array[i]);
            padding.left = Math.max(padding.left, this.mPaddingL[i]);
            padding.top = Math.max(padding.top, this.mPaddingT[i]);
            padding.right = Math.max(padding.right, this.mPaddingR[i]);
            padding.bottom = Math.max(padding.bottom, this.mPaddingB[i]);
        }
    }

    @Override
    public void getOutline(Outline outline) {
        LayerState state = this.mLayerState;
        ChildDrawable[] children = state.mChildren;
        int N = state.mNum;
        for (int i = 0; i < N; i++) {
            children[i].mDrawable.getOutline(outline);
            if (!outline.isEmpty()) {
                return;
            }
        }
    }

    @Override
    public void setHotspot(float x, float y) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setHotspot(x, y);
        }
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setHotspotBounds(left, top, right, bottom);
        }
        if (this.mHotspotBounds == null) {
            this.mHotspotBounds = new Rect(left, top, right, bottom);
        } else {
            this.mHotspotBounds.set(left, top, right, bottom);
        }
    }

    @Override
    public void getHotspotBounds(Rect outRect) {
        if (this.mHotspotBounds != null) {
            outRect.set(this.mHotspotBounds);
        } else {
            super.getHotspotBounds(outRect);
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setVisible(visible, restart);
        }
        return changed;
    }

    @Override
    public void setDither(boolean dither) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setDither(dither);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setAlpha(alpha);
        }
    }

    @Override
    public int getAlpha() {
        ChildDrawable[] array = this.mLayerState.mChildren;
        return this.mLayerState.mNum > 0 ? array[0].mDrawable.getAlpha() : super.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setColorFilter(cf);
        }
    }

    @Override
    public void setTintList(ColorStateList tint) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setTintList(tint);
        }
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setTintMode(tintMode);
        }
    }

    public void setOpacity(int opacity) {
        this.mOpacityOverride = opacity;
    }

    @Override
    public int getOpacity() {
        return this.mOpacityOverride != 0 ? this.mOpacityOverride : this.mLayerState.getOpacity();
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        this.mLayerState.mAutoMirrored = mirrored;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setAutoMirrored(mirrored);
        }
    }

    @Override
    public boolean isAutoMirrored() {
        return this.mLayerState.mAutoMirrored;
    }

    @Override
    public boolean isStateful() {
        return this.mLayerState.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean paddingChanged = false;
        boolean changed = false;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            ChildDrawable r = array[i];
            if (r.mDrawable.isStateful() && r.mDrawable.setState(state)) {
                changed = true;
            }
            if (refreshChildPadding(i, r)) {
                paddingChanged = true;
            }
        }
        if (paddingChanged) {
            onBoundsChange(getBounds());
        }
        return changed;
    }

    @Override
    protected boolean onLevelChange(int level) {
        boolean paddingChanged = false;
        boolean changed = false;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            ChildDrawable r = array[i];
            if (r.mDrawable.setLevel(level)) {
                changed = true;
            }
            if (refreshChildPadding(i, r)) {
                paddingChanged = true;
            }
        }
        if (paddingChanged) {
            onBoundsChange(getBounds());
        }
        return changed;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        int padL = 0;
        int padT = 0;
        int padR = 0;
        int padB = 0;
        boolean nest = this.mLayerState.mPaddingMode == 0;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            ChildDrawable r = array[i];
            r.mDrawable.setBounds(bounds.left + r.mInsetL + padL, bounds.top + r.mInsetT + padT, (bounds.right - r.mInsetR) - padR, (bounds.bottom - r.mInsetB) - padB);
            if (nest) {
                padL += this.mPaddingL[i];
                padR += this.mPaddingR[i];
                padT += this.mPaddingT[i];
                padB += this.mPaddingB[i];
            }
        }
    }

    @Override
    public int getIntrinsicWidth() {
        int width = -1;
        int padL = 0;
        int padR = 0;
        boolean nest = this.mLayerState.mPaddingMode == 0;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            ChildDrawable r = array[i];
            int w = r.mDrawable.getIntrinsicWidth() + r.mInsetL + r.mInsetR + padL + padR;
            if (w > width) {
                width = w;
            }
            if (nest) {
                padL += this.mPaddingL[i];
                padR += this.mPaddingR[i];
            }
        }
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        int height = -1;
        int padT = 0;
        int padB = 0;
        boolean nest = this.mLayerState.mPaddingMode == 0;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            ChildDrawable r = array[i];
            int h = r.mDrawable.getIntrinsicHeight() + r.mInsetT + r.mInsetB + padT + padB;
            if (h > height) {
                height = h;
            }
            if (nest) {
                padT += this.mPaddingT[i];
                padB += this.mPaddingB[i];
            }
        }
        return height;
    }

    private boolean refreshChildPadding(int i, ChildDrawable r) {
        Rect rect = this.mTmpRect;
        r.mDrawable.getPadding(rect);
        if (rect.left == this.mPaddingL[i] && rect.top == this.mPaddingT[i] && rect.right == this.mPaddingR[i] && rect.bottom == this.mPaddingB[i]) {
            return false;
        }
        this.mPaddingL[i] = rect.left;
        this.mPaddingT[i] = rect.top;
        this.mPaddingR[i] = rect.right;
        this.mPaddingB[i] = rect.bottom;
        return true;
    }

    void ensurePadding() {
        int N = this.mLayerState.mNum;
        if (this.mPaddingL == null || this.mPaddingL.length < N) {
            this.mPaddingL = new int[N];
            this.mPaddingT = new int[N];
            this.mPaddingR = new int[N];
            this.mPaddingB = new int[N];
        }
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        if (!this.mLayerState.canConstantState()) {
            return null;
        }
        this.mLayerState.mChangingConfigurations = getChangingConfigurations();
        return this.mLayerState;
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mLayerState = createConstantState(this.mLayerState, null);
            ChildDrawable[] array = this.mLayerState.mChildren;
            int N = this.mLayerState.mNum;
            for (int i = 0; i < N; i++) {
                array[i].mDrawable.mutate();
            }
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.clearMutated();
        }
        this.mMutated = false;
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int N = this.mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            array[i].mDrawable.setLayoutDirection(layoutDirection);
        }
        super.setLayoutDirection(layoutDirection);
    }

    static class ChildDrawable {
        public Drawable mDrawable;
        public int mId;
        public int mInsetB;
        public int mInsetL;
        public int mInsetR;
        public int mInsetT;
        public int[] mThemeAttrs;

        ChildDrawable() {
            this.mId = -1;
        }

        ChildDrawable(ChildDrawable orig, LayerDrawable owner, Resources res) {
            this.mId = -1;
            if (res != null) {
                this.mDrawable = orig.mDrawable.getConstantState().newDrawable(res);
            } else {
                this.mDrawable = orig.mDrawable.getConstantState().newDrawable();
            }
            this.mDrawable.setCallback(owner);
            this.mDrawable.setLayoutDirection(orig.mDrawable.getLayoutDirection());
            this.mDrawable.setBounds(orig.mDrawable.getBounds());
            this.mDrawable.setLevel(orig.mDrawable.getLevel());
            this.mThemeAttrs = orig.mThemeAttrs;
            this.mInsetL = orig.mInsetL;
            this.mInsetT = orig.mInsetT;
            this.mInsetR = orig.mInsetR;
            this.mInsetB = orig.mInsetB;
            this.mId = orig.mId;
        }
    }

    static class LayerState extends Drawable.ConstantState {
        private boolean mAutoMirrored;
        int mChangingConfigurations;
        ChildDrawable[] mChildren;
        int mChildrenChangingConfigurations;
        private boolean mHaveIsStateful;
        private boolean mHaveOpacity;
        private boolean mIsStateful;
        int mNum;
        private int mOpacity;
        private int mPaddingMode;
        int[] mThemeAttrs;

        LayerState(LayerState orig, LayerDrawable owner, Resources res) {
            this.mAutoMirrored = false;
            this.mPaddingMode = 0;
            if (orig != null) {
                ChildDrawable[] origChildDrawable = orig.mChildren;
                int N = orig.mNum;
                this.mNum = N;
                this.mChildren = new ChildDrawable[N];
                this.mChangingConfigurations = orig.mChangingConfigurations;
                this.mChildrenChangingConfigurations = orig.mChildrenChangingConfigurations;
                for (int i = 0; i < N; i++) {
                    ChildDrawable or = origChildDrawable[i];
                    this.mChildren[i] = new ChildDrawable(or, owner, res);
                }
                this.mHaveOpacity = orig.mHaveOpacity;
                this.mOpacity = orig.mOpacity;
                this.mHaveIsStateful = orig.mHaveIsStateful;
                this.mIsStateful = orig.mIsStateful;
                this.mAutoMirrored = orig.mAutoMirrored;
                this.mPaddingMode = orig.mPaddingMode;
                this.mThemeAttrs = orig.mThemeAttrs;
                return;
            }
            this.mNum = 0;
            this.mChildren = null;
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null || super.canApplyTheme()) {
                return true;
            }
            ChildDrawable[] array = this.mChildren;
            int N = this.mNum;
            for (int i = 0; i < N; i++) {
                ChildDrawable layer = array[i];
                if (layer.mThemeAttrs != null || layer.mDrawable.canApplyTheme()) {
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
        public Drawable newDrawable(Resources res) {
            return new LayerDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }

        public final int getOpacity() {
            if (this.mHaveOpacity) {
                return this.mOpacity;
            }
            ChildDrawable[] array = this.mChildren;
            int N = this.mNum;
            int op = N > 0 ? array[0].mDrawable.getOpacity() : -2;
            for (int i = 1; i < N; i++) {
                op = Drawable.resolveOpacity(op, array[i].mDrawable.getOpacity());
            }
            this.mOpacity = op;
            this.mHaveOpacity = true;
            return op;
        }

        public final boolean isStateful() {
            if (this.mHaveIsStateful) {
                return this.mIsStateful;
            }
            ChildDrawable[] array = this.mChildren;
            int N = this.mNum;
            boolean isStateful = false;
            int i = 0;
            while (true) {
                if (i >= N) {
                    break;
                }
                if (!array[i].mDrawable.isStateful()) {
                    i++;
                } else {
                    isStateful = true;
                    break;
                }
            }
            this.mIsStateful = isStateful;
            this.mHaveIsStateful = true;
            return isStateful;
        }

        public final boolean canConstantState() {
            ChildDrawable[] array = this.mChildren;
            int N = this.mNum;
            for (int i = 0; i < N; i++) {
                if (array[i].mDrawable.getConstantState() == null) {
                    return false;
                }
            }
            return true;
        }

        public void invalidateCache() {
            this.mHaveOpacity = false;
            this.mHaveIsStateful = false;
        }

        @Override
        public int addAtlasableBitmaps(Collection<Bitmap> atlasList) {
            ChildDrawable[] array = this.mChildren;
            int N = this.mNum;
            int pixelCount = 0;
            for (int i = 0; i < N; i++) {
                Drawable.ConstantState state = array[i].mDrawable.getConstantState();
                if (state != null) {
                    pixelCount += state.addAtlasableBitmaps(atlasList);
                }
            }
            return pixelCount;
        }
    }
}

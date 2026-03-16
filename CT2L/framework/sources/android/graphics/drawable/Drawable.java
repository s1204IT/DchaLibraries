package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.NinePatch;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Xfermode;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.StateSet;
import android.util.TypedValue;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class Drawable {
    private int mLayoutDirection;
    private static final Rect ZERO_BOUNDS_RECT = new Rect();
    static final PorterDuff.Mode DEFAULT_TINT_MODE = PorterDuff.Mode.SRC_IN;
    private int[] mStateSet = StateSet.WILD_CARD;
    private int mLevel = 0;
    private int mChangingConfigurations = 0;
    private Rect mBounds = ZERO_BOUNDS_RECT;
    private WeakReference<Callback> mCallback = null;
    private boolean mVisible = true;

    public interface Callback {
        void invalidateDrawable(Drawable drawable);

        void scheduleDrawable(Drawable drawable, Runnable runnable, long j);

        void unscheduleDrawable(Drawable drawable, Runnable runnable);
    }

    public abstract void draw(Canvas canvas);

    public abstract int getOpacity();

    public abstract void setAlpha(int i);

    public abstract void setColorFilter(ColorFilter colorFilter);

    public void setBounds(int left, int top, int right, int bottom) {
        Rect oldBounds = this.mBounds;
        if (oldBounds == ZERO_BOUNDS_RECT) {
            oldBounds = new Rect();
            this.mBounds = oldBounds;
        }
        if (oldBounds.left != left || oldBounds.top != top || oldBounds.right != right || oldBounds.bottom != bottom) {
            if (!oldBounds.isEmpty()) {
                invalidateSelf();
            }
            this.mBounds.set(left, top, right, bottom);
            onBoundsChange(this.mBounds);
        }
    }

    public void setBounds(Rect bounds) {
        setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    public final void copyBounds(Rect bounds) {
        bounds.set(this.mBounds);
    }

    public final Rect copyBounds() {
        return new Rect(this.mBounds);
    }

    public final Rect getBounds() {
        if (this.mBounds == ZERO_BOUNDS_RECT) {
            this.mBounds = new Rect();
        }
        return this.mBounds;
    }

    public Rect getDirtyBounds() {
        return getBounds();
    }

    public void setChangingConfigurations(int configs) {
        this.mChangingConfigurations = configs;
    }

    public int getChangingConfigurations() {
        return this.mChangingConfigurations;
    }

    public void setDither(boolean dither) {
    }

    public void setFilterBitmap(boolean filter) {
    }

    public final void setCallback(Callback cb) {
        this.mCallback = new WeakReference<>(cb);
    }

    public Callback getCallback() {
        if (this.mCallback != null) {
            return this.mCallback.get();
        }
        return null;
    }

    public void invalidateSelf() {
        Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    public void scheduleSelf(Runnable what, long when) {
        Callback callback = getCallback();
        if (callback != null) {
            callback.scheduleDrawable(this, what, when);
        }
    }

    public void unscheduleSelf(Runnable what) {
        Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleDrawable(this, what);
        }
    }

    public int getLayoutDirection() {
        return this.mLayoutDirection;
    }

    public void setLayoutDirection(int layoutDirection) {
        if (getLayoutDirection() != layoutDirection) {
            this.mLayoutDirection = layoutDirection;
        }
    }

    public int getAlpha() {
        return 255;
    }

    public void setXfermode(Xfermode mode) {
    }

    public void setColorFilter(int color, PorterDuff.Mode mode) {
        setColorFilter(new PorterDuffColorFilter(color, mode));
    }

    public void setTint(int tint) {
        setTintList(ColorStateList.valueOf(tint));
    }

    public void setTintList(ColorStateList tint) {
    }

    public void setTintMode(PorterDuff.Mode tintMode) {
    }

    public ColorFilter getColorFilter() {
        return null;
    }

    public void clearColorFilter() {
        setColorFilter(null);
    }

    public void setHotspot(float x, float y) {
    }

    public void setHotspotBounds(int left, int top, int right, int bottom) {
    }

    public void getHotspotBounds(Rect outRect) {
        outRect.set(getBounds());
    }

    public boolean isProjected() {
        return false;
    }

    public boolean isStateful() {
        return false;
    }

    public boolean setState(int[] stateSet) {
        if (Arrays.equals(this.mStateSet, stateSet)) {
            return false;
        }
        this.mStateSet = stateSet;
        return onStateChange(stateSet);
    }

    public int[] getState() {
        return this.mStateSet;
    }

    public void jumpToCurrentState() {
    }

    public Drawable getCurrent() {
        return this;
    }

    public final boolean setLevel(int level) {
        if (this.mLevel == level) {
            return false;
        }
        this.mLevel = level;
        return onLevelChange(level);
    }

    public final int getLevel() {
        return this.mLevel;
    }

    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = this.mVisible != visible;
        if (changed) {
            this.mVisible = visible;
            invalidateSelf();
        }
        return changed;
    }

    public final boolean isVisible() {
        return this.mVisible;
    }

    public void setAutoMirrored(boolean mirrored) {
    }

    public boolean isAutoMirrored() {
        return false;
    }

    public void applyTheme(Resources.Theme t) {
    }

    public boolean canApplyTheme() {
        return false;
    }

    public static int resolveOpacity(int op1, int op2) {
        if (op1 != op2) {
            if (op1 == 0 || op2 == 0) {
                return 0;
            }
            if (op1 == -3 || op2 == -3) {
                return -3;
            }
            return (op1 == -2 || op2 == -2) ? -2 : -1;
        }
        return op1;
    }

    public Region getTransparentRegion() {
        return null;
    }

    protected boolean onStateChange(int[] state) {
        return false;
    }

    protected boolean onLevelChange(int level) {
        return false;
    }

    protected void onBoundsChange(Rect bounds) {
    }

    public int getIntrinsicWidth() {
        return -1;
    }

    public int getIntrinsicHeight() {
        return -1;
    }

    public int getMinimumWidth() {
        int intrinsicWidth = getIntrinsicWidth();
        if (intrinsicWidth > 0) {
            return intrinsicWidth;
        }
        return 0;
    }

    public int getMinimumHeight() {
        int intrinsicHeight = getIntrinsicHeight();
        if (intrinsicHeight > 0) {
            return intrinsicHeight;
        }
        return 0;
    }

    public boolean getPadding(Rect padding) {
        padding.set(0, 0, 0, 0);
        return false;
    }

    public Insets getOpticalInsets() {
        return Insets.NONE;
    }

    public void getOutline(Outline outline) {
        outline.setRect(getBounds());
        outline.setAlpha(0.0f);
    }

    public Drawable mutate() {
        return this;
    }

    public void clearMutated() {
    }

    public static Drawable createFromStream(InputStream is, String srcName) {
        Trace.traceBegin(Trace.TRACE_TAG_RESOURCES, srcName != null ? srcName : "Unknown drawable");
        try {
            return createFromResourceStream(null, null, is, srcName);
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_RESOURCES);
        }
    }

    public static Drawable createFromResourceStream(Resources res, TypedValue value, InputStream is, String srcName) {
        Trace.traceBegin(Trace.TRACE_TAG_RESOURCES, srcName != null ? srcName : "Unknown drawable");
        try {
            return createFromResourceStream(res, value, is, srcName, null);
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_RESOURCES);
        }
    }

    public static Drawable createFromResourceStream(Resources res, TypedValue value, InputStream is, String srcName, BitmapFactory.Options opts) {
        if (is == null) {
            return null;
        }
        Rect pad = new Rect();
        if (opts == null) {
            opts = new BitmapFactory.Options();
        }
        opts.inScreenDensity = res != null ? res.getDisplayMetrics().noncompatDensityDpi : DisplayMetrics.DENSITY_DEVICE;
        Bitmap bm = BitmapFactory.decodeResourceStream(res, value, is, pad, opts);
        if (bm == null) {
            return null;
        }
        byte[] np = bm.getNinePatchChunk();
        if (np == null || !NinePatch.isNinePatchChunk(np)) {
            np = null;
            pad = null;
        }
        Rect opticalInsets = new Rect();
        bm.getOpticalInsets(opticalInsets);
        return drawableFromBitmap(res, bm, np, pad, opticalInsets, srcName);
    }

    public static Drawable createFromXml(Resources r, XmlPullParser parser) throws XmlPullParserException, IOException {
        return createFromXml(r, parser, null);
    }

    public static Drawable createFromXml(Resources r, XmlPullParser parser, Resources.Theme theme) throws XmlPullParserException, IOException {
        int type;
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
        Drawable drawable = createFromXmlInner(r, parser, attrs, theme);
        if (drawable == null) {
            throw new RuntimeException("Unknown initial tag: " + parser.getName());
        }
        return drawable;
    }

    public static Drawable createFromXmlInner(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
        return createFromXmlInner(r, parser, attrs, null);
    }

    public static Drawable createFromXmlInner(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        String name;
        Drawable drawable;
        name = parser.getName();
        switch (name) {
            case "selector":
                drawable = new StateListDrawable();
                break;
            case "animated-selector":
                drawable = new AnimatedStateListDrawable();
                break;
            case "level-list":
                drawable = new LevelListDrawable();
                break;
            case "layer-list":
                drawable = new LayerDrawable();
                break;
            case "transition":
                drawable = new TransitionDrawable();
                break;
            case "ripple":
                drawable = new RippleDrawable();
                break;
            case "color":
                drawable = new ColorDrawable();
                break;
            case "shape":
                drawable = new GradientDrawable();
                break;
            case "vector":
                drawable = new VectorDrawable();
                break;
            case "animated-vector":
                drawable = new AnimatedVectorDrawable();
                break;
            case "scale":
                drawable = new ScaleDrawable();
                break;
            case "clip":
                drawable = new ClipDrawable();
                break;
            case "rotate":
                drawable = new RotateDrawable();
                break;
            case "animated-rotate":
                drawable = new AnimatedRotateDrawable();
                break;
            case "animation-list":
                drawable = new AnimationDrawable();
                break;
            case "inset":
                drawable = new InsetDrawable();
                break;
            case "bitmap":
                drawable = new BitmapDrawable(r);
                if (r != null) {
                    ((BitmapDrawable) drawable).setTargetDensity(r.getDisplayMetrics());
                    break;
                }
                break;
            case "nine-patch":
                drawable = new NinePatchDrawable();
                if (r != null) {
                    ((NinePatchDrawable) drawable).setTargetDensity(r.getDisplayMetrics());
                    break;
                }
                break;
            default:
                throw new XmlPullParserException(parser.getPositionDescription() + ": invalid drawable tag " + name);
        }
        drawable.inflate(r, parser, attrs, theme);
        return drawable;
    }

    public static Drawable createFromPath(String pathName) {
        Drawable drawableDrawableFromBitmap = null;
        if (pathName != null) {
            Trace.traceBegin(Trace.TRACE_TAG_RESOURCES, pathName);
            try {
                Bitmap bm = BitmapFactory.decodeFile(pathName);
                if (bm != null) {
                    drawableDrawableFromBitmap = drawableFromBitmap(null, bm, null, null, null, pathName);
                }
            } finally {
                Trace.traceEnd(Trace.TRACE_TAG_RESOURCES);
            }
        }
        return drawableDrawableFromBitmap;
    }

    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
        inflate(r, parser, attrs, null);
    }

    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs, R.styleable.Drawable, 0, 0);
        } else {
            a = r.obtainAttributes(attrs, R.styleable.Drawable);
        }
        inflateWithAttributes(r, parser, a, 0);
        a.recycle();
    }

    void inflateWithAttributes(Resources r, XmlPullParser parser, TypedArray attrs, int visibleAttr) throws XmlPullParserException, IOException {
        this.mVisible = attrs.getBoolean(visibleAttr, this.mVisible);
    }

    public static abstract class ConstantState {
        public abstract int getChangingConfigurations();

        public abstract Drawable newDrawable();

        public Drawable newDrawable(Resources res) {
            return newDrawable();
        }

        public Drawable newDrawable(Resources res, Resources.Theme theme) {
            return newDrawable(null);
        }

        public int addAtlasableBitmaps(Collection<Bitmap> atlasList) {
            return 0;
        }

        protected final boolean isAtlasable(Bitmap bitmap) {
            return bitmap != null && bitmap.getConfig() == Bitmap.Config.ARGB_8888;
        }

        public boolean canApplyTheme() {
            return false;
        }
    }

    public ConstantState getConstantState() {
        return null;
    }

    private static Drawable drawableFromBitmap(Resources res, Bitmap bm, byte[] np, Rect pad, Rect layoutBounds, String srcName) {
        return np != null ? new NinePatchDrawable(res, bm, np, pad, layoutBounds, srcName) : new BitmapDrawable(res, bm);
    }

    PorterDuffColorFilter updateTintFilter(PorterDuffColorFilter tintFilter, ColorStateList tint, PorterDuff.Mode tintMode) {
        if (tint == null || tintMode == null) {
            return null;
        }
        int color = tint.getColorForState(getState(), 0);
        if (tintFilter == null) {
            return new PorterDuffColorFilter(color, tintMode);
        }
        tintFilter.setColor(color);
        tintFilter.setMode(tintMode);
        return tintFilter;
    }

    static TypedArray obtainAttributes(Resources res, Resources.Theme theme, AttributeSet set, int[] attrs) {
        return theme == null ? res.obtainAttributes(set, attrs) : theme.obtainStyledAttributes(set, attrs, 0, 0);
    }

    public static PorterDuff.Mode parseTintMode(int value, PorterDuff.Mode defaultMode) {
        switch (value) {
            case 3:
                PorterDuff.Mode defaultMode2 = PorterDuff.Mode.SRC_OVER;
                return defaultMode2;
            case 4:
            case 6:
            case 7:
            case 8:
            case 10:
            case 11:
            case 12:
            case 13:
            default:
                return defaultMode;
            case 5:
                PorterDuff.Mode defaultMode3 = PorterDuff.Mode.SRC_IN;
                return defaultMode3;
            case 9:
                PorterDuff.Mode defaultMode4 = PorterDuff.Mode.SRC_ATOP;
                return defaultMode4;
            case 14:
                PorterDuff.Mode defaultMode5 = PorterDuff.Mode.MULTIPLY;
                return defaultMode5;
            case 15:
                PorterDuff.Mode defaultMode6 = PorterDuff.Mode.SCREEN;
                return defaultMode6;
            case 16:
                PorterDuff.Mode defaultMode7 = PorterDuff.Mode.ADD;
                return defaultMode7;
        }
    }
}

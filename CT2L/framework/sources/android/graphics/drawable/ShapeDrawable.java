package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ShapeDrawable extends Drawable {
    private boolean mMutated;
    private ShapeState mShapeState;
    private PorterDuffColorFilter mTintFilter;

    public static abstract class ShaderFactory {
        public abstract Shader resize(int i, int i2);
    }

    public ShapeDrawable() {
        this(new ShapeState(null), null);
    }

    public ShapeDrawable(Shape s) {
        this(new ShapeState(null), null);
        this.mShapeState.mShape = s;
    }

    public Shape getShape() {
        return this.mShapeState.mShape;
    }

    public void setShape(Shape s) {
        this.mShapeState.mShape = s;
        updateShape();
    }

    public void setShaderFactory(ShaderFactory fact) {
        this.mShapeState.mShaderFactory = fact;
    }

    public ShaderFactory getShaderFactory() {
        return this.mShapeState.mShaderFactory;
    }

    public Paint getPaint() {
        return this.mShapeState.mPaint;
    }

    public void setPadding(int left, int top, int right, int bottom) {
        if ((left | top | right | bottom) == 0) {
            this.mShapeState.mPadding = null;
        } else {
            if (this.mShapeState.mPadding == null) {
                this.mShapeState.mPadding = new Rect();
            }
            this.mShapeState.mPadding.set(left, top, right, bottom);
        }
        invalidateSelf();
    }

    public void setPadding(Rect padding) {
        if (padding == null) {
            this.mShapeState.mPadding = null;
        } else {
            if (this.mShapeState.mPadding == null) {
                this.mShapeState.mPadding = new Rect();
            }
            this.mShapeState.mPadding.set(padding);
        }
        invalidateSelf();
    }

    public void setIntrinsicWidth(int width) {
        this.mShapeState.mIntrinsicWidth = width;
        invalidateSelf();
    }

    public void setIntrinsicHeight(int height) {
        this.mShapeState.mIntrinsicHeight = height;
        invalidateSelf();
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mShapeState.mIntrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mShapeState.mIntrinsicHeight;
    }

    @Override
    public boolean getPadding(Rect padding) {
        if (this.mShapeState.mPadding == null) {
            return super.getPadding(padding);
        }
        padding.set(this.mShapeState.mPadding);
        return true;
    }

    private static int modulateAlpha(int paintAlpha, int alpha) {
        int scale = alpha + (alpha >>> 7);
        return (paintAlpha * scale) >>> 8;
    }

    protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
        shape.draw(canvas, paint);
    }

    @Override
    public void draw(Canvas canvas) {
        boolean clearColorFilter;
        Rect r = getBounds();
        ShapeState state = this.mShapeState;
        Paint paint = state.mPaint;
        int prevAlpha = paint.getAlpha();
        paint.setAlpha(modulateAlpha(prevAlpha, state.mAlpha));
        if (paint.getAlpha() != 0 || paint.getXfermode() != null || paint.hasShadowLayer()) {
            if (this.mTintFilter != null && paint.getColorFilter() == null) {
                paint.setColorFilter(this.mTintFilter);
                clearColorFilter = true;
            } else {
                clearColorFilter = false;
            }
            if (state.mShape != null) {
                int count = canvas.save();
                canvas.translate(r.left, r.top);
                onDraw(state.mShape, canvas, paint);
                canvas.restoreToCount(count);
            } else {
                canvas.drawRect(r, paint);
            }
            if (clearColorFilter) {
                paint.setColorFilter(null);
            }
        }
        paint.setAlpha(prevAlpha);
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mShapeState.mChangingConfigurations;
    }

    @Override
    public void setAlpha(int alpha) {
        this.mShapeState.mAlpha = alpha;
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return this.mShapeState.mAlpha;
    }

    @Override
    public void setTintList(ColorStateList tint) {
        this.mShapeState.mTint = tint;
        this.mTintFilter = updateTintFilter(this.mTintFilter, tint, this.mShapeState.mTintMode);
        invalidateSelf();
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        this.mShapeState.mTintMode = tintMode;
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mShapeState.mTint, tintMode);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.mShapeState.mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        if (this.mShapeState.mShape == null) {
            Paint p = this.mShapeState.mPaint;
            if (p.getXfermode() == null) {
                int alpha = p.getAlpha();
                if (alpha == 0) {
                    return -2;
                }
                if (alpha == 255) {
                    return -1;
                }
            }
        }
        return -3;
    }

    @Override
    public void setDither(boolean dither) {
        this.mShapeState.mPaint.setDither(dither);
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        updateShape();
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        ShapeState state = this.mShapeState;
        if (state.mTint == null || state.mTintMode == null) {
            return false;
        }
        this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
        return true;
    }

    @Override
    public boolean isStateful() {
        ShapeState s = this.mShapeState;
        return super.isStateful() || (s.mTint != null && s.mTint.isStateful());
    }

    protected boolean inflateTag(String name, Resources r, XmlPullParser parser, AttributeSet attrs) {
        if (!"padding".equals(name)) {
            return false;
        }
        TypedArray a = r.obtainAttributes(attrs, R.styleable.ShapeDrawablePadding);
        setPadding(a.getDimensionPixelOffset(0, 0), a.getDimensionPixelOffset(1, 0), a.getDimensionPixelOffset(2, 0), a.getDimensionPixelOffset(3, 0));
        a.recycle();
        return true;
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.ShapeDrawable);
        updateStateFromTypedArray(a);
        a.recycle();
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type == 2) {
                String name = parser.getName();
                if (!inflateTag(name, r, parser, attrs)) {
                    Log.w("drawable", "Unknown element: " + name + " for ShapeDrawable " + this);
                }
            }
        }
        initializeWithState(this.mShapeState, r);
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        ShapeState state = this.mShapeState;
        if (state != null && state.mThemeAttrs != null) {
            TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.ShapeDrawable);
            updateStateFromTypedArray(a);
            a.recycle();
            initializeWithState(state, t.getResources());
        }
    }

    private void updateStateFromTypedArray(TypedArray a) {
        ShapeState state = this.mShapeState;
        Paint paint = state.mPaint;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        int color = paint.getColor();
        paint.setColor(a.getColor(4, color));
        boolean dither = paint.isDither();
        paint.setDither(a.getBoolean(0, dither));
        setIntrinsicWidth((int) a.getDimension(3, state.mIntrinsicWidth));
        setIntrinsicHeight((int) a.getDimension(2, state.mIntrinsicHeight));
        int tintMode = a.getInt(5, -1);
        if (tintMode != -1) {
            state.mTintMode = Drawable.parseTintMode(tintMode, PorterDuff.Mode.SRC_IN);
        }
        ColorStateList tint = a.getColorStateList(1);
        if (tint != null) {
            state.mTint = tint;
        }
    }

    private void updateShape() {
        if (this.mShapeState.mShape != null) {
            Rect r = getBounds();
            int w = r.width();
            int h = r.height();
            this.mShapeState.mShape.resize(w, h);
            if (this.mShapeState.mShaderFactory != null) {
                this.mShapeState.mPaint.setShader(this.mShapeState.mShaderFactory.resize(w, h));
            }
        }
        invalidateSelf();
    }

    @Override
    public void getOutline(Outline outline) {
        if (this.mShapeState.mShape != null) {
            this.mShapeState.mShape.getOutline(outline);
            outline.setAlpha(getAlpha() / 255.0f);
        }
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        this.mShapeState.mChangingConfigurations = getChangingConfigurations();
        return this.mShapeState;
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            if (this.mShapeState.mPaint != null) {
                this.mShapeState.mPaint = new Paint(this.mShapeState.mPaint);
            } else {
                this.mShapeState.mPaint = new Paint(1);
            }
            if (this.mShapeState.mPadding != null) {
                this.mShapeState.mPadding = new Rect(this.mShapeState.mPadding);
            } else {
                this.mShapeState.mPadding = new Rect();
            }
            try {
                this.mShapeState.mShape = this.mShapeState.mShape.mo15clone();
                this.mMutated = true;
                return this;
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        this.mMutated = false;
    }

    static final class ShapeState extends Drawable.ConstantState {
        int mAlpha;
        int mChangingConfigurations;
        int mIntrinsicHeight;
        int mIntrinsicWidth;
        Rect mPadding;
        Paint mPaint;
        ShaderFactory mShaderFactory;
        Shape mShape;
        int[] mThemeAttrs;
        ColorStateList mTint;
        PorterDuff.Mode mTintMode;

        ShapeState(ShapeState orig) {
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mAlpha = 255;
            if (orig != null) {
                this.mThemeAttrs = orig.mThemeAttrs;
                this.mPaint = orig.mPaint;
                this.mShape = orig.mShape;
                this.mTint = orig.mTint;
                this.mTintMode = orig.mTintMode;
                this.mPadding = orig.mPadding;
                this.mIntrinsicWidth = orig.mIntrinsicWidth;
                this.mIntrinsicHeight = orig.mIntrinsicHeight;
                this.mAlpha = orig.mAlpha;
                this.mShaderFactory = orig.mShaderFactory;
                return;
            }
            this.mPaint = new Paint(1);
        }

        @Override
        public boolean canApplyTheme() {
            return this.mThemeAttrs != null;
        }

        @Override
        public Drawable newDrawable() {
            return new ShapeDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new ShapeDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }
    }

    private ShapeDrawable(ShapeState state, Resources res) {
        this.mShapeState = state;
        initializeWithState(state, res);
    }

    private void initializeWithState(ShapeState state, Resources res) {
        this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
    }
}

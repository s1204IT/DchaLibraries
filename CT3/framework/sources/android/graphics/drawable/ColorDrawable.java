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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewDebug;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ColorDrawable extends Drawable {

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "state_")
    private ColorState mColorState;
    private boolean mMutated;
    private final Paint mPaint;
    private PorterDuffColorFilter mTintFilter;

    ColorDrawable(ColorState state, Resources res, ColorDrawable colorDrawable) {
        this(state, res);
    }

    public ColorDrawable() {
        this.mPaint = new Paint(1);
        this.mColorState = new ColorState();
    }

    public ColorDrawable(int color) {
        this.mPaint = new Paint(1);
        this.mColorState = new ColorState();
        setColor(color);
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mColorState.getChangingConfigurations();
    }

    @Override
    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mColorState = new ColorState(this.mColorState);
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        this.mMutated = false;
    }

    @Override
    public void draw(Canvas canvas) {
        ColorFilter colorFilter = this.mPaint.getColorFilter();
        if ((this.mColorState.mUseColor >>> 24) == 0 && colorFilter == null && this.mTintFilter == null) {
            return;
        }
        if (colorFilter == null) {
            this.mPaint.setColorFilter(this.mTintFilter);
        }
        this.mPaint.setColor(this.mColorState.mUseColor);
        canvas.drawRect(getBounds(), this.mPaint);
        this.mPaint.setColorFilter(colorFilter);
    }

    public int getColor() {
        return this.mColorState.mUseColor;
    }

    public void setColor(int color) {
        if (this.mColorState.mBaseColor == color && this.mColorState.mUseColor == color) {
            return;
        }
        ColorState colorState = this.mColorState;
        this.mColorState.mUseColor = color;
        colorState.mBaseColor = color;
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return this.mColorState.mUseColor >>> 24;
    }

    @Override
    public void setAlpha(int alpha) {
        int baseAlpha = this.mColorState.mBaseColor >>> 24;
        int useAlpha = (baseAlpha * (alpha + (alpha >> 7))) >> 8;
        int useColor = ((this.mColorState.mBaseColor << 8) >>> 8) | (useAlpha << 24);
        if (this.mColorState.mUseColor == useColor) {
            return;
        }
        this.mColorState.mUseColor = useColor;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        this.mColorState.mTint = tint;
        this.mTintFilter = updateTintFilter(this.mTintFilter, tint, this.mColorState.mTintMode);
        invalidateSelf();
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        this.mColorState.mTintMode = tintMode;
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mColorState.mTint, tintMode);
        invalidateSelf();
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        ColorState state = this.mColorState;
        if (state.mTint != null && state.mTintMode != null) {
            this.mTintFilter = updateTintFilter(this.mTintFilter, state.mTint, state.mTintMode);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        if (this.mColorState.mTint != null) {
            return this.mColorState.mTint.isStateful();
        }
        return false;
    }

    @Override
    public int getOpacity() {
        if (this.mTintFilter != null || this.mPaint.getColorFilter() != null) {
            return -3;
        }
        switch (this.mColorState.mUseColor >>> 24) {
            case 0:
                break;
            case 255:
                break;
        }
        return -3;
    }

    @Override
    public void getOutline(Outline outline) {
        outline.setRect(getBounds());
        outline.setAlpha(getAlpha() / 255.0f);
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.ColorDrawable);
        updateStateFromTypedArray(a);
        a.recycle();
        updateLocalState(r);
    }

    private void updateStateFromTypedArray(TypedArray a) {
        ColorState state = this.mColorState;
        state.mChangingConfigurations |= a.getChangingConfigurations();
        state.mThemeAttrs = a.extractThemeAttrs();
        state.mBaseColor = a.getColor(0, state.mBaseColor);
        state.mUseColor = state.mBaseColor;
    }

    @Override
    public boolean canApplyTheme() {
        if (this.mColorState.canApplyTheme()) {
            return true;
        }
        return super.canApplyTheme();
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        super.applyTheme(t);
        ColorState state = this.mColorState;
        if (state == null) {
            return;
        }
        if (state.mThemeAttrs != null) {
            TypedArray a = t.resolveAttributes(state.mThemeAttrs, R.styleable.ColorDrawable);
            updateStateFromTypedArray(a);
            a.recycle();
        }
        if (state.mTint != null && state.mTint.canApplyTheme()) {
            state.mTint = state.mTint.obtainForTheme(t);
        }
        updateLocalState(t.getResources());
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        return this.mColorState;
    }

    static final class ColorState extends Drawable.ConstantState {
        int mBaseColor;
        int mChangingConfigurations;
        int[] mThemeAttrs;
        ColorStateList mTint;
        PorterDuff.Mode mTintMode;

        @ViewDebug.ExportedProperty
        int mUseColor;

        ColorState() {
            this.mTint = null;
            this.mTintMode = ColorDrawable.DEFAULT_TINT_MODE;
        }

        ColorState(ColorState state) {
            this.mTint = null;
            this.mTintMode = ColorDrawable.DEFAULT_TINT_MODE;
            this.mThemeAttrs = state.mThemeAttrs;
            this.mBaseColor = state.mBaseColor;
            this.mUseColor = state.mUseColor;
            this.mChangingConfigurations = state.mChangingConfigurations;
            this.mTint = state.mTint;
            this.mTintMode = state.mTintMode;
        }

        @Override
        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null) {
                return true;
            }
            if (this.mTint != null) {
                return this.mTint.canApplyTheme();
            }
            return false;
        }

        @Override
        public Drawable newDrawable() {
            return new ColorDrawable(this, null, 0 == true ? 1 : 0);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new ColorDrawable(this, res, null);
        }

        @Override
        public int getChangingConfigurations() {
            return (this.mTint != null ? this.mTint.getChangingConfigurations() : 0) | this.mChangingConfigurations;
        }
    }

    private ColorDrawable(ColorState state, Resources res) {
        this.mPaint = new Paint(1);
        this.mColorState = state;
        updateLocalState(res);
    }

    private void updateLocalState(Resources r) {
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mColorState.mTint, this.mColorState.mTintMode);
    }
}

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
import android.graphics.Xfermode;
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

    public ColorDrawable() {
        this.mPaint = new Paint(1);
        this.mColorState = new ColorState();
    }

    public ColorDrawable(int i) {
        this.mPaint = new Paint(1);
        this.mColorState = new ColorState();
        setColor(i);
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
        if ((this.mColorState.mUseColor >>> 24) != 0 || colorFilter != null || this.mTintFilter != null) {
            if (colorFilter == null) {
                this.mPaint.setColorFilter(this.mTintFilter);
            }
            this.mPaint.setColor(this.mColorState.mUseColor);
            canvas.drawRect(getBounds(), this.mPaint);
            this.mPaint.setColorFilter(colorFilter);
        }
    }

    public int getColor() {
        return this.mColorState.mUseColor;
    }

    public void setColor(int i) {
        if (this.mColorState.mBaseColor != i || this.mColorState.mUseColor != i) {
            ColorState colorState = this.mColorState;
            this.mColorState.mUseColor = i;
            colorState.mBaseColor = i;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return this.mColorState.mUseColor >>> 24;
    }

    @Override
    public void setAlpha(int i) {
        int i2 = ((((this.mColorState.mBaseColor >>> 24) * (i + (i >> 7))) >> 8) << 24) | ((this.mColorState.mBaseColor << 8) >>> 8);
        if (this.mColorState.mUseColor != i2) {
            this.mColorState.mUseColor = i2;
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
    }

    @Override
    public void setTintList(ColorStateList colorStateList) {
        this.mColorState.mTint = colorStateList;
        this.mTintFilter = updateTintFilter(this.mTintFilter, colorStateList, this.mColorState.mTintMode);
        invalidateSelf();
    }

    @Override
    public void setTintMode(PorterDuff.Mode mode) {
        this.mColorState.mTintMode = mode;
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mColorState.mTint, mode);
        invalidateSelf();
    }

    @Override
    protected boolean onStateChange(int[] iArr) {
        ColorState colorState = this.mColorState;
        if (colorState.mTint != null && colorState.mTintMode != null) {
            this.mTintFilter = updateTintFilter(this.mTintFilter, colorState.mTint, colorState.mTintMode);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        return this.mColorState.mTint != null && this.mColorState.mTint.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return this.mColorState.mTint != null && this.mColorState.mTint.hasFocusStateSpecified();
    }

    @Override
    public void setXfermode(Xfermode xfermode) {
        this.mPaint.setXfermode(xfermode);
        invalidateSelf();
    }

    public Xfermode getXfermode() {
        return this.mPaint.getXfermode();
    }

    @Override
    public int getOpacity() {
        if (this.mTintFilter != null || this.mPaint.getColorFilter() != null) {
            return -3;
        }
        int i = this.mColorState.mUseColor >>> 24;
        if (i != 0) {
            return i != 255 ? -3 : -1;
        }
        return -2;
    }

    @Override
    public void getOutline(Outline outline) {
        outline.setRect(getBounds());
        outline.setAlpha(getAlpha() / 255.0f);
    }

    @Override
    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(resources, xmlPullParser, attributeSet, theme);
        TypedArray typedArrayObtainAttributes = obtainAttributes(resources, theme, attributeSet, R.styleable.ColorDrawable);
        updateStateFromTypedArray(typedArrayObtainAttributes);
        typedArrayObtainAttributes.recycle();
        updateLocalState(resources);
    }

    private void updateStateFromTypedArray(TypedArray typedArray) {
        ColorState colorState = this.mColorState;
        colorState.mChangingConfigurations |= typedArray.getChangingConfigurations();
        colorState.mThemeAttrs = typedArray.extractThemeAttrs();
        colorState.mBaseColor = typedArray.getColor(0, colorState.mBaseColor);
        colorState.mUseColor = colorState.mBaseColor;
    }

    @Override
    public boolean canApplyTheme() {
        return this.mColorState.canApplyTheme() || super.canApplyTheme();
    }

    @Override
    public void applyTheme(Resources.Theme theme) {
        super.applyTheme(theme);
        ColorState colorState = this.mColorState;
        if (colorState == null) {
            return;
        }
        if (colorState.mThemeAttrs != null) {
            TypedArray typedArrayResolveAttributes = theme.resolveAttributes(colorState.mThemeAttrs, R.styleable.ColorDrawable);
            updateStateFromTypedArray(typedArrayResolveAttributes);
            typedArrayResolveAttributes.recycle();
        }
        if (colorState.mTint != null && colorState.mTint.canApplyTheme()) {
            colorState.mTint = colorState.mTint.obtainForTheme(theme);
        }
        updateLocalState(theme.getResources());
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
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
        }

        ColorState(ColorState colorState) {
            this.mTint = null;
            this.mTintMode = Drawable.DEFAULT_TINT_MODE;
            this.mThemeAttrs = colorState.mThemeAttrs;
            this.mBaseColor = colorState.mBaseColor;
            this.mUseColor = colorState.mUseColor;
            this.mChangingConfigurations = colorState.mChangingConfigurations;
            this.mTint = colorState.mTint;
            this.mTintMode = colorState.mTintMode;
        }

        @Override
        public boolean canApplyTheme() {
            return this.mThemeAttrs != null || (this.mTint != null && this.mTint.canApplyTheme());
        }

        @Override
        public Drawable newDrawable() {
            return new ColorDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources resources) {
            return new ColorDrawable(this, resources);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations | (this.mTint != null ? this.mTint.getChangingConfigurations() : 0);
        }
    }

    private ColorDrawable(ColorState colorState, Resources resources) {
        this.mPaint = new Paint(1);
        this.mColorState = colorState;
        updateLocalState(resources);
    }

    private void updateLocalState(Resources resources) {
        this.mTintFilter = updateTintFilter(this.mTintFilter, this.mColorState.mTint, this.mColorState.mTintMode);
    }
}

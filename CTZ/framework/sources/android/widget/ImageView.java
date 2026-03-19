package android.widget;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.TtmlUtils;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewHierarchyEncoder;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.io.IOException;

@RemoteViews.RemoteView
public class ImageView extends View {
    private static final String LOG_TAG = "ImageView";
    private static boolean sCompatAdjustViewBounds;
    private static boolean sCompatDone;
    private static boolean sCompatDrawableVisibilityDispatch;
    private static boolean sCompatUseCorrectStreamDensity;
    private boolean mAdjustViewBounds;
    private int mAlpha;
    private int mBaseline;
    private boolean mBaselineAlignBottom;
    private ColorFilter mColorFilter;
    private boolean mColorMod;
    private boolean mCropToPadding;
    private Matrix mDrawMatrix;
    private Drawable mDrawable;
    private int mDrawableHeight;
    private ColorStateList mDrawableTintList;
    private PorterDuff.Mode mDrawableTintMode;
    private int mDrawableWidth;
    private boolean mHasColorFilter;
    private boolean mHasDrawableTint;
    private boolean mHasDrawableTintMode;
    private boolean mHaveFrame;
    private int mLevel;
    private Matrix mMatrix;
    private int mMaxHeight;
    private int mMaxWidth;
    private boolean mMergeState;
    private BitmapDrawable mRecycleableBitmapDrawable;
    private int mResource;
    private ScaleType mScaleType;
    private int[] mState;
    private final RectF mTempDst;
    private final RectF mTempSrc;
    private Uri mUri;
    private final int mViewAlphaScale;
    private Xfermode mXfermode;
    private static final ScaleType[] sScaleTypeArray = {ScaleType.MATRIX, ScaleType.FIT_XY, ScaleType.FIT_START, ScaleType.FIT_CENTER, ScaleType.FIT_END, ScaleType.CENTER, ScaleType.CENTER_CROP, ScaleType.CENTER_INSIDE};
    private static final Matrix.ScaleToFit[] sS2FArray = {Matrix.ScaleToFit.FILL, Matrix.ScaleToFit.START, Matrix.ScaleToFit.CENTER, Matrix.ScaleToFit.END};

    public ImageView(Context context) {
        super(context);
        this.mResource = 0;
        this.mHaveFrame = false;
        this.mAdjustViewBounds = false;
        this.mMaxWidth = Integer.MAX_VALUE;
        this.mMaxHeight = Integer.MAX_VALUE;
        this.mColorFilter = null;
        this.mHasColorFilter = false;
        this.mAlpha = 255;
        this.mViewAlphaScale = 256;
        this.mColorMod = false;
        this.mDrawable = null;
        this.mRecycleableBitmapDrawable = null;
        this.mDrawableTintList = null;
        this.mDrawableTintMode = null;
        this.mHasDrawableTint = false;
        this.mHasDrawableTintMode = false;
        this.mState = null;
        this.mMergeState = false;
        this.mLevel = 0;
        this.mDrawMatrix = null;
        this.mTempSrc = new RectF();
        this.mTempDst = new RectF();
        this.mBaseline = -1;
        this.mBaselineAlignBottom = false;
        initImageView();
    }

    public ImageView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ImageView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public ImageView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mResource = 0;
        this.mHaveFrame = false;
        this.mAdjustViewBounds = false;
        this.mMaxWidth = Integer.MAX_VALUE;
        this.mMaxHeight = Integer.MAX_VALUE;
        this.mColorFilter = null;
        this.mHasColorFilter = false;
        this.mAlpha = 255;
        this.mViewAlphaScale = 256;
        this.mColorMod = false;
        this.mDrawable = null;
        this.mRecycleableBitmapDrawable = null;
        this.mDrawableTintList = null;
        this.mDrawableTintMode = null;
        this.mHasDrawableTint = false;
        this.mHasDrawableTintMode = false;
        this.mState = null;
        this.mMergeState = false;
        this.mLevel = 0;
        this.mDrawMatrix = null;
        this.mTempSrc = new RectF();
        this.mTempDst = new RectF();
        this.mBaseline = -1;
        this.mBaselineAlignBottom = false;
        initImageView();
        if (getImportantForAutofill() == 0) {
            setImportantForAutofill(2);
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ImageView, i, i2);
        Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(0);
        if (drawable != null) {
            setImageDrawable(drawable);
        }
        this.mBaselineAlignBottom = typedArrayObtainStyledAttributes.getBoolean(6, false);
        this.mBaseline = typedArrayObtainStyledAttributes.getDimensionPixelSize(8, -1);
        setAdjustViewBounds(typedArrayObtainStyledAttributes.getBoolean(2, false));
        setMaxWidth(typedArrayObtainStyledAttributes.getDimensionPixelSize(3, Integer.MAX_VALUE));
        setMaxHeight(typedArrayObtainStyledAttributes.getDimensionPixelSize(4, Integer.MAX_VALUE));
        int i3 = typedArrayObtainStyledAttributes.getInt(1, -1);
        if (i3 >= 0) {
            setScaleType(sScaleTypeArray[i3]);
        }
        if (typedArrayObtainStyledAttributes.hasValue(5)) {
            this.mDrawableTintList = typedArrayObtainStyledAttributes.getColorStateList(5);
            this.mHasDrawableTint = true;
            this.mDrawableTintMode = PorterDuff.Mode.SRC_ATOP;
            this.mHasDrawableTintMode = true;
        }
        if (typedArrayObtainStyledAttributes.hasValue(9)) {
            this.mDrawableTintMode = Drawable.parseTintMode(typedArrayObtainStyledAttributes.getInt(9, -1), this.mDrawableTintMode);
            this.mHasDrawableTintMode = true;
        }
        applyImageTint();
        int i4 = typedArrayObtainStyledAttributes.getInt(10, 255);
        if (i4 != 255) {
            setImageAlpha(i4);
        }
        this.mCropToPadding = typedArrayObtainStyledAttributes.getBoolean(7, false);
        typedArrayObtainStyledAttributes.recycle();
    }

    private void initImageView() {
        this.mMatrix = new Matrix();
        this.mScaleType = ScaleType.FIT_CENTER;
        if (!sCompatDone) {
            int i = this.mContext.getApplicationInfo().targetSdkVersion;
            sCompatAdjustViewBounds = i <= 17;
            sCompatUseCorrectStreamDensity = i > 23;
            sCompatDrawableVisibilityDispatch = i < 24;
            sCompatDone = true;
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable drawable) {
        return this.mDrawable == drawable || super.verifyDrawable(drawable);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mDrawable != null) {
            this.mDrawable.jumpToCurrentState();
        }
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        if (drawable == this.mDrawable) {
            if (drawable != null) {
                int intrinsicWidth = drawable.getIntrinsicWidth();
                int intrinsicHeight = drawable.getIntrinsicHeight();
                if (intrinsicWidth != this.mDrawableWidth || intrinsicHeight != this.mDrawableHeight) {
                    this.mDrawableWidth = intrinsicWidth;
                    this.mDrawableHeight = intrinsicHeight;
                    configureBounds();
                }
            }
            invalidate();
            return;
        }
        super.invalidateDrawable(drawable);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return (getBackground() == null || getBackground().getCurrent() == null) ? false : true;
    }

    @Override
    public void onPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        super.onPopulateAccessibilityEventInternal(accessibilityEvent);
        CharSequence contentDescription = getContentDescription();
        if (!TextUtils.isEmpty(contentDescription)) {
            accessibilityEvent.getText().add(contentDescription);
        }
    }

    public boolean getAdjustViewBounds() {
        return this.mAdjustViewBounds;
    }

    @RemotableViewMethod
    public void setAdjustViewBounds(boolean z) {
        this.mAdjustViewBounds = z;
        if (z) {
            setScaleType(ScaleType.FIT_CENTER);
        }
    }

    public int getMaxWidth() {
        return this.mMaxWidth;
    }

    @RemotableViewMethod
    public void setMaxWidth(int i) {
        this.mMaxWidth = i;
    }

    public int getMaxHeight() {
        return this.mMaxHeight;
    }

    @RemotableViewMethod
    public void setMaxHeight(int i) {
        this.mMaxHeight = i;
    }

    public Drawable getDrawable() {
        if (this.mDrawable == this.mRecycleableBitmapDrawable) {
            this.mRecycleableBitmapDrawable = null;
        }
        return this.mDrawable;
    }

    private class ImageDrawableCallback implements Runnable {
        private final Drawable drawable;
        private final int resource;
        private final Uri uri;

        ImageDrawableCallback(Drawable drawable, Uri uri, int i) {
            this.drawable = drawable;
            this.uri = uri;
            this.resource = i;
        }

        @Override
        public void run() {
            ImageView.this.setImageDrawable(this.drawable);
            ImageView.this.mUri = this.uri;
            ImageView.this.mResource = this.resource;
        }
    }

    @RemotableViewMethod(asyncImpl = "setImageResourceAsync")
    public void setImageResource(int i) {
        int i2 = this.mDrawableWidth;
        int i3 = this.mDrawableHeight;
        updateDrawable(null);
        this.mResource = i;
        this.mUri = null;
        resolveUri();
        if (i2 != this.mDrawableWidth || i3 != this.mDrawableHeight) {
            requestLayout();
        }
        invalidate();
    }

    public Runnable setImageResourceAsync(int i) {
        Drawable drawable;
        if (i != 0) {
            try {
                drawable = getContext().getDrawable(i);
            } catch (Exception e) {
                Log.w(LOG_TAG, "Unable to find resource: " + i, e);
                i = 0;
                drawable = null;
            }
        } else {
            drawable = null;
        }
        return new ImageDrawableCallback(drawable, null, i);
    }

    @RemotableViewMethod(asyncImpl = "setImageURIAsync")
    public void setImageURI(Uri uri) {
        if (this.mResource == 0) {
            if (this.mUri == uri) {
                return;
            }
            if (uri != null && this.mUri != null && uri.equals(this.mUri)) {
                return;
            }
        }
        updateDrawable(null);
        this.mResource = 0;
        this.mUri = uri;
        int i = this.mDrawableWidth;
        int i2 = this.mDrawableHeight;
        resolveUri();
        if (i != this.mDrawableWidth || i2 != this.mDrawableHeight) {
            requestLayout();
        }
        invalidate();
    }

    public Runnable setImageURIAsync(Uri uri) {
        Drawable drawableFromUri;
        if (this.mResource == 0 && (this.mUri == uri || (uri != null && this.mUri != null && uri.equals(this.mUri)))) {
            return null;
        }
        if (uri != null) {
            drawableFromUri = getDrawableFromUri(uri);
        } else {
            drawableFromUri = null;
        }
        if (drawableFromUri == null) {
            uri = null;
        }
        return new ImageDrawableCallback(drawableFromUri, uri, 0);
    }

    public void setImageDrawable(Drawable drawable) {
        if (this.mDrawable != drawable) {
            this.mResource = 0;
            this.mUri = null;
            int i = this.mDrawableWidth;
            int i2 = this.mDrawableHeight;
            updateDrawable(drawable);
            if (i != this.mDrawableWidth || i2 != this.mDrawableHeight) {
                requestLayout();
            }
            invalidate();
        }
    }

    @RemotableViewMethod(asyncImpl = "setImageIconAsync")
    public void setImageIcon(Icon icon) {
        setImageDrawable(icon == null ? null : icon.loadDrawable(this.mContext));
    }

    public Runnable setImageIconAsync(Icon icon) {
        return new ImageDrawableCallback(icon == null ? null : icon.loadDrawable(this.mContext), null, 0);
    }

    public void setImageTintList(ColorStateList colorStateList) {
        this.mDrawableTintList = colorStateList;
        this.mHasDrawableTint = true;
        applyImageTint();
    }

    public ColorStateList getImageTintList() {
        return this.mDrawableTintList;
    }

    public void setImageTintMode(PorterDuff.Mode mode) {
        this.mDrawableTintMode = mode;
        this.mHasDrawableTintMode = true;
        applyImageTint();
    }

    public PorterDuff.Mode getImageTintMode() {
        return this.mDrawableTintMode;
    }

    private void applyImageTint() {
        if (this.mDrawable != null) {
            if (this.mHasDrawableTint || this.mHasDrawableTintMode) {
                this.mDrawable = this.mDrawable.mutate();
                if (this.mHasDrawableTint) {
                    this.mDrawable.setTintList(this.mDrawableTintList);
                }
                if (this.mHasDrawableTintMode) {
                    this.mDrawable.setTintMode(this.mDrawableTintMode);
                }
                if (this.mDrawable.isStateful()) {
                    this.mDrawable.setState(getDrawableState());
                }
            }
        }
    }

    @RemotableViewMethod
    public void setImageBitmap(Bitmap bitmap) {
        this.mDrawable = null;
        if (this.mRecycleableBitmapDrawable == null) {
            this.mRecycleableBitmapDrawable = new BitmapDrawable(this.mContext.getResources(), bitmap);
        } else {
            this.mRecycleableBitmapDrawable.setBitmap(bitmap);
        }
        setImageDrawable(this.mRecycleableBitmapDrawable);
    }

    public void setImageState(int[] iArr, boolean z) {
        this.mState = iArr;
        this.mMergeState = z;
        if (this.mDrawable != null) {
            refreshDrawableState();
            resizeFromDrawable();
        }
    }

    @Override
    public void setSelected(boolean z) {
        super.setSelected(z);
        resizeFromDrawable();
    }

    @RemotableViewMethod
    public void setImageLevel(int i) {
        this.mLevel = i;
        if (this.mDrawable != null) {
            this.mDrawable.setLevel(i);
            resizeFromDrawable();
        }
    }

    public enum ScaleType {
        MATRIX(0),
        FIT_XY(1),
        FIT_START(2),
        FIT_CENTER(3),
        FIT_END(4),
        CENTER(5),
        CENTER_CROP(6),
        CENTER_INSIDE(7);

        final int nativeInt;

        ScaleType(int i) {
            this.nativeInt = i;
        }
    }

    public void setScaleType(ScaleType scaleType) {
        if (scaleType == null) {
            throw new NullPointerException();
        }
        if (this.mScaleType != scaleType) {
            this.mScaleType = scaleType;
            requestLayout();
            invalidate();
        }
    }

    public ScaleType getScaleType() {
        return this.mScaleType;
    }

    public Matrix getImageMatrix() {
        if (this.mDrawMatrix == null) {
            return new Matrix(Matrix.IDENTITY_MATRIX);
        }
        return this.mDrawMatrix;
    }

    public void setImageMatrix(Matrix matrix) {
        if (matrix != null && matrix.isIdentity()) {
            matrix = null;
        }
        if ((matrix == null && !this.mMatrix.isIdentity()) || (matrix != null && !this.mMatrix.equals(matrix))) {
            this.mMatrix.set(matrix);
            configureBounds();
            invalidate();
        }
    }

    public boolean getCropToPadding() {
        return this.mCropToPadding;
    }

    public void setCropToPadding(boolean z) {
        if (this.mCropToPadding != z) {
            this.mCropToPadding = z;
            requestLayout();
            invalidate();
        }
    }

    private void resolveUri() {
        Drawable drawable;
        if (this.mDrawable != null || getResources() == null) {
            return;
        }
        if (this.mResource != 0) {
            try {
                drawable = this.mContext.getDrawable(this.mResource);
            } catch (Exception e) {
                Log.w(LOG_TAG, "Unable to find resource: " + this.mResource, e);
                this.mResource = 0;
                drawable = null;
            }
        } else if (this.mUri != null) {
            drawable = getDrawableFromUri(this.mUri);
            if (drawable == null) {
                Log.w(LOG_TAG, "resolveUri failed on bad bitmap uri: " + this.mUri);
                this.mUri = null;
            }
        } else {
            return;
        }
        updateDrawable(drawable);
    }

    private Drawable getDrawableFromUri(Uri uri) {
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)) {
            try {
                ContentResolver.OpenResourceIdResult resourceId = this.mContext.getContentResolver().getResourceId(uri);
                return resourceId.r.getDrawable(resourceId.id, this.mContext.getTheme());
            } catch (Exception e) {
                Log.w(LOG_TAG, "Unable to open content: " + uri, e);
            }
        } else if ("content".equals(scheme) || ContentResolver.SCHEME_FILE.equals(scheme)) {
            try {
                return ImageDecoder.decodeDrawable(ImageDecoder.createSource(this.mContext.getContentResolver(), uri, sCompatUseCorrectStreamDensity ? getResources() : null), new ImageDecoder.OnHeaderDecodedListener() {
                    @Override
                    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageDecoder.ImageInfo imageInfo, ImageDecoder.Source source) {
                        imageDecoder.setAllocator(1);
                    }
                });
            } catch (IOException e2) {
                Log.w(LOG_TAG, "Unable to open content: " + uri, e2);
            }
        } else {
            return Drawable.createFromPath(uri.toString());
        }
        return null;
    }

    @Override
    public int[] onCreateDrawableState(int i) {
        if (this.mState == null) {
            return super.onCreateDrawableState(i);
        }
        if (!this.mMergeState) {
            return this.mState;
        }
        return mergeDrawableStates(super.onCreateDrawableState(i + this.mState.length), this.mState);
    }

    private void updateDrawable(Drawable drawable) {
        boolean z;
        if (drawable != this.mRecycleableBitmapDrawable && this.mRecycleableBitmapDrawable != null) {
            this.mRecycleableBitmapDrawable.setBitmap(null);
        }
        boolean z2 = false;
        if (this.mDrawable != null) {
            z = this.mDrawable == drawable;
            this.mDrawable.setCallback(null);
            unscheduleDrawable(this.mDrawable);
            if (!sCompatDrawableVisibilityDispatch && !z && isAttachedToWindow()) {
                this.mDrawable.setVisible(false, false);
            }
        } else {
            z = false;
        }
        this.mDrawable = drawable;
        if (drawable != null) {
            drawable.setCallback(this);
            drawable.setLayoutDirection(getLayoutDirection());
            if (drawable.isStateful()) {
                drawable.setState(getDrawableState());
            }
            if (!z || sCompatDrawableVisibilityDispatch) {
                if (sCompatDrawableVisibilityDispatch) {
                    drawable.setVisible(z2, true);
                } else {
                    drawable.setVisible(z2, true);
                }
            }
            drawable.setLevel(this.mLevel);
            this.mDrawableWidth = drawable.getIntrinsicWidth();
            this.mDrawableHeight = drawable.getIntrinsicHeight();
            applyImageTint();
            applyColorMod();
            configureBounds();
            return;
        }
        this.mDrawableHeight = -1;
        this.mDrawableWidth = -1;
    }

    private void resizeFromDrawable() {
        Drawable drawable = this.mDrawable;
        if (drawable != null) {
            int intrinsicWidth = drawable.getIntrinsicWidth();
            if (intrinsicWidth < 0) {
                intrinsicWidth = this.mDrawableWidth;
            }
            int intrinsicHeight = drawable.getIntrinsicHeight();
            if (intrinsicHeight < 0) {
                intrinsicHeight = this.mDrawableHeight;
            }
            if (intrinsicWidth != this.mDrawableWidth || intrinsicHeight != this.mDrawableHeight) {
                this.mDrawableWidth = intrinsicWidth;
                this.mDrawableHeight = intrinsicHeight;
                requestLayout();
            }
        }
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        if (this.mDrawable != null) {
            this.mDrawable.setLayoutDirection(i);
        }
    }

    private static Matrix.ScaleToFit scaleTypeToScaleToFit(ScaleType scaleType) {
        return sS2FArray[scaleType.nativeInt - 1];
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int i3;
        int i4;
        float f;
        boolean z;
        boolean z2;
        int iResolveSizeAndState;
        int iResolveSizeAndState2;
        boolean z3;
        resolveUri();
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        if (this.mDrawable == null) {
            this.mDrawableWidth = -1;
            this.mDrawableHeight = -1;
            f = 0.0f;
            z = false;
            z2 = false;
            i3 = 0;
            i4 = 0;
        } else {
            i3 = this.mDrawableWidth;
            i4 = this.mDrawableHeight;
            if (i3 <= 0) {
                i3 = 1;
            }
            if (i4 <= 0) {
                i4 = 1;
            }
            if (this.mAdjustViewBounds) {
                z = mode != 1073741824;
                z2 = mode2 != 1073741824;
                f = i3 / i4;
            } else {
                f = 0.0f;
                z = false;
                z2 = false;
            }
        }
        int i5 = this.mPaddingLeft;
        int i6 = this.mPaddingRight;
        int i7 = this.mPaddingTop;
        int i8 = this.mPaddingBottom;
        if (z || z2) {
            int iResolveAdjustedSize = resolveAdjustedSize(i3 + i5 + i6, this.mMaxWidth, i);
            int iResolveAdjustedSize2 = resolveAdjustedSize(i4 + i7 + i8, this.mMaxHeight, i2);
            if (f != 0.0f) {
                float f2 = (iResolveAdjustedSize2 - i7) - i8;
                if (Math.abs((((iResolveAdjustedSize - i5) - i6) / f2) - f) <= 1.0E-7d) {
                    iResolveSizeAndState = iResolveAdjustedSize;
                    iResolveSizeAndState2 = iResolveAdjustedSize2;
                } else if (z) {
                    int i9 = ((int) (f2 * f)) + i5 + i6;
                    if (!z2 && !sCompatAdjustViewBounds) {
                        iResolveAdjustedSize = resolveAdjustedSize(i9, this.mMaxWidth, i);
                    }
                    if (i9 <= iResolveAdjustedSize) {
                        iResolveAdjustedSize = i9;
                        z3 = true;
                    } else {
                        z3 = false;
                    }
                    if (!z3 && z2) {
                        int i10 = ((int) (((iResolveAdjustedSize - i5) - i6) / f)) + i7 + i8;
                        if (!z && !sCompatAdjustViewBounds) {
                            iResolveAdjustedSize2 = resolveAdjustedSize(i10, this.mMaxHeight, i2);
                        }
                        if (i10 <= iResolveAdjustedSize2) {
                            iResolveSizeAndState2 = i10;
                            iResolveSizeAndState = iResolveAdjustedSize;
                        }
                    }
                }
            } else {
                iResolveSizeAndState = iResolveAdjustedSize;
                iResolveSizeAndState2 = iResolveAdjustedSize2;
            }
        } else {
            int iMax = Math.max(i3 + i5 + i6, getSuggestedMinimumWidth());
            int iMax2 = Math.max(i4 + i7 + i8, getSuggestedMinimumHeight());
            iResolveSizeAndState = resolveSizeAndState(iMax, i, 0);
            iResolveSizeAndState2 = resolveSizeAndState(iMax2, i2, 0);
        }
        setMeasuredDimension(iResolveSizeAndState, iResolveSizeAndState2);
    }

    private int resolveAdjustedSize(int i, int i2, int i3) {
        int mode = View.MeasureSpec.getMode(i3);
        int size = View.MeasureSpec.getSize(i3);
        if (mode == Integer.MIN_VALUE) {
            return Math.min(Math.min(i, size), i2);
        }
        if (mode != 0) {
            return mode != 1073741824 ? i : size;
        }
        return Math.min(i, i2);
    }

    @Override
    protected boolean setFrame(int i, int i2, int i3, int i4) {
        boolean frame = super.setFrame(i, i2, i3, i4);
        this.mHaveFrame = true;
        configureBounds();
        return frame;
    }

    private void configureBounds() {
        boolean z;
        float fMin;
        float f;
        float f2;
        if (this.mDrawable == null || !this.mHaveFrame) {
            return;
        }
        int i = this.mDrawableWidth;
        int i2 = this.mDrawableHeight;
        int width = (getWidth() - this.mPaddingLeft) - this.mPaddingRight;
        int height = (getHeight() - this.mPaddingTop) - this.mPaddingBottom;
        if ((i < 0 || width == i) && (i2 < 0 || height == i2)) {
            z = true;
        } else {
            z = false;
        }
        if (i <= 0 || i2 <= 0 || ScaleType.FIT_XY == this.mScaleType) {
            this.mDrawable.setBounds(0, 0, width, height);
            this.mDrawMatrix = null;
            return;
        }
        this.mDrawable.setBounds(0, 0, i, i2);
        if (ScaleType.MATRIX == this.mScaleType) {
            if (this.mMatrix.isIdentity()) {
                this.mDrawMatrix = null;
                return;
            } else {
                this.mDrawMatrix = this.mMatrix;
                return;
            }
        }
        if (z) {
            this.mDrawMatrix = null;
            return;
        }
        if (ScaleType.CENTER == this.mScaleType) {
            this.mDrawMatrix = this.mMatrix;
            this.mDrawMatrix.setTranslate(Math.round((width - i) * 0.5f), Math.round((height - i2) * 0.5f));
            return;
        }
        float f3 = 0.0f;
        if (ScaleType.CENTER_CROP == this.mScaleType) {
            this.mDrawMatrix = this.mMatrix;
            if (i * height > width * i2) {
                float f4 = height / i2;
                f2 = 0.0f;
                f3 = (width - (i * f4)) * 0.5f;
                f = f4;
            } else {
                f = width / i;
                f2 = (height - (i2 * f)) * 0.5f;
            }
            this.mDrawMatrix.setScale(f, f);
            this.mDrawMatrix.postTranslate(Math.round(f3), Math.round(f2));
            return;
        }
        if (ScaleType.CENTER_INSIDE == this.mScaleType) {
            this.mDrawMatrix = this.mMatrix;
            if (i <= width && i2 <= height) {
                fMin = 1.0f;
            } else {
                fMin = Math.min(width / i, height / i2);
            }
            float fRound = Math.round((width - (i * fMin)) * 0.5f);
            float fRound2 = Math.round((height - (i2 * fMin)) * 0.5f);
            this.mDrawMatrix.setScale(fMin, fMin);
            this.mDrawMatrix.postTranslate(fRound, fRound2);
            return;
        }
        this.mTempSrc.set(0.0f, 0.0f, i, i2);
        this.mTempDst.set(0.0f, 0.0f, width, height);
        this.mDrawMatrix = this.mMatrix;
        this.mDrawMatrix.setRectToRect(this.mTempSrc, this.mTempDst, scaleTypeToScaleToFit(this.mScaleType));
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable drawable = this.mDrawable;
        if (drawable != null && drawable.isStateful() && drawable.setState(getDrawableState())) {
            invalidateDrawable(drawable);
        }
    }

    @Override
    public void drawableHotspotChanged(float f, float f2) {
        super.drawableHotspotChanged(f, f2);
        if (this.mDrawable != null) {
            this.mDrawable.setHotspot(f, f2);
        }
    }

    public void animateTransform(Matrix matrix) {
        if (this.mDrawable == null) {
            return;
        }
        if (matrix == null) {
            this.mDrawable.setBounds(0, 0, getWidth(), getHeight());
        } else {
            this.mDrawable.setBounds(0, 0, this.mDrawableWidth, this.mDrawableHeight);
            if (this.mDrawMatrix == null) {
                this.mDrawMatrix = new Matrix();
            }
            this.mDrawMatrix.set(matrix);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mDrawable == null || this.mDrawableWidth == 0 || this.mDrawableHeight == 0) {
            return;
        }
        if (this.mDrawMatrix == null && this.mPaddingTop == 0 && this.mPaddingLeft == 0) {
            this.mDrawable.draw(canvas);
            return;
        }
        int saveCount = canvas.getSaveCount();
        canvas.save();
        if (this.mCropToPadding) {
            int i = this.mScrollX;
            int i2 = this.mScrollY;
            canvas.clipRect(this.mPaddingLeft + i, this.mPaddingTop + i2, ((i + this.mRight) - this.mLeft) - this.mPaddingRight, ((i2 + this.mBottom) - this.mTop) - this.mPaddingBottom);
        }
        canvas.translate(this.mPaddingLeft, this.mPaddingTop);
        if (this.mDrawMatrix != null) {
            canvas.concat(this.mDrawMatrix);
        }
        this.mDrawable.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    public int getBaseline() {
        if (this.mBaselineAlignBottom) {
            return getMeasuredHeight();
        }
        return this.mBaseline;
    }

    public void setBaseline(int i) {
        if (this.mBaseline != i) {
            this.mBaseline = i;
            requestLayout();
        }
    }

    public void setBaselineAlignBottom(boolean z) {
        if (this.mBaselineAlignBottom != z) {
            this.mBaselineAlignBottom = z;
            requestLayout();
        }
    }

    public boolean getBaselineAlignBottom() {
        return this.mBaselineAlignBottom;
    }

    public final void setColorFilter(int i, PorterDuff.Mode mode) {
        setColorFilter(new PorterDuffColorFilter(i, mode));
    }

    @RemotableViewMethod
    public final void setColorFilter(int i) {
        setColorFilter(i, PorterDuff.Mode.SRC_ATOP);
    }

    public final void clearColorFilter() {
        setColorFilter((ColorFilter) null);
    }

    public final void setXfermode(Xfermode xfermode) {
        if (this.mXfermode != xfermode) {
            this.mXfermode = xfermode;
            this.mColorMod = true;
            applyColorMod();
            invalidate();
        }
    }

    public ColorFilter getColorFilter() {
        return this.mColorFilter;
    }

    public void setColorFilter(ColorFilter colorFilter) {
        if (this.mColorFilter != colorFilter) {
            this.mColorFilter = colorFilter;
            this.mHasColorFilter = true;
            this.mColorMod = true;
            applyColorMod();
            invalidate();
        }
    }

    public int getImageAlpha() {
        return this.mAlpha;
    }

    @RemotableViewMethod
    public void setImageAlpha(int i) {
        setAlpha(i);
    }

    @RemotableViewMethod
    @Deprecated
    public void setAlpha(int i) {
        int i2 = i & 255;
        if (this.mAlpha != i2) {
            this.mAlpha = i2;
            this.mColorMod = true;
            applyColorMod();
            invalidate();
        }
    }

    private void applyColorMod() {
        if (this.mDrawable != null && this.mColorMod) {
            this.mDrawable = this.mDrawable.mutate();
            if (this.mHasColorFilter) {
                this.mDrawable.setColorFilter(this.mColorFilter);
            }
            this.mDrawable.setXfermode(this.mXfermode);
            this.mDrawable.setAlpha((this.mAlpha * 256) >> 8);
        }
    }

    @Override
    public boolean isOpaque() {
        return super.isOpaque() || (this.mDrawable != null && this.mXfermode == null && this.mDrawable.getOpacity() == -1 && ((this.mAlpha * 256) >> 8) == 255 && isFilledByImage());
    }

    private boolean isFilledByImage() {
        if (this.mDrawable == null) {
            return false;
        }
        Rect bounds = this.mDrawable.getBounds();
        Matrix matrix = this.mDrawMatrix;
        if (matrix == null) {
            return bounds.left <= 0 && bounds.top <= 0 && bounds.right >= getWidth() && bounds.bottom >= getHeight();
        }
        if (!matrix.rectStaysRect()) {
            return false;
        }
        RectF rectF = this.mTempSrc;
        RectF rectF2 = this.mTempDst;
        rectF.set(bounds);
        matrix.mapRect(rectF2, rectF);
        return rectF2.left <= 0.0f && rectF2.top <= 0.0f && rectF2.right >= ((float) getWidth()) && rectF2.bottom >= ((float) getHeight());
    }

    @Override
    public void onVisibilityAggregated(boolean z) {
        super.onVisibilityAggregated(z);
        if (this.mDrawable != null && !sCompatDrawableVisibilityDispatch) {
            this.mDrawable.setVisible(z, false);
        }
    }

    @Override
    @RemotableViewMethod
    public void setVisibility(int i) {
        super.setVisibility(i);
        if (this.mDrawable != null && sCompatDrawableVisibilityDispatch) {
            this.mDrawable.setVisible(i == 0, false);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mDrawable != null && sCompatDrawableVisibilityDispatch) {
            this.mDrawable.setVisible(getVisibility() == 0, false);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mDrawable != null && sCompatDrawableVisibilityDispatch) {
            this.mDrawable.setVisible(false, false);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ImageView.class.getName();
    }

    @Override
    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        super.encodeProperties(viewHierarchyEncoder);
        viewHierarchyEncoder.addProperty("layout:baseline", getBaseline());
    }

    @Override
    public boolean isDefaultFocusHighlightNeeded(Drawable drawable, Drawable drawable2) {
        return super.isDefaultFocusHighlightNeeded(drawable, drawable2) && (this.mDrawable == null || !this.mDrawable.isStateful() || !this.mDrawable.hasFocusStateSpecified());
    }
}

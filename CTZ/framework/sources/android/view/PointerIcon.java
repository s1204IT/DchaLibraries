package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.R;
import com.android.internal.util.XmlUtils;

public final class PointerIcon implements Parcelable {
    private static final String TAG = "PointerIcon";
    public static final int TYPE_ALIAS = 1010;
    public static final int TYPE_ALL_SCROLL = 1013;
    public static final int TYPE_ARROW = 1000;
    public static final int TYPE_CELL = 1006;
    public static final int TYPE_CONTEXT_MENU = 1001;
    public static final int TYPE_COPY = 1011;
    public static final int TYPE_CROSSHAIR = 1007;
    public static final int TYPE_CUSTOM = -1;
    public static final int TYPE_DEFAULT = 1000;
    public static final int TYPE_GRAB = 1020;
    public static final int TYPE_GRABBING = 1021;
    public static final int TYPE_HAND = 1002;
    public static final int TYPE_HELP = 1003;
    public static final int TYPE_HORIZONTAL_DOUBLE_ARROW = 1014;
    public static final int TYPE_NOT_SPECIFIED = 1;
    public static final int TYPE_NO_DROP = 1012;
    public static final int TYPE_NULL = 0;
    private static final int TYPE_OEM_FIRST = 10000;
    public static final int TYPE_SPOT_ANCHOR = 2002;
    public static final int TYPE_SPOT_HOVER = 2000;
    public static final int TYPE_SPOT_TOUCH = 2001;
    public static final int TYPE_TEXT = 1008;
    public static final int TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW = 1017;
    public static final int TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW = 1016;
    public static final int TYPE_VERTICAL_DOUBLE_ARROW = 1015;
    public static final int TYPE_VERTICAL_TEXT = 1009;
    public static final int TYPE_WAIT = 1004;
    public static final int TYPE_ZOOM_IN = 1018;
    public static final int TYPE_ZOOM_OUT = 1019;
    private Bitmap mBitmap;
    private Bitmap[] mBitmapFrames;
    private int mDurationPerFrame;
    private float mHotSpotX;
    private float mHotSpotY;
    private int mSystemIconResourceId;
    private final int mType;
    private static final PointerIcon gNullIcon = new PointerIcon(0);
    private static final SparseArray<PointerIcon> gSystemIcons = new SparseArray<>();
    private static boolean sUseLargeIcons = false;
    public static final Parcelable.Creator<PointerIcon> CREATOR = new Parcelable.Creator<PointerIcon>() {
        @Override
        public PointerIcon createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            if (i == 0) {
                return PointerIcon.getNullIcon();
            }
            int i2 = parcel.readInt();
            if (i2 != 0) {
                PointerIcon pointerIcon = new PointerIcon(i);
                pointerIcon.mSystemIconResourceId = i2;
                return pointerIcon;
            }
            return PointerIcon.create(Bitmap.CREATOR.createFromParcel(parcel), parcel.readFloat(), parcel.readFloat());
        }

        @Override
        public PointerIcon[] newArray(int i) {
            return new PointerIcon[i];
        }
    };

    private PointerIcon(int i) {
        this.mType = i;
    }

    public static PointerIcon getNullIcon() {
        return gNullIcon;
    }

    public static PointerIcon getDefaultIcon(Context context) {
        return getSystemIcon(context, 1000);
    }

    public static PointerIcon getSystemIcon(Context context, int i) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (i == 0) {
            return gNullIcon;
        }
        PointerIcon pointerIcon = gSystemIcons.get(i);
        if (pointerIcon != null) {
            return pointerIcon;
        }
        int systemIconTypeIndex = getSystemIconTypeIndex(i);
        if (systemIconTypeIndex == 0) {
            systemIconTypeIndex = getSystemIconTypeIndex(1000);
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(null, R.styleable.Pointer, 0, sUseLargeIcons ? R.style.LargePointer : R.style.Pointer);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(systemIconTypeIndex, -1);
        typedArrayObtainStyledAttributes.recycle();
        if (resourceId == -1) {
            Log.w(TAG, "Missing theme resources for pointer icon type " + i);
            return i == 1000 ? gNullIcon : getSystemIcon(context, 1000);
        }
        PointerIcon pointerIcon2 = new PointerIcon(i);
        if (((-16777216) & resourceId) == 16777216) {
            pointerIcon2.mSystemIconResourceId = resourceId;
        } else {
            pointerIcon2.loadResource(context, context.getResources(), resourceId);
        }
        gSystemIcons.append(i, pointerIcon2);
        return pointerIcon2;
    }

    public static void setUseLargeIcons(boolean z) {
        sUseLargeIcons = z;
        gSystemIcons.clear();
    }

    public static PointerIcon create(Bitmap bitmap, float f, float f2) {
        if (bitmap == null) {
            throw new IllegalArgumentException("bitmap must not be null");
        }
        validateHotSpot(bitmap, f, f2);
        PointerIcon pointerIcon = new PointerIcon(-1);
        pointerIcon.mBitmap = bitmap;
        pointerIcon.mHotSpotX = f;
        pointerIcon.mHotSpotY = f2;
        return pointerIcon;
    }

    public static PointerIcon load(Resources resources, int i) {
        if (resources == null) {
            throw new IllegalArgumentException("resources must not be null");
        }
        PointerIcon pointerIcon = new PointerIcon(-1);
        pointerIcon.loadResource(null, resources, i);
        return pointerIcon;
    }

    public PointerIcon load(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (this.mSystemIconResourceId == 0 || this.mBitmap != null) {
            return this;
        }
        PointerIcon pointerIcon = new PointerIcon(this.mType);
        pointerIcon.mSystemIconResourceId = this.mSystemIconResourceId;
        pointerIcon.loadResource(context, context.getResources(), this.mSystemIconResourceId);
        return pointerIcon;
    }

    public int getType() {
        return this.mType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mType);
        if (this.mType != 0) {
            parcel.writeInt(this.mSystemIconResourceId);
            if (this.mSystemIconResourceId == 0) {
                this.mBitmap.writeToParcel(parcel, i);
                parcel.writeFloat(this.mHotSpotX);
                parcel.writeFloat(this.mHotSpotY);
            }
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof PointerIcon)) {
            return false;
        }
        PointerIcon pointerIcon = (PointerIcon) obj;
        if (this.mType != pointerIcon.mType || this.mSystemIconResourceId != pointerIcon.mSystemIconResourceId) {
            return false;
        }
        if (this.mSystemIconResourceId != 0 || (this.mBitmap == pointerIcon.mBitmap && this.mHotSpotX == pointerIcon.mHotSpotX && this.mHotSpotY == pointerIcon.mHotSpotY)) {
            return true;
        }
        return false;
    }

    private Bitmap getBitmapFromDrawable(BitmapDrawable bitmapDrawable) {
        Bitmap bitmap = bitmapDrawable.getBitmap();
        int intrinsicWidth = bitmapDrawable.getIntrinsicWidth();
        int intrinsicHeight = bitmapDrawable.getIntrinsicHeight();
        if (intrinsicWidth == bitmap.getWidth() && intrinsicHeight == bitmap.getHeight()) {
            return bitmap;
        }
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(0.0f, 0.0f, intrinsicWidth, intrinsicHeight);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, bitmap.getConfig());
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, rect, rectF, paint);
        return bitmapCreateBitmap;
    }

    private void loadResource(Context context, Resources resources, int i) {
        Drawable drawable;
        XmlResourceParser xml = resources.getXml(i);
        try {
            try {
                XmlUtils.beginDocument(xml, "pointer-icon");
                TypedArray typedArrayObtainAttributes = resources.obtainAttributes(xml, R.styleable.PointerIcon);
                int resourceId = typedArrayObtainAttributes.getResourceId(0, 0);
                float dimension = typedArrayObtainAttributes.getDimension(1, 0.0f);
                float dimension2 = typedArrayObtainAttributes.getDimension(2, 0.0f);
                typedArrayObtainAttributes.recycle();
                if (resourceId == 0) {
                    throw new IllegalArgumentException("<pointer-icon> is missing bitmap attribute.");
                }
                if (context == null) {
                    drawable = resources.getDrawable(resourceId);
                } else {
                    drawable = context.getDrawable(resourceId);
                }
                if (drawable instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                    int numberOfFrames = animationDrawable.getNumberOfFrames();
                    Drawable frame = animationDrawable.getFrame(0);
                    if (numberOfFrames == 1) {
                        Log.w(TAG, "Animation icon with single frame -- simply treating the first frame as a normal bitmap icon.");
                    } else {
                        this.mDurationPerFrame = animationDrawable.getDuration(0);
                        this.mBitmapFrames = new Bitmap[numberOfFrames - 1];
                        int intrinsicWidth = frame.getIntrinsicWidth();
                        int intrinsicHeight = frame.getIntrinsicHeight();
                        for (int i2 = 1; i2 < numberOfFrames; i2++) {
                            Drawable frame2 = animationDrawable.getFrame(i2);
                            if (!(frame2 instanceof BitmapDrawable)) {
                                throw new IllegalArgumentException("Frame of an animated pointer icon must refer to a bitmap drawable.");
                            }
                            if (frame2.getIntrinsicWidth() != intrinsicWidth || frame2.getIntrinsicHeight() != intrinsicHeight) {
                                throw new IllegalArgumentException("The bitmap size of " + i2 + "-th frame is different. All frames should have the exact same size and share the same hotspot.");
                            }
                            this.mBitmapFrames[i2 - 1] = getBitmapFromDrawable((BitmapDrawable) frame2);
                        }
                    }
                    drawable = frame;
                }
                if (!(drawable instanceof BitmapDrawable)) {
                    throw new IllegalArgumentException("<pointer-icon> bitmap attribute must refer to a bitmap drawable.");
                }
                Bitmap bitmapFromDrawable = getBitmapFromDrawable((BitmapDrawable) drawable);
                validateHotSpot(bitmapFromDrawable, dimension, dimension2);
                this.mBitmap = bitmapFromDrawable;
                this.mHotSpotX = dimension;
                this.mHotSpotY = dimension2;
            } catch (Exception e) {
                throw new IllegalArgumentException("Exception parsing pointer icon resource.", e);
            }
        } finally {
            xml.close();
        }
    }

    private static void validateHotSpot(Bitmap bitmap, float f, float f2) {
        if (f < 0.0f || f >= bitmap.getWidth()) {
            throw new IllegalArgumentException("x hotspot lies outside of the bitmap area");
        }
        if (f2 < 0.0f || f2 >= bitmap.getHeight()) {
            throw new IllegalArgumentException("y hotspot lies outside of the bitmap area");
        }
    }

    private static int getSystemIconTypeIndex(int r1) {
        switch (r1) {
            case 1000:
            case 1001:
            case 1002:
            case 1003:
            case 1004:
            default:
                switch (r1) {
                    case 1006:
                        break;
                    case 1007:
                        break;
                    case 1008:
                        break;
                    case 1009:
                        break;
                    case 1010:
                        break;
                    case 1011:
                        break;
                    case 1012:
                        break;
                    case 1013:
                        break;
                    case 1014:
                        break;
                    case 1015:
                        break;
                    case 1016:
                        break;
                    case 1017:
                        break;
                    case 1018:
                        break;
                    case 1019:
                        break;
                    case 1020:
                        break;
                    case 1021:
                        break;
                    default:
                        switch (r1) {
                        }
                }
        }
        return 0;
    }
}

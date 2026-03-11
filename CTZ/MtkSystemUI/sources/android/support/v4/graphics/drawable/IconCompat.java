package android.support.v4.graphics.drawable;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.os.BuildCompat;
import android.support.v4.util.Preconditions;
import android.text.TextUtils;
import android.util.Log;
import androidx.versionedparcelable.CustomVersionedParcelable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

public class IconCompat extends CustomVersionedParcelable {
    static final PorterDuff.Mode DEFAULT_TINT_MODE = PorterDuff.Mode.SRC_IN;
    public int mInt1;
    public int mInt2;
    Object mObj1;
    public ColorStateList mTintList = null;
    PorterDuff.Mode mTintMode = DEFAULT_TINT_MODE;
    public int mType;

    public static IconCompat createWithResource(String pkg, int resId) {
        if (pkg == null) {
            throw new IllegalArgumentException("Package must not be null.");
        }
        IconCompat rep = new IconCompat(2);
        rep.mInt1 = resId;
        rep.mObj1 = pkg;
        return rep;
    }

    public static IconCompat createWithBitmap(Bitmap bits) {
        if (bits == null) {
            throw new IllegalArgumentException("Bitmap must not be null.");
        }
        IconCompat rep = new IconCompat(1);
        rep.mObj1 = bits;
        return rep;
    }

    public static IconCompat createWithContentUri(String uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri must not be null.");
        }
        IconCompat rep = new IconCompat(4);
        rep.mObj1 = uri;
        return rep;
    }

    public static IconCompat createWithContentUri(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri must not be null.");
        }
        return createWithContentUri(uri.toString());
    }

    public IconCompat() {
    }

    private IconCompat(int mType) {
        this.mType = mType;
    }

    public String getResPackage() {
        if (this.mType == -1 && Build.VERSION.SDK_INT >= 23) {
            return getResPackage((Icon) this.mObj1);
        }
        if (this.mType != 2) {
            throw new IllegalStateException("called getResPackage() on " + this);
        }
        return (String) this.mObj1;
    }

    public int getResId() {
        if (this.mType == -1 && Build.VERSION.SDK_INT >= 23) {
            return getResId((Icon) this.mObj1);
        }
        if (this.mType != 2) {
            throw new IllegalStateException("called getResId() on " + this);
        }
        return this.mInt1;
    }

    public Icon toIcon() {
        Icon icon;
        int i = this.mType;
        if (i == -1) {
            return (Icon) this.mObj1;
        }
        switch (i) {
            case 1:
                icon = Icon.createWithBitmap((Bitmap) this.mObj1);
                break;
            case 2:
                icon = Icon.createWithResource((String) this.mObj1, this.mInt1);
                break;
            case 3:
                icon = Icon.createWithData((byte[]) this.mObj1, this.mInt1, this.mInt2);
                break;
            case 4:
                icon = Icon.createWithContentUri((String) this.mObj1);
                break;
            case 5:
                if (Build.VERSION.SDK_INT >= 26) {
                    icon = Icon.createWithAdaptiveBitmap((Bitmap) this.mObj1);
                } else {
                    icon = Icon.createWithBitmap(createLegacyIconFromAdaptiveIcon((Bitmap) this.mObj1, false));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown type");
        }
        if (this.mTintList != null) {
            icon.setTintList(this.mTintList);
        }
        if (this.mTintMode != DEFAULT_TINT_MODE) {
            icon.setTintMode(this.mTintMode);
        }
        return icon;
    }

    public Drawable loadDrawable(Context context) throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= 23) {
            return toIcon().loadDrawable(context);
        }
        Drawable result = loadDrawableInner(context);
        if (result != null && (this.mTintList != null || this.mTintMode != DEFAULT_TINT_MODE)) {
            result.mutate();
            DrawableCompat.setTintList(result, this.mTintList);
            DrawableCompat.setTintMode(result, this.mTintMode);
        }
        return result;
    }

    private Drawable loadDrawableInner(Context context) throws FileNotFoundException {
        Resources res;
        switch (this.mType) {
            case 1:
                return new BitmapDrawable(context.getResources(), (Bitmap) this.mObj1);
            case 2:
                String resPackage = (String) this.mObj1;
                if (TextUtils.isEmpty(resPackage)) {
                    resPackage = context.getPackageName();
                }
                if ("android".equals(resPackage)) {
                    res = Resources.getSystem();
                } else {
                    PackageManager pm = context.getPackageManager();
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(resPackage, 8192);
                        if (ai != null) {
                            Resources res2 = pm.getResourcesForApplication(ai);
                            res = res2;
                        } else {
                            return null;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e("IconCompat", String.format("Unable to find pkg=%s for icon %s", resPackage, this), e);
                        return null;
                    }
                }
                try {
                    return ResourcesCompat.getDrawable(res, this.mInt1, context.getTheme());
                } catch (RuntimeException e2) {
                    Log.e("IconCompat", String.format("Unable to load resource 0x%08x from pkg=%s", Integer.valueOf(this.mInt1), this.mObj1), e2);
                    return null;
                }
            case 3:
                return new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray((byte[]) this.mObj1, this.mInt1, this.mInt2));
            case 4:
                Uri uri = Uri.parse((String) this.mObj1);
                String scheme = uri.getScheme();
                InputStream is = null;
                if ("content".equals(scheme) || "file".equals(scheme)) {
                    try {
                        is = context.getContentResolver().openInputStream(uri);
                    } catch (Exception e3) {
                        Log.w("IconCompat", "Unable to load image from URI: " + uri, e3);
                    }
                } else {
                    try {
                        is = new FileInputStream(new File((String) this.mObj1));
                    } catch (FileNotFoundException e4) {
                        Log.w("IconCompat", "Unable to load image from path: " + uri, e4);
                    }
                }
                if (is != null) {
                    return new BitmapDrawable(context.getResources(), BitmapFactory.decodeStream(is));
                }
                return null;
            case 5:
                return new BitmapDrawable(context.getResources(), createLegacyIconFromAdaptiveIcon((Bitmap) this.mObj1, false));
            default:
                return null;
        }
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        int i = this.mType;
        if (i != -1) {
            switch (i) {
                case 1:
                case 5:
                    bundle.putParcelable("obj", (Bitmap) this.mObj1);
                    break;
                case 2:
                case 4:
                    bundle.putString("obj", (String) this.mObj1);
                    break;
                case 3:
                    bundle.putByteArray("obj", (byte[]) this.mObj1);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid icon");
            }
        } else {
            bundle.putParcelable("obj", (Parcelable) this.mObj1);
        }
        bundle.putInt("type", this.mType);
        bundle.putInt("int1", this.mInt1);
        bundle.putInt("int2", this.mInt2);
        if (this.mTintList != null) {
            bundle.putParcelable("tint_list", this.mTintList);
        }
        if (this.mTintMode != DEFAULT_TINT_MODE) {
            bundle.putString("tint_mode", this.mTintMode.name());
        }
        return bundle;
    }

    public String toString() {
        if (this.mType == -1) {
            return String.valueOf(this.mObj1);
        }
        StringBuilder sb = new StringBuilder("Icon(typ=").append(typeToString(this.mType));
        switch (this.mType) {
            case 1:
            case 5:
                sb.append(" size=");
                sb.append(((Bitmap) this.mObj1).getWidth());
                sb.append("x");
                sb.append(((Bitmap) this.mObj1).getHeight());
                break;
            case 2:
                sb.append(" pkg=");
                sb.append(getResPackage());
                sb.append(" id=");
                sb.append(String.format("0x%08x", Integer.valueOf(getResId())));
                break;
            case 3:
                sb.append(" len=");
                sb.append(this.mInt1);
                if (this.mInt2 != 0) {
                    sb.append(" off=");
                    sb.append(this.mInt2);
                }
                break;
            case 4:
                sb.append(" uri=");
                sb.append(this.mObj1);
                break;
        }
        if (this.mTintList != null) {
            sb.append(" tint=");
            sb.append(this.mTintList);
        }
        if (this.mTintMode != DEFAULT_TINT_MODE) {
            sb.append(" mode=");
            sb.append(this.mTintMode);
        }
        sb.append(")");
        return sb.toString();
    }

    private static String typeToString(int x) {
        switch (x) {
            case 1:
                return "BITMAP";
            case 2:
                return "RESOURCE";
            case 3:
                return "DATA";
            case 4:
                return "URI";
            case 5:
                return "BITMAP_MASKABLE";
            default:
                return "UNKNOWN";
        }
    }

    public static IconCompat createFromBundle(Bundle bundle) {
        int type = bundle.getInt("type");
        IconCompat icon = new IconCompat(type);
        icon.mInt1 = bundle.getInt("int1");
        icon.mInt2 = bundle.getInt("int2");
        if (bundle.containsKey("tint_list")) {
            icon.mTintList = (ColorStateList) bundle.getParcelable("tint_list");
        }
        if (bundle.containsKey("tint_mode")) {
            icon.mTintMode = PorterDuff.Mode.valueOf(bundle.getString("tint_mode"));
        }
        if (type != -1) {
            switch (type) {
                case 1:
                case 5:
                    icon.mObj1 = bundle.getParcelable("obj");
                    break;
                case 2:
                case 4:
                    icon.mObj1 = bundle.getString("obj");
                    break;
                case 3:
                    icon.mObj1 = bundle.getByteArray("obj");
                    break;
                default:
                    Log.w("IconCompat", "Unknown type " + type);
                    return null;
            }
        }
        return icon;
    }

    public static IconCompat createFromIcon(Icon icon) {
        Preconditions.checkNotNull(icon);
        int type = getType(icon);
        if (type == 2) {
            return createWithResource(getResPackage(icon), getResId(icon));
        }
        if (type == 4) {
            return createWithContentUri(getUri(icon));
        }
        IconCompat iconCompat = new IconCompat(-1);
        iconCompat.mObj1 = icon;
        return iconCompat;
    }

    public static int getType(Icon icon) {
        if (BuildCompat.isAtLeastP()) {
            return icon.getType();
        }
        try {
            return ((Integer) icon.getClass().getMethod("getType", new Class[0]).invoke(icon, new Object[0])).intValue();
        } catch (IllegalAccessException e) {
            Log.e("IconCompat", "Unable to get icon type " + icon, e);
            return -1;
        } catch (NoSuchMethodException e2) {
            Log.e("IconCompat", "Unable to get icon type " + icon, e2);
            return -1;
        } catch (InvocationTargetException e3) {
            Log.e("IconCompat", "Unable to get icon type " + icon, e3);
            return -1;
        }
    }

    public static String getResPackage(Icon icon) {
        if (BuildCompat.isAtLeastP()) {
            return icon.getResPackage();
        }
        try {
            return (String) icon.getClass().getMethod("getResPackage", new Class[0]).invoke(icon, new Object[0]);
        } catch (IllegalAccessException e) {
            Log.e("IconCompat", "Unable to get icon package", e);
            return null;
        } catch (NoSuchMethodException e2) {
            Log.e("IconCompat", "Unable to get icon package", e2);
            return null;
        } catch (InvocationTargetException e3) {
            Log.e("IconCompat", "Unable to get icon package", e3);
            return null;
        }
    }

    public static int getResId(Icon icon) {
        if (BuildCompat.isAtLeastP()) {
            return icon.getResId();
        }
        try {
            return ((Integer) icon.getClass().getMethod("getResId", new Class[0]).invoke(icon, new Object[0])).intValue();
        } catch (IllegalAccessException e) {
            Log.e("IconCompat", "Unable to get icon resource", e);
            return 0;
        } catch (NoSuchMethodException e2) {
            Log.e("IconCompat", "Unable to get icon resource", e2);
            return 0;
        } catch (InvocationTargetException e3) {
            Log.e("IconCompat", "Unable to get icon resource", e3);
            return 0;
        }
    }

    public static Uri getUri(Icon icon) {
        if (BuildCompat.isAtLeastP()) {
            return icon.getUri();
        }
        try {
            return (Uri) icon.getClass().getMethod("getUri", new Class[0]).invoke(icon, new Object[0]);
        } catch (IllegalAccessException e) {
            Log.e("IconCompat", "Unable to get icon uri", e);
            return null;
        } catch (NoSuchMethodException e2) {
            Log.e("IconCompat", "Unable to get icon uri", e2);
            return null;
        } catch (InvocationTargetException e3) {
            Log.e("IconCompat", "Unable to get icon uri", e3);
            return null;
        }
    }

    static Bitmap createLegacyIconFromAdaptiveIcon(Bitmap adaptiveIconBitmap, boolean addShadow) {
        int size = (int) (0.6666667f * Math.min(adaptiveIconBitmap.getWidth(), adaptiveIconBitmap.getHeight()));
        Bitmap icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);
        Paint paint = new Paint(3);
        float center = size * 0.5f;
        float radius = 0.9166667f * center;
        if (addShadow) {
            float blur = 0.010416667f * size;
            paint.setColor(0);
            paint.setShadowLayer(blur, 0.0f, 0.020833334f * size, 1023410176);
            canvas.drawCircle(center, center, radius, paint);
            paint.setShadowLayer(blur, 0.0f, 0.0f, 503316480);
            canvas.drawCircle(center, center, radius, paint);
            paint.clearShadowLayer();
        }
        paint.setColor(-16777216);
        BitmapShader shader = new BitmapShader(adaptiveIconBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix shift = new Matrix();
        shift.setTranslate((-(adaptiveIconBitmap.getWidth() - size)) / 2, (-(adaptiveIconBitmap.getHeight() - size)) / 2);
        shader.setLocalMatrix(shift);
        paint.setShader(shader);
        canvas.drawCircle(center, center, radius, paint);
        canvas.setBitmap(null);
        return icon;
    }
}

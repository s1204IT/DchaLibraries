package android.graphics.drawable;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

public final class Icon implements Parcelable {
    public static final int MIN_ASHMEM_ICON_SIZE = 131072;
    private static final String TAG = "Icon";
    public static final int TYPE_ADAPTIVE_BITMAP = 5;
    public static final int TYPE_BITMAP = 1;
    public static final int TYPE_DATA = 3;
    public static final int TYPE_RESOURCE = 2;
    public static final int TYPE_URI = 4;
    private static final int VERSION_STREAM_SERIALIZER = 1;
    private int mInt1;
    private int mInt2;
    private Object mObj1;
    private String mString1;
    private ColorStateList mTintList;
    private PorterDuff.Mode mTintMode;
    private final int mType;
    static final PorterDuff.Mode DEFAULT_TINT_MODE = Drawable.DEFAULT_TINT_MODE;
    public static final Parcelable.Creator<Icon> CREATOR = new Parcelable.Creator<Icon>() {
        @Override
        public Icon createFromParcel(Parcel parcel) {
            return new Icon(parcel);
        }

        @Override
        public Icon[] newArray(int i) {
            return new Icon[i];
        }
    };

    public @interface IconType {
    }

    public interface OnDrawableLoadedListener {
        void onDrawableLoaded(Drawable drawable);
    }

    @IconType
    public int getType() {
        return this.mType;
    }

    public Bitmap getBitmap() {
        if (this.mType != 1 && this.mType != 5) {
            throw new IllegalStateException("called getBitmap() on " + this);
        }
        return (Bitmap) this.mObj1;
    }

    private void setBitmap(Bitmap bitmap) {
        this.mObj1 = bitmap;
    }

    public int getDataLength() {
        int i;
        if (this.mType != 3) {
            throw new IllegalStateException("called getDataLength() on " + this);
        }
        synchronized (this) {
            i = this.mInt1;
        }
        return i;
    }

    public int getDataOffset() {
        int i;
        if (this.mType != 3) {
            throw new IllegalStateException("called getDataOffset() on " + this);
        }
        synchronized (this) {
            i = this.mInt2;
        }
        return i;
    }

    public byte[] getDataBytes() {
        byte[] bArr;
        if (this.mType != 3) {
            throw new IllegalStateException("called getDataBytes() on " + this);
        }
        synchronized (this) {
            bArr = (byte[]) this.mObj1;
        }
        return bArr;
    }

    public Resources getResources() {
        if (this.mType != 2) {
            throw new IllegalStateException("called getResources() on " + this);
        }
        return (Resources) this.mObj1;
    }

    public String getResPackage() {
        if (this.mType != 2) {
            throw new IllegalStateException("called getResPackage() on " + this);
        }
        return this.mString1;
    }

    public int getResId() {
        if (this.mType != 2) {
            throw new IllegalStateException("called getResId() on " + this);
        }
        return this.mInt1;
    }

    public String getUriString() {
        if (this.mType != 4) {
            throw new IllegalStateException("called getUriString() on " + this);
        }
        return this.mString1;
    }

    public Uri getUri() {
        return Uri.parse(getUriString());
    }

    private static final String typeToString(int i) {
        switch (i) {
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
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
    }

    public void loadDrawableAsync(Context context, Message message) {
        if (message.getTarget() == null) {
            throw new IllegalArgumentException("callback message must have a target handler");
        }
        new LoadDrawableTask(context, message).runAsync();
    }

    public void loadDrawableAsync(Context context, OnDrawableLoadedListener onDrawableLoadedListener, Handler handler) {
        new LoadDrawableTask(context, handler, onDrawableLoadedListener).runAsync();
    }

    public Drawable loadDrawable(Context context) throws Throwable {
        Drawable drawableLoadDrawableInner = loadDrawableInner(context);
        if (drawableLoadDrawableInner != null && (this.mTintList != null || this.mTintMode != DEFAULT_TINT_MODE)) {
            drawableLoadDrawableInner.mutate();
            drawableLoadDrawableInner.setTintList(this.mTintList);
            drawableLoadDrawableInner.setTintMode(this.mTintMode);
        }
        return drawableLoadDrawableInner;
    }

    private Drawable loadDrawableInner(Context context) throws Throwable {
        InputStream inputStreamOpenInputStream;
        switch (this.mType) {
            case 1:
                return new BitmapDrawable(context.getResources(), getBitmap());
            case 2:
                if (getResources() == null) {
                    String resPackage = getResPackage();
                    if (TextUtils.isEmpty(resPackage)) {
                        resPackage = context.getPackageName();
                    }
                    if (ZenModeConfig.SYSTEM_AUTHORITY.equals(resPackage)) {
                        this.mObj1 = Resources.getSystem();
                    } else {
                        PackageManager packageManager = context.getPackageManager();
                        try {
                            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(resPackage, 8192);
                            if (applicationInfo != null) {
                                this.mObj1 = packageManager.getResourcesForApplication(applicationInfo);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e(TAG, String.format("Unable to find pkg=%s for icon %s", resPackage, this), e);
                        }
                    }
                    try {
                        return getResources().getDrawable(getResId(), context.getTheme());
                    } catch (RuntimeException e2) {
                        Log.e(TAG, String.format("Unable to load resource 0x%08x from pkg=%s", Integer.valueOf(getResId()), getResPackage()), e2);
                    }
                    break;
                } else {
                    return getResources().getDrawable(getResId(), context.getTheme());
                }
                return null;
            case 3:
                return new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(getDataBytes(), getDataOffset(), getDataLength()));
            case 4:
                Uri uri = getUri();
                String scheme = uri.getScheme();
                if ("content".equals(scheme) || ContentResolver.SCHEME_FILE.equals(scheme)) {
                    try {
                        inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
                    } catch (Exception e3) {
                        Log.w(TAG, "Unable to load image from URI: " + uri, e3);
                        inputStreamOpenInputStream = null;
                    }
                    if (inputStreamOpenInputStream != null) {
                        return new BitmapDrawable(context.getResources(), BitmapFactory.decodeStream(inputStreamOpenInputStream));
                    }
                    break;
                } else {
                    try {
                        inputStreamOpenInputStream = new FileInputStream(new File(this.mString1));
                    } catch (FileNotFoundException e4) {
                        Log.w(TAG, "Unable to load image from path: " + uri, e4);
                        inputStreamOpenInputStream = null;
                    }
                    if (inputStreamOpenInputStream != null) {
                    }
                    break;
                }
                return null;
            case 5:
                return new AdaptiveIconDrawable((Drawable) null, new BitmapDrawable(context.getResources(), getBitmap()));
            default:
                return null;
        }
    }

    public Drawable loadDrawableAsUser(Context context, int i) {
        if (this.mType == 2) {
            String resPackage = getResPackage();
            if (TextUtils.isEmpty(resPackage)) {
                resPackage = context.getPackageName();
            }
            if (getResources() == null && !getResPackage().equals(ZenModeConfig.SYSTEM_AUTHORITY)) {
                try {
                    this.mObj1 = context.getPackageManager().getResourcesForApplicationAsUser(resPackage, i);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, String.format("Unable to find pkg=%s user=%d", getResPackage(), Integer.valueOf(i)), e);
                }
            }
        }
        return loadDrawable(context);
    }

    public void convertToAshmem() {
        if ((this.mType == 1 || this.mType == 5) && getBitmap().isMutable() && getBitmap().getAllocationByteCount() >= 131072) {
            setBitmap(getBitmap().createAshmemBitmap());
        }
    }

    public void writeToStream(OutputStream outputStream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(1);
        dataOutputStream.writeByte(this.mType);
        switch (this.mType) {
            case 1:
            case 5:
                getBitmap().compress(Bitmap.CompressFormat.PNG, 100, dataOutputStream);
                break;
            case 2:
                dataOutputStream.writeUTF(getResPackage());
                dataOutputStream.writeInt(getResId());
                break;
            case 3:
                dataOutputStream.writeInt(getDataLength());
                dataOutputStream.write(getDataBytes(), getDataOffset(), getDataLength());
                break;
            case 4:
                dataOutputStream.writeUTF(getUriString());
                break;
        }
    }

    private Icon(int i) {
        this.mTintMode = DEFAULT_TINT_MODE;
        this.mType = i;
    }

    public static Icon createFromStream(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        if (dataInputStream.readInt() >= 1) {
            switch (dataInputStream.readByte()) {
                case 1:
                    return createWithBitmap(BitmapFactory.decodeStream(dataInputStream));
                case 2:
                    return createWithResource(dataInputStream.readUTF(), dataInputStream.readInt());
                case 3:
                    int i = dataInputStream.readInt();
                    byte[] bArr = new byte[i];
                    dataInputStream.read(bArr, 0, i);
                    return createWithData(bArr, 0, i);
                case 4:
                    return createWithContentUri(dataInputStream.readUTF());
                case 5:
                    return createWithAdaptiveBitmap(BitmapFactory.decodeStream(dataInputStream));
                default:
                    return null;
            }
        }
        return null;
    }

    public boolean sameAs(Icon icon) {
        if (icon == this) {
            return true;
        }
        if (this.mType != icon.getType()) {
            return false;
        }
        switch (this.mType) {
            case 1:
            case 5:
                return getBitmap() == icon.getBitmap();
            case 2:
                return getResId() == icon.getResId() && Objects.equals(getResPackage(), icon.getResPackage());
            case 3:
                return getDataLength() == icon.getDataLength() && getDataOffset() == icon.getDataOffset() && Arrays.equals(getDataBytes(), icon.getDataBytes());
            case 4:
                return Objects.equals(getUriString(), icon.getUriString());
            default:
                return false;
        }
    }

    public static Icon createWithResource(Context context, int i) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null.");
        }
        Icon icon = new Icon(2);
        icon.mInt1 = i;
        icon.mString1 = context.getPackageName();
        return icon;
    }

    public static Icon createWithResource(Resources resources, int i) {
        if (resources == null) {
            throw new IllegalArgumentException("Resource must not be null.");
        }
        Icon icon = new Icon(2);
        icon.mInt1 = i;
        icon.mString1 = resources.getResourcePackageName(i);
        return icon;
    }

    public static Icon createWithResource(String str, int i) {
        if (str == null) {
            throw new IllegalArgumentException("Resource package name must not be null.");
        }
        Icon icon = new Icon(2);
        icon.mInt1 = i;
        icon.mString1 = str;
        return icon;
    }

    public static Icon createWithBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap must not be null.");
        }
        Icon icon = new Icon(1);
        icon.setBitmap(bitmap);
        return icon;
    }

    public static Icon createWithAdaptiveBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap must not be null.");
        }
        Icon icon = new Icon(5);
        icon.setBitmap(bitmap);
        return icon;
    }

    public static Icon createWithData(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new IllegalArgumentException("Data must not be null.");
        }
        Icon icon = new Icon(3);
        icon.mObj1 = bArr;
        icon.mInt1 = i2;
        icon.mInt2 = i;
        return icon;
    }

    public static Icon createWithContentUri(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Uri must not be null.");
        }
        Icon icon = new Icon(4);
        icon.mString1 = str;
        return icon;
    }

    public static Icon createWithContentUri(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri must not be null.");
        }
        Icon icon = new Icon(4);
        icon.mString1 = uri.toString();
        return icon;
    }

    public Icon setTint(int i) {
        return setTintList(ColorStateList.valueOf(i));
    }

    public Icon setTintList(ColorStateList colorStateList) {
        this.mTintList = colorStateList;
        return this;
    }

    public Icon setTintMode(PorterDuff.Mode mode) {
        this.mTintMode = mode;
        return this;
    }

    public boolean hasTint() {
        return (this.mTintList == null && this.mTintMode == DEFAULT_TINT_MODE) ? false : true;
    }

    public static Icon createWithFilePath(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Path must not be null.");
        }
        Icon icon = new Icon(4);
        icon.mString1 = str;
        return icon;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Icon(typ=");
        sb.append(typeToString(this.mType));
        switch (this.mType) {
            case 1:
            case 5:
                sb.append(" size=");
                sb.append(getBitmap().getWidth());
                sb.append("x");
                sb.append(getBitmap().getHeight());
                break;
            case 2:
                sb.append(" pkg=");
                sb.append(getResPackage());
                sb.append(" id=");
                sb.append(String.format("0x%08x", Integer.valueOf(getResId())));
                break;
            case 3:
                sb.append(" len=");
                sb.append(getDataLength());
                if (getDataOffset() != 0) {
                    sb.append(" off=");
                    sb.append(getDataOffset());
                }
                break;
            case 4:
                sb.append(" uri=");
                sb.append(getUriString());
                break;
        }
        if (this.mTintList != null) {
            sb.append(" tint=");
            String str = "";
            for (int i : this.mTintList.getColors()) {
                sb.append(String.format("%s0x%08x", str, Integer.valueOf(i)));
                str = "|";
            }
        }
        if (this.mTintMode != DEFAULT_TINT_MODE) {
            sb.append(" mode=");
            sb.append(this.mTintMode);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return (this.mType == 1 || this.mType == 5 || this.mType == 3) ? 1 : 0;
    }

    private Icon(Parcel parcel) {
        this(parcel.readInt());
        switch (this.mType) {
            case 1:
            case 5:
                this.mObj1 = Bitmap.CREATOR.createFromParcel(parcel);
                break;
            case 2:
                String string = parcel.readString();
                int i = parcel.readInt();
                this.mString1 = string;
                this.mInt1 = i;
                break;
            case 3:
                int i2 = parcel.readInt();
                byte[] blob = parcel.readBlob();
                if (i2 != blob.length) {
                    throw new RuntimeException("internal unparceling error: blob length (" + blob.length + ") != expected length (" + i2 + ")");
                }
                this.mInt1 = i2;
                this.mObj1 = blob;
                break;
                break;
            case 4:
                this.mString1 = parcel.readString();
                break;
            default:
                throw new RuntimeException("invalid " + getClass().getSimpleName() + " type in parcel: " + this.mType);
        }
        if (parcel.readInt() == 1) {
            this.mTintList = ColorStateList.CREATOR.createFromParcel(parcel);
        }
        this.mTintMode = PorterDuff.intToMode(parcel.readInt());
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mType);
        switch (this.mType) {
            case 1:
            case 5:
                getBitmap();
                getBitmap().writeToParcel(parcel, i);
                break;
            case 2:
                parcel.writeString(getResPackage());
                parcel.writeInt(getResId());
                break;
            case 3:
                parcel.writeInt(getDataLength());
                parcel.writeBlob(getDataBytes(), getDataOffset(), getDataLength());
                break;
            case 4:
                parcel.writeString(getUriString());
                break;
        }
        if (this.mTintList == null) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(1);
            this.mTintList.writeToParcel(parcel, i);
        }
        parcel.writeInt(PorterDuff.modeToInt(this.mTintMode));
    }

    public static Bitmap scaleDownIfNecessary(Bitmap bitmap, int i, int i2) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > i || height > i2) {
            float f = width;
            float f2 = height;
            float fMin = Math.min(i / f, i2 / f2);
            return Bitmap.createScaledBitmap(bitmap, Math.max(1, (int) (f * fMin)), Math.max(1, (int) (fMin * f2)), true);
        }
        return bitmap;
    }

    public void scaleDownIfNecessary(int i, int i2) {
        if (this.mType != 1 && this.mType != 5) {
            return;
        }
        setBitmap(scaleDownIfNecessary(getBitmap(), i, i2));
    }

    private class LoadDrawableTask implements Runnable {
        final Context mContext;
        final Message mMessage;

        public LoadDrawableTask(Context context, Handler handler, final OnDrawableLoadedListener onDrawableLoadedListener) {
            this.mContext = context;
            this.mMessage = Message.obtain(handler, new Runnable() {
                @Override
                public void run() {
                    onDrawableLoadedListener.onDrawableLoaded((Drawable) LoadDrawableTask.this.mMessage.obj);
                }
            });
        }

        public LoadDrawableTask(Context context, Message message) {
            this.mContext = context;
            this.mMessage = message;
        }

        @Override
        public void run() {
            this.mMessage.obj = Icon.this.loadDrawable(this.mContext);
            this.mMessage.sendToTarget();
        }

        public void runAsync() {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(this);
        }
    }
}

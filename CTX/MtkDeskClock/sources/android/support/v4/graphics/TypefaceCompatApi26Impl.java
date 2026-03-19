package android.support.v4.graphics;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.fonts.FontVariationAxis;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.content.res.FontResourcesParserCompat;
import android.support.v4.provider.FontsContractCompat;
import android.util.Log;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;

@RequiresApi(26)
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class TypefaceCompatApi26Impl extends TypefaceCompatApi21Impl {
    private static final String ABORT_CREATION_METHOD = "abortCreation";
    private static final String ADD_FONT_FROM_ASSET_MANAGER_METHOD = "addFontFromAssetManager";
    private static final String ADD_FONT_FROM_BUFFER_METHOD = "addFontFromBuffer";
    private static final String CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD = "createFromFamiliesWithDefault";
    private static final String DEFAULT_FAMILY = "sans-serif";
    private static final String FONT_FAMILY_CLASS = "android.graphics.FontFamily";
    private static final String FREEZE_METHOD = "freeze";
    private static final int RESOLVE_BY_FONT_TABLE = -1;
    private static final String TAG = "TypefaceCompatApi26Impl";
    protected final Method mAbortCreation;
    protected final Method mAddFontFromAssetManager;
    protected final Method mAddFontFromBuffer;
    protected final Method mCreateFromFamiliesWithDefault;
    protected final Class mFontFamily;
    protected final Constructor mFontFamilyCtor;
    protected final Method mFreeze;

    public TypefaceCompatApi26Impl() {
        Class fontFamily;
        Constructor fontFamilyCtor;
        Method addFontFromBuffer;
        Method freeze;
        Method abortCreation;
        Method createFromFamiliesWithDefault;
        Method createFromFamiliesWithDefault2;
        try {
            Class fontFamily2 = obtainFontFamily();
            Constructor fontFamilyCtor2 = obtainFontFamilyCtor(fontFamily2);
            Method addFontFromAssetManager = obtainAddFontFromAssetManagerMethod(fontFamily2);
            Method addFontFromBuffer2 = obtainAddFontFromBufferMethod(fontFamily2);
            Method freeze2 = obtainFreezeMethod(fontFamily2);
            Method abortCreation2 = obtainAbortCreationMethod(fontFamily2);
            Method createFromFamiliesWithDefault3 = obtainCreateFromFamiliesWithDefaultMethod(fontFamily2);
            fontFamily = fontFamily2;
            createFromFamiliesWithDefault2 = createFromFamiliesWithDefault3;
            createFromFamiliesWithDefault = abortCreation2;
            abortCreation = freeze2;
            freeze = addFontFromBuffer2;
            addFontFromBuffer = addFontFromAssetManager;
            fontFamilyCtor = fontFamilyCtor2;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Log.e(TAG, "Unable to collect necessary methods for class " + e.getClass().getName(), e);
            fontFamily = null;
            fontFamilyCtor = null;
            addFontFromBuffer = null;
            freeze = null;
            abortCreation = null;
            createFromFamiliesWithDefault = null;
            createFromFamiliesWithDefault2 = null;
        }
        this.mFontFamily = fontFamily;
        this.mFontFamilyCtor = fontFamilyCtor;
        this.mAddFontFromAssetManager = addFontFromBuffer;
        this.mAddFontFromBuffer = freeze;
        this.mFreeze = abortCreation;
        this.mAbortCreation = createFromFamiliesWithDefault;
        this.mCreateFromFamiliesWithDefault = createFromFamiliesWithDefault2;
    }

    private boolean isFontFamilyPrivateAPIAvailable() {
        if (this.mAddFontFromAssetManager == null) {
            Log.w(TAG, "Unable to collect necessary private methods. Fallback to legacy implementation.");
        }
        return this.mAddFontFromAssetManager != null;
    }

    private Object newFamily() {
        try {
            return this.mFontFamilyCtor.newInstance(new Object[0]);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean addFontFromAssetManager(Context context, Object family, String fileName, int ttcIndex, int weight, int style, @Nullable FontVariationAxis[] axes) {
        try {
            Boolean result = (Boolean) this.mAddFontFromAssetManager.invoke(family, context.getAssets(), fileName, 0, false, Integer.valueOf(ttcIndex), Integer.valueOf(weight), Integer.valueOf(style), axes);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean addFontFromBuffer(Object family, ByteBuffer buffer, int ttcIndex, int weight, int style) {
        try {
            Boolean result = (Boolean) this.mAddFontFromBuffer.invoke(family, buffer, Integer.valueOf(ttcIndex), null, Integer.valueOf(weight), Integer.valueOf(style));
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    protected Typeface createFromFamiliesWithDefault(Object family) {
        try {
            Object familyArray = Array.newInstance((Class<?>) this.mFontFamily, 1);
            Array.set(familyArray, 0, family);
            return (Typeface) this.mCreateFromFamiliesWithDefault.invoke(null, familyArray, -1, -1);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean freeze(Object family) {
        try {
            Boolean result = (Boolean) this.mFreeze.invoke(family, new Object[0]);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void abortCreation(Object family) {
        try {
            this.mAbortCreation.invoke(family, new Object[0]);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Typeface createFromFontFamilyFilesResourceEntry(Context context, FontResourcesParserCompat.FontFamilyFilesResourceEntry fontFamilyFilesResourceEntry, Resources resources, int i) {
        if (!isFontFamilyPrivateAPIAvailable()) {
            return super.createFromFontFamilyFilesResourceEntry(context, fontFamilyFilesResourceEntry, resources, i);
        }
        Object objNewFamily = newFamily();
        for (FontResourcesParserCompat.FontFileResourceEntry fontFileResourceEntry : fontFamilyFilesResourceEntry.getEntries()) {
            if (!addFontFromAssetManager(context, objNewFamily, fontFileResourceEntry.getFileName(), fontFileResourceEntry.getTtcIndex(), fontFileResourceEntry.getWeight(), fontFileResourceEntry.isItalic() ? 1 : 0, FontVariationAxis.fromFontVariationSettings(fontFileResourceEntry.getVariationSettings()))) {
                abortCreation(objNewFamily);
                return null;
            }
        }
        if (freeze(objNewFamily)) {
            return createFromFamiliesWithDefault(objNewFamily);
        }
        return null;
    }

    @Override
    public Typeface createFromFontInfo(Context context, @Nullable CancellationSignal cancellationSignal, @NonNull FontsContractCompat.FontInfo[] fontInfoArr, int i) throws Throwable {
        Throwable th;
        Throwable th2;
        if (fontInfoArr.length < 1) {
            return null;
        }
        if (!isFontFamilyPrivateAPIAvailable()) {
            FontsContractCompat.FontInfo fontInfoFindBestInfo = findBestInfo(fontInfoArr, i);
            try {
                ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor = context.getContentResolver().openFileDescriptor(fontInfoFindBestInfo.getUri(), "r", cancellationSignal);
                if (parcelFileDescriptorOpenFileDescriptor != null) {
                    try {
                        Typeface typefaceBuild = new Typeface.Builder(parcelFileDescriptorOpenFileDescriptor.getFileDescriptor()).setWeight(fontInfoFindBestInfo.getWeight()).setItalic(fontInfoFindBestInfo.isItalic()).build();
                        if (parcelFileDescriptorOpenFileDescriptor != null) {
                            parcelFileDescriptorOpenFileDescriptor.close();
                        }
                        return typefaceBuild;
                    } catch (Throwable th3) {
                        th = th3;
                        th2 = null;
                        if (parcelFileDescriptorOpenFileDescriptor != null) {
                        }
                    }
                } else {
                    if (parcelFileDescriptorOpenFileDescriptor != null) {
                        parcelFileDescriptorOpenFileDescriptor.close();
                    }
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        } else {
            Map<Uri, ByteBuffer> mapPrepareFontData = FontsContractCompat.prepareFontData(context, fontInfoArr, cancellationSignal);
            Object objNewFamily = newFamily();
            int length = fontInfoArr.length;
            boolean z = false;
            int i2 = 0;
            while (i2 < length) {
                FontsContractCompat.FontInfo fontInfo = fontInfoArr[i2];
                ByteBuffer byteBuffer = mapPrepareFontData.get(fontInfo.getUri());
                if (byteBuffer != null) {
                    if (addFontFromBuffer(objNewFamily, byteBuffer, fontInfo.getTtcIndex(), fontInfo.getWeight(), fontInfo.isItalic() ? 1 : 0)) {
                        z = true;
                    } else {
                        abortCreation(objNewFamily);
                        return null;
                    }
                }
                i2++;
                z = z;
            }
            if (!z) {
                abortCreation(objNewFamily);
                return null;
            }
            if (freeze(objNewFamily)) {
                return Typeface.create(createFromFamiliesWithDefault(objNewFamily), i);
            }
            return null;
        }
    }

    @Override
    @Nullable
    public Typeface createFromResourcesFontFile(Context context, Resources resources, int id, String path, int style) {
        if (!isFontFamilyPrivateAPIAvailable()) {
            return super.createFromResourcesFontFile(context, resources, id, path, style);
        }
        Object fontFamily = newFamily();
        if (!addFontFromAssetManager(context, fontFamily, path, 0, -1, -1, null)) {
            abortCreation(fontFamily);
            return null;
        }
        if (freeze(fontFamily)) {
            return createFromFamiliesWithDefault(fontFamily);
        }
        return null;
    }

    protected Class obtainFontFamily() throws ClassNotFoundException {
        return Class.forName(FONT_FAMILY_CLASS);
    }

    protected Constructor obtainFontFamilyCtor(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getConstructor(new Class[0]);
    }

    protected Method obtainAddFontFromAssetManagerMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(ADD_FONT_FROM_ASSET_MANAGER_METHOD, AssetManager.class, String.class, Integer.TYPE, Boolean.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, FontVariationAxis[].class);
    }

    protected Method obtainAddFontFromBufferMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(ADD_FONT_FROM_BUFFER_METHOD, ByteBuffer.class, Integer.TYPE, FontVariationAxis[].class, Integer.TYPE, Integer.TYPE);
    }

    protected Method obtainFreezeMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(FREEZE_METHOD, new Class[0]);
    }

    protected Method obtainAbortCreationMethod(Class fontFamily) throws NoSuchMethodException {
        return fontFamily.getMethod(ABORT_CREATION_METHOD, new Class[0]);
    }

    protected Method obtainCreateFromFamiliesWithDefaultMethod(Class fontFamily) throws NoSuchMethodException {
        Object familyArray = Array.newInstance((Class<?>) fontFamily, 1);
        Method m = Typeface.class.getDeclaredMethod(CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD, familyArray.getClass(), Integer.TYPE, Integer.TYPE);
        m.setAccessible(true);
        return m;
    }
}

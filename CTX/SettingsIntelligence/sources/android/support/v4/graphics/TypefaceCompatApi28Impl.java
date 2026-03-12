package android.support.v4.graphics;

import android.graphics.Typeface;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TypefaceCompatApi28Impl extends TypefaceCompatApi26Impl {
    @Override
    protected Typeface createFromFamiliesWithDefault(Object family) {
        try {
            Object familyArray = Array.newInstance((Class<?>) this.mFontFamily, 1);
            Array.set(familyArray, 0, family);
            return (Typeface) this.mCreateFromFamiliesWithDefault.invoke(null, familyArray, "sans-serif", -1, -1);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Method obtainCreateFromFamiliesWithDefaultMethod(Class fontFamily) throws NoSuchMethodException {
        Object familyArray = Array.newInstance((Class<?>) fontFamily, 1);
        Method m = Typeface.class.getDeclaredMethod("createFromFamiliesWithDefault", familyArray.getClass(), String.class, Integer.TYPE, Integer.TYPE);
        m.setAccessible(true);
        return m;
    }
}

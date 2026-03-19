package android.support.design.resources;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v7.content.res.AppCompatResources;

public class MaterialResources {
    public static ColorStateList getColorStateList(Context context, TypedArray attributes, int index) {
        int resourceId;
        ColorStateList value;
        if (attributes.hasValue(index) && (resourceId = attributes.getResourceId(index, 0)) != 0 && (value = AppCompatResources.getColorStateList(context, resourceId)) != null) {
            return value;
        }
        return attributes.getColorStateList(index);
    }

    public static Drawable getDrawable(Context context, TypedArray attributes, int index) {
        int resourceId;
        Drawable value;
        if (attributes.hasValue(index) && (resourceId = attributes.getResourceId(index, 0)) != 0 && (value = AppCompatResources.getDrawable(context, resourceId)) != null) {
            return value;
        }
        return attributes.getDrawable(index);
    }

    public static TextAppearance getTextAppearance(Context context, TypedArray attributes, int index) {
        int resourceId;
        if (attributes.hasValue(index) && (resourceId = attributes.getResourceId(index, 0)) != 0) {
            return new TextAppearance(context, resourceId);
        }
        return null;
    }

    static int getIndexWithValue(TypedArray attributes, int a, int b) {
        if (attributes.hasValue(a)) {
            return a;
        }
        return b;
    }
}

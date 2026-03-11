package android.support.v7.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.mediarouter.R;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;

final class MediaRouterThemeHelper {
    static Context createThemedButtonContext(Context context) {
        Context context2 = new ContextThemeWrapper(context, getRouterThemeId(context));
        int style = getThemeResource(context2, R.attr.mediaRouteTheme);
        return style != 0 ? new ContextThemeWrapper(context2, style) : context2;
    }

    static Context createThemedDialogContext(Context context, int theme, boolean alertDialog) {
        if (theme == 0) {
            theme = getThemeResource(context, !alertDialog ? android.support.v7.appcompat.R.attr.dialogTheme : android.support.v7.appcompat.R.attr.alertDialogTheme);
        }
        Context context2 = new ContextThemeWrapper(context, theme);
        return getThemeResource(context2, R.attr.mediaRouteTheme) != 0 ? new ContextThemeWrapper(context2, getRouterThemeId(context2)) : context2;
    }

    static int createThemedDialogStyle(Context context) {
        int theme = getThemeResource(context, R.attr.mediaRouteTheme);
        if (theme == 0) {
            return getRouterThemeId(context);
        }
        return theme;
    }

    static int getThemeResource(Context context, int attr) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, value, true)) {
            return value.resourceId;
        }
        return 0;
    }

    static float getDisabledAlpha(Context context) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, value, true)) {
            return value.getFloat();
        }
        return 0.5f;
    }

    static int getControllerColor(Context context, int style) {
        int primaryColor = getThemeColor(context, style, android.support.v7.appcompat.R.attr.colorPrimary);
        if (ColorUtils.calculateContrast(-1, primaryColor) >= 3.0d) {
            return -1;
        }
        return -570425344;
    }

    static int getButtonTextColor(Context context) {
        int primaryColor = getThemeColor(context, 0, android.support.v7.appcompat.R.attr.colorPrimary);
        int backgroundColor = getThemeColor(context, 0, android.R.attr.colorBackground);
        if (ColorUtils.calculateContrast(primaryColor, backgroundColor) < 3.0d) {
            return getThemeColor(context, 0, android.support.v7.appcompat.R.attr.colorAccent);
        }
        return primaryColor;
    }

    static void setMediaControlsBackgroundColor(Context context, View mainControls, View groupControls, boolean hasGroup) {
        int primaryColor = getThemeColor(context, 0, android.support.v7.appcompat.R.attr.colorPrimary);
        int primaryDarkColor = getThemeColor(context, 0, android.support.v7.appcompat.R.attr.colorPrimaryDark);
        if (hasGroup && getControllerColor(context, 0) == -570425344) {
            primaryDarkColor = primaryColor;
            primaryColor = -1;
        }
        mainControls.setBackgroundColor(primaryColor);
        groupControls.setBackgroundColor(primaryDarkColor);
        mainControls.setTag(Integer.valueOf(primaryColor));
        groupControls.setTag(Integer.valueOf(primaryDarkColor));
    }

    static void setVolumeSliderColor(Context context, MediaRouteVolumeSlider volumeSlider, View backgroundView) {
        int controllerColor = getControllerColor(context, 0);
        if (Color.alpha(controllerColor) != 255) {
            int backgroundColor = ((Integer) backgroundView.getTag()).intValue();
            controllerColor = ColorUtils.compositeColors(controllerColor, backgroundColor);
        }
        volumeSlider.setColor(controllerColor);
    }

    private static boolean isLightTheme(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.isLightTheme, value, true) && value.data != 0;
    }

    private static int getThemeColor(Context context, int style, int attr) {
        if (style != 0) {
            int[] attrs = {attr};
            TypedArray ta = context.obtainStyledAttributes(style, attrs);
            int color = ta.getColor(0, 0);
            ta.recycle();
            if (color != 0) {
                return color;
            }
        }
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        if (value.resourceId != 0) {
            return context.getResources().getColor(value.resourceId);
        }
        return value.data;
    }

    private static int getRouterThemeId(Context context) {
        if (isLightTheme(context)) {
            if (getControllerColor(context, 0) == -570425344) {
                int themeId = R.style.Theme_MediaRouter_Light;
                return themeId;
            }
            int themeId2 = R.style.Theme_MediaRouter_Light_DarkControlPanel;
            return themeId2;
        }
        if (getControllerColor(context, 0) == -570425344) {
            int themeId3 = R.style.Theme_MediaRouter_LightControlPanel;
            return themeId3;
        }
        int themeId4 = R.style.Theme_MediaRouter;
        return themeId4;
    }
}

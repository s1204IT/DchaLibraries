package android.support.v4.text;

import android.os.Build;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import java.util.Locale;

public final class TextUtilsCompat {
    private static final Locale ROOT = new Locale("", "");

    public static int getLayoutDirectionFromLocale(Locale locale) {
        if (Build.VERSION.SDK_INT >= 17) {
            return TextUtils.getLayoutDirectionFromLocale(locale);
        }
        if (locale != null && !locale.equals(ROOT)) {
            String scriptSubtag = ICUCompat.maximizeAndGetScript(locale);
            if (scriptSubtag == null) {
                return getLayoutDirectionFromFirstChar(locale);
            }
            if (scriptSubtag.equalsIgnoreCase("Arab") || scriptSubtag.equalsIgnoreCase("Hebr")) {
                return 1;
            }
            return 0;
        }
        return 0;
    }

    private static int getLayoutDirectionFromFirstChar(Locale locale) {
        switch (Character.getDirectionality(locale.getDisplayName(locale).charAt(0))) {
            case DialogFragment.STYLE_NO_TITLE:
            case DialogFragment.STYLE_NO_FRAME:
                return 1;
            default:
                return 0;
        }
    }
}

package android.text;

import android.icu.lang.UCharacter;
import android.mtp.MtpConstants;

public class Emoji {
    public static int COMBINING_ENCLOSING_KEYCAP = 8419;
    public static int ZERO_WIDTH_JOINER = MtpConstants.RESPONSE_OBJECT_WRITE_PROTECTED;
    public static int VARIATION_SELECTOR_16 = 65039;
    public static int CANCEL_TAG = 917631;

    public static boolean isRegionalIndicatorSymbol(int i) {
        return 127462 <= i && i <= 127487;
    }

    public static boolean isEmojiModifier(int i) {
        return UCharacter.hasBinaryProperty(i, 59);
    }

    public static boolean isEmojiModifierBase(int i) {
        if (i == 129309 || i == 129340) {
            return true;
        }
        if ((129461 <= i && i <= 129462) || (129464 <= i && i <= 129465)) {
            return true;
        }
        return UCharacter.hasBinaryProperty(i, 60);
    }

    public static boolean isNewEmoji(int i) {
        if (i < 128761 || i > 129535) {
            return false;
        }
        return i == 9823 || i == 9854 || i == 128761 || i == 129402 || (129357 <= i && i <= 129359) || ((129388 <= i && i <= 129392) || ((129395 <= i && i <= 129398) || ((129404 <= i && i <= 129407) || ((129432 <= i && i <= 129442) || ((129456 <= i && i <= 129465) || ((129473 <= i && i <= 129474) || (129511 <= i && i <= 129535)))))));
    }

    public static boolean isEmoji(int i) {
        return isNewEmoji(i) || UCharacter.hasBinaryProperty(i, 57);
    }

    public static boolean isKeycapBase(int i) {
        return (48 <= i && i <= 57) || i == 35 || i == 42;
    }

    public static boolean isTagSpecChar(int i) {
        return 917536 <= i && i <= 917630;
    }
}

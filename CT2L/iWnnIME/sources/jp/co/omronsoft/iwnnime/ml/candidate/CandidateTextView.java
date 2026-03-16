package jp.co.omronsoft.iwnnime.ml.candidate;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.Styled;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import java.util.HashMap;
import jp.co.omronsoft.android.text.EmojiDrawable;
import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.KeyboardManager;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnAccessibility;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;

public class CandidateTextView extends TextView {
    private IWnnIME mWnn;
    private static Paint sTmpPaint = new Paint();
    private static TextPaint sTmpTextPaint = new TextPaint();
    private static Paint.FontMetricsInt sTmpFontMetricsInt = new Paint.FontMetricsInt();
    private static final HashMap<Integer, Integer> COMPOSITE_UNICODE_TABLE = new HashMap<Integer, Integer>() {
        {
            put(127471, 127477);
            put(127482, 127480);
            put(127467, 127479);
            put(127465, 127466);
            put(127470, 127481);
            put(127468, 127463);
            put(127466, 127480);
            put(127479, 127482);
            put(127464, 127475);
            put(127472, 127479);
            put(35, 8419);
            put(49, 8419);
            put(50, 8419);
            put(51, 8419);
            put(52, 8419);
            put(53, 8419);
            put(54, 8419);
            put(55, 8419);
            put(56, 8419);
            put(57, 8419);
            put(48, 8419);
        }
    };

    public CandidateTextView(Context context) {
        super(context);
        this.mWnn = null;
        setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER);
    }

    @Override
    public void setBackgroundDrawable(Drawable d) {
        super.setBackgroundDrawable(d);
        Resources res = getContext().getResources();
        setPadding(res.getDimensionPixelSize(R.dimen.candidate_default_padding_left), res.getDimensionPixelSize(R.dimen.candidate_default_padding_top), res.getDimensionPixelSize(R.dimen.candidate_default_padding_right), res.getDimensionPixelSize(R.dimen.candidate_default_padding_bottom));
    }

    public void displayCandidateDialog(Dialog builder) {
        Point listPos;
        if (this.mWnn != null) {
            Window window = builder.getWindow();
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.type = 1003;
            lp.token = getWindowToken();
            if (this.mWnn.isHwCandWindow()) {
                lp.gravity = 51;
                CandidatesManager candMan = this.mWnn.getCurrentCandidatesManager();
                if (candMan != null && (listPos = candMan.getCandidateListPos()) != null) {
                    lp.x = listPos.x;
                    lp.y = listPos.y;
                } else {
                    return;
                }
            } else {
                lp.gravity = 49;
                KeyboardManager km = this.mWnn.getCurrentKeyboardManager();
                Resources res = this.mWnn.getResources();
                Point size = km.getKeyboardSize(true);
                Point pos = km.getKeyboardPosition();
                int topPos = km.getImeTopPosition(pos.y, true, true);
                int offset = res.getDimensionPixelSize(R.dimen.cand_minimum_height);
                DisplayMetrics dm = res.getDisplayMetrics();
                lp.x = (pos.x / 2) - (((dm.widthPixels - pos.x) - size.x) / 2);
                lp.y = topPos - offset;
            }
            window.setAttributes(lp);
            WnnUtility.addFlagsForDialog(this.mWnn, window);
            builder.show();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != 0) {
            CandidatesManager candMan = this.mWnn.getCurrentCandidatesManager();
            candMan.closeDialogCheck();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        DefaultSoftKeyboard softKeyboard;
        if (this.mWnn == null || (softKeyboard = this.mWnn.getCurrentDefaultSoftKeyboard()) == null || !softKeyboard.isPopupKeyboard().booleanValue()) {
            return super.onTouchEvent(me);
        }
        softKeyboard.closePopupKeyboard();
        return true;
    }

    public void setIWnnIME(IWnnIME wnn) {
        this.mWnn = wnn;
    }

    public static int getTextWidths(CharSequence text, TextPaint paint) {
        int result;
        int second;
        if (text == null || paint == null) {
            return 0;
        }
        int length = text.length();
        float[] charWidths = new float[length];
        if (text instanceof Spanned) {
            synchronized (sTmpTextPaint) {
                result = Styled.getTextWidths(paint, sTmpTextPaint, (Spanned) text, 0, text.length(), charWidths, sTmpFontMetricsInt);
            }
        } else {
            result = paint.getTextWidths(text.toString(), charWidths);
        }
        sTmpPaint.setTextSize(paint.getTextSize());
        sTmpPaint.setTextScaleX(paint.getTextScaleX());
        String str = text.toString();
        int i = 0;
        while (i < length) {
            int first = str.codePointAt(i);
            int cpCount = Character.charCount(first);
            if (i < length - cpCount) {
                second = str.codePointAt(i + cpCount);
            } else {
                second = 0;
            }
            if (isCompositeUnicode(first, second)) {
                charWidths[i + cpCount] = 0.0f;
                i = i + cpCount + Character.charCount(second);
            } else {
                char high = text.charAt(i);
                if (Character.isHighSurrogate(high)) {
                    char low = text.charAt(i + 1);
                    if (Character.isLowSurrogate(low)) {
                        int codePoint = Character.toCodePoint(high, low);
                        if (EmojiDrawable.isEmoji(codePoint)) {
                            charWidths[i] = EmojiDrawable.getEmojiWidth(codePoint, sTmpPaint);
                        }
                        i++;
                    }
                }
            }
            i++;
        }
        for (float f : charWidths) {
            result = (int) (result + f);
        }
        return result;
    }

    private static boolean isCompositeUnicode(int first, int second) {
        if (!COMPOSITE_UNICODE_TABLE.containsKey(Integer.valueOf(first)) || COMPOSITE_UNICODE_TABLE.get(Integer.valueOf(first)).intValue() != second) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return false;
    }
}

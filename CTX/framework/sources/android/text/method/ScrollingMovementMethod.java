package android.text.method;

import android.text.Layout;
import android.text.Spannable;
import android.view.MotionEvent;
import android.widget.TextView;

public class ScrollingMovementMethod extends BaseMovementMethod implements MovementMethod {
    private static ScrollingMovementMethod sInstance;

    @Override
    protected boolean left(TextView textView, Spannable spannable) {
        return scrollLeft(textView, spannable, 1);
    }

    @Override
    protected boolean right(TextView textView, Spannable spannable) {
        return scrollRight(textView, spannable, 1);
    }

    @Override
    protected boolean up(TextView textView, Spannable spannable) {
        return scrollUp(textView, spannable, 1);
    }

    @Override
    protected boolean down(TextView textView, Spannable spannable) {
        return scrollDown(textView, spannable, 1);
    }

    @Override
    protected boolean pageUp(TextView textView, Spannable spannable) {
        return scrollPageUp(textView, spannable);
    }

    @Override
    protected boolean pageDown(TextView textView, Spannable spannable) {
        return scrollPageDown(textView, spannable);
    }

    @Override
    protected boolean top(TextView textView, Spannable spannable) {
        return scrollTop(textView, spannable);
    }

    @Override
    protected boolean bottom(TextView textView, Spannable spannable) {
        return scrollBottom(textView, spannable);
    }

    @Override
    protected boolean lineStart(TextView textView, Spannable spannable) {
        return scrollLineStart(textView, spannable);
    }

    @Override
    protected boolean lineEnd(TextView textView, Spannable spannable) {
        return scrollLineEnd(textView, spannable);
    }

    @Override
    protected boolean home(TextView textView, Spannable spannable) {
        return top(textView, spannable);
    }

    @Override
    protected boolean end(TextView textView, Spannable spannable) {
        return bottom(textView, spannable);
    }

    @Override
    public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent motionEvent) {
        return Touch.onTouchEvent(textView, spannable, motionEvent);
    }

    @Override
    public void onTakeFocus(TextView textView, Spannable spannable, int i) {
        Layout layout = textView.getLayout();
        if (layout != null && (i & 2) != 0) {
            textView.scrollTo(textView.getScrollX(), layout.getLineTop(0));
        }
        if (layout != null && (i & 1) != 0) {
            textView.scrollTo(textView.getScrollX(), layout.getLineTop((layout.getLineCount() - 1) + 1) - (textView.getHeight() - (textView.getTotalPaddingTop() + textView.getTotalPaddingBottom())));
        }
    }

    public static MovementMethod getInstance() {
        if (sInstance == null) {
            sInstance = new ScrollingMovementMethod();
        }
        return sInstance;
    }
}

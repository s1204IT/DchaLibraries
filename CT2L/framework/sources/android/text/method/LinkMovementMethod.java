package android.text.method;

import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

public class LinkMovementMethod extends ScrollingMovementMethod {
    private static final int CLICK = 1;
    private static final int DOWN = 3;
    private static Object FROM_BELOW = new NoCopySpan.Concrete();
    private static final int UP = 2;
    private static LinkMovementMethod sInstance;

    @Override
    public boolean canSelectArbitrarily() {
        return true;
    }

    @Override
    protected boolean handleMovementKey(TextView widget, Spannable buffer, int keyCode, int movementMetaState, KeyEvent event) {
        switch (keyCode) {
            case 23:
            case 66:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState) && event.getAction() == 0 && event.getRepeatCount() == 0 && action(1, widget, buffer)) {
                    return true;
                }
                break;
        }
        return super.handleMovementKey(widget, buffer, keyCode, movementMetaState, event);
    }

    @Override
    protected boolean up(TextView widget, Spannable buffer) {
        if (action(2, widget, buffer)) {
            return true;
        }
        return super.up(widget, buffer);
    }

    @Override
    protected boolean down(TextView widget, Spannable buffer) {
        if (action(3, widget, buffer)) {
            return true;
        }
        return super.down(widget, buffer);
    }

    @Override
    protected boolean left(TextView widget, Spannable buffer) {
        if (action(2, widget, buffer)) {
            return true;
        }
        return super.left(widget, buffer);
    }

    @Override
    protected boolean right(TextView widget, Spannable buffer) {
        if (action(3, widget, buffer)) {
            return true;
        }
        return super.right(widget, buffer);
    }

    private boolean action(int what, TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();
        int padding = widget.getTotalPaddingTop() + widget.getTotalPaddingBottom();
        int areatop = widget.getScrollY();
        int areabot = (widget.getHeight() + areatop) - padding;
        int linetop = layout.getLineForVertical(areatop);
        int linebot = layout.getLineForVertical(areabot);
        int first = layout.getLineStart(linetop);
        int last = layout.getLineEnd(linebot);
        ClickableSpan[] candidates = (ClickableSpan[]) buffer.getSpans(first, last, ClickableSpan.class);
        int a = Selection.getSelectionStart(buffer);
        int b = Selection.getSelectionEnd(buffer);
        int selStart = Math.min(a, b);
        int selEnd = Math.max(a, b);
        if (selStart < 0 && buffer.getSpanStart(FROM_BELOW) >= 0) {
            selEnd = buffer.length();
            selStart = selEnd;
        }
        if (selStart > last) {
            selEnd = Integer.MAX_VALUE;
            selStart = Integer.MAX_VALUE;
        }
        if (selEnd < first) {
            selEnd = -1;
            selStart = -1;
        }
        switch (what) {
            case 1:
                if (selStart == selEnd) {
                    return false;
                }
                ClickableSpan[] link = (ClickableSpan[]) buffer.getSpans(selStart, selEnd, ClickableSpan.class);
                if (link.length != 1) {
                    return false;
                }
                link[0].onClick(widget);
                return false;
            case 2:
                int beststart = -1;
                int bestend = -1;
                for (int i = 0; i < candidates.length; i++) {
                    int end = buffer.getSpanEnd(candidates[i]);
                    if ((end < selEnd || selStart == selEnd) && end > bestend) {
                        beststart = buffer.getSpanStart(candidates[i]);
                        bestend = end;
                    }
                }
                if (beststart >= 0) {
                    Selection.setSelection(buffer, bestend, beststart);
                    return true;
                }
                return false;
            case 3:
                int beststart2 = Integer.MAX_VALUE;
                int bestend2 = Integer.MAX_VALUE;
                for (int i2 = 0; i2 < candidates.length; i2++) {
                    int start = buffer.getSpanStart(candidates[i2]);
                    if ((start > selStart || selStart == selEnd) && start < beststart2) {
                        beststart2 = start;
                        bestend2 = buffer.getSpanEnd(candidates[i2]);
                    }
                }
                if (bestend2 < Integer.MAX_VALUE) {
                    Selection.setSelection(buffer, beststart2, bestend2);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();
        if (action == 1 || action == 0) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            int x2 = x - widget.getTotalPaddingLeft();
            int y2 = y - widget.getTotalPaddingTop();
            int x3 = x2 + widget.getScrollX();
            int y3 = y2 + widget.getScrollY();
            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y3);
            int off = layout.getOffsetForHorizontal(line, x3);
            ClickableSpan[] link = (ClickableSpan[]) buffer.getSpans(off, off, ClickableSpan.class);
            if (link.length != 0) {
                if (action == 1) {
                    link[0].onClick(widget);
                    return true;
                }
                if (action != 0) {
                    return true;
                }
                Selection.setSelection(buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]));
                return true;
            }
            Selection.removeSelection(buffer);
        }
        return super.onTouchEvent(widget, buffer, event);
    }

    @Override
    public void initialize(TextView widget, Spannable text) {
        Selection.removeSelection(text);
        text.removeSpan(FROM_BELOW);
    }

    @Override
    public void onTakeFocus(TextView view, Spannable text, int dir) {
        Selection.removeSelection(text);
        if ((dir & 1) != 0) {
            text.setSpan(FROM_BELOW, 0, 0, 34);
        } else {
            text.removeSpan(FROM_BELOW);
        }
    }

    public static MovementMethod getInstance() {
        if (sInstance == null) {
            sInstance = new LinkMovementMethod();
        }
        return sInstance;
    }
}

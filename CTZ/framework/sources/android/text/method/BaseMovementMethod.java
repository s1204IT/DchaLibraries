package android.text.method;

import android.text.Layout;
import android.text.Spannable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

public class BaseMovementMethod implements MovementMethod {
    @Override
    public boolean canSelectArbitrarily() {
        return false;
    }

    @Override
    public void initialize(TextView textView, Spannable spannable) {
    }

    @Override
    public boolean onKeyDown(TextView textView, Spannable spannable, int i, KeyEvent keyEvent) {
        boolean zHandleMovementKey = handleMovementKey(textView, spannable, i, getMovementMetaState(spannable, keyEvent), keyEvent);
        if (zHandleMovementKey) {
            MetaKeyKeyListener.adjustMetaAfterKeypress(spannable);
            MetaKeyKeyListener.resetLockedMeta(spannable);
        }
        return zHandleMovementKey;
    }

    @Override
    public boolean onKeyOther(TextView textView, Spannable spannable, KeyEvent keyEvent) {
        int movementMetaState = getMovementMetaState(spannable, keyEvent);
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == 0 || keyEvent.getAction() != 2) {
            return false;
        }
        int repeatCount = keyEvent.getRepeatCount();
        int i = 0;
        boolean z = false;
        while (i < repeatCount && handleMovementKey(textView, spannable, keyCode, movementMetaState, keyEvent)) {
            i++;
            z = true;
        }
        if (z) {
            MetaKeyKeyListener.adjustMetaAfterKeypress(spannable);
            MetaKeyKeyListener.resetLockedMeta(spannable);
        }
        return z;
    }

    @Override
    public boolean onKeyUp(TextView textView, Spannable spannable, int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void onTakeFocus(TextView textView, Spannable spannable, int i) {
    }

    @Override
    public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onTrackballEvent(TextView textView, Spannable spannable, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(TextView textView, Spannable spannable, MotionEvent motionEvent) {
        float f;
        float axisValue;
        boolean zScrollRight = false;
        if ((motionEvent.getSource() & 2) == 0 || motionEvent.getAction() != 8) {
            return false;
        }
        if ((motionEvent.getMetaState() & 1) != 0) {
            axisValue = motionEvent.getAxisValue(9);
            f = 0.0f;
        } else {
            f = -motionEvent.getAxisValue(9);
            axisValue = motionEvent.getAxisValue(10);
        }
        if (axisValue < 0.0f) {
            zScrollRight = false | scrollLeft(textView, spannable, (int) Math.ceil(-axisValue));
        } else if (axisValue > 0.0f) {
            zScrollRight = false | scrollRight(textView, spannable, (int) Math.ceil(axisValue));
        }
        if (f < 0.0f) {
            return zScrollRight | scrollUp(textView, spannable, (int) Math.ceil(-f));
        }
        if (f > 0.0f) {
            return zScrollRight | scrollDown(textView, spannable, (int) Math.ceil(f));
        }
        return zScrollRight;
    }

    protected int getMovementMetaState(Spannable spannable, KeyEvent keyEvent) {
        return KeyEvent.normalizeMetaState(MetaKeyKeyListener.getMetaState(spannable, keyEvent) & (-1537)) & (-194);
    }

    protected boolean handleMovementKey(TextView textView, Spannable spannable, int i, int i2, KeyEvent keyEvent) {
        switch (i) {
            case 19:
                if (KeyEvent.metaStateHasNoModifiers(i2)) {
                    return up(textView, spannable);
                }
                if (KeyEvent.metaStateHasModifiers(i2, 2)) {
                    return top(textView, spannable);
                }
                return false;
            case 20:
                if (KeyEvent.metaStateHasNoModifiers(i2)) {
                    return down(textView, spannable);
                }
                if (KeyEvent.metaStateHasModifiers(i2, 2)) {
                    return bottom(textView, spannable);
                }
                return false;
            case 21:
                if (KeyEvent.metaStateHasNoModifiers(i2)) {
                    return left(textView, spannable);
                }
                if (KeyEvent.metaStateHasModifiers(i2, 4096)) {
                    return leftWord(textView, spannable);
                }
                if (KeyEvent.metaStateHasModifiers(i2, 2)) {
                    return lineStart(textView, spannable);
                }
                return false;
            case 22:
                if (KeyEvent.metaStateHasNoModifiers(i2)) {
                    return right(textView, spannable);
                }
                if (KeyEvent.metaStateHasModifiers(i2, 4096)) {
                    return rightWord(textView, spannable);
                }
                if (KeyEvent.metaStateHasModifiers(i2, 2)) {
                    return lineEnd(textView, spannable);
                }
                return false;
            case 92:
                if (KeyEvent.metaStateHasNoModifiers(i2)) {
                    return pageUp(textView, spannable);
                }
                if (KeyEvent.metaStateHasModifiers(i2, 2)) {
                    return top(textView, spannable);
                }
                return false;
            case 93:
                if (KeyEvent.metaStateHasNoModifiers(i2)) {
                    return pageDown(textView, spannable);
                }
                if (KeyEvent.metaStateHasModifiers(i2, 2)) {
                    return bottom(textView, spannable);
                }
                return false;
            case 122:
                if (KeyEvent.metaStateHasNoModifiers(i2)) {
                    return home(textView, spannable);
                }
                if (KeyEvent.metaStateHasModifiers(i2, 4096)) {
                    return top(textView, spannable);
                }
                return false;
            case 123:
                if (KeyEvent.metaStateHasNoModifiers(i2)) {
                    return end(textView, spannable);
                }
                if (KeyEvent.metaStateHasModifiers(i2, 4096)) {
                    return bottom(textView, spannable);
                }
                return false;
            default:
                return false;
        }
    }

    protected boolean left(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean right(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean up(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean down(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean pageUp(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean pageDown(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean top(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean bottom(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean lineStart(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean lineEnd(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean leftWord(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean rightWord(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean home(TextView textView, Spannable spannable) {
        return false;
    }

    protected boolean end(TextView textView, Spannable spannable) {
        return false;
    }

    private int getTopLine(TextView textView) {
        return textView.getLayout().getLineForVertical(textView.getScrollY());
    }

    private int getBottomLine(TextView textView) {
        return textView.getLayout().getLineForVertical(textView.getScrollY() + getInnerHeight(textView));
    }

    private int getInnerWidth(TextView textView) {
        return (textView.getWidth() - textView.getTotalPaddingLeft()) - textView.getTotalPaddingRight();
    }

    private int getInnerHeight(TextView textView) {
        return (textView.getHeight() - textView.getTotalPaddingTop()) - textView.getTotalPaddingBottom();
    }

    private int getCharacterWidth(TextView textView) {
        return (int) Math.ceil(textView.getPaint().getFontSpacing());
    }

    private int getScrollBoundsLeft(TextView textView) {
        Layout layout = textView.getLayout();
        int topLine = getTopLine(textView);
        int bottomLine = getBottomLine(textView);
        if (topLine > bottomLine) {
            return 0;
        }
        int i = Integer.MAX_VALUE;
        while (topLine <= bottomLine) {
            int iFloor = (int) Math.floor(layout.getLineLeft(topLine));
            if (iFloor < i) {
                i = iFloor;
            }
            topLine++;
        }
        return i;
    }

    private int getScrollBoundsRight(TextView textView) {
        Layout layout = textView.getLayout();
        int topLine = getTopLine(textView);
        int bottomLine = getBottomLine(textView);
        if (topLine > bottomLine) {
            return 0;
        }
        int i = Integer.MIN_VALUE;
        while (topLine <= bottomLine) {
            int iCeil = (int) Math.ceil(layout.getLineRight(topLine));
            if (iCeil > i) {
                i = iCeil;
            }
            topLine++;
        }
        return i;
    }

    protected boolean scrollLeft(TextView textView, Spannable spannable, int i) {
        int scrollBoundsLeft = getScrollBoundsLeft(textView);
        int scrollX = textView.getScrollX();
        if (scrollX > scrollBoundsLeft) {
            textView.scrollTo(Math.max(scrollX - (getCharacterWidth(textView) * i), scrollBoundsLeft), textView.getScrollY());
            return true;
        }
        return false;
    }

    protected boolean scrollRight(TextView textView, Spannable spannable, int i) {
        int scrollBoundsRight = getScrollBoundsRight(textView) - getInnerWidth(textView);
        int scrollX = textView.getScrollX();
        if (scrollX < scrollBoundsRight) {
            textView.scrollTo(Math.min(scrollX + (getCharacterWidth(textView) * i), scrollBoundsRight), textView.getScrollY());
            return true;
        }
        return false;
    }

    protected boolean scrollUp(TextView textView, Spannable spannable, int i) {
        Layout layout = textView.getLayout();
        int scrollY = textView.getScrollY();
        int lineForVertical = layout.getLineForVertical(scrollY);
        if (layout.getLineTop(lineForVertical) == scrollY) {
            lineForVertical--;
        }
        if (lineForVertical >= 0) {
            Touch.scrollTo(textView, layout, textView.getScrollX(), layout.getLineTop(Math.max((lineForVertical - i) + 1, 0)));
            return true;
        }
        return false;
    }

    protected boolean scrollDown(TextView textView, Spannable spannable, int i) {
        Layout layout = textView.getLayout();
        int innerHeight = getInnerHeight(textView);
        int scrollY = textView.getScrollY() + innerHeight;
        int lineForVertical = layout.getLineForVertical(scrollY);
        int i2 = lineForVertical + 1;
        if (layout.getLineTop(i2) < scrollY + 1) {
            lineForVertical = i2;
        }
        int lineCount = layout.getLineCount() - 1;
        if (lineForVertical <= lineCount) {
            Touch.scrollTo(textView, layout, textView.getScrollX(), layout.getLineTop(Math.min((lineForVertical + i) - 1, lineCount) + 1) - innerHeight);
            return true;
        }
        return false;
    }

    protected boolean scrollPageUp(TextView textView, Spannable spannable) {
        Layout layout = textView.getLayout();
        int lineForVertical = layout.getLineForVertical(textView.getScrollY() - getInnerHeight(textView));
        if (lineForVertical >= 0) {
            Touch.scrollTo(textView, layout, textView.getScrollX(), layout.getLineTop(lineForVertical));
            return true;
        }
        return false;
    }

    protected boolean scrollPageDown(TextView textView, Spannable spannable) {
        Layout layout = textView.getLayout();
        int innerHeight = getInnerHeight(textView);
        int lineForVertical = layout.getLineForVertical(textView.getScrollY() + innerHeight + innerHeight);
        if (lineForVertical <= layout.getLineCount() - 1) {
            Touch.scrollTo(textView, layout, textView.getScrollX(), layout.getLineTop(lineForVertical + 1) - innerHeight);
            return true;
        }
        return false;
    }

    protected boolean scrollTop(TextView textView, Spannable spannable) {
        Layout layout = textView.getLayout();
        if (getTopLine(textView) < 0) {
            return false;
        }
        Touch.scrollTo(textView, layout, textView.getScrollX(), layout.getLineTop(0));
        return true;
    }

    protected boolean scrollBottom(TextView textView, Spannable spannable) {
        Layout layout = textView.getLayout();
        int lineCount = layout.getLineCount();
        if (getBottomLine(textView) <= lineCount - 1) {
            Touch.scrollTo(textView, layout, textView.getScrollX(), layout.getLineTop(lineCount) - getInnerHeight(textView));
            return true;
        }
        return false;
    }

    protected boolean scrollLineStart(TextView textView, Spannable spannable) {
        int scrollBoundsLeft = getScrollBoundsLeft(textView);
        if (textView.getScrollX() > scrollBoundsLeft) {
            textView.scrollTo(scrollBoundsLeft, textView.getScrollY());
            return true;
        }
        return false;
    }

    protected boolean scrollLineEnd(TextView textView, Spannable spannable) {
        int scrollBoundsRight = getScrollBoundsRight(textView) - getInnerWidth(textView);
        if (textView.getScrollX() < scrollBoundsRight) {
            textView.scrollTo(scrollBoundsRight, textView.getScrollY());
            return true;
        }
        return false;
    }
}

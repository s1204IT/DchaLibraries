package android.text.method;

import android.graphics.Rect;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

public class ArrowKeyMovementMethod extends BaseMovementMethod implements MovementMethod {
    private static final Object LAST_TAP_DOWN = new Object();
    private static ArrowKeyMovementMethod sInstance;

    private static boolean isSelecting(Spannable buffer) {
        return MetaKeyKeyListener.getMetaState(buffer, 1) == 1 || MetaKeyKeyListener.getMetaState(buffer, 2048) != 0;
    }

    private static int getCurrentLineTop(Spannable buffer, Layout layout) {
        return layout.getLineTop(layout.getLineForOffset(Selection.getSelectionEnd(buffer)));
    }

    private static int getPageHeight(TextView widget) {
        Rect rect = new Rect();
        if (widget.getGlobalVisibleRect(rect)) {
            return rect.height();
        }
        return 0;
    }

    @Override
    protected boolean handleMovementKey(TextView widget, Spannable buffer, int keyCode, int movementMetaState, KeyEvent event) {
        switch (keyCode) {
            case 23:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState) && event.getAction() == 0 && event.getRepeatCount() == 0 && MetaKeyKeyListener.getMetaState(buffer, 2048, event) != 0) {
                    return widget.showContextMenu();
                }
                break;
        }
        return super.handleMovementKey(widget, buffer, keyCode, movementMetaState, event);
    }

    @Override
    protected boolean left(TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();
        return isSelecting(buffer) ? Selection.extendLeft(buffer, layout) : Selection.moveLeft(buffer, layout);
    }

    @Override
    protected boolean right(TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();
        return isSelecting(buffer) ? Selection.extendRight(buffer, layout) : Selection.moveRight(buffer, layout);
    }

    @Override
    protected boolean up(TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();
        return isSelecting(buffer) ? Selection.extendUp(buffer, layout) : Selection.moveUp(buffer, layout);
    }

    @Override
    protected boolean down(TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();
        return isSelecting(buffer) ? Selection.extendDown(buffer, layout) : Selection.moveDown(buffer, layout);
    }

    @Override
    protected boolean pageUp(TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();
        boolean selecting = isSelecting(buffer);
        int targetY = getCurrentLineTop(buffer, layout) - getPageHeight(widget);
        boolean handled = false;
        do {
            int previousSelectionEnd = Selection.getSelectionEnd(buffer);
            if (selecting) {
                Selection.extendUp(buffer, layout);
            } else {
                Selection.moveUp(buffer, layout);
            }
            if (Selection.getSelectionEnd(buffer) == previousSelectionEnd) {
                break;
            }
            handled = true;
        } while (getCurrentLineTop(buffer, layout) > targetY);
        return handled;
    }

    @Override
    protected boolean pageDown(TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();
        boolean selecting = isSelecting(buffer);
        int targetY = getCurrentLineTop(buffer, layout) + getPageHeight(widget);
        boolean handled = false;
        do {
            int previousSelectionEnd = Selection.getSelectionEnd(buffer);
            if (selecting) {
                Selection.extendDown(buffer, layout);
            } else {
                Selection.moveDown(buffer, layout);
            }
            if (Selection.getSelectionEnd(buffer) == previousSelectionEnd) {
                break;
            }
            handled = true;
        } while (getCurrentLineTop(buffer, layout) < targetY);
        return handled;
    }

    @Override
    protected boolean top(TextView widget, Spannable buffer) {
        if (isSelecting(buffer)) {
            Selection.extendSelection(buffer, 0);
            return true;
        }
        Selection.setSelection(buffer, 0);
        return true;
    }

    @Override
    protected boolean bottom(TextView widget, Spannable buffer) {
        if (isSelecting(buffer)) {
            Selection.extendSelection(buffer, buffer.length());
            return true;
        }
        Selection.setSelection(buffer, buffer.length());
        return true;
    }

    @Override
    protected boolean lineStart(TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();
        return isSelecting(buffer) ? Selection.extendToLeftEdge(buffer, layout) : Selection.moveToLeftEdge(buffer, layout);
    }

    @Override
    protected boolean lineEnd(TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();
        return isSelecting(buffer) ? Selection.extendToRightEdge(buffer, layout) : Selection.moveToRightEdge(buffer, layout);
    }

    @Override
    protected boolean leftWord(TextView widget, Spannable buffer) {
        int selectionEnd = widget.getSelectionEnd();
        WordIterator wordIterator = widget.getWordIterator();
        wordIterator.setCharSequence(buffer, selectionEnd, selectionEnd);
        return Selection.moveToPreceding(buffer, wordIterator, isSelecting(buffer));
    }

    @Override
    protected boolean rightWord(TextView widget, Spannable buffer) {
        int selectionEnd = widget.getSelectionEnd();
        WordIterator wordIterator = widget.getWordIterator();
        wordIterator.setCharSequence(buffer, selectionEnd, selectionEnd);
        return Selection.moveToFollowing(buffer, wordIterator, isSelecting(buffer));
    }

    @Override
    protected boolean home(TextView widget, Spannable buffer) {
        return lineStart(widget, buffer);
    }

    @Override
    protected boolean end(TextView widget, Spannable buffer) {
        return lineEnd(widget, buffer);
    }

    private static boolean isTouchSelecting(boolean isMouse, Spannable buffer) {
        return isMouse ? Touch.isActivelySelecting(buffer) : isSelecting(buffer);
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int initialScrollX = -1;
        int initialScrollY = -1;
        int action = event.getAction();
        boolean isMouse = event.isFromSource(8194);
        if (action == 1) {
            initialScrollX = Touch.getInitialScrollX(widget, buffer);
            initialScrollY = Touch.getInitialScrollY(widget, buffer);
        }
        boolean handled = Touch.onTouchEvent(widget, buffer, event);
        if (widget.isFocused() && !widget.didTouchFocusSelect()) {
            if (action == 0) {
                if (isMouse || isTouchSelecting(isMouse, buffer)) {
                    int offset = widget.getOffsetForPosition(event.getX(), event.getY());
                    buffer.setSpan(LAST_TAP_DOWN, offset, offset, 34);
                    widget.getParent().requestDisallowInterceptTouchEvent(true);
                    return handled;
                }
                return handled;
            }
            if (action == 2) {
                if (isMouse && Touch.isSelectionStarted(buffer)) {
                    int offset2 = buffer.getSpanStart(LAST_TAP_DOWN);
                    Selection.setSelection(buffer, offset2);
                }
                if (isTouchSelecting(isMouse, buffer) && handled) {
                    widget.cancelLongPress();
                    int offset3 = widget.getOffsetForPosition(event.getX(), event.getY());
                    Selection.extendSelection(buffer, offset3);
                    return true;
                }
                return handled;
            }
            if (action == 1) {
                if ((initialScrollY >= 0 && initialScrollY != widget.getScrollY()) || (initialScrollX >= 0 && initialScrollX != widget.getScrollX())) {
                    widget.moveCursorToVisibleOffset();
                    return true;
                }
                int offset4 = widget.getOffsetForPosition(event.getX(), event.getY());
                if (isTouchSelecting(isMouse, buffer)) {
                    buffer.removeSpan(LAST_TAP_DOWN);
                    Selection.extendSelection(buffer, offset4);
                }
                MetaKeyKeyListener.adjustMetaAfterKeypress(buffer);
                MetaKeyKeyListener.resetLockedMeta(buffer);
                return true;
            }
            return handled;
        }
        return handled;
    }

    @Override
    public boolean canSelectArbitrarily() {
        return true;
    }

    @Override
    public void initialize(TextView widget, Spannable text) {
        Selection.setSelection(text, 0);
    }

    @Override
    public void onTakeFocus(TextView view, Spannable text, int dir) {
        if ((dir & 130) != 0) {
            if (view.getLayout() == null) {
                Selection.setSelection(text, text.length());
                return;
            }
            return;
        }
        Selection.setSelection(text, text.length());
    }

    public static MovementMethod getInstance() {
        if (sInstance == null) {
            sInstance = new ArrowKeyMovementMethod();
        }
        return sInstance;
    }
}

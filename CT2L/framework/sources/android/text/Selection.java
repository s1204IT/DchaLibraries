package android.text;

public class Selection {
    public static final Object SELECTION_END;
    public static final Object SELECTION_START;

    public interface PositionIterator {
        public static final int DONE = -1;

        int following(int i);

        int preceding(int i);
    }

    private Selection() {
    }

    public static final int getSelectionStart(CharSequence text) {
        if (text instanceof Spanned) {
            return ((Spanned) text).getSpanStart(SELECTION_START);
        }
        return -1;
    }

    public static final int getSelectionEnd(CharSequence text) {
        if (text instanceof Spanned) {
            return ((Spanned) text).getSpanStart(SELECTION_END);
        }
        return -1;
    }

    public static void setSelection(Spannable text, int start, int stop) {
        int ostart = getSelectionStart(text);
        int oend = getSelectionEnd(text);
        if (ostart != start || oend != stop) {
            text.setSpan(SELECTION_START, start, start, 546);
            text.setSpan(SELECTION_END, stop, stop, 34);
        }
    }

    public static final void setSelection(Spannable text, int index) {
        setSelection(text, index, index);
    }

    public static final void selectAll(Spannable text) {
        setSelection(text, 0, text.length());
    }

    public static final void extendSelection(Spannable text, int index) {
        if (text.getSpanStart(SELECTION_END) != index) {
            text.setSpan(SELECTION_END, index, index, 34);
        }
    }

    public static final void removeSelection(Spannable text) {
        text.removeSpan(SELECTION_START);
        text.removeSpan(SELECTION_END);
    }

    public static boolean moveUp(Spannable text, Layout layout) {
        int move;
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);
        if (start != end) {
            int min = Math.min(start, end);
            int max = Math.max(start, end);
            setSelection(text, min);
            return (min == 0 && max == text.length()) ? false : true;
        }
        int line = layout.getLineForOffset(end);
        if (line > 0) {
            if (layout.getParagraphDirection(line) == layout.getParagraphDirection(line - 1)) {
                float h = layout.getPrimaryHorizontal(end);
                move = layout.getOffsetForHorizontal(line - 1, h);
            } else {
                move = layout.getLineStart(line - 1);
            }
            setSelection(text, move);
            return true;
        }
        if (end == 0) {
            return false;
        }
        setSelection(text, 0);
        return true;
    }

    public static boolean moveDown(Spannable text, Layout layout) {
        int move;
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);
        if (start != end) {
            int min = Math.min(start, end);
            int max = Math.max(start, end);
            setSelection(text, max);
            return (min == 0 && max == text.length()) ? false : true;
        }
        int line = layout.getLineForOffset(end);
        if (line < layout.getLineCount() - 1) {
            if (layout.getParagraphDirection(line) == layout.getParagraphDirection(line + 1)) {
                float h = layout.getPrimaryHorizontal(end);
                move = layout.getOffsetForHorizontal(line + 1, h);
            } else {
                move = layout.getLineStart(line + 1);
            }
            setSelection(text, move);
            return true;
        }
        if (end == text.length()) {
            return false;
        }
        setSelection(text, text.length());
        return true;
    }

    public static boolean moveLeft(Spannable text, Layout layout) {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);
        if (start != end) {
            setSelection(text, chooseHorizontal(layout, -1, start, end));
            return true;
        }
        int to = layout.getOffsetToLeftOf(end);
        if (to != end) {
            setSelection(text, to);
            return true;
        }
        return false;
    }

    public static boolean moveRight(Spannable text, Layout layout) {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);
        if (start != end) {
            setSelection(text, chooseHorizontal(layout, 1, start, end));
            return true;
        }
        int to = layout.getOffsetToRightOf(end);
        if (to != end) {
            setSelection(text, to);
            return true;
        }
        return false;
    }

    public static boolean extendUp(Spannable text, Layout layout) {
        int move;
        int end = getSelectionEnd(text);
        int line = layout.getLineForOffset(end);
        if (line > 0) {
            if (layout.getParagraphDirection(line) == layout.getParagraphDirection(line - 1)) {
                float h = layout.getPrimaryHorizontal(end);
                move = layout.getOffsetForHorizontal(line - 1, h);
            } else {
                move = layout.getLineStart(line - 1);
            }
            extendSelection(text, move);
        } else if (end != 0) {
            extendSelection(text, 0);
        }
        return true;
    }

    public static boolean extendDown(Spannable text, Layout layout) {
        int move;
        int end = getSelectionEnd(text);
        int line = layout.getLineForOffset(end);
        if (line < layout.getLineCount() - 1) {
            if (layout.getParagraphDirection(line) == layout.getParagraphDirection(line + 1)) {
                float h = layout.getPrimaryHorizontal(end);
                move = layout.getOffsetForHorizontal(line + 1, h);
            } else {
                move = layout.getLineStart(line + 1);
            }
            extendSelection(text, move);
        } else if (end != text.length()) {
            extendSelection(text, text.length());
        }
        return true;
    }

    public static boolean extendLeft(Spannable text, Layout layout) {
        int end = getSelectionEnd(text);
        int to = layout.getOffsetToLeftOf(end);
        if (to != end) {
            extendSelection(text, to);
        }
        return true;
    }

    public static boolean extendRight(Spannable text, Layout layout) {
        int end = getSelectionEnd(text);
        int to = layout.getOffsetToRightOf(end);
        if (to != end) {
            extendSelection(text, to);
        }
        return true;
    }

    public static boolean extendToLeftEdge(Spannable text, Layout layout) {
        int where = findEdge(text, layout, -1);
        extendSelection(text, where);
        return true;
    }

    public static boolean extendToRightEdge(Spannable text, Layout layout) {
        int where = findEdge(text, layout, 1);
        extendSelection(text, where);
        return true;
    }

    public static boolean moveToLeftEdge(Spannable text, Layout layout) {
        int where = findEdge(text, layout, -1);
        setSelection(text, where);
        return true;
    }

    public static boolean moveToRightEdge(Spannable text, Layout layout) {
        int where = findEdge(text, layout, 1);
        setSelection(text, where);
        return true;
    }

    public static boolean moveToPreceding(Spannable text, PositionIterator iter, boolean extendSelection) {
        int offset = iter.preceding(getSelectionEnd(text));
        if (offset != -1) {
            if (extendSelection) {
                extendSelection(text, offset);
                return true;
            }
            setSelection(text, offset);
            return true;
        }
        return true;
    }

    public static boolean moveToFollowing(Spannable text, PositionIterator iter, boolean extendSelection) {
        int offset = iter.following(getSelectionEnd(text));
        if (offset != -1) {
            if (extendSelection) {
                extendSelection(text, offset);
                return true;
            }
            setSelection(text, offset);
            return true;
        }
        return true;
    }

    private static int findEdge(Spannable text, Layout layout, int dir) {
        int pt = getSelectionEnd(text);
        int line = layout.getLineForOffset(pt);
        int pdir = layout.getParagraphDirection(line);
        if (dir * pdir < 0) {
            return layout.getLineStart(line);
        }
        int end = layout.getLineEnd(line);
        return line != layout.getLineCount() + (-1) ? end - 1 : end;
    }

    private static int chooseHorizontal(Layout layout, int direction, int off1, int off2) {
        int line1 = layout.getLineForOffset(off1);
        int line2 = layout.getLineForOffset(off2);
        if (line1 == line2) {
            float h1 = layout.getPrimaryHorizontal(off1);
            float h2 = layout.getPrimaryHorizontal(off2);
            return direction < 0 ? h1 < h2 ? off1 : off2 : h1 <= h2 ? off2 : off1;
        }
        int line = layout.getLineForOffset(off1);
        int textdir = layout.getParagraphDirection(line);
        if (textdir == direction) {
            return Math.max(off1, off2);
        }
        return Math.min(off1, off2);
    }

    private static final class START implements NoCopySpan {
        private START() {
        }
    }

    private static final class END implements NoCopySpan {
        private END() {
        }
    }

    static {
        SELECTION_START = new START();
        SELECTION_END = new END();
    }
}

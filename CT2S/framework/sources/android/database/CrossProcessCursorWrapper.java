package android.database;

public class CrossProcessCursorWrapper extends CursorWrapper implements CrossProcessCursor {
    public CrossProcessCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    @Override
    public void fillWindow(int position, CursorWindow window) {
        if (this.mCursor instanceof CrossProcessCursor) {
            CrossProcessCursor crossProcessCursor = (CrossProcessCursor) this.mCursor;
            crossProcessCursor.fillWindow(position, window);
        } else {
            DatabaseUtils.cursorFillWindow(this.mCursor, position, window);
        }
    }

    @Override
    public CursorWindow getWindow() {
        if (!(this.mCursor instanceof CrossProcessCursor)) {
            return null;
        }
        CrossProcessCursor crossProcessCursor = (CrossProcessCursor) this.mCursor;
        return crossProcessCursor.getWindow();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        if (!(this.mCursor instanceof CrossProcessCursor)) {
            return true;
        }
        CrossProcessCursor crossProcessCursor = (CrossProcessCursor) this.mCursor;
        return crossProcessCursor.onMove(oldPosition, newPosition);
    }
}

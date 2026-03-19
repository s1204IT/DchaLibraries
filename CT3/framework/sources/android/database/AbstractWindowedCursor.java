package android.database;

import android.os.Build;
import android.util.Log;

public abstract class AbstractWindowedCursor extends AbstractCursor {
    private static final boolean DEBUG_CLOSE_TRACE = Log.isLoggable("DBStaleDataTrace", 2);
    private static final boolean DEBUG_IS_ENG_BUILD = "eng".equals(Build.TYPE);
    protected CursorWindow mWindow;
    private Throwable mWindowCloseTrace;

    @Override
    public byte[] getBlob(int columnIndex) {
        checkPosition();
        return this.mWindow.getBlob(this.mPos, columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        checkPosition();
        return this.mWindow.getString(this.mPos, columnIndex);
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        checkPosition();
        this.mWindow.copyStringToBuffer(this.mPos, columnIndex, buffer);
    }

    @Override
    public short getShort(int columnIndex) {
        checkPosition();
        return this.mWindow.getShort(this.mPos, columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        checkPosition();
        return this.mWindow.getInt(this.mPos, columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        checkPosition();
        return this.mWindow.getLong(this.mPos, columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) {
        checkPosition();
        return this.mWindow.getFloat(this.mPos, columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) {
        checkPosition();
        return this.mWindow.getDouble(this.mPos, columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex) {
        checkPosition();
        return this.mWindow.getType(this.mPos, columnIndex) == 0;
    }

    @Deprecated
    public boolean isBlob(int columnIndex) {
        return getType(columnIndex) == 4;
    }

    @Deprecated
    public boolean isString(int columnIndex) {
        return getType(columnIndex) == 3;
    }

    @Deprecated
    public boolean isLong(int columnIndex) {
        return getType(columnIndex) == 1;
    }

    @Deprecated
    public boolean isFloat(int columnIndex) {
        return getType(columnIndex) == 2;
    }

    @Override
    public int getType(int columnIndex) {
        checkPosition();
        return this.mWindow.getType(this.mPos, columnIndex);
    }

    @Override
    protected void checkPosition() {
        super.checkPosition();
        if (this.mWindow != null) {
            return;
        }
        if (DEBUG_IS_ENG_BUILD || DEBUG_CLOSE_TRACE) {
            Log.v("CursorOrCursorWindowClosed", "CursorWindow close stack trace", this.mWindowCloseTrace);
        }
        throw new StaleDataException("Attempting to access a closed CursorWindow.Most probable cause: cursor is deactivated prior to calling this method.");
    }

    @Override
    public CursorWindow getWindow() {
        return this.mWindow;
    }

    public void setWindow(CursorWindow window) {
        if (window == this.mWindow) {
            return;
        }
        closeWindow();
        this.mWindow = window;
    }

    public boolean hasWindow() {
        return this.mWindow != null;
    }

    protected void closeWindow() {
        if (this.mWindow == null) {
            return;
        }
        this.mWindow.close();
        this.mWindow = null;
        if (!DEBUG_IS_ENG_BUILD && !DEBUG_CLOSE_TRACE) {
            return;
        }
        this.mWindowCloseTrace = new Throwable("stacktrace");
    }

    protected void clearOrCreateWindow(String name) {
        if (this.mWindow == null) {
            this.mWindow = new CursorWindow(name);
        } else {
            this.mWindow.clear();
        }
    }

    @Override
    protected void onDeactivateOrClose() {
        super.onDeactivateOrClose();
        closeWindow();
    }
}

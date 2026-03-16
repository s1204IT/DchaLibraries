package android.database.sqlite;

import android.database.AbstractWindowedCursor;
import android.database.CursorWindow;
import android.database.DatabaseUtils;
import android.os.StrictMode;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class SQLiteCursor extends AbstractWindowedCursor {
    static final int NO_COUNT = -1;
    static final String TAG = "SQLiteCursor";
    private Map<String, Integer> mColumnNameMap;
    private final String[] mColumns;
    private int mCount;
    private int mCursorWindowCapacity;
    private final SQLiteCursorDriver mDriver;
    private final String mEditTable;
    private final SQLiteQuery mQuery;
    private final Throwable mStackTrace;

    @Deprecated
    public SQLiteCursor(SQLiteDatabase db, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
        this(driver, editTable, query);
    }

    public SQLiteCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
        this.mCount = -1;
        if (query == null) {
            throw new IllegalArgumentException("query object cannot be null");
        }
        if (StrictMode.vmSqliteObjectLeaksEnabled()) {
            this.mStackTrace = new DatabaseObjectNotClosedException().fillInStackTrace();
        } else {
            this.mStackTrace = null;
        }
        this.mDriver = driver;
        this.mEditTable = editTable;
        this.mColumnNameMap = null;
        this.mQuery = query;
        this.mColumns = query.getColumnNames();
        this.mRowIdColumnIndex = DatabaseUtils.findRowIdColumnIndex(this.mColumns);
    }

    public SQLiteDatabase getDatabase() {
        return this.mQuery.getDatabase();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        if (this.mWindow == null || newPosition < this.mWindow.getStartPosition() || newPosition >= this.mWindow.getStartPosition() + this.mWindow.getNumRows()) {
            fillWindow(newPosition);
            return true;
        }
        return true;
    }

    @Override
    public int getCount() {
        if (this.mCount == -1) {
            fillWindow(0);
        }
        return this.mCount;
    }

    private void fillWindow(int requiredPos) {
        clearOrCreateWindow(getDatabase().getPath());
        try {
            if (this.mCount == -1) {
                int startPos = DatabaseUtils.cursorPickFillWindowStartPosition(requiredPos, 0);
                this.mCount = this.mQuery.fillWindow(this.mWindow, startPos, requiredPos, true);
                this.mCursorWindowCapacity = this.mWindow.getNumRows();
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "received count(*) from native_fill_window: " + this.mCount);
                    return;
                }
                return;
            }
            int startPos2 = DatabaseUtils.cursorPickFillWindowStartPosition(requiredPos, this.mCursorWindowCapacity);
            this.mQuery.fillWindow(this.mWindow, startPos2, requiredPos, false);
        } catch (RuntimeException ex) {
            closeWindow();
            throw ex;
        }
    }

    @Override
    public int getColumnIndex(String columnName) {
        if (this.mColumnNameMap == null) {
            String[] columns = this.mColumns;
            int columnCount = columns.length;
            HashMap<String, Integer> map = new HashMap<>(columnCount, 1.0f);
            for (int i = 0; i < columnCount; i++) {
                map.put(columns[i], Integer.valueOf(i));
            }
            this.mColumnNameMap = map;
        }
        int periodIndex = columnName.lastIndexOf(46);
        if (periodIndex != -1) {
            Exception e = new Exception();
            Log.e(TAG, "requesting column name with table name -- " + columnName, e);
            columnName = columnName.substring(periodIndex + 1);
        }
        Integer i2 = this.mColumnNameMap.get(columnName);
        if (i2 != null) {
            return i2.intValue();
        }
        return -1;
    }

    @Override
    public String[] getColumnNames() {
        return this.mColumns;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        this.mDriver.cursorDeactivated();
    }

    @Override
    public void close() {
        super.close();
        synchronized (this) {
            this.mQuery.close();
            this.mDriver.cursorClosed();
        }
    }

    @Override
    public boolean requery() {
        if (isClosed()) {
            return false;
        }
        synchronized (this) {
            if (!this.mQuery.getDatabase().isOpen()) {
                return false;
            }
            if (this.mWindow != null) {
                this.mWindow.clear();
            }
            this.mPos = -1;
            this.mCount = -1;
            this.mDriver.cursorRequeried(this);
            try {
                return super.requery();
            } catch (IllegalStateException e) {
                Log.w(TAG, "requery() failed " + e.getMessage(), e);
                return false;
            }
        }
    }

    @Override
    public void setWindow(CursorWindow window) {
        super.setWindow(window);
        this.mCount = -1;
    }

    public void setSelectionArguments(String[] selectionArgs) {
        this.mDriver.setBindArguments(selectionArgs);
    }

    @Override
    protected void finalize() {
        try {
            if (this.mWindow != null) {
                if (this.mStackTrace != null) {
                    String sql = this.mQuery.getSql();
                    int len = sql.length();
                    StringBuilder sbAppend = new StringBuilder().append("Finalizing a Cursor that has not been deactivated or closed. database = ").append(this.mQuery.getDatabase().getLabel()).append(", table = ").append(this.mEditTable).append(", query = ");
                    if (len > 1000) {
                        len = 1000;
                    }
                    StrictMode.onSqliteObjectLeaked(sbAppend.append(sql.substring(0, len)).toString(), this.mStackTrace);
                }
                close();
            }
        } finally {
            super.finalize();
        }
    }
}

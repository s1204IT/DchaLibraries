package android.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCursor implements CrossProcessCursor {
    private static final String TAG = "Cursor";
    protected boolean mClosed;
    protected ContentResolver mContentResolver;
    private Uri mNotifyUri;
    private ContentObserver mSelfObserver;
    private boolean mSelfObserverRegistered;
    private final Object mSelfObserverLock = new Object();
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    private final ContentObservable mContentObservable = new ContentObservable();
    private Bundle mExtras = Bundle.EMPTY;
    protected int mPos = -1;

    @Deprecated
    protected int mRowIdColumnIndex = -1;

    @Deprecated
    protected Long mCurrentRowID = null;

    @Deprecated
    protected HashMap<Long, Map<String, Object>> mUpdatedRows = new HashMap<>();

    @Override
    public abstract String[] getColumnNames();

    @Override
    public abstract int getCount();

    @Override
    public abstract double getDouble(int i);

    @Override
    public abstract float getFloat(int i);

    @Override
    public abstract int getInt(int i);

    @Override
    public abstract long getLong(int i);

    @Override
    public abstract short getShort(int i);

    @Override
    public abstract String getString(int i);

    @Override
    public abstract boolean isNull(int i);

    @Override
    public int getType(int column) {
        return 3;
    }

    @Override
    public byte[] getBlob(int column) {
        throw new UnsupportedOperationException("getBlob is not supported");
    }

    @Override
    public CursorWindow getWindow() {
        return null;
    }

    @Override
    public int getColumnCount() {
        return getColumnNames().length;
    }

    @Override
    public void deactivate() {
        onDeactivateOrClose();
    }

    protected void onDeactivateOrClose() {
        if (this.mSelfObserver != null) {
            this.mContentResolver.unregisterContentObserver(this.mSelfObserver);
            this.mSelfObserverRegistered = false;
        }
        this.mDataSetObservable.notifyInvalidated();
    }

    @Override
    public boolean requery() {
        if (this.mSelfObserver != null && !this.mSelfObserverRegistered) {
            this.mContentResolver.registerContentObserver(this.mNotifyUri, true, this.mSelfObserver);
            this.mSelfObserverRegistered = true;
        }
        this.mDataSetObservable.notifyChanged();
        return true;
    }

    @Override
    public boolean isClosed() {
        return this.mClosed;
    }

    @Override
    public void close() {
        this.mClosed = true;
        this.mContentObservable.unregisterAll();
        onDeactivateOrClose();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return true;
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        String result = getString(columnIndex);
        if (result != null) {
            char[] data = buffer.data;
            if (data == null || data.length < result.length()) {
                buffer.data = result.toCharArray();
            } else {
                result.getChars(0, result.length(), data, 0);
            }
            buffer.sizeCopied = result.length();
            return;
        }
        buffer.sizeCopied = 0;
    }

    @Override
    public final int getPosition() {
        return this.mPos;
    }

    @Override
    public final boolean moveToPosition(int position) {
        int count = getCount();
        if (position >= count) {
            this.mPos = count;
            return false;
        }
        if (position < 0) {
            this.mPos = -1;
            return false;
        }
        if (position == this.mPos) {
            return true;
        }
        boolean result = onMove(this.mPos, position);
        if (!result) {
            this.mPos = -1;
            return result;
        }
        this.mPos = position;
        if (this.mRowIdColumnIndex != -1) {
            this.mCurrentRowID = Long.valueOf(getLong(this.mRowIdColumnIndex));
            return result;
        }
        return result;
    }

    @Override
    public void fillWindow(int position, CursorWindow window) {
        DatabaseUtils.cursorFillWindow(this, position, window);
    }

    @Override
    public final boolean move(int offset) {
        return moveToPosition(this.mPos + offset);
    }

    @Override
    public final boolean moveToFirst() {
        return moveToPosition(0);
    }

    @Override
    public final boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }

    @Override
    public final boolean moveToNext() {
        return moveToPosition(this.mPos + 1);
    }

    @Override
    public final boolean moveToPrevious() {
        return moveToPosition(this.mPos - 1);
    }

    @Override
    public final boolean isFirst() {
        return this.mPos == 0 && getCount() != 0;
    }

    @Override
    public final boolean isLast() {
        int cnt = getCount();
        return this.mPos == cnt + (-1) && cnt != 0;
    }

    @Override
    public final boolean isBeforeFirst() {
        return getCount() == 0 || this.mPos == -1;
    }

    @Override
    public final boolean isAfterLast() {
        return getCount() == 0 || this.mPos == getCount();
    }

    @Override
    public int getColumnIndex(String columnName) {
        int periodIndex = columnName.lastIndexOf(46);
        if (periodIndex != -1) {
            Exception e = new Exception();
            Log.e(TAG, "requesting column name with table name -- " + columnName, e);
            columnName = columnName.substring(periodIndex + 1);
        }
        String[] columnNames = getColumnNames();
        int length = columnNames.length;
        for (int i = 0; i < length; i++) {
            if (columnNames[i].equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) {
        int index = getColumnIndex(columnName);
        if (index < 0) {
            throw new IllegalArgumentException("column '" + columnName + "' does not exist");
        }
        return index;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return getColumnNames()[columnIndex];
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        this.mContentObservable.registerObserver(observer);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        if (!this.mClosed) {
            this.mContentObservable.unregisterObserver(observer);
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        this.mDataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        this.mDataSetObservable.unregisterObserver(observer);
    }

    protected void onChange(boolean selfChange) {
        synchronized (this.mSelfObserverLock) {
            this.mContentObservable.dispatchChange(selfChange, null);
            if (this.mNotifyUri != null && selfChange) {
                this.mContentResolver.notifyChange(this.mNotifyUri, this.mSelfObserver);
            }
        }
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri notifyUri) {
        setNotificationUri(cr, notifyUri, UserHandle.myUserId());
    }

    public void setNotificationUri(ContentResolver cr, Uri notifyUri, int userHandle) {
        synchronized (this.mSelfObserverLock) {
            this.mNotifyUri = notifyUri;
            this.mContentResolver = cr;
            if (this.mSelfObserver != null) {
                this.mContentResolver.unregisterContentObserver(this.mSelfObserver);
            }
            this.mSelfObserver = new SelfContentObserver(this);
            this.mContentResolver.registerContentObserver(this.mNotifyUri, true, this.mSelfObserver, userHandle);
            this.mSelfObserverRegistered = true;
        }
    }

    @Override
    public Uri getNotificationUri() {
        Uri uri;
        synchronized (this.mSelfObserverLock) {
            uri = this.mNotifyUri;
        }
        return uri;
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return false;
    }

    public void setExtras(Bundle extras) {
        if (extras == null) {
            extras = Bundle.EMPTY;
        }
        this.mExtras = extras;
    }

    @Override
    public Bundle getExtras() {
        return this.mExtras;
    }

    @Override
    public Bundle respond(Bundle extras) {
        return Bundle.EMPTY;
    }

    @Deprecated
    protected boolean isFieldUpdated(int columnIndex) {
        return false;
    }

    @Deprecated
    protected Object getUpdatedField(int columnIndex) {
        return null;
    }

    protected void checkPosition() {
        if (-1 == this.mPos || getCount() == this.mPos) {
            throw new CursorIndexOutOfBoundsException(this.mPos, getCount());
        }
    }

    protected void finalize() {
        if (this.mSelfObserver != null && this.mSelfObserverRegistered) {
            this.mContentResolver.unregisterContentObserver(this.mSelfObserver);
        }
        try {
            if (!this.mClosed) {
                close();
            }
        } catch (Exception e) {
        }
    }

    protected static class SelfContentObserver extends ContentObserver {
        WeakReference<AbstractCursor> mCursor;

        public SelfContentObserver(AbstractCursor cursor) {
            super(null);
            this.mCursor = new WeakReference<>(cursor);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange) {
            AbstractCursor cursor = this.mCursor.get();
            if (cursor != null) {
                cursor.onChange(false);
            }
        }
    }
}

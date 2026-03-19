package android.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCursor implements CrossProcessCursor {
    private static final String TAG = "Cursor";

    @Deprecated
    protected boolean mClosed;

    @Deprecated
    protected ContentResolver mContentResolver;
    protected Long mCurrentRowID;
    private Uri mNotifyUri;
    protected int mRowIdColumnIndex;
    private ContentObserver mSelfObserver;
    private boolean mSelfObserverRegistered;
    protected HashMap<Long, Map<String, Object>> mUpdatedRows;
    private final Object mSelfObserverLock = new Object();
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    private final ContentObservable mContentObservable = new ContentObservable();
    private Bundle mExtras = Bundle.EMPTY;

    @Deprecated
    protected int mPos = -1;

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
    public int getType(int i) {
        return 3;
    }

    @Override
    public byte[] getBlob(int i) {
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
    public boolean onMove(int i, int i2) {
        return true;
    }

    @Override
    public void copyStringToBuffer(int i, CharArrayBuffer charArrayBuffer) {
        String string = getString(i);
        if (string != null) {
            char[] cArr = charArrayBuffer.data;
            if (cArr != null && cArr.length >= string.length()) {
                string.getChars(0, string.length(), cArr, 0);
            } else {
                charArrayBuffer.data = string.toCharArray();
            }
            charArrayBuffer.sizeCopied = string.length();
            return;
        }
        charArrayBuffer.sizeCopied = 0;
    }

    @Override
    public final int getPosition() {
        return this.mPos;
    }

    @Override
    public final boolean moveToPosition(int i) {
        int count = getCount();
        if (i >= count) {
            this.mPos = count;
            return false;
        }
        if (i < 0) {
            this.mPos = -1;
            return false;
        }
        if (i == this.mPos) {
            return true;
        }
        boolean zOnMove = onMove(this.mPos, i);
        if (!zOnMove) {
            this.mPos = -1;
        } else {
            this.mPos = i;
        }
        return zOnMove;
    }

    @Override
    public void fillWindow(int i, CursorWindow cursorWindow) {
        DatabaseUtils.cursorFillWindow(this, i, cursorWindow);
    }

    @Override
    public final boolean move(int i) {
        return moveToPosition(this.mPos + i);
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
        int count = getCount();
        return this.mPos == count + (-1) && count != 0;
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
    public int getColumnIndex(String str) {
        int iLastIndexOf = str.lastIndexOf(46);
        if (iLastIndexOf != -1) {
            Log.e(TAG, "requesting column name with table name -- " + str, new Exception());
            str = str.substring(iLastIndexOf + 1);
        }
        String[] columnNames = getColumnNames();
        int length = columnNames.length;
        for (int i = 0; i < length; i++) {
            if (columnNames[i].equalsIgnoreCase(str)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getColumnIndexOrThrow(String str) {
        String string;
        int columnIndex = getColumnIndex(str);
        if (columnIndex < 0) {
            try {
                string = Arrays.toString(getColumnNames());
            } catch (Exception e) {
                Log.d(TAG, "Cannot collect column names for debug purposes", e);
                string = "";
            }
            throw new IllegalArgumentException("column '" + str + "' does not exist. Available columns: " + string);
        }
        return columnIndex;
    }

    @Override
    public String getColumnName(int i) {
        return getColumnNames()[i];
    }

    @Override
    public void registerContentObserver(ContentObserver contentObserver) {
        this.mContentObservable.registerObserver(contentObserver);
    }

    @Override
    public void unregisterContentObserver(ContentObserver contentObserver) {
        if (!this.mClosed) {
            this.mContentObservable.unregisterObserver(contentObserver);
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        this.mDataSetObservable.registerObserver(dataSetObserver);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        this.mDataSetObservable.unregisterObserver(dataSetObserver);
    }

    protected void onChange(boolean z) {
        synchronized (this.mSelfObserverLock) {
            this.mContentObservable.dispatchChange(z, null);
            if (this.mNotifyUri != null && z) {
                this.mContentResolver.notifyChange(this.mNotifyUri, this.mSelfObserver);
            }
        }
    }

    @Override
    public void setNotificationUri(ContentResolver contentResolver, Uri uri) {
        setNotificationUri(contentResolver, uri, contentResolver.getUserId());
    }

    public void setNotificationUri(ContentResolver contentResolver, Uri uri, int i) {
        synchronized (this.mSelfObserverLock) {
            this.mNotifyUri = uri;
            this.mContentResolver = contentResolver;
            if (this.mSelfObserver != null) {
                this.mContentResolver.unregisterContentObserver(this.mSelfObserver);
            }
            this.mSelfObserver = new SelfContentObserver(this);
            this.mContentResolver.registerContentObserver(this.mNotifyUri, true, this.mSelfObserver, i);
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

    @Override
    public void setExtras(Bundle bundle) {
        if (bundle == null) {
            bundle = Bundle.EMPTY;
        }
        this.mExtras = bundle;
    }

    @Override
    public Bundle getExtras() {
        return this.mExtras;
    }

    @Override
    public Bundle respond(Bundle bundle) {
        return Bundle.EMPTY;
    }

    @Deprecated
    protected boolean isFieldUpdated(int i) {
        return false;
    }

    @Deprecated
    protected Object getUpdatedField(int i) {
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

        public SelfContentObserver(AbstractCursor abstractCursor) {
            super(null);
            this.mCursor = new WeakReference<>(abstractCursor);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean z) {
            AbstractCursor abstractCursor = this.mCursor.get();
            if (abstractCursor != null) {
                abstractCursor.onChange(false);
            }
        }
    }
}

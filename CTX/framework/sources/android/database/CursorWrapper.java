package android.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;

public class CursorWrapper implements Cursor {
    protected final Cursor mCursor;

    public CursorWrapper(Cursor cursor) {
        this.mCursor = cursor;
    }

    public Cursor getWrappedCursor() {
        return this.mCursor;
    }

    @Override
    public void close() {
        this.mCursor.close();
    }

    @Override
    public boolean isClosed() {
        return this.mCursor.isClosed();
    }

    @Override
    public int getCount() {
        return this.mCursor.getCount();
    }

    @Override
    @Deprecated
    public void deactivate() {
        this.mCursor.deactivate();
    }

    @Override
    public boolean moveToFirst() {
        return this.mCursor.moveToFirst();
    }

    @Override
    public int getColumnCount() {
        return this.mCursor.getColumnCount();
    }

    @Override
    public int getColumnIndex(String str) {
        return this.mCursor.getColumnIndex(str);
    }

    @Override
    public int getColumnIndexOrThrow(String str) throws IllegalArgumentException {
        return this.mCursor.getColumnIndexOrThrow(str);
    }

    @Override
    public String getColumnName(int i) {
        return this.mCursor.getColumnName(i);
    }

    @Override
    public String[] getColumnNames() {
        return this.mCursor.getColumnNames();
    }

    @Override
    public double getDouble(int i) {
        return this.mCursor.getDouble(i);
    }

    @Override
    public void setExtras(Bundle bundle) {
        this.mCursor.setExtras(bundle);
    }

    @Override
    public Bundle getExtras() {
        return this.mCursor.getExtras();
    }

    @Override
    public float getFloat(int i) {
        return this.mCursor.getFloat(i);
    }

    @Override
    public int getInt(int i) {
        return this.mCursor.getInt(i);
    }

    @Override
    public long getLong(int i) {
        return this.mCursor.getLong(i);
    }

    @Override
    public short getShort(int i) {
        return this.mCursor.getShort(i);
    }

    @Override
    public String getString(int i) {
        return this.mCursor.getString(i);
    }

    @Override
    public void copyStringToBuffer(int i, CharArrayBuffer charArrayBuffer) {
        this.mCursor.copyStringToBuffer(i, charArrayBuffer);
    }

    @Override
    public byte[] getBlob(int i) {
        return this.mCursor.getBlob(i);
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return this.mCursor.getWantsAllOnMoveCalls();
    }

    @Override
    public boolean isAfterLast() {
        return this.mCursor.isAfterLast();
    }

    @Override
    public boolean isBeforeFirst() {
        return this.mCursor.isBeforeFirst();
    }

    @Override
    public boolean isFirst() {
        return this.mCursor.isFirst();
    }

    @Override
    public boolean isLast() {
        return this.mCursor.isLast();
    }

    @Override
    public int getType(int i) {
        return this.mCursor.getType(i);
    }

    @Override
    public boolean isNull(int i) {
        return this.mCursor.isNull(i);
    }

    @Override
    public boolean moveToLast() {
        return this.mCursor.moveToLast();
    }

    @Override
    public boolean move(int i) {
        return this.mCursor.move(i);
    }

    @Override
    public boolean moveToPosition(int i) {
        return this.mCursor.moveToPosition(i);
    }

    @Override
    public boolean moveToNext() {
        return this.mCursor.moveToNext();
    }

    @Override
    public int getPosition() {
        return this.mCursor.getPosition();
    }

    @Override
    public boolean moveToPrevious() {
        return this.mCursor.moveToPrevious();
    }

    @Override
    public void registerContentObserver(ContentObserver contentObserver) {
        this.mCursor.registerContentObserver(contentObserver);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        this.mCursor.registerDataSetObserver(dataSetObserver);
    }

    @Override
    @Deprecated
    public boolean requery() {
        return this.mCursor.requery();
    }

    @Override
    public Bundle respond(Bundle bundle) {
        return this.mCursor.respond(bundle);
    }

    @Override
    public void setNotificationUri(ContentResolver contentResolver, Uri uri) {
        this.mCursor.setNotificationUri(contentResolver, uri);
    }

    @Override
    public Uri getNotificationUri() {
        return this.mCursor.getNotificationUri();
    }

    @Override
    public void unregisterContentObserver(ContentObserver contentObserver) {
        this.mCursor.unregisterContentObserver(contentObserver);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        this.mCursor.unregisterDataSetObserver(dataSetObserver);
    }
}

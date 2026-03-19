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
    public int getColumnIndex(String columnName) {
        return this.mCursor.getColumnIndex(columnName);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        return this.mCursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    public String getColumnName(int columnIndex) {
        return this.mCursor.getColumnName(columnIndex);
    }

    @Override
    public String[] getColumnNames() {
        return this.mCursor.getColumnNames();
    }

    @Override
    public double getDouble(int columnIndex) {
        return this.mCursor.getDouble(columnIndex);
    }

    @Override
    public void setExtras(Bundle extras) {
        this.mCursor.setExtras(extras);
    }

    @Override
    public Bundle getExtras() {
        return this.mCursor.getExtras();
    }

    @Override
    public float getFloat(int columnIndex) {
        return this.mCursor.getFloat(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        return this.mCursor.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        return this.mCursor.getLong(columnIndex);
    }

    @Override
    public short getShort(int columnIndex) {
        return this.mCursor.getShort(columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
        return this.mCursor.getString(columnIndex);
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        this.mCursor.copyStringToBuffer(columnIndex, buffer);
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        return this.mCursor.getBlob(columnIndex);
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
    public int getType(int columnIndex) {
        return this.mCursor.getType(columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex) {
        return this.mCursor.isNull(columnIndex);
    }

    @Override
    public boolean moveToLast() {
        return this.mCursor.moveToLast();
    }

    @Override
    public boolean move(int offset) {
        return this.mCursor.move(offset);
    }

    @Override
    public boolean moveToPosition(int position) {
        return this.mCursor.moveToPosition(position);
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
    public void registerContentObserver(ContentObserver observer) {
        this.mCursor.registerContentObserver(observer);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        this.mCursor.registerDataSetObserver(observer);
    }

    @Override
    @Deprecated
    public boolean requery() {
        return this.mCursor.requery();
    }

    @Override
    public Bundle respond(Bundle extras) {
        return this.mCursor.respond(extras);
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {
        this.mCursor.setNotificationUri(cr, uri);
    }

    @Override
    public Uri getNotificationUri() {
        return this.mCursor.getNotificationUri();
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        this.mCursor.unregisterContentObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        this.mCursor.unregisterDataSetObserver(observer);
    }
}

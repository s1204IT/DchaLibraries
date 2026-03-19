package android.database.sqlite;

public class SQLiteTableLockedException extends SQLiteException {
    public SQLiteTableLockedException() {
    }

    public SQLiteTableLockedException(String str) {
        super(str);
    }
}

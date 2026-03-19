package android.database.sqlite;

import android.database.SQLException;

public class SQLiteException extends SQLException {
    public SQLiteException() {
    }

    public SQLiteException(String str) {
        super(str);
    }

    public SQLiteException(String str, Throwable th) {
        super(str, th);
    }
}

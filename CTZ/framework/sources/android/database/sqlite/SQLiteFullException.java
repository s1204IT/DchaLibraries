package android.database.sqlite;

public class SQLiteFullException extends SQLiteException {
    public SQLiteFullException() {
    }

    public SQLiteFullException(String str) {
        super(str);
    }
}

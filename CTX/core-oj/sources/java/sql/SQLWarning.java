package java.sql;

public class SQLWarning extends SQLException {
    private static final long serialVersionUID = 3917336774604784856L;

    public SQLWarning(String str, String str2, int i) {
        super(str, str2, i);
        DriverManager.println("SQLWarning: reason(" + str + ") SQLState(" + str2 + ") vendor code(" + i + ")");
    }

    public SQLWarning(String str, String str2) {
        super(str, str2);
        DriverManager.println("SQLWarning: reason(" + str + ") SQLState(" + str2 + ")");
    }

    public SQLWarning(String str) {
        super(str);
        DriverManager.println("SQLWarning: reason(" + str + ")");
    }

    public SQLWarning() {
        DriverManager.println("SQLWarning: ");
    }

    public SQLWarning(Throwable th) {
        super(th);
        DriverManager.println("SQLWarning");
    }

    public SQLWarning(String str, Throwable th) {
        super(str, th);
        DriverManager.println("SQLWarning : reason(" + str + ")");
    }

    public SQLWarning(String str, String str2, Throwable th) {
        super(str, str2, th);
        DriverManager.println("SQLWarning: reason(" + str + ") SQLState(" + str2 + ")");
    }

    public SQLWarning(String str, String str2, int i, Throwable th) {
        super(str, str2, i, th);
        DriverManager.println("SQLWarning: reason(" + str + ") SQLState(" + str2 + ") vendor code(" + i + ")");
    }

    public SQLWarning getNextWarning() {
        try {
            return (SQLWarning) getNextException();
        } catch (ClassCastException e) {
            throw new Error("SQLWarning chain holds value that is not a SQLWarning");
        }
    }

    public void setNextWarning(SQLWarning sQLWarning) {
        setNextException(sQLWarning);
    }
}

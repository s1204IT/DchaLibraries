package java.sql;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class SQLException extends Exception implements Iterable<Throwable> {
    private static final AtomicReferenceFieldUpdater<SQLException, SQLException> nextUpdater = AtomicReferenceFieldUpdater.newUpdater(SQLException.class, SQLException.class, "next");
    private static final long serialVersionUID = 2135244094396331484L;
    private String SQLState;
    private volatile SQLException next;
    private int vendorCode;

    public SQLException(String str, String str2, int i) {
        super(str);
        this.SQLState = str2;
        this.vendorCode = i;
        if (!(this instanceof SQLWarning) && DriverManager.getLogWriter() != null) {
            DriverManager.println("SQLState(" + str2 + ") vendor code(" + i + ")");
            printStackTrace(DriverManager.getLogWriter());
        }
    }

    public SQLException(String str, String str2) {
        super(str);
        this.SQLState = str2;
        this.vendorCode = 0;
        if (!(this instanceof SQLWarning) && DriverManager.getLogWriter() != null) {
            printStackTrace(DriverManager.getLogWriter());
            DriverManager.println("SQLException: SQLState(" + str2 + ")");
        }
    }

    public SQLException(String str) {
        super(str);
        this.SQLState = null;
        this.vendorCode = 0;
        if (!(this instanceof SQLWarning) && DriverManager.getLogWriter() != null) {
            printStackTrace(DriverManager.getLogWriter());
        }
    }

    public SQLException() {
        this.SQLState = null;
        this.vendorCode = 0;
        if (!(this instanceof SQLWarning) && DriverManager.getLogWriter() != null) {
            printStackTrace(DriverManager.getLogWriter());
        }
    }

    public SQLException(Throwable th) {
        super(th);
        if (!(this instanceof SQLWarning) && DriverManager.getLogWriter() != null) {
            printStackTrace(DriverManager.getLogWriter());
        }
    }

    public SQLException(String str, Throwable th) {
        super(str, th);
        if (!(this instanceof SQLWarning) && DriverManager.getLogWriter() != null) {
            printStackTrace(DriverManager.getLogWriter());
        }
    }

    public SQLException(String str, String str2, Throwable th) {
        super(str, th);
        this.SQLState = str2;
        this.vendorCode = 0;
        if (!(this instanceof SQLWarning) && DriverManager.getLogWriter() != null) {
            printStackTrace(DriverManager.getLogWriter());
            DriverManager.println("SQLState(" + this.SQLState + ")");
        }
    }

    public SQLException(String str, String str2, int i, Throwable th) {
        super(str, th);
        this.SQLState = str2;
        this.vendorCode = i;
        if (!(this instanceof SQLWarning) && DriverManager.getLogWriter() != null) {
            DriverManager.println("SQLState(" + this.SQLState + ") vendor code(" + i + ")");
            printStackTrace(DriverManager.getLogWriter());
        }
    }

    public String getSQLState() {
        return this.SQLState;
    }

    public int getErrorCode() {
        return this.vendorCode;
    }

    public SQLException getNextException() {
        return this.next;
    }

    public void setNextException(SQLException sQLException) {
        SQLException sQLException2 = this;
        while (true) {
            SQLException sQLException3 = sQLException2.next;
            if (sQLException3 != null) {
                sQLException2 = sQLException3;
            } else if (nextUpdater.compareAndSet(sQLException2, null, sQLException)) {
                return;
            } else {
                sQLException2 = sQLException2.next;
            }
        }
    }

    @Override
    public Iterator<Throwable> iterator() {
        return new Iterator<Throwable>() {
            Throwable cause;
            SQLException firstException;
            SQLException nextException;

            {
                this.firstException = SQLException.this;
                this.nextException = this.firstException.getNextException();
                this.cause = this.firstException.getCause();
            }

            @Override
            public boolean hasNext() {
                if (this.firstException != null || this.nextException != null || this.cause != null) {
                    return true;
                }
                return false;
            }

            @Override
            public Throwable next() {
                if (this.firstException != null) {
                    SQLException sQLException = this.firstException;
                    this.firstException = null;
                    return sQLException;
                }
                if (this.cause != null) {
                    Throwable th = this.cause;
                    this.cause = this.cause.getCause();
                    return th;
                }
                if (this.nextException != null) {
                    SQLException sQLException2 = this.nextException;
                    this.cause = this.nextException.getCause();
                    this.nextException = this.nextException.getNextException();
                    return sQLException2;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}

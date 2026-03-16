package java.sql;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SQLException extends Exception implements Serializable, Iterable<Throwable> {
    private static final long serialVersionUID = 2135244094396331484L;
    private String SQLState;
    private SQLException next;
    private int vendorCode;

    public SQLException() {
        this.SQLState = null;
        this.vendorCode = 0;
        this.next = null;
    }

    public SQLException(String theReason) {
        this(theReason, (String) null, 0);
    }

    public SQLException(String theReason, String theSQLState) {
        this(theReason, theSQLState, 0);
    }

    public SQLException(String theReason, String theSQLState, int theErrorCode) {
        super(theReason);
        this.SQLState = null;
        this.vendorCode = 0;
        this.next = null;
        this.SQLState = theSQLState;
        this.vendorCode = theErrorCode;
    }

    public SQLException(Throwable theCause) {
        this(theCause == null ? null : theCause.toString(), null, 0, theCause);
    }

    public SQLException(String theReason, Throwable theCause) {
        super(theReason, theCause);
        this.SQLState = null;
        this.vendorCode = 0;
        this.next = null;
    }

    public SQLException(String theReason, String theSQLState, Throwable theCause) {
        super(theReason, theCause);
        this.SQLState = null;
        this.vendorCode = 0;
        this.next = null;
        this.SQLState = theSQLState;
    }

    public SQLException(String theReason, String theSQLState, int theErrorCode, Throwable theCause) {
        this(theReason, theSQLState, theCause);
        this.vendorCode = theErrorCode;
    }

    public int getErrorCode() {
        return this.vendorCode;
    }

    public SQLException getNextException() {
        return this.next;
    }

    public String getSQLState() {
        return this.SQLState;
    }

    public void setNextException(SQLException ex) {
        if (this.next != null) {
            this.next.setNextException(ex);
        } else {
            this.next = ex;
        }
    }

    @Override
    public Iterator<Throwable> iterator() {
        return new InternalIterator(this);
    }

    private static class InternalIterator implements Iterator<Throwable> {
        private SQLException current;

        InternalIterator(SQLException e) {
            this.current = e;
        }

        @Override
        public boolean hasNext() {
            return this.current != null;
        }

        @Override
        public Throwable next() {
            if (this.current == null) {
                throw new NoSuchElementException();
            }
            SQLException ret = this.current;
            this.current = this.current.next;
            return ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}

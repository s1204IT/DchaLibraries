package mf.org.apache.xml.resolver;

public class CatalogException extends Exception {
    private Exception exception;
    private int exceptionType;

    public CatalogException(int type, String message) {
        super(message);
        this.exception = null;
        this.exceptionType = 0;
        this.exceptionType = type;
        this.exception = null;
    }

    public CatalogException(int type) {
        super("Catalog Exception " + type);
        this.exception = null;
        this.exceptionType = 0;
        this.exceptionType = type;
        this.exception = null;
    }

    public CatalogException(Exception e) {
        this.exception = null;
        this.exceptionType = 0;
        this.exceptionType = 1;
        this.exception = e;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (message == null && this.exception != null) {
            return this.exception.getMessage();
        }
        return message;
    }

    public int getExceptionType() {
        return this.exceptionType;
    }

    @Override
    public String toString() {
        if (this.exception != null) {
            return this.exception.toString();
        }
        return super.toString();
    }
}

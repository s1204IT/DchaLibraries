package mf.javax.xml.stream;

public class FactoryConfigurationError extends Error {
    private static final long serialVersionUID = -2994412584589975744L;
    Exception nested;

    public FactoryConfigurationError() {
    }

    public FactoryConfigurationError(Exception e, String msg) {
        super(msg);
        this.nested = e;
    }

    public FactoryConfigurationError(String msg) {
        super(msg);
    }

    @Override
    public Throwable getCause() {
        return this.nested;
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (msg == null && this.nested != null) {
            String msg2 = this.nested.getMessage();
            if (msg2 == null) {
                return this.nested.getClass().toString();
            }
            return msg2;
        }
        return msg;
    }
}

package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class PUserDatabase extends ParametersHeader implements PUserDatabaseHeader, SIPHeaderNamesIms, ExtensionHeader {
    private String databaseName;

    public PUserDatabase(String str) {
        super("P-User-Database");
        this.databaseName = str;
    }

    public PUserDatabase() {
        super("P-User-Database");
    }

    @Override
    public String getDatabaseName() {
        return this.databaseName;
    }

    @Override
    public void setDatabaseName(String str) {
        if (str == null || str.equals(Separators.SP)) {
            throw new NullPointerException("Database name is null");
        }
        if (!str.contains("aaa://")) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("aaa://");
            stringBuffer.append(str);
            this.databaseName = stringBuffer.toString();
            return;
        }
        this.databaseName = str;
    }

    @Override
    protected String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(Separators.LESS_THAN);
        if (getDatabaseName() != null) {
            stringBuffer.append(getDatabaseName());
        }
        if (!this.parameters.isEmpty()) {
            stringBuffer.append(Separators.SEMICOLON + this.parameters.encode());
        }
        stringBuffer.append(Separators.GREATER_THAN);
        return stringBuffer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof PUserDatabaseHeader) && super.equals(obj);
    }

    @Override
    public Object clone() {
        return (PUserDatabase) super.clone();
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }
}

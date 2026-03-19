package gov.nist.javax.sip.header;

import gov.nist.core.DuplicateNameValueList;
import gov.nist.core.NameValue;
import gov.nist.core.NameValueList;
import gov.nist.javax.sip.address.GenericURI;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Iterator;
import javax.sip.header.Parameters;

public abstract class ParametersHeader extends SIPHeader implements Parameters, Serializable {
    protected DuplicateNameValueList duplicates;
    protected NameValueList parameters;

    @Override
    protected abstract String encodeBody();

    protected ParametersHeader() {
        this.parameters = new NameValueList();
        this.duplicates = new DuplicateNameValueList();
    }

    protected ParametersHeader(String str) {
        super(str);
        this.parameters = new NameValueList();
        this.duplicates = new DuplicateNameValueList();
    }

    protected ParametersHeader(String str, boolean z) {
        super(str);
        this.parameters = new NameValueList(z);
        this.duplicates = new DuplicateNameValueList();
    }

    @Override
    public String getParameter(String str) {
        return this.parameters.getParameter(str);
    }

    public Object getParameterValue(String str) {
        return this.parameters.getValue(str);
    }

    @Override
    public Iterator<String> getParameterNames() {
        return this.parameters.getNames();
    }

    public boolean hasParameters() {
        return (this.parameters == null || this.parameters.isEmpty()) ? false : true;
    }

    @Override
    public void removeParameter(String str) {
        this.parameters.delete(str);
    }

    public void setParameter(String str, String str2) throws ParseException {
        NameValue nameValue = this.parameters.getNameValue(str);
        if (nameValue != null) {
            nameValue.setValueAsObject(str2);
        } else {
            this.parameters.set(new NameValue(str, str2));
        }
    }

    public void setQuotedParameter(String str, String str2) throws ParseException {
        NameValue nameValue = this.parameters.getNameValue(str);
        if (nameValue != null) {
            nameValue.setValueAsObject(str2);
            nameValue.setQuotedValue();
        } else {
            NameValue nameValue2 = new NameValue(str, str2);
            nameValue2.setQuotedValue();
            this.parameters.set(nameValue2);
        }
    }

    protected void setParameter(String str, int i) {
        this.parameters.set(str, Integer.valueOf(i));
    }

    protected void setParameter(String str, boolean z) {
        this.parameters.set(str, Boolean.valueOf(z));
    }

    protected void setParameter(String str, float f) {
        Float fValueOf = Float.valueOf(f);
        NameValue nameValue = this.parameters.getNameValue(str);
        if (nameValue != null) {
            nameValue.setValueAsObject(fValueOf);
        } else {
            this.parameters.set(new NameValue(str, fValueOf));
        }
    }

    protected void setParameter(String str, Object obj) {
        this.parameters.set(str, obj);
    }

    public boolean hasParameter(String str) {
        return this.parameters.hasNameValue(str);
    }

    public void removeParameters() {
        this.parameters = new NameValueList();
    }

    public NameValueList getParameters() {
        return this.parameters;
    }

    public void setParameter(NameValue nameValue) {
        this.parameters.set(nameValue);
    }

    public void setParameters(NameValueList nameValueList) {
        this.parameters = nameValueList;
    }

    protected int getParameterAsInt(String str) {
        if (getParameterValue(str) == null) {
            return -1;
        }
        try {
            if (getParameterValue(str) instanceof String) {
                return Integer.parseInt(getParameter(str));
            }
            return ((Integer) getParameterValue(str)).intValue();
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected int getParameterAsHexInt(String str) {
        if (getParameterValue(str) == null) {
            return -1;
        }
        try {
            if (getParameterValue(str) instanceof String) {
                return Integer.parseInt(getParameter(str), 16);
            }
            return ((Integer) getParameterValue(str)).intValue();
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected float getParameterAsFloat(String str) {
        if (getParameterValue(str) == null) {
            return -1.0f;
        }
        try {
            if (getParameterValue(str) instanceof String) {
                return Float.parseFloat(getParameter(str));
            }
            return ((Float) getParameterValue(str)).floatValue();
        } catch (NumberFormatException e) {
            return -1.0f;
        }
    }

    protected long getParameterAsLong(String str) {
        if (getParameterValue(str) == null) {
            return -1L;
        }
        try {
            if (getParameterValue(str) instanceof String) {
                return Long.parseLong(getParameter(str));
            }
            return ((Long) getParameterValue(str)).longValue();
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    protected GenericURI getParameterAsURI(String str) {
        Object parameterValue = getParameterValue(str);
        if (parameterValue instanceof GenericURI) {
            return (GenericURI) parameterValue;
        }
        try {
            return new GenericURI((String) parameterValue);
        } catch (ParseException e) {
            return null;
        }
    }

    protected boolean getParameterAsBoolean(String str) {
        Object parameterValue = getParameterValue(str);
        if (parameterValue == null) {
            return false;
        }
        if (parameterValue instanceof Boolean) {
            return ((Boolean) parameterValue).booleanValue();
        }
        if (!(parameterValue instanceof String)) {
            return false;
        }
        return Boolean.valueOf((String) parameterValue).booleanValue();
    }

    public NameValue getNameValue(String str) {
        return this.parameters.getNameValue(str);
    }

    @Override
    public Object clone() {
        ParametersHeader parametersHeader = (ParametersHeader) super.clone();
        if (this.parameters != null) {
            parametersHeader.parameters = (NameValueList) this.parameters.clone();
        }
        return parametersHeader;
    }

    public void setMultiParameter(String str, String str2) {
        NameValue nameValue = new NameValue();
        nameValue.setName(str);
        nameValue.setValue(str2);
        this.duplicates.set(nameValue);
    }

    public void setMultiParameter(NameValue nameValue) {
        this.duplicates.set(nameValue);
    }

    public String getMultiParameter(String str) {
        return this.duplicates.getParameter(str);
    }

    public DuplicateNameValueList getMultiParameters() {
        return this.duplicates;
    }

    public Object getMultiParameterValue(String str) {
        return this.duplicates.getValue(str);
    }

    public Iterator<String> getMultiParameterNames() {
        return this.duplicates.getNames();
    }

    public boolean hasMultiParameters() {
        return (this.duplicates == null || this.duplicates.isEmpty()) ? false : true;
    }

    public void removeMultiParameter(String str) {
        this.duplicates.delete(str);
    }

    public boolean hasMultiParameter(String str) {
        return this.duplicates.hasNameValue(str);
    }

    public void removeMultiParameters() {
        this.duplicates = new DuplicateNameValueList();
    }

    protected final boolean equalParameters(Parameters parameters) {
        if (this == parameters) {
            return true;
        }
        Iterator<String> parameterNames = getParameterNames();
        while (parameterNames.hasNext()) {
            String next = parameterNames.next();
            String parameter = getParameter(next);
            String parameter2 = parameters.getParameter(next);
            if ((parameter == null) ^ (parameter2 == null)) {
                return false;
            }
            if (parameter != null && !parameter.equalsIgnoreCase(parameter2)) {
                return false;
            }
        }
        Iterator parameterNames2 = parameters.getParameterNames();
        while (parameterNames2.hasNext()) {
            String str = (String) parameterNames2.next();
            String parameter3 = parameters.getParameter(str);
            String parameter4 = getParameter(str);
            if ((parameter3 == null) ^ (parameter4 == null)) {
                return false;
            }
            if (parameter3 != null && !parameter3.equalsIgnoreCase(parameter4)) {
                return false;
            }
        }
        return true;
    }
}

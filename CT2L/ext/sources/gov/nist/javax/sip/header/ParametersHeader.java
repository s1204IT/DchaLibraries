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

    protected ParametersHeader(String hdrName) {
        super(hdrName);
        this.parameters = new NameValueList();
        this.duplicates = new DuplicateNameValueList();
    }

    protected ParametersHeader(String hdrName, boolean sync) {
        super(hdrName);
        this.parameters = new NameValueList(sync);
        this.duplicates = new DuplicateNameValueList();
    }

    @Override
    public String getParameter(String name) {
        return this.parameters.getParameter(name);
    }

    public Object getParameterValue(String name) {
        return this.parameters.getValue(name);
    }

    @Override
    public Iterator<String> getParameterNames() {
        return this.parameters.getNames();
    }

    public boolean hasParameters() {
        return (this.parameters == null || this.parameters.isEmpty()) ? false : true;
    }

    @Override
    public void removeParameter(String name) {
        this.parameters.delete(name);
    }

    public void setParameter(String name, String value) throws ParseException {
        NameValue nv = this.parameters.getNameValue(name);
        if (nv != null) {
            nv.setValueAsObject(value);
        } else {
            this.parameters.set(new NameValue(name, value));
        }
    }

    public void setQuotedParameter(String name, String value) throws ParseException {
        NameValue nv = this.parameters.getNameValue(name);
        if (nv != null) {
            nv.setValueAsObject(value);
            nv.setQuotedValue();
        } else {
            NameValue nv2 = new NameValue(name, value);
            nv2.setQuotedValue();
            this.parameters.set(nv2);
        }
    }

    protected void setParameter(String name, int value) {
        Integer val = Integer.valueOf(value);
        this.parameters.set(name, val);
    }

    protected void setParameter(String name, boolean value) {
        Boolean val = Boolean.valueOf(value);
        this.parameters.set(name, val);
    }

    protected void setParameter(String name, float value) {
        Float val = Float.valueOf(value);
        NameValue nv = this.parameters.getNameValue(name);
        if (nv != null) {
            nv.setValueAsObject(val);
        } else {
            this.parameters.set(new NameValue(name, val));
        }
    }

    protected void setParameter(String name, Object value) {
        this.parameters.set(name, value);
    }

    public boolean hasParameter(String parameterName) {
        return this.parameters.hasNameValue(parameterName);
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

    public void setParameters(NameValueList parameters) {
        this.parameters = parameters;
    }

    protected int getParameterAsInt(String parameterName) {
        int iIntValue;
        if (getParameterValue(parameterName) == null) {
            return -1;
        }
        try {
            if (getParameterValue(parameterName) instanceof String) {
                iIntValue = Integer.parseInt(getParameter(parameterName));
            } else {
                iIntValue = ((Integer) getParameterValue(parameterName)).intValue();
            }
            return iIntValue;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected int getParameterAsHexInt(String parameterName) {
        int iIntValue;
        if (getParameterValue(parameterName) == null) {
            return -1;
        }
        try {
            if (getParameterValue(parameterName) instanceof String) {
                iIntValue = Integer.parseInt(getParameter(parameterName), 16);
            } else {
                iIntValue = ((Integer) getParameterValue(parameterName)).intValue();
            }
            return iIntValue;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected float getParameterAsFloat(String parameterName) {
        float fFloatValue;
        if (getParameterValue(parameterName) == null) {
            return -1.0f;
        }
        try {
            if (getParameterValue(parameterName) instanceof String) {
                fFloatValue = Float.parseFloat(getParameter(parameterName));
            } else {
                fFloatValue = ((Float) getParameterValue(parameterName)).floatValue();
            }
            return fFloatValue;
        } catch (NumberFormatException e) {
            return -1.0f;
        }
    }

    protected long getParameterAsLong(String parameterName) {
        long jLongValue = -1;
        if (getParameterValue(parameterName) != null) {
            try {
                if (getParameterValue(parameterName) instanceof String) {
                    jLongValue = Long.parseLong(getParameter(parameterName));
                } else {
                    jLongValue = ((Long) getParameterValue(parameterName)).longValue();
                }
            } catch (NumberFormatException e) {
            }
        }
        return jLongValue;
    }

    protected GenericURI getParameterAsURI(String parameterName) {
        Object val = getParameterValue(parameterName);
        if (val instanceof GenericURI) {
            return (GenericURI) val;
        }
        try {
            return new GenericURI((String) val);
        } catch (ParseException e) {
            return null;
        }
    }

    protected boolean getParameterAsBoolean(String parameterName) {
        Object val = getParameterValue(parameterName);
        if (val == null) {
            return false;
        }
        if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue();
        }
        if (val instanceof String) {
            return Boolean.valueOf((String) val).booleanValue();
        }
        return false;
    }

    public NameValue getNameValue(String parameterName) {
        return this.parameters.getNameValue(parameterName);
    }

    @Override
    public Object clone() {
        ParametersHeader retval = (ParametersHeader) super.clone();
        if (this.parameters != null) {
            retval.parameters = (NameValueList) this.parameters.clone();
        }
        return retval;
    }

    public void setMultiParameter(String name, String value) {
        NameValue nv = new NameValue();
        nv.setName(name);
        nv.setValue(value);
        this.duplicates.set(nv);
    }

    public void setMultiParameter(NameValue nameValue) {
        this.duplicates.set(nameValue);
    }

    public String getMultiParameter(String name) {
        return this.duplicates.getParameter(name);
    }

    public DuplicateNameValueList getMultiParameters() {
        return this.duplicates;
    }

    public Object getMultiParameterValue(String name) {
        return this.duplicates.getValue(name);
    }

    public Iterator<String> getMultiParameterNames() {
        return this.duplicates.getNames();
    }

    public boolean hasMultiParameters() {
        return (this.duplicates == null || this.duplicates.isEmpty()) ? false : true;
    }

    public void removeMultiParameter(String name) {
        this.duplicates.delete(name);
    }

    public boolean hasMultiParameter(String parameterName) {
        return this.duplicates.hasNameValue(parameterName);
    }

    public void removeMultiParameters() {
        this.duplicates = new DuplicateNameValueList();
    }

    protected final boolean equalParameters(Parameters other) {
        if (this == other) {
            return true;
        }
        Iterator<String> parameterNames = getParameterNames();
        while (parameterNames.hasNext()) {
            String pname = parameterNames.next();
            String p1 = getParameter(pname);
            String p2 = other.getParameter(pname);
            if ((p2 == null) ^ (p1 == null)) {
                return false;
            }
            if (p1 != null && !p1.equalsIgnoreCase(p2)) {
                return false;
            }
        }
        Iterator i = other.getParameterNames();
        while (i.hasNext()) {
            String pname2 = (String) i.next();
            String p12 = other.getParameter(pname2);
            String p22 = getParameter(pname2);
            if ((p22 == null) ^ (p12 == null)) {
                return false;
            }
            if (p12 != null && !p12.equalsIgnoreCase(p22)) {
                return false;
            }
        }
        return true;
    }
}

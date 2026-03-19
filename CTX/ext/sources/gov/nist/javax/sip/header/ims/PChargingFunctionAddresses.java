package gov.nist.javax.sip.header.ims;

import gov.nist.core.NameValue;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.sip.header.ExtensionHeader;

public class PChargingFunctionAddresses extends ParametersHeader implements PChargingFunctionAddressesHeader, SIPHeaderNamesIms, ExtensionHeader {
    public PChargingFunctionAddresses() {
        super("P-Charging-Function-Addresses");
    }

    @Override
    protected String encodeBody() {
        StringBuffer stringBuffer = new StringBuffer();
        if (!this.duplicates.isEmpty()) {
            stringBuffer.append(this.duplicates.encode());
        }
        return stringBuffer.toString();
    }

    @Override
    public void setChargingCollectionFunctionAddress(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setChargingCollectionFunctionAddress(), the ccfAddress parameter is null.");
        }
        setMultiParameter(ParameterNamesIms.CCF, str);
    }

    @Override
    public void addChargingCollectionFunctionAddress(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setChargingCollectionFunctionAddress(), the ccfAddress parameter is null.");
        }
        this.parameters.set(ParameterNamesIms.CCF, str);
    }

    @Override
    public void removeChargingCollectionFunctionAddress(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setChargingCollectionFunctionAddress(), the ccfAddress parameter is null.");
        }
        if (!delete(str, ParameterNamesIms.CCF)) {
            throw new ParseException("CCF Address Not Removed", 0);
        }
    }

    @Override
    public ListIterator getChargingCollectionFunctionAddresses() {
        LinkedList linkedList = new LinkedList();
        for (NameValue nameValue : this.parameters) {
            if (nameValue.getName().equalsIgnoreCase(ParameterNamesIms.CCF)) {
                NameValue nameValue2 = new NameValue();
                nameValue2.setName(nameValue.getName());
                nameValue2.setValueAsObject(nameValue.getValueAsObject());
                linkedList.add(nameValue2);
            }
        }
        return linkedList.listIterator();
    }

    @Override
    public void setEventChargingFunctionAddress(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setEventChargingFunctionAddress(), the ecfAddress parameter is null.");
        }
        setMultiParameter(ParameterNamesIms.ECF, str);
    }

    @Override
    public void addEventChargingFunctionAddress(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setEventChargingFunctionAddress(), the ecfAddress parameter is null.");
        }
        this.parameters.set(ParameterNamesIms.ECF, str);
    }

    @Override
    public void removeEventChargingFunctionAddress(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setEventChargingFunctionAddress(), the ecfAddress parameter is null.");
        }
        if (!delete(str, ParameterNamesIms.ECF)) {
            throw new ParseException("ECF Address Not Removed", 0);
        }
    }

    @Override
    public ListIterator<NameValue> getEventChargingFunctionAddresses() {
        LinkedList linkedList = new LinkedList();
        ListIterator<NameValue> listIterator = linkedList.listIterator();
        for (NameValue nameValue : this.parameters) {
            if (nameValue.getName().equalsIgnoreCase(ParameterNamesIms.ECF)) {
                NameValue nameValue2 = new NameValue();
                nameValue2.setName(nameValue.getName());
                nameValue2.setValueAsObject(nameValue.getValueAsObject());
                listIterator.add(nameValue2);
            }
        }
        return listIterator;
    }

    public boolean delete(String str, String str2) {
        Iterator<NameValue> it = this.parameters.iterator();
        boolean z = false;
        while (it.hasNext()) {
            NameValue next = it.next();
            if (((String) next.getValueAsObject()).equalsIgnoreCase(str) && next.getName().equalsIgnoreCase(str2)) {
                it.remove();
                z = true;
            }
        }
        return z;
    }

    @Override
    public void setValue(String str) throws ParseException {
        throw new ParseException(str, 0);
    }
}

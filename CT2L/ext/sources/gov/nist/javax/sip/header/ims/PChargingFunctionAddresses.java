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
        StringBuffer encoding = new StringBuffer();
        if (!this.duplicates.isEmpty()) {
            encoding.append(this.duplicates.encode());
        }
        return encoding.toString();
    }

    @Override
    public void setChargingCollectionFunctionAddress(String ccfAddress) throws ParseException {
        if (ccfAddress == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setChargingCollectionFunctionAddress(), the ccfAddress parameter is null.");
        }
        setMultiParameter(ParameterNamesIms.CCF, ccfAddress);
    }

    @Override
    public void addChargingCollectionFunctionAddress(String ccfAddress) throws ParseException {
        if (ccfAddress == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setChargingCollectionFunctionAddress(), the ccfAddress parameter is null.");
        }
        this.parameters.set(ParameterNamesIms.CCF, ccfAddress);
    }

    @Override
    public void removeChargingCollectionFunctionAddress(String ccfAddress) throws ParseException {
        if (ccfAddress == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setChargingCollectionFunctionAddress(), the ccfAddress parameter is null.");
        }
        if (!delete(ccfAddress, ParameterNamesIms.CCF)) {
            throw new ParseException("CCF Address Not Removed", 0);
        }
    }

    @Override
    public ListIterator getChargingCollectionFunctionAddresses() {
        LinkedList ccfLIST = new LinkedList();
        for (NameValue nv : this.parameters) {
            if (nv.getName().equalsIgnoreCase(ParameterNamesIms.CCF)) {
                NameValue ccfNV = new NameValue();
                ccfNV.setName(nv.getName());
                ccfNV.setValueAsObject(nv.getValueAsObject());
                ccfLIST.add(ccfNV);
            }
        }
        return ccfLIST.listIterator();
    }

    @Override
    public void setEventChargingFunctionAddress(String ecfAddress) throws ParseException {
        if (ecfAddress == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setEventChargingFunctionAddress(), the ecfAddress parameter is null.");
        }
        setMultiParameter(ParameterNamesIms.ECF, ecfAddress);
    }

    @Override
    public void addEventChargingFunctionAddress(String ecfAddress) throws ParseException {
        if (ecfAddress == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setEventChargingFunctionAddress(), the ecfAddress parameter is null.");
        }
        this.parameters.set(ParameterNamesIms.ECF, ecfAddress);
    }

    @Override
    public void removeEventChargingFunctionAddress(String ecfAddress) throws ParseException {
        if (ecfAddress == null) {
            throw new NullPointerException("JAIN-SIP Exception, P-Charging-Function-Addresses, setEventChargingFunctionAddress(), the ecfAddress parameter is null.");
        }
        if (!delete(ecfAddress, ParameterNamesIms.ECF)) {
            throw new ParseException("ECF Address Not Removed", 0);
        }
    }

    @Override
    public ListIterator<NameValue> getEventChargingFunctionAddresses() {
        LinkedList<NameValue> listw = new LinkedList<>();
        ListIterator<NameValue> ecfLIST = listw.listIterator();
        for (NameValue nv : this.parameters) {
            if (nv.getName().equalsIgnoreCase(ParameterNamesIms.ECF)) {
                NameValue ecfNV = new NameValue();
                ecfNV.setName(nv.getName());
                ecfNV.setValueAsObject(nv.getValueAsObject());
                ecfLIST.add(ecfNV);
            }
        }
        return ecfLIST;
    }

    public boolean delete(String value, String name) {
        Iterator<NameValue> it = this.parameters.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            NameValue nv = it.next();
            if (((String) nv.getValueAsObject()).equalsIgnoreCase(value) && nv.getName().equalsIgnoreCase(name)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    @Override
    public void setValue(String value) throws ParseException {
        throw new ParseException(value, 0);
    }
}

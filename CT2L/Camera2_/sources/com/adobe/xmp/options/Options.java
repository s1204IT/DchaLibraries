package com.adobe.xmp.options;

import com.adobe.xmp.XMPException;
import java.util.HashMap;
import java.util.Map;

public abstract class Options {
    private int options = 0;
    private Map optionNames = null;

    protected abstract String defineOptionName(int i);

    protected abstract int getValidOptions();

    public Options() {
    }

    public Options(int options) throws XMPException {
        assertOptionsValid(options);
        setOptions(options);
    }

    public void clear() {
        this.options = 0;
    }

    public boolean isExactly(int optionBits) {
        return getOptions() == optionBits;
    }

    public boolean containsAllOptions(int optionBits) {
        return (getOptions() & optionBits) == optionBits;
    }

    public boolean containsOneOf(int optionBits) {
        return (getOptions() & optionBits) != 0;
    }

    protected boolean getOption(int optionBit) {
        return (this.options & optionBit) != 0;
    }

    public void setOption(int optionBits, boolean value) {
        this.options = value ? this.options | optionBits : this.options & (optionBits ^ (-1));
    }

    public int getOptions() {
        return this.options;
    }

    public void setOptions(int options) throws XMPException {
        assertOptionsValid(options);
        this.options = options;
    }

    public boolean equals(Object obj) {
        return getOptions() == ((Options) obj).getOptions();
    }

    public int hashCode() {
        return getOptions();
    }

    public String getOptionsString() {
        if (this.options == 0) {
            return "<none>";
        }
        StringBuffer sb = new StringBuffer();
        int theBits = this.options;
        while (theBits != 0) {
            int oneLessBit = theBits & (theBits - 1);
            int singleBit = theBits ^ oneLessBit;
            String bitName = getOptionName(singleBit);
            sb.append(bitName);
            if (oneLessBit != 0) {
                sb.append(" | ");
            }
            theBits = oneLessBit;
        }
        return sb.toString();
    }

    public String toString() {
        return "0x" + Integer.toHexString(this.options);
    }

    protected void assertConsistency(int options) throws XMPException {
    }

    private void assertOptionsValid(int options) throws XMPException {
        int invalidOptions = options & (getValidOptions() ^ (-1));
        if (invalidOptions == 0) {
            assertConsistency(options);
            return;
        }
        throw new XMPException("The option bit(s) 0x" + Integer.toHexString(invalidOptions) + " are invalid!", 103);
    }

    private String getOptionName(int option) {
        Map optionsNames = procureOptionNames();
        Integer key = new Integer(option);
        String result = (String) optionsNames.get(key);
        if (result == null) {
            String result2 = defineOptionName(option);
            if (result2 != null) {
                optionsNames.put(key, result2);
                return result2;
            }
            return "<option name not defined>";
        }
        return result;
    }

    private Map procureOptionNames() {
        if (this.optionNames == null) {
            this.optionNames = new HashMap();
        }
        return this.optionNames;
    }
}

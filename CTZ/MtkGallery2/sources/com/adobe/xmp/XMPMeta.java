package com.adobe.xmp;

public interface XMPMeta extends Cloneable {
    Object clone();

    boolean doesPropertyExist(String str, String str2);

    Integer getPropertyInteger(String str, String str2) throws XMPException;

    String getPropertyString(String str, String str2) throws XMPException;

    void setProperty(String str, String str2, Object obj) throws XMPException;
}

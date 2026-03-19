package com.android.org.bouncycastle.asn1.x500;

import com.android.org.bouncycastle.asn1.ASN1Choice;
import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1String;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.DERBMPString;
import com.android.org.bouncycastle.asn1.DERPrintableString;
import com.android.org.bouncycastle.asn1.DERT61String;
import com.android.org.bouncycastle.asn1.DERUTF8String;
import com.android.org.bouncycastle.asn1.DERUniversalString;

public class DirectoryString extends ASN1Object implements ASN1Choice, ASN1String {
    private ASN1String string;

    public static DirectoryString getInstance(Object obj) {
        if (obj == null || (obj instanceof DirectoryString)) {
            return (DirectoryString) obj;
        }
        if (obj instanceof DERT61String) {
            return new DirectoryString((DERT61String) obj);
        }
        if (obj instanceof DERPrintableString) {
            return new DirectoryString((DERPrintableString) obj);
        }
        if (obj instanceof DERUniversalString) {
            return new DirectoryString((DERUniversalString) obj);
        }
        if (obj instanceof DERUTF8String) {
            return new DirectoryString((DERUTF8String) obj);
        }
        if (obj instanceof DERBMPString) {
            return new DirectoryString((DERBMPString) obj);
        }
        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static DirectoryString getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        if (!z) {
            throw new IllegalArgumentException("choice item must be explicitly tagged");
        }
        return getInstance(aSN1TaggedObject.getObject());
    }

    private DirectoryString(DERT61String dERT61String) {
        this.string = dERT61String;
    }

    private DirectoryString(DERPrintableString dERPrintableString) {
        this.string = dERPrintableString;
    }

    private DirectoryString(DERUniversalString dERUniversalString) {
        this.string = dERUniversalString;
    }

    private DirectoryString(DERUTF8String dERUTF8String) {
        this.string = dERUTF8String;
    }

    private DirectoryString(DERBMPString dERBMPString) {
        this.string = dERBMPString;
    }

    public DirectoryString(String str) {
        this.string = new DERUTF8String(str);
    }

    @Override
    public String getString() {
        return this.string.getString();
    }

    public String toString() {
        return this.string.getString();
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        return ((ASN1Encodable) this.string).toASN1Primitive();
    }
}

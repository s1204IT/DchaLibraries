package com.android.org.bouncycastle.asn1.util;

import com.android.org.bouncycastle.asn1.ASN1ApplicationSpecific;
import com.android.org.bouncycastle.asn1.ASN1Boolean;
import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1Encoding;
import com.android.org.bouncycastle.asn1.ASN1Enumerated;
import com.android.org.bouncycastle.asn1.ASN1GeneralizedTime;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1Set;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.ASN1UTCTime;
import com.android.org.bouncycastle.asn1.BERApplicationSpecific;
import com.android.org.bouncycastle.asn1.BEROctetString;
import com.android.org.bouncycastle.asn1.BERSequence;
import com.android.org.bouncycastle.asn1.BERSet;
import com.android.org.bouncycastle.asn1.BERTaggedObject;
import com.android.org.bouncycastle.asn1.DERApplicationSpecific;
import com.android.org.bouncycastle.asn1.DERBMPString;
import com.android.org.bouncycastle.asn1.DERBitString;
import com.android.org.bouncycastle.asn1.DERExternal;
import com.android.org.bouncycastle.asn1.DERGraphicString;
import com.android.org.bouncycastle.asn1.DERIA5String;
import com.android.org.bouncycastle.asn1.DERNull;
import com.android.org.bouncycastle.asn1.DERPrintableString;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.DERT61String;
import com.android.org.bouncycastle.asn1.DERUTF8String;
import com.android.org.bouncycastle.asn1.DERVideotexString;
import com.android.org.bouncycastle.asn1.DERVisibleString;
import com.android.org.bouncycastle.util.Strings;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.io.IOException;
import java.util.Enumeration;

public class ASN1Dump {
    private static final int SAMPLE_SIZE = 32;
    private static final String TAB = "    ";

    static void _dumpAsString(String indent, boolean verbose, ASN1Primitive aSN1Primitive, StringBuffer buf) {
        String nl = Strings.lineSeparator();
        if (aSN1Primitive instanceof ASN1Sequence) {
            Enumeration e = aSN1Primitive.getObjects();
            String tab = indent + TAB;
            buf.append(indent);
            if (aSN1Primitive instanceof BERSequence) {
                buf.append("BER Sequence");
            } else if (aSN1Primitive instanceof DERSequence) {
                buf.append("DER Sequence");
            } else {
                buf.append("Sequence");
            }
            buf.append(nl);
            while (e.hasMoreElements()) {
                ?? NextElement = e.nextElement();
                if (NextElement == 0 || NextElement.equals(DERNull.INSTANCE)) {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(nl);
                } else if (NextElement instanceof ASN1Primitive) {
                    _dumpAsString(tab, verbose, NextElement, buf);
                } else {
                    _dumpAsString(tab, verbose, ((ASN1Encodable) NextElement).toASN1Primitive(), buf);
                }
            }
            return;
        }
        if (aSN1Primitive instanceof ASN1TaggedObject) {
            String tab2 = indent + TAB;
            buf.append(indent);
            if (aSN1Primitive instanceof BERTaggedObject) {
                buf.append("BER Tagged [");
            } else {
                buf.append("Tagged [");
            }
            buf.append(Integer.toString(aSN1Primitive.getTagNo()));
            buf.append(']');
            if (!aSN1Primitive.isExplicit()) {
                buf.append(" IMPLICIT ");
            }
            buf.append(nl);
            if (aSN1Primitive.isEmpty()) {
                buf.append(tab2);
                buf.append("EMPTY");
                buf.append(nl);
                return;
            }
            _dumpAsString(tab2, verbose, aSN1Primitive.getObject(), buf);
            return;
        }
        if (aSN1Primitive instanceof ASN1Set) {
            Enumeration e2 = aSN1Primitive.getObjects();
            String tab3 = indent + TAB;
            buf.append(indent);
            if (aSN1Primitive instanceof BERSet) {
                buf.append("BER Set");
            } else {
                buf.append("DER Set");
            }
            buf.append(nl);
            while (e2.hasMoreElements()) {
                ?? NextElement2 = e2.nextElement();
                if (NextElement2 == 0) {
                    buf.append(tab3);
                    buf.append("NULL");
                    buf.append(nl);
                } else if (NextElement2 instanceof ASN1Primitive) {
                    _dumpAsString(tab3, verbose, NextElement2, buf);
                } else {
                    _dumpAsString(tab3, verbose, ((ASN1Encodable) NextElement2).toASN1Primitive(), buf);
                }
            }
            return;
        }
        if (aSN1Primitive instanceof ASN1OctetString) {
            if (aSN1Primitive instanceof BEROctetString) {
                buf.append(indent + "BER Constructed Octet String[" + aSN1Primitive.getOctets().length + "] ");
            } else {
                buf.append(indent + "DER Octet String[" + aSN1Primitive.getOctets().length + "] ");
            }
            if (verbose) {
                buf.append(dumpBinaryDataAsString(indent, aSN1Primitive.getOctets()));
                return;
            } else {
                buf.append(nl);
                return;
            }
        }
        if (aSN1Primitive instanceof ASN1ObjectIdentifier) {
            buf.append(indent + "ObjectIdentifier(" + aSN1Primitive.getId() + ")" + nl);
            return;
        }
        if (aSN1Primitive instanceof ASN1Boolean) {
            buf.append(indent + "Boolean(" + aSN1Primitive.isTrue() + ")" + nl);
            return;
        }
        if (aSN1Primitive instanceof ASN1Integer) {
            buf.append(indent + "Integer(" + aSN1Primitive.getValue() + ")" + nl);
            return;
        }
        if (aSN1Primitive instanceof DERBitString) {
            buf.append(indent + "DER Bit String[" + aSN1Primitive.getBytes().length + ", " + aSN1Primitive.getPadBits() + "] ");
            if (verbose) {
                buf.append(dumpBinaryDataAsString(indent, aSN1Primitive.getBytes()));
                return;
            } else {
                buf.append(nl);
                return;
            }
        }
        if (aSN1Primitive instanceof DERIA5String) {
            buf.append(indent + "IA5String(" + aSN1Primitive.getString() + ") " + nl);
            return;
        }
        if (aSN1Primitive instanceof DERUTF8String) {
            buf.append(indent + "UTF8String(" + aSN1Primitive.getString() + ") " + nl);
            return;
        }
        if (aSN1Primitive instanceof DERPrintableString) {
            buf.append(indent + "PrintableString(" + aSN1Primitive.getString() + ") " + nl);
            return;
        }
        if (aSN1Primitive instanceof DERVisibleString) {
            buf.append(indent + "VisibleString(" + aSN1Primitive.getString() + ") " + nl);
            return;
        }
        if (aSN1Primitive instanceof DERBMPString) {
            buf.append(indent + "BMPString(" + aSN1Primitive.getString() + ") " + nl);
            return;
        }
        if (aSN1Primitive instanceof DERT61String) {
            buf.append(indent + "T61String(" + aSN1Primitive.getString() + ") " + nl);
            return;
        }
        if (aSN1Primitive instanceof DERGraphicString) {
            buf.append(indent + "GraphicString(" + aSN1Primitive.getString() + ") " + nl);
            return;
        }
        if (aSN1Primitive instanceof DERVideotexString) {
            buf.append(indent + "VideotexString(" + aSN1Primitive.getString() + ") " + nl);
            return;
        }
        if (aSN1Primitive instanceof ASN1UTCTime) {
            buf.append(indent + "UTCTime(" + aSN1Primitive.getTime() + ") " + nl);
            return;
        }
        if (aSN1Primitive instanceof ASN1GeneralizedTime) {
            buf.append(indent + "GeneralizedTime(" + aSN1Primitive.getTime() + ") " + nl);
            return;
        }
        if (aSN1Primitive instanceof BERApplicationSpecific) {
            buf.append(outputApplicationSpecific(ASN1Encoding.BER, indent, verbose, aSN1Primitive, nl));
            return;
        }
        if (aSN1Primitive instanceof DERApplicationSpecific) {
            buf.append(outputApplicationSpecific(ASN1Encoding.DER, indent, verbose, aSN1Primitive, nl));
            return;
        }
        if (aSN1Primitive instanceof ASN1Enumerated) {
            buf.append(indent + "DER Enumerated(" + aSN1Primitive.getValue() + ")" + nl);
            return;
        }
        if (aSN1Primitive instanceof DERExternal) {
            buf.append(indent + "External " + nl);
            String tab4 = indent + TAB;
            if (aSN1Primitive.getDirectReference() != null) {
                buf.append(tab4 + "Direct Reference: " + aSN1Primitive.getDirectReference().getId() + nl);
            }
            if (aSN1Primitive.getIndirectReference() != null) {
                buf.append(tab4 + "Indirect Reference: " + aSN1Primitive.getIndirectReference().toString() + nl);
            }
            if (aSN1Primitive.getDataValueDescriptor() != null) {
                _dumpAsString(tab4, verbose, aSN1Primitive.getDataValueDescriptor(), buf);
            }
            buf.append(tab4 + "Encoding: " + aSN1Primitive.getEncoding() + nl);
            _dumpAsString(tab4, verbose, aSN1Primitive.getExternalContent(), buf);
            return;
        }
        buf.append(indent + aSN1Primitive.toString() + nl);
    }

    private static String outputApplicationSpecific(String type, String indent, boolean verbose, ASN1Primitive obj, String nl) throws IOException {
        ASN1ApplicationSpecific app = ASN1ApplicationSpecific.getInstance(obj);
        StringBuffer buf = new StringBuffer();
        if (app.isConstructed()) {
            try {
                ASN1Sequence s = ASN1Sequence.getInstance(app.getObject(16));
                buf.append(indent + type + " ApplicationSpecific[" + app.getApplicationTag() + "]" + nl);
                Enumeration e = s.getObjects();
                while (e.hasMoreElements()) {
                    _dumpAsString(indent + TAB, verbose, (ASN1Primitive) e.nextElement(), buf);
                }
            } catch (IOException e2) {
                buf.append(e2);
            }
            return buf.toString();
        }
        return indent + type + " ApplicationSpecific[" + app.getApplicationTag() + "] (" + new String(Hex.encode(app.getContents())) + ")" + nl;
    }

    public static String dumpAsString(Object obj) {
        return dumpAsString(obj, false);
    }

    public static String dumpAsString(Object obj, boolean verbose) {
        StringBuffer buf = new StringBuffer();
        if (obj instanceof ASN1Primitive) {
            _dumpAsString("", verbose, obj, buf);
        } else if (obj instanceof ASN1Encodable) {
            _dumpAsString("", verbose, ((ASN1Encodable) obj).toASN1Primitive(), buf);
        } else {
            return "unknown object type " + obj.toString();
        }
        return buf.toString();
    }

    private static String dumpBinaryDataAsString(String indent, byte[] bytes) {
        String nl = Strings.lineSeparator();
        StringBuffer buf = new StringBuffer();
        String indent2 = indent + TAB;
        buf.append(nl);
        for (int i = 0; i < bytes.length; i += 32) {
            if (bytes.length - i > 32) {
                buf.append(indent2);
                buf.append(new String(Hex.encode(bytes, i, 32)));
                buf.append(TAB);
                buf.append(calculateAscString(bytes, i, 32));
                buf.append(nl);
            } else {
                buf.append(indent2);
                buf.append(new String(Hex.encode(bytes, i, bytes.length - i)));
                for (int j = bytes.length - i; j != 32; j++) {
                    buf.append("  ");
                }
                buf.append(TAB);
                buf.append(calculateAscString(bytes, i, bytes.length - i));
                buf.append(nl);
            }
        }
        return buf.toString();
    }

    private static String calculateAscString(byte[] bytes, int off, int len) {
        StringBuffer buf = new StringBuffer();
        for (int i = off; i != off + len; i++) {
            if (bytes[i] >= 32 && bytes[i] <= 126) {
                buf.append((char) bytes[i]);
            }
        }
        return buf.toString();
    }
}

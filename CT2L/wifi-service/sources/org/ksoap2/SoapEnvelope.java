package org.ksoap2;

import java.io.IOException;
import org.ksoap2.kdom.Element;
import org.ksoap2.kdom.Node;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SoapEnvelope {
    public static final String ENC = "http://schemas.xmlsoap.org/soap/encoding/";
    public static final String ENC2003 = "http://www.w3.org/2003/05/soap-encoding";
    public static final String ENV = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String ENV2003 = "http://www.w3.org/2003/05/soap-envelope";
    public static final String NS20 = "http://www.wi-fi.org/specifications/hotspot2dot0/v1.0/spp";
    public static final int VER10 = 100;
    public static final int VER11 = 110;
    public static final int VER12 = 120;
    public static final String XSD = "http://www.w3.org/2001/XMLSchema";
    public static final String XSD1999 = "http://www.w3.org/1999/XMLSchema";
    public static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String XSI1999 = "http://www.w3.org/1999/XMLSchema-instance";
    public Object bodyIn;
    public Object bodyOut;
    public String enc;
    public String encodingStyle;
    public String env;
    public Element[] headerIn;
    public Element[] headerOut;
    public String ns;
    public String omadm;
    public int version;
    public String xsd;
    public String xsi;

    public static boolean stringToBoolean(String booleanAsString) {
        if (booleanAsString == null) {
            return false;
        }
        String booleanAsString2 = booleanAsString.trim().toLowerCase();
        return booleanAsString2.equals("1") || booleanAsString2.equals("true");
    }

    public SoapEnvelope(int version) {
        this.version = version;
        if (version == 100) {
            this.xsi = XSI1999;
            this.xsd = XSD1999;
        } else {
            this.xsi = XSI;
            this.xsd = XSD;
        }
        if (version < 120) {
            this.enc = ENC;
            this.env = ENV;
        } else {
            this.enc = ENC2003;
            this.env = ENV2003;
        }
        this.ns = NS20;
    }

    public void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.nextTag();
        parser.require(2, this.env, "Envelope");
        this.encodingStyle = parser.getAttributeValue(this.env, "encodingStyle");
        parser.nextTag();
        if (parser.getEventType() == 2 && parser.getNamespace().equals(this.env) && parser.getName().equals("Header")) {
            parseHeader(parser);
            parser.require(3, this.env, "Header");
            parser.nextTag();
        }
        parser.require(2, this.env, "Body");
        this.encodingStyle = parser.getAttributeValue(this.env, "encodingStyle");
        parseBody(parser);
        parser.require(3, this.env, "Body");
        parser.nextTag();
        parser.require(3, this.env, "Envelope");
    }

    public void parseHeader(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.nextTag();
        Node headers = new Node();
        headers.parse(parser);
        int count = 0;
        for (int i = 0; i < headers.getChildCount(); i++) {
            if (headers.getElement(i) != null) {
                count++;
            }
        }
        this.headerIn = new Element[count];
        int count2 = 0;
        for (int i2 = 0; i2 < headers.getChildCount(); i2++) {
            Element child = headers.getElement(i2);
            if (child != null) {
                this.headerIn[count2] = child;
                count2++;
            }
        }
    }

    public void parseBody(XmlPullParser parser) throws XmlPullParserException, IOException {
        SoapFault fault;
        parser.nextTag();
        if (parser.getEventType() == 2 && parser.getNamespace().equals(this.env) && parser.getName().equals("Fault")) {
            if (this.version < 120) {
                fault = new SoapFault(this.version);
            } else {
                fault = new SoapFault12(this.version);
            }
            fault.parse(parser);
            this.bodyIn = fault;
            return;
        }
        Node node = this.bodyIn instanceof Node ? (Node) this.bodyIn : new Node();
        node.parse(parser);
        this.bodyIn = node;
    }

    public void write(XmlSerializer writer) throws IOException {
        writer.setPrefix("soap", this.env);
        writer.setPrefix("spp", this.ns);
        writer.startTag(this.env, "Envelope");
        writer.startTag(this.env, "Header");
        writeHeader(writer);
        writer.endTag(this.env, "Header");
        writer.startTag(this.env, "Body");
        writeBody(writer);
        writer.endTag(this.env, "Body");
        writer.endTag(this.env, "Envelope");
    }

    public void writeHeader(XmlSerializer writer) throws IOException {
        if (this.headerOut != null) {
            for (int i = 0; i < this.headerOut.length; i++) {
                this.headerOut[i].write(writer);
            }
        }
    }

    public void writeBody(XmlSerializer writer) throws IOException {
        if (this.encodingStyle != null) {
            writer.attribute(this.env, "encodingStyle", this.encodingStyle);
        }
        ((Node) this.bodyOut).write(writer);
    }

    public void setOutputSoapObject(Object soapObject) {
        this.bodyOut = soapObject;
    }
}

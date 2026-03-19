package mf.org.apache.xml.serialize;

public class XHTMLSerializer extends HTMLSerializer {
    public XHTMLSerializer() {
        super(true, new OutputFormat("xhtml", null, false));
    }

    public XHTMLSerializer(OutputFormat format) {
        super(true, format != null ? format : new OutputFormat("xhtml", null, false));
    }
}

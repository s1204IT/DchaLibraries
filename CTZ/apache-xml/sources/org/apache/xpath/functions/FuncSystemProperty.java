package org.apache.xpath.functions;

import java.io.BufferedInputStream;
import java.util.Properties;
import javax.xml.transform.TransformerException;
import org.apache.xml.serializer.SerializerConstants;
import org.apache.xml.utils.Constants;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;
import org.apache.xpath.res.XPATHErrorResources;

public class FuncSystemProperty extends FunctionOneArg {
    static final String XSLT_PROPERTIES = "org/apache/xalan/res/XSLTInfo.properties";
    static final long serialVersionUID = 3694874980992204867L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        String strSubstring;
        String str = this.m_arg0.execute(xPathContext).str();
        int iIndexOf = str.indexOf(58);
        Properties properties = new Properties();
        loadPropertyFile("org/apache/xalan/res/XSLTInfo.properties", properties);
        String property = null;
        if (iIndexOf > 0) {
            String namespaceForPrefix = xPathContext.getNamespaceContext().getNamespaceForPrefix(iIndexOf >= 0 ? str.substring(0, iIndexOf) : "");
            if (iIndexOf >= 0) {
                strSubstring = str.substring(iIndexOf + 1);
            } else {
                strSubstring = str;
            }
            if (namespaceForPrefix.startsWith("http://www.w3.org/XSL/Transform") || namespaceForPrefix.equals(Constants.S_XSLNAMESPACEURL)) {
                property = properties.getProperty(strSubstring);
                if (property == null) {
                    warn(xPathContext, XPATHErrorResources.WG_PROPERTY_NOT_SUPPORTED, new Object[]{str});
                    return XString.EMPTYSTRING;
                }
            } else {
                warn(xPathContext, XPATHErrorResources.WG_DONT_DO_ANYTHING_WITH_NS, new Object[]{namespaceForPrefix, str});
                try {
                    if (!xPathContext.isSecureProcessing()) {
                        property = System.getProperty(strSubstring);
                    } else {
                        warn(xPathContext, XPATHErrorResources.WG_SECURITY_EXCEPTION, new Object[]{str});
                    }
                    if (property == null) {
                        return XString.EMPTYSTRING;
                    }
                } catch (SecurityException e) {
                    warn(xPathContext, XPATHErrorResources.WG_SECURITY_EXCEPTION, new Object[]{str});
                    return XString.EMPTYSTRING;
                }
            }
        } else {
            try {
                if (!xPathContext.isSecureProcessing()) {
                    property = System.getProperty(str);
                } else {
                    warn(xPathContext, XPATHErrorResources.WG_SECURITY_EXCEPTION, new Object[]{str});
                }
                if (property != null) {
                    strSubstring = "";
                } else {
                    return XString.EMPTYSTRING;
                }
            } catch (SecurityException e2) {
                warn(xPathContext, XPATHErrorResources.WG_SECURITY_EXCEPTION, new Object[]{str});
                return XString.EMPTYSTRING;
            }
        }
        if (strSubstring.equals("version") && property.length() > 0) {
            try {
                return new XString(SerializerConstants.XMLVERSION10);
            } catch (Exception e3) {
                return new XString(property);
            }
        }
        return new XString(property);
    }

    public void loadPropertyFile(String str, Properties properties) {
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(SecuritySupport.getInstance().getResourceAsStream(ObjectFactory.findClassLoader(), str));
            properties.load(bufferedInputStream);
            bufferedInputStream.close();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }
}

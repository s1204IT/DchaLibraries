package org.apache.xalan.extensions;

import java.io.IOException;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.Stylesheet;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.functions.FuncExtFunction;

public abstract class ExtensionHandler {
    protected String m_namespaceUri;
    protected String m_scriptLang;

    public abstract Object callFunction(String str, Vector vector, Object obj, ExpressionContext expressionContext) throws TransformerException;

    public abstract Object callFunction(FuncExtFunction funcExtFunction, Vector vector, ExpressionContext expressionContext) throws TransformerException;

    public abstract boolean isElementAvailable(String str);

    public abstract boolean isFunctionAvailable(String str);

    public abstract void processElement(String str, ElemTemplateElement elemTemplateElement, TransformerImpl transformerImpl, Stylesheet stylesheet, Object obj) throws TransformerException, IOException;

    static Class getClassForName(String str) throws ClassNotFoundException {
        if (str.equals("org.apache.xalan.xslt.extensions.Redirect")) {
            str = "org.apache.xalan.lib.Redirect";
        }
        return ObjectFactory.findProviderClass(str, ObjectFactory.findClassLoader(), true);
    }

    protected ExtensionHandler(String str, String str2) {
        this.m_namespaceUri = str;
        this.m_scriptLang = str2;
    }
}

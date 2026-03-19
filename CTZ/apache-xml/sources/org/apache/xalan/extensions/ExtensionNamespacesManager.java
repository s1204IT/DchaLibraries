package org.apache.xalan.extensions;

import java.util.Vector;
import org.apache.xalan.templates.Constants;
import org.apache.xpath.compiler.PsuedoNames;

public class ExtensionNamespacesManager {
    private Vector m_extensions = new Vector();
    private Vector m_predefExtensions = new Vector(7);
    private Vector m_unregisteredExtensions = new Vector();

    public ExtensionNamespacesManager() {
        setPredefinedNamespaces();
    }

    public void registerExtension(String str) {
        if (namespaceIndex(str, this.m_extensions) == -1) {
            int iNamespaceIndex = namespaceIndex(str, this.m_predefExtensions);
            if (iNamespaceIndex != -1) {
                this.m_extensions.add(this.m_predefExtensions.get(iNamespaceIndex));
            } else if (!this.m_unregisteredExtensions.contains(str)) {
                this.m_unregisteredExtensions.add(str);
            }
        }
    }

    public void registerExtension(ExtensionNamespaceSupport extensionNamespaceSupport) {
        String namespace = extensionNamespaceSupport.getNamespace();
        if (namespaceIndex(namespace, this.m_extensions) == -1) {
            this.m_extensions.add(extensionNamespaceSupport);
            if (this.m_unregisteredExtensions.contains(namespace)) {
                this.m_unregisteredExtensions.remove(namespace);
            }
        }
    }

    public int namespaceIndex(String str, Vector vector) {
        for (int i = 0; i < vector.size(); i++) {
            if (((ExtensionNamespaceSupport) vector.get(i)).getNamespace().equals(str)) {
                return i;
            }
        }
        return -1;
    }

    public Vector getExtensions() {
        return this.m_extensions;
    }

    public void registerUnregisteredNamespaces() {
        for (int i = 0; i < this.m_unregisteredExtensions.size(); i++) {
            ExtensionNamespaceSupport extensionNamespaceSupportDefineJavaNamespace = defineJavaNamespace((String) this.m_unregisteredExtensions.get(i));
            if (extensionNamespaceSupportDefineJavaNamespace != null) {
                this.m_extensions.add(extensionNamespaceSupportDefineJavaNamespace);
            }
        }
    }

    public ExtensionNamespaceSupport defineJavaNamespace(String str) {
        return defineJavaNamespace(str, str);
    }

    public ExtensionNamespaceSupport defineJavaNamespace(String str, String str2) {
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        if (str2.startsWith("class:")) {
            str2 = str2.substring(6);
        }
        int iLastIndexOf = str2.lastIndexOf(PsuedoNames.PSEUDONAME_ROOT);
        if (-1 != iLastIndexOf) {
            str2 = str2.substring(iLastIndexOf + 1);
        }
        if (str2 == null || str2.trim().length() == 0) {
            return null;
        }
        try {
            ExtensionHandler.getClassForName(str2);
            return new ExtensionNamespaceSupport(str, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{str, "javaclass", str2});
        } catch (ClassNotFoundException e) {
            return new ExtensionNamespaceSupport(str, "org.apache.xalan.extensions.ExtensionHandlerJavaPackage", new Object[]{str, "javapackage", str2 + Constants.ATTRVAL_THIS});
        }
    }

    private void setPredefinedNamespaces() {
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXTENSIONS_JAVA_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaPackage", new Object[]{org.apache.xml.utils.Constants.S_EXTENSIONS_JAVA_URL, "javapackage", ""}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXTENSIONS_OLD_JAVA_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaPackage", new Object[]{org.apache.xml.utils.Constants.S_EXTENSIONS_OLD_JAVA_URL, "javapackage", ""}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXTENSIONS_LOTUSXSL_JAVA_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaPackage", new Object[]{org.apache.xml.utils.Constants.S_EXTENSIONS_LOTUSXSL_JAVA_URL, "javapackage", ""}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport("http://xml.apache.org/xalan", "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{"http://xml.apache.org/xalan", "javaclass", "org.apache.xalan.lib.Extensions"}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_BUILTIN_OLD_EXTENSIONS_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{org.apache.xml.utils.Constants.S_BUILTIN_OLD_EXTENSIONS_URL, "javaclass", "org.apache.xalan.lib.Extensions"}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXTENSIONS_REDIRECT_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{org.apache.xml.utils.Constants.S_EXTENSIONS_REDIRECT_URL, "javaclass", "org.apache.xalan.lib.Redirect"}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXTENSIONS_PIPE_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{org.apache.xml.utils.Constants.S_EXTENSIONS_PIPE_URL, "javaclass", "org.apache.xalan.lib.PipeDocument"}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXTENSIONS_SQL_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{org.apache.xml.utils.Constants.S_EXTENSIONS_SQL_URL, "javaclass", "org.apache.xalan.lib.sql.XConnection"}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXSLT_COMMON_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{org.apache.xml.utils.Constants.S_EXSLT_COMMON_URL, "javaclass", "org.apache.xalan.lib.ExsltCommon"}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXSLT_MATH_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{org.apache.xml.utils.Constants.S_EXSLT_MATH_URL, "javaclass", "org.apache.xalan.lib.ExsltMath"}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXSLT_SETS_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{org.apache.xml.utils.Constants.S_EXSLT_SETS_URL, "javaclass", "org.apache.xalan.lib.ExsltSets"}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXSLT_DATETIME_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{org.apache.xml.utils.Constants.S_EXSLT_DATETIME_URL, "javaclass", "org.apache.xalan.lib.ExsltDatetime"}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXSLT_DYNAMIC_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{org.apache.xml.utils.Constants.S_EXSLT_DYNAMIC_URL, "javaclass", "org.apache.xalan.lib.ExsltDynamic"}));
        this.m_predefExtensions.add(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXSLT_STRINGS_URL, "org.apache.xalan.extensions.ExtensionHandlerJavaClass", new Object[]{org.apache.xml.utils.Constants.S_EXSLT_STRINGS_URL, "javaclass", "org.apache.xalan.lib.ExsltStrings"}));
    }
}

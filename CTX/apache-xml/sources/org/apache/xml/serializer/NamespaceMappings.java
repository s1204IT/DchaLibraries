package org.apache.xml.serializer;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.xalan.templates.Constants;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class NamespaceMappings {
    private static final String EMPTYSTRING = "";
    private static final String XML_PREFIX = "xml";
    private int count = 0;
    private Hashtable m_namespaces = new Hashtable();
    private Stack m_nodeStack = new Stack();

    public NamespaceMappings() {
        initNamespaces();
    }

    private void initNamespaces() {
        createPrefixStack("").push(new MappingRecord("", "", -1));
        createPrefixStack("xml").push(new MappingRecord("xml", "http://www.w3.org/XML/1998/namespace", -1));
    }

    public String lookupNamespace(String str) {
        String str2;
        Stack prefixStack = getPrefixStack(str);
        if (prefixStack != null && !prefixStack.isEmpty()) {
            str2 = ((MappingRecord) prefixStack.peek()).m_uri;
        } else {
            str2 = null;
        }
        if (str2 == null) {
            return "";
        }
        return str2;
    }

    MappingRecord getMappingFromPrefix(String str) {
        Stack stack = (Stack) this.m_namespaces.get(str);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return (MappingRecord) stack.peek();
    }

    public String lookupPrefix(String str) {
        Enumeration enumerationKeys = this.m_namespaces.keys();
        while (enumerationKeys.hasMoreElements()) {
            String str2 = (String) enumerationKeys.nextElement();
            String strLookupNamespace = lookupNamespace(str2);
            if (strLookupNamespace != null && strLookupNamespace.equals(str)) {
                return str2;
            }
        }
        return null;
    }

    MappingRecord getMappingFromURI(String str) {
        Enumeration enumerationKeys = this.m_namespaces.keys();
        while (enumerationKeys.hasMoreElements()) {
            MappingRecord mappingFromPrefix = getMappingFromPrefix((String) enumerationKeys.nextElement());
            if (mappingFromPrefix != null && mappingFromPrefix.m_uri.equals(str)) {
                return mappingFromPrefix;
            }
        }
        return null;
    }

    boolean popNamespace(String str) {
        Stack prefixStack;
        if (str.startsWith("xml") || (prefixStack = getPrefixStack(str)) == null) {
            return false;
        }
        prefixStack.pop();
        return true;
    }

    public boolean pushNamespace(String str, String str2, int i) {
        if (str.startsWith("xml")) {
            return false;
        }
        Stack stack = (Stack) this.m_namespaces.get(str);
        if (stack == null) {
            Hashtable hashtable = this.m_namespaces;
            Stack stack2 = new Stack();
            hashtable.put(str, stack2);
            stack = stack2;
        }
        if (!stack.empty()) {
            MappingRecord mappingRecord = (MappingRecord) stack.peek();
            if (str2.equals(mappingRecord.m_uri) || i == mappingRecord.m_declarationDepth) {
                return false;
            }
        }
        MappingRecord mappingRecord2 = new MappingRecord(str, str2, i);
        stack.push(mappingRecord2);
        this.m_nodeStack.push(mappingRecord2);
        return true;
    }

    void popNamespaces(int i, ContentHandler contentHandler) {
        while (!this.m_nodeStack.isEmpty()) {
            MappingRecord mappingRecord = (MappingRecord) this.m_nodeStack.peek();
            int i2 = mappingRecord.m_declarationDepth;
            if (i >= 1 && mappingRecord.m_declarationDepth >= i) {
                MappingRecord mappingRecord2 = (MappingRecord) this.m_nodeStack.pop();
                String str = mappingRecord.m_prefix;
                Stack prefixStack = getPrefixStack(str);
                if (mappingRecord2 == ((MappingRecord) prefixStack.peek())) {
                    prefixStack.pop();
                    if (contentHandler != null) {
                        try {
                            contentHandler.endPrefixMapping(str);
                        } catch (SAXException e) {
                        }
                    }
                }
            } else {
                return;
            }
        }
    }

    public String generateNextPrefix() {
        StringBuilder sb = new StringBuilder();
        sb.append(Constants.ATTRNAME_NS);
        int i = this.count;
        this.count = i + 1;
        sb.append(i);
        return sb.toString();
    }

    public Object clone() throws CloneNotSupportedException {
        NamespaceMappings namespaceMappings = new NamespaceMappings();
        namespaceMappings.m_nodeStack = (Stack) this.m_nodeStack.clone();
        namespaceMappings.count = this.count;
        namespaceMappings.m_namespaces = (Hashtable) this.m_namespaces.clone();
        namespaceMappings.count = this.count;
        return namespaceMappings;
    }

    final void reset() {
        this.count = 0;
        this.m_namespaces.clear();
        this.m_nodeStack.clear();
        initNamespaces();
    }

    class MappingRecord {
        final int m_declarationDepth;
        final String m_prefix;
        final String m_uri;

        MappingRecord(String str, String str2, int i) {
            this.m_prefix = str;
            this.m_uri = str2 == null ? "" : str2;
            this.m_declarationDepth = i;
        }
    }

    private class Stack {
        private int top = -1;
        private int max = 20;
        Object[] m_stack = new Object[this.max];

        public Object clone() throws CloneNotSupportedException {
            Stack stack = NamespaceMappings.this.new Stack();
            stack.max = this.max;
            stack.top = this.top;
            stack.m_stack = new Object[stack.max];
            for (int i = 0; i <= this.top; i++) {
                stack.m_stack[i] = this.m_stack[i];
            }
            return stack;
        }

        public Stack() {
        }

        public Object push(Object obj) {
            this.top++;
            if (this.max <= this.top) {
                int i = (2 * this.max) + 1;
                Object[] objArr = new Object[i];
                System.arraycopy(this.m_stack, 0, objArr, 0, this.max);
                this.max = i;
                this.m_stack = objArr;
            }
            this.m_stack[this.top] = obj;
            return obj;
        }

        public Object pop() {
            if (this.top >= 0) {
                this.top--;
                return this.m_stack[this.top];
            }
            return null;
        }

        public Object peek() {
            if (this.top >= 0) {
                return this.m_stack[this.top];
            }
            return null;
        }

        public Object peek(int i) {
            return this.m_stack[i];
        }

        public boolean isEmpty() {
            return this.top < 0;
        }

        public boolean empty() {
            return this.top < 0;
        }

        public void clear() {
            for (int i = 0; i <= this.top; i++) {
                this.m_stack[i] = null;
            }
            this.top = -1;
        }

        public Object getElement(int i) {
            return this.m_stack[i];
        }
    }

    private Stack getPrefixStack(String str) {
        return (Stack) this.m_namespaces.get(str);
    }

    private Stack createPrefixStack(String str) {
        Stack stack = new Stack();
        this.m_namespaces.put(str, stack);
        return stack;
    }

    public String[] lookupAllPrefixes(String str) {
        ArrayList arrayList = new ArrayList();
        Enumeration enumerationKeys = this.m_namespaces.keys();
        while (enumerationKeys.hasMoreElements()) {
            String str2 = (String) enumerationKeys.nextElement();
            String strLookupNamespace = lookupNamespace(str2);
            if (strLookupNamespace != null && strLookupNamespace.equals(str)) {
                arrayList.add(str2);
            }
        }
        String[] strArr = new String[arrayList.size()];
        arrayList.toArray(strArr);
        return strArr;
    }
}

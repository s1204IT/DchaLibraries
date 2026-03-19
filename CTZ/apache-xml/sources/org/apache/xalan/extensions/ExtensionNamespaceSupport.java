package org.apache.xalan.extensions;

import java.lang.reflect.Constructor;
import javax.xml.transform.TransformerException;

public class ExtensionNamespaceSupport {
    Object[] m_args;
    String m_handlerClass;
    String m_namespace;
    Class[] m_sig;

    public ExtensionNamespaceSupport(String str, String str2, Object[] objArr) {
        this.m_namespace = null;
        this.m_handlerClass = null;
        this.m_sig = null;
        this.m_args = null;
        this.m_namespace = str;
        this.m_handlerClass = str2;
        this.m_args = objArr;
        this.m_sig = new Class[this.m_args.length];
        for (int i = 0; i < this.m_args.length; i++) {
            if (this.m_args[i] != null) {
                this.m_sig[i] = this.m_args[i].getClass();
            } else {
                this.m_sig = null;
                return;
            }
        }
    }

    public String getNamespace() {
        return this.m_namespace;
    }

    public ExtensionHandler launch() throws TransformerException {
        try {
            Class classForName = ExtensionHandler.getClassForName(this.m_handlerClass);
            Constructor<?> constructor = null;
            if (this.m_sig != null) {
                constructor = classForName.getConstructor(this.m_sig);
            } else {
                Constructor<?>[] constructors = classForName.getConstructors();
                int i = 0;
                while (true) {
                    if (i >= constructors.length) {
                        break;
                    }
                    if (constructors[i].getParameterTypes().length != this.m_args.length) {
                        i++;
                    } else {
                        constructor = constructors[i];
                        break;
                    }
                }
            }
            if (constructor != null) {
                return (ExtensionHandler) constructor.newInstance(this.m_args);
            }
            throw new TransformerException("ExtensionHandler constructor not found");
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }
}

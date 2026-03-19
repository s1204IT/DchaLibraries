package org.apache.xml.utils;

import java.io.Serializable;
import java.util.ArrayList;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;

public class ObjectPool implements Serializable {
    static final long serialVersionUID = -8519013691660936643L;
    private final ArrayList freeStack;
    private final Class objectType;

    public ObjectPool(Class cls) {
        this.objectType = cls;
        this.freeStack = new ArrayList();
    }

    public ObjectPool(String str) {
        try {
            this.objectType = ObjectFactory.findProviderClass(str, ObjectFactory.findClassLoader(), true);
            this.freeStack = new ArrayList();
        } catch (ClassNotFoundException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public ObjectPool(Class cls, int i) {
        this.objectType = cls;
        this.freeStack = new ArrayList(i);
    }

    public ObjectPool() {
        this.objectType = null;
        this.freeStack = new ArrayList();
    }

    public synchronized Object getInstanceIfFree() {
        if (!this.freeStack.isEmpty()) {
            return this.freeStack.remove(this.freeStack.size() - 1);
        }
        return null;
    }

    public synchronized Object getInstance() {
        if (this.freeStack.isEmpty()) {
            try {
                return this.objectType.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(XMLMessages.createXMLMessage(XMLErrorResources.ER_EXCEPTION_CREATING_POOL, null));
            }
        }
        return this.freeStack.remove(this.freeStack.size() - 1);
    }

    public synchronized void freeInstance(Object obj) {
        this.freeStack.add(obj);
    }
}

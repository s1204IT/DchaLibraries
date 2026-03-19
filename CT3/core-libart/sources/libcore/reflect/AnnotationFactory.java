package libcore.reflect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class AnnotationFactory implements InvocationHandler, Serializable {
    private static final transient Map<Class<? extends Annotation>, AnnotationMember[]> cache = new WeakHashMap();
    private AnnotationMember[] elements;
    private final Class<? extends Annotation> klazz;

    public static AnnotationMember[] getElementsDescription(Class<? extends Annotation> annotationType) {
        synchronized (cache) {
            AnnotationMember[] desc = cache.get(annotationType);
            if (desc != null) {
                return desc;
            }
            if (!annotationType.isAnnotation()) {
                throw new IllegalArgumentException("Type is not annotation: " + annotationType.getName());
            }
            Method[] declaredMethods = annotationType.getDeclaredMethods();
            AnnotationMember[] desc2 = new AnnotationMember[declaredMethods.length];
            for (int i = 0; i < declaredMethods.length; i++) {
                Method element = declaredMethods[i];
                String name = element.getName();
                Class<?> type = element.getReturnType();
                try {
                    desc2[i] = new AnnotationMember(name, element.getDefaultValue(), type, element);
                } catch (Throwable t) {
                    desc2[i] = new AnnotationMember(name, t, type, element);
                }
            }
            synchronized (cache) {
                cache.put(annotationType, desc2);
            }
            return desc2;
        }
    }

    public static <A extends Annotation> A createAnnotation(Class<? extends Annotation> annotationType, AnnotationMember[] elements) {
        AnnotationFactory factory = new AnnotationFactory(annotationType, elements);
        return (A) Proxy.newProxyInstance(annotationType.getClassLoader(), new Class[]{annotationType}, factory);
    }

    private AnnotationFactory(Class<? extends Annotation> klzz, AnnotationMember[] values) {
        this.klazz = klzz;
        AnnotationMember[] defs = getElementsDescription(this.klazz);
        if (values == null) {
            this.elements = defs;
            return;
        }
        this.elements = new AnnotationMember[defs.length];
        for (int i = this.elements.length - 1; i >= 0; i--) {
            int length = values.length;
            int i2 = 0;
            while (true) {
                if (i2 < length) {
                    AnnotationMember val = values[i2];
                    if (!val.name.equals(defs[i].name)) {
                        i2++;
                    } else {
                        this.elements[i] = val.setDefinition(defs[i]);
                        break;
                    }
                } else {
                    this.elements[i] = defs[i];
                    break;
                }
            }
        }
    }

    private void readObject(ObjectInputStream os) throws ClassNotFoundException, IOException {
        os.defaultReadObject();
        AnnotationMember[] defs = getElementsDescription(this.klazz);
        AnnotationMember[] old = this.elements;
        List<AnnotationMember> merged = new ArrayList<>(defs.length + old.length);
        for (AnnotationMember el1 : old) {
            int length = defs.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    AnnotationMember el2 = defs[i];
                    if (el2.name.equals(el1.name)) {
                        break;
                    } else {
                        i++;
                    }
                } else {
                    merged.add(el1);
                    break;
                }
            }
        }
        for (AnnotationMember def : defs) {
            int length2 = old.length;
            int i2 = 0;
            while (true) {
                if (i2 < length2) {
                    AnnotationMember val = old[i2];
                    if (!val.name.equals(def.name)) {
                        i2++;
                    } else {
                        merged.add(val.setDefinition(def));
                        break;
                    }
                } else {
                    merged.add(def);
                    break;
                }
            }
        }
        this.elements = (AnnotationMember[]) merged.toArray(new AnnotationMember[merged.size()]);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!this.klazz.isInstance(obj)) {
            return false;
        }
        if (Proxy.isProxyClass(obj.getClass())) {
            Object handler = Proxy.getInvocationHandler(obj);
            if (handler instanceof AnnotationFactory) {
                AnnotationFactory other = (AnnotationFactory) handler;
                if (this.elements.length != other.elements.length) {
                    return false;
                }
                AnnotationMember[] annotationMemberArr = this.elements;
                int length = annotationMemberArr.length;
                int i = 0;
                while (i < length) {
                    AnnotationMember el1 = annotationMemberArr[i];
                    for (AnnotationMember el2 : other.elements) {
                        if (el1.equals(el2)) {
                            break;
                        }
                    }
                    return false;
                }
                return true;
            }
        }
        for (AnnotationMember el : this.elements) {
            if (el.tag == '!') {
                return false;
            }
            try {
                if (!el.definingMethod.isAccessible()) {
                    el.definingMethod.setAccessible(true);
                }
                Object otherValue = el.definingMethod.invoke(obj, new Object[0]);
                if (otherValue != null) {
                    if (el.tag == '[') {
                        if (!el.equalArrayValue(otherValue)) {
                            return false;
                        }
                    } else if (!el.value.equals(otherValue)) {
                        return false;
                    }
                } else if (el.value != AnnotationMember.NO_VALUE) {
                    return false;
                }
            } catch (Throwable th) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int hash = 0;
        for (AnnotationMember element : this.elements) {
            hash += element.hashCode();
        }
        return hash;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('@');
        result.append(this.klazz.getName());
        result.append('(');
        for (int i = 0; i < this.elements.length; i++) {
            if (i != 0) {
                result.append(", ");
            }
            result.append(this.elements[i]);
        }
        result.append(')');
        return result.toString();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        int i = 0;
        String name = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("annotationType".equals(name)) {
                return this.klazz;
            }
            if ("toString".equals(name)) {
                return toString();
            }
            if ("hashCode".equals(name)) {
                return Integer.valueOf(hashCode());
            }
            AnnotationMember element = null;
            AnnotationMember[] annotationMemberArr = this.elements;
            int length = annotationMemberArr.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                AnnotationMember el = annotationMemberArr[i];
                if (!name.equals(el.name)) {
                    i++;
                } else {
                    element = el;
                    break;
                }
            }
            if (element == null || !method.equals(element.definingMethod)) {
                throw new IllegalArgumentException(method.toString());
            }
            Object value = element.validateValue();
            if (value == null) {
                throw new IncompleteAnnotationException(this.klazz, name);
            }
            return value;
        }
        if (parameterTypes.length == 1 && parameterTypes[0] == Object.class && "equals".equals(name)) {
            return Boolean.valueOf(equals(args[0]));
        }
        throw new IllegalArgumentException("Invalid method for annotation type: " + method);
    }
}

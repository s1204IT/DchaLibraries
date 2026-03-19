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
import java.util.Map;
import java.util.WeakHashMap;

public final class AnnotationFactory implements InvocationHandler, Serializable {
    private static final transient Map<Class<? extends Annotation>, AnnotationMember[]> cache = new WeakHashMap();
    private AnnotationMember[] elements;
    private final Class<? extends Annotation> klazz;

    public static AnnotationMember[] getElementsDescription(Class<? extends Annotation> cls) {
        synchronized (cache) {
            AnnotationMember[] annotationMemberArr = cache.get(cls);
            if (annotationMemberArr != null) {
                return annotationMemberArr;
            }
            if (!cls.isAnnotation()) {
                throw new IllegalArgumentException("Type is not annotation: " + cls.getName());
            }
            Method[] declaredMethods = cls.getDeclaredMethods();
            AnnotationMember[] annotationMemberArr2 = new AnnotationMember[declaredMethods.length];
            for (int i = 0; i < declaredMethods.length; i++) {
                Method method = declaredMethods[i];
                String name = method.getName();
                Class<?> returnType = method.getReturnType();
                try {
                    annotationMemberArr2[i] = new AnnotationMember(name, method.getDefaultValue(), returnType, method);
                } catch (Throwable th) {
                    annotationMemberArr2[i] = new AnnotationMember(name, th, returnType, method);
                }
            }
            synchronized (cache) {
                cache.put(cls, annotationMemberArr2);
            }
            return annotationMemberArr2;
        }
    }

    public static <A extends Annotation> A createAnnotation(Class<? extends Annotation> cls, AnnotationMember[] annotationMemberArr) {
        return (A) Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new AnnotationFactory(cls, annotationMemberArr));
    }

    private AnnotationFactory(Class<? extends Annotation> cls, AnnotationMember[] annotationMemberArr) {
        this.klazz = cls;
        AnnotationMember[] elementsDescription = getElementsDescription(this.klazz);
        if (annotationMemberArr == null) {
            this.elements = elementsDescription;
            return;
        }
        this.elements = new AnnotationMember[elementsDescription.length];
        for (int length = this.elements.length - 1; length >= 0; length--) {
            int length2 = annotationMemberArr.length;
            int i = 0;
            while (true) {
                if (i < length2) {
                    AnnotationMember annotationMember = annotationMemberArr[i];
                    if (!annotationMember.name.equals(elementsDescription[length].name)) {
                        i++;
                    } else {
                        this.elements[length] = annotationMember.setDefinition(elementsDescription[length]);
                        break;
                    }
                } else {
                    this.elements[length] = elementsDescription[length];
                    break;
                }
            }
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        AnnotationMember[] elementsDescription = getElementsDescription(this.klazz);
        AnnotationMember[] annotationMemberArr = this.elements;
        ArrayList arrayList = new ArrayList(elementsDescription.length + annotationMemberArr.length);
        for (AnnotationMember annotationMember : annotationMemberArr) {
            int length = elementsDescription.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    if (elementsDescription[i].name.equals(annotationMember.name)) {
                        break;
                    } else {
                        i++;
                    }
                } else {
                    arrayList.add(annotationMember);
                    break;
                }
            }
        }
        for (AnnotationMember annotationMember2 : elementsDescription) {
            int length2 = annotationMemberArr.length;
            int i2 = 0;
            while (true) {
                if (i2 < length2) {
                    AnnotationMember annotationMember3 = annotationMemberArr[i2];
                    if (!annotationMember3.name.equals(annotationMember2.name)) {
                        i2++;
                    } else {
                        arrayList.add(annotationMember3.setDefinition(annotationMember2));
                        break;
                    }
                } else {
                    arrayList.add(annotationMember2);
                    break;
                }
            }
        }
        this.elements = (AnnotationMember[]) arrayList.toArray(new AnnotationMember[arrayList.size()]);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!this.klazz.isInstance(obj)) {
            return false;
        }
        if (Proxy.isProxyClass(obj.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(obj);
            if (invocationHandler instanceof AnnotationFactory) {
                AnnotationFactory annotationFactory = (AnnotationFactory) invocationHandler;
                if (this.elements.length != annotationFactory.elements.length) {
                    return false;
                }
                AnnotationMember[] annotationMemberArr = this.elements;
                int length = annotationMemberArr.length;
                int i = 0;
                while (i < length) {
                    AnnotationMember annotationMember = annotationMemberArr[i];
                    for (AnnotationMember annotationMember2 : annotationFactory.elements) {
                        if (annotationMember.equals(annotationMember2)) {
                            break;
                        }
                    }
                    return false;
                }
                return true;
            }
        }
        for (AnnotationMember annotationMember3 : this.elements) {
            if (annotationMember3.tag == '!') {
                return false;
            }
            try {
                if (!annotationMember3.definingMethod.isAccessible()) {
                    annotationMember3.definingMethod.setAccessible(true);
                }
                Object objInvoke = annotationMember3.definingMethod.invoke(obj, new Object[0]);
                if (objInvoke != null) {
                    if (annotationMember3.tag == '[') {
                        if (!annotationMember3.equalArrayValue(objInvoke)) {
                            return false;
                        }
                    } else if (!annotationMember3.value.equals(objInvoke)) {
                        return false;
                    }
                } else if (annotationMember3.value != AnnotationMember.NO_VALUE) {
                    return false;
                }
            } catch (Throwable th) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int iHashCode = 0;
        for (AnnotationMember annotationMember : this.elements) {
            iHashCode += annotationMember.hashCode();
        }
        return iHashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('@');
        sb.append(this.klazz.getName());
        sb.append('(');
        for (int i = 0; i < this.elements.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(this.elements[i]);
        }
        sb.append(')');
        return sb.toString();
    }

    @Override
    public Object invoke(Object obj, Method method, Object[] objArr) throws Throwable {
        String name = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        int i = 0;
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
            AnnotationMember annotationMember = null;
            AnnotationMember[] annotationMemberArr = this.elements;
            int length = annotationMemberArr.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                AnnotationMember annotationMember2 = annotationMemberArr[i];
                if (!name.equals(annotationMember2.name)) {
                    i++;
                } else {
                    annotationMember = annotationMember2;
                    break;
                }
            }
            if (annotationMember == null || !method.equals(annotationMember.definingMethod)) {
                throw new IllegalArgumentException(method.toString());
            }
            Object objValidateValue = annotationMember.validateValue();
            if (objValidateValue == null) {
                throw new IncompleteAnnotationException(this.klazz, name);
            }
            return objValidateValue;
        }
        if (parameterTypes.length == 1 && parameterTypes[0] == Object.class && "equals".equals(name)) {
            return Boolean.valueOf(equals(objArr[0]));
        }
        throw new IllegalArgumentException("Invalid method for annotation type: " + method);
    }
}

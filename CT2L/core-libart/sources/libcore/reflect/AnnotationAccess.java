package libcore.reflect;

import com.android.dex.Dex;
import com.android.dex.EncodedValueReader;
import com.android.dex.FieldId;
import com.android.dex.ProtoId;
import com.android.dex.TypeList;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import libcore.util.EmptyArray;

public final class AnnotationAccess {
    private static final Class<?>[] NO_ARGUMENTS = null;
    private static final byte VISIBILITY_BUILD = 0;
    private static final byte VISIBILITY_RUNTIME = 1;
    private static final byte VISIBILITY_SYSTEM = 2;

    private AnnotationAccess() {
    }

    public static <A extends Annotation> A getAnnotation(Class<?> cls, Class<A> cls2) {
        if (cls2 == null) {
            throw new NullPointerException("annotationType == null");
        }
        A a = (A) getDeclaredAnnotation(cls, cls2);
        if (a != null) {
            return a;
        }
        if (isInherited(cls2)) {
            for (Class<? super Object> superclass = cls.getSuperclass(); superclass != null; superclass = superclass.getSuperclass()) {
                A a2 = (A) getDeclaredAnnotation(superclass, cls2);
                if (a2 != null) {
                    return a2;
                }
            }
        }
        return null;
    }

    private static boolean isInherited(Class<? extends Annotation> annotationType) {
        return isDeclaredAnnotationPresent(annotationType, Inherited.class);
    }

    public static Annotation[] getAnnotations(Class<?> c) {
        HashMap<Class<?>, Annotation> map = new HashMap<>();
        for (Annotation declaredAnnotation : getDeclaredAnnotations(c)) {
            map.put(declaredAnnotation.annotationType(), declaredAnnotation);
        }
        for (Class<?> sup = c.getSuperclass(); sup != null; sup = sup.getSuperclass()) {
            for (Annotation declaredAnnotation2 : getDeclaredAnnotations(sup)) {
                Class<? extends Annotation> clazz = declaredAnnotation2.annotationType();
                if (!map.containsKey(clazz) && isInherited(clazz)) {
                    map.put(clazz, declaredAnnotation2);
                }
            }
        }
        Collection<Annotation> coll = map.values();
        return (Annotation[]) coll.toArray(new Annotation[coll.size()]);
    }

    public static boolean isAnnotationPresent(Class<?> c, Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            throw new NullPointerException("annotationType == null");
        }
        if (isDeclaredAnnotationPresent(c, annotationType)) {
            return true;
        }
        if (isInherited(annotationType)) {
            for (Class<?> sup = c.getSuperclass(); sup != null; sup = sup.getSuperclass()) {
                if (isDeclaredAnnotationPresent(sup, annotationType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Annotation> getDeclaredAnnotations(AnnotatedElement element) {
        int offset = getAnnotationSetOffset(element);
        return annotationSetToAnnotations(getDexClass(element), offset);
    }

    public static <A extends Annotation> A getDeclaredAnnotation(AnnotatedElement annotatedElement, Class<A> cls) {
        com.android.dex.Annotation annotation = getAnnotation(annotatedElement, (Class<? extends Annotation>) cls);
        if (annotation != null) {
            return (A) toAnnotationInstance(getDexClass(annotatedElement), cls, annotation);
        }
        return null;
    }

    public static boolean isDeclaredAnnotationPresent(AnnotatedElement element, Class<? extends Annotation> annotationClass) {
        return getAnnotation(element, annotationClass) != null;
    }

    private static com.android.dex.Annotation getAnnotation(AnnotatedElement element, Class<? extends Annotation> annotationClass) {
        int annotationSetOffset = getAnnotationSetOffset(element);
        if (annotationSetOffset == 0) {
            return null;
        }
        Class<?> dexClass = getDexClass(element);
        Dex dex = dexClass.getDex();
        Dex.Section setIn = dex.open(annotationSetOffset);
        String annotationInternalName = InternalNames.getInternalName(annotationClass);
        int size = setIn.readInt();
        for (int i = 0; i < size; i++) {
            int annotationOffset = setIn.readInt();
            Dex.Section annotationIn = dex.open(annotationOffset);
            com.android.dex.Annotation candidate = annotationIn.readAnnotation();
            String candidateInternalName = dex.typeNames().get(candidate.getTypeIndex());
            if (candidateInternalName.equals(annotationInternalName)) {
                return candidate;
            }
        }
        return null;
    }

    private static int getAnnotationSetOffset(AnnotatedElement element) {
        Class<?> dexClass = getDexClass(element);
        int directoryOffset = dexClass.getDexAnnotationDirectoryOffset();
        if (directoryOffset == 0) {
            return 0;
        }
        Dex.Section directoryIn = dexClass.getDex().open(directoryOffset);
        int classSetOffset = directoryIn.readInt();
        if (element instanceof Class) {
            return classSetOffset;
        }
        int fieldsSize = directoryIn.readInt();
        int methodsSize = directoryIn.readInt();
        directoryIn.readInt();
        if (element instanceof Field) {
            int fieldIndex = ((Field) element).getDexFieldIndex();
            for (int i = 0; i < fieldsSize; i++) {
                int candidateFieldIndex = directoryIn.readInt();
                int i2 = directoryIn.readInt();
                if (candidateFieldIndex == fieldIndex) {
                    return i2;
                }
            }
            return 0;
        }
        directoryIn.skip(fieldsSize * 8);
        int methodIndex = element instanceof Method ? ((Method) element).getDexMethodIndex() : ((Constructor) element).getDexMethodIndex();
        for (int i3 = 0; i3 < methodsSize; i3++) {
            int candidateMethodIndex = directoryIn.readInt();
            int i4 = directoryIn.readInt();
            if (candidateMethodIndex == methodIndex) {
                return i4;
            }
        }
        return 0;
    }

    private static Class<?> getDexClass(AnnotatedElement element) {
        return element instanceof Class ? (Class) element : ((Member) element).getDeclaringClass();
    }

    public static Annotation[][] getParameterAnnotations(Class<?> declaringClass, int methodDexIndex) {
        Dex dex = declaringClass.getDex();
        int protoIndex = dex.methodIds().get(methodDexIndex).getProtoIndex();
        ProtoId proto = dex.protoIds().get(protoIndex);
        TypeList parametersList = dex.readTypeList(proto.getParametersOffset());
        short[] types = parametersList.getTypes();
        int typesCount = types.length;
        int directoryOffset = declaringClass.getDexAnnotationDirectoryOffset();
        if (directoryOffset == 0) {
            return (Annotation[][]) Array.newInstance((Class<?>) Annotation.class, typesCount, 0);
        }
        Dex.Section directoryIn = dex.open(directoryOffset);
        directoryIn.readInt();
        int fieldsSize = directoryIn.readInt();
        int methodsSize = directoryIn.readInt();
        int parametersSize = directoryIn.readInt();
        for (int i = 0; i < fieldsSize; i++) {
            directoryIn.readInt();
            directoryIn.readInt();
        }
        for (int i2 = 0; i2 < methodsSize; i2++) {
            directoryIn.readInt();
            directoryIn.readInt();
        }
        for (int i3 = 0; i3 < parametersSize; i3++) {
            int candidateMethodDexIndex = directoryIn.readInt();
            int annotationSetRefListOffset = directoryIn.readInt();
            if (candidateMethodDexIndex == methodDexIndex) {
                Dex.Section refList = dex.open(annotationSetRefListOffset);
                int parameterCount = refList.readInt();
                Annotation[][] result = new Annotation[parameterCount][];
                for (int p = 0; p < parameterCount; p++) {
                    int annotationSetOffset = refList.readInt();
                    List<Annotation> annotations = annotationSetToAnnotations(declaringClass, annotationSetOffset);
                    result[p] = (Annotation[]) annotations.toArray(new Annotation[annotations.size()]);
                }
                return result;
            }
        }
        return (Annotation[][]) Array.newInstance((Class<?>) Annotation.class, typesCount, 0);
    }

    public static Object getDefaultValue(Method method) {
        Class<?> annotationClass = method.getDeclaringClass();
        Dex dex = annotationClass.getDex();
        EncodedValueReader reader = getOnlyAnnotationValue(dex, annotationClass, "Ldalvik/annotation/AnnotationDefault;");
        if (reader == null) {
            return null;
        }
        int fieldCount = reader.readAnnotation();
        if (reader.getAnnotationType() != annotationClass.getDexTypeIndex()) {
            throw new AssertionError("annotation value type != annotation class");
        }
        int methodNameIndex = dex.findStringIndex(method.getName());
        for (int i = 0; i < fieldCount; i++) {
            int candidateNameIndex = reader.readAnnotationName();
            if (candidateNameIndex == methodNameIndex) {
                Class<?> returnType = method.getReturnType();
                return decodeValue(annotationClass, returnType, dex, reader);
            }
            reader.skipValue();
        }
        return null;
    }

    public static Class<?> getEnclosingClass(Class<?> c) {
        Dex dex = c.getDex();
        EncodedValueReader reader = getOnlyAnnotationValue(dex, c, "Ldalvik/annotation/EnclosingClass;");
        if (reader == null) {
            return null;
        }
        return c.getDexCacheType(dex, reader.readType());
    }

    public static AccessibleObject getEnclosingMethodOrConstructor(Class<?> c) {
        Dex dex = c.getDex();
        EncodedValueReader reader = getOnlyAnnotationValue(dex, c, "Ldalvik/annotation/EnclosingMethod;");
        if (reader == null) {
            return null;
        }
        return indexToMethod(c, dex, reader.readMethod());
    }

    public static Class<?>[] getMemberClasses(Class<?> c) {
        Dex dex = c.getDex();
        EncodedValueReader reader = getOnlyAnnotationValue(dex, c, "Ldalvik/annotation/MemberClasses;");
        return reader == null ? EmptyArray.CLASS : (Class[]) decodeValue(c, Class[].class, dex, reader);
    }

    public static String getSignature(AnnotatedElement element) {
        Class<?> dexClass = getDexClass(element);
        Dex dex = dexClass.getDex();
        EncodedValueReader reader = getOnlyAnnotationValue(dex, element, "Ldalvik/annotation/Signature;");
        if (reader == null) {
            return null;
        }
        String[] array = (String[]) decodeValue(dexClass, String[].class, dex, reader);
        StringBuilder result = new StringBuilder();
        for (String s : array) {
            result.append(s);
        }
        return result.toString();
    }

    public static Class<?>[] getExceptions(AnnotatedElement element) {
        Class<?> dexClass = getDexClass(element);
        Dex dex = dexClass.getDex();
        EncodedValueReader reader = getOnlyAnnotationValue(dex, element, "Ldalvik/annotation/Throws;");
        return reader == null ? EmptyArray.CLASS : (Class[]) decodeValue(dexClass, Class[].class, dex, reader);
    }

    public static int getInnerClassFlags(Class<?> c, int defaultValue) {
        Dex dex = c.getDex();
        EncodedValueReader reader = getAnnotationReader(dex, c, "Ldalvik/annotation/InnerClass;", 2);
        if (reader != null) {
            reader.readAnnotationName();
            int defaultValue2 = reader.readInt();
            return defaultValue2;
        }
        return defaultValue;
    }

    public static String getInnerClassName(Class<?> c) {
        Dex dex = c.getDex();
        EncodedValueReader reader = getAnnotationReader(dex, c, "Ldalvik/annotation/InnerClass;", 2);
        if (reader == null) {
            return null;
        }
        reader.readAnnotationName();
        reader.readInt();
        reader.readAnnotationName();
        if (reader.peek() != 30) {
            return (String) decodeValue(c, String.class, dex, reader);
        }
        return null;
    }

    public static boolean isAnonymousClass(Class<?> c) {
        Dex dex = c.getDex();
        EncodedValueReader reader = getAnnotationReader(dex, c, "Ldalvik/annotation/InnerClass;", 2);
        if (reader == null) {
            return false;
        }
        reader.readAnnotationName();
        reader.readInt();
        reader.readAnnotationName();
        return reader.peek() == 30;
    }

    private static EncodedValueReader getAnnotationReader(Dex dex, AnnotatedElement element, String annotationName, int expectedFieldCount) {
        int annotationSetOffset = getAnnotationSetOffset(element);
        if (annotationSetOffset == 0) {
            return null;
        }
        Dex.Section setIn = dex.open(annotationSetOffset);
        com.android.dex.Annotation annotation = null;
        int i = 0;
        int size = setIn.readInt();
        while (true) {
            if (i >= size) {
                break;
            }
            int annotationOffset = setIn.readInt();
            Dex.Section annotationIn = dex.open(annotationOffset);
            com.android.dex.Annotation candidate = annotationIn.readAnnotation();
            String candidateAnnotationName = dex.typeNames().get(candidate.getTypeIndex());
            if (!annotationName.equals(candidateAnnotationName)) {
                i++;
            } else {
                annotation = candidate;
                break;
            }
        }
        if (annotation == null) {
            return null;
        }
        EncodedValueReader reader = annotation.getReader();
        int fieldCount = reader.readAnnotation();
        String readerAnnotationName = dex.typeNames().get(reader.getAnnotationType());
        if (!readerAnnotationName.equals(annotationName)) {
            throw new AssertionError();
        }
        if (fieldCount != expectedFieldCount) {
            return null;
        }
        return reader;
    }

    private static EncodedValueReader getOnlyAnnotationValue(Dex dex, AnnotatedElement element, String annotationName) {
        EncodedValueReader reader = getAnnotationReader(dex, element, annotationName, 1);
        if (reader == null) {
            return null;
        }
        reader.readAnnotationName();
        return reader;
    }

    private static Class<? extends Annotation> getAnnotationClass(Class<?> context, Dex dex, int typeIndex) {
        try {
            Class dexCacheType = context.getDexCacheType(dex, typeIndex);
            if (dexCacheType.isAnnotation()) {
                return dexCacheType;
            }
            throw new IncompatibleClassChangeError("Expected annotation: " + dexCacheType.getName());
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }

    private static AccessibleObject indexToMethod(Class<?> context, Dex dex, int methodIndex) {
        Class<?> declaringClass = context.getDexCacheType(dex, dex.declaringClassIndexFromMethodIndex(methodIndex));
        String name = context.getDexCacheString(dex, dex.nameIndexFromMethodIndex(methodIndex));
        short[] types = dex.parameterTypeIndicesFromMethodIndex(methodIndex);
        Class<?>[] parametersArray = new Class[types.length];
        for (int i = 0; i < types.length; i++) {
            parametersArray[i] = context.getDexCacheType(dex, types[i]);
        }
        try {
            return name.equals("<init>") ? declaringClass.getDeclaredConstructor(parametersArray) : declaringClass.getDeclaredMethod(name, parametersArray);
        } catch (NoSuchMethodException e) {
            throw new IncompatibleClassChangeError("Couldn't find " + declaringClass.getName() + "." + name + Arrays.toString(parametersArray));
        }
    }

    private static List<Annotation> annotationSetToAnnotations(Class<?> context, int offset) {
        Class<? extends Annotation> annotationClass;
        if (offset == 0) {
            return Collections.emptyList();
        }
        Dex dex = context.getDex();
        Dex.Section setIn = dex.open(offset);
        int size = setIn.readInt();
        List<Annotation> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int annotationOffset = setIn.readInt();
            Dex.Section annotationIn = dex.open(annotationOffset);
            com.android.dex.Annotation annotation = annotationIn.readAnnotation();
            if (annotation.getVisibility() == 1 && (annotationClass = getAnnotationClass(context, dex, annotation.getTypeIndex())) != null) {
                result.add(toAnnotationInstance(context, dex, annotationClass, annotation.getReader()));
            }
        }
        return result;
    }

    private static <A extends Annotation> A toAnnotationInstance(Class<?> cls, Class<A> cls2, com.android.dex.Annotation annotation) {
        return (A) toAnnotationInstance(cls, cls.getDex(), cls2, annotation.getReader());
    }

    private static <A extends Annotation> A toAnnotationInstance(Class<?> cls, Dex dex, Class<A> cls2, EncodedValueReader encodedValueReader) {
        int annotation = encodedValueReader.readAnnotation();
        if (cls2 != cls.getDexCacheType(dex, encodedValueReader.getAnnotationType())) {
            throw new AssertionError("annotation value type != return type");
        }
        AnnotationMember[] annotationMemberArr = new AnnotationMember[annotation];
        for (int i = 0; i < annotation; i++) {
            String str = dex.strings().get(encodedValueReader.readAnnotationName());
            try {
                Method method = cls2.getMethod(str, NO_ARGUMENTS);
                Class<?> returnType = method.getReturnType();
                annotationMemberArr[i] = new AnnotationMember(str, decodeValue(cls, returnType, dex, encodedValueReader), returnType, method);
            } catch (NoSuchMethodException e) {
                throw new IncompatibleClassChangeError("Couldn't find " + cls2.getName() + "." + str);
            }
        }
        return (A) AnnotationFactory.createAnnotation(cls2, annotationMemberArr);
    }

    private static Object decodeValue(Class<?> context, Class<?> type, Dex dex, EncodedValueReader reader) {
        if (type.isArray()) {
            int size = reader.readArray();
            Class<?> componentType = type.getComponentType();
            Object array = Array.newInstance(componentType, size);
            for (int i = 0; i < size; i++) {
                Array.set(array, i, decodeValue(context, componentType, dex, reader));
            }
            return array;
        }
        if (type.isEnum()) {
            int fieldIndex = reader.readEnum();
            FieldId fieldId = dex.fieldIds().get(fieldIndex);
            String fieldName = dex.strings().get(fieldId.getNameIndex());
            try {
                Field field = type.getDeclaredField(fieldName);
                return field.get(null);
            } catch (IllegalAccessException e) {
                IllegalAccessError error = new IllegalAccessError();
                error.initCause(e);
                throw error;
            } catch (NoSuchFieldException e2) {
                NoSuchFieldError error2 = new NoSuchFieldError();
                error2.initCause(e2);
                throw error2;
            }
        }
        if (type.isAnnotation()) {
            return toAnnotationInstance(context, dex, type, reader);
        }
        if (type == String.class) {
            int index = reader.readString();
            return context.getDexCacheString(dex, index);
        }
        if (type == Class.class) {
            int index2 = reader.readType();
            return context.getDexCacheType(dex, index2);
        }
        if (type == Byte.TYPE) {
            return Byte.valueOf(reader.readByte());
        }
        if (type == Short.TYPE) {
            return Short.valueOf(reader.readShort());
        }
        if (type == Integer.TYPE) {
            return Integer.valueOf(reader.readInt());
        }
        if (type == Long.TYPE) {
            return Long.valueOf(reader.readLong());
        }
        if (type == Float.TYPE) {
            return Float.valueOf(reader.readFloat());
        }
        if (type == Double.TYPE) {
            return Double.valueOf(reader.readDouble());
        }
        if (type == Character.TYPE) {
            return Character.valueOf(reader.readChar());
        }
        if (type == Boolean.TYPE) {
            return Boolean.valueOf(reader.readBoolean());
        }
        throw new AssertionError("Unexpected annotation value type: " + type);
    }
}

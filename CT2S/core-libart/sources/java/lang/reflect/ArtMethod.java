package java.lang.reflect;

import com.android.dex.Dex;
import java.lang.annotation.Annotation;
import libcore.reflect.AnnotationAccess;
import libcore.util.EmptyArray;

public final class ArtMethod {
    private int accessFlags;
    private Class<?> declaringClass;
    private ArtMethod[] dexCacheResolvedMethods;
    Class<?>[] dexCacheResolvedTypes;
    private int dexCodeItemOffset;
    private int dexMethodIndex;
    private int methodIndex;

    private ArtMethod() {
    }

    Class getDeclaringClass() {
        return this.declaringClass;
    }

    public int getAccessFlags() {
        return this.accessFlags;
    }

    int getDexMethodIndex() {
        return this.dexMethodIndex;
    }

    public static String getMethodName(ArtMethod artMethod) {
        ArtMethod artMethod2 = artMethod.findOverriddenMethodIfProxy();
        Dex dex = artMethod2.getDeclaringClass().getDex();
        int nameIndex = dex.nameIndexFromMethodIndex(artMethod2.getDexMethodIndex());
        return artMethod2.getDexCacheString(dex, nameIndex);
    }

    public static boolean equalConstructorParameters(ArtMethod artMethod, Class<?>[] params) {
        Dex dex = artMethod.getDeclaringClass().getDex();
        short[] types = dex.parameterTypeIndicesFromMethodIndex(artMethod.getDexMethodIndex());
        if (types.length != params.length) {
            return false;
        }
        for (int i = 0; i < types.length; i++) {
            if (artMethod.getDexCacheType(dex, types[i]) != params[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equalMethodParameters(ArtMethod artMethod, Class<?>[] params) {
        return equalConstructorParameters(artMethod.findOverriddenMethodIfProxy(), params);
    }

    Class<?>[] getParameterTypes() {
        Dex dex = getDeclaringClass().getDex();
        short[] types = dex.parameterTypeIndicesFromMethodIndex(this.dexMethodIndex);
        if (types.length == 0) {
            return EmptyArray.CLASS;
        }
        Class<?>[] parametersArray = new Class[types.length];
        for (int i = 0; i < types.length; i++) {
            parametersArray[i] = getDexCacheType(dex, types[i]);
        }
        return parametersArray;
    }

    Class<?> getReturnType() {
        Dex dex = this.declaringClass.getDex();
        int returnTypeIndex = dex.returnTypeIndexFromMethodIndex(this.dexMethodIndex);
        return getDexCacheType(dex, returnTypeIndex);
    }

    int compareParameters(Class<?>[] params) {
        int comparison;
        Dex dex = getDeclaringClass().getDex();
        short[] types = dex.parameterTypeIndicesFromMethodIndex(this.dexMethodIndex);
        int length = Math.min(types.length, params.length);
        for (int i = 0; i < length; i++) {
            Class<?> aType = getDexCacheType(dex, types[i]);
            Class<?> bType = params[i];
            if (aType != bType && (comparison = aType.getName().compareTo(bType.getName())) != 0) {
                return comparison;
            }
        }
        return types.length - params.length;
    }

    Annotation[][] getParameterAnnotations() {
        return AnnotationAccess.getParameterAnnotations(this.declaringClass, this.dexMethodIndex);
    }

    private String getDexCacheString(Dex dex, int dexStringIndex) {
        return this.declaringClass.getDexCacheString(dex, dexStringIndex);
    }

    private Class<?> getDexCacheType(Dex dex, int dexTypeIndex) {
        Class<?> resolvedType = this.dexCacheResolvedTypes[dexTypeIndex];
        if (resolvedType == null) {
            return this.declaringClass.getDexCacheType(dex, dexTypeIndex);
        }
        return resolvedType;
    }

    ArtMethod findOverriddenMethodIfProxy() {
        if (this.declaringClass.isProxy()) {
            return this.dexCacheResolvedMethods[this.dexMethodIndex];
        }
        return this;
    }
}

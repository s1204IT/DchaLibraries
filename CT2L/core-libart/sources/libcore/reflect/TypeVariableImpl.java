package libcore.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public final class TypeVariableImpl<D extends GenericDeclaration> implements TypeVariable<D> {
    private ListOfTypes bounds;
    private final GenericDeclaration declOfVarUser;
    private TypeVariableImpl<D> formalVar;
    private D genericDeclaration;
    private final String name;

    public boolean equals(Object o) {
        if (!(o instanceof TypeVariable)) {
            return false;
        }
        TypeVariable<?> that = (TypeVariable) o;
        return getName().equals(that.getName()) && getGenericDeclaration().equals(that.getGenericDeclaration());
    }

    public int hashCode() {
        return (getName().hashCode() * 31) + getGenericDeclaration().hashCode();
    }

    TypeVariableImpl(D genericDecl, String name, ListOfTypes bounds) {
        this.genericDeclaration = genericDecl;
        this.name = name;
        this.bounds = bounds;
        this.formalVar = this;
        this.declOfVarUser = null;
    }

    TypeVariableImpl(D genericDecl, String name) {
        this.name = name;
        this.declOfVarUser = genericDecl;
    }

    static TypeVariable findFormalVar(GenericDeclaration layer, String name) {
        for (TypeVariable<?> typeVariable : layer.getTypeParameters()) {
            if (name.equals(typeVariable.getName())) {
                return typeVariable;
            }
        }
        return null;
    }

    private static GenericDeclaration nextLayer(GenericDeclaration decl) {
        if (decl instanceof Class) {
            Class cl = (Class) decl;
            GenericDeclaration decl2 = (GenericDeclaration) AnnotationAccess.getEnclosingMethodOrConstructor(cl);
            if (decl2 == null) {
                return cl.getEnclosingClass();
            }
            return decl2;
        }
        if (decl instanceof Method) {
            return ((Method) decl).getDeclaringClass();
        }
        if (decl instanceof Constructor) {
            return ((Constructor) decl).getDeclaringClass();
        }
        throw new AssertionError();
    }

    void resolve() {
        if (this.formalVar == null) {
            GenericDeclaration curLayer = this.declOfVarUser;
            do {
                TypeVariable var = findFormalVar(curLayer, this.name);
                if (var == null) {
                    curLayer = nextLayer(curLayer);
                } else {
                    this.formalVar = (TypeVariableImpl) var;
                    this.genericDeclaration = this.formalVar.genericDeclaration;
                    this.bounds = this.formalVar.bounds;
                    return;
                }
            } while (curLayer != null);
            throw new AssertionError("illegal type variable reference");
        }
    }

    @Override
    public Type[] getBounds() {
        resolve();
        return (Type[]) this.bounds.getResolvedTypes().clone();
    }

    @Override
    public D getGenericDeclaration() {
        resolve();
        return this.genericDeclaration;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }
}

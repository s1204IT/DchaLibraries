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

    public boolean equals(Object obj) {
        if (!(obj instanceof TypeVariable)) {
            return false;
        }
        TypeVariable typeVariable = (TypeVariable) obj;
        return getName().equals(typeVariable.getName()) && getGenericDeclaration().equals(typeVariable.getGenericDeclaration());
    }

    public int hashCode() {
        return (31 * getName().hashCode()) + getGenericDeclaration().hashCode();
    }

    TypeVariableImpl(D d, String str, ListOfTypes listOfTypes) {
        this.genericDeclaration = d;
        this.name = str;
        this.bounds = listOfTypes;
        this.formalVar = this;
        this.declOfVarUser = null;
    }

    TypeVariableImpl(D d, String str) {
        this.name = str;
        this.declOfVarUser = d;
    }

    static TypeVariable findFormalVar(GenericDeclaration genericDeclaration, String str) {
        for (TypeVariable<?> typeVariable : genericDeclaration.getTypeParameters()) {
            if (str.equals(typeVariable.getName())) {
                return typeVariable;
            }
        }
        return null;
    }

    private static GenericDeclaration nextLayer(GenericDeclaration genericDeclaration) {
        if (genericDeclaration instanceof Class) {
            Class cls = (Class) genericDeclaration;
            GenericDeclaration enclosingMethod = cls.getEnclosingMethod();
            if (enclosingMethod == null) {
                enclosingMethod = cls.getEnclosingConstructor();
            }
            if (enclosingMethod != null) {
                return enclosingMethod;
            }
            return cls.getEnclosingClass();
        }
        if (genericDeclaration instanceof Method) {
            return ((Method) genericDeclaration).getDeclaringClass();
        }
        if (genericDeclaration instanceof Constructor) {
            return ((Constructor) genericDeclaration).getDeclaringClass();
        }
        throw new AssertionError();
    }

    void resolve() {
        if (this.formalVar != null) {
            return;
        }
        GenericDeclaration genericDeclarationNextLayer = this.declOfVarUser;
        do {
            TypeVariable typeVariableFindFormalVar = findFormalVar(genericDeclarationNextLayer, this.name);
            if (typeVariableFindFormalVar == null) {
                genericDeclarationNextLayer = nextLayer(genericDeclarationNextLayer);
            } else {
                this.formalVar = (TypeVariableImpl) typeVariableFindFormalVar;
                this.genericDeclaration = this.formalVar.genericDeclaration;
                this.bounds = this.formalVar.bounds;
                return;
            }
        } while (genericDeclarationNextLayer != null);
        throw new AssertionError("illegal type variable reference");
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

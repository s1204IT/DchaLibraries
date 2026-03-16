package libcore.reflect;

import java.lang.reflect.TypeVariable;
import java.util.ArrayList;

final class ListOfVariables {
    final ArrayList<TypeVariable<?>> array = new ArrayList<>();

    ListOfVariables() {
    }

    void add(TypeVariable<?> elem) {
        this.array.add(elem);
    }

    TypeVariable<?>[] getArray() {
        TypeVariable<?>[] a = new TypeVariable[this.array.size()];
        return (TypeVariable[]) this.array.toArray(a);
    }
}

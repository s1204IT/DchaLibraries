package java.lang.invoke;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

public class MethodHandleImpl extends MethodHandle implements Cloneable {
    private HandleInfo info;

    public native Member getMemberInternal();

    MethodHandleImpl(long j, int i, MethodType methodType) {
        super(j, i, methodType);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    MethodHandleInfo reveal() {
        if (this.info == null) {
            this.info = new HandleInfo(getMemberInternal(), this);
        }
        return this.info;
    }

    static class HandleInfo implements MethodHandleInfo {
        private final MethodHandle handle;
        private final Member member;

        HandleInfo(Member member, MethodHandle methodHandle) {
            this.member = member;
            this.handle = methodHandle;
        }

        @Override
        public int getReferenceKind() {
            int handleKind = this.handle.getHandleKind();
            switch (handleKind) {
                case 0:
                    if (this.member.getDeclaringClass().isInterface()) {
                        return 9;
                    }
                    return 5;
                case 1:
                    return 7;
                case 2:
                    return this.member instanceof Constructor ? 8 : 7;
                case 3:
                    return 6;
                default:
                    switch (handleKind) {
                        case 9:
                            return 1;
                        case 10:
                            return 3;
                        case 11:
                            return 2;
                        case 12:
                            return 4;
                        default:
                            throw new AssertionError((Object) ("Unexpected handle kind: " + this.handle.getHandleKind()));
                    }
            }
        }

        @Override
        public Class<?> getDeclaringClass() {
            return this.member.getDeclaringClass();
        }

        @Override
        public String getName() {
            if (this.member instanceof Constructor) {
                return "<init>";
            }
            return this.member.getName();
        }

        @Override
        public MethodType getMethodType() {
            MethodType methodTypeChangeReturnType;
            boolean z;
            MethodType methodTypeType = this.handle.type();
            if (this.member instanceof Constructor) {
                methodTypeChangeReturnType = methodTypeType.changeReturnType(Void.TYPE);
                z = true;
            } else {
                methodTypeChangeReturnType = methodTypeType;
                z = false;
            }
            int handleKind = this.handle.getHandleKind();
            if (handleKind != 4) {
                switch (handleKind) {
                    case 0:
                    case 1:
                    case 2:
                        z = true;
                        break;
                    default:
                        switch (handleKind) {
                            case 9:
                            case 10:
                                z = true;
                                break;
                        }
                        break;
                }
            } else {
                z = true;
            }
            return z ? methodTypeChangeReturnType.dropParameterTypes(0, 1) : methodTypeChangeReturnType;
        }

        @Override
        public <T extends Member> T reflectAs(Class<T> cls, MethodHandles.Lookup lookup) {
            try {
                lookup.checkAccess(this.member.getDeclaringClass(), this.member.getDeclaringClass(), this.member.getModifiers(), this.member.getName());
                return (T) this.member;
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Unable to access member.", e);
            }
        }

        @Override
        public int getModifiers() {
            return this.member.getModifiers();
        }
    }
}

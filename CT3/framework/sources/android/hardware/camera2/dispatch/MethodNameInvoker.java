package android.hardware.camera2.dispatch;

import android.hardware.camera2.utils.UncheckedThrow;
import com.android.internal.util.Preconditions;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class MethodNameInvoker<T> {
    private final ConcurrentHashMap<String, Method> mMethods = new ConcurrentHashMap<>();
    private final Dispatchable<T> mTarget;
    private final Class<T> mTargetClass;

    public MethodNameInvoker(Dispatchable<T> target, Class<T> targetClass) {
        this.mTargetClass = targetClass;
        this.mTarget = target;
    }

    public <K> K invoke(String str, Object... objArr) throws Throwable {
        Preconditions.checkNotNull(str, "methodName must not be null");
        Method method = this.mMethods.get(str);
        if (method == null) {
            Method[] methods = this.mTargetClass.getMethods();
            int i = 0;
            int length = methods.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                Method method2 = methods[i];
                if (!method2.getName().equals(str) || objArr.length != method2.getParameterTypes().length) {
                    i++;
                } else {
                    method = method2;
                    this.mMethods.put(str, method2);
                    break;
                }
            }
            if (method == null) {
                throw new IllegalArgumentException("Method " + str + " does not exist on class " + this.mTargetClass);
            }
        }
        try {
            return (K) this.mTarget.dispatch(method, objArr);
        } catch (Throwable th) {
            UncheckedThrow.throwAnyException(th);
            return null;
        }
    }
}

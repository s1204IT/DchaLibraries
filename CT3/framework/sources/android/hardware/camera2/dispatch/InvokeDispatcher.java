package android.hardware.camera2.dispatch;

import android.hardware.camera2.utils.UncheckedThrow;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class InvokeDispatcher<T> implements Dispatchable<T> {
    private static final String TAG = "InvocationSink";
    private final T mTarget;

    public InvokeDispatcher(T t) {
        this.mTarget = (T) Preconditions.checkNotNull(t, "target must not be null");
    }

    @Override
    public Object dispatch(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(this.mTarget, args);
        } catch (IllegalAccessException e) {
            Log.wtf(TAG, "IllegalAccessException while invoking " + method, e);
            return null;
        } catch (IllegalArgumentException e2) {
            Log.wtf(TAG, "IllegalArgumentException while invoking " + method, e2);
            return null;
        } catch (InvocationTargetException e3) {
            Throwable t = e3.getTargetException();
            UncheckedThrow.throwAnyException(t);
            return null;
        }
    }
}

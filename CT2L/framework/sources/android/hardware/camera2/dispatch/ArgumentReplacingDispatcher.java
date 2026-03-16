package android.hardware.camera2.dispatch;

import com.android.internal.util.Preconditions;
import java.lang.reflect.Method;

public class ArgumentReplacingDispatcher<T, TArg> implements Dispatchable<T> {
    private final int mArgumentIndex;
    private final TArg mReplaceWith;
    private final Dispatchable<T> mTarget;

    public ArgumentReplacingDispatcher(Dispatchable<T> dispatchable, int i, TArg targ) {
        this.mTarget = (Dispatchable) Preconditions.checkNotNull(dispatchable, "target must not be null");
        this.mArgumentIndex = Preconditions.checkArgumentNonnegative(i, "argumentIndex must not be negative");
        this.mReplaceWith = (TArg) Preconditions.checkNotNull(targ, "replaceWith must not be null");
    }

    @Override
    public Object dispatch(Method method, Object[] args) throws Throwable {
        if (args.length > this.mArgumentIndex) {
            args = arrayCopy(args);
            args[this.mArgumentIndex] = this.mReplaceWith;
        }
        return this.mTarget.dispatch(method, args);
    }

    private static Object[] arrayCopy(Object[] array) {
        int length = array.length;
        Object[] newArray = new Object[length];
        for (int i = 0; i < length; i++) {
            newArray[i] = array[i];
        }
        return newArray;
    }
}

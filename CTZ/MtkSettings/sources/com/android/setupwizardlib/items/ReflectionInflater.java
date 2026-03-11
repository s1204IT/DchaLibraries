package com.android.setupwizardlib.items;

import android.content.Context;
import android.util.AttributeSet;
import android.view.InflateException;
import java.lang.reflect.Constructor;
import java.util.HashMap;

public abstract class ReflectionInflater<T> extends SimpleInflater<T> {
    private static final Class<?>[] CONSTRUCTOR_SIGNATURE = {Context.class, AttributeSet.class};
    private static final HashMap<String, Constructor<?>> sConstructorMap = new HashMap<>();
    private final Context mContext;
    private String mDefaultPackage;
    private final Object[] mTempConstructorArgs;

    protected ReflectionInflater(Context context) {
        super(context.getResources());
        this.mTempConstructorArgs = new Object[2];
        this.mContext = context;
    }

    public final T createItem(String str, String str2, AttributeSet attributeSet) {
        String strConcat;
        if (str2 != null && str.indexOf(46) == -1) {
            strConcat = str2.concat(str);
        } else {
            strConcat = str;
        }
        Constructor<?> constructor = sConstructorMap.get(strConcat);
        if (constructor == null) {
            try {
                constructor = this.mContext.getClassLoader().loadClass(strConcat).getConstructor(CONSTRUCTOR_SIGNATURE);
                constructor.setAccessible(true);
                sConstructorMap.put(str, constructor);
            } catch (Exception e) {
                throw new InflateException(attributeSet.getPositionDescription() + ": Error inflating class " + strConcat, e);
            }
        }
        this.mTempConstructorArgs[0] = this.mContext;
        this.mTempConstructorArgs[1] = attributeSet;
        T t = (T) constructor.newInstance(this.mTempConstructorArgs);
        this.mTempConstructorArgs[0] = null;
        this.mTempConstructorArgs[1] = null;
        return t;
    }

    @Override
    protected T onCreateItem(String str, AttributeSet attributeSet) {
        return createItem(str, this.mDefaultPackage, attributeSet);
    }

    public void setDefaultPackage(String str) {
        this.mDefaultPackage = str;
    }
}

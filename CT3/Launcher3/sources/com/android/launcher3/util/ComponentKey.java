package com.android.launcher3.util;

import android.content.ComponentName;
import android.content.Context;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import java.util.Arrays;

public class ComponentKey {

    static final boolean f3assertionsDisabled;
    public final ComponentName componentName;
    private final int mHashCode;
    public final UserHandleCompat user;

    static {
        f3assertionsDisabled = !ComponentKey.class.desiredAssertionStatus();
    }

    public ComponentKey(ComponentName componentName, UserHandleCompat user) {
        if (!f3assertionsDisabled) {
            if (!(componentName != null)) {
                throw new AssertionError();
            }
        }
        if (!f3assertionsDisabled) {
            if (!(user != null)) {
                throw new AssertionError();
            }
        }
        this.componentName = componentName;
        this.user = user;
        this.mHashCode = Arrays.hashCode(new Object[]{componentName, user});
    }

    public String flattenToString(Context context) {
        String flattened = this.componentName.flattenToString();
        if (this.user != null) {
            return flattened + "#" + UserManagerCompat.getInstance(context).getSerialNumberForUser(this.user);
        }
        return flattened;
    }

    public int hashCode() {
        return this.mHashCode;
    }

    public boolean equals(Object o) {
        ComponentKey other = (ComponentKey) o;
        if (other.componentName.equals(this.componentName)) {
            return other.user.equals(this.user);
        }
        return false;
    }
}

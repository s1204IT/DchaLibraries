package com.android.contacts.compat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.telecom.PhoneAccount;
import com.mediatek.contacts.util.Log;

public class PhoneAccountCompat {
    private static final String TAG = PhoneAccountCompat.class.getSimpleName();

    public static Icon getIcon(PhoneAccount phoneAccount) {
        if (phoneAccount == null || !CompatUtils.isMarshmallowCompatible()) {
            return null;
        }
        return phoneAccount.getIcon();
    }

    public static Drawable createIconDrawable(PhoneAccount phoneAccount, Context context) {
        if (phoneAccount == null || context == null) {
            return null;
        }
        if (CompatUtils.isMarshmallowCompatible()) {
            return createIconDrawableMarshmallow(phoneAccount, context);
        }
        if (!CompatUtils.isLollipopMr1Compatible()) {
            return null;
        }
        return createIconDrawableLollipopMr1(phoneAccount, context);
    }

    private static Drawable createIconDrawableMarshmallow(PhoneAccount phoneAccount, Context context) {
        Icon icon = getIcon(phoneAccount);
        if (icon == null) {
            return null;
        }
        return icon.loadDrawable(context);
    }

    private static Drawable createIconDrawableLollipopMr1(PhoneAccount phoneAccount, Context context) {
        try {
            return (Drawable) PhoneAccount.class.getMethod("createIconDrawable", Context.class).invoke(phoneAccount, context);
        } catch (ReflectiveOperationException e) {
            return null;
        } catch (Throwable th) {
            Log.e(TAG, "Unexpected exception when attempting to call android.telecom.PhoneAccount#createIconDrawable", th);
            return null;
        }
    }
}

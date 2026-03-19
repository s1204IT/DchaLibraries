package com.android.contacts.util;

import android.content.Context;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.DeviceLocalAccountType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface DeviceLocalAccountTypeFactory {
    public static final int TYPE_DEVICE = 1;
    public static final int TYPE_OTHER = 0;
    public static final int TYPE_SIM = 2;

    @Retention(RetentionPolicy.SOURCE)
    public @interface LocalAccountType {
    }

    int classifyAccount(String str);

    AccountType getAccountType(String str);

    public static class Util {
        private Util() {
        }

        public static boolean isLocalAccountType(int i) {
            return i == 2 || i == 1;
        }

        public static boolean isLocalAccountType(DeviceLocalAccountTypeFactory deviceLocalAccountTypeFactory, String str) {
            return isLocalAccountType(deviceLocalAccountTypeFactory.classifyAccount(str));
        }
    }

    public static class Default implements DeviceLocalAccountTypeFactory {
        private Context mContext;

        public Default(Context context) {
            this.mContext = context;
        }

        @Override
        public int classifyAccount(String str) {
            return str == null ? 1 : 0;
        }

        @Override
        public AccountType getAccountType(String str) {
            if (str != null) {
                throw new IllegalArgumentException(str + " is not a device account type.");
            }
            return new DeviceLocalAccountType(this.mContext);
        }
    }
}

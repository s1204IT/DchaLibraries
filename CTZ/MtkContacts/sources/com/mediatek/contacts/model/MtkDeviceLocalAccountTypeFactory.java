package com.mediatek.contacts.model;

import android.content.Context;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.DeviceLocalAccountType;
import com.android.contacts.model.account.FallbackAccountType;
import com.android.contacts.util.DeviceLocalAccountTypeFactory;
import com.mediatek.contacts.model.account.CsimAccountType;
import com.mediatek.contacts.model.account.GsimAccountType;
import com.mediatek.contacts.model.account.LocalPhoneAccountType;
import com.mediatek.contacts.model.account.RuimAccountType;
import com.mediatek.contacts.model.account.UsimAccountType;
import com.mediatek.contacts.util.Log;

public class MtkDeviceLocalAccountTypeFactory implements DeviceLocalAccountTypeFactory {
    private Context mContext;

    public MtkDeviceLocalAccountTypeFactory(Context context) {
        Log.i("MtkDeviceLocalAccountTypeFactory", "Constructor");
        this.mContext = context;
    }

    @Override
    public int classifyAccount(String str) {
        int i = 0;
        if (str == null) {
            Log.e("MtkDeviceLocalAccountTypeFactory", "[classifyAccount] accountType should not be null !");
            return 0;
        }
        switch (str) {
            case "SIM Account":
            case "USIM Account":
            case "CSIM Account":
            case "RUIM Account":
                i = 2;
                break;
            case "Local Phone Account":
                i = 1;
                break;
        }
        Log.i("MtkDeviceLocalAccountTypeFactory", "[classifyAccount]accountType=" + str + ", type=" + i);
        return i;
    }

    @Override
    public AccountType getAccountType(String str) {
        Log.i("MtkDeviceLocalAccountTypeFactory", "[getAccountType]accountType=" + str);
        if (str == null) {
            return new DeviceLocalAccountType(this.mContext);
        }
        switch (str) {
            case "SIM Account":
                return new GsimAccountType(this.mContext, null);
            case "USIM Account":
                return new UsimAccountType(this.mContext, null);
            case "CSIM Account":
                return new CsimAccountType(this.mContext, null);
            case "RUIM Account":
                return new RuimAccountType(this.mContext, null);
            case "Local Phone Account":
                return new LocalPhoneAccountType(this.mContext, null);
            default:
                Log.i("MtkDeviceLocalAccountTypeFactory", str + " is not a device account type.");
                return new FallbackAccountType(this.mContext);
        }
    }
}

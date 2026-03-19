package com.android.managedprovisioning.parser;

import android.content.Context;
import android.content.Intent;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.IllegalProvisioningArgumentException;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;

public class MessageParser implements ProvisioningDataParser {
    private final Context mContext;
    private final Utils mUtils;

    public MessageParser(Context context) {
        this(context, new Utils());
    }

    MessageParser(Context context, Utils utils) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
    }

    @Override
    public ProvisioningParams parse(Intent intent) throws IllegalProvisioningArgumentException {
        return getParser(intent).parse(intent);
    }

    ProvisioningDataParser getParser(Intent intent) {
        if ("android.nfc.action.NDEF_DISCOVERED".equals(intent.getAction())) {
            return new PropertiesProvisioningDataParser(this.mContext, this.mUtils);
        }
        return new ExtrasProvisioningDataParser(this.mContext, this.mUtils);
    }
}

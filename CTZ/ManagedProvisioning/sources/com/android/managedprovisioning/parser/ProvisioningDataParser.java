package com.android.managedprovisioning.parser;

import android.content.Intent;
import com.android.managedprovisioning.common.IllegalProvisioningArgumentException;
import com.android.managedprovisioning.model.ProvisioningParams;

public interface ProvisioningDataParser {
    ProvisioningParams parse(Intent intent) throws IllegalProvisioningArgumentException;
}

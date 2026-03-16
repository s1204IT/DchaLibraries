package org.apache.harmony.security.fortress;

import java.security.Provider;
import java.util.List;

public interface SecurityAccess {
    List<String> getAliases(Provider.Service service);

    Provider.Service getService(Provider provider, String str);

    void renumProviders();
}

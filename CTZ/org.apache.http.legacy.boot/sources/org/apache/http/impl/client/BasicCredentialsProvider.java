package org.apache.http.impl.client;

import java.util.HashMap;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;

@Deprecated
public class BasicCredentialsProvider implements CredentialsProvider {
    private final HashMap<AuthScope, Credentials> credMap = new HashMap<>();

    @Override
    public synchronized void setCredentials(AuthScope authScope, Credentials credentials) {
        if (authScope == null) {
            throw new IllegalArgumentException("Authentication scope may not be null");
        }
        this.credMap.put(authScope, credentials);
    }

    private static Credentials matchCredentials(HashMap<AuthScope, Credentials> map, AuthScope authScope) {
        Credentials credentials = map.get(authScope);
        if (credentials == null) {
            int i = -1;
            AuthScope authScope2 = null;
            for (AuthScope authScope3 : map.keySet()) {
                int iMatch = authScope.match(authScope3);
                if (iMatch > i) {
                    authScope2 = authScope3;
                    i = iMatch;
                }
            }
            if (authScope2 != null) {
                return map.get(authScope2);
            }
            return credentials;
        }
        return credentials;
    }

    @Override
    public synchronized Credentials getCredentials(AuthScope authScope) {
        if (authScope == null) {
            throw new IllegalArgumentException("Authentication scope may not be null");
        }
        return matchCredentials(this.credMap, authScope);
    }

    public String toString() {
        return this.credMap.toString();
    }

    @Override
    public synchronized void clear() {
        this.credMap.clear();
    }
}

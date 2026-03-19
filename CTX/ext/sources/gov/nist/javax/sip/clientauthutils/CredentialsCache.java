package gov.nist.javax.sip.clientauthutils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import javax.sip.header.AuthorizationHeader;

class CredentialsCache {
    private ConcurrentHashMap<String, List<AuthorizationHeader>> authorizationHeaders = new ConcurrentHashMap<>();
    private Timer timer;

    class TimeoutTask extends TimerTask {
        String callId;
        String userName;

        public TimeoutTask(String str, String str2) {
            this.callId = str2;
            this.userName = str;
        }

        @Override
        public void run() {
            CredentialsCache.this.authorizationHeaders.remove(this.callId);
        }
    }

    CredentialsCache(Timer timer) {
        this.timer = timer;
    }

    void cacheAuthorizationHeader(String str, AuthorizationHeader authorizationHeader, int i) {
        String username = authorizationHeader.getUsername();
        if (str == null) {
            throw new NullPointerException("Call ID is null!");
        }
        if (authorizationHeader == null) {
            throw new NullPointerException("Null authorization domain");
        }
        List<AuthorizationHeader> linkedList = this.authorizationHeaders.get(str);
        if (linkedList == null) {
            linkedList = new LinkedList<>();
            this.authorizationHeaders.put(str, linkedList);
        } else {
            String realm = authorizationHeader.getRealm();
            ListIterator<AuthorizationHeader> listIterator = linkedList.listIterator();
            while (listIterator.hasNext()) {
                if (realm.equals(listIterator.next().getRealm())) {
                    listIterator.remove();
                }
            }
        }
        linkedList.add(authorizationHeader);
        TimeoutTask timeoutTask = new TimeoutTask(str, username);
        if (i != -1) {
            this.timer.schedule(timeoutTask, i * 1000);
        }
    }

    Collection<AuthorizationHeader> getCachedAuthorizationHeaders(String str) {
        if (str == null) {
            throw new NullPointerException("Null arg!");
        }
        return this.authorizationHeaders.get(str);
    }

    public void removeAuthenticationHeader(String str) {
        this.authorizationHeaders.remove(str);
    }
}

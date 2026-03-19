package android.telecom;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class AuthenticatorService extends Service {
    private static Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }

    public class Authenticator extends AbstractAccountAuthenticator {
        public Authenticator(Context context) {
            super(context);
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String str) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse, String str, String str2, String[] strArr, Bundle bundle) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String str, Bundle bundle) throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAuthTokenLabel(String str) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String str, Bundle bundle) throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strArr) throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }
    }
}

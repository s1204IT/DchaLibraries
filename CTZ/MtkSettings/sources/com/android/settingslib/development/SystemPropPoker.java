package com.android.settingslib.development;

import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class SystemPropPoker {
    private static final SystemPropPoker sInstance = new SystemPropPoker();
    private boolean mBlockPokes = false;

    private SystemPropPoker() {
    }

    public static SystemPropPoker getInstance() {
        return sInstance;
    }

    public void blockPokes() {
        this.mBlockPokes = true;
    }

    public void unblockPokes() {
        this.mBlockPokes = false;
    }

    public void poke() {
        if (!this.mBlockPokes) {
            createPokerTask().execute(new Void[0]);
        }
    }

    PokerTask createPokerTask() {
        return new PokerTask();
    }

    public static class PokerTask extends AsyncTask<Void, Void, Void> {
        String[] listServices() {
            return ServiceManager.listServices();
        }

        IBinder checkService(String str) {
            return ServiceManager.checkService(str);
        }

        @Override
        public Void doInBackground(Void... voidArr) {
            String[] strArrListServices = listServices();
            if (strArrListServices == null) {
                Log.e("SystemPropPoker", "There are no services, how odd");
                return null;
            }
            for (String str : strArrListServices) {
                IBinder iBinderCheckService = checkService(str);
                if (iBinderCheckService != null) {
                    Parcel parcelObtain = Parcel.obtain();
                    try {
                        iBinderCheckService.transact(1599295570, parcelObtain, null, 0);
                    } catch (RemoteException e) {
                    } catch (Exception e2) {
                        Log.i("SystemPropPoker", "Someone wrote a bad service '" + str + "' that doesn't like to be poked", e2);
                    }
                    parcelObtain.recycle();
                }
            }
            return null;
        }
    }
}

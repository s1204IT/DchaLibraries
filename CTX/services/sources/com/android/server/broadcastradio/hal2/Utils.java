package com.android.server.broadcastradio.hal2;

import android.os.RemoteException;

class Utils {
    private static final String TAG = "BcRadio2Srv.utils";

    interface FuncThrowingRemoteException<T> {
        T exec() throws RemoteException;
    }

    interface VoidFuncThrowingRemoteException {
        void exec() throws RemoteException;
    }

    Utils() {
    }

    static FrequencyBand getBand(int i) {
        return i < 30 ? FrequencyBand.UNKNOWN : i < 500 ? FrequencyBand.AM_LW : i < 1705 ? FrequencyBand.AM_MW : i < 30000 ? FrequencyBand.AM_SW : i < 60000 ? FrequencyBand.UNKNOWN : i < 110000 ? FrequencyBand.FM : FrequencyBand.UNKNOWN;
    }

    static <T> T maybeRethrow(FuncThrowingRemoteException<T> funcThrowingRemoteException) {
        try {
            return funcThrowingRemoteException.exec();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    static void maybeRethrow(VoidFuncThrowingRemoteException voidFuncThrowingRemoteException) {
        try {
            voidFuncThrowingRemoteException.exec();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }
}

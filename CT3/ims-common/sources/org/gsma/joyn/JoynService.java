package org.gsma.joyn;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.IInterface;
import java.lang.reflect.Method;
import java.util.List;

public abstract class JoynService {
    public static final String ACTION_RCS_SERVICE_UP = "org.gsma.joyn.action.RCS_SERVICE_UP";
    public static final String TAG = "TAPI-JoynService";
    protected Context ctx;
    protected JoynServiceListener serviceListener;
    private IInterface api = null;
    protected Integer version = null;

    public abstract void connect();

    public abstract void disconnect();

    public static class Error {
        public static final int CONNECTION_LOST = 2;
        public static final int INTERNAL_ERROR = 0;
        public static final int SERVICE_DISABLED = 1;

        private Error() {
        }
    }

    public JoynService(Context ctx, JoynServiceListener listener) {
        Logger.d(TAG, "JoynService() constructor " + ctx + " listener = " + listener);
        this.ctx = ctx;
        this.serviceListener = listener;
    }

    private Object callApiMethod(String method, Object param, Class paramClass) throws JoynServiceException {
        if (this.api != null) {
            Class<?> cls = this.api.getClass();
            try {
                if (param != null) {
                    Method m = cls.getDeclaredMethod(method, paramClass);
                    return m.invoke(this.api, param);
                }
                Method m2 = cls.getDeclaredMethod(method, null);
                return m2.invoke(this.api, new Object[0]);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    protected void setApi(IInterface api) {
        this.api = api;
    }

    public boolean isServiceConnected() {
        return this.api != null;
    }

    public int getServiceVersion() throws JoynServiceException {
        Logger.d(TAG, "getServiceVersion() entry " + this.api);
        if (this.api != null) {
            if (this.version == null) {
                try {
                    this.version = (Integer) callApiMethod("getServiceVersion", null, null);
                } catch (Exception e) {
                    throw new JoynServiceException(e.getMessage());
                }
            }
            return this.version.intValue();
        }
        throw new JoynServiceNotAvailableException();
    }

    public boolean isServiceRegistered() throws JoynServiceException {
        Logger.d(TAG, "isServiceRegistered() entry " + this.api);
        if (this.api != null) {
            return ((Boolean) callApiMethod("isServiceRegistered", null, null)).booleanValue();
        }
        throw new JoynServiceNotAvailableException();
    }

    public void addServiceRegistrationListener(JoynServiceRegistrationListener listener) throws JoynServiceException {
        Logger.d(TAG, "addServiceRegistrationListener() entry " + this.api);
        if (this.api != null) {
            callApiMethod("addServiceRegistrationListener", listener, IJoynServiceRegistrationListener.class);
            return;
        }
        throw new JoynServiceNotAvailableException();
    }

    public void removeServiceRegistrationListener(JoynServiceRegistrationListener listener) throws JoynServiceException {
        Logger.d(TAG, "removeServiceRegistrationListener() entry " + this.api);
        if (this.api != null) {
            callApiMethod("removeServiceRegistrationListener", listener, IJoynServiceRegistrationListener.class);
            return;
        }
        throw new JoynServiceNotAvailableException();
    }

    public static boolean isServiceStarted(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService("activity");
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (serviceList != null) {
            for (int i = 0; i < serviceList.size(); i++) {
                ActivityManager.RunningServiceInfo serviceInfo = serviceList.get(i);
                ComponentName serviceName = serviceInfo.service;
                if (serviceName.getClassName().equals("com.orangelabs.rcs.service.RcsCoreService")) {
                    return serviceInfo.pid != 0;
                }
            }
        }
        return false;
    }
}

package android.bluetooth;

import android.bluetooth.IBluetoothDun;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ProxyInfo;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import java.util.HashSet;
import java.util.Set;

public final class BluetoothDun {
    private static final boolean DBG = true;
    public static final String EXTRA_PREVIOUS_STATE = "android.bluetooth.profile.extra.PREVIOUS_STATE";
    public static final String EXTRA_STATE = "android.bluetooth.profile.extra.STATE";
    public static final String STATE_CHANGED_ACTION = "android.bluetooth.dun.intent.DUN_STATE";
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_DISCONNECTING = 3;
    private static final String TAG = "BluetoothDun";
    private static final boolean VDBG = true;
    private BluetoothAdapter mAdapter;
    private boolean mBindDunService;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback;
    private ServiceConnection mConnection;
    private Context mContext;
    private IBluetoothDun mService;
    private ServiceListener mServiceListener;

    public interface ServiceListener {
        void onServiceConnected(BluetoothDun bluetoothDun);

        void onServiceDisconnected();
    }

    public BluetoothDun(Context context, ServiceListener l) {
        this.mService = null;
        this.mBindDunService = false;
        this.mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
            @Override
            public void onBluetoothStateChange(boolean up) {
                Log.d(BluetoothDun.TAG, "onBluetoothStateChange: up=" + up);
                synchronized (BluetoothDun.this.mConnection) {
                    if (!BluetoothDun.this.mBindDunService) {
                        Log.d(BluetoothDun.TAG, "DUN is not enabled in project configuration. Ignore BT state change.");
                        return;
                    }
                    if (!up) {
                        Log.d(BluetoothDun.TAG, "Unbinding service...");
                        try {
                            BluetoothDun.this.mService = null;
                            if (BluetoothDun.this.mContext == null) {
                                Log.w(BluetoothDun.TAG, "Context is null");
                            } else {
                                BluetoothDun.this.mContext.unbindService(BluetoothDun.this.mConnection);
                            }
                        } catch (Exception re) {
                            Log.e(BluetoothDun.TAG, re.toString());
                        }
                    }
                    try {
                        if (BluetoothDun.this.mService == null) {
                            Log.d(BluetoothDun.TAG, "Binding service...");
                            BluetoothDun.this.doBind();
                        }
                    } catch (Exception re2) {
                        Log.e(BluetoothDun.TAG, re2.toString());
                    }
                }
            }
        };
        this.mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                synchronized (BluetoothDun.this.mConnection) {
                    Log.d(BluetoothDun.TAG, "Proxy object connected");
                    BluetoothDun.this.mService = IBluetoothDun.Stub.asInterface(service);
                    if (BluetoothDun.this.mServiceListener != null) {
                        BluetoothDun.this.mServiceListener.onServiceConnected(BluetoothDun.this);
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                synchronized (BluetoothDun.this.mConnection) {
                    Log.d(BluetoothDun.TAG, "Proxy object disconnected");
                    BluetoothDun.this.mService = null;
                    if (BluetoothDun.this.mServiceListener != null) {
                        BluetoothDun.this.mServiceListener.onServiceDisconnected();
                    }
                }
            }
        };
        this.mContext = context;
        this.mServiceListener = l;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        String dunEnabled = SystemProperties.get("bt.profiles.dun.enabled");
        synchronized (this.mConnection) {
            if (dunEnabled.isEmpty() || Integer.parseInt(dunEnabled) == 0) {
                Log.w(TAG, "bt.profiles.dun.enabled is empty or 0");
                this.mBindDunService = false;
            } else {
                this.mBindDunService = doBind();
            }
        }
    }

    public BluetoothDun(Context context) {
        this.mService = null;
        this.mBindDunService = false;
        this.mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
            @Override
            public void onBluetoothStateChange(boolean up) {
                Log.d(BluetoothDun.TAG, "onBluetoothStateChange: up=" + up);
                synchronized (BluetoothDun.this.mConnection) {
                    if (!BluetoothDun.this.mBindDunService) {
                        Log.d(BluetoothDun.TAG, "DUN is not enabled in project configuration. Ignore BT state change.");
                        return;
                    }
                    if (!up) {
                        Log.d(BluetoothDun.TAG, "Unbinding service...");
                        try {
                            BluetoothDun.this.mService = null;
                            if (BluetoothDun.this.mContext == null) {
                                Log.w(BluetoothDun.TAG, "Context is null");
                            } else {
                                BluetoothDun.this.mContext.unbindService(BluetoothDun.this.mConnection);
                            }
                        } catch (Exception re) {
                            Log.e(BluetoothDun.TAG, re.toString());
                        }
                    }
                    try {
                        if (BluetoothDun.this.mService == null) {
                            Log.d(BluetoothDun.TAG, "Binding service...");
                            BluetoothDun.this.doBind();
                        }
                    } catch (Exception re2) {
                        Log.e(BluetoothDun.TAG, re2.toString());
                    }
                }
            }
        };
        this.mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                synchronized (BluetoothDun.this.mConnection) {
                    Log.d(BluetoothDun.TAG, "Proxy object connected");
                    BluetoothDun.this.mService = IBluetoothDun.Stub.asInterface(service);
                    if (BluetoothDun.this.mServiceListener != null) {
                        BluetoothDun.this.mServiceListener.onServiceConnected(BluetoothDun.this);
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                synchronized (BluetoothDun.this.mConnection) {
                    Log.d(BluetoothDun.TAG, "Proxy object disconnected");
                    BluetoothDun.this.mService = null;
                    if (BluetoothDun.this.mServiceListener != null) {
                        BluetoothDun.this.mServiceListener.onServiceDisconnected();
                    }
                }
            }
        };
        this.mContext = context;
        this.mServiceListener = null;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        String dunEnabled = SystemProperties.get("bt.profiles.dun.enabled");
        synchronized (this.mConnection) {
            if (dunEnabled.isEmpty() || Integer.parseInt(dunEnabled) == 0) {
                Log.w(TAG, "bt.profiles.dun.enabled is empty or 0");
                this.mBindDunService = false;
            } else {
                this.mBindDunService = doBind();
            }
        }
    }

    boolean doBind() {
        if (this.mContext == null) {
            Log.e(TAG, "Context is null");
            return false;
        }
        Intent intent = new Intent(IBluetoothDun.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle())) {
            Log.e(TAG, "Could not bind to Bluetooth DUN Service with " + intent);
            return false;
        }
        return true;
    }

    protected void finalize() {
        close();
    }

    public synchronized void close() {
        Log.d(TAG, "close()");
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
            synchronized (this.mConnection) {
                if (this.mBindDunService) {
                    if (this.mService != null) {
                        try {
                            this.mService = null;
                            if (this.mContext == null) {
                                Log.w(TAG, "Context is null");
                            } else {
                                this.mContext.unbindService(this.mConnection);
                            }
                        } catch (Exception re) {
                            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, re);
                        }
                    }
                    this.mBindDunService = false;
                }
                this.mContext = null;
                this.mServiceListener = null;
            }
        }
    }

    public Set<BluetoothDevice> getConnectedDevices() {
        Log.d(TAG, "getConnectedDevices()");
        HashSet<BluetoothDevice> devices = new HashSet<>();
        if (this.mService != null) {
            BluetoothDevice connDev = null;
            try {
                connDev = this.mService.dunGetConnectedDevice();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
            if (connDev != null) {
                devices.add(connDev);
            }
        } else {
            Log.w(TAG, "getConnectedDevices error: not attached to DUN service");
        }
        return devices;
    }

    public boolean connect(BluetoothDevice device) {
        return false;
    }

    public boolean disconnect(BluetoothDevice device) {
        if (this.mService != null) {
            try {
                this.mService.dunDisconnect();
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "disconnect error: not attached to DUN service");
        return false;
    }

    public int getState(BluetoothDevice device) {
        if (this.mService != null) {
            try {
                Set<BluetoothDevice> remoteDevices = getConnectedDevices();
                if (device == null || remoteDevices == null || !remoteDevices.contains(device)) {
                    return 0;
                }
                return this.mService.dunGetState();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "get state error: not attached to DUN service");
        }
        return 0;
    }

    public void setBluetoothTethering(boolean value) {
        Log.d(TAG, "setBluetoothTethering(" + value + ")");
        if (this.mService == null) {
            Log.d(TAG, "Service is not ready");
            return;
        }
        try {
            this.mService.setBluetoothTethering(value);
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
        }
    }

    public boolean isTetheringOn() {
        if (this.mService == null) {
            Log.d(TAG, "Service is not ready");
            return false;
        }
        try {
            return this.mService.isTetheringOn();
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
            return false;
        }
    }
}

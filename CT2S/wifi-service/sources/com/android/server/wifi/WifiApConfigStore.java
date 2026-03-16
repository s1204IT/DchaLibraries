package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

class WifiApConfigStore extends StateMachine {
    private static final String AP_CONFIG_FILE = Environment.getDataDirectory() + "/misc/wifi/softap.conf";
    private static final int AP_CONFIG_FILE_VERSION = 1;
    private static final String TAG = "WifiApConfigStore";
    private State mActiveState;
    private Context mContext;
    private State mDefaultState;
    private State mInactiveState;
    private AsyncChannel mReplyChannel;
    private WifiConfiguration mWifiApConfig;

    WifiApConfigStore(Context context, Handler target) {
        super(TAG, target.getLooper());
        this.mDefaultState = new DefaultState();
        this.mInactiveState = new InactiveState();
        this.mActiveState = new ActiveState();
        this.mWifiApConfig = null;
        this.mReplyChannel = new AsyncChannel();
        this.mContext = context;
        addState(this.mDefaultState);
        addState(this.mInactiveState, this.mDefaultState);
        addState(this.mActiveState, this.mDefaultState);
        setInitialState(this.mInactiveState);
    }

    public static WifiApConfigStore makeWifiApConfigStore(Context context, Handler target) {
        WifiApConfigStore s = new WifiApConfigStore(context, target);
        s.start();
        return s;
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case 131097:
                case 131098:
                    Log.e(WifiApConfigStore.TAG, "Unexpected message: " + message);
                    break;
                case 131099:
                    WifiApConfigStore.this.mReplyChannel.replyToMessage(message, 131100, WifiApConfigStore.this.mWifiApConfig);
                    break;
                default:
                    Log.e(WifiApConfigStore.TAG, "Failed to handle " + message);
                    break;
            }
            return true;
        }
    }

    class InactiveState extends State {
        InactiveState() {
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case 131097:
                    WifiConfiguration config = (WifiConfiguration) message.obj;
                    if (config.SSID != null) {
                        WifiApConfigStore.this.mWifiApConfig = (WifiConfiguration) message.obj;
                        WifiApConfigStore.this.transitionTo(WifiApConfigStore.this.mActiveState);
                    } else {
                        Log.e(WifiApConfigStore.TAG, "Try to setup AP config without SSID: " + message);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    class ActiveState extends State {
        ActiveState() {
        }

        public void enter() {
            new Thread(new Runnable() {
                @Override
                public void run() throws Throwable {
                    WifiApConfigStore.this.writeApConfiguration(WifiApConfigStore.this.mWifiApConfig);
                    WifiApConfigStore.this.sendMessage(131098);
                }
            }).start();
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case 131097:
                    WifiApConfigStore.this.deferMessage(message);
                    return true;
                case 131098:
                    WifiApConfigStore.this.transitionTo(WifiApConfigStore.this.mInactiveState);
                    return true;
                default:
                    return false;
            }
        }
    }

    void loadApConfiguration() {
        WifiConfiguration config;
        DataInputStream in;
        DataInputStream in2 = null;
        try {
            try {
                config = new WifiConfiguration();
                in = new DataInputStream(new BufferedInputStream(new FileInputStream(AP_CONFIG_FILE)));
            } catch (IOException e) {
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            int version = in.readInt();
            if (version != 1) {
                Log.e(TAG, "Bad version on hotspot configuration file, set defaults");
                setDefaultApConfiguration();
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e2) {
                    }
                }
                return;
            }
            config.SSID = in.readUTF();
            config.wifiApChannelIndex = in.readInt();
            int authType = in.readInt();
            config.allowedKeyManagement.set(authType);
            if (authType != 0) {
                config.preSharedKey = in.readUTF();
            } else {
                String IsWEP = in.readUTF();
                if (IsWEP.equals("wep128")) {
                    config.wepKeys[0] = in.readUTF();
                }
            }
            this.mWifiApConfig = config;
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e3) {
                }
            }
        } catch (IOException e4) {
            in2 = in;
            setDefaultApConfiguration();
            if (in2 != null) {
                try {
                    in2.close();
                } catch (IOException e5) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            in2 = in;
            if (in2 != null) {
                try {
                    in2.close();
                } catch (IOException e6) {
                }
            }
            throw th;
        }
    }

    Messenger getMessenger() {
        return new Messenger(getHandler());
    }

    private void writeApConfiguration(WifiConfiguration config) throws Throwable {
        DataOutputStream out;
        DataOutputStream out2 = null;
        try {
            try {
                out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(AP_CONFIG_FILE)));
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            out.writeInt(1);
            out.writeUTF(config.SSID);
            out.writeInt(config.wifiApChannelIndex);
            int authType = config.getAuthType();
            out.writeInt(authType);
            if (authType != 0) {
                out.writeUTF(config.preSharedKey);
            } else if (config.wepKeys[0] != null) {
                out.writeUTF("wep128");
                out.writeUTF(config.wepKeys[0]);
            } else {
                out.writeUTF("open");
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e2) {
                }
            }
        } catch (IOException e3) {
            e = e3;
            out2 = out;
            Log.e(TAG, "Error writing hotspot configuration" + e);
            if (out2 != null) {
                try {
                    out2.close();
                } catch (IOException e4) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            out2 = out;
            if (out2 != null) {
                try {
                    out2.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
    }

    private void setDefaultApConfiguration() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = this.mContext.getString(R.string.imProtocolSkype);
        config.allowedKeyManagement.set(4);
        String randomUUID = UUID.randomUUID().toString();
        config.preSharedKey = randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
        sendMessage(131097, config);
    }
}

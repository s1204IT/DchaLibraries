package android.net.lowpan;

import android.net.lowpan.ILowpanEnergyScanCallback;
import android.net.lowpan.ILowpanNetScanCallback;
import android.net.lowpan.LowpanScanner;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

public class LowpanScanner {
    private static final String TAG = LowpanScanner.class.getSimpleName();
    private ILowpanInterface mBinder;
    private Callback mCallback = null;
    private Handler mHandler = null;
    private ArrayList<Integer> mChannelMask = null;
    private int mTxPower = Integer.MAX_VALUE;

    public static abstract class Callback {
        public void onNetScanBeacon(LowpanBeaconInfo lowpanBeaconInfo) {
        }

        public void onEnergyScanResult(LowpanEnergyScanResult lowpanEnergyScanResult) {
        }

        public void onScanFinished() {
        }
    }

    LowpanScanner(ILowpanInterface iLowpanInterface) {
        this.mBinder = iLowpanInterface;
    }

    public synchronized void setCallback(Callback callback, Handler handler) {
        this.mCallback = callback;
        this.mHandler = handler;
    }

    public void setCallback(Callback callback) {
        setCallback(callback, null);
    }

    public void setChannelMask(Collection<Integer> collection) {
        if (collection == null) {
            this.mChannelMask = null;
            return;
        }
        if (this.mChannelMask == null) {
            this.mChannelMask = new ArrayList<>();
        } else {
            this.mChannelMask.clear();
        }
        this.mChannelMask.addAll(collection);
    }

    public Collection<Integer> getChannelMask() {
        return (Collection) this.mChannelMask.clone();
    }

    public void addChannel(int i) {
        if (this.mChannelMask == null) {
            this.mChannelMask = new ArrayList<>();
        }
        this.mChannelMask.add(Integer.valueOf(i));
    }

    public void setTxPower(int i) {
        this.mTxPower = i;
    }

    public int getTxPower() {
        return this.mTxPower;
    }

    private Map<String, Object> createScanOptionMap() {
        HashMap map = new HashMap();
        if (this.mChannelMask != null) {
            LowpanProperties.KEY_CHANNEL_MASK.putInMap(map, this.mChannelMask.stream().mapToInt(new ToIntFunction() {
                @Override
                public final int applyAsInt(Object obj) {
                    return ((Integer) obj).intValue();
                }
            }).toArray());
        }
        if (this.mTxPower != Integer.MAX_VALUE) {
            LowpanProperties.KEY_MAX_TX_POWER.putInMap(map, Integer.valueOf(this.mTxPower));
        }
        return map;
    }

    public void startNetScan() throws LowpanException {
        try {
            this.mBinder.startNetScan(createScanOptionMap(), new AnonymousClass1());
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        } catch (ServiceSpecificException e2) {
            throw LowpanException.rethrowFromServiceSpecificException(e2);
        }
    }

    class AnonymousClass1 extends ILowpanNetScanCallback.Stub {
        AnonymousClass1() {
        }

        @Override
        public void onNetScanBeacon(final LowpanBeaconInfo lowpanBeaconInfo) {
            final Callback callback;
            Handler handler;
            synchronized (LowpanScanner.this) {
                callback = LowpanScanner.this.mCallback;
                handler = LowpanScanner.this.mHandler;
            }
            if (callback == null) {
                return;
            }
            Runnable runnable = new Runnable() {
                @Override
                public final void run() {
                    callback.onNetScanBeacon(lowpanBeaconInfo);
                }
            };
            if (handler != null) {
                handler.post(runnable);
            } else {
                runnable.run();
            }
        }

        @Override
        public void onNetScanFinished() {
            final Callback callback;
            Handler handler;
            synchronized (LowpanScanner.this) {
                callback = LowpanScanner.this.mCallback;
                handler = LowpanScanner.this.mHandler;
            }
            if (callback == null) {
                return;
            }
            Runnable runnable = new Runnable() {
                @Override
                public final void run() {
                    callback.onScanFinished();
                }
            };
            if (handler != null) {
                handler.post(runnable);
            } else {
                runnable.run();
            }
        }
    }

    public void stopNetScan() {
        try {
            this.mBinder.stopNetScan();
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public void startEnergyScan() throws LowpanException {
        try {
            this.mBinder.startEnergyScan(createScanOptionMap(), new AnonymousClass2());
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        } catch (ServiceSpecificException e2) {
            throw LowpanException.rethrowFromServiceSpecificException(e2);
        }
    }

    class AnonymousClass2 extends ILowpanEnergyScanCallback.Stub {
        AnonymousClass2() {
        }

        @Override
        public void onEnergyScanResult(final int i, final int i2) {
            final Callback callback = LowpanScanner.this.mCallback;
            Handler handler = LowpanScanner.this.mHandler;
            if (callback == null) {
                return;
            }
            Runnable runnable = new Runnable() {
                @Override
                public final void run() {
                    LowpanScanner.AnonymousClass2.lambda$onEnergyScanResult$0(callback, i, i2);
                }
            };
            if (handler != null) {
                handler.post(runnable);
            } else {
                runnable.run();
            }
        }

        static void lambda$onEnergyScanResult$0(Callback callback, int i, int i2) {
            if (callback != null) {
                LowpanEnergyScanResult lowpanEnergyScanResult = new LowpanEnergyScanResult();
                lowpanEnergyScanResult.setChannel(i);
                lowpanEnergyScanResult.setMaxRssi(i2);
                callback.onEnergyScanResult(lowpanEnergyScanResult);
            }
        }

        @Override
        public void onEnergyScanFinished() {
            final Callback callback = LowpanScanner.this.mCallback;
            Handler handler = LowpanScanner.this.mHandler;
            if (callback == null) {
                return;
            }
            Runnable runnable = new Runnable() {
                @Override
                public final void run() {
                    callback.onScanFinished();
                }
            };
            if (handler != null) {
                handler.post(runnable);
            } else {
                runnable.run();
            }
        }
    }

    public void stopEnergyScan() {
        try {
            this.mBinder.stopEnergyScan();
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }
}

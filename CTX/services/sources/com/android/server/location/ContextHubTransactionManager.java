package com.android.server.location;

import android.hardware.contexthub.V1_0.IContexthub;
import android.hardware.location.IContextHubTransactionCallback;
import android.hardware.location.NanoAppBinary;
import android.hardware.location.NanoAppState;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class ContextHubTransactionManager {
    private static final int MAX_PENDING_REQUESTS = 10000;
    private static final String TAG = "ContextHubTransactionManager";
    private final ContextHubClientManager mClientManager;
    private final IContexthub mContextHubProxy;
    private final NanoAppStateManager mNanoAppStateManager;
    private final ArrayDeque<ContextHubServiceTransaction> mTransactionQueue = new ArrayDeque<>();
    private final AtomicInteger mNextAvailableId = new AtomicInteger();
    private final ScheduledThreadPoolExecutor mTimeoutExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> mTimeoutFuture = null;

    ContextHubTransactionManager(IContexthub iContexthub, ContextHubClientManager contextHubClientManager, NanoAppStateManager nanoAppStateManager) {
        this.mContextHubProxy = iContexthub;
        this.mClientManager = contextHubClientManager;
        this.mNanoAppStateManager = nanoAppStateManager;
    }

    ContextHubServiceTransaction createLoadTransaction(final int i, final NanoAppBinary nanoAppBinary, final IContextHubTransactionCallback iContextHubTransactionCallback) {
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 0) {
            @Override
            int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.loadNanoApp(i, ContextHubServiceUtil.createHidlNanoAppBinary(nanoAppBinary), getTransactionId());
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to load nanoapp with ID 0x" + Long.toHexString(nanoAppBinary.getNanoAppId()), e);
                    return 1;
                }
            }

            @Override
            void onTransactionComplete(int i2) {
                if (i2 == 0) {
                    ContextHubTransactionManager.this.mNanoAppStateManager.addNanoAppInstance(i, nanoAppBinary.getNanoAppId(), nanoAppBinary.getNanoAppVersion());
                }
                try {
                    iContextHubTransactionCallback.onTransactionComplete(i2);
                    if (i2 == 0) {
                        ContextHubTransactionManager.this.mClientManager.onNanoAppLoaded(i, nanoAppBinary.getNanoAppId());
                    }
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    ContextHubServiceTransaction createUnloadTransaction(final int i, final long j, final IContextHubTransactionCallback iContextHubTransactionCallback) {
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 1) {
            @Override
            int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.unloadNanoApp(i, j, getTransactionId());
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to unload nanoapp with ID 0x" + Long.toHexString(j), e);
                    return 1;
                }
            }

            @Override
            void onTransactionComplete(int i2) {
                if (i2 == 0) {
                    ContextHubTransactionManager.this.mNanoAppStateManager.removeNanoAppInstance(i, j);
                }
                try {
                    iContextHubTransactionCallback.onTransactionComplete(i2);
                    if (i2 == 0) {
                        ContextHubTransactionManager.this.mClientManager.onNanoAppUnloaded(i, j);
                    }
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    ContextHubServiceTransaction createEnableTransaction(final int i, final long j, final IContextHubTransactionCallback iContextHubTransactionCallback) {
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 2) {
            @Override
            int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.enableNanoApp(i, j, getTransactionId());
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to enable nanoapp with ID 0x" + Long.toHexString(j), e);
                    return 1;
                }
            }

            @Override
            void onTransactionComplete(int i2) {
                try {
                    iContextHubTransactionCallback.onTransactionComplete(i2);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    ContextHubServiceTransaction createDisableTransaction(final int i, final long j, final IContextHubTransactionCallback iContextHubTransactionCallback) {
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 3) {
            @Override
            int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.disableNanoApp(i, j, getTransactionId());
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to disable nanoapp with ID 0x" + Long.toHexString(j), e);
                    return 1;
                }
            }

            @Override
            void onTransactionComplete(int i2) {
                try {
                    iContextHubTransactionCallback.onTransactionComplete(i2);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onTransactionComplete", e);
                }
            }
        };
    }

    ContextHubServiceTransaction createQueryTransaction(final int i, final IContextHubTransactionCallback iContextHubTransactionCallback) {
        return new ContextHubServiceTransaction(this.mNextAvailableId.getAndIncrement(), 4) {
            @Override
            int onTransact() {
                try {
                    return ContextHubTransactionManager.this.mContextHubProxy.queryApps(i);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while trying to query for nanoapps", e);
                    return 1;
                }
            }

            @Override
            void onTransactionComplete(int i2) {
                onQueryResponse(i2, Collections.emptyList());
            }

            @Override
            void onQueryResponse(int i2, List<NanoAppState> list) {
                try {
                    iContextHubTransactionCallback.onQueryResponse(i2, list);
                } catch (RemoteException e) {
                    Log.e(ContextHubTransactionManager.TAG, "RemoteException while calling client onQueryComplete", e);
                }
            }
        };
    }

    synchronized void addTransaction(ContextHubServiceTransaction contextHubServiceTransaction) throws IllegalStateException {
        if (this.mTransactionQueue.size() == 10000) {
            throw new IllegalStateException("Transaction queue is full (capacity = 10000)");
        }
        this.mTransactionQueue.add(contextHubServiceTransaction);
        if (this.mTransactionQueue.size() == 1) {
            startNextTransaction();
        }
    }

    synchronized void onTransactionResponse(int i, int i2) {
        int i3;
        ContextHubServiceTransaction contextHubServiceTransactionPeek = this.mTransactionQueue.peek();
        if (contextHubServiceTransactionPeek == null) {
            Log.w(TAG, "Received unexpected transaction response (no transaction pending)");
            return;
        }
        if (contextHubServiceTransactionPeek.getTransactionId() != i) {
            Log.w(TAG, "Received unexpected transaction response (expected ID = " + contextHubServiceTransactionPeek.getTransactionId() + ", received ID = " + i + ")");
            return;
        }
        if (i2 == 0) {
            i3 = 0;
        } else {
            i3 = 5;
        }
        contextHubServiceTransactionPeek.onTransactionComplete(i3);
        removeTransactionAndStartNext();
    }

    synchronized void onQueryResponse(List<NanoAppState> list) {
        ContextHubServiceTransaction contextHubServiceTransactionPeek = this.mTransactionQueue.peek();
        if (contextHubServiceTransactionPeek == null) {
            Log.w(TAG, "Received unexpected query response (no transaction pending)");
            return;
        }
        if (contextHubServiceTransactionPeek.getTransactionType() != 4) {
            Log.w(TAG, "Received unexpected query response (expected " + contextHubServiceTransactionPeek + ")");
            return;
        }
        contextHubServiceTransactionPeek.onQueryResponse(0, list);
        removeTransactionAndStartNext();
    }

    synchronized void onHubReset() {
        if (this.mTransactionQueue.peek() == null) {
            return;
        }
        removeTransactionAndStartNext();
    }

    private void removeTransactionAndStartNext() {
        this.mTimeoutFuture.cancel(false);
        this.mTransactionQueue.remove().setComplete();
        if (!this.mTransactionQueue.isEmpty()) {
            startNextTransaction();
        }
    }

    private void startNextTransaction() {
        int i = 1;
        while (i != 0 && !this.mTransactionQueue.isEmpty()) {
            final ContextHubServiceTransaction contextHubServiceTransactionPeek = this.mTransactionQueue.peek();
            int iOnTransact = contextHubServiceTransactionPeek.onTransact();
            if (iOnTransact == 0) {
                this.mTimeoutFuture = this.mTimeoutExecutor.schedule(new Runnable() {
                    @Override
                    public final void run() {
                        ContextHubTransactionManager.lambda$startNextTransaction$0(this.f$0, contextHubServiceTransactionPeek);
                    }
                }, contextHubServiceTransactionPeek.getTimeout(TimeUnit.SECONDS), TimeUnit.SECONDS);
            } else {
                contextHubServiceTransactionPeek.onTransactionComplete(ContextHubServiceUtil.toTransactionResult(iOnTransact));
                this.mTransactionQueue.remove();
            }
            i = iOnTransact;
        }
    }

    public static void lambda$startNextTransaction$0(ContextHubTransactionManager contextHubTransactionManager, ContextHubServiceTransaction contextHubServiceTransaction) {
        synchronized (contextHubTransactionManager) {
            if (!contextHubServiceTransaction.isComplete()) {
                Log.d(TAG, contextHubServiceTransaction + " timed out");
                contextHubServiceTransaction.onTransactionComplete(6);
                contextHubTransactionManager.removeTransactionAndStartNext();
            }
        }
    }
}

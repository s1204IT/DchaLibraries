package com.android.server.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.autofill.IAutofillFieldClassificationService;
import android.util.Log;
import android.util.Slog;
import android.view.autofill.AutofillValue;
import com.android.internal.annotations.GuardedBy;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class FieldClassificationStrategy {
    private static final String TAG = "FieldClassificationStrategy";
    private final Context mContext;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private ArrayList<Command> mQueuedCommands;

    @GuardedBy("mLock")
    private IAutofillFieldClassificationService mRemoteService;

    @GuardedBy("mLock")
    private ServiceConnection mServiceConnection;
    private final int mUserId;

    private interface Command {
        void run(IAutofillFieldClassificationService iAutofillFieldClassificationService) throws RemoteException;
    }

    private interface MetadataParser<T> {
        T get(Resources resources, int i);
    }

    public FieldClassificationStrategy(Context context, int i) {
        this.mContext = context;
        this.mUserId = i;
    }

    ServiceInfo getServiceInfo() {
        String servicesSystemSharedLibraryPackageName = this.mContext.getPackageManager().getServicesSystemSharedLibraryPackageName();
        if (servicesSystemSharedLibraryPackageName == null) {
            Slog.w(TAG, "no external services package!");
            return null;
        }
        Intent intent = new Intent("android.service.autofill.AutofillFieldClassificationService");
        intent.setPackage(servicesSystemSharedLibraryPackageName);
        ResolveInfo resolveInfoResolveService = this.mContext.getPackageManager().resolveService(intent, 132);
        if (resolveInfoResolveService == null || resolveInfoResolveService.serviceInfo == null) {
            Slog.w(TAG, "No valid components found.");
            return null;
        }
        return resolveInfoResolveService.serviceInfo;
    }

    private ComponentName getServiceComponentName() {
        ServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo == null) {
            return null;
        }
        ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        if (!"android.permission.BIND_AUTOFILL_FIELD_CLASSIFICATION_SERVICE".equals(serviceInfo.permission)) {
            Slog.w(TAG, componentName.flattenToShortString() + " does not require permission android.permission.BIND_AUTOFILL_FIELD_CLASSIFICATION_SERVICE");
            return null;
        }
        if (Helper.sVerbose) {
            Slog.v(TAG, "getServiceComponentName(): " + componentName);
        }
        return componentName;
    }

    void reset() {
        synchronized (this.mLock) {
            if (this.mServiceConnection != null) {
                if (Helper.sDebug) {
                    Slog.d(TAG, "reset(): unbinding service.");
                }
                this.mContext.unbindService(this.mServiceConnection);
                this.mServiceConnection = null;
            } else if (Helper.sDebug) {
                Slog.d(TAG, "reset(): service is not bound. Do nothing.");
            }
        }
    }

    private void connectAndRun(Command command) {
        synchronized (this.mLock) {
            if (this.mRemoteService != null) {
                try {
                    if (Helper.sVerbose) {
                        Slog.v(TAG, "running command right away");
                    }
                    command.run(this.mRemoteService);
                } catch (RemoteException e) {
                    Slog.w(TAG, "exception calling service: " + e);
                }
                return;
            }
            if (Helper.sDebug) {
                Slog.d(TAG, "service is null; queuing command");
            }
            if (this.mQueuedCommands == null) {
                this.mQueuedCommands = new ArrayList<>(1);
            }
            this.mQueuedCommands.add(command);
            if (this.mServiceConnection != null) {
                return;
            }
            if (Helper.sVerbose) {
                Slog.v(TAG, "creating connection");
            }
            this.mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    if (Helper.sVerbose) {
                        Slog.v(FieldClassificationStrategy.TAG, "onServiceConnected(): " + componentName);
                    }
                    synchronized (FieldClassificationStrategy.this.mLock) {
                        FieldClassificationStrategy.this.mRemoteService = IAutofillFieldClassificationService.Stub.asInterface(iBinder);
                        if (FieldClassificationStrategy.this.mQueuedCommands != null) {
                            int size = FieldClassificationStrategy.this.mQueuedCommands.size();
                            if (Helper.sDebug) {
                                Slog.d(FieldClassificationStrategy.TAG, "running " + size + " queued commands");
                            }
                            for (int i = 0; i < size; i++) {
                                Command command2 = (Command) FieldClassificationStrategy.this.mQueuedCommands.get(i);
                                try {
                                    if (Helper.sVerbose) {
                                        Slog.v(FieldClassificationStrategy.TAG, "running queued command #" + i);
                                    }
                                    command2.run(FieldClassificationStrategy.this.mRemoteService);
                                } catch (RemoteException e2) {
                                    Slog.w(FieldClassificationStrategy.TAG, "exception calling " + componentName + ": " + e2);
                                }
                            }
                            FieldClassificationStrategy.this.mQueuedCommands = null;
                        } else if (Helper.sDebug) {
                            Slog.d(FieldClassificationStrategy.TAG, "no queued commands");
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    if (Helper.sVerbose) {
                        Slog.v(FieldClassificationStrategy.TAG, "onServiceDisconnected(): " + componentName);
                    }
                    synchronized (FieldClassificationStrategy.this.mLock) {
                        FieldClassificationStrategy.this.mRemoteService = null;
                    }
                }

                @Override
                public void onBindingDied(ComponentName componentName) {
                    if (Helper.sVerbose) {
                        Slog.v(FieldClassificationStrategy.TAG, "onBindingDied(): " + componentName);
                    }
                    synchronized (FieldClassificationStrategy.this.mLock) {
                        FieldClassificationStrategy.this.mRemoteService = null;
                    }
                }

                @Override
                public void onNullBinding(ComponentName componentName) {
                    if (Helper.sVerbose) {
                        Slog.v(FieldClassificationStrategy.TAG, "onNullBinding(): " + componentName);
                    }
                    synchronized (FieldClassificationStrategy.this.mLock) {
                        FieldClassificationStrategy.this.mRemoteService = null;
                    }
                }
            };
            ComponentName serviceComponentName = getServiceComponentName();
            if (Helper.sVerbose) {
                Slog.v(TAG, "binding to: " + serviceComponentName);
            }
            if (serviceComponentName != null) {
                Intent intent = new Intent();
                intent.setComponent(serviceComponentName);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mContext.bindServiceAsUser(intent, this.mServiceConnection, 1, UserHandle.of(this.mUserId));
                    if (Helper.sVerbose) {
                        Slog.v(TAG, "bound");
                    }
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
            return;
        }
    }

    String[] getAvailableAlgorithms() {
        return (String[]) getMetadataValue("android.autofill.field_classification.available_algorithms", new MetadataParser() {
            @Override
            public final Object get(Resources resources, int i) {
                return resources.getStringArray(i);
            }
        });
    }

    String getDefaultAlgorithm() {
        return (String) getMetadataValue("android.autofill.field_classification.default_algorithm", new MetadataParser() {
            @Override
            public final Object get(Resources resources, int i) {
                return resources.getString(i);
            }
        });
    }

    private <T> T getMetadataValue(String str, MetadataParser<T> metadataParser) {
        ServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo == null) {
            return null;
        }
        try {
            return metadataParser.get(this.mContext.getPackageManager().getResourcesForApplication(serviceInfo.applicationInfo), serviceInfo.metaData.getInt(str));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting application resources for " + serviceInfo, e);
            return null;
        }
    }

    void getScores(final RemoteCallback remoteCallback, final String str, final Bundle bundle, final List<AutofillValue> list, final String[] strArr) {
        connectAndRun(new Command() {
            @Override
            public final void run(IAutofillFieldClassificationService iAutofillFieldClassificationService) {
                iAutofillFieldClassificationService.getScores(remoteCallback, str, bundle, list, strArr);
            }
        });
    }

    void dump(String str, PrintWriter printWriter) {
        ComponentName serviceComponentName = getServiceComponentName();
        printWriter.print(str);
        printWriter.print("User ID: ");
        printWriter.println(this.mUserId);
        printWriter.print(str);
        printWriter.print("Queued commands: ");
        if (this.mQueuedCommands == null) {
            printWriter.println("N/A");
        } else {
            printWriter.println(this.mQueuedCommands.size());
        }
        printWriter.print(str);
        printWriter.print("Implementation: ");
        if (serviceComponentName == null) {
            printWriter.println("N/A");
            return;
        }
        printWriter.println(serviceComponentName.flattenToShortString());
        printWriter.print(str);
        printWriter.print("Available algorithms: ");
        printWriter.println(Arrays.toString(getAvailableAlgorithms()));
        printWriter.print(str);
        printWriter.print("Default algorithm: ");
        printWriter.println(getDefaultAlgorithm());
    }
}

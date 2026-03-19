package com.android.bluetooth.map;

import android.app.AlertDialog;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.UserManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.bluetooth.map.BluetoothMapbMessage;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.bluetooth.opp.BluetoothShare;
import com.android.bluetooth.sap.SapService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ServerRequestHandler;

public class BluetoothMapObexServer extends ServerRequestHandler {
    private static final int MAS_INSTANCE_INFORMATION_LENGTH = 200;
    private static final long PROVIDER_ANR_TIMEOUT = 20000;
    private static final String TAG = "BluetoothMapObexServer";
    private static final int THREADED_MAIL_HEADER_ID = 250;
    private static final long THREAD_MAIL_KEY = 1397510985;
    private static final String TYPE_GET_CONVO_LISTING = "x-bt/MAP-convo-listing";
    private static final String TYPE_GET_FOLDER_LISTING = "x-obex/folder-listing";
    private static final String TYPE_GET_MAS_INSTANCE_INFORMATION = "x-bt/MASInstanceInformation";
    private static final String TYPE_GET_MESSAGE_LISTING = "x-bt/MAP-msg-listing";
    private static final String TYPE_MESSAGE = "x-bt/message";
    private static final String TYPE_MESSAGE_UPDATE = "x-bt/MAP-messageUpdate";
    private static final String TYPE_SET_MESSAGE_STATUS = "x-bt/messageStatus";
    private static final String TYPE_SET_NOTIFICATION_FILTER = "x-bt/MAP-notification-filter";
    private static final String TYPE_SET_NOTIFICATION_REGISTRATION = "x-bt/MAP-NotificationRegistration";
    private static final String TYPE_SET_OWNER_STATUS = "x-bt/participant";
    private static final int UUID_LENGTH = 16;
    private static final boolean V = false;
    private BluetoothMapAccountItem mAccount;
    private long mAccountId;
    private String mAuthority;
    private String mBaseUriString;
    private Handler mCallback;
    private Context mContext;
    private BluetoothMapFolderElement mCurrentFolder;
    private Uri mEmailFolderUri;
    private boolean mEnableSmsMms;
    private int mMasId;
    private BluetoothMapMasInstance mMasInstance;
    private BluetoothMapContentObserver mObserver;
    BluetoothMapContent mOutContent;
    private ContentProviderClient mProviderClient;
    private int mRemoteFeatureMask;
    private ContentResolver mResolver;
    private BluetoothMapSimManager mSimManager;
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final byte[] MAP_TARGET = {-69, 88, 43, 64, 66, 12, 17, -37, -80, -34, 8, 0, 32, 12, -102, 102};
    public static final ParcelUuid MAP = ParcelUuid.fromString("00001134-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid MNS = ParcelUuid.fromString("00001133-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid MAS = ParcelUuid.fromString("00001132-0000-1000-8000-00805F9B34FB");
    private boolean mIsAborted = false;
    private boolean mThreadIdSupport = false;
    private String mMessageVersion = "1.0";

    public BluetoothMapObexServer(Handler handler, Context context, BluetoothMapContentObserver bluetoothMapContentObserver, BluetoothMapMasInstance bluetoothMapMasInstance, BluetoothMapAccountItem bluetoothMapAccountItem, boolean z) throws RemoteException {
        this.mObserver = null;
        this.mCallback = null;
        this.mBaseUriString = null;
        this.mAccountId = 0L;
        this.mAccount = null;
        this.mEmailFolderUri = null;
        this.mMasId = 0;
        this.mRemoteFeatureMask = 31;
        this.mEnableSmsMms = false;
        this.mProviderClient = null;
        this.mCallback = handler;
        this.mContext = context;
        this.mObserver = bluetoothMapContentObserver;
        this.mEnableSmsMms = z;
        this.mAccount = bluetoothMapAccountItem;
        this.mMasId = bluetoothMapMasInstance.getMasId();
        this.mMasInstance = bluetoothMapMasInstance;
        this.mRemoteFeatureMask = this.mMasInstance.getRemoteFeatureMask();
        if (bluetoothMapAccountItem != null && bluetoothMapAccountItem.getProviderAuthority() != null) {
            this.mAccountId = bluetoothMapAccountItem.getAccountId();
            this.mAuthority = bluetoothMapAccountItem.getProviderAuthority();
            this.mResolver = this.mContext.getContentResolver();
            if (D) {
                Log.d(TAG, "BluetoothMapObexServer(): accountId=" + this.mAccountId);
            }
            this.mBaseUriString = bluetoothMapAccountItem.mBase_uri + "/";
            if (D) {
                Log.d(TAG, "BluetoothMapObexServer(): baseUri=" + this.mBaseUriString);
            }
            if (bluetoothMapAccountItem.getType() == BluetoothMapUtils.TYPE.EMAIL) {
                this.mEmailFolderUri = BluetoothMapContract.buildFolderUri(this.mAuthority, Long.toString(this.mAccountId));
                if (D) {
                    Log.d(TAG, "BluetoothMapObexServer(): mEmailFolderUri=" + this.mEmailFolderUri);
                }
            }
            this.mProviderClient = acquireUnstableContentProviderOrThrow();
        }
        buildFolderStructure();
        this.mObserver.setFolderStructure(this.mCurrentFolder.getRoot());
        this.mOutContent = new BluetoothMapContent(this.mContext, this.mAccount, this.mMasInstance);
    }

    private ContentProviderClient acquireUnstableContentProviderOrThrow() throws RemoteException {
        ContentProviderClient contentProviderClientAcquireUnstableContentProviderClient = this.mResolver.acquireUnstableContentProviderClient(this.mAuthority);
        if (contentProviderClientAcquireUnstableContentProviderClient == null) {
            throw new RemoteException("Failed to acquire provider for " + this.mAuthority);
        }
        contentProviderClientAcquireUnstableContentProviderClient.setDetectNotResponding(PROVIDER_ANR_TIMEOUT);
        return contentProviderClientAcquireUnstableContentProviderClient;
    }

    private void buildFolderStructure() throws RemoteException {
        boolean z;
        this.mCurrentFolder = new BluetoothMapFolderElement("root", null);
        this.mCurrentFolder.setHasSmsMmsContent(this.mEnableSmsMms);
        boolean z2 = false;
        if (this.mAccount != null) {
            z = this.mAccount.getType() == BluetoothMapUtils.TYPE.IM;
            if (this.mAccount.getType() == BluetoothMapUtils.TYPE.EMAIL) {
                z2 = true;
            }
        } else {
            z = false;
        }
        this.mCurrentFolder.setHasImContent(z);
        this.mCurrentFolder.setHasEmailContent(z2);
        BluetoothMapFolderElement bluetoothMapFolderElementAddFolder = this.mCurrentFolder.addFolder("telecom");
        bluetoothMapFolderElementAddFolder.setHasSmsMmsContent(this.mEnableSmsMms);
        bluetoothMapFolderElementAddFolder.setHasImContent(z);
        bluetoothMapFolderElementAddFolder.setHasEmailContent(z2);
        BluetoothMapFolderElement bluetoothMapFolderElementAddFolder2 = bluetoothMapFolderElementAddFolder.addFolder(NotificationCompat.CATEGORY_MESSAGE);
        bluetoothMapFolderElementAddFolder2.setHasSmsMmsContent(this.mEnableSmsMms);
        bluetoothMapFolderElementAddFolder2.setHasImContent(z);
        bluetoothMapFolderElementAddFolder2.setHasEmailContent(z2);
        addBaseFolders(bluetoothMapFolderElementAddFolder2);
        if (this.mEnableSmsMms) {
            addSmsMmsFolders(bluetoothMapFolderElementAddFolder2);
        }
        if (z2) {
            if (D) {
                Log.d(TAG, "buildFolderStructure(): " + this.mEmailFolderUri.toString());
            }
            addEmailFolders(bluetoothMapFolderElementAddFolder2);
        }
        if (z) {
            addImFolders(bluetoothMapFolderElementAddFolder2);
        }
    }

    private void addBaseFolders(BluetoothMapFolderElement bluetoothMapFolderElement) {
        bluetoothMapFolderElement.addFolder(BluetoothMapContract.FOLDER_NAME_INBOX);
        bluetoothMapFolderElement.addFolder(BluetoothMapContract.FOLDER_NAME_OUTBOX);
        bluetoothMapFolderElement.addFolder(BluetoothMapContract.FOLDER_NAME_SENT);
        bluetoothMapFolderElement.addFolder(BluetoothMapContract.FOLDER_NAME_DELETED);
    }

    private void addSmsMmsFolders(BluetoothMapFolderElement bluetoothMapFolderElement) {
        bluetoothMapFolderElement.addSmsMmsFolder(BluetoothMapContract.FOLDER_NAME_INBOX);
        bluetoothMapFolderElement.addSmsMmsFolder(BluetoothMapContract.FOLDER_NAME_OUTBOX);
        bluetoothMapFolderElement.addSmsMmsFolder(BluetoothMapContract.FOLDER_NAME_SENT);
        bluetoothMapFolderElement.addSmsMmsFolder(BluetoothMapContract.FOLDER_NAME_DELETED);
        bluetoothMapFolderElement.addSmsMmsFolder(BluetoothMapContract.FOLDER_NAME_DRAFT);
    }

    private void addImFolders(BluetoothMapFolderElement bluetoothMapFolderElement) throws RemoteException {
        bluetoothMapFolderElement.addImFolder(BluetoothMapContract.FOLDER_NAME_INBOX, 1L);
        bluetoothMapFolderElement.addImFolder(BluetoothMapContract.FOLDER_NAME_OUTBOX, 4L);
        bluetoothMapFolderElement.addImFolder(BluetoothMapContract.FOLDER_NAME_SENT, 2L);
        bluetoothMapFolderElement.addImFolder(BluetoothMapContract.FOLDER_NAME_DELETED, 5L);
        bluetoothMapFolderElement.addImFolder(BluetoothMapContract.FOLDER_NAME_DRAFT, 3L);
    }

    private void addEmailFolders(BluetoothMapFolderElement bluetoothMapFolderElement) throws RemoteException {
        Cursor cursorQuery = this.mProviderClient.query(this.mEmailFolderUri, BluetoothMapContract.BT_FOLDER_PROJECTION, "parent_id = " + bluetoothMapFolderElement.getFolderId(), null, null);
        try {
            if (cursorQuery != null) {
                cursorQuery.moveToPosition(-1);
                while (cursorQuery.moveToNext()) {
                    addEmailFolders(bluetoothMapFolderElement.addEmailFolder(cursorQuery.getString(cursorQuery.getColumnIndex("name")), cursorQuery.getLong(cursorQuery.getColumnIndex("_id"))));
                }
            } else if (D) {
                Log.d(TAG, "addEmailFolders(): no elements found");
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th) {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            throw th;
        }
    }

    public boolean isSrmSupported() {
        return true;
    }

    public int getRemoteFeatureMask() {
        return this.mRemoteFeatureMask;
    }

    public void setRemoteFeatureMask(int i) {
        if (D) {
            Log.d(TAG, "setRemoteFeatureMask() " + Integer.toHexString(i));
        }
        this.mRemoteFeatureMask = i;
        this.mOutContent.setRemoteFeatureMask(i);
    }

    public int onConnect(HeaderSet headerSet, HeaderSet headerSet2) {
        if (D) {
            Log.d(TAG, "onConnect():");
        }
        this.mSimManager = new BluetoothMapSimManager();
        this.mSimManager.init(this.mContext);
        this.mThreadIdSupport = false;
        this.mMessageVersion = "1.0";
        notifyUpdateWakeLock();
        try {
            byte[] bArr = (byte[]) headerSet.getHeader(70);
            Long l = (Long) headerSet.getHeader(THREADED_MAIL_HEADER_ID);
            if (bArr == null) {
                return 198;
            }
            if (D) {
                Log.d(TAG, "onConnect(): uuid=" + Arrays.toString(bArr));
            }
            if (bArr.length != 16) {
                Log.w(TAG, "Wrong UUID length");
                return 198;
            }
            for (int i = 0; i < 16; i++) {
                if (bArr[i] != MAP_TARGET[i]) {
                    Log.w(TAG, "Wrong UUID");
                    return 198;
                }
            }
            headerSet2.setHeader(74, bArr);
            try {
                byte[] bArr2 = (byte[]) headerSet.getHeader(74);
                if (bArr2 != null) {
                    if (D) {
                        Log.d(TAG, "onConnect(): remote=" + Arrays.toString(bArr2));
                    }
                    headerSet2.setHeader(70, bArr2);
                }
                if (l != null && l.longValue() == THREAD_MAIL_KEY) {
                    this.mThreadIdSupport = true;
                    headerSet2.setHeader(THREADED_MAIL_HEADER_ID, Long.valueOf(THREAD_MAIL_KEY));
                }
                if ((this.mRemoteFeatureMask & 512) == 512) {
                    this.mThreadIdSupport = true;
                }
                if ((this.mRemoteFeatureMask & 256) == 256) {
                    this.mMessageVersion = "1.1";
                }
                if (this.mCallback != null) {
                    Message messageObtain = Message.obtain(this.mCallback);
                    messageObtain.what = SapService.MSG_SESSION_ESTABLISHED;
                    messageObtain.sendToTarget();
                    return 160;
                }
                return 160;
            } catch (IOException e) {
                Log.e(TAG, "Exception during onConnect:", e);
                this.mThreadIdSupport = false;
                return 208;
            }
        } catch (IOException e2) {
            Log.e(TAG, "Exception during onConnect:", e2);
            return 208;
        }
    }

    public void onDisconnect(HeaderSet headerSet, HeaderSet headerSet2) {
        if (D) {
            Log.d(TAG, "onDisconnect(): enter");
        }
        this.mSimManager.unregisterReceiver();
        notifyUpdateWakeLock();
        headerSet2.responseCode = 160;
        if (this.mCallback != null) {
            Message messageObtain = Message.obtain(this.mCallback);
            messageObtain.what = SapService.MSG_SESSION_DISCONNECTED;
            messageObtain.sendToTarget();
        }
    }

    public int onAbort(HeaderSet headerSet, HeaderSet headerSet2) {
        if (D) {
            Log.d(TAG, "onAbort(): enter.");
        }
        notifyUpdateWakeLock();
        this.mIsAborted = true;
        return 160;
    }

    private boolean isUserUnlocked() {
        UserManager userManager = UserManager.get(this.mContext);
        return userManager == null || userManager.isUserUnlocked();
    }

    public int onPut(Operation operation) {
        long singleSubId;
        if (D) {
            Log.d(TAG, "onPut(): enter");
        }
        this.mIsAborted = false;
        notifyUpdateWakeLock();
        BluetoothMapAppParams bluetoothMapAppParams = null;
        try {
            HeaderSet receivedHeader = operation.getReceivedHeader();
            String str = (String) receivedHeader.getHeader(66);
            String str2 = (String) receivedHeader.getHeader(1);
            byte[] bArr = (byte[]) receivedHeader.getHeader(76);
            if (bArr != null) {
                bluetoothMapAppParams = new BluetoothMapAppParams(bArr);
            }
            BluetoothMapAppParams bluetoothMapAppParams2 = bluetoothMapAppParams;
            if (D) {
                Log.d(TAG, "type = " + str + ", name = " + str2);
            }
            if (str.equals(TYPE_MESSAGE_UPDATE)) {
                return updateInbox();
            }
            if (str.equals(TYPE_SET_NOTIFICATION_REGISTRATION)) {
                return this.mObserver.setNotificationRegistration(bluetoothMapAppParams2.getNotificationStatus());
            }
            if (str.equals(TYPE_SET_NOTIFICATION_FILTER)) {
                if (!isUserUnlocked()) {
                    Log.e(TAG, "Storage locked, " + str + " failed");
                    return 211;
                }
                this.mObserver.setNotificationFilter(bluetoothMapAppParams2.getNotificationFilter());
                return 160;
            }
            if (str.equals(TYPE_SET_MESSAGE_STATUS)) {
                if (!isUserUnlocked()) {
                    Log.e(TAG, "Storage locked, " + str + " failed");
                    return 211;
                }
                return setMessageStatus(str2, bluetoothMapAppParams2);
            }
            if (!str.equals(TYPE_MESSAGE)) {
                return str.equals(TYPE_SET_OWNER_STATUS) ? setOwnerStatus(str2, bluetoothMapAppParams2) : BluetoothShare.STATUS_RUNNING;
            }
            if (this.mSimManager.getSubCount() != 0) {
                if (this.mSimManager.getSubCount() == 1) {
                    singleSubId = this.mSimManager.getSingleSubId();
                } else {
                    int defaultSmsSubscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId();
                    Log.d(TAG, "[onPut] Settings messageSubId = " + defaultSmsSubscriptionId);
                    if (defaultSmsSubscriptionId != -1 && defaultSmsSubscriptionId != -2) {
                        if (defaultSmsSubscriptionId == -3) {
                            return pushMessage(operation, str2, bluetoothMapAppParams2, this.mMessageVersion, true);
                        }
                        singleSubId = defaultSmsSubscriptionId;
                    }
                    return pushMessage(operation, str2, bluetoothMapAppParams2, this.mMessageVersion, false);
                }
            } else {
                singleSubId = -1;
            }
            long j = singleSubId;
            Log.d(TAG, "[onPut] pushMessageGemini subId = " + j);
            if (!isUserUnlocked()) {
                Log.e(TAG, "Storage locked, " + str + " failed");
                return 211;
            }
            return pushMessageGemini(operation, str2, bluetoothMapAppParams2, this.mMessageVersion, j);
        } catch (RemoteException e) {
            try {
                this.mProviderClient = acquireUnstableContentProviderOrThrow();
            } catch (RemoteException e2) {
            }
            return BluetoothShare.STATUS_RUNNING;
        } catch (Exception e3) {
            if (D) {
                Log.e(TAG, "Exception occured while handling request", e3);
            } else {
                Log.e(TAG, "Exception occured while handling request");
            }
            if (this.mIsAborted) {
                return 160;
            }
            return BluetoothShare.STATUS_RUNNING;
        }
    }

    private int updateInbox() throws RemoteException {
        BluetoothMapFolderElement folderByName;
        if (this.mAccount == null || (folderByName = this.mCurrentFolder.getFolderByName(BluetoothMapContract.FOLDER_NAME_INBOX)) == null) {
            return 209;
        }
        long j = this.mAccountId;
        if (D) {
            Log.d(TAG, "updateInbox inbox=" + folderByName.getName() + "id=" + folderByName.getFolderId());
        }
        Bundle bundle = new Bundle(2);
        if (j != -1) {
            if (D) {
                Log.d(TAG, "updateInbox accountId=" + j);
            }
            bundle.putLong(BluetoothMapContract.EXTRA_UPDATE_FOLDER_ID, folderByName.getFolderId());
            bundle.putLong(BluetoothMapContract.EXTRA_UPDATE_ACCOUNT_ID, j);
            Uri uri = Uri.parse(this.mBaseUriString);
            if (D) {
                Log.d(TAG, "updateInbox in: " + uri.toString());
            }
            try {
                if (D) {
                    Log.d(TAG, "updateInbox call()...");
                }
                if (this.mProviderClient.call(BluetoothMapContract.METHOD_UPDATE_FOLDER, null, bundle) != null) {
                    return 160;
                }
                if (D) {
                    Log.d(TAG, "updateInbox call failed");
                }
                return 209;
            } catch (RemoteException e) {
                this.mProviderClient = acquireUnstableContentProviderOrThrow();
                return 211;
            } catch (IllegalArgumentException e2) {
                if (D) {
                    Log.e(TAG, "UpdateInbox - if uri is not known", e2);
                }
                return 211;
            } catch (NullPointerException e3) {
                if (D) {
                    Log.e(TAG, "UpdateInbox - if uri or method is null", e3);
                }
                return 211;
            }
        }
        if (D) {
            Log.d(TAG, "updateInbox accountId=0 -> OBEX_HTTP_NOT_IMPLEMENTED");
        }
        return 209;
    }

    private BluetoothMapFolderElement getFolderElementFromName(String str) {
        BluetoothMapFolderElement subFolder;
        BluetoothMapFolderElement bluetoothMapFolderElement = null;
        if (str == null) {
            BluetoothMapFolderElement bluetoothMapFolderElement2 = this.mCurrentFolder;
            if (D) {
            }
        } else {
            try {
                if (str.trim().isEmpty()) {
                    BluetoothMapFolderElement bluetoothMapFolderElement22 = this.mCurrentFolder;
                    try {
                        if (D) {
                            return bluetoothMapFolderElement22;
                        }
                        Log.d(TAG, "no folder name supplied, setting folder to current: " + bluetoothMapFolderElement22.getName());
                        return bluetoothMapFolderElement22;
                    } catch (Exception e) {
                        bluetoothMapFolderElement = bluetoothMapFolderElement22;
                        e = e;
                    }
                } else {
                    BluetoothMapFolderElement subFolder2 = this.mCurrentFolder.getSubFolder(str);
                    if (subFolder2 == null) {
                        try {
                            subFolder = this.mCurrentFolder.getRoot().getSubFolder("telecom").getSubFolder(NotificationCompat.CATEGORY_MESSAGE).getSubFolder(str);
                        } catch (Exception e2) {
                            e = e2;
                            bluetoothMapFolderElement = subFolder2;
                        }
                    } else {
                        subFolder = subFolder2;
                    }
                    if (D) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Folder name: ");
                        sb.append(str);
                        sb.append(" resulted in this element: ");
                        sb.append(subFolder != null ? subFolder.getName() : "null folder");
                        Log.d(TAG, sb.toString());
                    }
                    return subFolder;
                }
            } catch (Exception e3) {
                e = e3;
            }
        }
        Log.e(TAG, "Exception occured getFolderElementFromName", e);
        return bluetoothMapFolderElement;
    }

    private int pushMessage(final Operation operation, String str, final BluetoothMapAppParams bluetoothMapAppParams, String str2, boolean z) {
        long subIdByThread;
        if (bluetoothMapAppParams.getCharset() == -1) {
            if (D) {
                Log.d(TAG, "Missing charset - unable to decode message content. appParams.getCharset() = " + bluetoothMapAppParams.getCharset());
            }
            return 204;
        }
        try {
            BluetoothMapFolderElement folderElementFromName = getFolderElementFromName(str);
            if (folderElementFromName == null) {
                Log.w(TAG, "folderElement == null - sending OBEX_HTTP_PRECON_FAILED");
                return 204;
            }
            final String name = folderElementFromName.getName();
            if (!name.equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_OUTBOX) && !name.equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_DRAFT)) {
                if (D) {
                    Log.d(TAG, "[pushMessage]: Is only allowed to outbox and draft. folderName=" + name);
                    return 198;
                }
                return 198;
            }
            final InputStream inputStreamOpenInputStream = operation.openInputStream();
            final BluetoothMapbMessage bluetoothMapbMessage = BluetoothMapbMessage.parse(inputStreamOpenInputStream, bluetoothMapAppParams.getCharset());
            bluetoothMapbMessage.setVersionString(str2);
            Log.d(TAG, "[pushMessage] BluetoothMapbMessage.parse success");
            if (this.mObserver == null) {
                Log.d(TAG, "[pushMessage] observer == null");
                return 211;
            }
            if (z && bluetoothMapbMessage != null) {
                String singleRecipient = bluetoothMapbMessage.getSingleRecipient();
                if (!TextUtils.isEmpty(singleRecipient)) {
                    long threadIdByNumber = this.mOutContent.getThreadIdByNumber(singleRecipient);
                    if (threadIdByNumber > 0) {
                        subIdByThread = this.mOutContent.getSubIdByThread(threadIdByNumber);
                    } else {
                        subIdByThread = -1;
                    }
                }
            } else {
                subIdByThread = -1;
            }
            if (subIdByThread <= -1) {
                final BluetoothMapContentObserver bluetoothMapContentObserver = this.mObserver;
                this.mCallback.post(new Runnable() {
                    @Override
                    public void run() {
                        BluetoothMapObexServer.this.showSubSelectDialog(operation, name, bluetoothMapAppParams, bluetoothMapbMessage, bluetoothMapContentObserver, inputStreamOpenInputStream);
                    }
                });
                return 160;
            }
            return sendMessage(operation, name, bluetoothMapAppParams, bluetoothMapbMessage, this.mObserver, inputStreamOpenInputStream, subIdByThread);
        } catch (IllegalArgumentException e) {
            if (D) {
                Log.w(TAG, "[pushMessage] Wrongly formatted bMessage received", e);
            }
            return 204;
        } catch (Exception e2) {
            Log.e(TAG, "[pushMessage] Exception occured: ", e2);
            return BluetoothShare.STATUS_RUNNING;
        }
    }

    private int pushMessageGemini(Operation operation, String str, BluetoothMapAppParams bluetoothMapAppParams, String str2, long j) throws Throwable {
        Throwable th;
        InputStream inputStreamOpenInputStream;
        if (bluetoothMapAppParams.getCharset() == -1) {
            if (D) {
                Log.d(TAG, "pushMessage: Missing charset - unable to decode message content. appParams.getCharset() = " + bluetoothMapAppParams.getCharset());
            }
            return 204;
        }
        InputStream inputStream = null;
        try {
            try {
                BluetoothMapFolderElement folderElementFromName = getFolderElementFromName(str);
                if (folderElementFromName == null) {
                    Log.w(TAG, "pushMessage: folderElement == null - sending OBEX_HTTP_PRECON_FAILED");
                    return 204;
                }
                String name = folderElementFromName.getName();
                if (!name.equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_OUTBOX) && !name.equalsIgnoreCase(BluetoothMapContract.FOLDER_NAME_DRAFT)) {
                    if (D) {
                        Log.d(TAG, "pushMessage: Is only allowed to outbox and draft. folderName=" + name);
                    }
                    return 198;
                }
                inputStreamOpenInputStream = operation.openInputStream();
                try {
                    BluetoothMapbMessage bluetoothMapbMessage = BluetoothMapbMessage.parse(inputStreamOpenInputStream, bluetoothMapAppParams.getCharset());
                    bluetoothMapbMessage.setVersionString(str2);
                    if (D) {
                        Log.d(TAG, "pushMessage: charset" + bluetoothMapAppParams.getCharset() + "folderId: " + folderElementFromName.getFolderId() + "Name: " + name + "TYPE: " + bluetoothMapbMessage.getType());
                    }
                    if (bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.SMS_GSM) || bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.SMS_CDMA)) {
                        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
                        if (telephonyManager.getPhoneType() == 1) {
                            bluetoothMapbMessage.setType(BluetoothMapUtils.TYPE.SMS_GSM);
                        } else if (telephonyManager.getPhoneType() == 2) {
                            bluetoothMapbMessage.setType(BluetoothMapUtils.TYPE.SMS_CDMA);
                        }
                        if (D) {
                            Log.d(TAG, "Updated message type: " + bluetoothMapbMessage.getType());
                        }
                    }
                    if (this.mObserver != null && bluetoothMapbMessage != null) {
                        if ((bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.EMAIL) && folderElementFromName.getFolderId() == -1) || ((bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.SMS_GSM) || bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.SMS_CDMA) || bluetoothMapbMessage.getType().equals(BluetoothMapUtils.TYPE.MMS)) && !folderElementFromName.hasSmsMmsContent())) {
                            if (D) {
                                Log.w(TAG, "Wrong message type recieved");
                            }
                            if (inputStreamOpenInputStream != null) {
                                try {
                                    inputStreamOpenInputStream.close();
                                } catch (IOException e) {
                                }
                            }
                            return 198;
                        }
                        long jPushMessage = this.mObserver.pushMessage(bluetoothMapbMessage, folderElementFromName, bluetoothMapAppParams, this.mBaseUriString, j);
                        if (D) {
                            Log.d(TAG, "pushMessage handle: " + jPushMessage);
                        }
                        if (jPushMessage < 0) {
                            if (D) {
                                Log.w(TAG, "Message  handle not created");
                            }
                            if (inputStreamOpenInputStream != null) {
                                try {
                                    inputStreamOpenInputStream.close();
                                } catch (IOException e2) {
                                }
                            }
                            return 211;
                        }
                        HeaderSet headerSet = new HeaderSet();
                        String mapHandle = BluetoothMapUtils.getMapHandle(jPushMessage, bluetoothMapbMessage.getType());
                        if (D) {
                            Log.d(TAG, "handleStr: " + mapHandle + " message.getType(): " + bluetoothMapbMessage.getType());
                        }
                        headerSet.setHeader(1, mapHandle);
                        operation.sendHeaders(headerSet);
                        if (inputStreamOpenInputStream != null) {
                            try {
                                inputStreamOpenInputStream.close();
                            } catch (IOException e3) {
                            }
                        }
                        return 160;
                    }
                    if (D) {
                        Log.w(TAG, "mObserver or parsed message not available");
                    }
                    if (inputStreamOpenInputStream != null) {
                        try {
                            inputStreamOpenInputStream.close();
                        } catch (IOException e4) {
                        }
                    }
                    return 211;
                } catch (RemoteException e5) {
                    inputStream = inputStreamOpenInputStream;
                    try {
                        this.mProviderClient = acquireUnstableContentProviderOrThrow();
                    } catch (RemoteException e6) {
                        if (D) {
                            Log.w(TAG, "acquireUnstableContentProviderOrThrow FAILED");
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e7) {
                        }
                    }
                    return BluetoothShare.STATUS_RUNNING;
                } catch (IOException e8) {
                    e = e8;
                    inputStream = inputStreamOpenInputStream;
                    if (D) {
                        Log.e(TAG, "Exception occured: ", e);
                    }
                    if (!this.mIsAborted) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e9) {
                            }
                        }
                        return BluetoothShare.STATUS_RUNNING;
                    }
                    if (D) {
                        Log.d(TAG, "PushMessage Operation Aborted");
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e10) {
                        }
                    }
                    return 160;
                } catch (IllegalArgumentException e11) {
                    e = e11;
                    inputStream = inputStreamOpenInputStream;
                    if (D) {
                        Log.e(TAG, "Wrongly formatted bMessage received", e);
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e12) {
                        }
                    }
                    return 204;
                } catch (Exception e13) {
                    e = e13;
                    inputStream = inputStreamOpenInputStream;
                    if (D) {
                        Log.e(TAG, "Exception:", e);
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e14) {
                        }
                    }
                    return BluetoothShare.STATUS_RUNNING;
                } catch (Throwable th2) {
                    th = th2;
                    if (inputStreamOpenInputStream == null) {
                        throw th;
                    }
                    try {
                        inputStreamOpenInputStream.close();
                        throw th;
                    } catch (IOException e15) {
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                inputStreamOpenInputStream = inputStream;
            }
        } catch (RemoteException e16) {
        } catch (IOException e17) {
            e = e17;
        } catch (IllegalArgumentException e18) {
            e = e18;
        } catch (Exception e19) {
            e = e19;
        }
    }

    private int setMessageStatus(String str, BluetoothMapAppParams bluetoothMapAppParams) {
        int statusIndicator = bluetoothMapAppParams.getStatusIndicator();
        int statusValue = bluetoothMapAppParams.getStatusValue();
        if (str == null) {
            return 204;
        }
        if (statusIndicator == -1 || statusValue == -1) {
        }
        if (this.mObserver == null) {
            if (D) {
                Log.e(TAG, "Error: no mObserver!");
            }
            return 211;
        }
        try {
            long cpHandle = BluetoothMapUtils.getCpHandle(str);
            BluetoothMapUtils.TYPE msgTypeFromHandle = BluetoothMapUtils.getMsgTypeFromHandle(str);
            if (D) {
                Log.d(TAG, "setMessageStatus. Handle:" + cpHandle + ", MsgType: " + msgTypeFromHandle);
            }
            if (statusIndicator == 1) {
                if (!this.mObserver.setMessageStatusDeleted(cpHandle, msgTypeFromHandle, this.mCurrentFolder, this.mBaseUriString, statusValue)) {
                    if (D) {
                        Log.w(TAG, "setMessageStatusDeleted failed");
                    }
                    return 211;
                }
                return 160;
            }
            if (statusIndicator == 0) {
                try {
                    if (!this.mObserver.setMessageStatusRead(cpHandle, msgTypeFromHandle, this.mBaseUriString, statusValue)) {
                        if (D) {
                            Log.w(TAG, "not able to update the message");
                        }
                        return 211;
                    }
                    return 160;
                } catch (RemoteException e) {
                    if (D) {
                        Log.w(TAG, "Error in setMessageStatusRead()", e);
                    }
                    return 211;
                }
            }
            return 160;
        } catch (NumberFormatException e2) {
            Log.w(TAG, "Wrongly formatted message handle: " + str);
            return 204;
        } catch (IllegalArgumentException e3) {
            Log.w(TAG, "Message type not found in handle string: " + str);
            return 204;
        }
    }

    private int setOwnerStatus(String str, BluetoothMapAppParams bluetoothMapAppParams) throws RemoteException {
        if (this.mAccount == null || this.mAccount.getType() != BluetoothMapUtils.TYPE.IM) {
            return 211;
        }
        Bundle bundle = new Bundle(5);
        int presenceAvailability = bluetoothMapAppParams.getPresenceAvailability();
        String presenceStatus = bluetoothMapAppParams.getPresenceStatus();
        long lastActivity = bluetoothMapAppParams.getLastActivity();
        int chatState = bluetoothMapAppParams.getChatState();
        String chatStateConvoIdString = bluetoothMapAppParams.getChatStateConvoIdString();
        if (presenceAvailability == -1 && presenceStatus == null && lastActivity == -1 && chatState == -1 && chatStateConvoIdString == null) {
            return 204;
        }
        if (presenceAvailability != -1) {
            bundle.putInt(BluetoothMapContract.EXTRA_PRESENCE_STATE, presenceAvailability);
        }
        if (presenceStatus != null) {
            bundle.putString(BluetoothMapContract.EXTRA_PRESENCE_STATUS, presenceStatus);
        }
        if (lastActivity != -1) {
            bundle.putLong(BluetoothMapContract.EXTRA_LAST_ACTIVE, lastActivity);
        }
        if (chatState != -1 && chatStateConvoIdString != null) {
            bundle.putInt(BluetoothMapContract.EXTRA_CHAT_STATE, chatState);
            bundle.putString(BluetoothMapContract.EXTRA_CONVERSATION_ID, chatStateConvoIdString);
        }
        Uri uri = Uri.parse(this.mBaseUriString);
        if (D) {
            Log.d(TAG, "setOwnerStatus in: " + uri.toString());
        }
        try {
            if (D) {
                Log.d(TAG, "setOwnerStatus call()...");
            }
            if (this.mProviderClient.call(BluetoothMapContract.METHOD_SET_OWNER_STATUS, null, bundle) != null) {
                return 160;
            }
            if (D) {
                Log.d(TAG, "setOwnerStatus call failed");
                return 209;
            }
            return 209;
        } catch (RemoteException e) {
            this.mProviderClient = acquireUnstableContentProviderOrThrow();
            return 211;
        } catch (IllegalArgumentException e2) {
            if (D) {
                Log.e(TAG, "setOwnerStatus - if uri is not known", e2);
            }
            return 211;
        } catch (NullPointerException e3) {
            if (D) {
                Log.e(TAG, "setOwnerStatus - if uri or method is null", e3);
            }
            return 211;
        }
    }

    public int onSetPath(HeaderSet headerSet, HeaderSet headerSet2, boolean z, boolean z2) {
        notifyUpdateWakeLock();
        try {
            String str = (String) headerSet.getHeader(1);
            if (D) {
                Log.d(TAG, "onSetPath name is " + str + " backup: " + z + " create: " + z2);
            }
            if (z) {
                if (this.mCurrentFolder.getParent() == null) {
                    return BluetoothShare.STATUS_RUNNING;
                }
                this.mCurrentFolder = this.mCurrentFolder.getParent();
            }
            if (str == null || str.trim().isEmpty()) {
                if (!z) {
                    this.mCurrentFolder = this.mCurrentFolder.getRoot();
                    return 160;
                }
                return 160;
            }
            BluetoothMapFolderElement subFolder = this.mCurrentFolder.getSubFolder(str);
            if (subFolder == null) {
                return BluetoothShare.STATUS_RUNNING;
            }
            this.mCurrentFolder = subFolder;
            return 160;
        } catch (Exception e) {
            if (D) {
                Log.e(TAG, "request headers error", e);
            } else {
                Log.e(TAG, "request headers error");
            }
            return BluetoothShare.STATUS_RUNNING;
        }
    }

    public void onClose() {
        if (this.mCallback != null) {
            Message messageObtain = Message.obtain(this.mCallback);
            messageObtain.what = SapService.MSG_SERVERSESSION_CLOSE;
            messageObtain.arg1 = this.mMasId;
            messageObtain.sendToTarget();
            if (D) {
                Log.d(TAG, "onClose(): msg MSG_SERVERSESSION_CLOSE sent out.");
            }
        }
        if (this.mProviderClient != null) {
            this.mProviderClient.release();
            this.mProviderClient = null;
        }
    }

    public int onGet(Operation operation) {
        BluetoothMapAppParams bluetoothMapAppParams;
        notifyUpdateWakeLock();
        this.mIsAborted = false;
        try {
            HeaderSet receivedHeader = operation.getReceivedHeader();
            String str = (String) receivedHeader.getHeader(66);
            byte[] bArr = (byte[]) receivedHeader.getHeader(76);
            if (bArr != null) {
                bluetoothMapAppParams = new BluetoothMapAppParams(bArr);
            } else {
                bluetoothMapAppParams = null;
            }
            if (D) {
                Log.d(TAG, "OnGet type is " + str);
            }
            if (str == null) {
                return BluetoothShare.STATUS_RUNNING;
            }
            if (str.equals(TYPE_GET_FOLDER_LISTING)) {
                return sendFolderListingRsp(operation, bluetoothMapAppParams);
            }
            if (str.equals(TYPE_GET_MESSAGE_LISTING)) {
                String str2 = (String) receivedHeader.getHeader(1);
                if (!isUserUnlocked()) {
                    Log.e(TAG, "Storage locked, " + str + " failed");
                    return 211;
                }
                return sendMessageListingRsp(operation, bluetoothMapAppParams, str2);
            }
            if (str.equals(TYPE_GET_CONVO_LISTING)) {
                String str3 = (String) receivedHeader.getHeader(1);
                if (!isUserUnlocked()) {
                    Log.e(TAG, "Storage locked, " + str + " failed");
                    return 211;
                }
                return sendConvoListingRsp(operation, bluetoothMapAppParams, str3);
            }
            if (str.equals(TYPE_GET_MAS_INSTANCE_INFORMATION)) {
                return sendMASInstanceInformationRsp(operation, bluetoothMapAppParams);
            }
            if (str.equals(TYPE_MESSAGE)) {
                String str4 = (String) receivedHeader.getHeader(1);
                if (!isUserUnlocked()) {
                    Log.e(TAG, "Storage locked, " + str + " failed");
                    return 211;
                }
                return sendGetMessageRsp(operation, str4, bluetoothMapAppParams, this.mMessageVersion);
            }
            Log.w(TAG, "unknown type request: " + str);
            return 198;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Exception:", e);
            return 204;
        } catch (ParseException e2) {
            Log.e(TAG, "Exception:", e2);
            return 204;
        } catch (Exception e3) {
            if (D) {
                Log.e(TAG, "Exception occured while handling request", e3);
            } else {
                Log.e(TAG, "Exception occured while handling request");
            }
            if (!this.mIsAborted) {
                return BluetoothShare.STATUS_RUNNING;
            }
            if (D) {
                Log.d(TAG, "onGet Operation Aborted");
                return 160;
            }
            return 160;
        }
    }

    private int sendMessageListingRsp(Operation operation, BluetoothMapAppParams bluetoothMapAppParams, String str) throws Throwable {
        BluetoothMapFolderElement root;
        boolean zMsgListingHasUnread;
        byte[] bArrEncode;
        HeaderSet headerSet = new HeaderSet();
        BluetoothMapAppParams bluetoothMapAppParams2 = new BluetoothMapAppParams();
        int i = 0;
        if (bluetoothMapAppParams == null) {
            bluetoothMapAppParams = new BluetoothMapAppParams();
            bluetoothMapAppParams.setMaxListCount(1024);
            bluetoothMapAppParams.setStartOffset(0);
        }
        if (bluetoothMapAppParams.getFilterMsgHandle() == -1 && bluetoothMapAppParams.getFilterConvoId() == null) {
            root = getFolderElementFromName(str);
            if (root == null) {
                Log.w(TAG, "sendMessageListingRsp: folderToList == null-sending OBEX_HTTP_BAD_REQUEST");
                return BluetoothShare.STATUS_RUNNING;
            }
            Log.v(TAG, "sendMessageListingRsp: has sms " + root.hasSmsMmsContent() + ", has email " + root.hasEmailContent() + ", has IM " + root.hasImContent());
        } else {
            Log.v(TAG, "sendMessageListingRsp: ignore folder ");
            root = this.mCurrentFolder.getRoot();
            root.setIngore(true);
        }
        try {
            if (bluetoothMapAppParams.getMaxListCount() == -1) {
                bluetoothMapAppParams.setMaxListCount(1024);
            }
            if (bluetoothMapAppParams.getStartOffset() == -1) {
                bluetoothMapAppParams.setStartOffset(0);
            }
            if (bluetoothMapAppParams.getMaxListCount() != 0) {
                BluetoothMapMessageListing bluetoothMapMessageListingMsgListing = this.mOutContent.msgListing(root, bluetoothMapAppParams);
                bluetoothMapAppParams2.setMessageListingSize(bluetoothMapMessageListingMsgListing.getCount());
                bArrEncode = bluetoothMapMessageListingMsgListing.encode(this.mThreadIdSupport, (this.mRemoteFeatureMask & 512) > 0 ? "1.1" : "1.0");
                zMsgListingHasUnread = bluetoothMapMessageListingMsgListing.hasUnread();
            } else {
                int iMsgListingSize = this.mOutContent.msgListingSize(root, bluetoothMapAppParams);
                zMsgListingHasUnread = this.mOutContent.msgListingHasUnread(root, bluetoothMapAppParams);
                bluetoothMapAppParams2.setMessageListingSize(iMsgListingSize);
                operation.noBodyHeader();
                bArrEncode = null;
            }
            root.setIngore(false);
            if (zMsgListingHasUnread) {
                bluetoothMapAppParams2.setNewMessage(1);
            } else {
                bluetoothMapAppParams2.setNewMessage(0);
            }
            if ((this.mRemoteFeatureMask & 2048) == 2048) {
                bluetoothMapAppParams2.setDatabaseIdentifier(0L, this.mMasInstance.getDbIdentifier());
            }
            if ((this.mRemoteFeatureMask & 4096) == 4096) {
                this.mObserver.refreshFolderVersionCounter();
                bluetoothMapAppParams2.setFolderVerCounter(this.mMasInstance.getFolderVersionCounter(), 0L);
            }
            bluetoothMapAppParams2.setMseTime(Calendar.getInstance().getTime().getTime());
            headerSet.setHeader(76, bluetoothMapAppParams2.encodeParams());
            operation.sendHeaders(headerSet);
            OutputStream outputStreamOpenOutputStream = operation.openOutputStream();
            int maxPacketSize = operation.getMaxPacketSize();
            if (bArrEncode != null) {
                while (i < bArrEncode.length && !this.mIsAborted) {
                    try {
                        try {
                            try {
                                int iMin = Math.min(maxPacketSize, bArrEncode.length - i);
                                outputStreamOpenOutputStream.write(bArrEncode, i, iMin);
                                i += iMin;
                            } catch (IOException e) {
                                if (D) {
                                    Log.w(TAG, e);
                                }
                                if (outputStreamOpenOutputStream != null) {
                                    outputStreamOpenOutputStream.close();
                                }
                            }
                        } catch (IOException e2) {
                        }
                    } finally {
                        if (outputStreamOpenOutputStream != null) {
                            try {
                                outputStreamOpenOutputStream.close();
                            } catch (IOException e3) {
                            }
                        }
                    }
                }
                if (outputStreamOpenOutputStream != null) {
                    outputStreamOpenOutputStream.close();
                }
                if (i != bArrEncode.length && !this.mIsAborted) {
                    Log.w(TAG, "sendMessageListingRsp: bytesWritten != outBytes.length - sending OBEX_HTTP_BAD_REQUEST");
                    return BluetoothShare.STATUS_RUNNING;
                }
            }
            return 160;
        } catch (IOException e4) {
            Log.w(TAG, "sendMessageListingRsp: IOException - sending OBEX_HTTP_BAD_REQUEST", e4);
            if (!this.mIsAborted) {
                return BluetoothShare.STATUS_RUNNING;
            }
            if (D) {
                Log.d(TAG, "sendMessageListingRsp Operation Aborted");
            }
            return 160;
        } catch (IllegalArgumentException e5) {
            Log.w(TAG, "sendMessageListingRsp: IllegalArgumentException - sending OBEX_HTTP_BAD_REQUEST", e5);
            return BluetoothShare.STATUS_RUNNING;
        }
    }

    private void setMsgTypeFilterParams(BluetoothMapAppParams bluetoothMapAppParams, boolean z) {
        int i;
        if (!this.mEnableSmsMms) {
            i = 11;
        } else {
            i = 0;
        }
        if (this.mAccount == null) {
            i = i | 4 | 16;
        } else {
            if (this.mAccount.getType() != BluetoothMapUtils.TYPE.EMAIL) {
                i |= 4;
            }
            if (this.mAccount.getType() != BluetoothMapUtils.TYPE.IM) {
                i |= 16;
            }
        }
        if (z) {
            bluetoothMapAppParams.setFilterMessageType(i);
            return;
        }
        int filterMessageType = bluetoothMapAppParams.getFilterMessageType();
        if (filterMessageType == -1) {
            bluetoothMapAppParams.setFilterMessageType(filterMessageType);
        } else {
            bluetoothMapAppParams.setFilterMessageType(filterMessageType | i);
        }
    }

    private int sendConvoListingRsp(Operation operation, BluetoothMapAppParams bluetoothMapAppParams, String str) throws Throwable {
        BluetoothMapConvoListing bluetoothMapConvoListingConvoListing;
        byte[] bArrEncode;
        HeaderSet headerSet = new HeaderSet();
        BluetoothMapAppParams bluetoothMapAppParams2 = new BluetoothMapAppParams();
        int i = 0;
        if (bluetoothMapAppParams == null) {
            bluetoothMapAppParams = new BluetoothMapAppParams();
            bluetoothMapAppParams.setMaxListCount(1024);
            bluetoothMapAppParams.setStartOffset(0);
        }
        setMsgTypeFilterParams(bluetoothMapAppParams, true);
        try {
            if (bluetoothMapAppParams.getMaxListCount() == -1) {
                bluetoothMapAppParams.setMaxListCount(1024);
            }
            if (bluetoothMapAppParams.getStartOffset() == -1) {
                bluetoothMapAppParams.setStartOffset(0);
            }
            if (bluetoothMapAppParams.getMaxListCount() != 0) {
                bluetoothMapConvoListingConvoListing = this.mOutContent.convoListing(bluetoothMapAppParams, false);
                bluetoothMapAppParams2.setConvoListingSize(bluetoothMapConvoListingConvoListing.getCount());
                bArrEncode = bluetoothMapConvoListingConvoListing.encode();
                if (D) {
                    Log.d(TAG, "outBytes size:" + bArrEncode.length);
                }
            } else {
                bluetoothMapConvoListingConvoListing = this.mOutContent.convoListing(bluetoothMapAppParams, true);
                bluetoothMapAppParams2.setConvoListingSize(bluetoothMapConvoListingConvoListing.getCount());
                if (this.mEnableSmsMms) {
                    this.mOutContent.refreshSmsMmsConvoVersions();
                }
                if (this.mAccount != null) {
                    this.mOutContent.refreshImEmailConvoVersions();
                }
                this.mObserver.refreshConvoListVersionCounter();
                if ((this.mRemoteFeatureMask & 8192) > 0) {
                    bluetoothMapAppParams2.setConvoListingVerCounter(this.mMasInstance.getCombinedConvoListVersionCounter(), 0L);
                }
                operation.noBodyHeader();
                bArrEncode = null;
            }
            if (D) {
                Log.d(TAG, "outList size:" + bluetoothMapConvoListingConvoListing.getCount() + " MaxListCount: " + bluetoothMapAppParams.getMaxListCount());
            }
            bluetoothMapAppParams2.setDatabaseIdentifier(0L, this.mMasInstance.getDbIdentifier());
            bluetoothMapAppParams2.setMseTime(Calendar.getInstance().getTime().getTime());
            headerSet.setHeader(76, bluetoothMapAppParams2.encodeParams());
            operation.sendHeaders(headerSet);
            OutputStream outputStreamOpenOutputStream = operation.openOutputStream();
            int maxPacketSize = operation.getMaxPacketSize();
            if (bArrEncode != null) {
                while (i < bArrEncode.length && !this.mIsAborted) {
                    try {
                        try {
                            try {
                                int iMin = Math.min(maxPacketSize, bArrEncode.length - i);
                                outputStreamOpenOutputStream.write(bArrEncode, i, iMin);
                                i += iMin;
                            } catch (IOException e) {
                                if (D) {
                                    Log.w(TAG, e);
                                }
                                if (outputStreamOpenOutputStream != null) {
                                    outputStreamOpenOutputStream.close();
                                }
                            }
                        } catch (IOException e2) {
                        }
                    } finally {
                        if (outputStreamOpenOutputStream != null) {
                            try {
                                outputStreamOpenOutputStream.close();
                            } catch (IOException e3) {
                            }
                        }
                    }
                }
                if (outputStreamOpenOutputStream != null) {
                    outputStreamOpenOutputStream.close();
                }
                if (i != bArrEncode.length && !this.mIsAborted) {
                    Log.w(TAG, "sendConvoListingRsp: bytesWritten != outBytes.length - sending OBEX_HTTP_BAD_REQUEST");
                    return BluetoothShare.STATUS_RUNNING;
                }
            }
            return 160;
        } catch (IOException e4) {
            Log.w(TAG, "sendConvoListingRsp: IOException - sending OBEX_HTTP_BAD_REQUEST", e4);
            if (!this.mIsAborted) {
                return BluetoothShare.STATUS_RUNNING;
            }
            if (D) {
                Log.d(TAG, "sendConvoListingRsp Operation Aborted");
            }
            return 160;
        } catch (IllegalArgumentException e5) {
            Log.w(TAG, "sendConvoListingRsp: IllegalArgumentException - sending OBEX_HTTP_BAD_REQUEST", e5);
            return BluetoothShare.STATUS_RUNNING;
        }
    }

    private int sendFolderListingRsp(Operation operation, BluetoothMapAppParams bluetoothMapAppParams) {
        byte[] bArrEncode;
        BluetoothMapAppParams bluetoothMapAppParams2 = new BluetoothMapAppParams();
        HeaderSet headerSet = new HeaderSet();
        int i = 1024;
        if (bluetoothMapAppParams == null) {
            bluetoothMapAppParams = new BluetoothMapAppParams();
            bluetoothMapAppParams.setMaxListCount(1024);
        }
        try {
            int maxListCount = bluetoothMapAppParams.getMaxListCount();
            int startOffset = bluetoothMapAppParams.getStartOffset();
            int i2 = 0;
            if (startOffset == -1) {
                startOffset = 0;
            }
            if (maxListCount != -1) {
                i = maxListCount;
            }
            if (i != 0) {
                bArrEncode = this.mCurrentFolder.encode(startOffset, i);
            } else {
                bluetoothMapAppParams2.setFolderListingSize(this.mCurrentFolder.getSubFolderCount());
                operation.noBodyHeader();
                bArrEncode = null;
            }
            headerSet.setHeader(76, bluetoothMapAppParams2.encodeParams());
            operation.sendHeaders(headerSet);
            OutputStream outputStreamOpenOutputStream = i != 0 ? operation.openOutputStream() : null;
            int maxPacketSize = operation.getMaxPacketSize();
            if (bArrEncode == null) {
                return 160;
            }
            while (i2 < bArrEncode.length && !this.mIsAborted) {
                try {
                    try {
                        int iMin = Math.min(maxPacketSize, bArrEncode.length - i2);
                        outputStreamOpenOutputStream.write(bArrEncode, i2, iMin);
                        i2 += iMin;
                    } catch (IOException e) {
                    }
                } catch (IOException e2) {
                    if (outputStreamOpenOutputStream != null) {
                        outputStreamOpenOutputStream.close();
                    }
                } catch (Throwable th) {
                    if (outputStreamOpenOutputStream != null) {
                        try {
                            outputStreamOpenOutputStream.close();
                        } catch (IOException e3) {
                        }
                    }
                    throw th;
                }
            }
            if (outputStreamOpenOutputStream != null) {
                outputStreamOpenOutputStream.close();
            }
            if (i2 == bArrEncode.length || this.mIsAborted) {
                return 160;
            }
            return BluetoothShare.STATUS_RUNNING;
        } catch (IOException e4) {
            Log.w(TAG, "sendFolderListingRsp: IOException - sending OBEX_HTTP_BAD_REQUEST Exception:", e4);
            if (!this.mIsAborted) {
                return BluetoothShare.STATUS_RUNNING;
            }
            if (D) {
                Log.d(TAG, "sendFolderListingRsp Operation Aborted");
            }
            return 160;
        } catch (IllegalArgumentException e5) {
            Log.w(TAG, "sendFolderListingRsp: IllegalArgumentException - sending OBEX_HTTP_BAD_REQUEST Exception:", e5);
            return 204;
        }
    }

    private int sendMASInstanceInformationRsp(Operation operation, BluetoothMapAppParams bluetoothMapAppParams) {
        String uciFull;
        try {
            if (this.mMasId != bluetoothMapAppParams.getMasInstanceId()) {
                return BluetoothShare.STATUS_RUNNING;
            }
            if (this.mAccount != null) {
                if (this.mAccount.getType() == BluetoothMapUtils.TYPE.EMAIL) {
                    uciFull = this.mAccount.getName() != null ? this.mAccount.getName() : "EMAIL";
                } else if (this.mAccount.getType() == BluetoothMapUtils.TYPE.IM) {
                    uciFull = this.mAccount.getUciFull();
                    if (uciFull == null) {
                        String uci = this.mAccount.getUci();
                        int length = 5;
                        if (uci != null) {
                            length = 5 + uci.length();
                        }
                        StringBuilder sb = new StringBuilder(length);
                        sb.append("un");
                        if (this.mMasId < 10) {
                            sb.append("00");
                        } else if (this.mMasId < 100) {
                            sb.append("0");
                        }
                        sb.append(this.mMasId);
                        if (uci != null) {
                            sb.append(":");
                            sb.append(uci);
                        }
                        uciFull = sb.toString();
                    }
                } else {
                    uciFull = null;
                }
            } else {
                uciFull = BluetoothMapMasInstance.TYPE_SMS_MMS_STR;
            }
            byte[] bArrTruncateUtf8StringToBytearray = BluetoothMapUtils.truncateUtf8StringToBytearray(uciFull, 200);
            OutputStream outputStreamOpenOutputStream = operation.openOutputStream();
            int maxPacketSize = operation.getMaxPacketSize();
            if (bArrTruncateUtf8StringToBytearray == null) {
                return 160;
            }
            int i = 0;
            while (i < bArrTruncateUtf8StringToBytearray.length && !this.mIsAborted) {
                try {
                    try {
                        int iMin = Math.min(maxPacketSize, bArrTruncateUtf8StringToBytearray.length - i);
                        outputStreamOpenOutputStream.write(bArrTruncateUtf8StringToBytearray, i, iMin);
                        i += iMin;
                    } catch (IOException e) {
                    }
                } catch (IOException e2) {
                    if (outputStreamOpenOutputStream != null) {
                        outputStreamOpenOutputStream.close();
                    }
                } catch (Throwable th) {
                    if (outputStreamOpenOutputStream != null) {
                        try {
                            outputStreamOpenOutputStream.close();
                        } catch (IOException e3) {
                        }
                    }
                    throw th;
                }
            }
            if (outputStreamOpenOutputStream != null) {
                outputStreamOpenOutputStream.close();
            }
            if (i == bArrTruncateUtf8StringToBytearray.length || this.mIsAborted) {
                return 160;
            }
            return BluetoothShare.STATUS_RUNNING;
        } catch (IOException e4) {
            Log.w(TAG, "sendMASInstanceInformationRsp: IOException - sending OBEX_HTTP_BAD_REQUEST", e4);
            if (!this.mIsAborted) {
                return BluetoothShare.STATUS_RUNNING;
            }
            if (D) {
                Log.d(TAG, "sendMASInstanceInformationRsp Operation Aborted");
            }
            return 160;
        }
    }

    private int sendGetMessageRsp(Operation operation, String str, BluetoothMapAppParams bluetoothMapAppParams, String str2) {
        try {
            byte[] message = this.mOutContent.getMessage(str, bluetoothMapAppParams, this.mCurrentFolder, str2);
            if ((BluetoothMapUtils.getMsgTypeFromHandle(str).equals(BluetoothMapUtils.TYPE.EMAIL) || BluetoothMapUtils.getMsgTypeFromHandle(str).equals(BluetoothMapUtils.TYPE.IM)) && bluetoothMapAppParams.getFractionRequest() == 0) {
                BluetoothMapAppParams bluetoothMapAppParams2 = new BluetoothMapAppParams();
                HeaderSet headerSet = new HeaderSet();
                bluetoothMapAppParams2.setFractionDeliver(1);
                headerSet.setHeader(76, bluetoothMapAppParams2.encodeParams());
                operation.sendHeaders(headerSet);
            }
            OutputStream outputStreamOpenOutputStream = operation.openOutputStream();
            int maxPacketSize = operation.getMaxPacketSize();
            if (message == null) {
                return 160;
            }
            int i = 0;
            while (i < message.length && !this.mIsAborted) {
                try {
                    try {
                        try {
                            int iMin = Math.min(maxPacketSize, message.length - i);
                            outputStreamOpenOutputStream.write(message, i, iMin);
                            i += iMin;
                        } catch (IOException e) {
                            if (D && e.getMessage().equals("Abort Received")) {
                                Log.w(TAG, "getMessage() Aborted...", e);
                            }
                            if (outputStreamOpenOutputStream != null) {
                                outputStreamOpenOutputStream.close();
                            }
                        }
                    } catch (Throwable th) {
                        if (outputStreamOpenOutputStream != null) {
                            try {
                                outputStreamOpenOutputStream.close();
                            } catch (IOException e2) {
                            }
                        }
                        throw th;
                    }
                } catch (IOException e3) {
                }
            }
            if (outputStreamOpenOutputStream != null) {
                outputStreamOpenOutputStream.close();
            }
            if (i == message.length || this.mIsAborted) {
                return 160;
            }
            return BluetoothShare.STATUS_RUNNING;
        } catch (IOException e4) {
            Log.w(TAG, "sendGetMessageRsp: IOException - sending OBEX_HTTP_BAD_REQUEST", e4);
            if (!this.mIsAborted) {
                return BluetoothShare.STATUS_RUNNING;
            }
            if (D) {
                Log.d(TAG, "sendGetMessageRsp Operation Aborted");
            }
            return 160;
        } catch (IllegalArgumentException e5) {
            Log.w(TAG, "sendGetMessageRsp: IllegalArgumentException (e.g. invalid handle) - sending OBEX_HTTP_BAD_REQUEST", e5);
            return BluetoothShare.STATUS_RUNNING;
        }
    }

    public int onDelete(HeaderSet headerSet, HeaderSet headerSet2) {
        if (D) {
            Log.v(TAG, "onDelete() " + headerSet.toString());
        }
        this.mIsAborted = false;
        notifyUpdateWakeLock();
        BluetoothMapAppParams bluetoothMapAppParams = null;
        try {
            String str = (String) headerSet.getHeader(66);
            String str2 = (String) headerSet.getHeader(1);
            byte[] bArr = (byte[]) headerSet.getHeader(76);
            if (bArr != null) {
                bluetoothMapAppParams = new BluetoothMapAppParams(bArr);
            }
            if (D) {
                Log.d(TAG, "type = " + str + ", name = " + str2);
            }
            if (!str.equals(TYPE_SET_NOTIFICATION_FILTER)) {
                return str.equals(TYPE_SET_OWNER_STATUS) ? setOwnerStatus(str2, bluetoothMapAppParams) : BluetoothShare.STATUS_RUNNING;
            }
            this.mObserver.setNotificationFilter(bluetoothMapAppParams.getNotificationFilter());
            return 160;
        } catch (RemoteException e) {
            try {
                this.mProviderClient = acquireUnstableContentProviderOrThrow();
            } catch (RemoteException e2) {
            }
            return BluetoothShare.STATUS_RUNNING;
        } catch (Exception e3) {
            if (D) {
                Log.e(TAG, "Exception occured while handling request", e3);
            } else {
                Log.e(TAG, "Exception occured while handling request");
            }
            if (this.mIsAborted) {
                return 160;
            }
            return BluetoothShare.STATUS_RUNNING;
        }
    }

    private void notifyUpdateWakeLock() {
        if (this.mCallback != null) {
            Message messageObtain = Message.obtain(this.mCallback);
            messageObtain.what = SapService.MSG_ACQUIRE_WAKE_LOCK;
            messageObtain.sendToTarget();
        }
    }

    private static void logHeader(HeaderSet headerSet) {
        Log.v(TAG, "Dumping HeaderSet " + headerSet.toString());
        try {
            Log.v(TAG, "CONNECTION_ID : " + headerSet.getHeader(203));
            Log.v(TAG, "NAME : " + headerSet.getHeader(1));
            Log.v(TAG, "TYPE : " + headerSet.getHeader(66));
            Log.v(TAG, "TARGET : " + headerSet.getHeader(70));
            Log.v(TAG, "WHO : " + headerSet.getHeader(74));
            Log.v(TAG, "APPLICATION_PARAMETER : " + headerSet.getHeader(76));
        } catch (IOException e) {
            Log.e(TAG, "dump HeaderSet error " + e);
        }
        Log.v(TAG, "NEW!!! Dumping HeaderSet END");
    }

    private int pushMessage(final Operation operation, String str, final BluetoothMapAppParams bluetoothMapAppParams, boolean z) {
        long j;
        long subIdByThread;
        if (bluetoothMapAppParams.getCharset() == -1) {
            if (D) {
                Log.d(TAG, "Missing charset - unable to decode message content. appParams.getCharset() = " + bluetoothMapAppParams.getCharset());
            }
            return 204;
        }
        try {
            BluetoothMapFolderElement folderElementFromName = getFolderElementFromName(str);
            if (folderElementFromName == null) {
                Log.w(TAG, "[pushMessage] folderElement == null - sending OBEX_HTTP_PRECON_FAILED");
                return 204;
            }
            final String name = folderElementFromName.getName();
            if (!name.equals(BluetoothMapContract.FOLDER_NAME_OUTBOX) && !name.equals(BluetoothMapContract.FOLDER_NAME_DRAFT)) {
                if (D) {
                    Log.d(TAG, "[pushMessage]: Is only allowed to outbox and draft. folderName=" + name);
                    return 198;
                }
                return 198;
            }
            final InputStream inputStreamOpenInputStream = operation.openInputStream();
            final BluetoothMapbMessage bluetoothMapbMessage = BluetoothMapbMessage.parse(inputStreamOpenInputStream, bluetoothMapAppParams.getCharset());
            Log.d(TAG, "[pushMessage] BluetoothMapbMessage.parse success");
            if (this.mObserver == null) {
                Log.d(TAG, "[pushMessage] observer == null");
                return 211;
            }
            if (z && bluetoothMapbMessage != null) {
                String singleRecipient = bluetoothMapbMessage.getSingleRecipient();
                if (!TextUtils.isEmpty(singleRecipient)) {
                    long threadIdByNumber = this.mOutContent.getThreadIdByNumber(singleRecipient);
                    if (threadIdByNumber > 0) {
                        subIdByThread = this.mOutContent.getSubIdByThread(threadIdByNumber);
                    } else {
                        subIdByThread = -1;
                    }
                    j = subIdByThread;
                }
            } else {
                j = -1;
            }
            if (j <= -1) {
                final BluetoothMapContentObserver bluetoothMapContentObserver = this.mObserver;
                this.mCallback.post(new Runnable() {
                    @Override
                    public void run() {
                        BluetoothMapObexServer.this.showSubSelectDialog(operation, name, bluetoothMapAppParams, bluetoothMapbMessage, bluetoothMapContentObserver, inputStreamOpenInputStream);
                    }
                });
                return 160;
            }
            return sendMessage(operation, name, bluetoothMapAppParams, bluetoothMapbMessage, this.mObserver, inputStreamOpenInputStream, j);
        } catch (IllegalArgumentException e) {
            if (D) {
                Log.w(TAG, "[pushMessage] Wrongly formatted bMessage received", e);
            }
            return 204;
        } catch (Exception e2) {
            Log.e(TAG, "[pushMessage] Exception occured: ", e2);
            return BluetoothShare.STATUS_RUNNING;
        }
    }

    private int sendMessage(Operation operation, String str, BluetoothMapAppParams bluetoothMapAppParams, BluetoothMapbMessage bluetoothMapbMessage, BluetoothMapContentObserver bluetoothMapContentObserver, InputStream inputStream, long j) {
        try {
            long jPushMessage = bluetoothMapContentObserver.pushMessage(bluetoothMapbMessage, getFolderElementFromName(str), bluetoothMapAppParams, this.mBaseUriString, j);
            if (D) {
                Log.d(TAG, "[sendMessage] handle: " + jPushMessage);
            }
            if (jPushMessage < 0) {
                Log.d(TAG, "[sendMessage] handle < 0");
                return 211;
            }
            HeaderSet headerSet = new HeaderSet();
            String mapHandle = BluetoothMapUtils.getMapHandle(jPushMessage, bluetoothMapbMessage.getType());
            if (D) {
                Log.d(TAG, "[sendMessage] handleStr: " + mapHandle + " message.getType(): " + bluetoothMapbMessage.getType());
            }
            headerSet.setHeader(1, mapHandle);
            operation.sendHeaders(headerSet);
            inputStream.close();
            return 160;
        } catch (IllegalArgumentException e) {
            if (D) {
                Log.w(TAG, "[sendMessage] Wrongly formatted bMessage received", e);
                return 204;
            }
            return 204;
        } catch (Exception e2) {
            Log.e(TAG, "[sendMessage] Exception occured: ", e2);
            return BluetoothShare.STATUS_RUNNING;
        }
    }

    public void showSubSelectDialog(final Operation operation, final String str, final BluetoothMapAppParams bluetoothMapAppParams, final BluetoothMapbMessage bluetoothMapbMessage, final BluetoothMapContentObserver bluetoothMapContentObserver, final InputStream inputStream) {
        Log.d(TAG, "[showSubSelectDialog] enter");
        String str2 = "";
        ArrayList<BluetoothMapbMessage.VCard> recipients = bluetoothMapbMessage.getRecipients();
        boolean z = true;
        if (recipients != null && recipients.size() == 1) {
            String[] phoneNumber = recipients.get(0).getPhoneNumber();
            if (phoneNumber != null && phoneNumber.length == 1) {
                str2 = phoneNumber[0];
            }
        } else {
            z = false;
        }
        Log.d(TAG, "[showSubSelectDialog] isSingleRecipient = " + z + " recipentNumber = " + str2);
        final List<SubscriptionInfo> listUpdateSubInfoList = updateSubInfoList();
        BluetoothMapSubAdapter bluetoothMapSubAdapter = new BluetoothMapSubAdapter(this.mContext, listUpdateSubInfoList);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
        builder.setTitle(this.mContext.getString(R.string.sim_selected_dialog_title));
        builder.setCancelable(false);
        builder.setAdapter(bluetoothMapSubAdapter, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                final long subscriptionId = ((SubscriptionInfo) listUpdateSubInfoList.get(i)).getSubscriptionId();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(BluetoothMapObexServer.TAG, "[showSubSelectDialog] sendMessage enter: subId " + subscriptionId);
                        BluetoothMapObexServer.this.sendMessage(operation, str, bluetoothMapAppParams, bluetoothMapbMessage, bluetoothMapContentObserver, inputStream, subscriptionId);
                    }
                }).start();
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialogCreate = builder.create();
        alertDialogCreate.getWindow().setType(2038);
        alertDialogCreate.show();
    }

    private List<SubscriptionInfo> updateSubInfoList() {
        ArrayList arrayList = new ArrayList();
        List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(this.mContext).getActiveSubscriptionInfoList();
        Log.d(TAG, "updateSubInfoList subInfoRecordInOneSim=" + activeSubscriptionInfoList);
        if (activeSubscriptionInfoList != null && activeSubscriptionInfoList.size() > 0) {
            for (int i = 0; i < activeSubscriptionInfoList.size(); i++) {
                SubscriptionInfo subscriptionInfo = activeSubscriptionInfoList.get(i);
                arrayList.add(subscriptionInfo);
                Log.i(TAG, "updateSubInfoList name=" + ((Object) subscriptionInfo.getDisplayName()));
            }
        }
        return arrayList;
    }
}

package org.gsma.joyn.contacts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.provider.ContactsContract;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.gsma.joyn.ICoreServiceWrapper;
import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceConfiguration;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.Logger;
import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.contacts.IContactsService;

public class ContactsService extends JoynService {
    public static final ComponentName DEFAULT_IMPL_COMPONENT = new ComponentName("com.orangelabs.rcs", "com.orangelabs.rcs.service.RcsCoreService");
    public static final ComponentName STANDALONE_PRESENCE_IMPL_COMPONENT = new ComponentName("com.mediatek.presence", "com.mediatek.presence.service.RcsCoreService");
    public static final String TAG = "TAPI-ContactsService";
    private IContactsService api;
    private ServiceConnection apiConnection;
    private Context mContext;

    public ContactsService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
        this.api = null;
        this.mContext = null;
        this.apiConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Logger.i(ContactsService.TAG, "onServiceConnected entry " + className);
                ICoreServiceWrapper mCoreServiceWrapperBinder = ICoreServiceWrapper.Stub.asInterface(service);
                IBinder binder = null;
                try {
                    binder = mCoreServiceWrapperBinder.getContactsServiceBinder();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                ContactsService.this.setApi(IContactsService.Stub.asInterface(binder));
                if (((JoynService) ContactsService.this).serviceListener != null) {
                    ((JoynService) ContactsService.this).serviceListener.onServiceConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                Logger.i(ContactsService.TAG, "onServiceDisconnected entry " + className);
                ContactsService.this.setApi(null);
                if (((JoynService) ContactsService.this).serviceListener != null) {
                    ((JoynService) ContactsService.this).serviceListener.onServiceDisconnected(2);
                }
            }
        };
        this.mContext = ctx;
    }

    @Override
    public void connect() {
        ComponentName cmp;
        Logger.i(TAG, "ContactsService connect() entry");
        Intent intent = new Intent();
        if (JoynServiceConfiguration.isPresenceDiscoverySupported(this.ctx)) {
            cmp = STANDALONE_PRESENCE_IMPL_COMPONENT;
        } else {
            cmp = DEFAULT_IMPL_COMPONENT;
        }
        intent.setComponent(cmp);
        this.ctx.bindService(intent, this.apiConnection, 0);
    }

    @Override
    public void disconnect() {
        Logger.i(TAG, "ContactsService disconnect() entry");
        try {
            this.ctx.unbindService(this.apiConnection);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    protected void setApi(IInterface api) {
        super.setApi(api);
        this.api = (IContactsService) api;
    }

    public JoynContact getJoynContact(String contactId) throws JoynServiceException {
        Logger.i(TAG, "getJoynContact entry Id=" + contactId);
        if (this.api != null) {
            try {
                return this.api.getJoynContact(contactId);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<JoynContact> getJoynContacts() throws JoynServiceException {
        Logger.i(TAG, "getJoynContacts entry ");
        if (this.api != null) {
            try {
                Set<JoynContact> result = new HashSet<>();
                List<JoynContact> contacts = this.api.getJoynContacts();
                for (int i = 0; i < contacts.size(); i++) {
                    JoynContact contact = contacts.get(i);
                    result.add(contact);
                }
                Logger.i(TAG, "getJoynContacts returning result = " + result);
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<JoynContact> getJoynContactsOnline() throws JoynServiceException {
        Logger.i(TAG, "getJoynContactsOnline() entry ");
        if (this.api != null) {
            try {
                Set<JoynContact> result = new HashSet<>();
                result.addAll(this.api.getJoynContactsOnline());
                Logger.i(TAG, "getJoynContactsOnline() returning result = " + result);
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<JoynContact> getJoynContactsSupporting(String serviceId) throws JoynServiceException {
        Logger.i(TAG, "getJoynContactsSupporting() entry ");
        if (this.api != null) {
            try {
                Set<JoynContact> result = new HashSet<>();
                result.addAll(this.api.getJoynContactsSupporting(serviceId));
                Logger.i(TAG, "getJoynContactsSupporting() returning result = " + result);
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public String getVCard(Uri contactUri) throws JoynServiceException {
        Logger.i(TAG, "getVCard() entry ");
        String fileName = null;
        Cursor cursor = this.mContext.getContentResolver().query(contactUri, null, null, null, null);
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ChatLog.Message.DISPLAY_NAME));
            String lookupKey = cursor.getString(cursor.getColumnIndex("lookup"));
            Uri vCardUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
            Logger.i(TAG, "getVCard() uri= " + vCardUri);
            try {
                AssetFileDescriptor fd = this.mContext.getContentResolver().openAssetFileDescriptor(vCardUri, "r");
                FileInputStream fis = fd.createInputStream();
                byte[] buf = new byte[(int) fd.getDeclaredLength()];
                if (buf.length > 0) {
                    int n = fis.read(buf);
                    String Vcard = new String(buf);
                    fileName = Environment.getExternalStorageDirectory().toString() + File.separator + name + ".vcf";
                    Logger.i(TAG, "getVCard() filename= " + fileName + ",bytes read:" + n);
                    File vCardFile = new File(fileName);
                    if (vCardFile.exists()) {
                        vCardFile.delete();
                    }
                    FileOutputStream mFileOutputStream = new FileOutputStream(vCardFile, true);
                    mFileOutputStream.write(Vcard.toString().getBytes());
                    mFileOutputStream.close();
                }
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        cursor.close();
        return fileName;
    }

    public List<String> getImBlockedContactsFromLocal() {
        Logger.i(TAG, "getImBlockedContactsFromLocal entry");
        if (this.api == null) {
            return null;
        }
        try {
            return this.api.getImBlockedContactsFromLocal();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isImBlockedForContact(String contact) throws JoynServiceException {
        if (this.api != null) {
            try {
                return this.api.isImBlockedForContact(contact);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public List<String> getImBlockedContacts() throws JoynServiceException {
        if (this.api != null) {
            try {
                return this.api.getImBlockedContacts();
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public String getTimeStampForBlockedContact(String contact) throws JoynServiceException {
        Logger.i(TAG, "getTimeStampForBlockedContact entry" + contact);
        if (this.api != null) {
            try {
                return this.api.getTimeStampForBlockedContact(contact);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void setImBlockedForContact(String contact, boolean flag) throws JoynServiceException {
        Logger.i(TAG, "setImBlockedForContact entry" + contact + ",flag=" + flag);
        if (this.api != null) {
            try {
                this.api.setImBlockedForContact(contact, flag);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void setFtBlockedForContact(String contact, boolean flag) throws JoynServiceException {
        Logger.i(TAG, "setFtBlockedForContact entry" + contact + ",flag=" + flag);
        if (this.api != null) {
            try {
                this.api.setFtBlockedForContact(contact, flag);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public boolean isRcsValidNumber(String number) throws JoynServiceException {
        if (this.api != null) {
            try {
                return this.api.isRcsValidNumber(number);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public int getRegistrationState(String contact) throws JoynServiceException {
        if (this.api != null) {
            try {
                return this.api.getRegistrationState(contact);
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void loadImBlockedContactsToLocal() throws JoynServiceException {
        if (this.api != null) {
            try {
                this.api.loadImBlockedContactsToLocal();
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }
}

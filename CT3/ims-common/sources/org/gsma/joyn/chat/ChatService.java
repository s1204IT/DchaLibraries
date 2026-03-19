package org.gsma.joyn.chat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.gsma.joyn.ICoreServiceWrapper;
import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.JoynServiceRegistrationListener;
import org.gsma.joyn.Logger;
import org.gsma.joyn.chat.IChat;
import org.gsma.joyn.chat.IChatService;
import org.gsma.joyn.chat.IGroupChat;

public class ChatService extends JoynService {
    public static final String TAG = "TAPI-ChatService";
    private IChatService api;
    private ServiceConnection apiConnection;

    public ChatService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
        this.api = null;
        this.apiConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Logger.i(ChatService.TAG, "onServiceConnected entry" + service);
                ICoreServiceWrapper mCoreServiceWrapperBinder = ICoreServiceWrapper.Stub.asInterface(service);
                IBinder binder = null;
                try {
                    binder = mCoreServiceWrapperBinder.getChatServiceBinder();
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                ChatService.this.setApi(IChatService.Stub.asInterface(binder));
                if (((JoynService) ChatService.this).serviceListener != null) {
                    ((JoynService) ChatService.this).serviceListener.onServiceConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                ChatService.this.setApi(null);
                Logger.i(ChatService.TAG, "onServiceDisconnected entry");
                if (((JoynService) ChatService.this).serviceListener != null) {
                    ((JoynService) ChatService.this).serviceListener.onServiceDisconnected(2);
                }
            }
        };
    }

    @Override
    public void connect() {
        Logger.i(TAG, "connected() entry");
        Intent intent = new Intent();
        ComponentName cmp = new ComponentName("com.orangelabs.rcs", "com.orangelabs.rcs.service.RcsCoreService");
        intent.setComponent(cmp);
        boolean connected = this.ctx.bindService(intent, this.apiConnection, 0);
        Logger.i(TAG, "connect() exit status" + connected);
    }

    @Override
    public void disconnect() {
        try {
            Logger.i(TAG, "disconnect() entry");
            this.ctx.unbindService(this.apiConnection);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    protected void setApi(IInterface api) {
        super.setApi(api);
        Logger.i(TAG, "setApi entry" + api);
        this.api = (IChatService) api;
    }

    public ChatServiceConfiguration getConfiguration() throws JoynServiceException {
        if (this.api != null) {
            try {
                Logger.i(TAG, "getConfiguration entry");
                return this.api.getConfiguration();
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    @Override
    public int getServiceVersion() throws JoynServiceException {
        if (this.api != null) {
            if (this.version == null) {
                try {
                    this.version = Integer.valueOf(this.api.getServiceVersion());
                    Logger.i(TAG, "getServiceVersion entry " + this.version);
                } catch (Exception e) {
                    throw new JoynServiceException(e.getMessage());
                }
            }
            return this.version.intValue();
        }
        throw new JoynServiceNotAvailableException();
    }

    @Override
    public boolean isServiceRegistered() throws JoynServiceException {
        Logger.i(TAG, "isServiceRegistered entry ");
        if (this.api != null) {
            try {
                boolean serviceStatus = this.api.isServiceRegistered();
                Logger.i(TAG, "isServiceRegistered entry " + serviceStatus);
                return serviceStatus;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    @Override
    public void addServiceRegistrationListener(JoynServiceRegistrationListener listener) throws JoynServiceException {
        Logger.i(TAG, "addServiceRegistrationListener entry" + listener);
        if (this.api != null) {
            try {
                this.api.addServiceRegistrationListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    @Override
    public void removeServiceRegistrationListener(JoynServiceRegistrationListener listener) throws JoynServiceException {
        Logger.i(TAG, "removeServiceRegistrationListener entry" + listener);
        if (this.api != null) {
            try {
                this.api.removeServiceRegistrationListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Chat openSingleChat(String contact, ChatListener listener) throws JoynServiceException {
        Logger.i(TAG, "openSingleChat entry " + contact);
        if (this.api != null) {
            try {
                IChat chatIntf = this.api.openSingleChat(contact, listener);
                if (chatIntf != null) {
                    return new Chat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public ExtendChat openSingleChat(String contact, ExtendChatListener listener) throws JoynServiceException {
        Logger.i(TAG, "openSingleChat entry " + contact);
        if (this.api != null) {
            try {
                IExtendChat chatIntf = this.api.openSingleChatEx(contact, listener);
                if (chatIntf != null) {
                    return new ExtendChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public ExtendChat openSecondaryDeviceChat(ExtendChatListener listener) throws JoynServiceException {
        Logger.i(TAG, "openSecondaryDeviceChat entry ");
        if (this.api != null) {
            try {
                IExtendChat chatIntf = this.api.openSecondaryDeviceChat(listener);
                if (chatIntf != null) {
                    return new ExtendChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public ExtendChat openMultipleChat(List<String> contacts, ExtendChatListener listener) throws JoynServiceException {
        Logger.i(TAG, "openChat entry " + contacts);
        if (this.api != null) {
            try {
                IExtendChat chatIntf = this.api.openMultipleChat(contacts, listener);
                if (chatIntf != null) {
                    return new ExtendChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public GroupChat initiateGroupChat(Set<String> contacts, String subject, GroupChatListener listener) throws JoynServiceException {
        Logger.i(TAG, "initiateGroupChat entry= " + contacts + " subject =" + subject);
        if (this.api != null) {
            try {
                IGroupChat chatIntf = this.api.initiateGroupChat(new ArrayList(contacts), subject, listener);
                if (chatIntf != null) {
                    return new GroupChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public GroupChat initiateClosedGroupChat(Set<String> contacts, String subject, GroupChatListener listener) throws JoynServiceException {
        Logger.i(TAG, "initiateClosedGroupChat entry= " + contacts + " subject =" + subject);
        if (this.api != null) {
            try {
                IGroupChat chatIntf = this.api.initiateClosedGroupChat(new ArrayList(contacts), subject, listener);
                if (chatIntf != null) {
                    return new GroupChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public GroupChat rejoinGroupChat(String chatId) throws JoynServiceException {
        Logger.i(TAG, "rejoinGroupChat entry= " + chatId);
        if (this.api != null) {
            try {
                IGroupChat chatIntf = this.api.rejoinGroupChat(chatId);
                if (chatIntf != null) {
                    return new GroupChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public GroupChat rejoinGroupChatId(String chatId, String rejoinId) throws JoynServiceException {
        Logger.i(TAG, "rejoinGroupChat entry= " + chatId + "; rejoin id: " + rejoinId);
        if (this.api != null) {
            try {
                IGroupChat chatIntf = this.api.rejoinGroupChatId(chatId, rejoinId);
                if (chatIntf != null) {
                    return new GroupChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public GroupChat restartGroupChat(String chatId) throws JoynServiceException {
        Logger.i(TAG, "restartGroupChat entry= " + chatId);
        if (this.api != null) {
            try {
                IGroupChat chatIntf = this.api.restartGroupChat(chatId);
                if (chatIntf != null) {
                    return new GroupChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void syncAllGroupChats(GroupChatSyncingListener listener) throws JoynServiceException {
        Logger.i(TAG, "syncAllGroupChats ");
        if (this.api != null) {
            try {
                this.api.syncAllGroupChats(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void syncGroupChat(String chatId, GroupChatSyncingListener listener) throws JoynServiceException {
        Logger.i(TAG, "sync group info: " + chatId);
        if (this.api != null) {
            try {
                this.api.syncGroupChat(chatId, listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<Chat> getChats() throws JoynServiceException {
        Logger.i(TAG, "getChats entry ");
        if (this.api != null) {
            try {
                Set<Chat> result = new HashSet<>();
                List<IBinder> chatList = this.api.getChats();
                for (IBinder binder : chatList) {
                    Chat chat = new Chat(IChat.Stub.asInterface(binder));
                    result.add(chat);
                }
                Logger.i(TAG, "getChats returning with  " + result);
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Chat getChat(String chatId) throws JoynServiceException {
        Logger.i(TAG, "getChat entry " + chatId);
        if (this.api != null) {
            try {
                IChat chatIntf = this.api.getChat(chatId);
                if (chatIntf != null) {
                    return new Chat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Chat getChatFor(Intent intent) throws JoynServiceException {
        Logger.i(TAG, "getChatFor entry " + intent);
        if (this.api != null) {
            try {
                String contact = intent.getStringExtra("contact");
                if (contact != null) {
                    return getChat(contact);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public ExtendChat getExtendChat(String contact) throws JoynServiceException {
        Logger.i(TAG, "getChat entry " + contact);
        if (this.api != null) {
            try {
                IExtendChat chatIntf = this.api.getExtendChat(contact);
                if (chatIntf != null) {
                    return new ExtendChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public ExtendChat getExtendChatFor(Intent intent) throws JoynServiceException {
        Logger.i(TAG, "getChatFor entry " + intent);
        if (this.api != null) {
            try {
                String contact = intent.getStringExtra("contact");
                if (contact != null) {
                    return getExtendChat(contact);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public ExtendChat getExtendChatForSecondaryDevice() throws JoynServiceException {
        Logger.i(TAG, "getChatForSecondaryDevice entry ");
        if (this.api != null) {
            try {
                IExtendChat chatIntf = this.api.getChatForSecondaryDevice();
                if (chatIntf != null) {
                    return new ExtendChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public Set<GroupChat> getGroupChats() throws JoynServiceException {
        Logger.i(TAG, "getGroupChats entry ");
        if (this.api != null) {
            try {
                Set<GroupChat> result = new HashSet<>();
                List<IBinder> chatList = this.api.getGroupChats();
                for (IBinder binder : chatList) {
                    GroupChat chat = new GroupChat(IGroupChat.Stub.asInterface(binder));
                    result.add(chat);
                }
                Logger.i(TAG, "getGroupChats returning with " + result);
                return result;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public GroupChat getGroupChat(String chatId) throws JoynServiceException {
        Logger.i(TAG, "getGroupChat entry " + chatId);
        if (this.api != null) {
            try {
                IGroupChat chatIntf = this.api.getGroupChat(chatId);
                if (chatIntf != null) {
                    return new GroupChat(chatIntf);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public GroupChat getGroupChatFor(Intent intent) throws JoynServiceException {
        Logger.i(TAG, "getGroupChat entry " + intent);
        if (this.api != null) {
            try {
                String chatId = intent.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
                if (chatId != null) {
                    return getGroupChat(chatId);
                }
                return null;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void blockGroupMessages(String chatId, boolean flag) throws JoynServiceException {
        Logger.i(TAG, "blockGroupMessages() entry with chatId: " + chatId + ",flag:" + flag);
        if (this.api != null) {
            try {
                this.api.blockGroupMessages(chatId, flag);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void addEventListener(NewChatListener listener) throws JoynServiceException {
        Logger.i(TAG, "addEventListener entry" + listener);
        if (this.api != null) {
            try {
                this.api.addEventListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }

    public void removeEventListener(NewChatListener listener) throws JoynServiceException {
        Logger.i(TAG, "removeEventListener entry" + listener);
        if (this.api != null) {
            try {
                this.api.removeEventListener(listener);
                return;
            } catch (Exception e) {
                throw new JoynServiceException(e.getMessage());
            }
        }
        throw new JoynServiceNotAvailableException();
    }
}

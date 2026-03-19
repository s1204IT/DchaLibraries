package com.android.bluetooth.avrcpcontroller;

import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.a2dpsink.A2dpSinkService;
import com.android.bluetooth.avrcpcontroller.BrowseTree;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class AvrcpControllerStateMachine extends StateMachine {
    private static final int ABS_VOL_BASE = 127;
    static final int ABS_VOL_TIMEOUT_MILLIS = 1000;
    static final int CMD_TIMEOUT_MILLIS = 5000;
    private static final boolean DBG = true;
    private static final ArrayList<MediaBrowser.MediaItem> EMPTY_MEDIA_ITEM_LIST = new ArrayList<>();
    private static final MediaMetadata EMPTY_MEDIA_METADATA = new MediaMetadata.Builder().build();
    static final int GET_FOLDER_ITEMS_PAGINATION_SIZE = 20;
    static final int MAX_FOLDER_ITEMS = 1000;
    static final int MESSAGE_CHANGE_FOLDER_PATH = 8;
    static final int MESSAGE_FETCH_ATTR_AND_PLAY_ITEM = 9;
    static final int MESSAGE_GET_FOLDER_LIST = 6;
    static final int MESSAGE_GET_NOW_PLAYING_LIST = 5;
    static final int MESSAGE_GET_PLAYER_LIST = 7;
    static final int MESSAGE_INTERNAL_ABS_VOL_TIMEOUT = 404;
    static final int MESSAGE_INTERNAL_BROWSE_DEPTH_INCREMENT = 401;
    static final int MESSAGE_INTERNAL_CMD_TIMEOUT = 403;
    static final int MESSAGE_INTERNAL_MOVE_N_LEVELS_UP = 402;
    static final int MESSAGE_PROCESS_BROWSE_CONNECTION_CHANGE = 303;
    static final int MESSAGE_PROCESS_CONNECTION_CHANGE = 302;
    static final int MESSAGE_PROCESS_FOLDER_PATH = 112;
    static final int MESSAGE_PROCESS_GET_FOLDER_ITEMS = 109;
    static final int MESSAGE_PROCESS_GET_FOLDER_ITEMS_OUT_OF_RANGE = 110;
    static final int MESSAGE_PROCESS_GET_PLAYER_ITEMS = 111;
    static final int MESSAGE_PROCESS_PLAY_POS_CHANGED = 106;
    static final int MESSAGE_PROCESS_PLAY_STATUS_CHANGED = 107;
    static final int MESSAGE_PROCESS_RC_FEATURES = 301;
    static final int MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION = 104;
    static final int MESSAGE_PROCESS_SET_ABS_VOL_CMD = 103;
    static final int MESSAGE_PROCESS_SET_ADDRESSED_PLAYER = 114;
    static final int MESSAGE_PROCESS_SET_BROWSED_PLAYER = 113;
    static final int MESSAGE_PROCESS_TRACK_CHANGED = 105;
    static final int MESSAGE_PROCESS_VOLUME_CHANGED_NOTIFICATION = 108;
    static final int MESSAGE_SEND_GROUP_NAVIGATION_CMD = 3;
    static final int MESSAGE_SEND_PASS_THROUGH_CMD = 1;
    static final int MESSAGE_SET_BROWSED_PLAYER = 10;
    static final int MESSAGE_START_METADATA_BROADCASTS = 202;
    static final int MESSAGE_STOP_METADATA_BROADCASTS = 201;
    private static final byte NOTIFICATION_RSP_TYPE_CHANGED = 1;
    private static final byte NOTIFICATION_RSP_TYPE_INTERIM = 0;
    private static final String TAG = "AvrcpControllerSM";
    private static final boolean VDBG = false;
    private AvrcpPlayer mAddressedPlayer;
    private final AudioManager mAudioManager;
    private final BroadcastReceiver mBroadcastReceiver;
    private int mBrowseDepth;
    private BrowseTree mBrowseTree;
    private final ChangeFolderPath mChangeFolderPath;
    private final State mConnected;
    private final Context mContext;
    private final State mDisconnected;
    private final GetFolderList mGetFolderList;
    private final GetPlayerListing mGetPlayerListing;
    private Boolean mIsConnected;
    private final Object mLock;
    private final MoveToRoot mMoveToRoot;
    private int mPreviousPercentageVol;
    private RemoteDevice mRemoteDevice;
    private final SetAddresedPlayerAndPlayItem mSetAddrPlayer;
    private final SetBrowsedPlayer mSetBrowsedPlayer;
    private int mVolumeChangedNotificationsToIgnore;

    static int access$1312(AvrcpControllerStateMachine avrcpControllerStateMachine, int i) {
        int i2 = avrcpControllerStateMachine.mBrowseDepth + i;
        avrcpControllerStateMachine.mBrowseDepth = i2;
        return i2;
    }

    static int access$1320(AvrcpControllerStateMachine avrcpControllerStateMachine, int i) {
        int i2 = avrcpControllerStateMachine.mBrowseDepth - i;
        avrcpControllerStateMachine.mBrowseDepth = i2;
        return i2;
    }

    static int access$1408(AvrcpControllerStateMachine avrcpControllerStateMachine) {
        int i = avrcpControllerStateMachine.mVolumeChangedNotificationsToIgnore;
        avrcpControllerStateMachine.mVolumeChangedNotificationsToIgnore = i + 1;
        return i;
    }

    static int access$1410(AvrcpControllerStateMachine avrcpControllerStateMachine) {
        int i = avrcpControllerStateMachine.mVolumeChangedNotificationsToIgnore;
        avrcpControllerStateMachine.mVolumeChangedNotificationsToIgnore = i - 1;
        return i;
    }

    AvrcpControllerStateMachine(Context context) {
        super(TAG);
        this.mLock = new Object();
        this.mIsConnected = false;
        this.mVolumeChangedNotificationsToIgnore = 0;
        this.mPreviousPercentageVol = -1;
        this.mBrowseDepth = 0;
        this.mBrowseTree = new BrowseTree();
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION") && intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) == 3) {
                    AvrcpControllerStateMachine.this.sendMessage(AvrcpControllerStateMachine.MESSAGE_PROCESS_VOLUME_CHANGED_NOTIFICATION);
                }
            }
        };
        this.mContext = context;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
        this.mDisconnected = new Disconnected();
        this.mConnected = new Connected();
        this.mSetBrowsedPlayer = new SetBrowsedPlayer();
        this.mSetAddrPlayer = new SetAddresedPlayerAndPlayItem();
        this.mChangeFolderPath = new ChangeFolderPath();
        this.mGetFolderList = new GetFolderList();
        this.mGetPlayerListing = new GetPlayerListing();
        this.mMoveToRoot = new MoveToRoot();
        addState(this.mDisconnected);
        addState(this.mConnected);
        addState(this.mSetBrowsedPlayer, this.mConnected);
        addState(this.mSetAddrPlayer, this.mConnected);
        addState(this.mChangeFolderPath, this.mConnected);
        addState(this.mGetFolderList, this.mConnected);
        addState(this.mGetPlayerListing, this.mConnected);
        addState(this.mMoveToRoot, this.mConnected);
        setInitialState(this.mDisconnected);
    }

    class Disconnected extends State {
        Disconnected() {
        }

        public boolean processMessage(Message message) {
            Log.d(AvrcpControllerStateMachine.TAG, " HandleMessage: " + AvrcpControllerStateMachine.dumpMessageString(message.what));
            if (message.what == 302) {
                if (message.arg1 == 2) {
                    AvrcpControllerStateMachine.this.mBrowseTree.init();
                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                    synchronized (AvrcpControllerStateMachine.this.mLock) {
                        AvrcpControllerStateMachine.this.mRemoteDevice = new RemoteDevice(bluetoothDevice);
                        AvrcpControllerStateMachine.this.mAddressedPlayer = new AvrcpPlayer();
                        AvrcpControllerStateMachine.this.mIsConnected = true;
                    }
                    MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.AVRCP_CONTROLLER);
                    Intent intent = new Intent("android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED");
                    intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", 0);
                    intent.putExtra("android.bluetooth.profile.extra.STATE", 2);
                    intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
                    AvrcpControllerStateMachine.this.mContext.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
                }
                return true;
            }
            Log.w(AvrcpControllerStateMachine.TAG, "Currently Disconnected not handling " + AvrcpControllerStateMachine.dumpMessageString(message.what));
            return false;
        }
    }

    class Connected extends State {
        Connected() {
        }

        public boolean processMessage(Message message) {
            Log.d(AvrcpControllerStateMachine.TAG, " HandleMessage: " + AvrcpControllerStateMachine.dumpMessageString(message.what));
            A2dpSinkService a2dpSinkService = A2dpSinkService.getA2dpSinkService();
            synchronized (AvrcpControllerStateMachine.this.mLock) {
                int i = message.what;
                if (i == 1) {
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                    AvrcpControllerService.sendPassThroughCommandNative(Utils.getByteAddress(bluetoothDevice), message.arg1, message.arg2);
                    if (a2dpSinkService != null) {
                        Log.d(AvrcpControllerStateMachine.TAG, " inform AVRCP Commands to A2DP Sink ");
                        a2dpSinkService.informAvrcpPassThroughCmd(bluetoothDevice, message.arg1, message.arg2);
                    }
                } else if (i == 3) {
                    AvrcpControllerService.sendGroupNavigationCommandNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), message.arg1, message.arg2);
                } else if (i == AvrcpControllerStateMachine.MESSAGE_PROCESS_SET_ADDRESSED_PLAYER) {
                    AvrcpControllerService.getPlayerListNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), 0, 255);
                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mGetPlayerListing);
                    AvrcpControllerStateMachine.this.sendMessageDelayed(403, 5000L);
                } else if (i != AvrcpControllerStateMachine.MESSAGE_INTERNAL_ABS_VOL_TIMEOUT) {
                    switch (i) {
                        case 5:
                            AvrcpControllerStateMachine.this.mGetFolderList.setFolder((String) message.obj);
                            AvrcpControllerStateMachine.this.mGetFolderList.setBounds(message.arg1, message.arg2);
                            AvrcpControllerStateMachine.this.mGetFolderList.setScope(3);
                            AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mGetFolderList);
                            break;
                        case 6:
                            AvrcpControllerStateMachine.this.mGetFolderList.setBounds(message.arg1, message.arg2);
                            AvrcpControllerStateMachine.this.mGetFolderList.setFolder((String) message.obj);
                            AvrcpControllerStateMachine.this.mGetFolderList.setScope(1);
                            AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mGetFolderList);
                            break;
                        case 7:
                            AvrcpControllerService.getPlayerListNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), (byte) message.arg1, (byte) message.arg2);
                            AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mGetPlayerListing);
                            AvrcpControllerStateMachine.this.sendMessageDelayed(403, 5000L);
                            break;
                        case 8:
                            int i2 = message.arg1;
                            Bundle bundle = (Bundle) message.obj;
                            String string = bundle.getString(AvrcpControllerService.EXTRA_FOLDER_BT_ID);
                            String string2 = bundle.getString(AvrcpControllerService.EXTRA_FOLDER_ID);
                            AvrcpControllerService.changeFolderPathNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), (byte) message.arg1, AvrcpControllerService.hexStringToByteUID(string));
                            AvrcpControllerStateMachine.this.mChangeFolderPath.setFolder(string2);
                            AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mChangeFolderPath);
                            AvrcpControllerStateMachine.this.sendMessage(AvrcpControllerStateMachine.MESSAGE_INTERNAL_BROWSE_DEPTH_INCREMENT, (byte) message.arg1);
                            AvrcpControllerStateMachine.this.sendMessageDelayed(403, 5000L);
                            break;
                        case 9:
                            int i3 = message.arg1;
                            String str = (String) message.obj;
                            BrowseTree.BrowseNode currentBrowsedPlayer = AvrcpControllerStateMachine.this.mBrowseTree.getCurrentBrowsedPlayer();
                            BrowseTree.BrowseNode currentAddressedPlayer = AvrcpControllerStateMachine.this.mBrowseTree.getCurrentAddressedPlayer();
                            Log.d(AvrcpControllerStateMachine.TAG, "currBrPlayer " + currentBrowsedPlayer + " currAddrPlayer " + currentAddressedPlayer);
                            if (currentBrowsedPlayer == null || currentBrowsedPlayer.equals(currentAddressedPlayer)) {
                                AvrcpControllerService.playItemNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), (byte) i3, AvrcpControllerService.hexStringToByteUID(str), 0);
                            } else {
                                AvrcpControllerService.setAddressedPlayerNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), currentBrowsedPlayer.getPlayerID());
                                AvrcpControllerStateMachine.this.mSetAddrPlayer.setItemAndScope(currentBrowsedPlayer.getID(), str, i3);
                                AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mSetAddrPlayer);
                            }
                            break;
                        case 10:
                            AvrcpControllerService.setBrowsedPlayerNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), message.arg1);
                            AvrcpControllerStateMachine.this.mSetBrowsedPlayer.setFolder((String) message.obj);
                            AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mSetBrowsedPlayer);
                            break;
                        default:
                            switch (i) {
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_SET_ABS_VOL_CMD:
                                    AvrcpControllerStateMachine.access$1408(AvrcpControllerStateMachine.this);
                                    AvrcpControllerStateMachine.this.removeMessages(AvrcpControllerStateMachine.MESSAGE_INTERNAL_ABS_VOL_TIMEOUT);
                                    AvrcpControllerStateMachine.this.sendMessageDelayed(AvrcpControllerStateMachine.MESSAGE_INTERNAL_ABS_VOL_TIMEOUT, 1000L);
                                    AvrcpControllerStateMachine.this.setAbsVolume(message.arg1, message.arg2);
                                    break;
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION:
                                    AvrcpControllerStateMachine.this.mRemoteDevice.setNotificationLabel(message.arg1);
                                    AvrcpControllerStateMachine.this.mRemoteDevice.setAbsVolNotificationRequested(true);
                                    int volumePercentage = AvrcpControllerStateMachine.this.getVolumePercentage();
                                    Log.d(AvrcpControllerStateMachine.TAG, " Sending Interim Response = " + volumePercentage + " label " + message.arg1);
                                    AvrcpControllerService.sendRegisterAbsVolRspNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), (byte) 0, volumePercentage, AvrcpControllerStateMachine.this.mRemoteDevice.getNotificationLabel());
                                    break;
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_TRACK_CHANGED:
                                    AvrcpControllerStateMachine.this.mAddressedPlayer.updateCurrentTrack((TrackInfo) message.obj);
                                    AvrcpControllerStateMachine.this.broadcastMetaDataChanged(AvrcpControllerStateMachine.this.mAddressedPlayer.getCurrentTrack().getMediaMetaData());
                                    break;
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_POS_CHANGED:
                                    if (message.arg2 != -1) {
                                        AvrcpControllerStateMachine.this.mAddressedPlayer.setPlayTime(message.arg2);
                                        AvrcpControllerStateMachine.this.broadcastPlayBackStateChanged(AvrcpControllerStateMachine.this.getCurrentPlayBackState());
                                    }
                                    break;
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_STATUS_CHANGED:
                                    int i4 = message.arg1;
                                    AvrcpControllerStateMachine.this.mAddressedPlayer.setPlayStatus(i4);
                                    AvrcpControllerStateMachine.this.broadcastPlayBackStateChanged(AvrcpControllerStateMachine.this.getCurrentPlayBackState());
                                    if (i4 == 3) {
                                        a2dpSinkService.informTGStatePlaying(AvrcpControllerStateMachine.this.mRemoteDevice.mBTDevice, true);
                                    } else if (i4 == 2 || i4 == 1) {
                                        a2dpSinkService.informTGStatePlaying(AvrcpControllerStateMachine.this.mRemoteDevice.mBTDevice, false);
                                    }
                                    break;
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_VOLUME_CHANGED_NOTIFICATION:
                                    if (AvrcpControllerStateMachine.this.mVolumeChangedNotificationsToIgnore <= 0) {
                                        if (AvrcpControllerStateMachine.this.mRemoteDevice.getAbsVolNotificationRequested()) {
                                            int volumePercentage2 = AvrcpControllerStateMachine.this.getVolumePercentage();
                                            if (volumePercentage2 != AvrcpControllerStateMachine.this.mPreviousPercentageVol) {
                                                AvrcpControllerService.sendRegisterAbsVolRspNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), (byte) 1, volumePercentage2, AvrcpControllerStateMachine.this.mRemoteDevice.getNotificationLabel());
                                                AvrcpControllerStateMachine.this.mPreviousPercentageVol = volumePercentage2;
                                                AvrcpControllerStateMachine.this.mRemoteDevice.setAbsVolNotificationRequested(false);
                                            }
                                        }
                                    } else {
                                        AvrcpControllerStateMachine.access$1410(AvrcpControllerStateMachine.this);
                                        if (AvrcpControllerStateMachine.this.mVolumeChangedNotificationsToIgnore == 0) {
                                            AvrcpControllerStateMachine.this.removeMessages(AvrcpControllerStateMachine.MESSAGE_INTERNAL_ABS_VOL_TIMEOUT);
                                        }
                                    }
                                    break;
                                default:
                                    switch (i) {
                                        case 301:
                                            AvrcpControllerStateMachine.this.mRemoteDevice.setRemoteFeatures(message.arg1);
                                            break;
                                        case 302:
                                            if (message.arg1 == 0) {
                                                synchronized (AvrcpControllerStateMachine.this.mLock) {
                                                    AvrcpControllerStateMachine.this.mIsConnected = false;
                                                    AvrcpControllerStateMachine.this.mRemoteDevice = null;
                                                    break;
                                                }
                                                AvrcpControllerStateMachine.this.mBrowseTree.clear();
                                                AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mDisconnected);
                                                BluetoothDevice bluetoothDevice2 = (BluetoothDevice) message.obj;
                                                Intent intent = new Intent("android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED");
                                                intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", 2);
                                                intent.putExtra("android.bluetooth.profile.extra.STATE", 0);
                                                intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice2);
                                                AvrcpControllerStateMachine.this.mContext.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
                                            }
                                            break;
                                        case 303:
                                            Intent intent2 = new Intent(AvrcpControllerService.ACTION_BROWSE_CONNECTION_STATE_CHANGED);
                                            intent2.putExtra("android.bluetooth.device.extra.DEVICE", (BluetoothDevice) message.obj);
                                            Log.d(AvrcpControllerStateMachine.TAG, "Browse connection state " + message.arg1);
                                            if (message.arg1 == 1) {
                                                intent2.putExtra("android.bluetooth.profile.extra.STATE", 2);
                                            } else if (message.arg1 == 0) {
                                                intent2.putExtra("android.bluetooth.profile.extra.STATE", 0);
                                                AvrcpControllerStateMachine.this.mBrowseDepth = 0;
                                            } else {
                                                Log.w(AvrcpControllerStateMachine.TAG, "Incorrect browse state " + message.arg1);
                                            }
                                            AvrcpControllerStateMachine.this.mContext.sendBroadcast(intent2, ProfileService.BLUETOOTH_PERM);
                                            break;
                                        default:
                                            return false;
                                    }
                                    break;
                            }
                            break;
                    }
                } else {
                    Log.d(AvrcpControllerStateMachine.TAG, "Timed out on volume changed notification");
                    AvrcpControllerStateMachine.this.mVolumeChangedNotificationsToIgnore = 0;
                }
                return true;
            }
        }
    }

    class ChangeFolderPath extends CmdState {
        private static final String STATE_TAG = "AVRCPSM.ChangeFolderPath";
        private String mID;
        private int mTmpIncrDirection;

        ChangeFolderPath() {
            super();
            this.mID = "";
        }

        public void setFolder(String str) {
            this.mID = str;
        }

        @Override
        public void enter() {
            super.enter();
            this.mTmpIncrDirection = -1;
        }

        public boolean processMessage(Message message) {
            Log.d(STATE_TAG, "processMessage " + message.what);
            int i = message.what;
            if (i != 1 && i != 3) {
                if (i == AvrcpControllerStateMachine.MESSAGE_PROCESS_FOLDER_PATH) {
                    Log.d(STATE_TAG, "MESSAGE_PROCESS_FOLDER_PATH returned " + message.arg1 + " elements");
                    if (this.mTmpIncrDirection == 0) {
                        AvrcpControllerStateMachine.access$1320(AvrcpControllerStateMachine.this, 1);
                    } else if (this.mTmpIncrDirection == 1) {
                        AvrcpControllerStateMachine.access$1312(AvrcpControllerStateMachine.this, 1);
                    } else {
                        throw new IllegalStateException("incorrect nav " + this.mTmpIncrDirection);
                    }
                    Log.d(STATE_TAG, "New browse depth " + AvrcpControllerStateMachine.this.mBrowseDepth);
                    if (message.arg1 <= 0) {
                        AvrcpControllerStateMachine.this.broadcastFolderList(this.mID, AvrcpControllerStateMachine.EMPTY_MEDIA_ITEM_LIST);
                    } else {
                        AvrcpControllerStateMachine.this.sendMessage(6, 0, message.arg1 - 1, this.mID);
                    }
                    AvrcpControllerStateMachine.this.mBrowseTree.setCurrentBrowsedFolder(this.mID);
                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                } else if (i == AvrcpControllerStateMachine.MESSAGE_INTERNAL_BROWSE_DEPTH_INCREMENT) {
                    this.mTmpIncrDirection = message.arg1;
                } else if (i == 403) {
                    Log.e(STATE_TAG, "change folder failed, sending empty list.");
                    AvrcpControllerStateMachine.this.broadcastFolderList(this.mID, AvrcpControllerStateMachine.EMPTY_MEDIA_ITEM_LIST);
                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                } else {
                    switch (i) {
                        default:
                            switch (i) {
                                default:
                                    switch (i) {
                                        case 302:
                                        case 303:
                                            break;
                                        default:
                                            Log.d(STATE_TAG, "deferring message " + message.what + " to Connected state.");
                                            AvrcpControllerStateMachine.this.deferMessage(message);
                                            break;
                                    }
                                case AvrcpControllerStateMachine.MESSAGE_STOP_METADATA_BROADCASTS:
                                case AvrcpControllerStateMachine.MESSAGE_START_METADATA_BROADCASTS:
                                    return false;
                            }
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_SET_ABS_VOL_CMD:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_TRACK_CHANGED:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_POS_CHANGED:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_STATUS_CHANGED:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_VOLUME_CHANGED_NOTIFICATION:
                            break;
                    }
                }
                return true;
            }
            return false;
        }
    }

    class GetFolderList extends CmdState {
        private static final String STATE_TAG = "AVRCPSM.GetFolderList";
        int mCurrInd;
        int mEndInd;
        private ArrayList<MediaBrowser.MediaItem> mFolderList;
        String mID;
        int mScope;
        int mStartInd;

        GetFolderList() {
            super();
            this.mID = "";
            this.mFolderList = new ArrayList<>();
        }

        @Override
        public void enter() {
            super.enter();
            this.mCurrInd = 0;
            this.mFolderList.clear();
            callNativeFunctionForScope(this.mStartInd, Math.min(this.mEndInd, (this.mStartInd + 20) - 1));
        }

        public void setScope(int i) {
            this.mScope = i;
        }

        public void setFolder(String str) {
            Log.d(STATE_TAG, "Setting folder to " + str);
            this.mID = str;
        }

        public void setBounds(int i, int i2) {
            Log.d(STATE_TAG, "startInd " + i + " endInd " + i2);
            this.mStartInd = i;
            this.mEndInd = Math.min(i2, 1000);
        }

        public boolean processMessage(Message message) {
            Log.d(STATE_TAG, "processMessage " + message.what);
            int i = message.what;
            if (i != 1 && i != 3) {
                if (i == 5) {
                    this.mEndInd = 0;
                    AvrcpControllerStateMachine.this.deferMessage(message);
                } else if (i != 403) {
                    switch (i) {
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                            break;
                        default:
                            switch (i) {
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_SET_ABS_VOL_CMD:
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION:
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_TRACK_CHANGED:
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_POS_CHANGED:
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_STATUS_CHANGED:
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_VOLUME_CHANGED_NOTIFICATION:
                                    break;
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_GET_FOLDER_ITEMS:
                                    ArrayList arrayList = (ArrayList) message.obj;
                                    this.mFolderList.addAll(arrayList);
                                    Log.d(STATE_TAG, "Start " + this.mStartInd + " End " + this.mEndInd + " Curr " + this.mCurrInd + " received " + arrayList.size());
                                    this.mCurrInd = this.mCurrInd + arrayList.size();
                                    sendFolderBroadcastAndUpdateNode();
                                    if (this.mCurrInd > this.mEndInd || arrayList.size() == 0) {
                                        AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                                    } else {
                                        callNativeFunctionForScope(this.mCurrInd, Math.min(this.mEndInd, (this.mCurrInd + 20) - 1));
                                        AvrcpControllerStateMachine.this.removeMessages(403);
                                        AvrcpControllerStateMachine.this.sendMessageDelayed(403, 5000L);
                                    }
                                    break;
                                case AvrcpControllerStateMachine.MESSAGE_PROCESS_GET_FOLDER_ITEMS_OUT_OF_RANGE:
                                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                                    break;
                                default:
                                    switch (i) {
                                        default:
                                            switch (i) {
                                                case 302:
                                                case 303:
                                                    break;
                                                default:
                                                    Log.d(STATE_TAG, "deferring message " + message.what + " to connected!");
                                                    AvrcpControllerStateMachine.this.deferMessage(message);
                                                    break;
                                            }
                                        case AvrcpControllerStateMachine.MESSAGE_STOP_METADATA_BROADCASTS:
                                        case AvrcpControllerStateMachine.MESSAGE_START_METADATA_BROADCASTS:
                                            return false;
                                    }
                                    break;
                            }
                            break;
                    }
                } else {
                    sendFolderBroadcastAndUpdateNode();
                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                }
                return true;
            }
            return false;
        }

        private void sendFolderBroadcastAndUpdateNode() {
            BrowseTree.BrowseNode browseNodeFindBrowseNodeByID = AvrcpControllerStateMachine.this.mBrowseTree.findBrowseNodeByID(this.mID);
            if (browseNodeFindBrowseNodeByID == null) {
                Log.e(AvrcpControllerStateMachine.TAG, "Can not find BrowseNode by ID: " + this.mID);
                return;
            }
            if (browseNodeFindBrowseNodeByID.isPlayer()) {
                MediaDescription.Builder builder = new MediaDescription.Builder();
                builder.setMediaId("NOW_PLAYING:" + browseNodeFindBrowseNodeByID.getPlayerID());
                builder.setTitle(BrowseTree.NOW_PLAYING_PREFIX);
                Bundle bundle = new Bundle();
                bundle.putString(AvrcpControllerService.MEDIA_ITEM_UID_KEY, "NOW_PLAYING:" + browseNodeFindBrowseNodeByID.getID());
                builder.setExtras(bundle);
                this.mFolderList.add(new MediaBrowser.MediaItem(builder.build(), 1));
            }
            AvrcpControllerStateMachine.this.mBrowseTree.refreshChildren(browseNodeFindBrowseNodeByID, this.mFolderList);
            AvrcpControllerStateMachine.this.broadcastFolderList(this.mID, this.mFolderList);
            if (this.mScope == 3) {
                AvrcpControllerStateMachine.this.mBrowseTree.setCurrentBrowsedFolder(this.mID);
            }
        }

        private void callNativeFunctionForScope(int i, int i2) {
            int i3 = this.mScope;
            if (i3 == 1) {
                AvrcpControllerService.getFolderListNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), i, i2);
                return;
            }
            if (i3 == 3) {
                AvrcpControllerService.getNowPlayingListNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), i, i2);
                return;
            }
            Log.e(STATE_TAG, "Scope " + this.mScope + " cannot be handled here.");
        }
    }

    class GetPlayerListing extends CmdState {
        private static final String STATE_TAG = "AVRCPSM.GetPlayerList";

        GetPlayerListing() {
            super();
        }

        public boolean processMessage(Message message) {
            Log.d(STATE_TAG, "processMessage " + message.what);
            int i = message.what;
            if (i == 1 || i == 3) {
                return false;
            }
            if (i == AvrcpControllerStateMachine.MESSAGE_PROCESS_GET_PLAYER_ITEMS) {
                AvrcpControllerStateMachine.this.mBrowseTree.refreshChildren(BrowseTree.ROOT, (List) message.obj);
                ArrayList arrayList = new ArrayList();
                Iterator<BrowseTree.BrowseNode> it = AvrcpControllerStateMachine.this.mBrowseTree.findBrowseNodeByID(BrowseTree.ROOT).getChildren().iterator();
                while (it.hasNext()) {
                    arrayList.add(it.next().getMediaItem());
                }
                AvrcpControllerStateMachine.this.broadcastFolderList(BrowseTree.ROOT, arrayList);
                AvrcpControllerStateMachine.this.mBrowseTree.setCurrentBrowsedFolder(BrowseTree.ROOT);
                AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
            } else if (i == 403) {
                AvrcpControllerStateMachine.this.broadcastFolderList(BrowseTree.ROOT, AvrcpControllerStateMachine.EMPTY_MEDIA_ITEM_LIST);
                AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
            } else {
                switch (i) {
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_SET_ABS_VOL_CMD:
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION:
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_TRACK_CHANGED:
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_POS_CHANGED:
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_STATUS_CHANGED:
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_VOLUME_CHANGED_NOTIFICATION:
                        return false;
                    default:
                        switch (i) {
                            case AvrcpControllerStateMachine.MESSAGE_STOP_METADATA_BROADCASTS:
                            case AvrcpControllerStateMachine.MESSAGE_START_METADATA_BROADCASTS:
                                return false;
                            default:
                                switch (i) {
                                    case 302:
                                    case 303:
                                        return false;
                                    default:
                                        Log.d(STATE_TAG, "deferring message " + message.what + " to connected!");
                                        AvrcpControllerStateMachine.this.deferMessage(message);
                                        break;
                                }
                                break;
                        }
                        break;
                }
            }
            return true;
        }
    }

    class MoveToRoot extends CmdState {
        private static final String STATE_TAG = "AVRCPSM.MoveToRoot";
        private String mID;

        MoveToRoot() {
            super();
            this.mID = "";
        }

        public void setFolder(String str) {
            Log.d(STATE_TAG, "setFolder " + str);
            this.mID = str;
        }

        @Override
        public void enter() {
            super.enter();
            AvrcpControllerStateMachine.this.sendMessage(AvrcpControllerStateMachine.MESSAGE_INTERNAL_MOVE_N_LEVELS_UP);
        }

        public boolean processMessage(Message message) {
            Log.d(STATE_TAG, "processMessage " + message.what + " browse depth " + AvrcpControllerStateMachine.this.mBrowseDepth);
            int i = message.what;
            if (i != 1 && i != 3) {
                if (i != AvrcpControllerStateMachine.MESSAGE_PROCESS_FOLDER_PATH) {
                    switch (i) {
                        default:
                            switch (i) {
                                default:
                                    switch (i) {
                                        case 302:
                                        case 303:
                                            break;
                                        default:
                                            switch (i) {
                                                case AvrcpControllerStateMachine.MESSAGE_INTERNAL_MOVE_N_LEVELS_UP:
                                                    if (AvrcpControllerStateMachine.this.mBrowseDepth == 0) {
                                                        Log.w(STATE_TAG, "Already in root!");
                                                        AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                                                        AvrcpControllerStateMachine.this.sendMessage(6, 0, 255, this.mID);
                                                    } else {
                                                        AvrcpControllerService.changeFolderPathNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), (byte) 0, AvrcpControllerService.hexStringToByteUID(null));
                                                    }
                                                    break;
                                                case 403:
                                                    AvrcpControllerStateMachine.this.broadcastFolderList(BrowseTree.ROOT, AvrcpControllerStateMachine.EMPTY_MEDIA_ITEM_LIST);
                                                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                                                    break;
                                                default:
                                                    Log.d(STATE_TAG, "deferring message " + message.what + " to connected!");
                                                    AvrcpControllerStateMachine.this.deferMessage(message);
                                                    break;
                                            }
                                            break;
                                    }
                                case AvrcpControllerStateMachine.MESSAGE_STOP_METADATA_BROADCASTS:
                                case AvrcpControllerStateMachine.MESSAGE_START_METADATA_BROADCASTS:
                                    return false;
                            }
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_SET_ABS_VOL_CMD:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_TRACK_CHANGED:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_POS_CHANGED:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_STATUS_CHANGED:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_VOLUME_CHANGED_NOTIFICATION:
                            break;
                    }
                } else {
                    AvrcpControllerStateMachine.access$1320(AvrcpControllerStateMachine.this, 1);
                    Log.d(STATE_TAG, "New browse depth " + AvrcpControllerStateMachine.this.mBrowseDepth);
                    if (AvrcpControllerStateMachine.this.mBrowseDepth < 0) {
                        throw new IllegalArgumentException("Browse depth negative!");
                    }
                    AvrcpControllerStateMachine.this.sendMessage(AvrcpControllerStateMachine.MESSAGE_INTERNAL_MOVE_N_LEVELS_UP);
                }
                return true;
            }
            return false;
        }
    }

    class SetBrowsedPlayer extends CmdState {
        private static final String STATE_TAG = "AVRCPSM.SetBrowsedPlayer";
        String mID;

        SetBrowsedPlayer() {
            super();
            this.mID = "";
        }

        public void setFolder(String str) {
            this.mID = str;
        }

        public boolean processMessage(Message message) {
            Log.d(STATE_TAG, "processMessage " + message.what);
            int i = message.what;
            if (i == 1 || i == 3) {
                return false;
            }
            if (i == AvrcpControllerStateMachine.MESSAGE_PROCESS_SET_BROWSED_PLAYER) {
                Log.d(STATE_TAG, "player depth " + message.arg2);
                AvrcpControllerStateMachine.this.mBrowseDepth = message.arg2;
                if (AvrcpControllerStateMachine.this.mBrowseDepth != 0 || message.arg1 != 0) {
                    AvrcpControllerStateMachine.this.mMoveToRoot.setFolder(this.mID);
                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mMoveToRoot);
                } else {
                    AvrcpControllerStateMachine.this.broadcastFolderList(this.mID, AvrcpControllerStateMachine.EMPTY_MEDIA_ITEM_LIST);
                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                }
                AvrcpControllerStateMachine.this.mBrowseTree.setCurrentBrowsedFolder(this.mID);
                AvrcpControllerStateMachine.this.mBrowseTree.setCurrentBrowsedPlayer(this.mID);
            } else if (i == 403) {
                AvrcpControllerStateMachine.this.broadcastFolderList(this.mID, AvrcpControllerStateMachine.EMPTY_MEDIA_ITEM_LIST);
                AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
            } else {
                switch (i) {
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_SET_ABS_VOL_CMD:
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION:
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_TRACK_CHANGED:
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_POS_CHANGED:
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_STATUS_CHANGED:
                    case AvrcpControllerStateMachine.MESSAGE_PROCESS_VOLUME_CHANGED_NOTIFICATION:
                        return false;
                    default:
                        switch (i) {
                            case AvrcpControllerStateMachine.MESSAGE_STOP_METADATA_BROADCASTS:
                            case AvrcpControllerStateMachine.MESSAGE_START_METADATA_BROADCASTS:
                                return false;
                            default:
                                switch (i) {
                                    case 302:
                                    case 303:
                                        return false;
                                    default:
                                        Log.d(STATE_TAG, "deferring message " + message.what + " to connected!");
                                        AvrcpControllerStateMachine.this.deferMessage(message);
                                        break;
                                }
                                break;
                        }
                        break;
                }
            }
            return true;
        }
    }

    class SetAddresedPlayerAndPlayItem extends CmdState {
        private static final String STATE_TAG = "AVRCPSM.SetAddresedPlayerAndPlayItem";
        String mAddrPlayerId;
        String mPlayItemId;
        int mScope;

        SetAddresedPlayerAndPlayItem() {
            super();
        }

        public void setItemAndScope(String str, String str2, int i) {
            this.mAddrPlayerId = str;
            this.mPlayItemId = str2;
            this.mScope = i;
        }

        public boolean processMessage(Message message) {
            Log.d(STATE_TAG, "processMessage " + message.what);
            int i = message.what;
            if (i != 1 && i != 3) {
                if (i == AvrcpControllerStateMachine.MESSAGE_PROCESS_SET_ADDRESSED_PLAYER) {
                    AvrcpControllerStateMachine.this.mBrowseTree.setCurrentAddressedPlayer(this.mAddrPlayerId);
                    AvrcpControllerService.playItemNative(AvrcpControllerStateMachine.this.mRemoteDevice.getBluetoothAddress(), (byte) this.mScope, AvrcpControllerService.hexStringToByteUID(this.mPlayItemId), 0);
                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                } else if (i == 403) {
                    AvrcpControllerStateMachine.this.transitionTo(AvrcpControllerStateMachine.this.mConnected);
                } else {
                    switch (i) {
                        default:
                            switch (i) {
                                default:
                                    switch (i) {
                                        case 302:
                                        case 303:
                                            break;
                                        default:
                                            Log.d(STATE_TAG, "deferring message " + message.what + " to connected!");
                                            AvrcpControllerStateMachine.this.deferMessage(message);
                                            break;
                                    }
                                case AvrcpControllerStateMachine.MESSAGE_STOP_METADATA_BROADCASTS:
                                case AvrcpControllerStateMachine.MESSAGE_START_METADATA_BROADCASTS:
                                    return false;
                            }
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_SET_ABS_VOL_CMD:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_TRACK_CHANGED:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_POS_CHANGED:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_PLAY_STATUS_CHANGED:
                        case AvrcpControllerStateMachine.MESSAGE_PROCESS_VOLUME_CHANGED_NOTIFICATION:
                            break;
                    }
                }
                return true;
            }
            return false;
        }
    }

    abstract class CmdState extends State {
        CmdState() {
        }

        public void enter() {
            AvrcpControllerStateMachine.this.sendMessageDelayed(403, 5000L);
        }

        public void exit() {
            AvrcpControllerStateMachine.this.removeMessages(403);
        }
    }

    boolean isConnected() {
        boolean zBooleanValue;
        synchronized (this.mLock) {
            zBooleanValue = this.mIsConnected.booleanValue();
        }
        return zBooleanValue;
    }

    void doQuit() {
        try {
            this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        } catch (IllegalArgumentException e) {
        }
        quit();
    }

    void dump(StringBuilder sb) {
        ProfileService.println(sb, "StateMachine: " + toString());
    }

    MediaMetadata getCurrentMetaData() {
        MediaMetadata mediaMetadata;
        synchronized (this.mLock) {
            if (this.mAddressedPlayer != null && this.mAddressedPlayer.getCurrentTrack() != null) {
                Log.d(TAG, "getCurrentMetaData mmd " + this.mAddressedPlayer.getCurrentTrack().getMediaMetaData());
            }
            mediaMetadata = EMPTY_MEDIA_METADATA;
        }
        return mediaMetadata;
    }

    PlaybackState getCurrentPlayBackState() {
        return getCurrentPlayBackState(true);
    }

    PlaybackState getCurrentPlayBackState(boolean z) {
        if (z) {
            synchronized (this.mLock) {
                if (this.mAddressedPlayer == null) {
                    return new PlaybackState.Builder().setState(7, -1L, 0.0f).build();
                }
                return this.mAddressedPlayer.getPlaybackState();
            }
        }
        AvrcpControllerService.getPlaybackStateNative(this.mRemoteDevice.getBluetoothAddress());
        return null;
    }

    void getChildren(String str, int i, int i2) {
        Message messageObtainMessage;
        BrowseTree.BrowseNode browseNodeFindBrowseNodeByID = this.mBrowseTree.findBrowseNodeByID(str);
        if (browseNodeFindBrowseNodeByID == null) {
            Log.e(TAG, "Invalid folder to browse " + this.mBrowseTree);
            broadcastFolderList(str, EMPTY_MEDIA_ITEM_LIST);
            return;
        }
        Log.d(TAG, "To Browse folder " + browseNodeFindBrowseNodeByID + " is cached " + browseNodeFindBrowseNodeByID.isCached() + " current folder " + this.mBrowseTree.getCurrentBrowsedFolder());
        if (browseNodeFindBrowseNodeByID.equals(this.mBrowseTree.getCurrentBrowsedFolder()) && browseNodeFindBrowseNodeByID.isCached()) {
            Log.d(TAG, "Same cached folder -- returning existing children.");
            BrowseTree.BrowseNode browseNodeFindBrowseNodeByID2 = this.mBrowseTree.findBrowseNodeByID(str);
            ArrayList<MediaBrowser.MediaItem> arrayList = new ArrayList<>();
            Iterator<BrowseTree.BrowseNode> it = browseNodeFindBrowseNodeByID2.getChildren().iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().getMediaItem());
            }
            broadcastFolderList(str, arrayList);
            return;
        }
        int direction = this.mBrowseTree.getDirection(str);
        BrowseTree.BrowseNode currentBrowsedFolder = this.mBrowseTree.getCurrentBrowsedFolder();
        Log.d(TAG, "Browse direction parent " + this.mBrowseTree.getCurrentBrowsedFolder() + " req " + str + " direction " + direction);
        if (BrowseTree.ROOT.equals(str)) {
            messageObtainMessage = obtainMessage(7, i, i2);
        } else if (browseNodeFindBrowseNodeByID.isPlayer() && direction != 2) {
            messageObtainMessage = obtainMessage(10, browseNodeFindBrowseNodeByID.getPlayerID(), 0, browseNodeFindBrowseNodeByID.getID());
        } else if (browseNodeFindBrowseNodeByID.isNowPlaying()) {
            messageObtainMessage = obtainMessage(5, i, i2, str);
        } else {
            if (!(currentBrowsedFolder.isNowPlaying() && browseNodeFindBrowseNodeByID.getID().equals(BrowseTree.ROOT))) {
                Log.d(TAG, "Browse direction " + currentBrowsedFolder + " " + browseNodeFindBrowseNodeByID + " = " + direction);
                int i3 = -1;
                if (direction == -1) {
                    Log.w(TAG, "parent " + browseNodeFindBrowseNodeByID + " is not a direct successor or predeccessor of current folder " + currentBrowsedFolder);
                    broadcastFolderList(str, EMPTY_MEDIA_ITEM_LIST);
                    return;
                }
                if (direction != 0) {
                    if (direction == 1) {
                        i3 = 0;
                    }
                } else {
                    i3 = 1;
                }
                Bundle bundle = new Bundle();
                bundle.putString(AvrcpControllerService.EXTRA_FOLDER_ID, browseNodeFindBrowseNodeByID.getID());
                bundle.putString(AvrcpControllerService.EXTRA_FOLDER_BT_ID, browseNodeFindBrowseNodeByID.getFolderUID());
                messageObtainMessage = obtainMessage(8, i3, 0, bundle);
            } else {
                messageObtainMessage = obtainMessage(6, i, i2, browseNodeFindBrowseNodeByID.getFolderUID());
            }
        }
        if (messageObtainMessage != null) {
            sendMessage(messageObtainMessage);
        }
    }

    public void fetchAttrAndPlayItem(String str) {
        BrowseTree.BrowseNode browseNodeFindFolderByIDLocked = this.mBrowseTree.findFolderByIDLocked(str);
        BrowseTree.BrowseNode currentBrowsedFolder = this.mBrowseTree.getCurrentBrowsedFolder();
        Log.d(TAG, "fetchAttrAndPlayItem mediaId=" + str + " node=" + browseNodeFindFolderByIDLocked);
        if (browseNodeFindFolderByIDLocked != null) {
            sendMessage(obtainMessage(9, currentBrowsedFolder.isNowPlaying() ? 3 : 1, 0, browseNodeFindFolderByIDLocked.getFolderUID()));
        }
    }

    private void broadcastMetaDataChanged(MediaMetadata mediaMetadata) {
        Intent intent = new Intent(AvrcpControllerService.ACTION_TRACK_EVENT);
        intent.putExtra(AvrcpControllerService.EXTRA_METADATA, mediaMetadata);
        this.mContext.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void broadcastFolderList(String str, ArrayList<MediaBrowser.MediaItem> arrayList) {
        Intent intent = new Intent(AvrcpControllerService.ACTION_FOLDER_LIST);
        intent.putExtra(AvrcpControllerService.EXTRA_FOLDER_ID, str);
        intent.putParcelableArrayListExtra(AvrcpControllerService.EXTRA_FOLDER_LIST, arrayList);
        this.mContext.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void broadcastPlayBackStateChanged(PlaybackState playbackState) {
        Intent intent = new Intent(AvrcpControllerService.ACTION_TRACK_EVENT);
        intent.putExtra(AvrcpControllerService.EXTRA_PLAYBACK, playbackState);
        Log.d(TAG, " broadcastPlayBackStateChanged = " + playbackState.toString());
        this.mContext.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void setAbsVolume(int i, int i2) {
        int streamMaxVolume = this.mAudioManager.getStreamMaxVolume(3);
        int streamVolume = this.mAudioManager.getStreamVolume(3);
        if (this.mRemoteDevice.getFirstAbsVolCmdRecvd()) {
            int i3 = (streamMaxVolume * i) / 127;
            Log.d(TAG, " setAbsVolume =" + i + " maxVol = " + streamMaxVolume + " cur = " + streamVolume + " new = " + i3);
            if (i3 != streamVolume) {
                this.mAudioManager.setStreamVolume(3, i3, 1);
            }
        } else {
            this.mRemoteDevice.setFirstAbsVolCmdRecvd();
            i = (streamVolume * 127) / streamMaxVolume;
            Log.d(TAG, " SetAbsVol recvd for first time, respond with " + i);
        }
        AvrcpControllerService.sendAbsVolRspNative(this.mRemoteDevice.getBluetoothAddress(), i, i2);
    }

    private int getVolumePercentage() {
        return (this.mAudioManager.getStreamVolume(3) * 127) / this.mAudioManager.getStreamMaxVolume(3);
    }

    public static String dumpMessageString(int i) {
        if (i == 1) {
            return "REQ_PASS_THROUGH_CMD";
        }
        if (i == 3) {
            return "REQ_GRP_NAV_CMD";
        }
        switch (i) {
            case MESSAGE_PROCESS_SET_ABS_VOL_CMD:
                return "CB_SET_ABS_VOL_CMD";
            case MESSAGE_PROCESS_REGISTER_ABS_VOL_NOTIFICATION:
                return "CB_REGISTER_ABS_VOL";
            case MESSAGE_PROCESS_TRACK_CHANGED:
                return "CB_TRACK_CHANGED";
            case MESSAGE_PROCESS_PLAY_POS_CHANGED:
                return "CB_PLAY_POS_CHANGED";
            case MESSAGE_PROCESS_PLAY_STATUS_CHANGED:
                return "CB_PLAY_STATUS_CHANGED";
            default:
                switch (i) {
                    case 301:
                        return "CB_RC_FEATURES";
                    case 302:
                        return "CB_CONN_CHANGED";
                    default:
                        return Integer.toString(i);
                }
        }
    }

    public static String displayBluetoothAvrcpSettings(BluetoothAvrcpPlayerSettings bluetoothAvrcpPlayerSettings) {
        StringBuffer stringBuffer = new StringBuffer();
        int settings = bluetoothAvrcpPlayerSettings.getSettings();
        if ((settings & 1) != 0) {
            stringBuffer.append(" EQ : ");
            stringBuffer.append(Integer.toString(bluetoothAvrcpPlayerSettings.getSettingValue(1)));
        }
        if ((settings & 2) != 0) {
            stringBuffer.append(" REPEAT : ");
            stringBuffer.append(Integer.toString(bluetoothAvrcpPlayerSettings.getSettingValue(2)));
        }
        if ((settings & 4) != 0) {
            stringBuffer.append(" SHUFFLE : ");
            stringBuffer.append(Integer.toString(bluetoothAvrcpPlayerSettings.getSettingValue(4)));
        }
        if ((settings & 8) != 0) {
            stringBuffer.append(" SCAN : ");
            stringBuffer.append(Integer.toString(bluetoothAvrcpPlayerSettings.getSettingValue(8)));
        }
        return stringBuffer.toString();
    }
}

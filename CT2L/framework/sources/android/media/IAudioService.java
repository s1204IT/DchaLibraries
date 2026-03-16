package android.media;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.media.IAudioFocusDispatcher;
import android.media.IAudioRoutesObserver;
import android.media.IRemoteControlDisplay;
import android.media.IRingtonePlayer;
import android.media.IVolumeController;
import android.media.audiopolicy.AudioPolicyConfig;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IAudioService extends IInterface {
    int abandonAudioFocus(IAudioFocusDispatcher iAudioFocusDispatcher, String str, AudioAttributes audioAttributes) throws RemoteException;

    void adjustMasterVolume(int i, int i2, String str) throws RemoteException;

    void adjustStreamVolume(int i, int i2, int i3, String str) throws RemoteException;

    void adjustSuggestedStreamVolume(int i, int i2, int i3, String str) throws RemoteException;

    void avrcpSupportsAbsoluteVolume(String str, boolean z) throws RemoteException;

    void disableSafeMediaVolume() throws RemoteException;

    void forceRemoteSubmixFullVolume(boolean z, IBinder iBinder) throws RemoteException;

    void forceVolumeControlStream(int i, IBinder iBinder) throws RemoteException;

    int getCurrentAudioFocus() throws RemoteException;

    int getLastAudibleMasterVolume() throws RemoteException;

    int getLastAudibleStreamVolume(int i) throws RemoteException;

    int getMasterMaxVolume() throws RemoteException;

    int getMasterStreamType() throws RemoteException;

    int getMasterVolume() throws RemoteException;

    int getMode() throws RemoteException;

    int getRingerModeExternal() throws RemoteException;

    int getRingerModeInternal() throws RemoteException;

    IRingtonePlayer getRingtonePlayer() throws RemoteException;

    int getStreamMaxVolume(int i) throws RemoteException;

    int getStreamVolume(int i) throws RemoteException;

    int getVibrateSetting(int i) throws RemoteException;

    boolean isBluetoothA2dpOn() throws RemoteException;

    boolean isBluetoothScoOn() throws RemoteException;

    boolean isCameraSoundForced() throws RemoteException;

    boolean isHdmiSystemAudioSupported() throws RemoteException;

    boolean isMasterMute() throws RemoteException;

    boolean isSpeakerphoneOn() throws RemoteException;

    boolean isStreamAffectedByRingerMode(int i) throws RemoteException;

    boolean isStreamMute(int i) throws RemoteException;

    boolean isValidRingerMode(int i) throws RemoteException;

    boolean loadSoundEffects() throws RemoteException;

    void notifyVolumeControllerVisible(IVolumeController iVolumeController, boolean z) throws RemoteException;

    void playSoundEffect(int i) throws RemoteException;

    void playSoundEffectVolume(int i, float f) throws RemoteException;

    String registerAudioPolicy(AudioPolicyConfig audioPolicyConfig, IAudioPolicyCallback iAudioPolicyCallback, boolean z) throws RemoteException;

    boolean registerRemoteControlDisplay(IRemoteControlDisplay iRemoteControlDisplay, int i, int i2) throws RemoteException;

    boolean registerRemoteController(IRemoteControlDisplay iRemoteControlDisplay, int i, int i2, ComponentName componentName) throws RemoteException;

    void reloadAudioSettings() throws RemoteException;

    void remoteControlDisplayUsesBitmapSize(IRemoteControlDisplay iRemoteControlDisplay, int i, int i2) throws RemoteException;

    void remoteControlDisplayWantsPlaybackPositionSync(IRemoteControlDisplay iRemoteControlDisplay, boolean z) throws RemoteException;

    int requestAudioFocus(AudioAttributes audioAttributes, int i, IBinder iBinder, IAudioFocusDispatcher iAudioFocusDispatcher, String str, String str2, int i2, IAudioPolicyCallback iAudioPolicyCallback) throws RemoteException;

    int setBluetoothA2dpDeviceConnectionState(BluetoothDevice bluetoothDevice, int i, int i2) throws RemoteException;

    void setBluetoothA2dpOn(boolean z) throws RemoteException;

    void setBluetoothScoOn(boolean z) throws RemoteException;

    int setFocusPropertiesForPolicy(int i, IAudioPolicyCallback iAudioPolicyCallback) throws RemoteException;

    int setHdmiSystemAudioSupported(boolean z) throws RemoteException;

    void setMasterMute(boolean z, int i, String str, IBinder iBinder) throws RemoteException;

    void setMasterVolume(int i, int i2, String str) throws RemoteException;

    void setMicrophoneMute(boolean z, String str) throws RemoteException;

    void setMode(int i, IBinder iBinder) throws RemoteException;

    void setRemoteStreamVolume(int i) throws RemoteException;

    void setRingerModeExternal(int i, String str) throws RemoteException;

    void setRingerModeInternal(int i, String str) throws RemoteException;

    void setRingtonePlayer(IRingtonePlayer iRingtonePlayer) throws RemoteException;

    void setSpeakerphoneOn(boolean z) throws RemoteException;

    void setStreamMute(int i, boolean z, IBinder iBinder) throws RemoteException;

    void setStreamSolo(int i, boolean z, IBinder iBinder) throws RemoteException;

    void setStreamVolume(int i, int i2, int i3, String str) throws RemoteException;

    void setVibrateSetting(int i, int i2) throws RemoteException;

    void setVolumeController(IVolumeController iVolumeController) throws RemoteException;

    void setWiredDeviceConnectionState(int i, int i2, String str) throws RemoteException;

    boolean shouldVibrate(int i) throws RemoteException;

    void startBluetoothSco(IBinder iBinder, int i) throws RemoteException;

    void startBluetoothScoVirtualCall(IBinder iBinder) throws RemoteException;

    AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver iAudioRoutesObserver) throws RemoteException;

    void stopBluetoothSco(IBinder iBinder) throws RemoteException;

    void unloadSoundEffects() throws RemoteException;

    void unregisterAudioFocusClient(String str) throws RemoteException;

    void unregisterAudioPolicyAsync(IAudioPolicyCallback iAudioPolicyCallback) throws RemoteException;

    void unregisterRemoteControlDisplay(IRemoteControlDisplay iRemoteControlDisplay) throws RemoteException;

    public static abstract class Stub extends Binder implements IAudioService {
        private static final String DESCRIPTOR = "android.media.IAudioService";
        static final int TRANSACTION_abandonAudioFocus = 43;
        static final int TRANSACTION_adjustMasterVolume = 3;
        static final int TRANSACTION_adjustStreamVolume = 2;
        static final int TRANSACTION_adjustSuggestedStreamVolume = 1;
        static final int TRANSACTION_avrcpSupportsAbsoluteVolume = 35;
        static final int TRANSACTION_disableSafeMediaVolume = 65;
        static final int TRANSACTION_forceRemoteSubmixFullVolume = 10;
        static final int TRANSACTION_forceVolumeControlStream = 54;
        static final int TRANSACTION_getCurrentAudioFocus = 45;
        static final int TRANSACTION_getLastAudibleMasterVolume = 18;
        static final int TRANSACTION_getLastAudibleStreamVolume = 17;
        static final int TRANSACTION_getMasterMaxVolume = 16;
        static final int TRANSACTION_getMasterStreamType = 57;
        static final int TRANSACTION_getMasterVolume = 14;
        static final int TRANSACTION_getMode = 29;
        static final int TRANSACTION_getRingerModeExternal = 22;
        static final int TRANSACTION_getRingerModeInternal = 23;
        static final int TRANSACTION_getRingtonePlayer = 56;
        static final int TRANSACTION_getStreamMaxVolume = 15;
        static final int TRANSACTION_getStreamVolume = 13;
        static final int TRANSACTION_getVibrateSetting = 26;
        static final int TRANSACTION_isBluetoothA2dpOn = 41;
        static final int TRANSACTION_isBluetoothScoOn = 39;
        static final int TRANSACTION_isCameraSoundForced = 61;
        static final int TRANSACTION_isHdmiSystemAudioSupported = 67;
        static final int TRANSACTION_isMasterMute = 12;
        static final int TRANSACTION_isSpeakerphoneOn = 37;
        static final int TRANSACTION_isStreamAffectedByRingerMode = 64;
        static final int TRANSACTION_isStreamMute = 9;
        static final int TRANSACTION_isValidRingerMode = 24;
        static final int TRANSACTION_loadSoundEffects = 32;
        static final int TRANSACTION_notifyVolumeControllerVisible = 63;
        static final int TRANSACTION_playSoundEffect = 30;
        static final int TRANSACTION_playSoundEffectVolume = 31;
        static final int TRANSACTION_registerAudioPolicy = 68;
        static final int TRANSACTION_registerRemoteControlDisplay = 46;
        static final int TRANSACTION_registerRemoteController = 47;
        static final int TRANSACTION_reloadAudioSettings = 34;
        static final int TRANSACTION_remoteControlDisplayUsesBitmapSize = 49;
        static final int TRANSACTION_remoteControlDisplayWantsPlaybackPositionSync = 50;
        static final int TRANSACTION_requestAudioFocus = 42;
        static final int TRANSACTION_setBluetoothA2dpDeviceConnectionState = 59;
        static final int TRANSACTION_setBluetoothA2dpOn = 40;
        static final int TRANSACTION_setBluetoothScoOn = 38;
        static final int TRANSACTION_setFocusPropertiesForPolicy = 70;
        static final int TRANSACTION_setHdmiSystemAudioSupported = 66;
        static final int TRANSACTION_setMasterMute = 11;
        static final int TRANSACTION_setMasterVolume = 6;
        static final int TRANSACTION_setMicrophoneMute = 19;
        static final int TRANSACTION_setMode = 28;
        static final int TRANSACTION_setRemoteStreamVolume = 5;
        static final int TRANSACTION_setRingerModeExternal = 20;
        static final int TRANSACTION_setRingerModeInternal = 21;
        static final int TRANSACTION_setRingtonePlayer = 55;
        static final int TRANSACTION_setSpeakerphoneOn = 36;
        static final int TRANSACTION_setStreamMute = 8;
        static final int TRANSACTION_setStreamSolo = 7;
        static final int TRANSACTION_setStreamVolume = 4;
        static final int TRANSACTION_setVibrateSetting = 25;
        static final int TRANSACTION_setVolumeController = 62;
        static final int TRANSACTION_setWiredDeviceConnectionState = 58;
        static final int TRANSACTION_shouldVibrate = 27;
        static final int TRANSACTION_startBluetoothSco = 51;
        static final int TRANSACTION_startBluetoothScoVirtualCall = 52;
        static final int TRANSACTION_startWatchingRoutes = 60;
        static final int TRANSACTION_stopBluetoothSco = 53;
        static final int TRANSACTION_unloadSoundEffects = 33;
        static final int TRANSACTION_unregisterAudioFocusClient = 44;
        static final int TRANSACTION_unregisterAudioPolicyAsync = 69;
        static final int TRANSACTION_unregisterRemoteControlDisplay = 48;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAudioService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAudioService)) {
                return (IAudioService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            AudioPolicyConfig _arg0;
            BluetoothDevice _arg02;
            ComponentName _arg3;
            AudioAttributes _arg2;
            AudioAttributes _arg03;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    int _arg1 = data.readInt();
                    int _arg22 = data.readInt();
                    String _arg32 = data.readString();
                    adjustSuggestedStreamVolume(_arg04, _arg1, _arg22, _arg32);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    int _arg12 = data.readInt();
                    int _arg23 = data.readInt();
                    String _arg33 = data.readString();
                    adjustStreamVolume(_arg05, _arg12, _arg23, _arg33);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    int _arg13 = data.readInt();
                    String _arg24 = data.readString();
                    adjustMasterVolume(_arg06, _arg13, _arg24);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    int _arg14 = data.readInt();
                    int _arg25 = data.readInt();
                    String _arg34 = data.readString();
                    setStreamVolume(_arg07, _arg14, _arg25, _arg34);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    setRemoteStreamVolume(_arg08);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    int _arg15 = data.readInt();
                    String _arg26 = data.readString();
                    setMasterVolume(_arg09, _arg15, _arg26);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    boolean _arg16 = data.readInt() != 0;
                    IBinder _arg27 = data.readStrongBinder();
                    setStreamSolo(_arg010, _arg16, _arg27);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    boolean _arg17 = data.readInt() != 0;
                    IBinder _arg28 = data.readStrongBinder();
                    setStreamMute(_arg011, _arg17, _arg28);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg012 = data.readInt();
                    boolean _result = isStreamMute(_arg012);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg013 = data.readInt() != 0;
                    IBinder _arg18 = data.readStrongBinder();
                    forceRemoteSubmixFullVolume(_arg013, _arg18);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg014 = data.readInt() != 0;
                    int _arg19 = data.readInt();
                    String _arg29 = data.readString();
                    IBinder _arg35 = data.readStrongBinder();
                    setMasterMute(_arg014, _arg19, _arg29, _arg35);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result2 = isMasterMute();
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg015 = data.readInt();
                    int _result3 = getStreamVolume(_arg015);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    int _result4 = getMasterVolume();
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg016 = data.readInt();
                    int _result5 = getStreamMaxVolume(_arg016);
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _result6 = getMasterMaxVolume();
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    int _result7 = getLastAudibleStreamVolume(_arg017);
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _result8 = getLastAudibleMasterVolume();
                    reply.writeNoException();
                    reply.writeInt(_result8);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg018 = data.readInt() != 0;
                    String _arg110 = data.readString();
                    setMicrophoneMute(_arg018, _arg110);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg019 = data.readInt();
                    String _arg111 = data.readString();
                    setRingerModeExternal(_arg019, _arg111);
                    reply.writeNoException();
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg020 = data.readInt();
                    String _arg112 = data.readString();
                    setRingerModeInternal(_arg020, _arg112);
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    int _result9 = getRingerModeExternal();
                    reply.writeNoException();
                    reply.writeInt(_result9);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    int _result10 = getRingerModeInternal();
                    reply.writeNoException();
                    reply.writeInt(_result10);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg021 = data.readInt();
                    boolean _result11 = isValidRingerMode(_arg021);
                    reply.writeNoException();
                    reply.writeInt(_result11 ? 1 : 0);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg022 = data.readInt();
                    int _arg113 = data.readInt();
                    setVibrateSetting(_arg022, _arg113);
                    reply.writeNoException();
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg023 = data.readInt();
                    int _result12 = getVibrateSetting(_arg023);
                    reply.writeNoException();
                    reply.writeInt(_result12);
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg024 = data.readInt();
                    boolean _result13 = shouldVibrate(_arg024);
                    reply.writeNoException();
                    reply.writeInt(_result13 ? 1 : 0);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg025 = data.readInt();
                    IBinder _arg114 = data.readStrongBinder();
                    setMode(_arg025, _arg114);
                    reply.writeNoException();
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    int _result14 = getMode();
                    reply.writeNoException();
                    reply.writeInt(_result14);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg026 = data.readInt();
                    playSoundEffect(_arg026);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg027 = data.readInt();
                    float _arg115 = data.readFloat();
                    playSoundEffectVolume(_arg027, _arg115);
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result15 = loadSoundEffects();
                    reply.writeNoException();
                    reply.writeInt(_result15 ? 1 : 0);
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    unloadSoundEffects();
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    reloadAudioSettings();
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg028 = data.readString();
                    boolean _arg116 = data.readInt() != 0;
                    avrcpSupportsAbsoluteVolume(_arg028, _arg116);
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg029 = data.readInt() != 0;
                    setSpeakerphoneOn(_arg029);
                    reply.writeNoException();
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result16 = isSpeakerphoneOn();
                    reply.writeNoException();
                    reply.writeInt(_result16 ? 1 : 0);
                    return true;
                case 38:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg030 = data.readInt() != 0;
                    setBluetoothScoOn(_arg030);
                    reply.writeNoException();
                    return true;
                case 39:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result17 = isBluetoothScoOn();
                    reply.writeNoException();
                    reply.writeInt(_result17 ? 1 : 0);
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg031 = data.readInt() != 0;
                    setBluetoothA2dpOn(_arg031);
                    reply.writeNoException();
                    return true;
                case 41:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result18 = isBluetoothA2dpOn();
                    reply.writeNoException();
                    reply.writeInt(_result18 ? 1 : 0);
                    return true;
                case 42:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = AudioAttributes.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    int _arg117 = data.readInt();
                    IBinder _arg210 = data.readStrongBinder();
                    IAudioFocusDispatcher _arg36 = IAudioFocusDispatcher.Stub.asInterface(data.readStrongBinder());
                    String _arg4 = data.readString();
                    String _arg5 = data.readString();
                    int _arg6 = data.readInt();
                    IAudioPolicyCallback _arg7 = IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder());
                    int _result19 = requestAudioFocus(_arg03, _arg117, _arg210, _arg36, _arg4, _arg5, _arg6, _arg7);
                    reply.writeNoException();
                    reply.writeInt(_result19);
                    return true;
                case 43:
                    data.enforceInterface(DESCRIPTOR);
                    IAudioFocusDispatcher _arg032 = IAudioFocusDispatcher.Stub.asInterface(data.readStrongBinder());
                    String _arg118 = data.readString();
                    if (data.readInt() != 0) {
                        _arg2 = AudioAttributes.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    int _result20 = abandonAudioFocus(_arg032, _arg118, _arg2);
                    reply.writeNoException();
                    reply.writeInt(_result20);
                    return true;
                case 44:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg033 = data.readString();
                    unregisterAudioFocusClient(_arg033);
                    reply.writeNoException();
                    return true;
                case 45:
                    data.enforceInterface(DESCRIPTOR);
                    int _result21 = getCurrentAudioFocus();
                    reply.writeNoException();
                    reply.writeInt(_result21);
                    return true;
                case 46:
                    data.enforceInterface(DESCRIPTOR);
                    IRemoteControlDisplay _arg034 = IRemoteControlDisplay.Stub.asInterface(data.readStrongBinder());
                    int _arg119 = data.readInt();
                    int _arg211 = data.readInt();
                    boolean _result22 = registerRemoteControlDisplay(_arg034, _arg119, _arg211);
                    reply.writeNoException();
                    reply.writeInt(_result22 ? 1 : 0);
                    return true;
                case 47:
                    data.enforceInterface(DESCRIPTOR);
                    IRemoteControlDisplay _arg035 = IRemoteControlDisplay.Stub.asInterface(data.readStrongBinder());
                    int _arg120 = data.readInt();
                    int _arg212 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg3 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    boolean _result23 = registerRemoteController(_arg035, _arg120, _arg212, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result23 ? 1 : 0);
                    return true;
                case 48:
                    data.enforceInterface(DESCRIPTOR);
                    IRemoteControlDisplay _arg036 = IRemoteControlDisplay.Stub.asInterface(data.readStrongBinder());
                    unregisterRemoteControlDisplay(_arg036);
                    return true;
                case 49:
                    data.enforceInterface(DESCRIPTOR);
                    IRemoteControlDisplay _arg037 = IRemoteControlDisplay.Stub.asInterface(data.readStrongBinder());
                    int _arg121 = data.readInt();
                    int _arg213 = data.readInt();
                    remoteControlDisplayUsesBitmapSize(_arg037, _arg121, _arg213);
                    return true;
                case 50:
                    data.enforceInterface(DESCRIPTOR);
                    IRemoteControlDisplay _arg038 = IRemoteControlDisplay.Stub.asInterface(data.readStrongBinder());
                    boolean _arg122 = data.readInt() != 0;
                    remoteControlDisplayWantsPlaybackPositionSync(_arg038, _arg122);
                    return true;
                case 51:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg039 = data.readStrongBinder();
                    int _arg123 = data.readInt();
                    startBluetoothSco(_arg039, _arg123);
                    reply.writeNoException();
                    return true;
                case 52:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg040 = data.readStrongBinder();
                    startBluetoothScoVirtualCall(_arg040);
                    reply.writeNoException();
                    return true;
                case 53:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg041 = data.readStrongBinder();
                    stopBluetoothSco(_arg041);
                    reply.writeNoException();
                    return true;
                case 54:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg042 = data.readInt();
                    IBinder _arg124 = data.readStrongBinder();
                    forceVolumeControlStream(_arg042, _arg124);
                    reply.writeNoException();
                    return true;
                case 55:
                    data.enforceInterface(DESCRIPTOR);
                    IRingtonePlayer _arg043 = IRingtonePlayer.Stub.asInterface(data.readStrongBinder());
                    setRingtonePlayer(_arg043);
                    reply.writeNoException();
                    return true;
                case 56:
                    data.enforceInterface(DESCRIPTOR);
                    IRingtonePlayer _result24 = getRingtonePlayer();
                    reply.writeNoException();
                    reply.writeStrongBinder(_result24 != null ? _result24.asBinder() : null);
                    return true;
                case 57:
                    data.enforceInterface(DESCRIPTOR);
                    int _result25 = getMasterStreamType();
                    reply.writeNoException();
                    reply.writeInt(_result25);
                    return true;
                case 58:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg044 = data.readInt();
                    int _arg125 = data.readInt();
                    String _arg214 = data.readString();
                    setWiredDeviceConnectionState(_arg044, _arg125, _arg214);
                    reply.writeNoException();
                    return true;
                case 59:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = BluetoothDevice.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    int _arg126 = data.readInt();
                    int _arg215 = data.readInt();
                    int _result26 = setBluetoothA2dpDeviceConnectionState(_arg02, _arg126, _arg215);
                    reply.writeNoException();
                    reply.writeInt(_result26);
                    return true;
                case 60:
                    data.enforceInterface(DESCRIPTOR);
                    IAudioRoutesObserver _arg045 = IAudioRoutesObserver.Stub.asInterface(data.readStrongBinder());
                    AudioRoutesInfo _result27 = startWatchingRoutes(_arg045);
                    reply.writeNoException();
                    if (_result27 != null) {
                        reply.writeInt(1);
                        _result27.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 61:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result28 = isCameraSoundForced();
                    reply.writeNoException();
                    reply.writeInt(_result28 ? 1 : 0);
                    return true;
                case 62:
                    data.enforceInterface(DESCRIPTOR);
                    IVolumeController _arg046 = IVolumeController.Stub.asInterface(data.readStrongBinder());
                    setVolumeController(_arg046);
                    reply.writeNoException();
                    return true;
                case 63:
                    data.enforceInterface(DESCRIPTOR);
                    IVolumeController _arg047 = IVolumeController.Stub.asInterface(data.readStrongBinder());
                    boolean _arg127 = data.readInt() != 0;
                    notifyVolumeControllerVisible(_arg047, _arg127);
                    reply.writeNoException();
                    return true;
                case 64:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg048 = data.readInt();
                    boolean _result29 = isStreamAffectedByRingerMode(_arg048);
                    reply.writeNoException();
                    reply.writeInt(_result29 ? 1 : 0);
                    return true;
                case 65:
                    data.enforceInterface(DESCRIPTOR);
                    disableSafeMediaVolume();
                    reply.writeNoException();
                    return true;
                case 66:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg049 = data.readInt() != 0;
                    int _result30 = setHdmiSystemAudioSupported(_arg049);
                    reply.writeNoException();
                    reply.writeInt(_result30);
                    return true;
                case 67:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result31 = isHdmiSystemAudioSupported();
                    reply.writeNoException();
                    reply.writeInt(_result31 ? 1 : 0);
                    return true;
                case 68:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = AudioPolicyConfig.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    IAudioPolicyCallback _arg128 = IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder());
                    boolean _arg216 = data.readInt() != 0;
                    String _result32 = registerAudioPolicy(_arg0, _arg128, _arg216);
                    reply.writeNoException();
                    reply.writeString(_result32);
                    return true;
                case 69:
                    data.enforceInterface(DESCRIPTOR);
                    IAudioPolicyCallback _arg050 = IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder());
                    unregisterAudioPolicyAsync(_arg050);
                    return true;
                case 70:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg051 = data.readInt();
                    IAudioPolicyCallback _arg129 = IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder());
                    int _result33 = setFocusPropertiesForPolicy(_arg051, _arg129);
                    reply.writeNoException();
                    reply.writeInt(_result33);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IAudioService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(direction);
                    _data.writeInt(suggestedStreamType);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void adjustStreamVolume(int streamType, int direction, int flags, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    _data.writeInt(direction);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void adjustMasterVolume(int direction, int flags, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(direction);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setStreamVolume(int streamType, int index, int flags, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    _data.writeInt(index);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setRemoteStreamVolume(int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(index);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setMasterVolume(int index, int flags, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(index);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setStreamSolo(int streamType, boolean state, IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    _data.writeInt(state ? 1 : 0);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setStreamMute(int streamType, boolean state, IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    _data.writeInt(state ? 1 : 0);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isStreamMute(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void forceRemoteSubmixFullVolume(boolean startForcing, IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(startForcing ? 1 : 0);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setMasterMute(boolean state, int flags, String callingPackage, IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state ? 1 : 0);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isMasterMute() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getStreamVolume(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getMasterVolume() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getStreamMaxVolume(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getMasterMaxVolume() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getLastAudibleStreamVolume(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getLastAudibleMasterVolume() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setMicrophoneMute(boolean on, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on ? 1 : 0);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setRingerModeExternal(int ringerMode, String caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ringerMode);
                    _data.writeString(caller);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setRingerModeInternal(int ringerMode, String caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ringerMode);
                    _data.writeString(caller);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getRingerModeExternal() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getRingerModeInternal() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isValidRingerMode(int ringerMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ringerMode);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setVibrateSetting(int vibrateType, int vibrateSetting) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vibrateType);
                    _data.writeInt(vibrateSetting);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getVibrateSetting(int vibrateType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vibrateType);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean shouldVibrate(int vibrateType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vibrateType);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setMode(int mode, IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void playSoundEffect(int effectType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(effectType);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void playSoundEffectVolume(int effectType, float volume) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(effectType);
                    _data.writeFloat(volume);
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public boolean loadSoundEffects() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unloadSoundEffects() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void reloadAudioSettings() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void avrcpSupportsAbsoluteVolume(String address, boolean support) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(support ? 1 : 0);
                    this.mRemote.transact(35, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setSpeakerphoneOn(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on ? 1 : 0);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSpeakerphoneOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setBluetoothScoOn(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on ? 1 : 0);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isBluetoothScoOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setBluetoothA2dpOn(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on ? 1 : 0);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isBluetoothA2dpOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int requestAudioFocus(AudioAttributes aa, int durationHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, int flags, IAudioPolicyCallback pcb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (aa != null) {
                        _data.writeInt(1);
                        aa.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(durationHint);
                    _data.writeStrongBinder(cb);
                    _data.writeStrongBinder(fd != null ? fd.asBinder() : null);
                    _data.writeString(clientId);
                    _data.writeString(callingPackageName);
                    _data.writeInt(flags);
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int abandonAudioFocus(IAudioFocusDispatcher fd, String clientId, AudioAttributes aa) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(fd != null ? fd.asBinder() : null);
                    _data.writeString(clientId);
                    if (aa != null) {
                        _data.writeInt(1);
                        aa.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterAudioFocusClient(String clientId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(clientId);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getCurrentAudioFocus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean registerRemoteControlDisplay(IRemoteControlDisplay rcd, int w, int h) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(rcd != null ? rcd.asBinder() : null);
                    _data.writeInt(w);
                    _data.writeInt(h);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean registerRemoteController(IRemoteControlDisplay rcd, int w, int h, ComponentName listenerComp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(rcd != null ? rcd.asBinder() : null);
                    _data.writeInt(w);
                    _data.writeInt(h);
                    if (listenerComp != null) {
                        _data.writeInt(1);
                        listenerComp.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterRemoteControlDisplay(IRemoteControlDisplay rcd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(rcd != null ? rcd.asBinder() : null);
                    this.mRemote.transact(48, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void remoteControlDisplayUsesBitmapSize(IRemoteControlDisplay rcd, int w, int h) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(rcd != null ? rcd.asBinder() : null);
                    _data.writeInt(w);
                    _data.writeInt(h);
                    this.mRemote.transact(49, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void remoteControlDisplayWantsPlaybackPositionSync(IRemoteControlDisplay rcd, boolean wantsSync) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(rcd != null ? rcd.asBinder() : null);
                    _data.writeInt(wantsSync ? 1 : 0);
                    this.mRemote.transact(50, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void startBluetoothSco(IBinder cb, int targetSdkVersion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb);
                    _data.writeInt(targetSdkVersion);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void startBluetoothScoVirtualCall(IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopBluetoothSco(IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void forceVolumeControlStream(int streamType, IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setRingtonePlayer(IRingtonePlayer player) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(player != null ? player.asBinder() : null);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IRingtonePlayer getRingtonePlayer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    IRingtonePlayer _result = IRingtonePlayer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getMasterStreamType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setWiredDeviceConnectionState(int device, int state, String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(device);
                    _data.writeInt(state);
                    _data.writeString(name);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setBluetoothA2dpDeviceConnectionState(BluetoothDevice device, int state, int profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(state);
                    _data.writeInt(profile);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver observer) throws RemoteException {
                AudioRoutesInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = AudioRoutesInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isCameraSoundForced() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setVolumeController(IVolumeController controller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(controller != null ? controller.asBinder() : null);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyVolumeControllerVisible(IVolumeController controller, boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(controller != null ? controller.asBinder() : null);
                    _data.writeInt(visible ? 1 : 0);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isStreamAffectedByRingerMode(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void disableSafeMediaVolume() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setHdmiSystemAudioSupported(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on ? 1 : 0);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isHdmiSystemAudioSupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String registerAudioPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb, boolean hasFocusListener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyConfig != null) {
                        _data.writeInt(1);
                        policyConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    _data.writeInt(hasFocusListener ? 1 : 0);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterAudioPolicyAsync(IAudioPolicyCallback pcb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    this.mRemote.transact(69, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public int setFocusPropertiesForPolicy(int duckingBehavior, IAudioPolicyCallback pcb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(duckingBehavior);
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}

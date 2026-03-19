package com.mediatek.bluetooth.avrcp;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback;

public interface IBTAvrcpMusic extends IInterface {
    long duration() throws RemoteException;

    void enqueue(long[] jArr, int i) throws RemoteException;

    long getAlbumId() throws RemoteException;

    String getAlbumName() throws RemoteException;

    String getArtistName() throws RemoteException;

    long getAudioId() throws RemoteException;

    byte[] getCapabilities() throws RemoteException;

    int getEqualizeMode() throws RemoteException;

    long[] getNowPlaying() throws RemoteException;

    String getNowPlayingItemName(long j) throws RemoteException;

    byte getPlayStatus() throws RemoteException;

    int getQueuePosition() throws RemoteException;

    int getRepeatMode() throws RemoteException;

    int getScanMode() throws RemoteException;

    int getShuffleMode() throws RemoteException;

    String getTrackName() throws RemoteException;

    boolean informBatteryStatusOfCT() throws RemoteException;

    boolean informDisplayableCharacterSet(int i) throws RemoteException;

    void next() throws RemoteException;

    void nextGroup() throws RemoteException;

    void open(long[] jArr, int i) throws RemoteException;

    void pause() throws RemoteException;

    void play() throws RemoteException;

    long position() throws RemoteException;

    void prev() throws RemoteException;

    void prevGroup() throws RemoteException;

    boolean regNotificationEvent(byte b, int i) throws RemoteException;

    void registerCallback(IBTAvrcpMusicCallback iBTAvrcpMusicCallback) throws RemoteException;

    void resume() throws RemoteException;

    boolean setEqualizeMode(int i) throws RemoteException;

    boolean setPlayerApplicationSettingValue(byte b, byte b2) throws RemoteException;

    void setQueuePosition(int i) throws RemoteException;

    boolean setRepeatMode(int i) throws RemoteException;

    boolean setScanMode(int i) throws RemoteException;

    boolean setShuffleMode(int i) throws RemoteException;

    void stop() throws RemoteException;

    void unregisterCallback(IBTAvrcpMusicCallback iBTAvrcpMusicCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IBTAvrcpMusic {
        public Stub() {
            attachInterface(this, "com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    registerCallback(IBTAvrcpMusicCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    unregisterCallback(IBTAvrcpMusicCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    boolean zRegNotificationEvent = regNotificationEvent(parcel.readByte(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zRegNotificationEvent ? 1 : 0);
                    return true;
                case 4:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    boolean playerApplicationSettingValue = setPlayerApplicationSettingValue(parcel.readByte(), parcel.readByte());
                    parcel2.writeNoException();
                    parcel2.writeInt(playerApplicationSettingValue ? 1 : 0);
                    return true;
                case 5:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    byte[] capabilities = getCapabilities();
                    parcel2.writeNoException();
                    parcel2.writeByteArray(capabilities);
                    return true;
                case 6:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    play();
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    stop();
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    pause();
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    resume();
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    prev();
                    parcel2.writeNoException();
                    return true;
                case 11:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    next();
                    parcel2.writeNoException();
                    return true;
                case 12:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    prevGroup();
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    nextGroup();
                    parcel2.writeNoException();
                    return true;
                case 14:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    boolean equalizeMode = setEqualizeMode(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(equalizeMode ? 1 : 0);
                    return true;
                case 15:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    int equalizeMode2 = getEqualizeMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(equalizeMode2);
                    return true;
                case 16:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    boolean shuffleMode = setShuffleMode(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(shuffleMode ? 1 : 0);
                    return true;
                case 17:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    int shuffleMode2 = getShuffleMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(shuffleMode2);
                    return true;
                case 18:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    boolean repeatMode = setRepeatMode(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(repeatMode ? 1 : 0);
                    return true;
                case 19:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    int repeatMode2 = getRepeatMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(repeatMode2);
                    return true;
                case 20:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    boolean scanMode = setScanMode(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(scanMode ? 1 : 0);
                    return true;
                case 21:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    int scanMode2 = getScanMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(scanMode2);
                    return true;
                case 22:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    boolean zInformDisplayableCharacterSet = informDisplayableCharacterSet(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zInformDisplayableCharacterSet ? 1 : 0);
                    return true;
                case 23:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    boolean zInformBatteryStatusOfCT = informBatteryStatusOfCT();
                    parcel2.writeNoException();
                    parcel2.writeInt(zInformBatteryStatusOfCT ? 1 : 0);
                    return true;
                case 24:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    byte playStatus = getPlayStatus();
                    parcel2.writeNoException();
                    parcel2.writeByte(playStatus);
                    return true;
                case 25:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    long jPosition = position();
                    parcel2.writeNoException();
                    parcel2.writeLong(jPosition);
                    return true;
                case 26:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    long jDuration = duration();
                    parcel2.writeNoException();
                    parcel2.writeLong(jDuration);
                    return true;
                case 27:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    long audioId = getAudioId();
                    parcel2.writeNoException();
                    parcel2.writeLong(audioId);
                    return true;
                case 28:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    String trackName = getTrackName();
                    parcel2.writeNoException();
                    parcel2.writeString(trackName);
                    return true;
                case 29:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    String albumName = getAlbumName();
                    parcel2.writeNoException();
                    parcel2.writeString(albumName);
                    return true;
                case 30:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    long albumId = getAlbumId();
                    parcel2.writeNoException();
                    parcel2.writeLong(albumId);
                    return true;
                case 31:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    String artistName = getArtistName();
                    parcel2.writeNoException();
                    parcel2.writeString(artistName);
                    return true;
                case 32:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    enqueue(parcel.createLongArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 33:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    long[] nowPlaying = getNowPlaying();
                    parcel2.writeNoException();
                    parcel2.writeLongArray(nowPlaying);
                    return true;
                case 34:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    String nowPlayingItemName = getNowPlayingItemName(parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeString(nowPlayingItemName);
                    return true;
                case 35:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    open(parcel.createLongArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 36:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    int queuePosition = getQueuePosition();
                    parcel2.writeNoException();
                    parcel2.writeInt(queuePosition);
                    return true;
                case 37:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusic");
                    setQueuePosition(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }
    }
}

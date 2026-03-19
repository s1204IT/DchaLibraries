package com.android.internal.telephony.cat;

class BipChannelManager {
    public static final int MAXCHANNELID = 7;
    public static final int MAXPSCID = 5;
    public static final int MAXUICCSERVIER = 2;
    private int[] mBipChannelStatus;
    private Channel[] mChannels;
    private byte mChannelIdPool = 0;
    private byte mCurrentOccupiedPSCh = 0;
    private byte mCurrentOccupiedUICCSerCh = 0;

    public BipChannelManager() {
        this.mChannels = null;
        this.mBipChannelStatus = null;
        this.mBipChannelStatus = new int[7];
        this.mChannels = new Channel[7];
        for (int i = 0; i < 7; i++) {
            this.mChannels[i] = null;
            this.mBipChannelStatus[i] = 0;
        }
    }

    public boolean isChannelIdOccupied(int cId) {
        CatLog.d("[BIP]", "isChannelIdOccupied, mChannelIdPool " + ((int) this.mChannelIdPool) + ":" + cId);
        return (this.mChannelIdPool & (1 << (cId + (-1)))) > 0;
    }

    public int getFreeChannelId() {
        for (int i = 0; i < 7; i++) {
            if ((this.mChannelIdPool & (1 << i)) == 0) {
                return i + 1;
            }
        }
        return 0;
    }

    public int acquireChannelId(int protocolType) {
        CatLog.d("[BIP]", "acquireChannelId, protocolType " + protocolType + ",occupied " + ((int) this.mCurrentOccupiedPSCh) + "," + ((int) this.mCurrentOccupiedUICCSerCh));
        if ((3 == protocolType && 2 <= this.mCurrentOccupiedUICCSerCh) || ((1 == protocolType || 2 == protocolType) && 5 <= this.mCurrentOccupiedPSCh)) {
            return 0;
        }
        for (byte i = 0; i < 7; i = (byte) (i + 1)) {
            if ((this.mChannelIdPool & (1 << i)) == 0) {
                this.mChannelIdPool = (byte) (this.mChannelIdPool | ((byte) (1 << i)));
                if (3 == protocolType) {
                    this.mCurrentOccupiedUICCSerCh = (byte) (this.mCurrentOccupiedUICCSerCh + 1);
                } else if (1 == protocolType || 2 == protocolType) {
                    this.mCurrentOccupiedPSCh = (byte) (this.mCurrentOccupiedPSCh + 1);
                }
                CatLog.d("[BIP]", "acquireChannelId, mChannelIdPool " + ((int) this.mChannelIdPool) + ":" + (i + 1));
                return i + 1;
            }
        }
        return 0;
    }

    public void releaseChannelId(int cId, int protocolType) {
        if (cId <= 0 || cId > 7) {
            CatLog.e("[BIP]", "releaseChannelId, Invalid cid:" + cId);
            return;
        }
        try {
            if ((this.mChannelIdPool & (1 << ((byte) (cId - 1)))) == 0) {
                CatLog.e("[BIP]", "releaseChannelId, cId:" + cId + " has been released.");
                return;
            }
            if (3 == protocolType && this.mCurrentOccupiedUICCSerCh >= 0) {
                this.mCurrentOccupiedUICCSerCh = (byte) (this.mCurrentOccupiedUICCSerCh - 1);
            } else if ((1 == protocolType || 2 == protocolType) && this.mCurrentOccupiedPSCh >= 0) {
                this.mCurrentOccupiedPSCh = (byte) (this.mCurrentOccupiedPSCh - 1);
            } else {
                CatLog.e("[BIP]", "releaseChannelId, bad parameters.cId:" + cId + ":" + ((int) this.mChannelIdPool));
            }
            this.mChannelIdPool = (byte) (this.mChannelIdPool & ((byte) (~(1 << ((byte) (cId - 1))))));
            CatLog.d("[BIP]", "releaseChannelId, cId " + cId + ",protocolType " + protocolType + ",occupied " + ((int) this.mCurrentOccupiedPSCh) + "," + ((int) this.mCurrentOccupiedUICCSerCh) + ":" + ((int) this.mChannelIdPool));
        } catch (IndexOutOfBoundsException e) {
            CatLog.e("[BIP]", "IndexOutOfBoundsException releaseChannelId cId=" + cId + ":" + ((int) this.mChannelIdPool));
        }
    }

    public void releaseChannelId(int cId) {
        if (cId <= 0 || cId > 7) {
            CatLog.e("[BIP]", "releaseChannelId, Invalid cid:" + cId);
            return;
        }
        try {
            if ((this.mChannelIdPool & (1 << ((byte) (cId - 1)))) == 0) {
                CatLog.e("[BIP]", "releaseChannelId, cId:" + cId + " has been released.");
                return;
            }
            if (this.mChannels[cId - 1] == null) {
                CatLog.e("[BIP]", "channel object is null.");
                return;
            }
            int protocolType = this.mChannels[cId - 1].mProtocolType;
            if (3 == protocolType && this.mCurrentOccupiedUICCSerCh > 0) {
                this.mCurrentOccupiedUICCSerCh = (byte) (this.mCurrentOccupiedUICCSerCh - 1);
            } else if ((1 == protocolType || 2 == protocolType) && this.mCurrentOccupiedPSCh > 0) {
                this.mCurrentOccupiedPSCh = (byte) (this.mCurrentOccupiedPSCh - 1);
            } else {
                CatLog.e("[BIP]", "releaseChannelId, bad parameters.cId:" + cId + ":" + ((int) this.mChannelIdPool));
            }
            this.mChannelIdPool = (byte) (this.mChannelIdPool & ((byte) (~(1 << ((byte) (cId - 1))))));
            CatLog.d("[BIP]", "releaseChannelId, cId " + cId + ",protocolType" + protocolType + ",occupied " + ((int) this.mCurrentOccupiedPSCh) + "," + ((int) this.mCurrentOccupiedUICCSerCh) + ":" + ((int) this.mChannelIdPool));
        } catch (IndexOutOfBoundsException e) {
            CatLog.e("[BIP]", "IndexOutOfBoundsException releaseChannelId cId=" + cId + ":" + ((int) this.mChannelIdPool));
        }
    }

    public int addChannel(int cId, Channel ch) {
        CatLog.d("[BIP]", "BCM-addChannel:" + cId);
        if (cId > 0) {
            try {
                this.mChannels[cId - 1] = ch;
                this.mBipChannelStatus[cId - 1] = 4;
            } catch (IndexOutOfBoundsException e) {
                CatLog.e("[BIP]", "IndexOutOfBoundsException addChannel cId=" + cId);
                return -1;
            }
        } else {
            CatLog.e("[BIP]", "No free channel id.");
        }
        return cId;
    }

    public Channel getChannel(int cId) {
        try {
            return this.mChannels[cId - 1];
        } catch (IndexOutOfBoundsException e) {
            CatLog.e("[BIP]", "IndexOutOfBoundsException getChannel cId=" + cId);
            return null;
        }
    }

    public int getBipChannelStatus(int cId) {
        return this.mBipChannelStatus[cId - 1];
    }

    public void setBipChannelStatus(int cId, int status) {
        try {
            this.mBipChannelStatus[cId - 1] = status;
        } catch (IndexOutOfBoundsException e) {
            CatLog.e("[BIP]", "IndexOutOfBoundsException setBipChannelStatus cId=" + cId);
        }
    }

    public int removeChannel(int cId) {
        CatLog.d("[BIP]", "BCM-removeChannel:" + cId);
        try {
            releaseChannelId(cId);
            this.mChannels[cId - 1] = null;
            this.mBipChannelStatus[cId - 1] = 2;
            return 1;
        } catch (IndexOutOfBoundsException e) {
            CatLog.e("[BIP]", "IndexOutOfBoundsException removeChannel cId=" + cId);
            return 0;
        } catch (NullPointerException e2) {
            CatLog.e("[BIP]", "removeChannel channel:" + cId + " is null");
            return 0;
        }
    }

    public boolean isClientChannelOpened() {
        for (int i = 0; i < 7; i++) {
            try {
                if (this.mChannels != null && this.mChannels[i] != null && (this.mChannels[i].mProtocolType & 3) != 0) {
                    return true;
                }
            } catch (NullPointerException e) {
                CatLog.e("[BIP]", "isClientChannelOpened channel:" + i + " is null");
            }
        }
        return false;
    }

    public void updateBipChannelStatus(int cId, int chStatus) {
        try {
            this.mChannels[cId - 1].mChannelStatus = chStatus;
        } catch (IndexOutOfBoundsException e) {
            CatLog.e("[BIP]", "IndexOutOfBoundsException updateBipChannelStatus cId=" + cId);
        } catch (NullPointerException e2) {
            CatLog.e("[BIP]", "updateBipChannelStatus id:" + cId + " is null");
        }
    }

    public void updateChannelStatus(int cId, int chStatus) {
        try {
            this.mChannels[cId - 1].mChannelStatusData.mChannelStatus = chStatus;
        } catch (IndexOutOfBoundsException e) {
            CatLog.e("[BIP]", "IndexOutOfBoundsException updateChannelStatus cId=" + cId);
        } catch (NullPointerException e2) {
            CatLog.e("[BIP]", "updateChannelStatus id:" + cId + " is null");
        }
    }

    public void updateChannelStatusInfo(int cId, int chStatusInfo) {
        try {
            this.mChannels[cId - 1].mChannelStatusData.mChannelStatusInfo = chStatusInfo;
        } catch (IndexOutOfBoundsException e) {
            CatLog.e("[BIP]", "IndexOutOfBoundsException updateChannelStatusInfo cId=" + cId);
        } catch (NullPointerException e2) {
            CatLog.e("[BIP]", "updateChannelStatusInfo id:" + cId + " is null");
        }
    }
}

package com.mediatek.internal.telephony.cat;

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

    public boolean isChannelIdOccupied(int i) {
        MtkCatLog.d("[BIP]", "isChannelIdOccupied, mChannelIdPool " + ((int) this.mChannelIdPool) + ":" + i);
        return ((1 << (i - 1)) & this.mChannelIdPool) > 0;
    }

    public int getFreeChannelId() {
        for (int i = 0; i < 7; i++) {
            if ((this.mChannelIdPool & (1 << i)) == 0) {
                return i + 1;
            }
        }
        return 0;
    }

    public int acquireChannelId(int i) {
        MtkCatLog.d("[BIP]", "acquireChannelId, protocolType " + i + ",occupied " + ((int) this.mCurrentOccupiedPSCh) + "," + ((int) this.mCurrentOccupiedUICCSerCh));
        if ((3 == i && 2 <= this.mCurrentOccupiedUICCSerCh) || ((1 == i || 2 == i) && 5 <= this.mCurrentOccupiedPSCh)) {
            return 0;
        }
        for (byte b = 0; b < 7; b = (byte) (b + 1)) {
            int i2 = 1 << b;
            if ((this.mChannelIdPool & i2) == 0) {
                this.mChannelIdPool = (byte) (this.mChannelIdPool | ((byte) i2));
                if (3 == i) {
                    this.mCurrentOccupiedUICCSerCh = (byte) (this.mCurrentOccupiedUICCSerCh + 1);
                } else if (1 == i || 2 == i) {
                    this.mCurrentOccupiedPSCh = (byte) (this.mCurrentOccupiedPSCh + 1);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("acquireChannelId, mChannelIdPool ");
                sb.append((int) this.mChannelIdPool);
                sb.append(":");
                int i3 = b + 1;
                sb.append(i3);
                MtkCatLog.d("[BIP]", sb.toString());
                return i3;
            }
        }
        return 0;
    }

    public void releaseChannelId(int i, int i2) {
        if (i <= 0 || i > 7) {
            MtkCatLog.e("[BIP]", "releaseChannelId, Invalid cid:" + i);
            return;
        }
        try {
            int i3 = 1 << ((byte) (i - 1));
            if ((this.mChannelIdPool & i3) == 0) {
                MtkCatLog.e("[BIP]", "releaseChannelId, cId:" + i + " has been released.");
                return;
            }
            if (3 == i2 && this.mCurrentOccupiedUICCSerCh >= 0) {
                this.mCurrentOccupiedUICCSerCh = (byte) (this.mCurrentOccupiedUICCSerCh - 1);
            } else if ((1 == i2 || 2 == i2) && this.mCurrentOccupiedPSCh >= 0) {
                this.mCurrentOccupiedPSCh = (byte) (this.mCurrentOccupiedPSCh - 1);
            } else {
                MtkCatLog.e("[BIP]", "releaseChannelId, bad parameters.cId:" + i + ":" + ((int) this.mChannelIdPool));
            }
            this.mChannelIdPool = (byte) (this.mChannelIdPool & ((byte) (~i3)));
            MtkCatLog.d("[BIP]", "releaseChannelId, cId " + i + ",protocolType " + i2 + ",occupied " + ((int) this.mCurrentOccupiedPSCh) + "," + ((int) this.mCurrentOccupiedUICCSerCh) + ":" + ((int) this.mChannelIdPool));
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.e("[BIP]", "IndexOutOfBoundsException releaseChannelId cId=" + i + ":" + ((int) this.mChannelIdPool));
        }
    }

    public void releaseChannelId(int i) {
        if (i <= 0 || i > 7) {
            MtkCatLog.e("[BIP]", "releaseChannelId, Invalid cid:" + i);
            return;
        }
        try {
            int i2 = i - 1;
            int i3 = 1 << ((byte) i2);
            if ((this.mChannelIdPool & i3) == 0) {
                MtkCatLog.e("[BIP]", "releaseChannelId, cId:" + i + " has been released.");
                return;
            }
            if (this.mChannels[i2] != null) {
                int i4 = this.mChannels[i2].mProtocolType;
                if (3 == i4 && this.mCurrentOccupiedUICCSerCh > 0) {
                    this.mCurrentOccupiedUICCSerCh = (byte) (this.mCurrentOccupiedUICCSerCh - 1);
                } else if ((1 == i4 || 2 == i4) && this.mCurrentOccupiedPSCh > 0) {
                    this.mCurrentOccupiedPSCh = (byte) (this.mCurrentOccupiedPSCh - 1);
                } else {
                    MtkCatLog.e("[BIP]", "releaseChannelId, bad parameters.cId:" + i + ":" + ((int) this.mChannelIdPool));
                }
                this.mChannelIdPool = (byte) (this.mChannelIdPool & ((byte) (~i3)));
                MtkCatLog.d("[BIP]", "releaseChannelId, cId " + i + ",protocolType" + i4 + ",occupied " + ((int) this.mCurrentOccupiedPSCh) + "," + ((int) this.mCurrentOccupiedUICCSerCh) + ":" + ((int) this.mChannelIdPool));
                return;
            }
            MtkCatLog.e("[BIP]", "channel object is null.");
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.e("[BIP]", "IndexOutOfBoundsException releaseChannelId cId=" + i + ":" + ((int) this.mChannelIdPool));
        }
    }

    public int addChannel(int i, Channel channel) {
        MtkCatLog.d("[BIP]", "BCM-addChannel:" + i);
        if (i > 0) {
            try {
                int i2 = i - 1;
                this.mChannels[i2] = channel;
                this.mBipChannelStatus[i2] = 4;
            } catch (IndexOutOfBoundsException e) {
                MtkCatLog.e("[BIP]", "IndexOutOfBoundsException addChannel cId=" + i);
                return -1;
            }
        } else {
            MtkCatLog.e("[BIP]", "No free channel id.");
        }
        return i;
    }

    public Channel getChannel(int i) {
        try {
            return this.mChannels[i - 1];
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.e("[BIP]", "IndexOutOfBoundsException getChannel cId=" + i);
            return null;
        }
    }

    public int getBipChannelStatus(int i) {
        return this.mBipChannelStatus[i - 1];
    }

    public void setBipChannelStatus(int i, int i2) {
        try {
            this.mBipChannelStatus[i - 1] = i2;
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.e("[BIP]", "IndexOutOfBoundsException setBipChannelStatus cId=" + i);
        }
    }

    public int removeChannel(int i) {
        MtkCatLog.d("[BIP]", "BCM-removeChannel:" + i);
        try {
            releaseChannelId(i);
            int i2 = i - 1;
            this.mChannels[i2] = null;
            this.mBipChannelStatus[i2] = 2;
            return 1;
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.e("[BIP]", "IndexOutOfBoundsException removeChannel cId=" + i);
            return 0;
        } catch (NullPointerException e2) {
            MtkCatLog.e("[BIP]", "removeChannel channel:" + i + " is null");
            return 0;
        }
    }

    public boolean isClientChannelOpened() {
        for (int i = 0; i < 7; i++) {
            try {
                if (this.mChannels != null && this.mChannels[i] != null && (3 & this.mChannels[i].mProtocolType) != 0) {
                    return true;
                }
            } catch (NullPointerException e) {
                MtkCatLog.e("[BIP]", "isClientChannelOpened channel:" + i + " is null");
            }
        }
        return false;
    }

    public void updateBipChannelStatus(int i, int i2) {
        try {
            this.mChannels[i - 1].mChannelStatus = i2;
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.e("[BIP]", "IndexOutOfBoundsException updateBipChannelStatus cId=" + i);
        } catch (NullPointerException e2) {
            MtkCatLog.e("[BIP]", "updateBipChannelStatus id:" + i + " is null");
        }
    }

    public void updateChannelStatus(int i, int i2) {
        try {
            this.mChannels[i - 1].mChannelStatusData.mChannelStatus = i2;
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.e("[BIP]", "IndexOutOfBoundsException updateChannelStatus cId=" + i);
        } catch (NullPointerException e2) {
            MtkCatLog.e("[BIP]", "updateChannelStatus id:" + i + " is null");
        }
    }

    public void updateChannelStatusInfo(int i, int i2) {
        try {
            this.mChannels[i - 1].mChannelStatusData.mChannelStatusInfo = i2;
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.e("[BIP]", "IndexOutOfBoundsException updateChannelStatusInfo cId=" + i);
        } catch (NullPointerException e2) {
            MtkCatLog.e("[BIP]", "updateChannelStatusInfo id:" + i + " is null");
        }
    }
}

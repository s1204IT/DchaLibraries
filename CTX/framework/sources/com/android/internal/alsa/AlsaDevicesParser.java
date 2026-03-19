package com.android.internal.alsa;

import android.provider.Downloads;
import android.provider.SettingsStringUtil;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class AlsaDevicesParser {
    protected static final boolean DEBUG = false;
    public static final int SCANSTATUS_EMPTY = 2;
    public static final int SCANSTATUS_FAIL = 1;
    public static final int SCANSTATUS_NOTSCANNED = -1;
    public static final int SCANSTATUS_SUCCESS = 0;
    private static final String TAG = "AlsaDevicesParser";
    private static final String kDevicesFilePath = "/proc/asound/devices";
    private static final int kEndIndex_CardNum = 8;
    private static final int kEndIndex_DeviceNum = 11;
    private static final int kIndex_CardDeviceField = 5;
    private static final int kStartIndex_CardNum = 6;
    private static final int kStartIndex_DeviceNum = 9;
    private static final int kStartIndex_Type = 14;
    private static LineTokenizer mTokenizer = new LineTokenizer(" :[]-");
    private boolean mHasCaptureDevices = false;
    private boolean mHasPlaybackDevices = false;
    private boolean mHasMIDIDevices = false;
    private int mScanStatus = -1;
    private final ArrayList<AlsaDeviceRecord> mDeviceRecords = new ArrayList<>();

    public class AlsaDeviceRecord {
        public static final int kDeviceDir_Capture = 0;
        public static final int kDeviceDir_Playback = 1;
        public static final int kDeviceDir_Unknown = -1;
        public static final int kDeviceType_Audio = 0;
        public static final int kDeviceType_Control = 1;
        public static final int kDeviceType_MIDI = 2;
        public static final int kDeviceType_Unknown = -1;
        int mCardNum = -1;
        int mDeviceNum = -1;
        int mDeviceType = -1;
        int mDeviceDir = -1;

        public AlsaDeviceRecord() {
        }

        public boolean parse(String str) {
            int length;
            int i = 0;
            int i2 = 0;
            while (true) {
                int iNextToken = AlsaDevicesParser.mTokenizer.nextToken(str, i);
                if (iNextToken == -1) {
                    return true;
                }
                int iNextDelimiter = AlsaDevicesParser.mTokenizer.nextDelimiter(str, iNextToken);
                if (iNextDelimiter == -1) {
                    length = str.length();
                } else {
                    length = iNextDelimiter;
                }
                String strSubstring = str.substring(iNextToken, length);
                switch (i2) {
                    case 1:
                        this.mCardNum = Integer.parseInt(strSubstring);
                        if (str.charAt(length) != '-') {
                            i2++;
                        }
                        break;
                    case 2:
                        this.mDeviceNum = Integer.parseInt(strSubstring);
                        break;
                    case 3:
                        if (!strSubstring.equals("digital")) {
                            if (strSubstring.equals(Downloads.Impl.COLUMN_CONTROL)) {
                                this.mDeviceType = 1;
                            } else {
                                strSubstring.equals("raw");
                            }
                        }
                        break;
                    case 4:
                        if (strSubstring.equals("audio")) {
                            this.mDeviceType = 0;
                        } else if (strSubstring.equals("midi")) {
                            this.mDeviceType = 2;
                            AlsaDevicesParser.this.mHasMIDIDevices = true;
                        }
                        break;
                    case 5:
                        try {
                            if (strSubstring.equals("capture")) {
                                this.mDeviceDir = 0;
                                AlsaDevicesParser.this.mHasCaptureDevices = true;
                            } else if (strSubstring.equals("playback")) {
                                this.mDeviceDir = 1;
                                AlsaDevicesParser.this.mHasPlaybackDevices = true;
                            }
                        } catch (NumberFormatException e) {
                            Slog.e(AlsaDevicesParser.TAG, "Failed to parse token " + i2 + " of " + AlsaDevicesParser.kDevicesFilePath + " token: " + strSubstring);
                            return false;
                        }
                        break;
                }
                i2++;
                i = length;
            }
        }

        public String textFormat() {
            StringBuilder sb = new StringBuilder();
            sb.append("[" + this.mCardNum + SettingsStringUtil.DELIMITER + this.mDeviceNum + "]");
            switch (this.mDeviceType) {
                case 0:
                    sb.append(" Audio");
                    break;
                case 1:
                    sb.append(" Control");
                    break;
                case 2:
                    sb.append(" MIDI");
                    break;
                default:
                    sb.append(" N/A");
                    break;
            }
            switch (this.mDeviceDir) {
                case 0:
                    sb.append(" Capture");
                    break;
                case 1:
                    sb.append(" Playback");
                    break;
                default:
                    sb.append(" N/A");
                    break;
            }
            return sb.toString();
        }
    }

    public int getDefaultDeviceNum(int i) {
        return 0;
    }

    public boolean hasPlaybackDevices(int i) {
        for (AlsaDeviceRecord alsaDeviceRecord : this.mDeviceRecords) {
            if (alsaDeviceRecord.mCardNum == i && alsaDeviceRecord.mDeviceType == 0 && alsaDeviceRecord.mDeviceDir == 1) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCaptureDevices(int i) {
        for (AlsaDeviceRecord alsaDeviceRecord : this.mDeviceRecords) {
            if (alsaDeviceRecord.mCardNum == i && alsaDeviceRecord.mDeviceType == 0 && alsaDeviceRecord.mDeviceDir == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMIDIDevices(int i) {
        for (AlsaDeviceRecord alsaDeviceRecord : this.mDeviceRecords) {
            if (alsaDeviceRecord.mCardNum == i && alsaDeviceRecord.mDeviceType == 2) {
                return true;
            }
        }
        return false;
    }

    private boolean isLineDeviceRecord(String str) {
        return str.charAt(5) == '[';
    }

    public int scan() {
        this.mDeviceRecords.clear();
        try {
            FileReader fileReader = new FileReader(new File(kDevicesFilePath));
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                if (isLineDeviceRecord(line)) {
                    AlsaDeviceRecord alsaDeviceRecord = new AlsaDeviceRecord();
                    alsaDeviceRecord.parse(line);
                    Slog.i(TAG, alsaDeviceRecord.textFormat());
                    this.mDeviceRecords.add(alsaDeviceRecord);
                }
            }
            fileReader.close();
            if (this.mDeviceRecords.size() > 0) {
                this.mScanStatus = 0;
            } else {
                this.mScanStatus = 2;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            this.mScanStatus = 1;
        } catch (IOException e2) {
            e2.printStackTrace();
            this.mScanStatus = 1;
        }
        return this.mScanStatus;
    }

    public int getScanStatus() {
        return this.mScanStatus;
    }

    private void Log(String str) {
    }
}

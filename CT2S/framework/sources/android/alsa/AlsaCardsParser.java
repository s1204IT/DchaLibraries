package android.alsa;

import android.net.ProxyInfo;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class AlsaCardsParser {
    private static final String TAG = "AlsaCardsParser";
    private static LineTokenizer tokenizer_ = new LineTokenizer(" :[]");
    private Vector<AlsaCardRecord> cardRecords_ = new Vector<>();

    public class AlsaCardRecord {
        public int mCardNum = -1;
        public String mField1 = ProxyInfo.LOCAL_EXCL_LIST;
        public String mCardName = ProxyInfo.LOCAL_EXCL_LIST;
        public String mCardDescription = ProxyInfo.LOCAL_EXCL_LIST;

        public AlsaCardRecord() {
        }

        public boolean parse(String line, int lineIndex) {
            int tokenIndex;
            if (lineIndex == 0) {
                int tokenIndex2 = AlsaCardsParser.tokenizer_.nextToken(line, AlsaCardsParser.tokenizer_.nextDelimiter(line, AlsaCardsParser.tokenizer_.nextToken(line, 0)));
                int delimIndex = AlsaCardsParser.tokenizer_.nextDelimiter(line, tokenIndex2);
                this.mField1 = line.substring(tokenIndex2, delimIndex);
                this.mCardName = line.substring(AlsaCardsParser.tokenizer_.nextToken(line, delimIndex));
            } else if (lineIndex == 1 && (tokenIndex = AlsaCardsParser.tokenizer_.nextToken(line, 0)) != -1) {
                this.mCardDescription = line.substring(tokenIndex);
            }
            return true;
        }

        public String textFormat() {
            return this.mCardName + " : " + this.mCardDescription;
        }
    }

    public void scan() {
        this.cardRecords_.clear();
        File cardsFile = new File("/proc/asound/cards");
        try {
            FileReader reader = new FileReader(cardsFile);
            BufferedReader bufferedReader = new BufferedReader(reader);
            while (true) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    AlsaCardRecord cardRecord = new AlsaCardRecord();
                    cardRecord.parse(line, 0);
                    cardRecord.parse(bufferedReader.readLine(), 1);
                    this.cardRecords_.add(cardRecord);
                } else {
                    reader.close();
                    return;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public AlsaCardRecord getCardRecordAt(int index) {
        return this.cardRecords_.get(index);
    }

    public int getNumCardRecords() {
        return this.cardRecords_.size();
    }

    public void Log() {
        int numCardRecs = getNumCardRecords();
        for (int index = 0; index < numCardRecs; index++) {
            Slog.w(TAG, "usb:" + getCardRecordAt(index).textFormat());
        }
    }
}

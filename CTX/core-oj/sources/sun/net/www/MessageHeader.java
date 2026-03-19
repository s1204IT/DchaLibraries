package sun.net.www;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

public class MessageHeader {
    private String[] keys;
    private int nkeys;
    private String[] values;

    public MessageHeader() {
        grow();
    }

    public MessageHeader(InputStream inputStream) throws IOException {
        parseHeader(inputStream);
    }

    public synchronized String getHeaderNamesInList() {
        StringJoiner stringJoiner;
        stringJoiner = new StringJoiner(",");
        for (int i = 0; i < this.nkeys; i++) {
            stringJoiner.add(this.keys[i]);
        }
        return stringJoiner.toString();
    }

    public synchronized void reset() {
        this.keys = null;
        this.values = null;
        this.nkeys = 0;
        grow();
    }

    public synchronized String findValue(String str) {
        if (str == null) {
            int i = this.nkeys;
            do {
                i--;
                if (i >= 0) {
                }
            } while (this.keys[i] != null);
            return this.values[i];
        }
        int i2 = this.nkeys;
        do {
            i2--;
            if (i2 >= 0) {
            }
        } while (!str.equalsIgnoreCase(this.keys[i2]));
        return this.values[i2];
        return null;
    }

    public synchronized int getKey(String str) {
        int i = this.nkeys;
        while (true) {
            i--;
            if (i < 0) {
                return -1;
            }
            if (this.keys[i] == str || (str != null && str.equalsIgnoreCase(this.keys[i]))) {
                break;
            }
        }
        return i;
    }

    public synchronized String getKey(int i) {
        if (i >= 0) {
            if (i < this.nkeys) {
                return this.keys[i];
            }
        }
        return null;
    }

    public synchronized String getValue(int i) {
        if (i >= 0) {
            if (i < this.nkeys) {
                return this.values[i];
            }
        }
        return null;
    }

    public synchronized String findNextValue(String str, String str2) {
        boolean z = false;
        try {
            if (str == null) {
                int i = this.nkeys;
                while (true) {
                    i--;
                    if (i < 0) {
                        break;
                    }
                    if (this.keys[i] == null) {
                        if (z) {
                            return this.values[i];
                        }
                        if (this.values[i] == str2) {
                            z = true;
                        }
                    }
                }
            } else {
                int i2 = this.nkeys;
                while (true) {
                    i2--;
                    if (i2 < 0) {
                        break;
                    }
                    if (str.equalsIgnoreCase(this.keys[i2])) {
                        if (z) {
                            return this.values[i2];
                        }
                        if (this.values[i2] == str2) {
                            z = true;
                        }
                    }
                }
            }
            return null;
        } catch (Throwable th) {
            throw th;
        }
    }

    public boolean filterNTLMResponses(String str) {
        boolean z;
        int i = 0;
        while (true) {
            if (i < this.nkeys) {
                if (!str.equalsIgnoreCase(this.keys[i]) || this.values[i] == null || this.values[i].length() <= 5 || !this.values[i].substring(0, 5).equalsIgnoreCase("NTLM ")) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (z) {
            int i2 = 0;
            for (int i3 = 0; i3 < this.nkeys; i3++) {
                if (!str.equalsIgnoreCase(this.keys[i3]) || (!"Negotiate".equalsIgnoreCase(this.values[i3]) && !"Kerberos".equalsIgnoreCase(this.values[i3]))) {
                    if (i3 != i2) {
                        this.keys[i2] = this.keys[i3];
                        this.values[i2] = this.values[i3];
                    }
                    i2++;
                }
            }
            if (i2 != this.nkeys) {
                this.nkeys = i2;
                return true;
            }
        }
        return false;
    }

    class HeaderIterator implements Iterator<String> {
        String key;
        Object lock;
        int index = 0;
        int next = -1;
        boolean haveNext = false;

        public HeaderIterator(String str, Object obj) {
            this.key = str;
            this.lock = obj;
        }

        @Override
        public boolean hasNext() {
            synchronized (this.lock) {
                if (this.haveNext) {
                    return true;
                }
                while (this.index < MessageHeader.this.nkeys) {
                    if (this.key.equalsIgnoreCase(MessageHeader.this.keys[this.index])) {
                        this.haveNext = true;
                        int i = this.index;
                        this.index = i + 1;
                        this.next = i;
                        return true;
                    }
                    this.index++;
                }
                return false;
            }
        }

        @Override
        public String next() {
            synchronized (this.lock) {
                if (this.haveNext) {
                    this.haveNext = false;
                    return MessageHeader.this.values[this.next];
                }
                if (hasNext()) {
                    return next();
                }
                throw new NoSuchElementException("No more elements");
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove not allowed");
        }
    }

    public Iterator<String> multiValueIterator(String str) {
        return new HeaderIterator(str, this);
    }

    public synchronized Map<String, List<String>> getHeaders() {
        return getHeaders(null);
    }

    public synchronized Map<String, List<String>> getHeaders(String[] strArr) {
        return filterAndAddHeaders(strArr, null);
    }

    public synchronized Map<String, List<String>> filterAndAddHeaders(String[] strArr, Map<String, List<String>> map) {
        HashMap map2;
        map2 = new HashMap();
        int i = this.nkeys;
        loop0: while (true) {
            boolean z = false;
            while (true) {
                i--;
                if (i < 0) {
                    break loop0;
                }
                if (strArr != null) {
                    int i2 = 0;
                    while (true) {
                        if (i2 >= strArr.length) {
                            break;
                        }
                        if (strArr[i2] == null || !strArr[i2].equalsIgnoreCase(this.keys[i])) {
                            i2++;
                        } else {
                            z = true;
                            break;
                        }
                    }
                }
                if (!z) {
                    Collection arrayList = (List) map2.get(this.keys[i]);
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                        map2.put(this.keys[i], arrayList);
                    }
                    arrayList.add(this.values[i]);
                }
            }
        }
        if (map != null) {
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                Collection arrayList2 = (List) map2.get(entry.getKey());
                if (arrayList2 == null) {
                    arrayList2 = new ArrayList();
                    map2.put(entry.getKey(), arrayList2);
                }
                arrayList2.addAll(entry.getValue());
            }
        }
        for (K k : map2.keySet()) {
            map2.put(k, Collections.unmodifiableList((List) map2.get(k)));
        }
        return Collections.unmodifiableMap(map2);
    }

    public synchronized void print(PrintStream printStream) {
        for (int i = 0; i < this.nkeys; i++) {
            if (this.keys[i] != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(this.keys[i]);
                sb.append(this.values[i] != null ? ": " + this.values[i] : "");
                sb.append("\r\n");
                printStream.print(sb.toString());
            }
        }
        printStream.print("\r\n");
        printStream.flush();
    }

    public synchronized void add(String str, String str2) {
        grow();
        this.keys[this.nkeys] = str;
        this.values[this.nkeys] = str2;
        this.nkeys++;
    }

    public synchronized void prepend(String str, String str2) {
        grow();
        for (int i = this.nkeys; i > 0; i--) {
            int i2 = i - 1;
            this.keys[i] = this.keys[i2];
            this.values[i] = this.values[i2];
        }
        this.keys[0] = str;
        this.values[0] = str2;
        this.nkeys++;
    }

    public synchronized void set(int i, String str, String str2) {
        grow();
        if (i < 0) {
            return;
        }
        if (i >= this.nkeys) {
            add(str, str2);
        } else {
            this.keys[i] = str;
            this.values[i] = str2;
        }
    }

    private void grow() {
        if (this.keys == null || this.nkeys >= this.keys.length) {
            String[] strArr = new String[this.nkeys + 4];
            String[] strArr2 = new String[this.nkeys + 4];
            if (this.keys != null) {
                System.arraycopy(this.keys, 0, strArr, 0, this.nkeys);
            }
            if (this.values != null) {
                System.arraycopy(this.values, 0, strArr2, 0, this.nkeys);
            }
            this.keys = strArr;
            this.values = strArr2;
        }
    }

    public synchronized void remove(String str) {
        int i = 0;
        try {
            if (str == null) {
                while (i < this.nkeys) {
                    while (this.keys[i] == null && i < this.nkeys) {
                        int i2 = i;
                        while (i2 < this.nkeys - 1) {
                            int i3 = i2 + 1;
                            this.keys[i2] = this.keys[i3];
                            this.values[i2] = this.values[i3];
                            i2 = i3;
                        }
                        this.nkeys--;
                    }
                    i++;
                }
            } else {
                while (i < this.nkeys) {
                    while (str.equalsIgnoreCase(this.keys[i]) && i < this.nkeys) {
                        int i4 = i;
                        while (i4 < this.nkeys - 1) {
                            int i5 = i4 + 1;
                            this.keys[i4] = this.keys[i5];
                            this.values[i4] = this.values[i5];
                            i4 = i5;
                        }
                        this.nkeys--;
                    }
                    i++;
                }
            }
        } catch (Throwable th) {
            throw th;
        }
    }

    public synchronized void set(String str, String str2) {
        int i = this.nkeys;
        do {
            i--;
            if (i < 0) {
                add(str, str2);
                return;
            }
        } while (!str.equalsIgnoreCase(this.keys[i]));
        this.values[i] = str2;
    }

    public synchronized void setIfNotSet(String str, String str2) {
        if (findValue(str) == null) {
            add(str, str2);
        }
    }

    public static String canonicalID(String str) {
        char cCharAt;
        if (str == null) {
            return "";
        }
        int length = str.length();
        int i = 0;
        boolean z = false;
        while (i < length && ((cCharAt = str.charAt(i)) == '<' || cCharAt <= ' ')) {
            i++;
            z = true;
        }
        while (i < length) {
            char cCharAt2 = str.charAt(length - 1);
            if (cCharAt2 != '>' && cCharAt2 > ' ') {
                break;
            }
            length--;
            z = true;
        }
        return z ? str.substring(i, length) : str;
    }

    public void parseHeader(InputStream inputStream) throws IOException {
        synchronized (this) {
            this.nkeys = 0;
        }
        mergeHeader(inputStream);
    }

    public void mergeHeader(InputStream inputStream) throws IOException {
        String strCopyValueOf;
        String strCopyValueOf2;
        int i;
        if (inputStream == null) {
            return;
        }
        char[] cArr = new char[10];
        int i2 = inputStream.read();
        while (i2 != 10 && i2 != 13 && i2 >= 0) {
            int i3 = 1;
            int i4 = 0;
            boolean z = i2 > 32;
            cArr[0] = (char) i2;
            i2 = -1;
            int i5 = -1;
            while (true) {
                int i6 = inputStream.read();
                if (i6 >= 0) {
                    if (i6 != 13) {
                        if (i6 == 32) {
                            z = false;
                        } else {
                            if (i6 == 58) {
                                if (z && i3 > 0) {
                                    i5 = i3;
                                }
                            } else {
                                switch (i6) {
                                    case 9:
                                        i6 = 32;
                                        break;
                                }
                            }
                            z = false;
                        }
                        if (i3 < cArr.length) {
                            char[] cArr2 = new char[cArr.length * 2];
                            System.arraycopy((Object) cArr, 0, (Object) cArr2, 0, i3);
                            cArr = cArr2;
                        }
                        cArr[i3] = (char) i6;
                        i3++;
                    }
                    i = inputStream.read();
                    if (i6 == 13 && i == 10 && (i = inputStream.read()) == 13) {
                        i = inputStream.read();
                    }
                    if (i != 10 && i != 13 && i <= 32) {
                        i6 = 32;
                        if (i3 < cArr.length) {
                        }
                        cArr[i3] = (char) i6;
                        i3++;
                    }
                }
            }
            i2 = i;
            while (i3 > 0 && cArr[i3 - 1] <= ' ') {
                i3--;
            }
            if (i5 <= 0) {
                strCopyValueOf = null;
            } else {
                strCopyValueOf = String.copyValueOf(cArr, 0, i5);
                if (i5 < i3 && cArr[i5] == ':') {
                    i5++;
                }
                i4 = i5;
                while (i4 < i3 && cArr[i4] <= ' ') {
                    i4++;
                }
            }
            if (i4 >= i3) {
                strCopyValueOf2 = new String();
            } else {
                strCopyValueOf2 = String.copyValueOf(cArr, i4, i3 - i4);
            }
            add(strCopyValueOf, strCopyValueOf2);
        }
    }

    public synchronized String toString() {
        String str;
        str = super.toString() + this.nkeys + " pairs: ";
        for (int i = 0; i < this.keys.length && i < this.nkeys; i++) {
            str = str + "{" + this.keys[i] + ": " + this.values[i] + "}";
        }
        return str;
    }
}

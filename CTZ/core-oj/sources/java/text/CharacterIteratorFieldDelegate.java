package java.text;

import java.text.Format;
import java.util.ArrayList;

class CharacterIteratorFieldDelegate implements Format.FieldDelegate {
    private ArrayList<AttributedString> attributedStrings = new ArrayList<>();
    private int size;

    CharacterIteratorFieldDelegate() {
    }

    @Override
    public void formatted(Format.Field field, Object obj, int i, int i2, StringBuffer stringBuffer) {
        if (i != i2) {
            if (i < this.size) {
                int length = this.size;
                int size = this.attributedStrings.size() - 1;
                while (i < length) {
                    int i3 = size - 1;
                    AttributedString attributedString = this.attributedStrings.get(size);
                    length -= attributedString.length();
                    int iMax = Math.max(0, i - length);
                    attributedString.addAttribute(field, obj, iMax, Math.min(i2 - i, attributedString.length() - iMax) + iMax);
                    size = i3;
                }
            }
            if (this.size < i) {
                this.attributedStrings.add(new AttributedString(stringBuffer.substring(this.size, i)));
                this.size = i;
            }
            if (this.size < i2) {
                AttributedString attributedString2 = new AttributedString(stringBuffer.substring(Math.max(i, this.size), i2));
                attributedString2.addAttribute(field, obj);
                this.attributedStrings.add(attributedString2);
                this.size = i2;
            }
        }
    }

    @Override
    public void formatted(int i, Format.Field field, Object obj, int i2, int i3, StringBuffer stringBuffer) {
        formatted(field, obj, i2, i3, stringBuffer);
    }

    public AttributedCharacterIterator getIterator(String str) {
        if (str.length() > this.size) {
            this.attributedStrings.add(new AttributedString(str.substring(this.size)));
            this.size = str.length();
        }
        int size = this.attributedStrings.size();
        AttributedCharacterIterator[] attributedCharacterIteratorArr = new AttributedCharacterIterator[size];
        for (int i = 0; i < size; i++) {
            attributedCharacterIteratorArr[i] = this.attributedStrings.get(i).getIterator();
        }
        return new AttributedString(attributedCharacterIteratorArr).getIterator();
    }
}

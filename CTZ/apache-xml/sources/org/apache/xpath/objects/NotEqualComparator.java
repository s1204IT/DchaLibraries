package org.apache.xpath.objects;

import org.apache.xml.utils.XMLString;

class NotEqualComparator extends Comparator {
    NotEqualComparator() {
    }

    @Override
    boolean compareStrings(XMLString xMLString, XMLString xMLString2) {
        return !xMLString.equals(xMLString2);
    }

    @Override
    boolean compareNumbers(double d, double d2) {
        return d != d2;
    }
}

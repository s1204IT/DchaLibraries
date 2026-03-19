package org.apache.xpath.objects;

import org.apache.xml.utils.XMLString;

class LessThanComparator extends Comparator {
    LessThanComparator() {
    }

    @Override
    boolean compareStrings(XMLString xMLString, XMLString xMLString2) {
        return xMLString.toDouble() < xMLString2.toDouble();
    }

    @Override
    boolean compareNumbers(double d, double d2) {
        return d < d2;
    }
}

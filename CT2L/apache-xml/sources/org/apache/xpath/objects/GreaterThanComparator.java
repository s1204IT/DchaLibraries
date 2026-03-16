package org.apache.xpath.objects;

import org.apache.xml.utils.XMLString;

class GreaterThanComparator extends Comparator {
    GreaterThanComparator() {
    }

    @Override
    boolean compareStrings(XMLString s1, XMLString s2) {
        return s1.toDouble() > s2.toDouble();
    }

    @Override
    boolean compareNumbers(double n1, double n2) {
        return n1 > n2;
    }
}

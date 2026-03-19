package org.junit.experimental.theories.suppliers;

import java.util.ArrayList;
import java.util.List;
import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

public class TestedOnSupplier extends ParameterSupplier {
    @Override
    public List<PotentialAssignment> getValueSources(ParameterSignature parameterSignature) {
        ArrayList arrayList = new ArrayList();
        for (int i : ((TestedOn) parameterSignature.getAnnotation(TestedOn.class)).ints()) {
            arrayList.add(PotentialAssignment.forValue("ints", Integer.valueOf(i)));
        }
        return arrayList;
    }
}

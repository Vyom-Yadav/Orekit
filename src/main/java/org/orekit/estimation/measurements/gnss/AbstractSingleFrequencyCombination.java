/* Copyright 2002-2022 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.estimation.measurements.gnss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.CombinedObservationData;
import org.orekit.gnss.CombinedObservationDataSet;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.RinexObservationHeader;
import org.orekit.gnss.SatelliteSystem;

/** Base class for single frequency combination of measurements.
 * @author Bryan Cazabonne
 * @since 10.1
 */
public abstract class AbstractSingleFrequencyCombination implements MeasurementCombination {

    /** Type of combination of measurements. */
    private final CombinationType type;

    /** Satellite system used for the combination. */
    private final SatelliteSystem system;

    /**
     * Constructor.
     * @param type combination of measurements type
     * @param system satellite system
     */
    protected AbstractSingleFrequencyCombination(final CombinationType type, final SatelliteSystem system) {
        this.type   = type;
        this.system = system;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return type.getName();
    }

    /** {@inheritDoc} */
    @Override
    public CombinedObservationDataSet combine(final ObservationDataSet observations) {

        // Rinex file header
        final RinexObservationHeader header = observations.getHeader();
        // Rinex version to integer
        final int version = (int) header.getRinexVersion();

        // Initialize list of measurements
        final List<ObservationData> pseudoRanges = new ArrayList<>();
        final List<ObservationData> phases       = new ArrayList<>();

        // Loop on observation data to fill lists
        for (final ObservationData od : observations.getObservationData()) {
            if (!Double.isNaN(od.getValue())) {
                if (od.getObservationType().getMeasurementType() == MeasurementType.PSEUDO_RANGE) {
                    pseudoRanges.add(od);
                } else if (od.getObservationType().getMeasurementType() == MeasurementType.CARRIER_PHASE) {
                    phases.add(od);
                }
            }
        }

        // Initialize list of combined observation data
        final List<CombinedObservationData> combined = new ArrayList<>();

        for (int i = 0; i < phases.size(); i++) {
            for (int j = 0; j < pseudoRanges.size(); j++) {
                final boolean combine = isCombinationPossible(version, phases.get(i), pseudoRanges.get(j));
                if (combine) {
                    combined.add(combine(phases.get(i), pseudoRanges.get(j)));
                }
            }
        }

        return new CombinedObservationDataSet(observations.getHeader(), observations.getSatelliteSystem(),
                                              observations.getPrnNumber(), observations.getDate(),
                                              observations.getRcvrClkOffset(), combined);
    }

    /**
     * Combines observation data using a single frequency combination of measurements.
     * @param phase phase measurement
     * @param pseudoRange pseudoRange measurement
     * @return a combined observation data
     */
    public CombinedObservationData combine(final ObservationData phase, final ObservationData pseudoRange) {

        // Observation types
        final ObservationType obsType1 = phase.getObservationType();
        final ObservationType obsType2 = pseudoRange.getObservationType();

        // Frequencies
        final Frequency freq1 = obsType1.getFrequency(system);
        final Frequency freq2 = obsType2.getFrequency(system);
        // Check if the combination of measurements if performed for two different frequencies
        if (freq1 != freq2) {
            throw new OrekitException(OrekitMessages.INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS,
                                      freq1, freq2, getName());
        }

        // Measurements types
        final MeasurementType measType1 = obsType1.getMeasurementType();
        final MeasurementType measType2 = obsType2.getMeasurementType();

        // Check if measurement types are the same
        if (measType1 == measType2) {
            // If the measurement types are the same, an exception is thrown
            throw new OrekitException(OrekitMessages.INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS,
                                      measType1, measType2, getName());
        }

        // Frequency
        final double f = freq1.getMHzFrequency();

        // Combined value
        final double combinedValue = getCombinedValue(phase.getValue(), pseudoRange.getValue());

        // Combined observation data
        return new CombinedObservationData(CombinationType.PHASE_MINUS_CODE, MeasurementType.COMBINED_RANGE_PHASE,
                                           combinedValue, f, Arrays.asList(phase, pseudoRange));
    }

    /**
     * Get the combined observed value of two measurements.
     * @param phase observed value of the phase measurement
     * @param pseudoRange observed value of the range measurement
     * @return combined observed value
     */
    protected abstract double getCombinedValue(double phase, double pseudoRange);

    /**
     * Verifies if two observation data can be combine.
     * @param version Rinex file version (integer part)
     * @param phase phase measurement
     * @param pseudoRange pseudoRange measurement
     * @return true if observation data can be combined
     */
    private boolean isCombinationPossible(final int version, final ObservationData phase, final ObservationData pseudoRange) {
        // Observation types
        final ObservationType obsType1 = phase.getObservationType();
        final ObservationType obsType2 = pseudoRange.getObservationType();
        // Single-frequency combination is possible only if data frequencies are the same
        if (obsType1.getFrequency(system) == obsType2.getFrequency(system)) {
            // Switch on Rinex version
            switch (version) {
                case 2 : return true;
                case 3 : return obsType1.getSignalCode() == obsType2.getSignalCode();
                default: return false;
            }
        } else {
            // False because observation data have different frequency
            return false;
        }
    }

}

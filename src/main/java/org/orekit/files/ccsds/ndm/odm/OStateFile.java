/* Copyright 2002-2021 CS GROUP
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

package org.orekit.files.ccsds.ndm.odm;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.SpacecraftState;

/** This class gathers the general state data present in both OPM and OMM files.
 * @param <M> type of the metadata
 * @param <D> type of the data
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class OStateFile<M extends OCommonMetadata, D extends OStateData> extends ODMFile<NDMSegment<M, D>> {

    /** Gravitational coefficient to use for building Cartesian/Keplerian orbits. */
    private double mu;

    /** Create a new OPM file object.
     */
    protected OStateFile() {
        mu = Double.NaN;
    }

    /** Get the metadata from the single {@link #getSegments() segment}.
     * @return metadata from the single {@link #getSegments() segment}
     */
    public M getMetadata() {
        return getSegments().get(0).getMetadata();
    }

    /** Get the data from the single {@link #getSegments() segment}.
     * @return data from the single {@link #getSegments() segment}
     */
    public D getData() {
        return getSegments().get(0).getData();
    }

    /** Set the gravitational coefficient to use for building Cartesian/Keplerian orbits.
     * @param mu gravitational coefficient to use for building Cartesian/Keplerian orbits
     */
    public void setMu(final double mu) {
        this.mu = mu;
    }

    /** Get the gravitational coefficient to use for building Cartesian/Keplerian orbits.
     * <p>
     * This method throws an exception if the gravitational coefficient has not been set properly
     * </p>
     * @return gravitational coefficient to use for building Cartesian/Keplerian orbits
     */
    public double getMu() {
        if (Double.isNaN(mu)) {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_GM);
        }
        return mu;
    }

    /**
     * Generate a {@link CartesianOrbit} from the state vector data. If the reference frame is not
     * pseudo-inertial, an exception is raised.
     * @return the {@link CartesianOrbit} generated from the state vector information
     */
    public abstract CartesianOrbit generateCartesianOrbit();

    /** Generate a {@link KeplerianOrbit} from the Keplerian elements if hasKeplerianElements is true,
     * or from the state vector data otherwise.
     * If the reference frame is not pseudo-inertial, an exception is raised.
     * @return the {@link KeplerianOrbit} generated from the state vector information
     */
    public abstract KeplerianOrbit generateKeplerianOrbit();

    /** Generate spacecraft state from the {@link CartesianOrbit} generated by generateCartesianOrbit.
     *  Raises an exception if OPM doesn't contain spacecraft mass information.
     * @return the spacecraft state of the OPM
     */
    public SpacecraftState generateSpacecraftState() {
        return new SpacecraftState(generateCartesianOrbit(), getData().getMass());
    }

}


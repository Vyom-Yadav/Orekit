/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.drag.atmosphere.Atmosphere;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.utils.Constants;

/** Atmospheric drag contribution to the
 *  {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *  <p>
 *  The drag acceleration is computed through the acceleration model of
 *  {@link org.orekit.forces.drag.DragForce DragForce}.
 *  </p>
 *
 * @author Pascal Parraud
 */
public class DSSTAtmosphericDrag extends AbstractGaussianContribution {

    /** Threshold for the choice of the Gauss quadrature order. */
    private static final double GAUSS_THRESHOLD = 6.0e-10;

    /** Upper limit for atmospheric drag (m) . */
    private static final double ATMOSPHERE_ALTITUDE_MAX = 1000000.;

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** Spacecraft shape. */
    private final DragSensitive spacecraft;

    /** Critical distance from the center of the central body for entering/leaving the atmosphere. */
    private final double     rbar;

    /** Simple constructor assuming spherical spacecraft.
     * @param atmosphere atmospheric model
     * @param cd drag coefficient
     * @param area cross sectionnal area of satellite
     */
    public DSSTAtmosphericDrag(final Atmosphere atmosphere, final double cd,
            final double area) {
        this(atmosphere, new IsotropicDrag(area, cd));
    }

    /** Simple constructor with custom spacecraft.
     * @param atmosphere atmospheric model
     * @param spacecraft spacecraft model
     */
    public DSSTAtmosphericDrag(final Atmosphere atmosphere, final DragSensitive spacecraft) {

        //Call to the constructor from superclass using the numerical drag model as ForceModel
        super("DSST-drag-", GAUSS_THRESHOLD, new DragForce(atmosphere, spacecraft));

        this.atmosphere = atmosphere;
        this.spacecraft = spacecraft;
        this.rbar = ATMOSPHERE_ALTITUDE_MAX + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
    }

    /** Get the atmospheric model.
     * @return atmosphere model
     */
    public Atmosphere getAtmosphere() {
        return atmosphere;
    }

    /** Get the critical distance.
     *  <p>
     *  The critical distance from the center of the central body aims at
     *  defining the atmosphere entry/exit.
     *  </p>
     *  @return the critical distance from the center of the central body (m)
     */
    public double getRbar() {
        return rbar;
    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    /** {@inheritDoc} */
    protected double[] getLLimits(final SpacecraftState state, final AbstractGaussianContributionContext context) throws OrekitException {

        // AuxiliaryElements auxiliary elements related to the current orbit
        final AuxiliaryElements auxiliaryElements = context.getAuxiliaryElements();

        final double perigee = auxiliaryElements.getSma() * (1. - auxiliaryElements.getEcc());
        // Trajectory entirely out of the atmosphere
        if (perigee > rbar) {
            return new double[2];
        }
        final double apogee  = auxiliaryElements.getSma() * (1. + auxiliaryElements.getEcc());
        // Trajectory entirely within of the atmosphere
        if (apogee < rbar) {
            return new double[]{-FastMath.PI + MathUtils.normalizeAngle(state.getLv(), 0),
                                FastMath.PI + MathUtils.normalizeAngle(state.getLv(), 0)};
        }
        // Else, trajectory partialy within of the atmosphere
        final double fb = FastMath.acos(((auxiliaryElements.getSma() * (1. - auxiliaryElements.getEcc() * auxiliaryElements.getEcc()) / rbar) - 1.) / auxiliaryElements.getEcc());
        final double wW = FastMath.atan2(auxiliaryElements.getH(), auxiliaryElements.getK());
        return new double[] {wW - fb, wW + fb};
    }

    /** {@inheritDoc} */
    protected <T extends RealFieldElement<T>> T[] getLLimits(final FieldSpacecraftState<T> state,
                                                             final FieldAbstractGaussianContributionContext<T> context)
        throws OrekitException {

        // AuxiliaryElements auxiliary elements related to the current orbit
        final FieldAuxiliaryElements<T> auxiliaryElements = context.getFieldAuxiliaryElements();

        final T[] tab = MathArrays.buildArray(state.getDate().getField(), 2);

        final T perigee = auxiliaryElements.getSma().multiply(auxiliaryElements.getEcc().negate().add(1.));
        // Trajectory entirely out of the atmosphere
        if (perigee.getReal() > rbar) {
            return tab;
        }
        final T apogee  = auxiliaryElements.getSma().multiply(auxiliaryElements.getEcc().add(1.));
        // Trajectory entirely within of the atmosphere
        if (apogee.getReal() < rbar) {
            tab[0] = auxiliaryElements.normalizeAngle(state.getLv(), 0).subtract(FastMath.PI);
            tab[1] = auxiliaryElements.normalizeAngle(state.getLv(), 0).add(FastMath.PI);
            return tab;
        }
        // Else, trajectory partialy within of the atmosphere
        final T fb = FastMath.acos(((auxiliaryElements.getSma().multiply(auxiliaryElements.getEcc().multiply(auxiliaryElements.getEcc()).negate().add(1.)).divide(rbar)).subtract(1.)).divide(auxiliaryElements.getEcc()));
        final T wW = FastMath.atan2(auxiliaryElements.getH(), auxiliaryElements.getK());

        tab[0] = wW.subtract(fb);
        tab[1] = wW.add(fb);
        return tab;
    }

    /** Get spacecraft shape.
     *
     * @return spacecraft shape
     */
    public DragSensitive getSpacecraft() {
        return spacecraft;
    }

}

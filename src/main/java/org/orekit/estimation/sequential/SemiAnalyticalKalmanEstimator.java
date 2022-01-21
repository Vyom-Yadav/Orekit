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
package org.orekit.estimation.sequential;

import java.util.List;

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.filtering.kalman.extended.ExtendedKalmanFilter;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

/**
 * Implementation of an Extended Semi-analytical Kalman Filter (ESKF) to perform orbit determination.
 * <p>
 * The filter uses a {@link DSSTPropagatorBuilder}.
 * </p>
 * <p>
 * The estimated parameters are driven by {@link ParameterDriver} objects. They are of 3 different types:<ol>
 *   <li><b>Orbital parameters</b>:The position and velocity of the spacecraft, or, more generally, its orbit.<br>
 *       These parameters are retrieved from the reference trajectory propagator builder when the filter is initialized.</li>
 *   <li><b>Propagation parameters</b>: Some parameters modelling physical processes (SRP or drag coefficients).<br>
 *       They are also retrieved from the propagator builder during the initialization phase.</li>
 *   <li><b>Measurements parameters</b>: Parameters related to measurements (station biases, positions etc...).<br>
 *       They are passed down to the filter in its constructor.</li>
 * </ol>
 * <p>
 * The Kalman filter implementation used is provided by the underlying mathematical library Hipparchus.
 * All the variables seen by Hipparchus (states, covariances, measurement matrices...) are normalized
 * using a specific scale for each estimated parameters or standard deviation noise for each measurement components.
 * </p>
 *
 * @see "Folcik Z., Orbit Determination Using Modern Filters/Smoothers and Continuous Thrust Modeling,
 *       Master of Science Thesis, Department of Aeronautics and Astronautics, MIT, June, 2008."
 *
 * @see "Cazabonne B., Bayard J., Journot M., and Cefola P. J., A Semi-analytical Approach for Orbit
 *       Determination based on Extended Kalman Filter, AAS Paper 21-614, AAS/AIAA Astrodynamics
 *       Specialist Conference, Big Sky, August 2021."
 *
 * @author Julie Bayard
 * @author Bryan Cazabonne
 * @author Maxime Journot
 * @since 11.1
 */
public class SemiAnalyticalKalmanEstimator {

    /** Builders for orbit propagators. */
    private DSSTPropagatorBuilder propagatorBuilder;

    /** Kalman filter process model. */
    private final SemiAnalyticalKalmanModel processModel;

    /** Filter. */
    private final ExtendedKalmanFilter<MeasurementDecorator> filter;

    /** Observer to retrieve current estimation info. */
    private KalmanObserver observer;

    /** Kalman filter estimator constructor (package private).
     * @param decomposer decomposer to use for the correction phase
     * @param propagatorBuilder propagator builder used to evaluate the orbit.
     * @param covarianceMatrixProvider provider for process noise matrix
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    public SemiAnalyticalKalmanEstimator(final MatrixDecomposer decomposer,
                                         final DSSTPropagatorBuilder propagatorBuilder,
                                         final CovarianceMatrixProvider covarianceMatrixProvider,
                                         final ParameterDriversList estimatedMeasurementParameters,
                                         final CovarianceMatrixProvider measurementProcessNoiseMatrix) {

        this.propagatorBuilder = propagatorBuilder;
        this.observer          = null;

        // Build the process model and measurement model
        this.processModel = new SemiAnalyticalKalmanModel(propagatorBuilder, covarianceMatrixProvider,
                                                          estimatedMeasurementParameters,  measurementProcessNoiseMatrix);

        // Extended Kalman Filter of Hipparchus
        this.filter = new ExtendedKalmanFilter<>(decomposer, processModel, processModel.getEstimate());

    }

    /** Set the observer.
     * @param observer the observer
     */
    public void setObserver(final KalmanObserver observer) {
        this.observer = observer;
    }

    /** Get the current measurement number.
     * @return current measurement number
     */
    public int getCurrentMeasurementNumber() {
        return processModel.getCurrentMeasurementNumber();
    }

    /** Get the current date.
     * @return current date
     */
    public AbsoluteDate getCurrentDate() {
        return processModel.getCurrentDate();
    }

    /** Get the "physical" estimated state (i.e. not normalized)
     * @return the "physical" estimated state
     */
    public RealVector getPhysicalEstimatedState() {
        return processModel.getPhysicalEstimatedState();
    }

    /** Get the "physical" estimated covariance matrix (i.e. not normalized)
     * @return the "physical" estimated covariance matrix
     */
    public RealMatrix getPhysicalEstimatedCovarianceMatrix() {
        return processModel.getPhysicalEstimatedCovarianceMatrix();
    }

    /** Get the orbital parameters supported by this estimator.
     * <p>
     * If there are more than one propagator builder, then the names
     * of the drivers have an index marker in square brackets appended
     * to them in order to distinguish the various orbits. So for example
     * with one builder generating Keplerian orbits the names would be
     * simply "a", "e", "i"... but if there are several builders the
     * names would be "a[0]", "e[0]", "i[0]"..."a[1]", "e[1]", "i[1]"...
     * </p>
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return orbital parameters supported by this estimator
     */
    public ParameterDriversList getOrbitalParametersDrivers(final boolean estimatedOnly) {

        final ParameterDriversList estimated = new ParameterDriversList();
        for (final ParameterDriver driver : propagatorBuilder.getOrbitalParametersDrivers().getDrivers()) {
            if (driver.isSelected() || !estimatedOnly) {
                driver.setName(driver.getName());
                estimated.add(driver);
            }
        }
        return estimated;
    }

    /** Get the propagator parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return propagator parameters supported by this estimator
     */
    public ParameterDriversList getPropagationParametersDrivers(final boolean estimatedOnly) {

        final ParameterDriversList estimated = new ParameterDriversList();
        for (final DelegatingDriver delegating : propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
            if (delegating.isSelected() || !estimatedOnly) {
                for (final ParameterDriver driver : delegating.getRawDrivers()) {
                    estimated.add(driver);
                }
            }
        }
        return estimated;
    }

    /** Get the list of estimated measurements parameters.
     * @return the list of estimated measurements parameters
     */
    public ParameterDriversList getEstimatedMeasurementsParameters() {
        return processModel.getEstimatedMeasurementsParameters();
    }

    /** Process a single measurement.
     * <p>
     * Update the filter with the new measurement by calling the estimate method.
     * </p>
     * @param observedMeasurements the list of measurements to process
     * @return estimated propagators
     */
    public DSSTPropagator processMeasurements(final List<ObservedMeasurement<?>> observedMeasurements) {
        try {
            processModel.setObserver(observer);
            return processModel.processMeasurements(observedMeasurements, filter);
        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        }
    }

}


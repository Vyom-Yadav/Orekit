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

import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.OrbitDeterminationPropagatorBuilder;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.utils.ParameterDriversList;

/** Class defining the process model dynamics to use with a {@link KalmanEstimator}.
 * @author Romain Gerbaud
 * @author Maxime Journot
 * @since 9.2
 */
public class KalmanModel extends AbstractKalmanModel {

    /** Kalman process model constructor.
     * @param propagatorBuilders propagators builders used to evaluate the orbits.
     * @param covarianceMatricesProviders providers for covariance matrices
     * @param estimatedMeasurementParameters measurement parameters to estimate
     * @param measurementProcessNoiseMatrix provider for measurement process noise matrix
     */
    public KalmanModel(final List<OrbitDeterminationPropagatorBuilder> propagatorBuilders,
                       final List<CovarianceMatrixProvider> covarianceMatricesProviders,
                       final ParameterDriversList estimatedMeasurementParameters,
                       final CovarianceMatrixProvider measurementProcessNoiseMatrix) {
        // call super constructor
        super(propagatorBuilders, covarianceMatricesProviders, estimatedMeasurementParameters,
              measurementProcessNoiseMatrix, new JacobiansMapper[propagatorBuilders.size()]);
    }

    /** {@inheritDoc} */
    @Override
    protected void updateReferenceTrajectories(final Propagator[] propagators,
                                               final PropagationType pType,
                                               final PropagationType sType) {

        // Update the reference trajectory propagator
        setReferenceTrajectories(propagators);

        // Jacobian harvesters
        final MatricesHarvester[] harvesters = new MatricesHarvester[propagators.length];

        for (int k = 0; k < propagators.length; ++k) {
            // Link the partial derivatives to this new propagator
            final String equationName = KalmanEstimator.class.getName() + "-derivatives-" + k;
            harvesters[k] = getReferenceTrajectories()[k].setupMatricesComputation(equationName, null, null);
        }

        // Update Jacobian harvesters
        setHarvesters(harvesters);

    }

}

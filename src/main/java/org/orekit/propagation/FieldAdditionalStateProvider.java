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
package org.orekit.propagation;

import org.hipparchus.CalculusFieldElement;

/** This interface represents providers for additional state data beyond {@link SpacecraftState}.
 * <p>
 * {@link FieldPropagator Propagators} generate {@link FieldSpacecraftState states} that contain at
 * least orbit, attitude, and mass. These states may however also contain {@link
 * FieldSpacecraftState#addAdditionalState(String, CalculusFieldElement...) additional states}. Instances of classes
 * implementing this interface are intended to be registered to propagators so they can add these
 * additional states incrementally after having computed the basic components
 * (orbit, attitude and mass).
 * </p>
 * <p>
 * Some additional states may depend on previous additional states to
 * be already available the before they can be computed. It may even be impossible to compute some
 * of these additional states at some time if they depend on conditions that are fulfilled only
 * after propagation as started or some event has occurred. As the propagator builds the complete
 * state incrementally, looping over the registered providers, it must call their {@link
 * #getAdditionalState(FieldSpacecraftState) getAdditionalState} methods in an order that fulfill these dependencies that
 * may be time-dependent and are not related to the order in which the providers are registered to
 * the propagator. This reordering is performed each time the complete state is built, using a yield
 * mechanism. The propagator first push all providers in a stack and then empty the stack, one provider
 * at a time, taking care to select only providers that do <em>not</em> {@link
 * #yield(FieldSpacecraftState) yield} when asked. Consider for example a case where providers A, B and C
 * have been registered and provider B needs in fact the additional state generated by state C. Then
 * when a complete state is built, the propagator puts the three providers, and then starts the incremental
 * generation of additional states. It first checks provider A which does not yield so it is popped from
 * the stack and the additional state it generates is added. Then provider B is checked, but it yields
 * because state from provider C is not yet available. So propagator checks provider C which does not
 * yield, so it is popped out of the stack and applied. At this stage, provider B is the only remaining one
 * in the stack, so it is checked again, but this time it does not yield because the state from provider
 * C is available as it has just been added, so provider B is popped from the stack and applied. The stack
 * is now empty and the propagator can return the completed state.
 * </p>
 * <p>
 * It is possible that at some stages in the propagation, a subset of the providers registered to a
 * propagator all yied and cannot {@link #getAdditionalState(FieldSpacecraftState) retrieve} their additional
 * state. This happens for example during the initialization phase of a propagator that
 * compute State Transition Matrices or Jacobian matrices. These features are managed as secondary equations
 * in the ODE integrator, and initialized after the primary equations (which correspond to orbit) have
 * been initialized. So when the primary equation are initialized, the providers that depend on the secondary
 * state will all yield. This behavior is expected. Another case occurs when users set up additional states
 * that induces dependency loop (state A depending on state B which depends on state C which depends on
 * state A). In this case, the three corresponding providers will wait for each other and indefinitely yield.
 * This second case is a deadlock and results from a design error of the additional states management at
 * application level. The propagator cannot know it in advance if as subset of providers that all yield is
 * normal or not. So at propagator level, when either situation is detected, the propagator just give up and
 * returns the most complete state it was able to compute, without generating any error. Errors will indeed
 * not be triggered in the first case (once the primary equations have been initialized, the secondary
 * equations will be initialized too), and they will be triggered in the second case as soon as user attempts
 * to retrieve an additional state that was not added.
 * </p>
 * @see org.orekit.propagation.FieldPropagator
 * @see org.orekit.propagation.integration.FieldAdditionalDerivativesProvider
 * @author Luc Maisonobe
 */
public interface FieldAdditionalStateProvider<T extends CalculusFieldElement<T>> {

    /** Get the name of the additional state.
     * @return name of the additional state
     */
    String getName();

    /** Check if this provider should yield so another provider has an opportunity to add missing parts.
     * <p>
     * Decision to yield is often based on an additional state being {@link FieldSpacecraftState#hasAdditionalState(String)
     * already available} in the provided {@code state} (but it could theoretically also depend on
     * an additional state derivative being {@link FieldSpacecraftState#hasAdditionalStateDerivative(String)
     * already available}, or any other criterion). If for example a provider needs the state transition
     * matrix, it could implement this method as:
     * </p>
     * <pre>{@code
     * public boolean yield(final FieldSpacecraftState state) {
     *     return !state.getAdditionalStates().containsKey("STM");
     * }
     * }</pre>
     * <p>
     * The default implementation returns {@code false}, meaning that state data can be
     * {@link #getAdditionalState(FieldSpacecraftState) generated} immediately.
     * </p>
     * @param state state to handle
     * @return true if this provider should yield so another provider has an opportunity to add missing parts
     * as the state is incrementally built up
     * @since 11.1
     */
    default boolean yield(FieldSpacecraftState<T> state) {
        return false;
    }

    /** Get the additional state.
     * @param state spacecraft state to which additional state should correspond
     * @return additional state corresponding to spacecraft state
     */
    T[] getAdditionalState(FieldSpacecraftState<T> state);

}

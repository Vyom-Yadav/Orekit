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
package org.orekit.attitudes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/** This classes manages a sequence of different attitude providers that are activated
 * in turn according to switching events.
 * <p>Only one attitude provider in the sequence is in an active state. When one of
 * the switch event associated with the active provider occurs, the active provider becomes
 * the one specified with the event. A simple example is a provider for the sun lighted part
 * of the orbit and another provider for the eclipse time. When the sun lighted provider is active,
 * the eclipse entry event is checked and when it occurs the eclipse provider is activated.
 * When the eclipse provider is active, the eclipse exit event is checked and when it occurs
 * the sun lighted provider is activated again. This sequence is a simple loop.</p>
 * <p>An active attitude provider may have several switch events and next provider settings, leading
 * to different activation patterns depending on which events are triggered first. An example
 * of this feature is handling switches to safe mode if some contingency condition is met, in
 * addition to the nominal switches that correspond to proper operations. Another example
 * is handling of maneuver mode.
 * <p>
 * Note that this attitude provider is stateful, it keeps in memory the sequence of active
 * underlying providers with their switch dates and the transitions from one provider to
 * the other. This implies that this provider should <em>not</em> be shared among different
 * propagators at the same time, each propagator should use its own instance of this provider.
 * <p>
 * The sequence kept in memory is reset when {@link #resetActiveProvider(AttitudeProvider)}
 * is called, and only the specify provider is kept. The sequence is also partially
 * reset each time a propagation starts. If a new propagation is started after a first
 * propagation has been run, all the already computed switches that occur after propagation
 * start for forward propagation or before propagation start for backward propagation will
 * be erased. New switches will be computed and applied properly according to the new
 * propagation settings. The already computed switches that are not in covered are kept
 * in memory. This implies that if a propagation is interrupted and restarted in the
 * same direction, then attitude switches will remain in place, ensuring that even if the
 * interruption occurred in the middle of an attitude transition the second propagation will
 * properly complete the transition that was started by the first propagator.
 * </p>
 * @author Luc Maisonobe
 * @since 5.1
 */
public class AttitudesSequence implements AttitudeProvider {

    /** Providers that have been activated. */
    private transient TimeSpanMap<AttitudeProvider> activated;

    /** Switching events list. */
    private final List<Switch<?>> switches;

    /** Constructor for an initially empty sequence.
     */
    public AttitudesSequence() {
        activated = null;
        switches  = new ArrayList<Switch<?>>();
    }

    /** Reset the active provider.
     * <p>
     * Calling this method clears all already seen switch history,
     * so it should <em>not</em> be used during the propagation itself,
     * it is intended to be used only at start
     * </p>
     * @param provider provider to activate
     */
    public void resetActiveProvider(final AttitudeProvider provider) {
        activated = new TimeSpanMap<AttitudeProvider>(provider);
    }

    /** Register all wrapped switch events to the propagator.
     * <p>
     * This method must be called once before propagation, after the
     * switching conditions have been set up by calls to {@link
     * #addSwitchingCondition(AttitudeProvider, AttitudeProvider, EventDetector,
     * boolean, boolean, double, AngularDerivativesFilter, SwitchHandler)
     * addSwitchingCondition}.
     * </p>
     * @param propagator propagator that will handle the events
     */
    public void registerSwitchEvents(final Propagator propagator) {
        for (final Switch<?> s : switches) {
            propagator.addEventDetector(s);
        }
    }

    /** Register all wrapped switch events to the propagator.
     * <p>
     * This method must be called once before propagation, after the
     * switching conditions have been set up by calls to {@link
     * #addSwitchingCondition(AttitudeProvider, AttitudeProvider, EventDetector,
     * boolean, boolean, double, AngularDerivativesFilter, SwitchHandler)
     * addSwitchingCondition}.
     * </p>
     * @param field field to which the elements belong
     * @param propagator propagator that will handle the events
     * @param <T> type of the field elements
     */
    public <T extends CalculusFieldElement<T>> void registerSwitchEvents(final Field<T> field, final FieldPropagator<T> propagator) {
        for (final Switch<?> sw : switches) {
            propagator.addEventDetector(new FieldEventDetector<T>() {

                /** {@inheritDoc} */
                @Override
                public void init(final FieldSpacecraftState<T> s0,
                                 final FieldAbsoluteDate<T> t) {
                    sw.init(s0.toSpacecraftState(), t.toAbsoluteDate());
                }

                /** {@inheritDoc} */
                @Override
                public T g(final FieldSpacecraftState<T> s) {
                    return field.getZero().add(sw.g(s.toSpacecraftState()));
                }

                /** {@inheritDoc} */
                @Override
                public T getThreshold() {
                    return field.getZero().add(sw.getThreshold());
                }

                /** {@inheritDoc} */
                @Override
                public T getMaxCheckInterval() {
                    return field.getZero().add(sw.getMaxCheckInterval());
                }

                /** {@inheritDoc} */
                @Override
                public int getMaxIterationCount() {
                    return sw.getMaxIterationCount();
                }

                /** {@inheritDoc} */
                @Override
                public Action eventOccurred(final FieldSpacecraftState<T> s, final boolean increasing) {
                    return sw.eventOccurred(s.toSpacecraftState(), increasing);
                }

                /** {@inheritDoc} */
                @Override
                public FieldSpacecraftState<T> resetState(final FieldSpacecraftState<T> oldState) {
                    return new FieldSpacecraftState<>(field, sw.resetState(oldState.toSpacecraftState()));
                }

            });
        }
    }

    /** Add a switching condition between two attitude providers.
     * <p>
     * The {@code past} and {@code future} attitude providers are defined with regard
     * to the natural flow of time. This means that if the propagation is forward, the
     * propagator will switch from {@code past} provider to {@code future} provider at
     * event occurrence, but if the propagation is backward, the propagator will switch
     * from {@code future} provider to {@code past} provider at event occurrence. The
     * transition between the two attitude laws is not instantaneous, the switch event
     * defines the start of the transition (i.e. when leaving the {@code past} attitude
     * law and entering the interpolated transition law). The end of the transition
     * (i.e. when leaving the interpolating transition law and entering the {@code future}
     * attitude law) occurs at switch time plus {@code transitionTime}.
     * </p>
     * <p>
     * An attitude provider may have several different switch events associated to
     * it. Depending on which event is triggered, the appropriate provider is
     * switched to.
     * </p>
     * <p>
     * The switch events specified here must <em>not</em> be registered to the
     * propagator directly. The proper way to register these events is to
     * call {@link #registerSwitchEvents(Propagator)} once after all switching
     * conditions have been set up. The reason for this is that the events will
     * be wrapped before being registered.
     * </p>
     * <p>
     * If the underlying detector has an event handler associated to it, this handler
     * will be triggered (i.e. its {@link org.orekit.propagation.events.handlers.EventHandler#eventOccurred(SpacecraftState,
     * EventDetector, boolean) eventOccurred} method will be called), <em>regardless</em>
     * of the event really triggering an attitude switch or not. As an example, if an
     * eclipse detector is used to switch from day to night attitude mode when entering
     * eclipse, with {@code switchOnIncrease} set to {@code false} and {@code switchOnDecrease}
     * set to {@code true}. Then a handler set directly at eclipse detector level would
     * be triggered at both eclipse entry and eclipse exit, but attitude switch would
     * occur <em>only</em> at eclipse entry. Note that for the sake of symmetry, the
     * transition start and end dates should match for both forward and backward propagation.
     * This implies that for backward propagation, we have to compensate for the {@code
     * transitionTime} when looking for the event. An unfortunate consequence is that the
     * {@link org.orekit.propagation.events.handlers.EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)
     * eventOccurred} method may appear to be called out of sync with respect to the
     * propagation (it will be called when propagator reaches transition end, despite it
     * refers to transition start, as per {@code transitionTime} compensation), and if the
     * method returns {@link Action#STOP}, it will stop at the end of the
     * transition instead of at the start. For these reasons, it is not recommended to
     * set up an event handler for events that are used to switch attitude. If an event
     * handler is needed for other purposes, a second handler should be registered to
     * the propagator rather than relying on the side effects of attitude switches.
     * </p>
     * <p>
     * The smoothness of the transition between past and future attitude laws can be tuned
     * using the {@code transitionTime} and {@code transitionFilter} parameters. The {@code
     * transitionTime} parameter specifies how much time is spent to switch from one law to
     * the other law. It should be larger than the event {@link EventDetector#getThreshold()
     * convergence threshold} in order to ensure attitude continuity. The {@code
     * transitionFilter} parameter specifies the attitude time derivatives that should match
     * at the boundaries between past attitude law and transition law on one side, and
     * between transition law and future law on the other side.
     * {@link AngularDerivativesFilter#USE_R} means only the rotation should be identical,
     * {@link AngularDerivativesFilter#USE_RR} means both rotation and rotation rate
     * should be identical, {@link AngularDerivativesFilter#USE_RRA} means both rotation,
     * rotation rate and rotation acceleration should be identical. During the transition,
     * the attitude law is computed by interpolating between past attitude law at switch time
     * and future attitude law at current intermediate time.
     * </p>
     * @param past attitude provider applicable for times in the switch event occurrence past
     * @param future attitude provider applicable for times in the switch event occurrence future
     * @param switchEvent event triggering the attitude providers switch
     * @param switchOnIncrease if true, switch is triggered on increasing event
     * @param switchOnDecrease if true, switch is triggered on decreasing event
     * @param transitionTime duration of the transition between the past and future attitude laws
     * @param transitionFilter specification of transition law time derivatives that
     * should match past and future attitude laws
     * @param handler handler to call for notifying when switch occurs (may be null)
     * @param <T> class type for the switch event
     * @since 7.1
     */
    public <T extends EventDetector> void addSwitchingCondition(final AttitudeProvider past,
                                                                final AttitudeProvider future,
                                                                final T switchEvent,
                                                                final boolean switchOnIncrease,
                                                                final boolean switchOnDecrease,
                                                                final double transitionTime,
                                                                final AngularDerivativesFilter transitionFilter,
                                                                final SwitchHandler handler) {

        // safety check, for ensuring attitude continuity
        if (transitionTime < switchEvent.getThreshold()) {
            throw new OrekitException(OrekitMessages.TOO_SHORT_TRANSITION_TIME_FOR_ATTITUDES_SWITCH,
                                      transitionTime, switchEvent.getThreshold());
        }

        // if it is the first switching condition, assume first active law is the past one
        if (activated == null) {
            resetActiveProvider(past);
        }

        // add the switching condition
        switches.add(new Switch<T>(switchEvent, switchOnIncrease, switchOnDecrease,
                                   past, future, transitionTime, transitionFilter, handler));

    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {
        return activated.get(date).getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                        final FieldAbsoluteDate<T> date,
                                                                        final Frame frame) {
        return activated.get(date.toAbsoluteDate()).getAttitude(pvProv, date, frame);
    }

    /** Switch specification.
     * @param <T> class type for the generic version
     */
    private class Switch<T extends EventDetector> implements EventDetector {

        /** Event. */
        private final T event;

        /** Event direction triggering the switch. */
        private final boolean switchOnIncrease;

        /** Event direction triggering the switch. */
        private final boolean switchOnDecrease;

        /** Attitude provider applicable for times in the switch event occurrence past. */
        private final AttitudeProvider past;

        /** Attitude provider applicable for times in the switch event occurrence future. */
        private final AttitudeProvider future;

        /** Duration of the transition between the past and future attitude laws. */
        private final double transitionTime;

        /** Order at which the transition law time derivatives should match past and future attitude laws. */
        private final AngularDerivativesFilter transitionFilter;

        /** Handler to call for notifying when switch occurs (may be null). */
        private final SwitchHandler switchHandler;

        /** Propagation direction. */
        private boolean forward;

        /** Simple constructor.
         * @param event event
         * @param switchOnIncrease if true, switch is triggered on increasing event
         * @param switchOnDecrease if true, switch is triggered on decreasing event
         * otherwise switch is triggered on decreasing event
         * @param past attitude provider applicable for times in the switch event occurrence past
         * @param future attitude provider applicable for times in the switch event occurrence future
         * @param transitionTime duration of the transition between the past and future attitude laws
         * @param transitionFilter order at which the transition law time derivatives
         * should match past and future attitude laws
         * @param switchHandler handler to call for notifying when switch occurs (may be null)
         */
        Switch(final T event,
               final boolean switchOnIncrease, final boolean switchOnDecrease,
               final AttitudeProvider past, final AttitudeProvider future,
               final double transitionTime, final AngularDerivativesFilter transitionFilter,
               final SwitchHandler switchHandler) {
            this.event            = event;
            this.switchOnIncrease = switchOnIncrease;
            this.switchOnDecrease = switchOnDecrease;
            this.past             = past;
            this.future           = future;
            this.transitionTime   = transitionTime;
            this.transitionFilter = transitionFilter;
            this.switchHandler    = switchHandler;
        }

        /** {@inheritDoc} */
        @Override
        public double getThreshold() {
            return event.getThreshold();
        }

        /** {@inheritDoc} */
        @Override
        public double getMaxCheckInterval() {
            return event.getMaxCheckInterval();
        }

        /** {@inheritDoc} */
        @Override
        public int getMaxIterationCount() {
            return event.getMaxIterationCount();
        }

        /** {@inheritDoc} */
        public void init(final SpacecraftState s0,
                         final AbsoluteDate t) {

            // reset the transition parameters (this will be done once for each switch,
            //  despite doing it only once would have sufficient; its not really a problem)
            forward = t.durationFrom(s0.getDate()) >= 0.0;
            if (activated.getSpansNumber() > 1) {
                // remove transitions that will be overridden during upcoming propagation
                if (forward) {
                    activated = activated.extractRange(AbsoluteDate.PAST_INFINITY, s0.getDate().shiftedBy(transitionTime));
                } else {
                    activated = activated.extractRange(s0.getDate().shiftedBy(-transitionTime), AbsoluteDate.FUTURE_INFINITY);
                }
            }

            // initialize the underlying event
            event.init(s0, t);

        }

        /** {@inheritDoc} */
        public double g(final SpacecraftState s) {
            return event.g(forward ? s : s.shiftedBy(-transitionTime));
        }

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final boolean increasing) {

            final AbsoluteDate date = s.getDate();
            if (activated.get(date) == (forward ? past : future) &&
                (increasing && switchOnIncrease || !increasing && switchOnDecrease)) {

                if (forward) {

                    // prepare transition
                    final AbsoluteDate transitionEnd = date.shiftedBy(transitionTime);
                    activated.addValidAfter(new TransitionProvider(s.getAttitude(), transitionEnd), date, false);

                    // prepare future law after transition
                    activated.addValidAfter(future, transitionEnd, false);

                    // notify about the switch
                    if (switchHandler != null) {
                        switchHandler.switchOccurred(past, future, s);
                    }

                    return event.eventOccurred(s, increasing);

                } else {

                    // estimate state at transition start, according to the past attitude law
                    final Orbit     sOrbit    = s.getOrbit().shiftedBy(-transitionTime);
                    final Attitude  sAttitude = past.getAttitude(sOrbit, sOrbit.getDate(), sOrbit.getFrame());
                    SpacecraftState sState    = new SpacecraftState(sOrbit, sAttitude, s.getMass());
                    for (final DoubleArrayDictionary.Entry entry : s.getAdditionalStatesValues().getData()) {
                        sState = sState.addAdditionalState(entry.getKey(), entry.getValue());
                    }

                    // prepare transition
                    activated.addValidBefore(new TransitionProvider(sAttitude, date), date, false);

                    // prepare past law before transition
                    activated.addValidBefore(past, sOrbit.getDate(), false);

                    // notify about the switch
                    if (switchHandler != null) {
                        switchHandler.switchOccurred(future, past, sState);
                    }

                    return event.eventOccurred(sState, increasing);

                }

            } else {
                // trigger the underlying event despite no attitude switch occurred
                return event.eventOccurred(s, increasing);
            }

        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final SpacecraftState oldState) {
            // delegate to underlying event
            return event.resetState(oldState);
        }

        /** Provider for transition phases.
         * @since 9.2
         */
        private class TransitionProvider implements AttitudeProvider {

            /** Attitude at preceding transition. */
            private final Attitude transitionPreceding;

            /** Date of final switch to following attitude law. */
            private final AbsoluteDate transitionEnd;

            /** Simple constructor.
             * @param transitionPreceding attitude at preceding transition
             * @param transitionEnd date of final switch to following attitude law
             */
            TransitionProvider(final Attitude transitionPreceding, final AbsoluteDate transitionEnd) {
                this.transitionPreceding = transitionPreceding;
                this.transitionEnd       = transitionEnd;
            }

            /** {@inheritDoc} */
            public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                        final AbsoluteDate date, final Frame frame) {

                // interpolate between the two boundary attitudes
                final TimeStampedAngularCoordinates start =
                                transitionPreceding.withReferenceFrame(frame).getOrientation();
                final TimeStampedAngularCoordinates end =
                                future.getAttitude(pvProv, transitionEnd, frame).getOrientation();
                final TimeStampedAngularCoordinates interpolated =
                                TimeStampedAngularCoordinates.interpolate(date, transitionFilter,
                                                                          Arrays.asList(start, end));

                return new Attitude(frame, interpolated);

            }

            /** {@inheritDoc} */
            public <S extends CalculusFieldElement<S>> FieldAttitude<S> getAttitude(final FieldPVCoordinatesProvider<S> pvProv,
                                                                                final FieldAbsoluteDate<S> date,
                                                                                final Frame frame) {

                // interpolate between the two boundary attitudes
                final TimeStampedFieldAngularCoordinates<S> start =
                                new TimeStampedFieldAngularCoordinates<>(date.getField(),
                                                                         transitionPreceding.withReferenceFrame(frame).getOrientation());
                final TimeStampedFieldAngularCoordinates<S> end =
                                future.getAttitude(pvProv,
                                                   new FieldAbsoluteDate<>(date.getField(), transitionEnd),
                                                   frame).getOrientation();
                final TimeStampedFieldAngularCoordinates<S> interpolated =
                                TimeStampedFieldAngularCoordinates.interpolate(date, transitionFilter,
                                                                               Arrays.asList(start, end));

                return new FieldAttitude<>(frame, interpolated);
            }

        }

    }

    /** Interface for attitude switch notifications.
     * <p>
     * This interface is intended to be implemented by users who want to be
     * notified when an attitude switch occurs.
     * </p>
     * @since 7.1
     */
    public interface SwitchHandler {

        /** Method called when attitude is switched from one law to another law.
         * @param preceding attitude law used preceding the switch (i.e. in the past
         * of the switch event for a forward propagation, or in the future
         * of the switch event for a backward propagation)
         * @param following attitude law used following the switch (i.e. in the future
         * of the switch event for a forward propagation, or in the past
         * of the switch event for a backward propagation)
         * @param state state at switch time (with attitude computed using the {@code preceding} law)
         */
        void switchOccurred(AttitudeProvider preceding, AttitudeProvider following, SpacecraftState state);

    }

}

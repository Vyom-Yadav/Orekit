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
package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.stream.Collectors;

import org.orekit.files.ccsds.definitions.DutyCycleType;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.utils.units.Unit;


/** Keys for {@link ManeuverHistoryMetadata maneuver history container} entries.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum ManeuverHistoryMetadataKey {

    /** Comment entry. */
    COMMENT((token, context, container) ->
            token.getType() == TokenType.ENTRY ? container.addComment(token.getContentAsNormalizedString()) : true),

    /** Maneuver identification number. */
    MAN_ID((token, context, container) -> token.processAsNormalizedString(container::setManID)),

    /** Identification number of previous maneuver. */
    MAN_PREV_ID((token, context, container) -> token.processAsNormalizedString(container::setManPrevID)),

    /** Identification number of next maneuver. */
    MAN_NEXT_ID((token, context, container) -> token.processAsNormalizedString(container::setManNextID)),

    /** Basis of this maneuver history data. */
    MAN_BASIS((token, context, container) -> token.processAsNormalizedString(container::setManBasis)),

    /** Identification number of the orbit determination or simulation upon which this maneuver is based.*/
    MAN_BASIS_ID((token, context, container) -> token.processAsNormalizedString(container::setManBasisID)),

    /** Identifier of the device used for this maneuver.*/
    MAN_DEVICE_ID((token, context, container) -> token.processAsNormalizedString(container::setManDeviceID)),

    /** Completion time of previous maneuver. */
    MAN_PREV_EPOCH((token, context, container) -> token.processAsDate(container::setManPrevEpoch, context)),

    /** Start time of next maneuver. */
    MAN_NEXT_EPOCH((token, context, container) -> token.processAsDate(container::setManNextEpoch, context)),

    /** Purposes of the maneuver. */
    MAN_PURPOSE((token, context, container) -> token.processAsNormalizedList(container::setManPurpose)),

    /** Prediction source on which this maneuver is based. */
    MAN_PRED_SOURCE((token, context, container) -> token.processAsNormalizedString(container::setManPredSource)),

    /** Reference frame of the maneuver. */
    MAN_REF_FRAME((token, context, container) -> token.processAsFrame(container::setManReferenceFrame, context, true, true, false)),

    /** Epoch of the {@link #COV_REF_FRAME orbit reference frame}. */
    MAN_FRAME_EPOCH((token, context, container) -> token.processAsDate(container::setManFrameEpoch, context)),

    /** Origin of maneuver gravitational assist body. */
    GRAV_ASSIST_NAME((token, context, container) -> token.processAsCenter(container::setGravitationalAssist,
                                                                         context.getDataContext().getCelestialBodies())),

    /** Type of duty cycle. */
    DC_TYPE((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                container.setDcType(DutyCycleType.valueOf(token.getContentAsUppercaseString()));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
        return true;
    }),

    /** Start time of duty cycle-based maneuver window. */
    DC_WIN_OPEN((token, context, container) -> token.processAsDate(container::setDcWindowOpen, context)),

    /** Start time of duty cycle-based maneuver window. */
    DC_WIN_CLOSE((token, context, container) -> token.processAsDate(container::setDcWindowClose, context)),

    /** Minimum number of "ON" duty cycles. */
    DC_MIN_CYCLES((token, context, container) -> token.processAsInteger(container::setDcMinCycles)),

    /** Maximum number of "ON" duty cycles. */
    DC_MAX_CYCLES((token, context, container) -> token.processAsInteger(container::setDcMaxCycles)),

    /** Start time of initial duty cycle-based maneuver execution. */
    DC_EXEC_START((token, context, container) -> token.processAsDate(container::setDcExecStart, context)),

    /** End time of final duty cycle-based maneuver execution. */
    DC_EXEC_STOP((token, context, container) -> token.processAsDate(container::setDcExecStop, context)),

    /** Duty cycle thrust reference time. */
    DC_REF_TIME((token, context, container) -> token.processAsDate(container::setDcRefTime, context)),

    /** Duty cycle pulse "ON" duration. */
    DC_TIME_PULSE_DURATION((token, context, container) -> token.processAsDouble(Unit.SECOND, container::setDcTimePulseDuration)),

    /** Duty cycle elapsed time between start of a pulse and start of next pulse. */
    DC_TIME_PULSE_PERIOD((token, context, container) -> token.processAsDouble(Unit.SECOND, container::setDcTimePulsePeriod)),

    /** Reference direction for triggering duty cycle. */
    DC_REF_DIR((token, context, container) -> token.processAsVector(container::setDcRefDir)),

    /** Spacecraft body frame in which {@link #dcBodyTrigger} is specified. */
    DC_BODY_FRAME((token, context, container) -> token.processAsFrame(f -> container.setDcBodyFrame(f.asSpacecraftBodyFrame()),
                                                                     context, false, false, true)),

    /** Direction in {@link #dcBodyFrame body frame} for triggering duty cycle. */
    DC_BODY_TRIGGER((token, context, container) -> token.processAsVector(container::setDcBodyTrigger)),

    /** Phase angle of pulse start. */
    DC_PA_START_ANGLE((token, context, container) -> token.processAsAngle(container::setDcPhaseStartAngle)),

    /** Phase angle of pulse stop. */
    DC_PA_STOP_ANGLE((token, context, container) -> token.processAsAngle(container::setDcPhaseStopAngle)),

    /** Maneuver elements of information. */
    MAN_COMPOSITION((token, context, container) -> {
        if (token.getType() == TokenType.ENTRY) {
            try {
                container.setManComposition(token.getContentAsNormalizedList().
                                            stream().
                                            map(s -> s.replace(' ', '_')).
                                            map(s -> ManeuverFieldType.valueOf(s)).
                                            collect(Collectors.toList()));
            } catch (IllegalArgumentException iae) {
                throw token.generateException(iae);
            }
        }
        return true;
    }),

    /** SI units for each elements of the maneuver. */
    MAN_UNITS((token, context, container) -> token.processAsUnitList(container::setManUnits));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    ManeuverHistoryMetadataKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process an token.
     * @param token token to process
     * @param context context binding
     * @param container container to fill
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ContextBinding context, final ManeuverHistoryMetadata container) {
        return processor.process(token, context, container);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param context context binding
         * @param container container to fill
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ContextBinding context, ManeuverHistoryMetadata container);
    }

}

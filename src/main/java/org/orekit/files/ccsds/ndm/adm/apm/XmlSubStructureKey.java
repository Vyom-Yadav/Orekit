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
package org.orekit.files.ccsds.ndm.adm.apm;

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;

/** Keywords for APM data sub-structure in XML files.
 * @author Luc Maisonobe
 * @since 11.0
 */
enum XmlSubStructureKey {

    /** General comment. */
    COMMENT((token, parser) -> token.getType() == TokenType.ENTRY ? parser.addGeneralComment(token.getContentAsNormalizedString()) : true),

    /** Quaternion section. */
    quaternionState((token, parser) -> parser.manageQuaternionSection(token.getType() == TokenType.START)),

    /** Euler elements / three axis stabilized section. */
    eulerElementsThree((token, parser) -> parser.manageEulerElementsThreeSection(token.getType() == TokenType.START)),

    /** Euler elements /spin stabilized section. */
    eulerElementsSpin((token, parser) -> parser.manageEulerElementsSpinSection(token.getType() == TokenType.START)),

    /** Spacecraft parameters section. */
    spacecraftParameters((token, parser) -> parser.manageSpacecraftParametersSection(token.getType() == TokenType.START)),

    /** Maneuver parameters section. */
    maneuverParameters((token, parser) -> parser.manageManeuverParametersSection(token.getType() == TokenType.START));

    /** Processing method. */
    private final TokenProcessor processor;

    /** Simple constructor.
     * @param processor processing method
     */
    XmlSubStructureKey(final TokenProcessor processor) {
        this.processor = processor;
    }

    /** Process one token.
     * @param token token to process
     * @param parser APM file parser
     * @return true of token was accepted
     */
    public boolean process(final ParseToken token, final ApmParser parser) {
        return processor.process(token, parser);
    }

    /** Interface for processing one token. */
    interface TokenProcessor {
        /** Process one token.
         * @param token token to process
         * @param parser APM file parser
         * @return true of token was accepted
         */
        boolean process(ParseToken token, ApmParser parser);
    }

}

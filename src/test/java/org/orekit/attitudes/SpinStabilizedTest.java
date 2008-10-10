/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.SolarSystemBody;
import org.orekit.data.DataDirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;

public class SpinStabilizedTest extends TestCase {

    public SpinStabilizedTest(String name) {
        super(name);
    }

    public void testBBQMode() throws OrekitException {
        CelestialBody sun = SolarSystemBody.getSun();
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             UTCScale.getInstance());
        double rate = 2.0 * Math.PI / (12 * 60);
        AttitudeLaw bbq =
            new SpinStabilized(new CelestialBodyPointed(Frame.getEME2000(), sun, Vector3D.PLUS_K,
                                     Vector3D.PLUS_I, Vector3D.PLUS_K),
                               date, Vector3D.PLUS_K, rate);
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Attitude attitude = bbq.getState(date, pv, Frame.getEME2000());
        Vector3D xDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        assertEquals(Math.atan(1.0 / 5000.0),
                     Vector3D.angle(xDirection, sun.getPVCoordinates(date, Frame.getEME2000()).getPosition()),
                     1.0e-15);
        assertEquals(rate, attitude.getSpin().getNorm(), 1.0e-6);

    }

    public void setUp() {
        String root = getClass().getClassLoader().getResource("regular-data").getPath();
        System.setProperty(DataDirectoryCrawler.OREKIT_DATA_PATH, root);
    }

    public static Test suite() {
        return new TestSuite(SpinStabilizedTest.class);
    }

}


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
package org.orekit.files.ilrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ilrs.CPF.CPFCoordinate;
import org.orekit.files.ilrs.CPF.CPFEphemeris;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;

public class CPFWriterTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testWriteJason3Version2() throws IOException, URISyntaxException {

        // Simple test for version 2.0, only contains position entries
        final String ex = "/ilrs/jason3_cpf_180613_16401.cne";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF file = new CPFParser().parse(source);

        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader(), TimeScalesFactory.getUTC());
        writer.write(tempCPFFilePath, file);

        final CPF generatedCpfFile = new CPFParser().parse(new DataSource(tempCPFFilePath));
        compareCpfFiles(file, generatedCpfFile);

    }

    @Test
    public void testWriteLageos1Version2() throws IOException, URISyntaxException {

        // Simple test for version 2.0, only contains position entries
        final String ex = "/ilrs/lageos1_cpf_180613_16401.hts";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF file = new CPFParser().parse(source);

        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader(), TimeScalesFactory.getUTC());
        writer.write(tempCPFFilePath, file);

        final CPF generatedCpfFile = new CPFParser().parse(new DataSource(tempCPFFilePath));
        compareCpfFiles(file, generatedCpfFile);

    }

    @Test
    public void testWriteGalileoVersion1() throws IOException, URISyntaxException {

        // Simple test for version 1.0, only contains position entries
        final String ex = "/ilrs/galileo212_cpf_180613_6641.esa";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF file = new CPFParser().parse(source);

        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader(), TimeScalesFactory.getUTC());
        writer.write(tempCPFFilePath, file);

        final CPF generatedCpfFile = new CPFParser().parse(new DataSource(tempCPFFilePath));
        compareCpfFiles(file, generatedCpfFile);

    }

    @Test
    public void testIssue868v1() throws IOException, URISyntaxException {

        // Load
        final String ex = "/ilrs/galileo212_cpf_180613_6641.esa";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF file = new CPFParser().parse(source);

        // Write
        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader(), TimeScalesFactory.getUTC());
        writer.write(tempCPFFilePath, file);

        // Verify
        final DataSource tempSource = new DataSource(tempCPFFilePath);
        try (Reader reader = tempSource.getOpener().openReaderOnce();
                        BufferedReader br = (reader == null) ? null : new BufferedReader(reader)) {
            // The testWriteGalileoVersion1() already verify the content of the file
            // The objective here is just the verify the fix of issue #868
            final String line1 = br.readLine();
            Assert.assertEquals(56, line1.length());
            final String line2 = br.readLine();
            Assert.assertEquals(82, line2.length());
        }
    }

    @Test
    public void testIssue868v2() throws IOException, URISyntaxException {

        // Load
        final String ex = "/ilrs/lageos1_cpf_180613_16401.hts";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final CPF file = new CPFParser().parse(source);

        // Write
        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        CPFWriter writer = new CPFWriter(file.getHeader(), TimeScalesFactory.getUTC());
        writer.write(tempCPFFilePath, file);

        // Verify
        final DataSource tempSource = new DataSource(tempCPFFilePath);
        try (Reader reader = tempSource.getOpener().openReaderOnce();
                        BufferedReader br = (reader == null) ? null : new BufferedReader(reader)) {
            // The testWriteLageos1Version2() already verify the content of the file
            // The objective here is just the verify the fix of issue #868
            final String line1 = br.readLine();
            Assert.assertEquals(58, line1.length());
            final String line2 = br.readLine();
            Assert.assertEquals(85, line2.length());
        }
    }

    @Test
    public void testNullFile() throws IOException {
        final String    ex      = "/ilrs/lageos1_cpf_180613_16401.hts";
        final DataSource source  = new DataSource(ex, () ->  getClass().getResourceAsStream(ex));
        final CPF   cpfFile = new CPFParser().parse(source);
        final CPFWriter writer  = new CPFWriter(cpfFile.getHeader(), TimeScalesFactory.getUTC());
        try {
            writer.write((BufferedWriter) null, cpfFile);
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            assertEquals(OrekitMessages.NULL_ARGUMENT, oiae.getSpecifier());
            assertEquals("writer", oiae.getParts()[0]);
        }
    }

    @Test
    public void testNullEphemeris() throws IOException {
        File tempCPFFile = tempFolder.newFile("TestNullEphemeris.cpf");
        CPFWriter writer = new CPFWriter(null, TimeScalesFactory.getUTC());
        writer.write(tempCPFFile.toString(), null);
        assertTrue(tempCPFFile.exists());
        try (FileInputStream   fis = new FileInputStream(tempCPFFile);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader    br  = new BufferedReader(isr)) {
            int count = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++count;
            }
            assertEquals(0, count);
        }
    }

    /** Test for issue #844 (https://gitlab.orekit.org/orekit/orekit/-/issues/844). */
    @Test
    public void testIssue844() throws IOException {

        // Create header
        final CPFHeader header = new CPFHeader();
        header.setSource("orekit");
        header.setStep(300);
        header.setStartEpoch(AbsoluteDate.J2000_EPOCH.shiftedBy(-300.0));
        header.setEndEpoch(AbsoluteDate.J2000_EPOCH.shiftedBy(300.0));
        header.setIlrsSatelliteId("070595");
        header.setName("tag");
        header.setNoradId("0705");
        header.setProductionEpoch(new DateComponents(2000, 1, 2));
        header.setProductionHour(12);
        header.setSequenceNumber(0705);
        header.setSic("0705");
        final CPFHeader headerV1 = header;
        headerV1.setVersion(1);

        // Writer
        final CPFWriter writer = new CPFWriter(headerV1, TimeScalesFactory.getUTC());

        // Create an empty CPF file
        final CPF cpf = new CPF();

        // Fast check
        assertEquals(0, cpf.getSatellites().size());

        // Add coordinates
        final int leap = 0;
        cpf.addSatelliteCoordinate(header.getIlrsSatelliteId(), new CPFCoordinate(AbsoluteDate.J2000_EPOCH.shiftedBy(-300.0), Vector3D.PLUS_I, leap));
        cpf.addSatelliteCoordinate(header.getIlrsSatelliteId(), new CPFCoordinate(AbsoluteDate.J2000_EPOCH,                   Vector3D.PLUS_J, leap));
        cpf.addSatelliteCoordinate(header.getIlrsSatelliteId(), new CPFCoordinate(AbsoluteDate.J2000_EPOCH.shiftedBy(300.0),  Vector3D.PLUS_K, leap));

        // Write the file
        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        writer.write(tempCPFFilePath, cpf);

        // Verify
        final List<CPFCoordinate> coordinatesInFile = cpf.getSatellites().get(header.getIlrsSatelliteId()).getCoordinates();
        assertEquals(0.0, Vector3D.PLUS_I.distance(coordinatesInFile.get(0).getPosition()), 1.0e-10);
        assertEquals(0.0, Vector3D.PLUS_J.distance(coordinatesInFile.get(1).getPosition()), 1.0e-10);
        assertEquals(0.0, Vector3D.PLUS_K.distance(coordinatesInFile.get(2).getPosition()), 1.0e-10);

    }

    /** Test for issue #844 (https://gitlab.orekit.org/orekit/orekit/-/issues/844). */
    @Test
    public void testIssue844Bis() throws IOException {

        // Create header
        final CPFHeader header = new CPFHeader();
        header.setSource("orekit");
        header.setStep(300);
        header.setStartEpoch(AbsoluteDate.J2000_EPOCH.shiftedBy(-300.0));
        header.setEndEpoch(AbsoluteDate.J2000_EPOCH.shiftedBy(300.0));
        header.setIlrsSatelliteId("070595");
        header.setName("tag");
        header.setNoradId("0705");
        header.setProductionEpoch(new DateComponents(2000, 1, 2));
        header.setProductionHour(12);
        header.setSequenceNumber(0705);
        header.setSic("0705");
        final CPFHeader headerV1 = header;
        headerV1.setVersion(1);

        // Writer
        final CPFWriter writer = new CPFWriter(headerV1, TimeScalesFactory.getUTC());

        // Create an empty CPF file
        final CPF cpf = new CPF();

        // Fast check
        assertEquals(0, cpf.getSatellites().size());

        // Add coordinates
        final int leap = 0;
        final List<CPFCoordinate> coordinates = new ArrayList<>();
        coordinates.add(new CPFCoordinate(AbsoluteDate.J2000_EPOCH.shiftedBy(-300.0), Vector3D.PLUS_I, leap));
        coordinates.add(new CPFCoordinate(AbsoluteDate.J2000_EPOCH,                   Vector3D.PLUS_J, leap));
        coordinates.add(new CPFCoordinate(AbsoluteDate.J2000_EPOCH.shiftedBy(300.0),  Vector3D.PLUS_K, leap));
        cpf.addSatelliteCoordinates(header.getIlrsSatelliteId(), coordinates);

        // Write the file
        String tempCPFFilePath = tempFolder.newFile("TestWriteCPF.cpf").toString();
        writer.write(tempCPFFilePath, cpf);

        // Verify
        final List<CPFCoordinate> coordinatesInFile = cpf.getSatellites().get(header.getIlrsSatelliteId()).getCoordinates();
        assertEquals(0.0, Vector3D.PLUS_I.distance(coordinatesInFile.get(0).getPosition()), 1.0e-10);
        assertEquals(0.0, Vector3D.PLUS_J.distance(coordinatesInFile.get(1).getPosition()), 1.0e-10);
        assertEquals(0.0, Vector3D.PLUS_K.distance(coordinatesInFile.get(2).getPosition()), 1.0e-10);

    }

    @Test
    @Deprecated
    public void testDefaultId() throws IOException {

        // Initialize
        final CPF cpf = new CPF();

        // Fast check
        assertEquals(0, cpf.getSatellites().size());

        // Add coordinates
        final int leap = 0;
        cpf.addSatelliteCoordinate(new CPFCoordinate(AbsoluteDate.J2000_EPOCH, Vector3D.PLUS_I, leap));

        // Verify
        assertEquals(1, cpf.getSatellites().size());

    }

    @Test
    @Deprecated
    public void testOldConstructor() throws IOException {

        // Initialize
        // Create an empty CPF file
        final CPF cpf = new CPF();
        final CPFEphemeris ephemeris = cpf.new CPFEphemeris();

        // Fast check
        assertEquals(0, ephemeris.getCoordinates().size());
        assertEquals(CPF.DEFAULT_ID, ephemeris.getId());

    }

    public static void compareCpfFiles(CPF file1, CPF file2) {

        // Header
        final CPFHeader header1 = file1.getHeader();
        final CPFHeader header2 = file2.getHeader();
        compareCpfHeader(header1, header2);

        // Ephemeris
        final CPFEphemeris eph1 = file1.getSatellites().get(header1.getIlrsSatelliteId());
        final CPFEphemeris eph2 = file2.getSatellites().get(header2.getIlrsSatelliteId());
        Assert.assertEquals(eph1.getId(), eph2.getId());
        Assert.assertEquals(eph1.getStart(), eph2.getStart());
        Assert.assertEquals(eph1.getStop(), eph2.getStop());

        // Coordinates
        final List<CPFCoordinate> coord1 = eph1.getCoordinates();
        final List<CPFCoordinate> coord2 = eph2.getCoordinates();
        Assert.assertEquals(coord1.size(), coord1.size());
        verifyEphemerisLine(coord1.get(0), coord2.get(0));
        verifyEphemerisLine(coord1.get(1), coord2.get(1));
        verifyEphemerisLine(coord1.get(100), coord2.get(100));
        verifyEphemerisLine(coord1.get(coord1.size() - 1), coord2.get(coord2.size() - 1));

    }

    public static void compareCpfHeader(CPFHeader header1, CPFHeader header2) {
        Assert.assertEquals(header1.getFormat(), header2.getFormat());
        Assert.assertEquals(header1.getVersion(), header2.getVersion());
        Assert.assertEquals(header1.getSource(), header2.getSource());
        Assert.assertEquals(header1.getProductionEpoch().getYear(), header2.getProductionEpoch().getYear());
        Assert.assertEquals(header1.getName(), header2.getName());
        Assert.assertEquals(header1.getIlrsSatelliteId(), header2.getIlrsSatelliteId());
        Assert.assertEquals(header1.getSic(), header2.getSic());
        Assert.assertEquals(0.0, header1.getStartEpoch().durationFrom(header2.getStartEpoch()), 1.0e-15);
        Assert.assertEquals(0.0, header1.getEndEpoch().durationFrom(header2.getEndEpoch()), 1.0e-15);
    }

    public static void verifyEphemerisLine(CPFCoordinate coord1, CPFCoordinate coord2) {
        Assert.assertEquals(0.0, coord1.getDate().durationFrom(coord2.getDate()), 1.0e-10);
        Assert.assertEquals(0.0, coord1.getPosition().distance(coord2.getPosition()), 1.0e-10);
    }

}

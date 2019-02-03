/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2019 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.engine;

import jgnash.engine.xstream.UUIDConverter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * JUnit test for XML storage with the Engine API.
 *
 * @author Craig Cavanaugh
 */
public class XMLEngineTest extends EngineTest {

    private static String tempFile;

    @Override
    public Engine createEngine() {
        try {
            testFile = Files.createTempFile("test", DataStoreType.XML.getDataStore().getFileExt()).toString();
            tempFile = testFile;

        } catch (IOException e1) {
            Logger.getLogger(XMLEngineTest.class.getName()).log(Level.SEVERE, e1.getLocalizedMessage(), e1);
        }

        EngineFactory.deleteDatabase(testFile);

        return EngineFactory.bootLocalEngine(testFile, EngineFactory.DEFAULT, EngineFactory.EMPTY_PASSWORD,
                DataStoreType.XML);
    }

    @AfterAll
    static void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(tempFile));
    }

    /**
     * Test UUID fix here
     */
    @Test
    void testUUIFix() {
        String badUUID = "c93972f6fdd5402eb314fc8402d2c51f";
        String goodUUID = "c93972f6-fdd5-402e-b314-fc8402d2c51f";

        assertEquals(goodUUID, UUIDConverter.fixUUID(badUUID));

        assertNotNull(UUID.fromString(goodUUID));
    }
}

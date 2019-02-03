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
package jgnash.report.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import jgnash.resource.util.OS;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;

/**
 * Utility class map font names to font files
 *
 * @author Craig Cavanaugh
 */
public class FontRegistry {

    /**
     * Maps the font file to the font name for embedding fonts in PDF files
     */
    private final Map<String, String> registeredFontMap = new HashMap<>();
    
    private static FontRegistry registry;
    
    private static final AtomicBoolean registrationComplete = new AtomicBoolean(false);
    
    private static final AtomicBoolean registrationStarted = new AtomicBoolean(false);
    
    private static final Lock lock = new ReentrantLock();
    
    private static final Condition isComplete = lock.newCondition();

    private FontRegistry() {
    }

    public static String getRegisteredFontPath(final String name) {
        if (!registrationStarted.get()) {
            registerFonts();
        }

        if (!registrationComplete.get()) {

            try {
                lock.lock();

                while (!registrationComplete.get()) {
                    isComplete.await();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(FontRegistry.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                lock.unlock();
            }
        }

        return registry.registeredFontMap.get(name.toLowerCase(Locale.ROOT));
    }

    private static void registerFonts() {

        if (!registrationStarted.get()) {
            registrationStarted.set(true);

            Thread thread = new Thread(() -> {
                lock.lock();

                try {
                    registry = new FontRegistry();
                    try {
                        registry.registerFontDirectories();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    registrationComplete.set(true);

                    isComplete.signal();

                    Logger.getLogger(FontRegistry.class.getName()).info("Font registration is complete");
                } finally {
                    lock.unlock();
                }
            });

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    private void registerFont(final String path) {
        try {
            if (path.toLowerCase(Locale.ROOT).endsWith(".ttf") || path.toLowerCase(Locale.ROOT).endsWith(".otf")
                    || path.toLowerCase(Locale.ROOT).indexOf(".ttc,") > 0) {
                Object allNames[] = BaseFont.getAllFontNames(path, BaseFont.WINANSI, null);

                String[][] names = (String[][]) allNames[2]; //full name
                for (String[] name : names) {
                    registeredFontMap.put(name[3].toLowerCase(Locale.ROOT), path);
                }

            } else if (path.toLowerCase(Locale.ROOT).endsWith(".ttc")) {
                String[] names = BaseFont.enumerateTTCNames(path);
                for (int i = 0; i < names.length; i++) {
                    registerFont(path + "," + i);
                }
            } else if (path.toLowerCase(Locale.ROOT).endsWith(".afm") || path.toLowerCase(Locale.ROOT).endsWith(".pfm")) {
                BaseFont bf = BaseFont.createFont(path, BaseFont.CP1252, false);
                String fullName = bf.getFullFontName()[0][3].toLowerCase(Locale.ROOT);
                registeredFontMap.put(fullName, path);
            }
        } catch (final DocumentException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register all the fonts in a directory and its subdirectories.
     *
     * @param dir the directory
     */
    @SuppressWarnings("ConstantConditions")
    private void registerFontDirectory(final String dir) throws IOException {

        final Path directory = Paths.get(dir);

        if (Files.isDirectory(directory)) {
            Files.list(directory).forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        registerFontDirectory(path.toString());
                    } else {
                        String name = path.toString();
                        String suffix = name.length() < 4 ? null : name.substring(name.length() - 3).toLowerCase(Locale.ROOT);

                        if (suffix != null) {
                            switch (suffix) {
                                case "afm":
                                case "pfm":
                                    if (Files.exists(Paths.get(name.substring(0, name.length() - 3) + "pfb"))) {
                                        registerFont(name);
                                    }
                                    break;
                                case "ttf":
                                case "otf":
                                case "ttc":
                                    registerFont(name);
                                    break;
                                default:
                                    break;  // unknown font type
                            }
                        }
                    }
                } catch (final Exception e) {
                    Logger.getLogger(FontRegistry.class.getName()).log(Level.FINEST,
                            MessageFormat.format("Could not find path for {0}", path), e);
                }
            });
        }
    }

    /**
     * Register fonts in known directories.
     */
    private void registerFontDirectories() throws IOException {
        if (OS.isSystemWindows()) {
            registerFontDirectory("c:/windows/fonts");
            registerFontDirectory("c:/winnt/fonts");
            registerFontDirectory("d:/windows/fonts");
            registerFontDirectory("d:/winnt/fonts");
        } else if (OS.isSystemOSX()) {
            final String userHome = System.getProperty("user.home");
            registerFontDirectory(userHome + "/Library/Fonts");
            registerFontDirectory("/Library/Fonts");
            registerFontDirectory("/Network/Library/Fonts");
            registerFontDirectory("/System/Library/Fonts");
        } else { // Linux, etc.
            registerFontDirectory("/usr/share/X11/fonts");
            registerFontDirectory("/usr/X/lib/X11/fonts");
            registerFontDirectory("/usr/openwin/lib/X11/fonts");
            registerFontDirectory("/usr/share/fonts");
            registerFontDirectory("/usr/X11R6/lib/X11/fonts");
        }
    }
}

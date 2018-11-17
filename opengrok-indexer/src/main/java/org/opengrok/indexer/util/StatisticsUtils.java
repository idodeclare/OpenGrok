/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

 /*
  * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
  */

package org.opengrok.indexer.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.web.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class StatisticsUtils {

    private final RuntimeEnvironment env;

    /**
     * Initializes an instance to use the specified, required environment.
     * @param env a defined instance
     */
    public StatisticsUtils(RuntimeEnvironment env) {
        if (env == null) {
            throw new IllegalArgumentException("env is null");
        }
        this.env = env;
    }

    /**
     * Dump statistics in JSON format into the file specified in configuration.
     *
     * @throws IOException
     */
    public void saveStatistics() throws IOException {
        String path = env.getStatisticsFilePath();
        if (path == null) {
            throw new FileNotFoundException("Statistics file is not set (null)");
        }
        saveStatistics(new File(path));
    }

    /**
     * Dump statistics in JSON format into a file.
     *
     * @param out the output file
     * @throws IOException
     */
    public void saveStatistics(File out) throws IOException {
        if (out == null) {
            throw new FileNotFoundException("Statistics file is not set (null)");
        }
        try (FileOutputStream ofstream = new FileOutputStream(out)) {
            saveStatistics(ofstream);
        }
    }

    /**
     * Dump statistics in JSON format into an output stream.
     *
     * @param out the output stream
     * @throws IOException
     */
    public void saveStatistics(OutputStream out) throws IOException {
        out.write(Util.statisticToJson(env.getStatistics()).toJSONString().getBytes());
    }

    /**
     * Load statistics from JSON file specified in configuration.
     *
     * @throws IOException
     * @throws ParseException
     */
    public void loadStatistics() throws IOException, ParseException {
        String path = env.getStatisticsFilePath();
        if (path == null) {
            throw new FileNotFoundException("Statistics file is not set (null)");
        }
        loadStatistics(new File(path));
    }

    /**
     * Load statistics from JSON file.
     *
     * @param in the file with json
     * @throws IOException
     * @throws ParseException
     */
    public void loadStatistics(File in) throws IOException, ParseException {
        if (in == null) {
            throw new FileNotFoundException("Statistics file is not set (null)");
        }
        try (FileInputStream ifstream = new FileInputStream(in)) {
            loadStatistics(ifstream);
        }
    }

    /**
     * Load statistics from an input stream.
     *
     * @param in the file with json
     * @throws IOException
     * @throws ParseException
     */
    public void loadStatistics(InputStream in) throws IOException, ParseException {
        try (InputStreamReader iReader = new InputStreamReader(in)) {
            JSONParser jsonParser = new JSONParser();
            env.setStatistics(Util.jsonToStatistics((JSONObject) jsonParser.parse(iReader)));
        }
    }
}

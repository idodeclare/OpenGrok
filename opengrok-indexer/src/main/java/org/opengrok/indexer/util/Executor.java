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
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.logger.LoggerFactory;

/**
 * Wrapper to Java Process API
 *
 * @author Emilio Monti - emilmont@gmail.com
 */
public class Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Executor.class);

    private List<String> cmdList;
    private File workingDirectory;
    private byte[] stdout;
    private byte[] stderr;
    private int timeout; // in seconds, 0 means no timeout

    /**
     * Create a new instance of the Executor.
     * @param cmd An array containing the command to execute
     */
    public Executor(String[] cmd) {
        this(Arrays.asList(cmd));
    }

    /**
     * Create a new instance of the Executor.
     * @param cmdList A list containing the command to execute
     */
    public Executor(List<String> cmdList) {
        this(cmdList, null);
    }

    /**
     * Create a new instance of the Executor with default command timeout value.
     * @param cmdList A list containing the command to execute
     * @param workingDirectory The directory the process should have as the
     *                         working directory
     */
    public Executor(List<String> cmdList, File workingDirectory) {
        this.cmdList = cmdList;
        this.workingDirectory = workingDirectory;
        this.timeout = RuntimeEnvironment.getInstance().getCommandTimeout() * 1000;
    }

    /**
     * Create a new instance of the Executor with specific timeout value.
     * @param cmdList A list containing the command to execute
     * @param workingDirectory The directory the process should have as the
     *                         working directory
     * @param timeout If the command runs longer than the timeout (seconds),
     *                it will be terminated. If the value is 0, no timer
     *                will be set up.
     */
    public Executor(List<String> cmdList, File workingDirectory, int timeout) {
        this.cmdList = cmdList;
        this.workingDirectory = workingDirectory;
        this.timeout = timeout * 1000;
    }

    /**
     * Create a new instance of the Executor with or without timeout,
     * @param cmdList A list containing the command to execute
     * @param workingDirectory The directory the process should have as the
     *                         working directory
     * @param UseTimeout terminate the process after default timeout or not
     */
    public Executor(List<String> cmdList, File workingDirectory, boolean UseTimeout) {
        this(cmdList, workingDirectory);
        if (!UseTimeout) {
            this.timeout = 0;
        }
    }

    /**
     * Execute the command and collect the output. All exceptions will be
     * logged.
     *
     * @return The exit code of the process
     */
    public int exec() {
        return exec(true);
    }

    /**
     * Execute the command and collect the output
     *
     * @param reportExceptions Should exceptions be added to the log or not
     * @return The exit code of the process
     */
    public int exec(boolean reportExceptions) {
        SpoolHandler spoolOut = new SpoolHandler();
        int ret = exec(reportExceptions, spoolOut);
        stdout = spoolOut.getBytes();
        return ret;
    }

    /**
     * Execute the command and collect the output
     *
     * @param reportExceptions Should exceptions be added to the log or not
     * @param handler The handler to handle data from standard output
     * @return The exit code of the process
     */
    public int exec(final boolean reportExceptions, StreamHandler handler) {
        int ret = -1;
        stdout = null;
        stderr = null;
        ExecutorProcess ep = null;
        try {
            ep = startExec(reportExceptions);
            handler.processStream(ep.process.getInputStream());
            ret = ep.process.waitFor();
            
            LOGGER.log(Level.FINE,
                "Finished command {0} in directory {1} with exit code {2}",
                new Object[] {ep.cmd_str, ep.dir_str, ret});

            // Wait for the stderr read-out thread to finish the processing and
            // only after that read the data.
            ep.err_thread.join();
            stderr = ep.err.getBytes();
        } catch (IOException e) {
            if (reportExceptions) {
                LOGGER.log(Level.SEVERE,
                        "Failed to read from process: " + cmdList.get(0), e);
            }
            if (ep == null) {
                return ret;
            }
        } catch (InterruptedException e) {
            if (reportExceptions) {
                LOGGER.log(Level.SEVERE,
                        "Waiting for process interrupted: " + cmdList.get(0), e);
            }
        } finally {
            if (ep != null) {
                ret = ep.finish();
            }
        }

        if (ret != 0 && reportExceptions) {
            int MAX_MSG_SZ = 512; /* limit to avoid flooding the logs */
            StringBuilder msg = new StringBuilder("Non-zero exit status ")
                    .append(ret).append(" from command ")
                    .append(ep.cmd_str)
                    .append(" in directory ")
                    .append(ep.dir_str);
            if (stderr != null && stderr.length > 0) {
                    msg.append(": ");
                    if (stderr.length > MAX_MSG_SZ) {
                            msg.append(new String(stderr, 0, MAX_MSG_SZ)).append("...");
                    } else {
                            msg.append(new String(stderr));
                    }
            }
            LOGGER.log(Level.WARNING, msg.toString());
        }

        return ret;
    }

    /**
     * Execute the command and collect the output
     *
     * @param reportExceptions Should exceptions be added to the log or not
     * @return The exit code of the process
     */
    private ExecutorProcess startExec(final boolean reportExceptions)
            throws IOException {

        final ExecutorProcess ep = new ExecutorProcess();
        ep.process_builder = new ProcessBuilder(cmdList);
        ep.cmd_str = StringUtils.joinArgv(ep.process_builder.command());

        if (workingDirectory != null) {
            ep.process_builder.directory(workingDirectory);
            if (ep.process_builder.environment().containsKey("PWD")) {
                ep.process_builder.environment().put("PWD",
                        workingDirectory.getAbsolutePath());
            }
        }

        File cwd = ep.process_builder.directory();
        if (cwd == null) {
            ep.dir_str = System.getProperty("user.dir");
        } else {
            ep.dir_str = cwd.toString();
        }

        String env_str = "";
        if (LOGGER.isLoggable(Level.FINER)) {
            Map<String,String> env_map = ep.process_builder.environment();
            env_str = " with environment: " + env_map.toString();
        }
        LOGGER.log(Level.FINE,
                "Executing command {0} in directory {1}{2}",
                new Object[] {ep.cmd_str, ep.dir_str, env_str});

        final Process proc = ep.process_builder.start();
        ep.process = proc;

        final InputStream errorStream = proc.getErrorStream();
        ep.err_thread = new Thread(() -> {
            try {
                ep.err.processStream(errorStream);
            } catch (IOException ex) {
                if (reportExceptions) {
                    LOGGER.log(Level.SEVERE,
                            "Error while executing command {0} in directory {1}",
                            new Object[] {ep.cmd_str, ep.dir_str});
                    LOGGER.log(Level.SEVERE,
                            "Error during process pipe listening", ex);
                }
            }
        });
        ep.err_thread.start();

        final int timeout = this.timeout;
        /*
         * Setup timer so if the process get stuck we can terminate it and
         * make progress instead of hanging the whole operation.
         */
        if (timeout != 0) {
            // invoking the constructor starts the background thread
            ep.timer = new Timer();
            ep.timer.schedule(new TimerTask() {
                @Override public void run() {
                    LOGGER.log(Level.WARNING,
                            String.format("Terminating process of command %s in directory %s " +
                            "due to timeout %d seconds", ep.cmd_str, ep.dir_str,
                            timeout / 1000));
                    proc.destroy();
                }
            }, timeout);
        }
        return ep;
    }

    /**
     * Get the output from the process as a string.
     *
     * @return The output from the process
     */
    public String getOutputString() {
        String ret = null;
        if (stdout != null) {
            ret = new String(stdout);
        }

        return ret;
    }

    /**
     * Get a reader to read the output from the process
     *
     * @return A reader reading the process output
     */
    public Reader getOutputReader() {
        return new InputStreamReader(getOutputStream());
    }

    /**
     * Get an input stream read the output from the process
     *
     * @return A reader reading the process output
     */
    public InputStream getOutputStream() {
        return new ByteArrayInputStream(stdout);
    }

    /**
     * Get the output from the process written to the error stream as a string.
     *
     * @return The error output from the process
     */
    public String getErrorString() {
        String ret = null;
        if (stderr != null) {
            ret = new String(stderr);
        }

        return ret;
    }

    /**
     * Get a reader to read the output the process wrote to the error stream.
     *
     * @return A reader reading the process error stream
     */
    public Reader getErrorReader() {
        return new InputStreamReader(getErrorStream());
    }

    /**
     * Get an input stream to read the output the process wrote to the error stream.
     *
     * @return An input stream for reading the process error stream
     */
    public InputStream getErrorStream() {
        return new ByteArrayInputStream(stderr);
    }

    /**
     * You should use the StreamHandler interface if you would like to process
     * the output from a process while it is running
     */
    public interface StreamHandler {

        /**
         * Process the data in the stream. The processStream function is
         * called _once_ during the lifetime of the process, and you should
         * process all of the input you want before returning from the function.
         *
         * @param in The InputStream containing the data
         * @throws java.io.IOException if any read error
         */
        void processStream(InputStream in) throws IOException;
    }

    private static class SpoolHandler implements StreamHandler {

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        public byte[] getBytes() {
            return bytes.toByteArray();
        }

        @Override
        public void processStream(InputStream input) throws IOException {            
            BufferedInputStream  in = new BufferedInputStream(input);

            byte[] buffer = new byte[8092];
            int len;

            while ((len = in.read(buffer)) != -1) {
                if (len > 0) {
                    bytes.write(buffer, 0, len);
                }
            }
        }
    }
    
    public static void registerErrorHandler() {
        UncaughtExceptionHandler dueh =
            Thread.getDefaultUncaughtExceptionHandler();
        if (dueh == null) {
            LOGGER.log(Level.FINE, "Installing default uncaught exception handler");
            Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LOGGER.log(Level.SEVERE, "Uncaught exception in thread "
                        + t.getName() + " with ID " + t.getId() + ": "
                        + e.getMessage(), e);
                }
            });
        }
    }

    private static class ExecutorProcess {
        final SpoolHandler err = new SpoolHandler();
        String cmd_str;
        String dir_str;
        ProcessBuilder process_builder;
        Process process;
        Timer timer;
        Thread err_thread;

        int finish() {
            if (timer != null) {
                timer.cancel();
            }
            if (process != null) {
                try {
                    IOUtils.close(process.getOutputStream());
                    IOUtils.close(process.getInputStream());
                    IOUtils.close(process.getErrorStream());
                    return process.exitValue();
                } catch (IllegalThreadStateException e) {
                    process.destroy();
                }
            }
            return -1;
        }
    }
}

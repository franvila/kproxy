/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests.executor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kroxylicious.systemtests.k8s.exception.KubeClusterException;

import static java.lang.String.join;

/**
 * Class provide execution of external command
 */
@SuppressWarnings({ "checkstyle:ClassDataAbstractionCoupling", "checkstyle:CyclomaticComplexity", "checkstyle:NPathComplexity" })
public class Exec {
    private static final Logger LOGGER = LoggerFactory.getLogger(Exec.class);
    private static final Pattern ERROR_PATTERN = Pattern.compile("Error from server \\(([a-zA-Z0-9]+)\\):");
    private static final Pattern INVALID_PATTERN = Pattern.compile("The ([a-zA-Z0-9]+) \"([a-z0-9.-]+)\" is invalid:");
    private static final Pattern PATH_SPLITTER = Pattern.compile(System.getProperty("path.separator"));
    private static final int MAXIMUM_EXEC_LOG_CHARACTER_SIZE = Integer.parseInt(System.getenv().getOrDefault("STRIMZI_EXEC_MAX_LOG_OUTPUT_CHARACTERS", "20000"));
    private static final Object LOCK = new Object();

    /**
     * The Process.
     */
    private Process process;
    private String stdOut;
    private String stdErr;
    private StreamGobbler stdOutReader;
    private StreamGobbler stdErrReader;
    private Path logPath;
    private final boolean appendLineSeparator;

    /**
     * Instantiates a new Exec.
     */
    public Exec() {
        this.appendLineSeparator = true;
    }

    /**
     * Getter for stdOutput
     *
     * @return string stdOut
     */
    public String out() {
        return stdOut;
    }

    /**
     * Getter for stdErrorOutput
     *
     * @return string stdErr
     */
    public String err() {
        return stdErr;
    }

    /**
     * Is running boolean.
     *
     * @return the boolean
     */
    public boolean isRunning() {
        return process.isAlive();
    }

    /**
     * Gets ret code.
     *
     * @return the ret code
     */
    public int getRetCode() {
        LOGGER.info("Process: {}", process);
        if (isRunning()) {
            return -1;
        }
        else {
            return process.exitValue();
        }
    }

    /**
     * Method executes external command
     *
     * @param dir the dir
     * @param command arguments for command
     * @return execution results
     */
    public static ExecResult exec(File dir, String... command) {
        return exec(Arrays.asList(command), dir);
    }

    /**
     * Exec exec result.
     *
     * @param command the command
     * @return the exec result
     */
    public static ExecResult exec(String... command) {
        return exec(Arrays.asList(command), null);
    }

    /**
     * Method executes external command
     *
     * @param command arguments for command
     * @param dir the dir
     * @return execution results
     */
    public static ExecResult exec(List<String> command, File dir) {
        return exec(null, command, 0, false, dir);
    }

    /**
     * Exec exec result.
     *
     * @param command the command
     * @return the exec result
     */
    public static ExecResult exec(List<String> command) {
        return exec(null, command, 0, false);
    }

    /**
     * Method executes external command
     *
     * @param input the input
     * @param command arguments for command
     * @return execution results
     */
    public static ExecResult exec(String input, List<String> command) {
        return exec(input, command, 0, false);
    }

    /**
     * Method executes external command
     * @param input the input
     * @param command arguments for command
     * @param timeout timeout for execution
     * @param logToOutput log output or not
     * @param dir the dir
     * @return execution results
     */
    public static ExecResult exec(String input, List<String> command, int timeout, boolean logToOutput, File dir) {
        return exec(input, command, timeout, logToOutput, true, dir);
    }

    /**
     * Exec exec result.
     *
     * @param input the input
     * @param command the command
     * @param timeout the timeout
     * @param logToOutput the log to output
     * @return the exec result
     */
    public static ExecResult exec(String input, List<String> command, int timeout, boolean logToOutput) {
        return exec(input, command, timeout, logToOutput, true, null);
    }

    /**
     * Method executes external command
     * @param input the input
     * @param command arguments for command
     * @param timeout timeout for execution
     * @param logToOutput log output or not
     * @param throwErrors look for errors in output and throws exception if true
     * @param dir the dir
     * @return execution results
     */
    public static ExecResult exec(String input, List<String> command, int timeout, boolean logToOutput, boolean throwErrors, File dir) {
        int ret = 1;
        ExecResult execResult;
        try {
            Exec executor = new Exec();
            ret = executor.execute(input, command, timeout, dir);
            synchronized (LOCK) {
                if (logToOutput || ret != 0) {
                    String log = ret != 0 ? "Failed to exec command" : "Command";
                    LOGGER.info("{}: {}", log, String.join(" ", command));
                    if (input != null && !input.contains("CustomResourceDefinition")) {
                        LOGGER.info("Input: {}", input.trim());
                    }
                    LOGGER.info("RETURN code: {}", ret);
                    if (!executor.out().isEmpty()) {
                        LOGGER.debug("======STDOUT START=======");
                        LOGGER.debug("{}", cutExecutorLog(executor.out().trim()));
                        LOGGER.debug("======STDOUT END======");
                    }
                    if (!executor.err().isEmpty()) {
                        LOGGER.debug("======STDERR START=======");
                        LOGGER.debug("{}", cutExecutorLog(executor.err().trim()));
                        LOGGER.debug("======STDERR END======");
                    }
                }
            }

            execResult = new ExecResult(ret, executor.out(), executor.err());

            if (throwErrors && ret != 0) {
                String msg = "`" + join(" ", command) + "` got status code " + ret + " and stderr:\n------\n" + executor.stdErr + "\n------\nand stdout:\n------\n"
                        + executor.stdOut + "\n------";

                Matcher matcher = ERROR_PATTERN.matcher(executor.err());
                KubeClusterException kubeClusterException = null;

                if (matcher.find()) {
                    switch (matcher.group(1)) {
                        case "NotFound":
                            kubeClusterException = new KubeClusterException.NotFound(execResult, msg);
                            break;
                        case "AlreadyExists":
                            kubeClusterException = new KubeClusterException.AlreadyExists(execResult, msg);
                            break;
                        default:
                            break;
                    }
                }
                matcher = INVALID_PATTERN.matcher(executor.err());
                if (matcher.find()) {
                    kubeClusterException = new KubeClusterException.InvalidResource(execResult, msg);
                }
                if (kubeClusterException == null) {
                    kubeClusterException = new KubeClusterException(execResult, msg);
                }
                throw kubeClusterException;
            }
            return new ExecResult(ret, executor.out(), executor.err());

        }
        catch (IOException | ExecutionException e) {
            throw new KubeClusterException(e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KubeClusterException(e);
        }
    }

    /**
     * Method executes external command
     *
     * @param input the input
     * @param commands arguments for command
     * @param timeoutMs timeout in ms for kill
     * @param dir the dir
     * @return returns ecode of execution
     * @throws IOException the io exception
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     */
    public int execute(String input, List<String> commands, long timeoutMs, File dir) throws IOException, InterruptedException, ExecutionException {
        LOGGER.trace("Running command - " + join(" ", commands.toArray(new String[0])));
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commands);
        dir = dir == null ? new File(System.getProperty("user.dir")) : dir;
        builder.directory(dir);
        process = builder.start();
        OutputStream outputStream = process.getOutputStream();
        if (input != null) {
            LOGGER.trace("With stdin {}", input);
            outputStream.write(input.getBytes(Charset.defaultCharset()));
        }
        // Close subprocess' stdin
        outputStream.close();

        Future<String> output = readStdOutput();
        Future<String> error = readStdError();

        int retCode = 1;
        if (timeoutMs > 0) {
            if (process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                retCode = process.exitValue();
            }
            else {
                process.destroyForcibly();
            }
        }
        else {
            retCode = process.waitFor();
        }

        try {
            stdOut = output.get(500, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException ex) {
            output.cancel(true);
            stdOut = stdOutReader.getData();
        }

        try {
            stdErr = error.get(500, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException ex) {
            error.cancel(true);
            stdErr = stdErrReader.getData();
        }
        storeOutputsToFile();

        return retCode;
    }

    /**
     * Get standard output of execution
     *
     * @return future string output
     */
    private Future<String> readStdOutput() {
        stdOutReader = new StreamGobbler(process.getInputStream());
        return stdOutReader.read();
    }

    /**
     * Get standard error output of execution
     *
     * @return future string error output
     */
    private Future<String> readStdError() {
        stdErrReader = new StreamGobbler(process.getErrorStream());
        return stdErrReader.read();
    }

    /**
     * Get stdOut and stdErr and store it into files
     */
    private void storeOutputsToFile() {
        if (logPath != null) {
            try {
                Files.createDirectories(logPath);
                Files.writeString(Paths.get(logPath.toString(), "stdOutput.log"), stdOut, Charset.defaultCharset());
                Files.writeString(Paths.get(logPath.toString(), "stdError.log"), stdErr, Charset.defaultCharset());
            }
            catch (Exception ex) {
                LOGGER.warn("Cannot save output of execution: " + ex.getMessage());
            }
        }
    }

    /**
     * Check if command is executable
     * @param cmd command
     * @return true.false boolean
     */
    public static boolean isExecutableOnPath(String cmd) {
        for (String dir : PATH_SPLITTER.split(System.getenv("PATH"))) {
            if (new File(dir, cmd).canExecute()) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method check the size of executor output log and cut it if it's too long.
     * @param log executor log
     * @return updated log if size is too big
     */
    public static String cutExecutorLog(String log) {
        if (log.length() > MAXIMUM_EXEC_LOG_CHARACTER_SIZE) {
            LOGGER.warn("Executor log is too long. Going to strip it and print only first {} characters", MAXIMUM_EXEC_LOG_CHARACTER_SIZE);
            return log.substring(0, MAXIMUM_EXEC_LOG_CHARACTER_SIZE);
        }
        return log;
    }

    /**
     * Class represent async reader
     */
    class StreamGobbler {
        private final InputStream is;
        private final StringBuilder data = new StringBuilder();

        /**
         * Constructor of StreamGobbler
         *
         * @param is input stream for reading
         */
        StreamGobbler(InputStream is) {
            this.is = is;
        }

        /**
         * Return data from stream sync
         *
         * @return string of data
         */
        public String getData() {
            return data.toString();
        }

        /**
         * read method
         *
         * @return return future string of output
         */
        public Future<String> read() {
            return CompletableFuture.supplyAsync(() -> {
                try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
                    while (scanner.hasNextLine()) {
                        data.append(scanner.nextLine());
                        if (appendLineSeparator) {
                            data.append(System.getProperty("line.separator"));
                        }
                    }
                    return data.toString();
                }
                catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, runnable -> new Thread(runnable).start());
        }
    }
}
package com.pty4j.windows;

import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.pty4j.util.PtyUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CygwinPtyProcess extends PtyProcess {
    private static final int CONNECT_PIPE_TIMEOUT = 1000;

    private static final int PIPE_ACCESS_INBOUND = 1;
    private static final int PIPE_ACCESS_OUTBOUND = 2;

    private static final AtomicInteger processCounter = new AtomicInteger();

    private final Process myProcess;
    private final NamedPipe myInputPipe;
    private final NamedPipe myOutputPipe;
    private final NamedPipe myErrorPipe;
    private final WinNT.HANDLE myInputHandle;
    private final WinNT.HANDLE myOutputHandle;
    private final WinNT.HANDLE myErrorHandle;

    public CygwinPtyProcess(String[] command, Map<String, String> environment, String workingDirectory, File logFile, boolean console)
            throws IOException {
        String pipePrefix = String.format("\\\\.\\pipe\\cygwinpty-%d-%d-", WinPty.KERNEL32.GetCurrentProcessId(), processCounter.getAndIncrement());
        String inPipeName = pipePrefix + "in";
        String outPipeName = pipePrefix + "out";
        String errPipeName = pipePrefix + "err";

        myInputHandle = WinPty.KERNEL32.CreateNamedPipeA(inPipeName, PIPE_ACCESS_OUTBOUND | WinNT.FILE_FLAG_OVERLAPPED, 0, 1, 0, 0, 0, null);
        myOutputHandle = WinPty.KERNEL32.CreateNamedPipeA(outPipeName, PIPE_ACCESS_INBOUND | WinNT.FILE_FLAG_OVERLAPPED, 0, 1, 0, 0, 0, null);
        myErrorHandle =
                console ? WinPty.KERNEL32.CreateNamedPipeA(errPipeName, PIPE_ACCESS_INBOUND | WinNT.FILE_FLAG_OVERLAPPED, 0, 1, 0, 0, 0, null) : null;

        if (myInputHandle == WinBase.INVALID_HANDLE_VALUE ||
                myOutputHandle == WinBase.INVALID_HANDLE_VALUE ||
                myErrorHandle == WinBase.INVALID_HANDLE_VALUE) {
            closeHandles();
            throw new IOException("Unable to create a named pipe");
        }

        myInputPipe = new NamedPipe(myInputHandle, false);
        myOutputPipe = new NamedPipe(myOutputHandle, false);
        myErrorPipe = myErrorHandle != null ? new NamedPipe(myErrorHandle, false) : null;

        myProcess = startProcess(inPipeName, outPipeName, errPipeName, workingDirectory, command, environment, logFile, console);
    }

    private Process startProcess(String inPipeName,
                                 String outPipeName,
                                 String errPipeName,
                                 String workingDirectory,
                                 String[] command,
                                 Map<String, String> environment,
                                 File logFile,
                                 boolean console) throws IOException {
        File nativeFile;
        try {
            nativeFile = PtyUtil.resolveNativeFile("cyglaunch.exe");
        } catch (Exception e) {
            throw new IOException(e);
        }
        String logPath = logFile == null ? "null" : logFile.getAbsolutePath();
        ProcessBuilder processBuilder =
                new ProcessBuilder(nativeFile.getAbsolutePath(), logPath, console ? "1" : "0", inPipeName, outPipeName, errPipeName);
        for (String s : command) {
            processBuilder.command().add(s);
        }
        processBuilder.directory(new File(workingDirectory));
        processBuilder.environment().clear();
        processBuilder.environment().putAll(environment);
        final Process process = processBuilder.start();

        try {
            waitForPipe(myInputHandle);
            waitForPipe(myOutputHandle);
            if (myErrorHandle != null) waitForPipe(myErrorHandle);
        } catch (IOException e) {
            process.destroy();
            closeHandles();
            throw e;
        }

        new Thread(() -> {
            while (true) {
                try {
                    process.waitFor();
                    break;
                } catch (InterruptedException ignore) {
                }
            }
            closeHandles();
        }).start();

        return process;
    }

    private static void waitForPipe(WinNT.HANDLE handle) throws IOException {
        WinNT.HANDLE connectEvent = WinPty.KERNEL32.CreateEventA(null, true, false, null);

        WinBase.OVERLAPPED povl = new WinBase.OVERLAPPED();
        povl.hEvent = connectEvent;

        boolean success = WinPty.KERNEL32.ConnectNamedPipe(handle, povl);
        if (!success) {
            switch (WinPty.KERNEL32.GetLastError()) {
                case WinError.ERROR_PIPE_CONNECTED:
                    success = true;
                    break;
                case WinError.ERROR_IO_PENDING:
                    if (WinPty.KERNEL32.WaitForSingleObject(connectEvent, CONNECT_PIPE_TIMEOUT) != WinBase.WAIT_OBJECT_0) {
                        WinPty.KERNEL32.CancelIo(handle);

                        success = false;
                    } else {
                        success = true;
                    }

                    break;
            }
        }

        WinPty.KERNEL32.CloseHandle(connectEvent);

        if (!success) throw new IOException("Cannot connect to a named pipe");
    }

    @Override
    public boolean isRunning() {
        try {
            myProcess.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    @Override
    public void setWinSize(WinSize winSize) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public WinSize getWinSize() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getPid() {
        // TODO: Dont know yet how to get pid. Need to test on Windows.
        return -1;
    }

    @Override
    public OutputStream getOutputStream() {
        return new CygwinPTYOutputStream(myInputPipe);
    }

    @Override
    public InputStream getInputStream() {
        return new CygwinPTYInputStream(myOutputPipe);
    }

    @Override
    public InputStream getErrorStream() {
        if (myErrorPipe == null) {
            return new InputStream() {
                @Override
                public int read() {
                    return -1;
                }
            };
        }
        return new CygwinPTYInputStream(myErrorPipe);
    }

    @Override
    public int waitFor() throws InterruptedException {
        return myProcess.waitFor();
    }

    @Override
    public int exitValue() {
        return myProcess.exitValue();
    }

    @Override
    public void destroy() {
        myProcess.destroy();
    }

    private void closeHandles() {
        WinPty.KERNEL32.CloseHandle(myInputHandle);
        WinPty.KERNEL32.CloseHandle(myOutputHandle);
        if (myErrorHandle != null) WinPty.KERNEL32.CloseHandle(myErrorHandle);
    }
}

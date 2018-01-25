package org.pty4j;


import com.sun.jna.Platform;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 作者： lzw<br/>
 * 创建时间：2018-01-16 11:15 <br/>
 */
@SuppressWarnings("Duplicates")
public class Terminal {

    private PtyProcess process;
    private InputStream inputReader;
    private InputStream errorReader;
    private BufferedWriter outputWriter;

    public void initializeProcess() throws Exception {

        String userHome = System.getProperty("user.home");
        String[] termCommand;
        if (Platform.isWindows()) {
            termCommand = "cmd.exe".split("\\s+");
        } else {
            termCommand = "/bin/bash -i".split("\\s+");
        }
        Map<String, String> envs = new HashMap<>(System.getenv());
        envs.put("TERM", "xterm");
        this.process = PtyProcess.exec(termCommand, envs, userHome, false, false, null);
        process.setWinSize(new WinSize(180, 20));
        this.inputReader = process.getInputStream();
        this.errorReader = process.getErrorStream();
        this.outputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        new Thread(() -> printReader(inputReader)).start();
        new Thread(() -> printReader(errorReader)).start();
//        process.waitFor();
    }

    private void printReader(InputStream inputStream) {
        try {
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                print(new String(data, 0, nRead));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void print(String text) {
        System.out.println(text);
    }

    public void setWinSize(int columns, int rows) {
        this.process.setWinSize(new WinSize(columns, rows));
    }

    public void onCommand(String command) throws IOException {
        outputWriter.write(command);
        outputWriter.flush();
    }

    public void waitFor() {
        try {
            System.out.println("退出代码: " + process.waitFor());
            destroy();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        process.destroyForcibly();
        try {
            inputReader.close();
            errorReader.close();
            outputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

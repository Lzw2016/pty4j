package org.pty4j;

import org.junit.Test;

import java.io.IOException;

/**
 * 作者： lzw<br/>
 * 创建时间：2018-01-17 11:48 <br/>
 */
public class Test01 {

    //"G:",
    //"cd G:\\CodeDownloadPath\\loan-mall",
    //"mvn clean package -Dmaven.test.skip=true -U --global-settings=D:\\ToolsSoftware\\Maven\\settings.xml"
    @Test
    public void t01() throws Exception {
        Terminal terminal = new Terminal();
        terminal.initializeProcess();
        terminal.onCommand("G:\r\n");
        terminal.onCommand("cd G:\\CodeDownloadPath\\loan-mall\r\n");
        terminal.onCommand("mvn clean package -Dmaven.test.skip=true -U --global-settings=D:\\ToolsSoftware\\Maven\\settings.xml\r\n");
        terminal.waitFor();

//        StringBuilder stringBuilder = terminal.getStringBuilder();
    }

    @Test
    public void t02() throws Exception {
        Terminal terminal = new Terminal();
        terminal.initializeProcess();
        terminal.onCommand("C:\r\n");
        terminal.onCommand("cd C:\\Users\\lzw\\Desktop\\react\\clever-devops-ui\r\n");
        terminal.onCommand("cnpm i\r\n");
        terminal.waitFor();
    }

    @Test
    public void t03() throws IOException {
//        URL url = PtyUtil.class.getResource("");
//        JarURLConnection jarCon = (JarURLConnection) url.openConnection();
//        JarFile jarFile = jarCon.getJarFile();
//        Enumeration<JarEntry> jarEntrys = jarFile.entries();
//        while (jarEntrys.hasMoreElements()) {
//            JarEntry entry = jarEntrys.nextElement();
//            String name = entry.getName();
//            System.out.println(name);
//        }
    }
}

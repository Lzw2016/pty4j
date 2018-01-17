package org.pty4j.util;

import com.google.common.collect.Lists;
import com.sun.jna.Platform;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import sun.net.www.protocol.file.FileURLConnection;

import java.io.File;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author traff
 */
public class PtyUtil {
    private static final String OS_VERSION = System.getProperty("os.version").toLowerCase();

    private final static String PTY_LIB_FOLDER;

    static {
        final String userHome = System.getProperty("user.home");
        final String pty4jPath = FilenameUtils.concat(userHome, ".pty4j");
        try {
            final String basePath = "libpty";
            URL url = PtyUtil.class.getResource("/" + basePath);
            URLConnection urlConnection = url.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                JarURLConnection jarCon = (JarURLConnection) url.openConnection();
                JarFile jarFile = jarCon.getJarFile();
                Enumeration<JarEntry> jarEntryList = jarFile.entries();
                while (jarEntryList.hasMoreElements()) {
                    JarEntry entry = jarEntryList.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(basePath)) {
                        continue;
                    }
                    String path = name.substring(name.indexOf('/') + 1, name.length());
                    if (path.length() <= 0) {
                        continue;
                    }
                    if (!entry.isDirectory()) {
                        try (InputStream inputStream = PtyUtil.class.getClassLoader().getResourceAsStream(name)) {
                            FileUtils.copyInputStreamToFile(inputStream, new File(FilenameUtils.concat(pty4jPath, path)));
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (urlConnection instanceof FileURLConnection) {
                FileURLConnection fileURLConnection = (FileURLConnection) url.openConnection();
                FileUtils.copyDirectory(new File(url.getPath()), new File(pty4jPath));
            } else {
                throw new IllegalStateException("不支持的 URLConnection 类型" + urlConnection.getClass());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        PTY_LIB_FOLDER = pty4jPath;
    }

    public static String[] toStringArray(Map<String, String> environment) {
        if (environment == null) return new String[0];
        return Lists.newArrayList(environment.entrySet()).stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }

    public static File resolveNativeLibrary() {
        String libFolderPath = getPtyLibFolderPath();

        if (libFolderPath != null) {

            File libFolder = new File(libFolderPath);
            File lib = resolveNativeLibrary(libFolder);

            lib = lib.exists() ? lib : resolveNativeLibrary(new File(libFolder, "libpty"));

            if (!lib.exists()) {
                throw new IllegalStateException(String.format("Couldn't find %s, jar folder %s", lib.getName(), libFolder.getAbsolutePath()));
            }

            return lib;
        } else {
            throw new IllegalStateException("Couldn't detect lib folder");
        }
    }


    /**
     * 得到当前系统环境对应的平台库文件路径
     *
     * @param fileName 文件名
     */
    public static File resolveNativeFile(String fileName) {
        File libFolder = new File(getPtyLibFolderPath());
        File file = resolveNativeFile(libFolder, fileName);
        return file.exists() ? file : resolveNativeFile(new File(libFolder, "libpty"), fileName);
    }

    /**
     * 得到当前系统环境对应的平台库文件的 基本路径
     */
    private static String getPtyLibFolderPath() {
        return PTY_LIB_FOLDER;
    }

    /**
     * 得到当前系统环境对应的平台库文件路径
     *
     * @param parent 基本路径 ..../libpty
     */
    private static File resolveNativeLibrary(File parent) {
        return resolveNativeFile(parent, getNativeLibraryName());
    }

    /**
     * 得到当前系统环境对应的平台库文件路径
     *
     * @param parent   基本路径 ..../libpty
     * @param fileName 文件名
     */
    private static File resolveNativeFile(File parent, String fileName) {
        final File path = new File(parent, getPlatformFolder());
        String arch = Platform.is64Bit() ? "x86_64" : "x86";
        String prefix = isWinXp() ? "xp" : arch;
        if (new File(parent, prefix).exists()) {
            return new File(new File(parent, prefix), fileName);
        } else {
            return new File(new File(path, prefix), fileName);
        }
    }

    /**
     * 得到当前系统环境对应的平台库文件所在文件夹的文件夹名
     */
    private static String getPlatformFolder() {
        String result;
        if (Platform.isMac()) {
            result = "macosx";
        } else if (Platform.isWindows()) {
            result = "win";
        } else if (Platform.isLinux()) {
            result = "linux";
        } else if (Platform.isFreeBSD()) {
            result = "freebsd";
        } else if (Platform.isOpenBSD()) {
            result = "openbsd";
        } else {
            throw new IllegalStateException("Platform " + Platform.getOSType() + " is not supported");
        }
        return result;
    }

    /**
     * 得到当前系统环境对应的平台库文件名称
     */
    private static String getNativeLibraryName() {
        String result;
        if (Platform.isMac()) {
            result = "libpty.dylib";
        } else if (Platform.isWindows()) {
            result = "winpty.dll";
        } else if (Platform.isLinux() || Platform.isFreeBSD() || Platform.isOpenBSD()) {
            result = "libpty.so";
        } else {
            throw new IllegalStateException("Platform " + Platform.getOSType() + " is not supported");
        }
        return result;
    }

    /**
     * 是否是WinXP系统
     */
    private static boolean isWinXp() {
        return Platform.isWindows() && (OS_VERSION.equals("5.1") || OS_VERSION.equals("5.2"));
    }
}

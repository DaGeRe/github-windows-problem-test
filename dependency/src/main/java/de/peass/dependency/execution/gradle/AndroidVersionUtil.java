package de.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import de.peass.dependency.execution.GradleParseUtil;

/**
 * Manages allowed android versions; all allowed android-versions should be specified in dependency/src/main/resources/allowed_android_versions.txt
 * @author reichelt
 *
 */
public class AndroidVersionUtil {

   private static Map<Integer, String> versions = new LinkedHashMap<>();
   private static Set<String> acceptedVersion = new HashSet<>();

   static {
      final ClassLoader classLoader = GradleParseUtil.class.getClassLoader();
      final URL versionFile = classLoader.getResource("allowed_android_versions.txt");
      if (versionFile != null) {
         try {
            final String[] runningAndroidVersions = IOUtils.toString(versionFile, Charset.defaultCharset()).split("\n");
            for (final String line : runningAndroidVersions) {
               final String version = line.substring(line.indexOf(';') + 1);
               versions.put(getMajorVersion(version), version.replace("\r", ""));
               acceptedVersion.add(version);
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
      } else {
         System.out.println("No version file existing!");
      }

      final File gradle = new File(System.getenv("user.home"), ".gradle");
      if (!gradle.exists()) {
         gradle.mkdir();
      }
   }

   private static int getMajorVersion(final String versionString) {
      final int dotIndex = versionString.indexOf('.');
      if (dotIndex != -1) {
         final String part = versionString.substring(0, dotIndex);
         final int parsed = Integer.parseInt(part);
         return parsed;
      } else {
         return Integer.parseInt(versionString);
      }

   }

   public static String getRunningVersion(final String versionString) {
      int majorVersion = getMajorVersion(versionString);
      return versions.get(majorVersion);
   }
}

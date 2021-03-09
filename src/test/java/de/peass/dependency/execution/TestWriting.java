package de.peass.dependency.execution;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class TestWriting {

   private static final File CURRENT = new File(new File("target"), "current_test");
   
   @Test
   public void testNoUpdate() throws IOException {
      final File gradleFile = new File("src/test/resources/gradle/differentPlugin.gradle");

      final File destFile = new File(CURRENT, "build.gradle");
      FileUtils.copyFile(gradleFile, destFile);

      GradleParseUtil.doSomethingWithFile(destFile);

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.forName("UTF-8"));
      Assert.assertThat(gradleFileContents, Matchers.anyOf(Matchers.containsString("Test: 1"),
            Matchers.containsString("Test: 2")));
   }

   @Test
   public void testBuildtoolUpdate() throws IOException {
      final File gradleFile = new File("src/test/resources/gradle/build.gradle");
      testUpdate(gradleFile, true);

      final File gradleFile2 = new File("src/test/resources/gradle/v2.gradle");
      testUpdate(gradleFile2, true);
   }

   @Test
   public void testAndroidLib() throws IOException {
      final File gradleFile3 = new File("src/test/resources/gradle/androidlib.gradle");
      testUpdate(gradleFile3, false);
   }

   public void testUpdate(final File gradleFile, final boolean buildtools) throws IOException {
      final File destFile = new File(CURRENT, "build.gradle");
      FileUtils.copyFile(gradleFile, destFile);

      GradleParseUtil.doSomethingWithFile(destFile);

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.forName("UTF-8"));

      if (buildtools) {
         Assert.assertThat(gradleFileContents, Matchers.anyOf(Matchers.containsString("Test: 1"),
                     Matchers.containsString("Test: 2")));
      }
   }
}

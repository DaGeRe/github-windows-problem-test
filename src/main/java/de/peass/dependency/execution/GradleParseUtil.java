package de.peass.dependency.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class GradleParseUtil {

   public static void doSomethingWithFile(final File buildfile) {
      try {
         final List<String> gradleFileContents = Files.readAllLines(Paths.get(buildfile.toURI()));

         BufferedWriter buildfileWriter = new BufferedWriter(new FileWriter(buildfile));
         for (String line : gradleFileContents) {
            buildfileWriter.write(line);
         }
         for (int i = 0; i < 10; i++) {
            buildfileWriter.write("Test: " + i);
         }
         buildfileWriter.flush();

//         Files.write(buildfile.toPath(), gradleFileContents, StandardCharsets.UTF_8);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

}

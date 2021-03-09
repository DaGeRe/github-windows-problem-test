package de.peass.dependency.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileWritingUtil {

   public static void doSomethingWithFile(final File exampleFile) {
      try {
         final List<String> fileContents = Files.readAllLines(Paths.get(exampleFile.toURI()));

         try (BufferedWriter buildfileWriter = new BufferedWriter(new FileWriter(exampleFile))){
            for (String line : fileContents) {
               buildfileWriter.write(line);
            }
            for (int i = 0; i < 10; i++) {
               buildfileWriter.write("Test: " + i);
            }
            buildfileWriter.flush();
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

}

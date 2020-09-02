package de.peass.dependency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.peass.TestConstants;
import de.peass.dependency.execution.TestExecutor;
import de.peass.testtransformation.JUnitTestTransformer;

public class TestDependencyManager {
   
   private static final Logger LOG = LogManager.getLogger(TestDependencyManager.class);
   
   @Test
   public void testBigFolderDeletion() throws IOException, InterruptedException, XmlPullParserException {
      final PeASSFolders folders = new PeASSFolders(TestConstants.projectFolder);

      final TestExecutor testExecutorMock = Mockito.mock(TestExecutor.class);

      final File testFolder = new File(folders.getTempMeasurementFolder(), "MyTestClass/15231312");
      final File rubishFile = new File(testFolder, "myRubish.txt");
      
      prepareMock(folders, testExecutorMock, testFolder, rubishFile);

      DependencyManager manager = new DependencyManager(testExecutorMock, folders, Mockito.mock(JUnitTestTransformer.class));

      manager.setDeleteFolderSize(1);
      manager.initialyGetTraces("1");
      
      Assert.assertFalse(rubishFile.exists());
      Assert.assertFalse(testFolder.exists());
   }

   private void prepareMock(final PeASSFolders folders, final TestExecutor testExecutorMock, final File testFolder, final File rubishFile) {
      Mockito.doAnswer(new Answer<Void>() {
         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            folders.getTempMeasurementFolder().mkdir();
            testFolder.mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(rubishFile))) {
               for (int i = 0; i < 2 * 1024 * 1024; i++) {
                  writer.write(i % 256);
               }
            }
            LOG.info("Written: {}", rubishFile.length());
            return null;
         }
      }).when(testExecutorMock).executeAllKoPeMeTests(Mockito.any());
   }
}
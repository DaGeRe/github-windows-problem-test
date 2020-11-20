package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.peass.TestConstants;
import de.peass.ci.helper.GitProjectBuilder;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.InitialDependency;
import de.peass.dependency.persistence.Version;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import de.peass.vcs.GitCommit;

public class TestTestChooser {
   
   private String testVersion;
   
   @Test
   public void testBasicChoosing() throws InterruptedException, IOException, JAXBException {
      Dependencies dependencies = createDependencies();
      
      TestChooser chooser = new TestChooser(false, new File("target/"), new PeASSFolders(TestConstants.CURRENT_FOLDER), new GitCommit(testVersion, "", "", ""), new File("target/views"), new File("target/properties"), 1, new LinkedList<>());
      Set<TestCase> tests = chooser.getTestSet(dependencies);
      
      Assert.assertEquals(tests.iterator().next(), new TestCase("defaultpackage.TestMe#testMe"));
   }
   
   @Test
   public void testViewChoosing() throws InterruptedException, IOException, JAXBException {
      Dependencies dependencies = createDependencies();
      
      TestChooser chooser = new TestChooser(true, new File("target/"), new PeASSFolders(TestConstants.CURRENT_FOLDER), new GitCommit(testVersion, "", "", ""), new File("target/views"), new File("target/properties"), 1, new LinkedList<>());
      Set<TestCase> tests = chooser.getTestSet(dependencies);
      
      Assert.assertEquals(tests.iterator().next(), new TestCase("defaultpackage.TestMe#testMe"));
   }

   private Dependencies createDependencies() throws InterruptedException, IOException {
      GitProjectBuilder builder = new GitProjectBuilder(TestConstants.CURRENT_FOLDER, new File("../dependency/src/test/resources/dependencyIT/basic_state"));
      builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/normal_change"), "Version 1");
      
      Dependencies dependencies = new Dependencies();
      final InitialDependency initial = new InitialDependency();
      initial.getEntities().add(new ChangedEntity("defaultpackage.NormalDependency#executeThing", ""));
      dependencies.getInitialversion().setVersion(builder.getTags().get(0));
      dependencies.getInitialversion().getInitialDependencies().put(new ChangedEntity("defaultpackage.TestMe#testMe", ""), initial);
      
      final Version version = new Version();
      version.getChangedClazzes().put(new ChangedEntity("defaultpackage.NormalDependency#executeThing", ""), new TestSet("defaultpackage.TestMe#testMe"));
      testVersion = builder.getTags().get(1);
      dependencies.getVersions().put(testVersion, version);
      
      Constants.OBJECTMAPPER.writeValue(TestContinuousDependencyReader.dependencyFile, dependencies);
      
      VersionComparator.setDependencies(dependencies);
      return dependencies;
   }
}

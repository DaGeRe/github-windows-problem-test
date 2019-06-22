/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.peass.dependency;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.analysis.CalledMethodLoader;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.CalledMethods;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestDependencies;
import de.peass.dependency.analysis.data.TestExistenceChanges;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.changesreading.ClazzChangeData;

/**
 * Runs tests with kieker and reads the dependencies of tests for each version
 * 
 * @author reichelt
 *
 */
public class DependencyManager extends TestResultManager {

   private static final Logger LOG = LogManager.getLogger(DependencyManager.class);

   private final TestDependencies dependencies = new TestDependencies();

   /**
    * Creates a new ChangeTestClassesHandler for the given folder with the given groupId and projectId. The groupId and projectId are needed to determine where the results are
    * afterwards.
    * 
    * @param projectFolder
    */
   public DependencyManager(final File projectFolder, final int timeout) {
      super(projectFolder, timeout);
   }

   public TestDependencies getDependencyMap() {
      return dependencies;
   }

   public boolean initialyGetTraces(final String version) throws IOException, InterruptedException, XmlPullParserException {
      if (folders.getTempMeasurementFolder().exists()) {
         FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
      }

      final ModuleClassMapping mapping = new ModuleClassMapping(executor);
      executor.loadClasses();
      final File logFile = new File(folders.getLogFolder(), version + File.separator + "init_log.txt");
      logFile.getParentFile().mkdirs();
      executor.executeAllKoPeMeTests(logFile);

      if (folders.getTempMeasurementFolder().exists()) {
         return readResultFules(mapping);
      } else {
         return printErrors();
      }
   }

   private boolean printErrors() throws IOException {
      try {
         boolean sourceFound = false;
         sourceFound = searchTestFiles(sourceFound);
         if (sourceFound) {
            LOG.debug("No result data available - error occured?");
            return false;
         } else {
            LOG.debug("No result data available, but no test-classes existing - so it is ok.");
            return true;
         }
      } catch (final XmlPullParserException e) {
         e.printStackTrace();
         return false;
      }
   }

   private boolean searchTestFiles(boolean sourceFound) throws IOException, XmlPullParserException {
      for (final File module : executor.getModules()) {
         final File testSourceFolder = new File(module, "src/test");
         if (testSourceFolder.exists()) {
            final Collection<File> javaTestFiles = FileUtils.listFiles(testSourceFolder, new WildcardFileFilter("*test*.java", IOCase.INSENSITIVE), TrueFileFilter.INSTANCE);
            if (javaTestFiles.size() > 0) {
               sourceFound = true;
            }
         }
      }
      return sourceFound;
   }

   private boolean readResultFules(final ModuleClassMapping mapping) {
      final Collection<File> xmlFiles = FileUtils.listFiles(folders.getTempMeasurementFolder(), new WildcardFileFilter("*.xml"), TrueFileFilter.INSTANCE);
      LOG.debug("Initial test execution finished, starting result collection, analyzing {} files", xmlFiles.size());
      for (final File testResultFile : xmlFiles) {
         final String testClassName = testResultFile.getParentFile().getName();
         final String testMethodName = testResultFile.getName().substring(0, testResultFile.getName().length() - 4); // remove
         // .xml
         final File parent = testResultFile.getParentFile();
         final String moduleOfClass = mapping.getModuleOfClass(testClassName);
         if (moduleOfClass == null) {
            throw new RuntimeException("Module of class " + testClassName + " is null");
         }
         final ChangedEntity entity = new ChangedEntity(testClassName, moduleOfClass, testMethodName);
         updateDependenciesOnce(entity, parent, mapping);
      }
      LOG.debug("Result collection finished");

      final File movedInitialResults = new File(folders.getTempMeasurementFolder().getParentFile(), "initialresults_kieker");
      folders.getTempMeasurementFolder().renameTo(movedInitialResults);
      cleanAboveSize(movedInitialResults, 100, "dat");
      return true;
   }

   /**
    * Updates Dependencies of the given testClassName and the given testMethodName based upon the file where the kieker-results are stored
    * 
    * @param testClassName
    * @param testMethodName
    * @param parent
    */
   public void updateDependenciesOnce(final ChangedEntity testClassName, final File parent, final ModuleClassMapping mapping) {
      LOG.debug("Parent: " + parent);
      final File kiekerResultFolder = findKiekerFolder(testClassName.getMethod(), parent);

      if (kiekerResultFolder == null) {
         LOG.error("No kieker folder found: " + parent);
         return;
      }

      final long size = FileUtils.sizeOfDirectory(kiekerResultFolder);
      final long sizeInMB = size / (1024 * 1024);

      LOG.debug("Size: {} Folder: {}", sizeInMB, kiekerResultFolder);
      if (sizeInMB > CalledMethodLoader.TRACE_MAX_SIZE) {
         return;
      }

      final File kiekerOutputFile = new File(folders.getLogFolder(), "ausgabe_kieker.txt");

      final Map<ChangedEntity, Set<String>> calledClasses = new CalledMethodLoader(kiekerResultFolder, mapping).getCalledMethods(kiekerOutputFile);

      for (final Iterator<ChangedEntity> iterator = calledClasses.keySet().iterator(); iterator.hasNext();) {
         final ChangedEntity entity = iterator.next();
         final String wholeClassName = entity.getJavaClazzName();

         // ignore inner class part, because it is in the same file - if the file exists, it is very likely that a subclass, which is in the logs, exists also
         final String outerClazzName = ClazzFinder.getOuterClass(wholeClassName);
         LOG.trace("Testing: " + outerClazzName);
         if (!executor.getExistingClasses().contains(outerClazzName)) {
            // Removes classes not in package
            iterator.remove();
         } else {
            LOG.trace("Existing: " + outerClazzName);
         }
      }
      for (final Iterator<ChangedEntity> iterator = calledClasses.keySet().iterator(); iterator.hasNext();) {
         final ChangedEntity clazz = iterator.next();
         if (clazz.getModule() == null) {
            throw new RuntimeException("Class " + clazz.getJavaClazzName() + " has no module!");
         }
      }

      LOG.debug("Test: {} ", testClassName);
      LOG.debug("Kieker: {} Dependencies: {}", kiekerResultFolder.getAbsolutePath(), calledClasses.size());
      setDependencies(testClassName, calledClasses);

   }

   private File findKiekerFolder(final String testMethodName, final File parent) {
      final File[] listFiles = parent.listFiles(new FileFilter() {
         @Override
         public boolean accept(final File pathname) {
            return pathname.getName().matches("[0-9]*");
         }
      });
      LOG.debug("Kieker-Files: {}", listFiles.length);
      if (listFiles.length == 0) {
         LOG.info("No result folder existing - probably a package name change?");
         LOG.info("Files: {}", Arrays.toString(parent.list()));
         return null;
      }
      for (final File kiekerFolder : listFiles) {
         LOG.debug("Analysing Folder: {} {}", kiekerFolder.getAbsolutePath(), testMethodName);
         final File kiekerNextFolder = new File(kiekerFolder, testMethodName);
         if (kiekerNextFolder.exists()) {
            final File kiekerResultFolder = kiekerNextFolder.listFiles()[0];
            LOG.debug("Test: " + testMethodName);
            return kiekerResultFolder;
         }
      }
      return null;
   }

   /**
    * Since we have no information about complete dependencies when reading an old dependencyfile, just add dependencies
    * 
    * @param testClassName
    * @param testMethodName
    * @param calledClasses Map from name of the called class to the methods of the class that are called
    */
   public void addDependencies(final ChangedEntity testClassName, final Map<ChangedEntity, Set<String>> calledClasses) {
      final Map<ChangedEntity, Set<String>> testDependencies = dependencies.getOrAddDependenciesForTest(testClassName);
      for (final Map.Entry<ChangedEntity, Set<String>> calledEntity : calledClasses.entrySet()) {
         LOG.debug("adding: " + calledEntity.getKey() + " Module: " + calledEntity.getKey().getModule());
         LOG.debug(testDependencies.keySet());
         final Set<String> oldSet = testDependencies.get(calledEntity.getKey());
         if (oldSet != null) {
            oldSet.addAll(calledEntity.getValue());
         } else {
            testDependencies.put(calledEntity.getKey(), calledEntity.getValue());
         }
      }
   }

   public void setDependencies(final ChangedEntity testClassName, final Map<ChangedEntity, Set<String>> calledClasses) {
      final Map<ChangedEntity, Set<String>> testDependencies = dependencies.getOrAddDependenciesForTest(testClassName);
      testDependencies.putAll(calledClasses);
   }

   /**
    * Updates the dependencies of the current version by running each testclass once. The testcases, that have been added in this version, are returned (since they can not be
    * determined from the old dependency information or the svn diff directly). TODO: What if testcases are removed?
    * 
    * @param testsToUpdate
    * @return
    * @throws IOException
    * @throws XmlPullParserException
    * @throws InterruptedException
    */
   public TestExistenceChanges updateDependencies(final TestSet testsToUpdate, final String version, final ModuleClassMapping mapping) throws IOException, XmlPullParserException {
      final Map<ChangedEntity, Map<ChangedEntity, Set<String>>> oldDepdendencies = dependencies.getCopiedDependencies();
      
      // Remove all old dependencies where changes happened, because they may
      // have been deleted
      for (final Entry<ChangedEntity, Set<String>> className : testsToUpdate.entrySet()) {
         for (final String method : className.getValue()) {
            final ChangedEntity methodEntity = className.getKey().copy();
            methodEntity.setMethod(method);
            dependencies.getDependencyMap().remove(methodEntity);
         }
      }

      LOG.debug("Beginne Abhängigkeiten-Aktuallisierung für {} Klassen", testsToUpdate.getClasses().size());

      final TestExistenceChanges changes = new TestExistenceChanges();

      for (final Entry<ChangedEntity, Set<String>> entry : testsToUpdate.entrySet()) {
         final String testClassName = entry.getKey().getJavaClazzName();
         final File testclazzFolder = getTestclazzFolder(entry);
         LOG.debug("Suche in {} Existiert: {} Ordner: {} Tests: {} ", testclazzFolder.getAbsolutePath(), testclazzFolder.exists(), testclazzFolder.isDirectory(), entry.getValue());
         if (testclazzFolder.exists()) {
            updateMethods(mapping, changes, entry, testClassName, testclazzFolder);
         } else {
            checkRemoved(oldDepdendencies, changes, entry, testClassName, testclazzFolder);
         }
      }

      findAddedTests(oldDepdendencies, changes);
      return changes;
   }

   File getTestclazzFolder(final Entry<ChangedEntity, Set<String>> entry) throws FileNotFoundException, IOException, XmlPullParserException {
      final File testclazzFolder;
      if (entry.getKey().getModule().equals("")) {
         final File xmlFileFolder = getXMLFileFolder(folders.getProjectFolder());
         testclazzFolder = new File(xmlFileFolder, entry.getKey().getJavaClazzName());
      } else {
         final File moduleFolder = new File(folders.getProjectFolder(), entry.getKey().getModule());
         final File xmlFileFolder = getXMLFileFolder(moduleFolder);
         testclazzFolder = new File(xmlFileFolder, entry.getKey().getJavaClazzName());
      }
      return testclazzFolder;
   }

   void updateMethods(final ModuleClassMapping mapping, final TestExistenceChanges changes, final Entry<ChangedEntity, Set<String>> entry, final String testClassName,
         final File testclazzFolder) {
      final Set<String> notFound = new TreeSet<>();
      notFound.addAll(entry.getValue());
      for (final File testResultFile : testclazzFolder.listFiles((FileFilter) new WildcardFileFilter("*.xml"))) {
         final String testClassName2 = testResultFile.getParentFile().getName();
         if (!testClassName2.equals(testClassName)) {
            LOG.error("Testclass " + testClassName + " != " + testClassName2);
         }
         final File parent = testResultFile.getParentFile();
         final String testMethodName = testResultFile.getName().substring(0, testResultFile.getName().length() - 4);
         final String module = mapping.getModuleOfClass(testClassName);
         updateDependenciesOnce(new ChangedEntity(testClassName, module, testMethodName), parent, mapping);
         notFound.remove(testMethodName);
      }
      LOG.debug("Removed tests: {}", notFound);
      for (final String testMethodName : notFound) {
         final ChangedEntity entity = entry.getKey().copy();
         entity.setMethod(testMethodName);
         dependencies.removeTest(entity);
         // testsToUpdate.removeTest(entry.getKey(), testMethodName);
         changes.addRemovedTest(new TestCase(testClassName, testMethodName, entry.getKey().getModule()));
      }
   }

   void checkRemoved(final Map<ChangedEntity, Map<ChangedEntity, Set<String>>> oldDepdendencies, final TestExistenceChanges changes, final Entry<ChangedEntity, Set<String>> entry,
         final String testClassName, final File testclazzFolder) {
      LOG.error("Testclass {} does not exist anymore or does not create results. Folder: {}", entry.getKey(), testclazzFolder);
      final TestCase testclass = new TestCase(testClassName, "", entry.getKey().getModule());
      boolean oldContained = false;
      for (final ChangedEntity oldTest : oldDepdendencies.keySet()) {
         if (testclass.getClazz().equals(oldTest.getClazz()) && testclass.getModule().equals(oldTest.getModule())) {
            oldContained = true;
         }
      }
      if (oldContained) {
         changes.addRemovedTest(testclass);
      } else {
         LOG.error("Test was only added incorrect, no removing necessary.");
      }
   }

   /**
    * A method is unknown if a class-wide change happened, e.g. if a new subclass is declared and because of this change a new testcase needs to be called.
    * 
    * @param oldDepdendencies
    * @param changes
    */
   private void findAddedTests(final Map<ChangedEntity, Map<ChangedEntity, Set<String>>> oldDepdendencies, final TestExistenceChanges changes) {
      for (final Map.Entry<ChangedEntity, CalledMethods> newDependency : dependencies.getDependencyMap().entrySet()) {
         // testclass -> depending class -> method
         final ChangedEntity testcase = newDependency.getKey();
         if (!oldDepdendencies.containsKey(testcase)) {
            changes.addAddedTest(testcase.onlyClazz(), testcase);
            for (final Map.Entry<ChangedEntity, Set<String>> newCallees : newDependency.getValue().getCalledMethods().entrySet()) {
               final ChangedEntity changedclass = newCallees.getKey();
               for (final String changedMethod : newCallees.getValue()) {
                  // Since the testcase is new, is is always caused
                  // primarily by a change of the test class, and not of
                  // any other changed class
                  final ChangedEntity methodEntity = changedclass.copy();
                  methodEntity.setMethod(changedMethod);
                  changes.addAddedTest(methodEntity, testcase);
               }
            }
         }
      }
   }

   /**
    * Returns the tests that need to be run in the current version based on the given changes, i.e. the given changed classes and changed methods
    * 
    * @param map from changed classes to changed methods (or, if class changed as a whole, an empty set)
    * @return Map from testclasses to the test methods of the class that need to be run
    */
   public TestSet getTestsToRun(final Map<ChangedEntity, ClazzChangeData> changedClassNames) {
      final TestSet testsToRun = new TestSet();
      for (final ChangedEntity testName : changedClassNames.keySet()) {
         if (testName.getJavaClazzName().toLowerCase().contains("test")) {
            testsToRun.addTest(testName, null);
         }
      }
      for (final Map.Entry<ChangedEntity, CalledMethods> testDependencies : dependencies.getDependencyMap().entrySet()) {
         final Set<ChangedEntity> currentTestDependencies = testDependencies.getValue().getCalledClasses();
         for (final ChangedEntity changedClass : changedClassNames.keySet()) {
            LOG.trace("Prüfe Abhängigkeiten für {} von {}", changedClass, testDependencies.getKey());
            LOG.trace("Abhängig: {} Abhängig von Testklasse: {}", currentTestDependencies.contains(changedClass), changedClass.equals(testDependencies.getKey()));
            if (currentTestDependencies.contains(changedClass)) {
               LOG.info("Test " + testDependencies.getKey() + " benötigt geänderte Klasse " + changedClass);
               final String testMethodName = testDependencies.getKey().getMethod();
               final ChangedEntity entity = testDependencies.getKey().copy();
               entity.setMethod(null);
               testsToRun.addTest(entity, testMethodName);
               break;
            }
         }
      }
      return testsToRun;
   }
}
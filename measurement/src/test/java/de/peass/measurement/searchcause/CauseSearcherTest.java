package de.peass.measurement.searchcause;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.kieker.BothTreeReader;
import de.peass.measurement.searchcause.treeanalysis.LevelDifferingDeterminer;
import kieker.analysis.exception.AnalysisConfigurationException;

public class CauseSearcherTest {

   private final CauseSearcherConfig causeSearchConfig = new CauseSearcherConfig(new TestCase("Test#test"), true, false, 5.0);
   private final MeasurementConfiguration measurementConfig = new MeasurementConfiguration(3, "2", "1");
   
   public void cleanup() {
      final File folder = new File("target/test_peass/");
      try {
         FileUtils.deleteDirectory(folder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }
   
   @Test
   public void testMeasurement() throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      final CallTreeNode root1 = new TreeBuilder().getRoot();
      final CallTreeNode root2 = new TreeBuilder().getRoot();
      
      final LevelDifferingDeterminer lcs = new LevelDifferingDeterminer(Arrays.asList(new CallTreeNode[] { root1 }),
            Arrays.asList(new CallTreeNode[] { root2 }),
            causeSearchConfig,
            measurementConfig);
      lcs.calculateDiffering();
      Assert.assertEquals(2, lcs.getDifferingPredecessor().size());

      lcs.analyseNode(root1);
      Assert.assertEquals(0, lcs.getTreeStructureDifferingNodes().size());
   }
   
   @Test
   public void testCauseSearching()
         throws InterruptedException, IOException, IllegalStateException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException, JAXBException {
      cleanup();
      final File folder = new File("target/test/");
      folder.mkdir();

      final BothTreeReader treeReader = Mockito.mock(BothTreeReader.class);
      final CallTreeNode root1 = new TreeBuilder().getRoot();
      final CallTreeNode root2 = new TreeBuilder().getRoot();

      Mockito.when(treeReader.getRootPredecessor()).thenReturn(root1);
      Mockito.when(treeReader.getRootVersion()).thenReturn(root2);
      final LevelMeasurer measurer = Mockito.mock(LevelMeasurer.class);

      final CauseSearcher searcher = new CauseSearcher(treeReader, causeSearchConfig, measurer, measurementConfig,
            new CauseSearchFolders(folder));
      final List<ChangedEntity> changes = searcher.search();

      System.out.println(changes);
      Assert.assertEquals(1, changes.size());
      Assert.assertEquals("ClassB#methodB", changes.get(0).toString());
   }
}

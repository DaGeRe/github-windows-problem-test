package de.peass.measurement.searchcause.kieker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.CauseSearcherConfig;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.treeanalysis.TreeUtil;
import de.peass.utils.Constants;
import kieker.analysis.exception.AnalysisConfigurationException;

public class BothTreeReader {

   private static final Logger LOG = LogManager.getLogger(BothTreeReader.class);

   private CallTreeNode rootPredecessor;
   private CallTreeNode rootVersion;

   private final CauseSearcherConfig causeSearchConfig;
   private final MeasurementConfiguration config;
   private final CauseSearchFolders folders;

   public BothTreeReader(final CauseSearcherConfig causeSearchConfig, final MeasurementConfiguration config, final CauseSearchFolders folders) {
      super();
      this.causeSearchConfig = causeSearchConfig;
      this.config = config;
      this.folders = folders;
   }

   public void readTrees() throws InterruptedException, IOException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException {
      final File potentialCacheFileOld = new File(folders.getTreeCacheFolder(config.getVersion(), causeSearchConfig.getTestCase()), config.getVersionOld());
      final File potentialCacheFile = new File(folders.getTreeCacheFolder(config.getVersion(), causeSearchConfig.getTestCase()), config.getVersion());
      if (potentialCacheFile.exists() && potentialCacheFileOld.exists()) {
         LOG.info("Using cache!");
         rootPredecessor = Constants.OBJECTMAPPER.readValue(potentialCacheFileOld, CallTreeNode.class);
         rootVersion = Constants.OBJECTMAPPER.readValue(potentialCacheFile, CallTreeNode.class);
      } else {
         determineTrees();
         LOG.info("Writing to cache");
         Constants.OBJECTMAPPER.writeValue(potentialCacheFileOld, rootPredecessor);
         Constants.OBJECTMAPPER.writeValue(potentialCacheFile, rootVersion);
      }
   }

   private void determineTrees() throws InterruptedException, IOException, FileNotFoundException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException {
      final TreeReader resultsManager = TreeReaderFactory.createTreeReader(folders, config.getVersionOld(), config.getTimeout());
      rootPredecessor = resultsManager.getTree(causeSearchConfig.getTestCase(), config.getVersionOld());

      final TreeReader resultsManagerPrevious = TreeReaderFactory.createTreeReader(folders, config.getVersion(), config.getTimeout());
      rootVersion = resultsManagerPrevious.getTree(causeSearchConfig.getTestCase(), config.getVersion());
      LOG.info("Traces equal: {}", TreeUtil.areTracesEqual(rootPredecessor, rootVersion));
   }

   public CallTreeNode getRootPredecessor() {
      return rootPredecessor;
   }

   public CallTreeNode getRootVersion() {
      return rootVersion;
   }
}

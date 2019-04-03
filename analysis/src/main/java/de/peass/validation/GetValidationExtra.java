package de.peass.validation;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.measurement.analysis.DataAnalyser;
import de.peass.utils.Constants;
import de.peran.FolderSearcher;

/**
 * Reads validation data from extra executed validation-experiments (are saved in $PEASS_REPOS/measurementdata/validation/).
 * 
 * @author reichelt
 *
 */
public class GetValidationExtra {

   private static final Logger LOG = LogManager.getLogger(GetValidationExtra.class);

   public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
      File repoFolder = new File(args.length > 0 ? args[0] : System.getenv(Constants.PEASS_REPOS));
      if (!repoFolder.exists()) {
         LOG.error("Folder " + repoFolder.getAbsolutePath() + " should exist!");
      }

      final File commitFolder = new File(repoFolder, "properties/validation/");

      File dependencyFolder = new File(repoFolder, "dependencies-final");
      File changeFolder = new File(repoFolder, "measurementdata/validation/results");
      File validationFile = new File(repoFolder, "measurementdata/validation/validation.json");
      GetValidationdata.getValidation(changeFolder, dependencyFolder, commitFolder, validationFile);
   }
}

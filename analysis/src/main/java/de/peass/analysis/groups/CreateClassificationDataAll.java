package de.peass.analysis.groups;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.all.RepoFolders;
import de.peass.analysis.changes.ProjectChanges;
import de.peran.FolderSearcher;

public class CreateClassificationDataAll {
   
   private static final Logger LOG = LogManager.getLogger(CreateClassificationDataAll.class);

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      RepoFolders folders = new RepoFolders();
      for (final File projectFile : folders.getResultsFolder().listFiles()) {
         final String project = projectFile.getName();
         final File changeFile = new File(projectFile, project + ".json");
         final File goalFile = new File(folders.getClassificationFolder(), project + ".json");
         if (changeFile.exists()) {
            final ProjectChanges changes = FolderSearcher.MAPPER.readValue(changeFile, ProjectChanges.class);
            CreateClassificationData.createClassificationData(changes, goalFile, project);
         } else {
            LOG.error("No Change File: " + project);
         }
      }
   }
}

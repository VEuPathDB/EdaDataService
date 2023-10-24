package org.veupathdb.service.eda.download;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.generated.model.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FileStore {

  private static final Logger LOG = LogManager.getLogger(FileStore.class);

  // map from dataset hash -> (map from release dir -> (map from file name -> File>))
  private static class MetadataMap extends HashMap<String, Map<String, Map<String, FileInfo>>> { }
  private final MetadataMap _metadata;

  public FileStore(Path projectDir) {
    _metadata = init(projectDir);
  }

  public Optional<List<String>> getReleaseNames(String datasetHash) {
    return Optional.ofNullable(_metadata.get(datasetHash))
        .map(map -> new ArrayList<>(map.keySet()));
  }

  public Optional<List<File>> getFiles(String datasetHash, String release) {
    return Optional.ofNullable(_metadata.get(datasetHash))
        .flatMap(releaseMap -> Optional.ofNullable(releaseMap.get(release))
            .map(fileMap -> new ArrayList<>(fileMap.values())));
  }

  public Optional<Path> getFilePath(String datasetHash, String release, String fileName) {
    return Optional.ofNullable(_metadata.get(datasetHash))
        .flatMap(releaseMap -> Optional.ofNullable(releaseMap.get(release))
            .flatMap(fileMap -> Optional.ofNullable(fileMap.get(fileName))
                .map(FileInfo::getPath)));
  }

  private static MetadataMap init(Path projectDir) {
    try {
      LOG.info("Initializing file store at project dir: " + projectDir.toAbsolutePath());
      MetadataMap datasetMap = new MetadataMap();
      for (Path releaseDir : getContents(projectDir, FileStore::isDirectory)) {
        for (Path datasetHashDir : getContents(releaseDir, FileStore::isDirectory)) {

          // build map of files for this dataset+release combo
          Map<String, FileInfo> files = getContents(datasetHashDir, BasicFileAttributes::isRegularFile)
              .stream().map(FileInfo::new).collect(Collectors.toMap(FileInfo::getName, f -> f));

          // create an entry for this dataset if not already present
          Map<String, Map<String, FileInfo>> studyReleases = datasetMap.computeIfAbsent(
              datasetHashDir.getFileName().toString(),
              key -> new HashMap<>());

          // add file listing for this release
          studyReleases.put(releaseDir.getFileName().toString(), files);
        }
      }
      return datasetMap;
    }
    catch (IOException e) {
      LOG.error("[IO] Unable to initialize FileStore for project dir " + projectDir.toAbsolutePath(), e);
      throw new RuntimeException("Unable to read filesystem contents", e);
    }
    catch (Exception e) {
      LOG.error("[Gen] Unable to initialize FileStore for project dir " + projectDir.toAbsolutePath(), e);
      throw e;
    }
  }

  private static boolean isDirectory(BasicFileAttributes attrs) {
    return attrs.isDirectory() && !attrs.isSymbolicLink();
  }

  private static List<Path> getContents(Path parentDir, Predicate<BasicFileAttributes> parameters) throws IOException {
    return Files.find(parentDir, 1, (path, attrs) -> parameters.test(attrs)).collect(Collectors.toList());
  }

}

package org.veupathdb.service.eda.subset.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.Resources;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudyOverview;
import org.veupathdb.service.eda.ss.model.db.StudyFactory;
import org.veupathdb.service.eda.ss.model.db.StudyProvider;
import org.veupathdb.service.eda.ss.model.db.VariableFactory;
import org.veupathdb.service.eda.ss.model.reducer.MetadataFileBinaryProvider;
import org.veupathdb.service.eda.ss.model.variable.binary.BinaryFilesManager;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MetadataCache implements StudyProvider {
  private static final Logger LOG = LogManager.getLogger(MetadataCache.class);

  // instance fields
  private List<StudyOverview> _studyOverviews;  // cache the overviews
  private final BinaryFilesManager _binaryFilesManager;
  private final Supplier<StudyProvider> _sourceStudyProvider;
  private final Map<String, Study> _studies = new HashMap<>(); // cache the studies
  private final Map<String, Boolean> _studyHasFilesCache = new HashMap<>();
  private final ScheduledExecutorService _scheduledThreadPool = Executors.newScheduledThreadPool(1); // Shut this down.
  private final CountDownLatch _appDbInitSignal;

  public MetadataCache(BinaryFilesManager binaryFilesManager, CountDownLatch appDbInitSignal) {
    _binaryFilesManager = binaryFilesManager;
    _sourceStudyProvider = this::getCuratedStudyFactory; // Lazily initialize to ensure database connection is established before construction.
    _scheduledThreadPool.scheduleAtFixedRate(this::invalidateOutOfDateStudies, 0L, 5L, TimeUnit.MINUTES);
    _appDbInitSignal = appDbInitSignal;
  }

  // Visible for testing
  MetadataCache(BinaryFilesManager binaryFilesManager,
                StudyProvider sourceStudyProvider,
                Duration refreshInterval) {
    _binaryFilesManager = binaryFilesManager;
    _sourceStudyProvider = () -> sourceStudyProvider;
    _scheduledThreadPool.scheduleAtFixedRate(this::invalidateOutOfDateStudies, 0L,
        refreshInterval.toMillis(), TimeUnit.MILLISECONDS);
    _appDbInitSignal = null;
  }

  @Override
  public synchronized Study getStudyById(String studyId) {
    return _studies.computeIfAbsent(studyId,
        id -> getCuratedStudyFactory().getStudyById(id));
  }

  @Override
  public synchronized List<StudyOverview> getStudyOverviews() {
    if (_studyOverviews == null) {
      _studyOverviews = _sourceStudyProvider.get().getStudyOverviews();
    }
    return Collections.unmodifiableList(_studyOverviews);
  }

  private synchronized boolean studyHasFiles(String studyId) {
    _studyHasFilesCache.computeIfAbsent(studyId, _binaryFilesManager::studyHasFiles);
    return _studyHasFilesCache.get(studyId);
  }

  private StudyProvider getCuratedStudyFactory() {
    return new StudyFactory(
        Resources.getApplicationDataSource(),
        Resources.getAppDbSchema(),
        StudyOverview.StudySourceType.CURATED,
        new VariableFactory(
            Resources.getApplicationDataSource(),
            Resources.getAppDbSchema(),
            new MetadataFileBinaryProvider(_binaryFilesManager),
            this::studyHasFiles)
        );
  }

  public synchronized void clear() {
    _studyOverviews = null;
    _studies.clear();
    _studyHasFilesCache.clear();
  }

  public void shutdown() {
    _scheduledThreadPool.shutdown();
  }

  private void invalidateOutOfDateStudies() {
    if (_appDbInitSignal != null) {
      try {
        _appDbInitSignal.await();
      } catch (InterruptedException e) {
        return;
      }
    }
    LOG.info("Checking which studies are out of date in cache.");
    List<StudyOverview> dbStudies = _sourceStudyProvider.get().getStudyOverviews();
    List<Study> studiesToRemove = _studies.values().stream()
        .filter(study -> isOutOfDate(study, dbStudies))
        .toList();
    synchronized (this) {
      LOG.info("Removing the following out of date or missing studies from cache: "
          + studiesToRemove.stream().map(StudyOverview::getStudyId).collect(Collectors.joining(",")));

      // For each study with a study overview, check if the files exist and cache the result.
      dbStudies.forEach(study -> _studyHasFilesCache.put(study.getStudyId(), _binaryFilesManager.studyHasFiles(study.getStudyId())));

      // Replace study overviews with those available in DB.
      _studyOverviews = dbStudies;

      // Remove any studies with full metadata loaded if they have been modified. They will be lazily repopulated.
      _studies.entrySet().removeIf(study ->
          studiesToRemove.stream().anyMatch(removeStudy -> removeStudy.getStudyId().equals(study.getKey())));
    }
  }

  private boolean isOutOfDate(StudyOverview studyOverview, List<StudyOverview> dbStudies) {
    Optional<StudyOverview> matchingDbStudy = dbStudies.stream()
        .filter(dbStudy -> dbStudy.getStudyId().equals(studyOverview.getStudyId()))
        .findAny();
    // Study not in DB anymore, remove it from cache.
    if (matchingDbStudy.isEmpty()) {
      return true;
    }
    // If in DB, check if it's out of date.
    return matchingDbStudy.get().getLastModified().after(studyOverview.getLastModified());
  }
}


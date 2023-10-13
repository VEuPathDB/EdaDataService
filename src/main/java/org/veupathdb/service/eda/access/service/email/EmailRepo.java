package org.veupathdb.service.eda.access.service.email;

import io.vulpine.lib.query.util.basic.BasicPreparedReadQuery;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.service.eda.access.model.DatasetEmails;
import org.veupathdb.service.eda.access.repo.SQL;
import org.veupathdb.service.eda.access.service.QueryUtil;
import org.veupathdb.service.eda.access.util.SqlUtil;

public class EmailRepo
{
  private static EmailRepo instance;

  private final Logger log;

  public EmailRepo() {
    log = LogProvider.logger(getClass());
  }

  public DatasetEmails selectDatasetEmails(final String datasetId) throws Exception {
    log.trace("EmailRepo#selectDatasetEmails(String)");

    return new BasicPreparedReadQuery<>(
      SQL.Select.Datasets.Emails,
      QueryUtil.getInstance()::getAppDbConnection,
      EmailUtil.getInstance()::resultSetToEmail,
      SqlUtil.prepareSingleString(datasetId)
    ).execute().getValue();
  }

  public static EmailRepo getInstance() {
    if (instance == null)
      instance = new EmailRepo();

    return instance;
  }

  public static DatasetEmails getDatasetEmails(final String datasetId) throws Exception {
    return getInstance().selectDatasetEmails(datasetId);
  }
}

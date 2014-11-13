package org.molgenis.gaf;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.Writable;
import org.molgenis.data.elasticsearch.SearchService;
import org.molgenis.data.meta.WritableMetaDataService;
import org.molgenis.data.validation.EntityValidator;
import org.molgenis.framework.server.MolgenisSettings;
import org.molgenis.util.FileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.gdata.util.ServiceException;

@Service
public class GafListFileImporterService
{
	private static final Logger logger = Logger.getLogger(GafListFileImporterService.class);

	@Autowired
	private MolgenisSettings molgenisSettings;

	@Autowired
	private GafListValidator gafListValidator;

	@Autowired
	private DataService dataService;

	@Autowired
	private WritableMetaDataService writableMetaDataService;

	@Autowired
	private SearchService searchService;

	@Autowired
	private EntityValidator entityValidator;

	@Autowired
	FileStore fileStore;

	public GafListValidationReport validateGAFList(GafListValidationReport report, MultipartFile csvFile)
			throws IOException, ServiceException, Exception
	{
		report.uploadCsvFile(csvFile);
		GafListFileRepository repo = new GafListFileRepository(report.getTempFile(), null, null, null);
		gafListValidator.validate(report, repo, GafListValidator.COLUMNS);
		repo.close();
		return report;
	}

	public void importGAFList(GafListValidationReport report, String key_gaf_list_protocol_name) throws IOException,
			ServiceException
	{
		File tmpFile = fileStore.getFile(report.getTempFileName());

		if (!report.getValidRunIds().isEmpty())
		{
			final String gaflistEntityName = molgenisSettings.getProperty(GafListFileRepository.GAFLIST_ENTITYNAME);
			GafListFileRepository gafListFileRepositoryToImport = new GafListFileRepository(tmpFile, null, null, report);
			report.setDataSetName(gaflistEntityName);
			report.setDataSetIdentifier(gaflistEntityName);

			try
			{
				Writable writableRepository = dataService.getWritableRepository(gaflistEntityName);
				for (Entity entity : gafListFileRepositoryToImport)
				{
					writableRepository.add(entity);
				}
			}
			finally
			{
				try
				{
					gafListFileRepositoryToImport.close();
				}
				catch (IOException e)
				{
					logger.error(e);
				}
			}
		}
	}
}

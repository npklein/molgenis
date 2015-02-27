package org.molgenis.data.importer;

import java.io.File;
import java.util.List;

import org.molgenis.data.DatabaseAction;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.framework.db.EntitiesValidationReport;
import org.molgenis.framework.db.EntityImportReport;
import org.springframework.core.Ordered;

public interface ImportService extends Ordered
{
	EntityImportReport doImport(RepositoryCollection source, DatabaseAction databaseAction);

	EntitiesValidationReport validateImport(File file, RepositoryCollection source);

	boolean canImport(File file, RepositoryCollection source);

	List<DatabaseAction> getSupportedDatabaseActions();

	boolean getMustChangeEntityName();

	List<String> getSupportedFileExtensions();
}

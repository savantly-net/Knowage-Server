/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.
 *
 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.eng.spagobi.analiticalmodel.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import it.eng.spago.base.SourceBean;
import it.eng.spago.error.EMFUserError;
import it.eng.spago.security.IEngUserProfile;
import it.eng.spagobi.analiticalmodel.document.bo.BIObject;
import it.eng.spagobi.analiticalmodel.document.bo.ObjTemplate;
import it.eng.spagobi.analiticalmodel.document.bo.OutputParameter;
import it.eng.spagobi.analiticalmodel.document.bo.SubObject;
import it.eng.spagobi.analiticalmodel.document.dao.IBIObjectDAO;
import it.eng.spagobi.analiticalmodel.document.dao.IOutputParameterDAO;
import it.eng.spagobi.analiticalmodel.document.dao.OutputParameterDAOImpl;
import it.eng.spagobi.analiticalmodel.document.dao.SubObjectDAOHibImpl;
import it.eng.spagobi.analiticalmodel.document.utils.CockpitStatisticsTablesUtils;
import it.eng.spagobi.analiticalmodel.execution.service.v2.dto.MetadataDTO;
import it.eng.spagobi.behaviouralmodel.analyticaldriver.bo.BIObjectParameter;
import it.eng.spagobi.behaviouralmodel.analyticaldriver.bo.ObjParuse;
import it.eng.spagobi.behaviouralmodel.analyticaldriver.bo.ObjParview;
import it.eng.spagobi.behaviouralmodel.analyticaldriver.bo.Parameter;
import it.eng.spagobi.behaviouralmodel.analyticaldriver.dao.IBIObjectParameterDAO;
import it.eng.spagobi.behaviouralmodel.analyticaldriver.dao.IObjParuseDAO;
import it.eng.spagobi.behaviouralmodel.analyticaldriver.dao.IObjParviewDAO;
import it.eng.spagobi.behaviouralmodel.analyticaldriver.dao.IParameterDAO;
import it.eng.spagobi.commons.bo.UserProfile;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.commons.serializer.MetadataJSONSerializer;
import it.eng.spagobi.commons.utilities.DocumentUtilities;
import it.eng.spagobi.commons.utilities.HibernateSessionManager;
import it.eng.spagobi.tools.dataset.bo.DataSetParameterItem;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.objmetadata.bo.ObjMetacontent;
import it.eng.spagobi.tools.objmetadata.bo.ObjMetadata;
import it.eng.spagobi.tools.objmetadata.dao.IObjMetacontentDAO;
import it.eng.spagobi.tools.objmetadata.dao.IObjMetadataDAO;
import it.eng.spagobi.tools.objmetadata.dao.ObjMetadataDAOHibImpl;
import it.eng.spagobi.utilities.assertion.Assert;
import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;

/**
 * @author Andrea Gioia (andrea.gioia@eng.it)
 *
 *         This class considers all objects with the id property set and not equal to 0 as object that already exist in the database (see method
 *         isAnExistingDocument)
 *
 */
public class AnalyticalModelDocumentManagementAPI {

	private IBIObjectDAO documentDAO;
	private IBIObjectParameterDAO documentParameterDAO;
	private IObjParuseDAO documentParuseDAO;
	private IObjParviewDAO documentParviewDAO;
	private IObjMetacontentDAO documentMetadataPropertyDAO;
	private IObjMetadataDAO metadataPropertyDAO;
	private IObjMetacontentDAO metadataContentDAO;
	private SubObjectDAOHibImpl subObjectDAO;
	private IOutputParameterDAO outuputParameterDAO;

	// default for document parameters
	public static final Integer REQUIRED = 0;
	public static final Integer MODIFIABLE = 1;
	public static final Integer MULTIVALUE = 0;
	public static final Integer VISIBLE = 1;
	public static final String COPY_OF = "Copy of ";

	private static Logger logger = Logger.getLogger(AnalyticalModelDocumentManagementAPI.class);

	public AnalyticalModelDocumentManagementAPI(IEngUserProfile userProfile) {
		try {
			documentDAO = DAOFactory.getBIObjectDAO();
			documentDAO.setUserProfile(userProfile);

			documentParameterDAO = DAOFactory.getBIObjectParameterDAO();
			documentParameterDAO.setUserProfile(userProfile);

			documentParuseDAO = DAOFactory.getObjParuseDAO();
			documentParuseDAO.setUserProfile(userProfile);

			documentParviewDAO = DAOFactory.getObjParviewDAO();
			documentParviewDAO.setUserProfile(userProfile);

			documentMetadataPropertyDAO = DAOFactory.getObjMetacontentDAO();
			documentMetadataPropertyDAO.setUserProfile(userProfile);

			metadataPropertyDAO = new ObjMetadataDAOHibImpl();
			metadataPropertyDAO.setUserProfile(userProfile);

			metadataContentDAO = DAOFactory.getObjMetacontentDAO();
			metadataContentDAO.setUserProfile(userProfile);

			subObjectDAO = new SubObjectDAOHibImpl();
			subObjectDAO.setUserProfile(userProfile);

			outuputParameterDAO = new OutputParameterDAOImpl();
			outuputParameterDAO.setUserProfile(userProfile);

		} catch (Throwable t) {
			throw new SpagoBIRuntimeException("Impossible to instatiate BIObjectDAO", t);
		}
	}

	/**
	 * Utility method. Returns the document associated to the descriptor object.
	 *
	 * @param docDescriptor Could be the document itself (an object of type BIObject), its id (an object of type Integer) or its label (an object of type
	 *                      String)
	 *
	 * @return the document associated to the descriptor object if it exist, null otherwise.
	 */
	public BIObject getDocument(Object docDescriptor) {
		BIObject document;

		document = null;

		try {
			Assert.assertNotNull(docDescriptor, "Input parameter [docDescriptor] cannot be null");

			if (docDescriptor instanceof BIObject) {
				document = (BIObject) docDescriptor;
				if (!isAnExistingDocument(document))
					document = null;
			} else if (docDescriptor instanceof Integer) {
				try {
					document = documentDAO.loadBIObjectById((Integer) docDescriptor);
				} catch (Throwable t) {
					throw new SpagoBIRuntimeException("Impossible to load document whose id is equal to [" + docDescriptor + "]", t);
				}
			} else if (docDescriptor instanceof String) {
				try {
					document = documentDAO.loadBIObjectByLabel((String) docDescriptor);
				} catch (Throwable t) {
					throw new SpagoBIRuntimeException("Impossible to load document whose label is equal to [" + docDescriptor + "]", t);
				}
			} else {
				throw new SpagoBIRuntimeException("Unable to manage a document descriptor of type [" + docDescriptor.getClass().getName() + "]");
			}
		} catch (SpagoBIRuntimeException t) {
			throw t; // nothing to add just re-throw
		} catch (Throwable t) {
			throw new SpagoBIRuntimeException("An unsespected error occured while loading object [" + docDescriptor + "]", t);
		}

		return document;
	}

	/**
	 * Utility method. Returns the analytical drivers associated to the document object.
	 *
	 * Could be the label of the document
	 *
	 * @return the list with analitycal drivers associated.
	 */
	public List getDocumentParameters(Object docDescriptor) {
		logger.debug("IN");
		try {
			List<JSONObject> parametersList = new ArrayList<JSONObject>();

			BIObject document = getDocument(docDescriptor);
			if (document == null) {
				throw new RuntimeException("Impossible to get document [" + docDescriptor + "] from SpagoBI Server");
			}

			try {
				List objParams = document.getDrivers();
				for (Iterator iterator = objParams.iterator(); iterator.hasNext();) {
					JSONObject paramJSON = new JSONObject();
					BIObjectParameter param = (BIObjectParameter) iterator.next();
					paramJSON.put("id", param.getId());
					paramJSON.put("label", param.getLabel());
					paramJSON.put("url", param.getParameterUrlName());
					paramJSON.put("parType", param.getParameter().getType());
					parametersList.add(paramJSON);
				}
			} catch (Throwable t) {
				throw new SpagoBIRuntimeException("Impossible to parse parameters. ", t);
			} finally {
				logger.debug("OUT");
			}

			return parametersList;

		} catch (Throwable t) {
			throw new RuntimeException("An unexpected error occured while executing method", t);
		} finally {
			logger.debug("OUT");
		}
	}

	/**
	 * Utility method. Returns the analytical driver associated to the descriptor object.
	 *
	 * @param analyticalDriverDescriptor Could be the analytical driver itself (an object of type Parameter) or its label (an object of type String)
	 *
	 * @return the analytical driver associated to the descriptor object if it exist, null otherwise.
	 */
	public Parameter getAnalyticalDriver(Object analyticalDriverDescriptor) {
		Parameter analyticalDriver;

		try {
			analyticalDriver = null;
			if (analyticalDriverDescriptor instanceof Parameter) {
				analyticalDriver = (Parameter) analyticalDriverDescriptor;
			} else if (analyticalDriverDescriptor instanceof String) {
				try {
					String analyticalDriverLabel = (String) analyticalDriverDescriptor;
					analyticalDriver = DAOFactory.getParameterDAO().loadForDetailByParameterLabel(analyticalDriverLabel);
				} catch (Throwable t) {
					throw new SpagoBIRuntimeException("Analytical driver " + analyticalDriver + " cannot be loaded", t);
				}
			} else {
				throw new SpagoBIRuntimeException(
						"Unable to manage an analytical driver descriptor of type [" + analyticalDriverDescriptor.getClass().getName() + "]");
			}
		} catch (SpagoBIRuntimeException t) {
			throw t; // nothing to add just re-throw
		} catch (Throwable t) {
			throw new SpagoBIRuntimeException("An unsespected error occured while loading object [" + analyticalDriverDescriptor + "]", t);
		}

		return analyticalDriver;
	}

	/**
	 * Utility method. Returns the metadata property associated to the descriptor object.
	 *
	 * @param matadataPropertyDescriptor Could be the metadata property's id (an object of type Integer) or its label (an object of type String)
	 *
	 * @return the analytical driver associated to the descriptor object if it exist, null otherwise.
	 */
	public ObjMetadata getMetadataProperty(Object matadataPropertyDescriptor) {

		ObjMetadata metadataProperty;

		logger.debug("IN");

		metadataProperty = null;
		try {
			if (matadataPropertyDescriptor instanceof Integer) {
				Integer id = (Integer) matadataPropertyDescriptor;
				metadataProperty = metadataPropertyDAO.loadObjMetaDataByID(id);
			} else if (matadataPropertyDescriptor instanceof String) {
				String label = (String) matadataPropertyDescriptor;
				metadataProperty = metadataPropertyDAO.loadObjMetadataByLabel(label);
			} else {
				throw new SpagoBIRuntimeException("Unable to manage a metadata descriptor of type [" + matadataPropertyDescriptor.getClass().getName() + "]");
			}
		} catch (SpagoBIRuntimeException t) {
			throw t; // nothing to add just re-throw
		} catch (Throwable t) {
			throw new SpagoBIRuntimeException("An unsespected error occured while loading metadata [" + matadataPropertyDescriptor + "]", t);
		}

		return metadataProperty;
	}

	/**
	 *
	 * @param document The document
	 * @return return true if the doocument's id property is set and not equal to 0. This method do not perform a real check on the database.
	 */
	public boolean isAnExistingDocument(BIObject document) {
		Integer documentId;

		Assert.assertNotNull(document, "Input parameter [document] cannot be null");

		documentId = document.getId();
		return (documentId != null && documentId.intValue() != 0);
	}

	/**
	 *
	 * @param document The document to save (insert or modify)
	 * @param template The new template of the document
	 *
	 * @return true if the save operation perform an overwrite ( = modify an existing document ), false otherwise ( = insert a new document )
	 *
	 */
	public boolean saveDocument(BIObject document, ObjTemplate template) {

		Boolean overwrite;

		Assert.assertNotNull(document, "Input parameter [document] cannot be null");

		if (isAnExistingDocument(document)) {
			try {
				documentDAO.modifyBIObject(document, template);
			} catch (Throwable t) {
				throw new SpagoBIRuntimeException("Impossible to update object [" + document.getLabel() + "]", t);
			}
			overwrite = true;
			logger.debug("Document [" + document.getLabel() + "] succesfully updated");
		} else {
			try {
				Integer id = documentDAO.insertBIObject(document, template);
				document.setId(id);
			} catch (Throwable t) {
				throw new SpagoBIRuntimeException("Impossible to insert object [" + document.getLabel() + "]", t);
			}

			overwrite = false;
			logger.debug("Document with [" + document.getLabel() + "] succesfully inserted with id [" + document.getId() + "]");
		}

		if (!DocumentUtilities.getValidLicenses().isEmpty())
			CockpitStatisticsTablesUtils.updateCockpitWidgetsTable(document, HibernateSessionManager.getCurrentSession());

		return overwrite;
	}

	/**
	 * Custom method specially created for and used only by the SUNBURST chart document.
	 *
	 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
	 */
	public boolean saveDocument(BIObject document, ObjTemplate template, List categories) {

		Boolean overwrite;

		Assert.assertNotNull(document, "Input parameter [document] cannot be null");

		if (isAnExistingDocument(document)) {
			try {
				// Provide information for the specific output categories parameters. (danristo)
				documentDAO.modifyBIObject(document, template, categories);
			} catch (Throwable t) {
				throw new SpagoBIRuntimeException("Impossible to update object [" + document.getLabel() + "]", t);
			}
			overwrite = true;
			logger.debug("Document [" + document.getLabel() + "] succesfully updated");
		} else {
			try {
				Integer id = documentDAO.insertBIObject(document, template);
				document.setId(id);
			} catch (Throwable t) {
				throw new SpagoBIRuntimeException("Impossible to insert object [" + document.getLabel() + "]", t);
			}

			overwrite = false;
			logger.debug("Document with [" + document.getLabel() + "] succesfully inserted with id [" + document.getId() + "]");
		}

		if (!DocumentUtilities.getValidLicenses().isEmpty())
			CockpitStatisticsTablesUtils.updateCockpitWidgetsTable(document, HibernateSessionManager.getCurrentSession());

		return overwrite;
	}

	/**
	 * Custom method specially created for some chart types (such as WORDCLOUD, CHORD and PARALLEL) that need some default (generic) output parameters to be
	 * removed from the list of final output parameters for the document of that chart type. For example, the WORDCLOUD chart type does not need a GROUPING_NAME
	 * and GROUPING_VALUE output parameters, so these two will be removed from the predefined (standard) list of output parameters (it will have only
	 * SERIE_NAME, SERIE_VALUE, CATEGORY_NAME, CATEGORY_VALUE parameters). According to this input information (a 'specificChartType' value), further methods
	 * will manage this list accordingly.
	 *
	 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
	 */
	public boolean saveDocument(BIObject document, ObjTemplate template, String specificChartType) {

		Boolean overwrite;

		Assert.assertNotNull(document, "Input parameter [document] cannot be null");

		if (isAnExistingDocument(document)) {
			try {
				// Provide information for the specific output categories parameters. (danristo)
				documentDAO.modifyBIObject(document, template, specificChartType);
			} catch (Throwable t) {
				throw new SpagoBIRuntimeException("Impossible to update object [" + document.getLabel() + "]", t);
			}
			overwrite = true;
			logger.debug("Document [" + document.getLabel() + "] succesfully updated");
		} else {
			try {
				Integer id = documentDAO.insertBIObject(document, template);
				document.setId(id);
			} catch (Throwable t) {
				throw new SpagoBIRuntimeException("Impossible to insert object [" + document.getLabel() + "]", t);
			}

			overwrite = false;
			logger.debug("Document with [" + document.getLabel() + "] succesfully inserted with id [" + document.getId() + "]");
		}

		if (!DocumentUtilities.getValidLicenses().isEmpty())
			CockpitStatisticsTablesUtils.updateCockpitWidgetsTable(document, HibernateSessionManager.getCurrentSession());

		return overwrite;
	}

	/**
	 *
	 * @param document The document whose parameters have to be saved (insert or modify)
	 */
	public void saveDocumentParameters(BIObject document) {
		IParameterDAO parameterDAO;
		try {
			parameterDAO = DAOFactory.getParameterDAO();

			// It inserts all the parameters of the object
			for (BIObjectParameter parameter : document.getDrivers()) {
				parameter.setParameter(parameterDAO.loadForDetailByParameterID(parameter.getParID()));

				parameter.setBiObjectID(document.getId());
				DAOFactory.getBIObjectParameterDAO().insertBIObjectParameter(parameter);
			}
		} catch (Exception e) {
			logger.error("Error while inserting parameters", e);
			throw new SpagoBIRuntimeException("Error while inserting parameters", e);
		}
	}

	/**
	 * Clone a document
	 *
	 * @param document   The document id of the document to clone
	 * @param documentId The document id of the document to clone
	 */
	public BIObject cloneDocument(Integer documentId) {
		logger.debug("IN");

		Assert.assertNotNull(documentId, "Input parameter [documentId] cannot be null");
		BIObject clonedDocument = null;
		if (documentId != null) {
			BIObject document = getDocument(documentId);
			logger.debug("Cloning the document");
			clonedDocument = document.clone();

			logger.debug("Cloning the template");
			ObjTemplate clonedTemplate = document.getActiveTemplate();
			if (clonedTemplate != null) {
				clonedTemplate = clonedTemplate.clone();
			}
			try {
				logger.debug("Udapting label and name of the clone");
				updateClonedDocumentProperties(clonedDocument);

				// document
				logger.debug("Saving the clone of the document");
				Integer clonedDocumentId = documentDAO.insertBIObject(clonedDocument, clonedTemplate);
				clonedDocument.setId(clonedDocumentId);

				// parameters
				logger.debug("Coping parameters");
				copyParameters(document, clonedDocument);
				//
				// //subobjects
				// copySubobjects(document, clonedDocument);

				// output Parameters
				copyOutputParameters(document, clonedDocument);
				// metadata
				logger.debug("Coping metadata");
				copyMetadata(document, clonedDocument);
			} catch (Throwable t) {
				logger.error("Impossible to update object [" + document.getLabel() + "]", t);
				throw new SpagoBIRuntimeException("Impossible to update object [" + document.getLabel() + "]", t);
			}

			logger.debug("Document [" + document.getLabel() + "] succesfully cloned");

		}

		return clonedDocument;
	}

	private void updateClonedDocumentProperties(BIObject document) {
		int version = 0;
		while (true) {
			String newLabel = buildCopiedString(document.getLabel(), version);
			BIObject doc = null;
			try {
				doc = documentDAO.loadBIObjectByLabel(newLabel);
			} catch (EMFUserError e) {
				throw new SpagoBIRuntimeException("Impossible to look if the docuemnt with label [" + newLabel + "] already exists", e);
			}
			if (doc == null) {
				break;
			}
			version++;
		}

		document.setLabel(buildCopiedString(document.getLabel(), version));
		document.setName(buildCopiedString(document.getName(), version));
		String userId = String.valueOf(((UserProfile) this.documentDAO.getUserProfile()).getUserId());
		document.setCreationUser(userId);
	}

	private String buildCopiedString(String toCopy, int newVersion) {
		String copied = toCopy;
		if (newVersion > 0) {
			int openBrackets = toCopy.lastIndexOf("(");
			if (openBrackets == -1) {
				openBrackets = toCopy.length();
			}
			copied = toCopy.substring(0, openBrackets) + "(" + newVersion + ")";
		}

		if (copied.indexOf(COPY_OF) < 0) {
			copied = COPY_OF + copied;
		}

		return copied;
	}

	// private String buildCopiedString(String toCopy, int newVersion){
	// String copied = "";
	// if(toCopy.indexOf(COPY_OF)==0){//already a copy
	// try {
	// int copiedVersion = 1;
	// int openBrackets = 0;
	// int closeBrackets = toCopy.lastIndexOf(")");
	// if(newVersion==0){
	// if(closeBrackets == toCopy.length()-1){
	// openBrackets = toCopy.lastIndexOf("(");
	// if(openBrackets>=0){
	// String version = toCopy.substring(openBrackets+1,closeBrackets);
	// Integer versionNumber = new Integer(version);
	// copiedVersion = versionNumber+1;
	// }
	// }
	// }else{
	// copiedVersion = newVersion;
	// }
	// copied = toCopy.substring(0,openBrackets)+copiedVersion+")";
	// } catch (Exception e) {
	// copied = COPY_OF+toCopy;
	// }
	//
	// }else{
	// copied = COPY_OF+toCopy;
	// }
	// return copied;
	// }
	//

	/**
	 *
	 * @param documentDescriptor The descriptor of the target document
	 * @param subObjectId        The id of the target subobject (optional). If it is nos specified the metadata properties will be applied to the main object
	 * @param metadata           The metadata properties to add. They are encoded as an array of object like the following one <code>
	 * {
	 * 	meta_id: NUMBER
	 * , meta_name: STRING
	 * , meta_content: STRING
	 * }
	 * </code>                at least one between attributes meta_id and meta_name must be set.
	 *
	 *                           TODO use this method to refactor class SaveMetadataAction
	 *
	 */
	public void saveDocumentMetadataProperties(Object documentDescriptor, Integer subObjectId, List<MetadataDTO> metadata) {

		logger.debug("IN");

		try {
			Assert.assertNotNull(documentDescriptor, "Input parameter [documentDescriptor] cannot be null");
			Assert.assertNotNull(metadata, "Input parameter [metadata] cannot be null");

			BIObject document = getDocument(documentDescriptor);
			if (document == null) {
				throw new SpagoBIRuntimeException("Impossible to resolve document [" + documentDescriptor + "]");
			}

			for (MetadataDTO metadataDTO : metadata) {

				Integer metadataPropertyId = null;

				if (metadataDTO.getMetaId() != null) {
					metadataPropertyId = metadataDTO.getMetaId();
				}
				String metadataPropertyName = metadataDTO.getName();
				if (metadataPropertyId == null && metadataPropertyName == null) {
					throw new SpagoBIRuntimeException(
							"Attributes [" + MetadataJSONSerializer.METADATA_ID + "] and [" + MetadataJSONSerializer.NAME + "] cannot be both null");
				}

				if (metadataPropertyId == null) {
					ObjMetadata metadataProperty = getMetadataProperty(metadataPropertyName);
					if (metadataProperty != null) {
						metadataPropertyId = metadataProperty.getObjMetaId();
					}

					if (metadataPropertyId == null) {
						logger.warn("Impossible to resolve metadata property [" + metadataPropertyName + "]");
						continue;
					}
				}

				String documentMetadataPropertyValue = metadataDTO.getText();
				if (documentMetadataPropertyValue == null) {
					throw new SpagoBIRuntimeException(
							"Attributes [" + MetadataJSONSerializer.TEXT + "] of metadata property cannot [" + metadataPropertyId + "] be null");
				}

				ObjMetacontent documentMatadataProperty = documentMetadataPropertyDAO.loadObjMetacontent(metadataPropertyId, document.getId(), subObjectId); // TODO
																																								// manage
																																								// subobjects

				if (documentMatadataProperty == null) {
					logger.debug("ObjMetacontent for metadata id = " + metadataPropertyId + ", biobject id = " + document.getId() + ", subobject id = "
							+ subObjectId + " was not found, creating a new one...");
					documentMatadataProperty = new ObjMetacontent();
					documentMatadataProperty.setObjmetaId(metadataPropertyId);
					documentMatadataProperty.setBiobjId(document.getId());
					documentMatadataProperty.setSubobjId(subObjectId);
					documentMatadataProperty.setContent(documentMetadataPropertyValue.getBytes("UTF-8"));
					documentMatadataProperty.setCreationDate(new Date());
					documentMatadataProperty.setLastChangeDate(new Date());

					documentMetadataPropertyDAO.insertObjMetacontent(documentMatadataProperty);
				} else {
					logger.debug("ObjMetacontent for metadata id = " + metadataPropertyId + ", biobject id = " + document.getId() + ", subobject id = "
							+ subObjectId + " was found, it will be modified...");
					documentMatadataProperty.setContent(documentMetadataPropertyValue.getBytes("UTF-8"));
					documentMatadataProperty.setLastChangeDate(new Date());

					documentMetadataPropertyDAO.modifyObjMetacontent(documentMatadataProperty);
				}

			}
		} catch (Throwable e) {
			throw new SpagoBIRuntimeException("Exception occurred while saving metadata", e);
		} finally {
			logger.debug("OUT");
		}
	}

	/**
	 * Copy all the parameters associated with sourceDocument to destinationDocument
	 *
	 * @param sourceDocument      can be an object of type BIObject or an Integer representing the id of the source document
	 * @param destinationDocument can be an object of type BIObject or an Integer representing the id of the destination document
	 */
	public void copyParameters(Object sourceDocument, Object destinationDocument) {
		copyParameters(getDocument(sourceDocument), getDocument(destinationDocument));
	}

	private void copyParameters(BIObject sourceDocument, BIObject destinationDocument) {

		String sourceDocumentLabel = null;
		String destinationDocumentLabel = null;

		try {
			Assert.assertNotNull(sourceDocument, "Input parameter [sourceDocument] cannot be null");
			Assert.assertNotNull(destinationDocument, "Input parameter [destinationDocument] cannot be null");

			sourceDocumentLabel = sourceDocument.getLabel();
			destinationDocumentLabel = destinationDocument.getLabel();

			List<BIObjectParameter> parameters = sourceDocument.getDrivers();

			// order parameters on priority value to mantein the same order of the original
			Comparator<BIObjectParameter> comparator = new Comparator<BIObjectParameter>() {
				@Override
				public int compare(BIObjectParameter c1, BIObjectParameter c2) {
					return c1.getPriority().compareTo(c2.getPriority());
				}
			};
			Collections.sort(parameters, comparator); // use the comparator as much as u want

			if (parameters != null && !parameters.isEmpty()) {
				// save the source parameter id for get its correlations and visibility roles
				for (BIObjectParameter parameter : parameters) {
					Integer sourceObjParId = parameter.getId();
					parameter.setBiObjectID(destinationDocument.getId());
					parameter.setId(null);
					try {
						documentParameterDAO.insertBIObjectParameter(parameter);
						// insert all paruse for the parameter (correlation)
						List<ObjParuse> paruses = documentParuseDAO.loadObjParuses(sourceObjParId);
						for (ObjParuse paruse : paruses) {
							// get the new objParId (just created)
							BIObjectParameter newParUse = documentParameterDAO.loadBiObjParameterByObjIdAndLabel(destinationDocument.getId(),
									parameter.getLabel());
							paruse.setParId(newParUse.getId());
							// get the new fatherObjParId (through the original father reference )
							BIObjectParameter sourceFatherParUse = documentParameterDAO.loadBiObjParameterById(paruse.getParFatherId());
							String sourceFatherParUseLabel = sourceFatherParUse.getLabel();
							BIObjectParameter newParUseFather = documentParameterDAO.loadBiObjParameterByObjIdAndLabel(destinationDocument.getId(),
									sourceFatherParUseLabel);
							paruse.setParFatherId(newParUseFather.getId());

							documentParuseDAO.insertObjParuse(paruse);
						}
						// insert all parview for the parameter (visibility)
						List<ObjParview> parviews = documentParviewDAO.loadObjParviews(sourceObjParId);
						for (ObjParview parview : parviews) {
							// get the new objParId (just created)
							BIObjectParameter newParView = documentParameterDAO.loadBiObjParameterByObjIdAndLabel(destinationDocument.getId(),
									parameter.getLabel());
							parview.setParId(newParView.getId());
							// get the new fatherObjParId (through the original father reference )
							BIObjectParameter sourceFatherParView = documentParameterDAO.loadBiObjParameterById(parview.getParFatherId());
							String sourceFatherParViewLabel = sourceFatherParView.getLabel();
							BIObjectParameter newParUseFather = documentParameterDAO.loadBiObjParameterByObjIdAndLabel(destinationDocument.getId(),
									sourceFatherParViewLabel);
							parview.setParFatherId(newParUseFather.getId());
							parview.setParFatherUrlName(newParUseFather.getParameterUrlName());
							documentParviewDAO.insertObjParview(parview);
						}
					} catch (Throwable t) {
						throw new SpagoBIRuntimeException("Impossible to copy parameter [" + parameter.getLabel() + "] from document [" + sourceDocumentLabel
								+ "] to document [" + destinationDocumentLabel + "]", t);
					}
				}
			} else {
				logger.warn("Document [" + sourceDocumentLabel + "] have no parameters");
			}
		} catch (Throwable t) {
			throw new SpagoBIRuntimeException("An unexpected error occured while copying parameters from document [" + sourceDocumentLabel + "] to document ["
					+ destinationDocumentLabel + "]", t);
		}
	}

	/**
	 * Copy all the subobjects associated with sourceDocument to destinationDocument
	 *
	 * @param sourceDocument      can be an object of type BIObject or an Integer representing the id of the source document
	 * @param destinationDocument can be an object of type BIObject or an Integer representing the id of the destination document
	 */

	public void copySubobjects(Object sourceDocument, Object destinationDocument) {
		copySubobjects(getDocument(sourceDocument), getDocument(destinationDocument));
	}

	private void copySubobjects(BIObject sourceDocument, BIObject destinationDocument) {

		String sourceDocumentLabel = null;
		String destinationDocumentLabel = null;

		try {

			Assert.assertNotNull(sourceDocument, "Input parameter [sourceDocument] cannot be null");
			Assert.assertNotNull(destinationDocument, "Input parameter [destinationDocument] cannot be null");

			sourceDocumentLabel = sourceDocument.getLabel();
			destinationDocumentLabel = destinationDocument.getLabel();

			List<SubObject> subobjects = subObjectDAO.getSubObjects(sourceDocument.getId());
			if (subobjects != null) {
				for (int i = 0; i < subobjects.size(); i++) {
					subObjectDAO.saveSubObject(destinationDocument.getId(), subobjects.get(i));
				}
			}

		} catch (EMFUserError e) {
			throw new SpagoBIRuntimeException("An unexpected error occured while copying subobjects from document [" + sourceDocumentLabel + "] to document ["
					+ destinationDocumentLabel + "]", e);
		}
	}

	/**
	 * Copy the output parameters
	 */
	private void copyOutputParameters(BIObject sourceDocument, BIObject destinationDocument) {
		logger.debug("IN");

		List<OutputParameter> outputParameters = sourceDocument.getOutputParameters();

		for (Iterator iterator = outputParameters.iterator(); iterator.hasNext();) {
			OutputParameter outputParameter = (OutputParameter) iterator.next();
			OutputParameter newOutPar = new OutputParameter();
			newOutPar.setBiObjectId(destinationDocument.getId());
			newOutPar.setFormatCode(outputParameter.getFormatCode());
			newOutPar.setFormatValue(outputParameter.getFormatValue());
			newOutPar.setIsUserDefined(outputParameter.getIsUserDefined());
			newOutPar.setName(outputParameter.getName());
			newOutPar.setType(outputParameter.getType());
			outuputParameterDAO.saveParameter(newOutPar);
		}
		logger.debug("OUT");
	}

	/**
	 * Copy all the metadata associated with sourceDocument to destinationDocument. It does not copy the metdata of subjocts of the source document
	 *
	 * @param sourceDocument      can be an object of type BIObject or an Integer representing the id of the source document
	 * @param destinationDocument can be an object of type BIObject or an Integer representing the id of the destination document
	 */

	public void copyMetadata(Object sourceDocument, Object destinationDocument) {
		copyMetadata(getDocument(sourceDocument), getDocument(destinationDocument));
	}

	private void copyMetadata(BIObject sourceDocument, BIObject destinationDocument) {

		String sourceDocumentLabel = null;
		String destinationDocumentLabel = null;

		try {

			Assert.assertNotNull(sourceDocument, "Input parameter [sourceDocument] cannot be null");
			Assert.assertNotNull(destinationDocument, "Input parameter [destinationDocument] cannot be null");

			sourceDocumentLabel = sourceDocument.getLabel();
			destinationDocumentLabel = destinationDocument.getLabel();

			List<ObjMetacontent> metadata = metadataContentDAO.loadObjOrSubObjMetacontents(sourceDocument.getId(), null);
			if (metadata != null) {
				for (int i = 0; i < metadata.size(); i++) {
					ObjMetacontent md = metadata.get(i);
					if (md.getSubobjId() == null) {// save only the metadata of the document.. not metadata of subojects
						md.setBiobjId(destinationDocument.getId());
						metadataContentDAO.insertObjMetacontent(md);
					}
				}
			}

		} catch (EMFUserError e) {
			throw new SpagoBIRuntimeException("An unexpected error occured while copying metadata from document [" + sourceDocumentLabel + "] to document ["
					+ destinationDocumentLabel + "]", e);
		}
	}

	/**
	 * This method add a parameter to the document for each parameter associated with the dataset. The added parameters will point to analytical drivers whose
	 * label match with the corresponding dataset parameter's name. If for one dataset parameter does not exist an analytical driver whose label match with the
	 * name of the parameter an exception will be thrown
	 *
	 * @param dataset  the datset
	 * @param document the document
	 */
	public void propagateDatasetParameters(IDataSet dataset, BIObject document) {

		List<DataSetParameterItem> datasetParameterItems = getDatasetParameters(dataset);

		int priority = 0;
		for (DataSetParameterItem datasetParameters : datasetParameterItems) {
			addParameter(document, datasetParameters.getName(), priority++);
		}
	}

	/**
	 * Add the analytical driver associated to the analyticalDriverDescriptor to the document associated to the documentDescriptor. The document must be already
	 * present on the database. The name and the url of the added parameters are both equal to the analytical driver label. This method do not check if the
	 * document already have a parameter with this name.
	 *
	 * @param documentDescriptor         can be the document itself(BIObject), the document id(Integer) or the document label(String)
	 * @param analyticalDriverDescriptor can be the analytical driver(Parameter) itself or its label (String)
	 * @param priority
	 */
	public void addParameter(BIObject documentDescriptor, Object analyticalDriverDescriptor, int priority) {
		BIObject document;
		BIObjectParameter documentParameter;
		Parameter analyticalDriver;

		try {
			Assert.assertNotNull(documentDescriptor, "Input parameter [documentDescriptor] cannot be null");
			Assert.assertNotNull(analyticalDriverDescriptor, "Input parameter [analyticalDriverDescriptor] cannot be null");

			document = getDocument(documentDescriptor);
			Assert.assertNotNull(document, "Analytical driver with " + documentDescriptor + " does not exist");

			analyticalDriver = getAnalyticalDriver(analyticalDriverDescriptor);
			if (analyticalDriver == null) {
				throw new SpagoBIRuntimeException("Analytical driver " + analyticalDriverDescriptor + " does not exist");
			}

			documentParameter = new BIObjectParameter();
			documentParameter.setBiObjectID(document.getId());
			documentParameter.setParID(analyticalDriver.getId());
			documentParameter.setParameter(analyticalDriver);
			documentParameter.setParameterUrlName(analyticalDriver.getLabel());
			documentParameter.setLabel(analyticalDriver.getName());
			documentParameter.setRequired(REQUIRED);
			documentParameter.setMultivalue(MULTIVALUE);
			documentParameter.setModifiable(MODIFIABLE);
			documentParameter.setVisible(VISIBLE);
			documentParameter.setPriority(priority);

			try {
				documentParameterDAO.insertBIObjectParameter(documentParameter);
			} catch (Throwable t) {
				throw new SpagoBIRuntimeException(
						"Impossible to save parameter whose label is equal to [" + analyticalDriverDescriptor + "] to document [" + document + "]", t);
			}

			if (document.getDrivers() == null) {
				document.setDrivers(new ArrayList<BIObjectParameter>());
			}
			document.getDrivers().add(documentParameter);

		} catch (SpagoBIRuntimeException t) {
			throw t; // nothing to add just re-throw
		} catch (Throwable t) {
			throw new SpagoBIRuntimeException(
					"An unsespected error occured while adding parameter [" + analyticalDriverDescriptor + "] to document [" + documentDescriptor + "]", t);
		}
	}

	// TODO move this in DataSetManagementAPI
	private List<DataSetParameterItem> getDatasetParameters(IDataSet dataset) {

		// GuiDataSetDetail datasetDetail;
		String parametersRawData;
		List<DataSetParameterItem> datasetParameters;

		logger.debug("IN");

		datasetParameters = new ArrayList<DataSetParameterItem>();

		try {
			// datasetDetail = dataset.getActiveDetail();
			parametersRawData = dataset.getParameters();
			if (parametersRawData == null) {
				return new ArrayList<DataSetParameterItem>();
			}
			parametersRawData = parametersRawData.trim();

			SourceBean parametersSourceBean;
			try {
				parametersSourceBean = SourceBean.fromXMLString(parametersRawData);
			} catch (Throwable t) {
				throw new SpagoBIRuntimeException("Parameters' rowdata are not encoded in a valid XML format [" + parametersRawData + "].", t);
			}

			List<SourceBean> rows = parametersSourceBean.getAttributeAsList("ROWS.ROW");
			for (SourceBean row : rows) {
				String name = (String) row.getAttribute("NAME");
				String type = (String) row.getAttribute("TYPE");

				DataSetParameterItem datasetParameter = new DataSetParameterItem();
				datasetParameter.setName(name);
				datasetParameter.setType(type);

				datasetParameters.add(datasetParameter);
			}

		} catch (SpagoBIRuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw new SpagoBIRuntimeException("An unexpected error occured while loading dataset parameters", t);
		} finally {
			logger.debug("OUT");
		}

		return datasetParameters;
	}

}

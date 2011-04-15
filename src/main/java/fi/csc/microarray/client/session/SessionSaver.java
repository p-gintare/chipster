package fi.csc.microarray.client.session;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import fi.csc.microarray.client.NameID;
import fi.csc.microarray.client.Session;
import fi.csc.microarray.client.operation.OperationRecord;
import fi.csc.microarray.client.operation.OperationRecord.InputRecord;
import fi.csc.microarray.client.operation.OperationRecord.ParameterRecord;
import fi.csc.microarray.client.session.schema.DataType;
import fi.csc.microarray.client.session.schema.FolderType;
import fi.csc.microarray.client.session.schema.InputType;
import fi.csc.microarray.client.session.schema.LinkType;
import fi.csc.microarray.client.session.schema.NameType;
import fi.csc.microarray.client.session.schema.ObjectFactory;
import fi.csc.microarray.client.session.schema.OperationType;
import fi.csc.microarray.client.session.schema.ParameterType;
import fi.csc.microarray.client.session.schema.SessionType;
import fi.csc.microarray.databeans.DataBean;
import fi.csc.microarray.databeans.DataFolder;
import fi.csc.microarray.databeans.DataItem;
import fi.csc.microarray.databeans.DataManager;
import fi.csc.microarray.databeans.DataBean.Link;
import fi.csc.microarray.databeans.DataBean.StorageMethod;
import fi.csc.microarray.databeans.handlers.ZipDataBeanHandler;
import fi.csc.microarray.util.IOUtils;
import fi.csc.microarray.util.SwingTools;

/**
 * @author hupponen
 *
 */
public class SessionSaver {

	private static final Logger logger = Logger.getLogger(SessionSaver.class);

	
	private final int DATA_BLOCK_SIZE = 2048;
	
	private File sessionFile;
	private HashMap<DataBean, URL> newURLs = new HashMap<DataBean, URL>();

	private int entryCounter = 0;

	private int itemIdCounter = 0;
	private HashMap<String, DataItem> itemIdMap = new HashMap<String, DataItem>();
	private HashMap<DataItem, String> reversedItemIdMap = new HashMap<DataItem, String>();

	private int operationIdCounter = 0;
	private HashMap<String, OperationRecord> operationRecordIdMap = new HashMap<String, OperationRecord>();
	private HashMap<OperationRecord, String> reversedOperationRecordIdMap = new HashMap<OperationRecord, String>();
	private HashMap<String, OperationType> operationRecordTypeMap = new HashMap<String, OperationType>();
	
	
	private DataManager dataManager;

	private ObjectFactory factory;
	private SessionType sessionType;

	
	public SessionSaver(File sessionFile) {
		this.sessionFile = sessionFile;
		this.dataManager = Session.getSession().getDataManager();

	}
	
	public void saveSession() throws IOException, JAXBException, SAXException {

		// xml schema object factory and xml root
		this.factory = new ObjectFactory();
		this.sessionType = factory.createSessionType();


		// save session version
		sessionType.setFormatVersion(ClientSession.SESSION_VERSION);

		// generate all ids
		generateIdsRecursively(dataManager.getRootFolder());

		// gather meta data and save actual data to the zip file
		saveMetadataRecursively(dataManager.getRootFolder());
		
		// validate and meta data
		Marshaller marshaller = ClientSession.getJAXBContext().createMarshaller();
		marshaller.setSchema(ClientSession.getSchema());
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.marshal(factory.createSession(sessionType), System.out);
		
		
		
		
		
		
		
		// figure out the target file
		boolean replaceOldSession = sessionFile.exists();
		File newSessionFile;
		File backupFile = null;
		if (replaceOldSession) {
			newSessionFile = new File(sessionFile.getAbsolutePath() + "-save-temp.zip");
			backupFile = new File(sessionFile.getAbsolutePath() + "-save-backup.zip");
		} else {
			newSessionFile = sessionFile;
		}

		
		ZipOutputStream zipOutputStream = null;
		boolean createdSuccessfully = false;
		try {
			
			zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(newSessionFile)));
			zipOutputStream.setLevel(1); // quite slow with bigger values														

		
			// save meta data
			ZipEntry sessionDataZipEntry = new ZipEntry(ClientSession.SESSION_DATA_FILENAME);
			zipOutputStream.putNextEntry(sessionDataZipEntry);
			marshaller.marshal(factory.createSession(sessionType), zipOutputStream);

			
			// save data bean contents
			writeDataBeanContentsToZipFile(zipOutputStream);
			
			zipOutputStream.closeEntry() ;							

			// FIXME finally?
			zipOutputStream.finish();
			zipOutputStream.close();

			
			
			// rename new session if replacing existing
			if (replaceOldSession) {
				
				// original to backup
				if (!sessionFile.renameTo(backupFile)) {
					throw new IOException("Creating backup " + sessionFile + " -> " + backupFile + " failed.");
				}
					
				// new to original
				if (newSessionFile.renameTo(sessionFile)) {
					createdSuccessfully = true;

					// remove backup
					backupFile.delete();
				} else {
					// try to move backup back to original
					// TODO remove new session file?
					if (backupFile.renameTo(sessionFile)) {
						throw new IOException("Moving new " + newSessionFile + " -> " + sessionFile + " failed, " +
								"restored original session file.");
					} else {
						throw new IOException("Moving new " + newSessionFile + " -> " + sessionFile + " failed, " +
						"also restoring original file failed, backup of original is " + backupFile);
					}
				}
			} 
			
			// session file is now saved, update the urls and handlers in the client
			for (DataBean bean: newURLs.keySet()) {

				// set new url and handler and type
				bean.setStorageMethod(StorageMethod.LOCAL_SESSION);
				bean.setContentUrl(newURLs.get(bean));
				bean.setHandler(new ZipDataBeanHandler());
			}

			createdSuccessfully = true;
			
		} catch (RuntimeException e) {
			// createdSuccesfully is false, so file will be deleted in finally block
			throw e;
			
		} catch (IOException e) {
			// createdSuccesfully is false, so file will be deleted in finally block
			throw e;

		} catch (JAXBException e) {
			// createdSuccesfully is false, so file will be deleted in finally block
			throw e;
		} finally {
			IOUtils.closeIfPossible(zipOutputStream); // called twice for normal execution, not a problem
			if (!replaceOldSession && !createdSuccessfully) {
				newSessionFile.delete(); // do not leave bad session files hanging around
			}
		}
	}

	private int generateIdsRecursively(DataFolder folder) throws IOException {
		
		int dataCount = 0;
		
		generateId(folder);
		
		for (DataItem data : folder.getChildren()) {
			if (data instanceof DataFolder) {
				int recDataCount = generateIdsRecursively((DataFolder)data);
				dataCount += recDataCount;
				
			} else {
				generateId((DataBean)data);
				dataCount++;
			}
		}

		return dataCount;
	}

	private void generateId(DataItem data) {
		String id = String.valueOf(itemIdCounter);
		itemIdCounter++;
		itemIdMap.put(id, data);
		reversedItemIdMap.put(data, id);
	}
	
	
	private String generateId(OperationRecord operationRecord) {
		String id = String.valueOf(operationIdCounter);
		operationIdCounter++;
		operationRecordIdMap.put(id, operationRecord);
		reversedOperationRecordIdMap.put(operationRecord, id);
		return id.toString();
	}

	private void saveMetadataRecursively(DataFolder folder) throws IOException {
		
		String folderId = reversedItemIdMap.get(folder);
		saveDataFolderMetadata(folder, folderId);
		
		for (DataItem data : folder.getChildren()) {
			if (data instanceof DataFolder) {
				saveMetadataRecursively((DataFolder)data);
				
			} else {
				DataBean bean = (DataBean)data;

				// create the new URL TODO check the ref
				String entryName = getNewZipEntryName();
				URL newURL = new URL(sessionFile.toURI().toURL(), "#" + entryName);

				// store the new URL temporarily
				newURLs.put(bean, newURL);

				// store metadata
				saveDataBeanMetadata(bean, newURL, folderId);

			}
		}
	}


	private void saveDataFolderMetadata(DataFolder folder, String folderId) {
		FolderType folderType = factory.createFolderType();
		
		// name
		folderType.setId(folderId);
		folderType.setName(folder.getName());
		
		// parent
		if (folder.getParent() != null) {
			String parentId = reversedItemIdMap.get(folder.getParent());
			if (parentId != null) {
				folderType.setParent(parentId);
			} else {
				logger.warn("unknown parent");
			}
		}
		
		// children
		if (folder.getChildCount() > 0) {
			for (DataItem child : folder.getChildren()) {
				String childId = reversedItemIdMap.get(child);
				if (childId != null) { 
					folderType.getChild().add(childId);
				} else {
					logger.warn("unknown child: " + child.getName());
				}
			}
		}
		
		sessionType.getFolder().add(folderType);
	}	
	
	
	private void saveDataBeanMetadata(DataBean bean, URL newURL, String folderId) {
		String beanId = reversedItemIdMap.get(bean);
		DataType dataType = factory.createDataType();
	
		// name and id
		dataType.setId(beanId);
		dataType.setName(bean.getName());

		// parent
		if (bean.getParent() != null) {
			String parentId = reversedItemIdMap.get(bean.getParent());
			if (parentId != null) {
				dataType.setFolder(parentId);
			} else {
				logger.warn("unknown parent");
			}
		}
		
		// notes
		dataType.setNotes(bean.getNotes());
		
		// storage method
		// for now all data content goes to session --> type is local session
		dataType.setStorageType(StorageMethod.LOCAL_SESSION.name());
		
		// url
		dataType.setUrl(newURL.toString());
		
		// cache url
		if (bean.getCacheUrl() != null) {
			dataType.setCacheUrl(bean.getCacheUrl().toString());
		}

		// FIXME accept beans without operation?
		if (bean.getOperationRecord() != null) {
			OperationRecord operationRecord = bean.getOperationRecord();
			String operId;
			
			// write operation or lookup already written
			if (!operationRecordIdMap.containsValue(operationRecord) ) {
				operId = generateId(operationRecord);
				saveOperationMetadata(operationRecord, operId);

			} else {
				operId = reversedOperationRecordIdMap.get(operationRecord).toString();
			}

			// link data to operation
			operationRecordTypeMap.get(operId).getOutput().add(beanId);
			
			// link the operation to data
			dataType.setResultOf(operId);
		}
		
		// links to other datasets
		for (Link type : Link.values()) {
			for (DataBean target : bean.getLinkTargets(type)) {
				String targetId = reversedItemIdMap.get(target);				
				// if for some weird reason target was not around when generating ids, skip it
				if (targetId == null) {
					continue;
				}
				LinkType linkType = factory.createLinkType();
				linkType.setTarget(targetId);
				linkType.setType(type.name());
				
				dataType.getLink().add(linkType);
			}
		}		
		
		sessionType.getData().add(dataType);
	}

	
	private void saveOperationMetadata(OperationRecord operationRecord, String operationId) {
		OperationType operationType = factory.createOperationType();
		
		// session id
		operationType.setId(operationId);
		
		// name
		NameType nameType = createNameType(operationRecord.getNameID());
		operationType.setName(nameType);
		
		// parameters
		for (ParameterRecord parameterRecord : operationRecord.getParameters()) {

			// Write parameter only when value is not empty
			if (parameterRecord.getValue() != null && !parameterRecord.getValue().equals("")) {	
				ParameterType parameterType = factory.createParameterType();
				parameterType.setName(createNameType(parameterRecord.getNameID()));
				parameterType.setValue(parameterRecord.getValue());
				operationType.getParameter().add(parameterType);
			}
		}

		// inputs
		for (InputRecord inputRecord : operationRecord.getInputs()) {

			String inputID = reversedItemIdMap.get(inputRecord.getValue());
			// skip inputs which were not around when generating ids
			if (inputID == null) {
				continue;
			}
			InputType inputType = factory.createInputType();
			inputType.setName(createNameType(inputRecord.getNameID()));
			inputType.setData(inputID);
			
			operationType.getInput().add(inputType);
		}
		
		// category
		operationType.setCategory(operationRecord.getCategoryName());
		if (operationRecord.getCategoryColor() != null) {
			operationType.setCategoryColor(SwingTools.colorToHexString(operationRecord.getCategoryColor()));
		}
		
		
		sessionType.getOperation().add(operationType);
		operationRecordTypeMap.put(operationId, operationType);
	}	


	private String getNewZipEntryName() {
		return "file-" + entryCounter++;
	}

	
	private void writeDataBeanContentsToZipFile(ZipOutputStream zipOutputStream) throws IOException {
		for (Entry<DataBean, URL> entry : this.newURLs.entrySet()) {
			String entryName = entry.getValue().getRef();

			// write bean contents to zip
			writeFile(zipOutputStream, entryName, entry.getKey().getContentByteStream());
		}
	}
	
	
	private void writeFile(ZipOutputStream out, String name, InputStream in) throws IOException {
		
		int byteCount;
		ZipEntry cpZipEntry = new ZipEntry(name);
		out.putNextEntry(cpZipEntry);

		byte[] b = new byte[DATA_BLOCK_SIZE];

		while ( (byteCount = in.read(b, 0, DATA_BLOCK_SIZE)) != -1 ) {
			out.write(b, 0, byteCount);
		}

		out.closeEntry() ;							
	}

	
	private NameType createNameType(String id, String displayName, String desription) {
		NameType nameType = factory.createNameType();
		nameType.setId(id);
		nameType.setDisplayName(displayName);
		nameType.setDescription(desription);
		return nameType;
	}
	

	private NameType createNameType(NameID nameID) {
		return createNameType(nameID.getID(), nameID.getDisplayName(), nameID.getDescription());
	}
}

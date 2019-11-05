package com.sapienter.jbilling.server.item.batch;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.*;

/**
 * The ItemProcessor adds all the relationships to the AssetDTO objects and does validation.
 * Any object which fails validation will result in the original line and an error message written to the error file
 *
 * @author Gerhard
 * @since 03/05/13
 */
public class AssetProcessor implements ItemProcessor<WrappedObjectLine<FieldSet>, WrappedObjectLine<AssetDTO>>, InitializingBean, StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(AssetProcessor.class);

    /** Company that will be assigned to the asset */
    private CompanyDTO          entity;
    /** Item that will be assigned to the asset */
    private ItemDTO             item;
    /** ItemType which allows asset management linked to the item */
    private ItemTypeDTO         itemType;
    /** Default status that will assigned to the asset */
    private AssetStatusDTO      defaultStatus;

    private AssetBL             assetBL;
    /** id of Item that will be assigned to the asset */
    private int                 itemId;
    /**JobExecution's Execution Context */
    private ExecutionContext    executionContext;

    /** The column names are in the first line of the line */
    private List<String> columnNames = null;
    /** MetaField name to MetaField map */
    private Map<String, MetaField> nameMetaFieldMap;
    private String identifierColumnName;
    private String notesColumnName;
    private String globalColumnName;
    private String entitiesColumnName;
    /** error file writer */
    private ResourceAwareItemWriterItemStream errorWriter;
    /**
     * Clear all references to linked objects and get their IDs from the JobParameters
     *
     * @param stepExecution
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        executionContext    = stepExecution.getJobExecution().getExecutionContext();
        entity              = new CompanyDTO(jobParameters.getLong(AssetImportConstants.JOB_PARM_ENTITY_ID).intValue());
        itemId              = jobParameters.getLong(AssetImportConstants.JOB_PARM_ITEM_ID).intValue();
        identifierColumnName= jobParameters.getString(AssetImportConstants.JOB_PARM_ID_COLUMN);
        notesColumnName     = jobParameters.getString(AssetImportConstants.JOB_PARM_NOTES_COLUMN);
        globalColumnName 	= jobParameters.getString(AssetImportConstants.JOB_PARM_GLOBAL);
        entitiesColumnName	= jobParameters.getString(AssetImportConstants.JOB_PARM_ENTITIES);
        itemType            = null;
        defaultStatus       = null;
        columnNames         = null;
        nameMetaFieldMap    = new HashMap<String, MetaField>();
        assetBL             = new AssetBL();
        LOG.debug("beforeStep: entityId %1$d itemId %2$d identifierColumnName %3$s notesColumnName %4$s", entity.getId(), itemId, identifierColumnName, notesColumnName);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    /**
     * Adds all the relationships to the AssetDTO objects and does validation.
     *
     * @param wrappedDto
     * @return the wrapper AssetDTO
     * @throws Exception when the validation fails
     */
    @Override
    public WrappedObjectLine<AssetDTO> process(WrappedObjectLine<FieldSet> wrappedDto) throws Exception {
        LOG.debug("[%1$d] %2$s", wrappedDto.getLineNr(), wrappedDto.getLine());

        //these can't be loaded in the beforeStep, because no Hibernate Session is available
        reloadReferencedObjects();

        FieldSet fieldSet = wrappedDto.getObject();

        //check if this is the first line, then we must reader column names
        if(columnNames == null) {
            StringBuilder ignoredColumns = new StringBuilder();

            //check that the columns names map to meta fields and create a
            //map between them
            columnNames = new ArrayList<String>(fieldSet.getFieldCount());
            for(int i=0; i<fieldSet.getFieldCount(); i++) {
                String columnName = fieldSet.readString(i);
                columnNames.add(columnName);
                MetaField metaField = itemType.findMetaField(columnName);
                if(metaField != null) {
                    nameMetaFieldMap.put(columnName, metaField);
                } else if(!(identifierColumnName.equals(columnName) || notesColumnName.equals(columnName)
                        || columnName.startsWith(AssetImportConstants.CONTAINED_ASSET_ITEM_COL_PREF)
                        || columnName.startsWith(AssetImportConstants.CONTAINED_ASSET_COL_PREF) || globalColumnName.equals(columnName) || entitiesColumnName.equals(columnName) )) {
                    ignoredColumns.append(" '").append(columnName).append('\'');
                }
            }

            //if we have unknown columns, write a message to the error file
            if(ignoredColumns.length() > 0) {
                writeLineToErrorFile(wrappedDto, "Ignored columns: "+ignoredColumns);
            }
            return null;
        }

        //update the total line read count
        int cnt = executionContext.getInt(AssetImportConstants.JOB_PARM_TOTAL_LINE_COUNT, 0);
        executionContext.putInt(AssetImportConstants.JOB_PARM_TOTAL_LINE_COUNT, cnt+1);

        //check if we have the right amount of tokens
        if(fieldSet.getFieldCount() != columnNames.size()) {
            writeLineToErrorFile(wrappedDto, "Expected ["+columnNames.size()+"] tokens found ["+fieldSet.getFieldCount());
            return null;
        }

        //Indexes of contained assets already processed.
        Map<String, String> containedAssets = new HashMap<String, String>();

        AssetDTO dto = new AssetDTO();
        //set the relationships on the AssetDTO
        dto.setEntity(entity);
        dto.setItem(item);
        dto.setAssetStatus(defaultStatus);
        dto.setCreateDatetime(new Date());
        dto.setDeleted(0);

        //List of contained assets to link to the Asset Group
        List<AssetDTO> containedAssetList = new ArrayList<AssetDTO>();

        //set the values based on the columns names in the first line
        for(int i=0; i <columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            String columnValue = fieldSet.readString(i).trim().replaceAll("\",\"", ",");
            
            if(identifierColumnName.equals(columnName)) {
                dto.setIdentifier(columnValue);
            } else if(notesColumnName.equals(columnName)) {
                dto.setNotes(columnValue);
            } else if(globalColumnName.equals(columnName)) {
            	if(StringUtils.equalsIgnoreCase("TRUE", columnValue)) {
            		dto.setGlobal(Boolean.TRUE);
            	} else {
            		dto.setGlobal(Boolean.FALSE);
            	}
            } else if(entitiesColumnName.equals(columnName)) {
            	// Entities is seprated by pipe "|" .
            	// Ex company ids 10 and 11 can be saved as '10|11'
            	Set<Integer> entityList = new HashSet<>();
            	CompanyDAS das = new CompanyDAS();
            	if(StringUtils.isNotEmpty(columnValue)) {
		            for(String s : StringUtils.trim(columnValue).split("\\|")) {
		            	try {
		            		if (null != das.findNow(Integer.parseInt(s))) {
		            			entityList.add(Integer.parseInt(s));
		            		} else {
		            			writeLineToErrorFile(wrappedDto, "The entity with  Id " + s +" does not exist");
		            		}
		            	} catch (NumberFormatException e ) {
		            		writeLineToErrorFile(wrappedDto, "The entity with  Id " + s +" does not exist");
		            	}
		            }
	            	dto.setEntities(AssetBL.convertToCompanyDTO(entityList));
            	} else {
            		entityList.add(entity.getId());
            		dto.setEntities(AssetBL.convertToCompanyDTO(entityList));
            	}
            	
            }
            
            
            else if(columnName.startsWith(AssetImportConstants.CONTAINED_ASSET_COL_PREF) || columnName.startsWith(AssetImportConstants.CONTAINED_ASSET_ITEM_COL_PREF)) {
                if(columnValue.trim().length() == 0) continue;

                //if this is a product group, load the contained assets
                String idx = columnName.startsWith(AssetImportConstants.CONTAINED_ASSET_ITEM_COL_PREF) ? columnName.substring(AssetImportConstants.CONTAINED_ASSET_ITEM_COL_PREF.length()) :
                        columnName.substring(AssetImportConstants.CONTAINED_ASSET_COL_PREF.length());

                String assetIdKey = AssetImportConstants.CONTAINED_ASSET_COL_PREF+idx;
                String assetItemKey = AssetImportConstants.CONTAINED_ASSET_ITEM_COL_PREF+idx;

                //check to see if we already processed this asset idx
                if(containedAssets.containsKey(assetIdKey) && containedAssets.containsKey(assetItemKey)) {
                    writeLineToErrorFile(wrappedDto, "Contained asset '"+assetIdKey+"' already processed with value '"+containedAssets.get(assetIdKey)+"'");
                    return null;
                }

                //add the column name and value to the map of contained assets
                containedAssets.put(columnName, columnValue);

                if(containedAssets.containsKey(assetIdKey) && containedAssets.containsKey(assetItemKey)) {
                    AssetDTO containedAsset = assetBL.getForItemAndIdentifier(containedAssets.get(assetIdKey), Integer.parseInt(containedAssets.get(assetItemKey)) );

                    //if the asset doesn't exist
                    if(containedAsset == null) {
                        writeLineToErrorFile(wrappedDto, "Unable to find contained asset with id '"+containedAssets.get(assetIdKey)+"'");
                        return null;
                    }

                    //add the asset to the list of contained assets. will be added to the group later.
                    containedAssetList.add(containedAsset);
                }
            } else {
                MetaField metaField = nameMetaFieldMap.get(columnName);

                if(metaField != null) {
                    MetaFieldValue value = metaField.createValue();

                    //if no value was supplied get the default value
                    if(columnValue.length() == 0) {
                        if(metaField.getDefaultValue() != null) {
                            value.setValue(metaField.getDefaultValue().getValue());
                        }
                    } else {
                        try {
                            value.setValue(getMetaFieldValueFromString(metaField.getDataType(), columnValue));
                        } catch (Exception e) {
                            LOG.warn("Error converting column '%s' with value '%s'", columnName, columnValue, e);
                            writeLineToErrorFile(wrappedDto, "Error converting column '"+columnName+"' with value '"+columnValue+"'");
                            return null;
                        }
                    }

                    //set the metafield value
                    MetaFieldHelper.setMetaField(dto, value, null);
                }
            }
        }

        //VALIDATION
        //validate the meta fields
        for(MetaField metaField: itemType.getAssetMetaFields()) {
            try {
                MetaFieldBL.validateMetaField(metaField, dto.getMetaField(metaField.getName()), null);
            } catch (SessionInternalError e) {
                writeLineToErrorFile(wrappedDto, "Validation failed for meta field '"+metaField.getName()+"' with value '"+dto.getMetaField(metaField.getName())+"'");
                return null;
            }
        }

        //check if the product allows asset management
        if(dto.getItem().getAssetManagementEnabled() == 0) {
            writeLineToErrorFile(wrappedDto, "The product does not allow asset management");
            return null;
        }

        //identifier must be between 1 and 200 characters
        if(dto.getIdentifier() == null || dto.getIdentifier().length() < 1 || dto.getIdentifier().length() > 200) {
            writeLineToErrorFile(wrappedDto, "The identifier must be between 1 and 200 characters long");
            return null;
        }

        //identifier must be unique per category
        //if the step's commit interval > 1 we would have to cache identifiers in the processor to check as well
        try {
            assetBL.checkForDuplicateIdentifier(dto);
        } catch (SessionInternalError e) {
            writeLineToErrorFile(wrappedDto, "An asset with the identifier already exists");
            return null;
        }

        //validate the members of this asset group is not already linked to another group or linked to an order
        try {
            assetBL.checkContainedAssets(containedAssetList, 0);
        } catch (SessionInternalError e) {
            writeLineToErrorFile(wrappedDto, e.getMessage());
            return null;
        }

        //add all the contained assets
        //this is done at the end to avoid a TransientObjectException
        dto.addContainedAssets(containedAssetList);
        return new WrappedObjectLine<AssetDTO>(wrappedDto.getLineNr(), wrappedDto.getLine(), dto);
    }

    /**
     * increment the error line count
     */
    private void incrementErrorCount() {
        int cnt = executionContext.getInt(AssetImportConstants.JOB_PARM_ERROR_LINE_COUNT, 0);
        executionContext.putInt(AssetImportConstants.JOB_PARM_ERROR_LINE_COUNT, cnt+1);

    }

    /**
     * Reload the referenced objects if they are not loaded
     */
    private void reloadReferencedObjects() {
        if(itemType == null) {
            item            = new ItemBL(itemId).getEntity();
            itemType        = item.findItemTypeWithAssetManagement();
            defaultStatus   = (itemType != null ? itemType.findDefaultAssetStatus() : null);

            LOG.debug("ItemType with asset management %1$s", itemType);
            LOG.debug("Default Status %1$s", defaultStatus);
            //load all objects we will need while processing
            if (null != item.getEntity())
            	item.getEntity().getLanguageId();
            if (CollectionUtils.isNotEmpty(item.getEntities())) {
            	item.getEntities().iterator().next().getLanguageId();
            }
            item.getMetaFields();
        }
    }

    /**
     * Convert the stringValue into the specified dataType.
     *
     * @param dataType
     * @param stringValue
     * @return
     */
    private Object getMetaFieldValueFromString(DataType dataType, String stringValue) {
        Object value = null;
        switch (dataType) {
            case BOOLEAN:
                value = Boolean.parseBoolean(stringValue);
                break;
            case DATE:
                try {
                    if(stringValue.length() == 10) {
                        value = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(stringValue).toDate();
                    } else if(stringValue.length() == 16) {
                        value = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(stringValue).toDate();
                    } else {
                        throw new IllegalArgumentException("Date ["+stringValue+"] must have format 'yyyy-MM-dd HH:mm' or 'yyyy-MM-dd'");
                    }
                    break;
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Date ["+stringValue+"] must have format 'yyyy-MM-dd HH:mm' or 'yyyy-MM-dd'", ex);
                }
            case DECIMAL:
                value = new BigDecimal(stringValue);
                break;
            case INTEGER:
                value = new Integer(stringValue);
                break;
            case LIST:
                value = Arrays.asList(stringValue.split(","));
                break;
            default:
                value = stringValue;
        }
        return value;
    }

    public void setErrorWriter(ResourceAwareItemWriterItemStream errorWriter) {
        this.errorWriter = errorWriter;
    }

    /**
     * Write the original line the WrappedObjectLine and the message to the error file
     *
     * @param item      Object containing the error
     * @param message   Error message to append to line
     * @throws Exception Thrown when an error occurred while trying to write to the file
     */
    private void writeLineToErrorFile(WrappedObjectLine<FieldSet> item, String message) throws Exception {
        incrementErrorCount();
        errorWriter.write(Arrays.asList(item.getLine() + ',' + message));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(errorWriter, "ErrorWriter must be set");
    }
    
}

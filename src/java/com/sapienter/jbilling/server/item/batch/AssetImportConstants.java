package com.sapienter.jbilling.server.item.batch;

/**
 * Constants used by the Asset Import batch job.
 *
 * @author Gerhard
 * @since 03/05/13
 */
public interface AssetImportConstants {
    // Parameters in JobParameters
    public static final String JOB_PARM_ITEM_ID             = "item_id";
    public static final String JOB_PARM_INPUT_FILE          = "input_file";
    public static final String JOB_PARM_ERROR_FILE          = "error_file";
    public static final String JOB_PARM_ENTITY_ID           = "entity_id";
    public static final String JOB_PARM_ID_COLUMN           = "id_column_name";
    public static final String JOB_PARM_NOTES_COLUMN        = "notes_column_name";
    public static final String JOB_PARM_USER_ID             = "user_id";
    public static final String JOB_PARM_GLOBAL             	= "global";
    public static final String JOB_PARM_ENTITIES            = "entities";
    // Parameters in JobInstance's ExecutionContext
    public static final String JOB_PARM_TOTAL_LINE_COUNT    = "total_lines";
    public static final String JOB_PARM_ERROR_LINE_COUNT    = "error_lines";

    //CSV headers for contained assets of a group
    public static final String CONTAINED_ASSET_COL_PREF     = "Asset";
    public static final String CONTAINED_ASSET_ITEM_COL_PREF= "AssetProduct";
}

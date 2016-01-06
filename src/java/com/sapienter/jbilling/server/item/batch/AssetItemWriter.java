package com.sapienter.jbilling.server.item.batch;

import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * Write the AssetDTO objects to the database.
 * It uses the AssetBL to ensure the right events gets fired.
 *
 * @author Gerhard
 * @since 14/05/13
 */
public class AssetItemWriter implements ItemWriter<WrappedObjectLine<AssetDTO>> {

    /** The user who did the import */
    private int userId;

    @BeforeStep
    void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();
        userId          = jobParameters.getLong(AssetImportConstants.JOB_PARM_USER_ID).intValue();
    }

    @Override
    public void write(List<? extends WrappedObjectLine<AssetDTO>> wrappedObjectLines) throws Exception {
        AssetBL assetBL = new AssetBL();
        for(WrappedObjectLine<AssetDTO> wrappedObjectLine : wrappedObjectLines) {
            assetBL.create(wrappedObjectLine.getObject(), userId);
        }
    }
}

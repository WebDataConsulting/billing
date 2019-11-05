package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * The plugin deletes files in a folder if they are older than a specified time.
 * The plugin takes the following parameters
 *  - folder_1 - Folder to scan
 *  - file_name_regex_1 - Regular expression to match file names ({@link java.util.regex.Pattern}
 *  - age_in_minutes_1 - File will be deleted if it's last modified date is older than the value
 *
 *  More folders and files can be added by adding another set of values for the parameters (i.e.
 *  folder_2, file_name_regex_2, age_in_minutes_2).
 *
 *  The variable ${base_dir} may be used to specify the application context folder.
 *
 * @author Gerhard Maree
 * @since 17/05/2013
 */
public class FileCleanupTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(FileCleanupTask.class);

    private static final String PARAM_FOLDER_STR = "folder_";
    private static final String PARAM_FILE_NAME_PATTERN_STR = "file_name_regex_";
    private static final String PARAM_AGE_IN_MINS_STR = "age_in_minutes_";

    protected static final ParameterDescription PARAM_FOLDER =
            new ParameterDescription(PARAM_FOLDER_STR + "1", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_FILE_NAME_PATTERN =
            new ParameterDescription(PARAM_FILE_NAME_PATTERN_STR + "1", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_AGE_IN_MINS =
            new ParameterDescription(PARAM_AGE_IN_MINS_STR + "1", false, ParameterDescription.Type.INT);

    public static final String FOLDER_BASE_DIR = "${base_dir}";

    //initializer for pluggable params
    {
        descriptions.add(PARAM_FOLDER);
        descriptions.add(PARAM_FILE_NAME_PATTERN);
        descriptions.add(PARAM_AGE_IN_MINS);
    }

    @Override
    public String getTaskName() {
        return "FileCleanupTask";
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        try {
            LOG.debug("FileCleanupTask ... started");
            _init(context);

            String folderStr = getParameter(PARAM_FOLDER_STR + "1", "");
            //check that we have all the parameters
            if (folderStr.trim().isEmpty()) {
                throw new JobExecutionException("Parameter " + PARAM_FOLDER.getName() + "1 not specified");
            }

            int idx = 1;
            //while there is a folder specified for the index (folder_1, folder_2 ....)
            while (!folderStr.trim().isEmpty()) {
                //get the file name expression and max age from the parameters
                String fileNameStr = getParameter(PARAM_FILE_NAME_PATTERN_STR + idx, "").trim();
                int ageInMins = getParameter(PARAM_AGE_IN_MINS_STR + idx, 60);

                LOG.debug("FileCleanupTask: Folder [%1$s], File Name Regex [%2$s], Age [$3$d] ", folderStr, fileNameStr, ageInMins);

                if (fileNameStr.trim().isEmpty()) {
                    throw new JobExecutionException("Parameter file_name_regex." + idx + " not specified");
                }

                final Pattern fileNamePattern = Pattern.compile(fileNameStr);

                //determine the base_dir location if necessary
                if (folderStr.contains(FOLDER_BASE_DIR)) {
                    String baseDir = Util.getSysProp("base_dir");
                    folderStr = folderStr.replace(FOLDER_BASE_DIR, baseDir);
                }

                File folder = new File(folderStr);

                //list all files which conforms to the file regex pattern
                File[] files = folder.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return fileNamePattern.matcher(name).matches();
                    }
                });

                //determine the maximum age a file can have
                long maxFileAge = System.currentTimeMillis() - (ageInMins * 60 * 1000);

                //delete the files
                if (files != null) {
                    for (File file : files) {
                        if (file.lastModified() < maxFileAge) {
                            file.delete();
                        }
                    }
                }

                idx++;
                folderStr = getParameter(PARAM_FOLDER_STR + idx, "");
            }
        } catch (Exception e) {
            LOG.error("Unable to clean up folder", e);
        }
        LOG.debug("FileCleanupTask ... finished");

    }
}

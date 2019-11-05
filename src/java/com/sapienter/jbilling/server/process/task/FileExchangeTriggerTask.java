package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.server.pluggableTask.IPluggableTaskSessionBean;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.Context;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author Vladimir Carevski
 * @since 04-APR-2014
 */
public class FileExchangeTriggerTask extends AbstractCronTask {

    private static final String PARAM_PLUGIN_ID = "plugin_id";

    private static final ParameterDescription PARAM_PLUGIN_ID_DESC =
            new ParameterDescription(PARAM_PLUGIN_ID, true, ParameterDescription.Type.STR);

    {
        descriptions.add(PARAM_PLUGIN_ID_DESC);
    }

    public String getTaskName() {
        return "Triggering Remote Copy, entity Id: " + getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        super._init(context);

        String pluginIdStr = getParameter(PARAM_PLUGIN_ID_DESC.getName(), "").trim();
        if (null == pluginIdStr || pluginIdStr.isEmpty()) {
            throw new JobExecutionException("No Plugin ID Configured");
        }
        try {
            Integer pluginId = Integer.parseInt(pluginIdStr);
            IFileExchangeTask fileExchangeTask = instantiateTask(pluginId);
            if (null != fileExchangeTask) {
                fileExchangeTask.execute();
            } else {
                throw new JobExecutionException("Can Not Create a Plugin with Given ID:" + pluginId);
            }
        } catch (NumberFormatException nfe) {
            throw new JobExecutionException(nfe);
        } catch (PluggableTaskException pte) {
            throw new JobExecutionException(pte);
        }
    }

    private IFileExchangeTask instantiateTask(Integer pluginId)
            throws PluggableTaskException {
        PluggableTaskBL<IFileExchangeTask> taskLoader =
                new PluggableTaskBL<IFileExchangeTask>(pluginId);
        IPluggableTaskSessionBean pluginSessionBean = getPluginSessionBean();
        PluggableTaskDTO dto = pluginSessionBean.getDTO(pluginId);
        if(null == dto) return null;
        taskLoader.set(dto);
        return taskLoader.instantiateTask();
    }

    private IPluggableTaskSessionBean getPluginSessionBean(){
        return (IPluggableTaskSessionBean) Context.
                getBean(Context.Name.PLUGGABLE_TASK_SESSION);
    }
}

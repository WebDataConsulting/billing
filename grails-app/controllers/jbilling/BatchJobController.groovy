package jbilling

import com.sapienter.jbilling.server.item.batch.AssetImportConstants
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution

/**
 * Controller to interrogate jobs run as Spring Batch jobs.
 *
 * @author Gerhard
 * @since 14/05/2013
 */
class BatchJobController {

    def jobExplorer

    /**
     * Check the status of a JobExecution. The status can be either 'done', 'fail' or 'busy'
     * It will either render a view, template or chain the response depending on the provided parameters.
     * E.g if the status is 'busy' it will forward to either 'busyTemplate', 'busyView' or a controller
     * specified by 'busyController', 'busyAction' and 'busyId'.
     * If all statuses must render the same response specify only 'template', 'view', or a
     * chain with 'controller', 'action' and 'id'
     *
     * In the model is provided
     *  jobParams:          JobParameters
     *  jobStatus:          'done', 'fail' or 'busy'
     *  jobId:              JobExecution id
     *  executionParams:    ExecutionContext of the JobExecution
     */
    def jobStatus (){
        JobExecution execution = jobExplorer.getJobExecution(params.long('id'))
        BatchStatus status = execution.status
		
        if (BatchStatus.COMPLETED == status) {
            displayJobResult('done', execution, params)
        } else if ([BatchStatus.FAILED, BatchStatus.ABANDONED, BatchStatus.UNKNOWN  ].contains(status) ) {
            displayJobResult('fail', execution, params)
        } else {
            displayJobResult('busy', execution, params)
        }
    }

    /**
     * Does the forwarding for the jobStatus action. See jobStatus for a description.
     *
     * @param jobStatus     'done', 'fail' or 'busy'
     * @param jobExecution  JobExecution for the current job
     * @param params        parameters passed to action
     */
    private void displayJobResult(jobStatus, jobExecution, params) {
        //build the model that will be supplied to the view
        def jobModel = [jobParams: jobExecution.jobParameters, jobStatus: jobStatus, jobId: params.id, executionParams: jobExecution.executionContext]

        //check if a template if specified as forward
        if (params[jobStatus+'Template'] || params['template']) {
            def template = params[jobStatus+'Template'] ?: params['template']
            render template: template, model: jobModel

        //check if a view if specified as forward
        } else if (params[jobStatus+'View'] || params['view']) {
            def view = params[jobStatus+'View'] ?: params['view']
            render view: view, model: jobModel

        //check if the forward must be chained
        } else {
            def controller = params[jobStatus+'Controller'] ?: params['controller']
            def action = params[jobStatus+'Action'] ?: params['action']
            def id = params[jobStatus+'Id'] ?: params['id']
            chain controller: controller, action: action, id: id, model: jobModel
        }
    }

    /**
     * Download a file, where the location is a parameter passed to the JobInstance
     *
     * @param id    JobExecution id
     * @param key   key in the JobInstance's JobParameters
     */
    def jobFile (){
        //get the file name from the JobParameters
        JobExecution jobExecution = jobExplorer.getJobExecution(params.long('id'))
        String fileName = jobExecution.jobParameters.getString(params.key) ?: jobExecution.executionContext.getString(params.key)

        def file = new File(fileName)
        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "attachment;filename=${file.getName()}")

        response.outputStream << file.newInputStream()
    }
}

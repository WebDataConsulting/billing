/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */
package com.sapienter.jbilling.server.rule;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.drools.KnowledgeBase;
import org.drools.agent.KnowledgeAgent;
import org.drools.agent.KnowledgeAgentFactory;
import org.drools.io.ResourceChangeScannerConfiguration;
import org.drools.io.ResourceFactory;
import org.drools.io.impl.ByteArrayResource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.StatelessKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.mvel2.optimizers.OptimizerFactory;

/**
 *
 * @author emilc
 */
@Deprecated
public abstract class RulesBaseTask extends PluggableTask {

    public static final ParameterDescription PARAM_FILE = new ParameterDescription("file", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAM_URL = new ParameterDescription("url", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAM_DIR = new ParameterDescription("dir", false, ParameterDescription.Type.STR);

    { 
        descriptions.add(PARAM_FILE);
        descriptions.add(PARAM_URL);
        descriptions.add(PARAM_DIR);
    }

    private static Map<Integer, KnowledgeAgent> knowledgeBasesCache = new HashMap<Integer, KnowledgeAgent>();
    private static AtomicBoolean isRulesChangeScanerStarted = new AtomicBoolean(false);

    protected Hashtable<Object, FactHandle> handlers = null;
    protected StatefulKnowledgeSession session = null;
    protected List<Object> rulesMemoryContext = new ArrayList<Object>();

    // abstract log provider, forces the implementing plug-in to produce the logger instance
    protected FormatLogger LOG = getLog();
    protected abstract FormatLogger getLog();


    protected void executeRules() throws TaskException {
        // show what's in first
        for (Object o: rulesMemoryContext) {
            LOG.debug("in memory context= %s", o);
        }

        // JBRULES-2253: NoClassDefFoundError with the ASM optimizer when optimizing MVEL consequences.
        // Use the reflective optimizer as a workaround - this may reduce rules performance.
        // @see http://mvel.codehaus.org/Optimizers
        OptimizerFactory.setDefaultOptimizer("reflective");
        LOG.debug("Using MVEL thread accessor optimizer: %s", OptimizerFactory.getThreadAccessorOptimizer());
        LOG.debug("Using MVEL accessor compiler: %s", OptimizerFactory.getDefaultAccessorCompiler());

        KnowledgeBase knowledgeBase;
        StatelessKnowledgeSession statelessSession;
        try {
            knowledgeBase = readKnowledgeBase();
            statelessSession = knowledgeBase.newStatelessKnowledgeSession();
        } catch (Exception e) {
            throw new TaskException(e);
        }

        // add the log object for the rules to use
        statelessSession.setGlobal("LOG", LOG);
        statelessSession.execute(rulesMemoryContext);
    }

    protected KnowledgeBase readKnowledgeBase() {
        if (knowledgeBasesCache.containsKey(task.getId())) {
            return knowledgeBasesCache.get(task.getId()).getKnowledgeBase();
        }

        // Creating agent with default KnowledgeAgentConfiguration for scanning files and directories
        KnowledgeAgent kAgent = KnowledgeAgentFactory.newKnowledgeAgent("Knowledge agent for task#" + task.getId());

        // Adding resources for observing by KnowledgeAgent and creating KnowledgeBase.
        // Current version of api (5.0.1) does not implement adding resources from KnowledgeBase,
        // that was mentioned in api documentation (may be bug in source code).
        // So, we use other aproach for configuring KnowledgeAgent
        // Now agent interface allowes defining resources and directories for observing
        // only through ChangeSet from Resource (usually xml config file)
        // We create needed configuration dynamically as string
        // from task parameters information
        kAgent.applyChangeSet(new ByteArrayResource(createChangeSetStringFromTaskParameters().getBytes()));

        // Cache agent for further usage without recreation
        knowledgeBasesCache.put(task.getId(), kAgent);
        // Start scanning services for automatical updates of cached agents
        startRulesScannerIfNeeded();

        return kAgent.getKnowledgeBase();
    }

    public static void invalidateRuleCache(Integer taskId) {
        knowledgeBasesCache.remove(taskId);
    }

    protected void executeStatefulRules(StatefulKnowledgeSession session, List context) {
        handlers = new Hashtable<Object, FactHandle>();
        for (Object o : context) {
            if (o != null) {
                LOG.debug("inserting object %s", o);
                handlers.put(o, session.insert(o));
            } else {
                LOG.warn("Attempted to insert a NULL object into the working memeory");
            }

        }

        session.fireAllRules();
        session.dispose();
        handlers.clear();
        handlers = null;
    }

    protected void removeObject(Object o) {
        FactHandle h = handlers.get(o);
        if (h != null) {
            LOG.debug("removing object %s hash %s", o, o.hashCode());
            session.retract(h);
            handlers.remove(o);
        }
    }

    /**
     * Creating ChangeSet configuration from task parameters
     * for obserivng KnowledgeBase
     *
     * @return xml-configuration string
     */
    private String createChangeSetStringFromTaskParameters() {
        // todo: may be some problems (messages in console) now with xml validation during parsing
        // but it's recomended in api-documentation schemas
        StringBuilder str = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<change-set xmlns='http://drools.org/drools-5.0/change-set' " +
                " xmlns:xs='http://www.w3.org/2001/XMLSchema-instance' " +
                " xs:schemaLocation='http://drools.org/drools-5.0/change-set drools-change-set-5.0.xsd' >");
        str.append("<add>");

        String defaultDir = Util.getSysProp("base_dir") + "rules";
        String prefix;
        for (String key : parameters.keySet()) {
            String value = (String) parameters.get(key);
            LOG.debug("processing parameter %s value %s", key, value);
            System.out.println("value *******************************" +value );
            if (StringUtils.isNotEmpty(value)) {
                if (key.equals("file")) {
                    String[] files = com.sapienter.jbilling.server.util.Util
                            .csvSplitLine(value, ' ');
                    for (String file : files) {
                        prefix = "";
                        if (!(new File(file)).isAbsolute()) {
                            // prepend the default directory if file path is relative
                            prefix = defaultDir + File.separator;
                        }
                        LOG.debug("adding parameter %s", file);
                        appendResource(str, "file:" + prefix + file, "PKG");
                    }

                } else if (key.equals("dir")) {
                    String[] dirs = com.sapienter.jbilling.server.util.Util
                            .csvSplitLine(value, ' ');
                    for (String dir : dirs) {
                        prefix = "";
                        if (!new File(dir).isAbsolute()) {
                            // prepend the default directory if directory path is relative
                            prefix = defaultDir + File.separator;
                        }
                        LOG.debug("adding parameter %s", dir);
                        appendResource(str, "file:" + prefix + dir, "PKG");
                    }

                } else if (key.equals("url")) {
                    String[] urls = com.sapienter.jbilling.server.util.Util
                            .csvSplitLine(value, ' ');
                    for (String url : urls) {
                        LOG.debug("adding parameter %s", url);
                        appendResource(str, url, "PKG");
                    }

                } else {
                    //for other types of resources
                    LOG.warn("Resource for parameter %s -> %s not supported", key, value);
                }
            }
        }
        if (parameters.isEmpty()) {
            appendResource(str, "file:" + defaultDir, "PKG");
            LOG.debug("No task parameters, using directory default: %s", defaultDir);
        }

        str.append("</add>");
        str.append("</change-set>");
        return str.toString();
    }

    private void appendResource(StringBuilder builder, String source, String type) {
        builder.append("<resource source='");
        builder.append(source);
        builder.append("' type='");
        builder.append(type);
        builder.append("' />");
    }

    private void startRulesScannerIfNeeded() {
        if (!isRulesChangeScanerStarted.getAndSet(true)) {
            // Set the interval on the ResourceChangeScannerService if it presented in configuration. Default value - 60s.
            if (Util.getSysProp("rules_scanner_interval") != null) {
                ResourceChangeScannerConfiguration sconf = ResourceFactory.getResourceChangeScannerService().newResourceChangeScannerConfiguration();
                // set the disk scanning interval to 30s, default is 60s
                sconf.setProperty("drools.resource.scanner.interval", Util.getSysProp("rules_scanner_interval"));
                ResourceFactory.getResourceChangeScannerService().configure(sconf);
            }
            // Starting services for scanning rules updates
            ResourceFactory.getResourceChangeNotifierService().start();
            ResourceFactory.getResourceChangeScannerService().start();
        }
    }

}

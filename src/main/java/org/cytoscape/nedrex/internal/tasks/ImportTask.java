package org.cytoscape.nedrex.internal.tasks;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.cytoscape.nedrex.internal.*;
import org.cytoscape.nedrex.internal.ui.SearchOptionPanel;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.swing.DialogTaskManager;
import org.json.simple.JSONObject;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NeDRex App
 *
 * @author Sepideh Sadegh, Judith Bernett
 * @author by: Andreas Maier
 */
public class ImportTask extends AbstractTask {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private RepoApplication app;
    private SearchOptionPanel optionsPanel;

    public ImportTask(RepoApplication app, SearchOptionPanel optionsPanel) {
        this.app = app;
        this.setNedrexService(app.getNedrexService());
        this.optionsPanel = optionsPanel;
    }

    protected void showWarningTime() {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(null, "If you want to use the NeDRex app, you need to first agree with our terms of use. The NeDRex Terms of Use are available at: https://api.nedrex.net/static/licence or via the Terms of Use menu in the App.", "License Agreement", JOptionPane.WARNING_MESSAGE);
                    }
                }
        );
    }

    private NeDRexService nedrexService;

    @Reference
    public void setNedrexService(NeDRexService nedrexService) {
        this.nedrexService = nedrexService;
    }

    public void unsetNedrexService(NeDRexService nedrexService) {
        if (this.nedrexService == nedrexService)
            this.nedrexService = null;
    }


    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {

        Boolean agreementStatus = optionsPanel.getAgreementStatus();

        if (agreementStatus) {
            taskMonitor.setTitle("Importing the network");
            taskMonitor.setProgress(0.0);

            JSONObject payload = new JSONObject();
            List<String> nodes = new ArrayList<String>();
            List<String> edges = new ArrayList<String>();

            taskMonitor.setProgress(0.1);
            taskMonitor.setStatusMessage("Processing your request...");

            edges = optionsPanel.getSelectedEdgeTypes();
            List<InteractionType> edgeTypes = optionsPanel.getSelectedEdgeTypeObjects();
            List<String> iidEvids = optionsPanel.getIIDevidence();
            List<String> drugGroups = optionsPanel.getSelectedDrugGroups();
            Boolean ppiSL = optionsPanel.getSelfLoop();
            List<Integer> taxIDs = new ArrayList<Integer>();
            taxIDs.add(9606);
            if (optionsPanel.allTaxIDSelected()) {
                taxIDs.add(-1);
            }
            System.out.println("This is the selected threshold: " + optionsPanel.getThreshold());
            String networkName = optionsPanel.getNetworkName();
            logger.info("The entered name of the new network by user: " + networkName);

            Boolean concise = optionsPanel.conciseVersion();
            logger.info("The option selected for concise: " + concise);

            payload.put("ppi_evidence", iidEvids);
            payload.put("ppi_self_loops", ppiSL);

            payload.put("concise", concise);
            payload.put("include_omim", optionsPanel.includeOMIM());

            if (optionsPanel.includeDisGeNet()) {
                payload.put("disgenet_threshold", optionsPanel.getThreshold());
            } else if (!optionsPanel.includeDisGeNet()) {
                payload.put("disgenet_threshold", 2D);
            }
            HashSet<String> nodes_selected_by_edges = edgeTypes.stream().filter(e -> e.getSourceType() != null).map(e -> e.getSourceType().toString()).collect(Collectors.toCollection(HashSet::new));
            nodes_selected_by_edges.addAll(edgeTypes.stream().filter(e -> e.getTargetType() != null).map(e -> e.getTargetType().toString()).collect(Collectors.toCollection(HashSet::new)));
            nodes_selected_by_edges = nodes_selected_by_edges.stream().map(String::toLowerCase).collect(Collectors.toCollection(HashSet::new));

            if (nodes_selected_by_edges.contains("protein")) {
                payload.put("taxid", taxIDs);
                if (!taxIDs.contains(-1)) {
                    if (!nodes.contains("protein")) {
                        nodes.add("protein");
                    }
                }
                if (optionsPanel.reviewedProteins()) {
                    payload.put("reviewed_proteins", Collections.singletonList(true));
                    if (!nodes.contains("protein")) {
                        nodes.add("protein");
                    }
                } else {
                    payload.put("reviewed_proteins", Arrays.asList(true, false));
                }
            }

            if (nodes_selected_by_edges.contains("drug")) {
                payload.put("drug_groups", drugGroups);
                if (!drugGroups.isEmpty() && !nodes.contains("drug")) {
                    nodes.add("drug");
                }
            }

            payload.put("nodes", nodes);
            payload.put("edges", edges);
            logger.info("The post JSON converted to string: " + payload.toString());
            System.out.println("The post JSON converted to string: " + payload.toString());

            HttpPost post = new HttpPost(this.nedrexService.API_LINK + "graph/builder");

            post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
            String uuid = new String();

            taskMonitor.setProgress(0.2);
            taskMonitor.setStatusMessage("Sending your request to our server...");

            try {

                HttpResponse response = nedrexService.send(post);
                HttpEntity entity = response.getEntity();
                BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent()));
                String line = "";
                logger.info("Response entity: ");
                while ((line = rd.readLine()) != null) {
                    logger.info("The uri of the response to the post: " + line + "\n");
                    System.out.println(line);
                    uuid = line;
                }
                System.out.println("UUID:" + uuid);
                EntityUtils.consume(entity);
            } catch (ClientProtocolException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }

            //// now GET
            String uid = uuid.replace("\"", "");
            System.out.println(uid);
            HttpGet request = new HttpGet(this.nedrexService.API_LINK + "graph/details/" + uid);

            taskMonitor.setProgress(0.3);
            taskMonitor.setStatusMessage("Your network is being built...");
            double progress = 0.3;
            try {
                HttpResponse response = nedrexService.send(request);
                boolean Success = false;
                boolean Failed = false;

                // we're letting it build for t*10 seconds
                for (int t = 0; t < 60; t++) {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        System.out.println(line);
                        logger.info(line);
                        if (line.contains("completed"))
                            Success = true;
                        if (line.contains("failed")) {
                            Failed = true;
                        }
                    }
                    if (Success) {
                        taskMonitor.setProgress(1.0);
                        taskMonitor.setStatusMessage("Successfully built the network!");
                        System.out.println("The build was successful!!!");
                        logger.info("The build was successful!");
                        String urlp = "";
                        networkName = networkName.replaceAll("\\W+", "");
                        if (!networkName.equals("")) {
                            urlp = this.nedrexService.API_LINK + "graph/download/" + uid + "/" + networkName + ".graphml";

                        } else {
                            networkName = uid;
                            urlp = this.nedrexService.API_LINK + "graph/download/" + uid + ".graphml";
                        }

                        DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
                        File file;
                        try {
                            String tempDir = System.getProperty("java.io.tmpdir");
                            file = new File(tempDir, networkName + ".graphml");
                            if (!file.createNewFile()) {
                                file = File.createTempFile("nedrex", ".graphml");
                            }
                        } catch (Exception e) {
                            file = File.createTempFile("nedrex", ".graphml");
                        }
                        System.out.println(file.getAbsolutePath());
                        taskmanager.execute(new TaskIterator(new DownloadNetworkTask(app, urlp, file, nedrexService), new ImportNetworkTask(app, file)));
                        break;
                    }
                    if (Failed) {
                        logger.info("The build has failed!");
                        break;
                    }
                    response = nedrexService.send(request);
                    try {
                        if (progress < 0.9) {
                            progress += 0.1;
                        }
                        taskMonitor.setProgress(progress);
                        logger.info("Waiting for build to complete, sleeping for 10 seconds...");
                        Thread.sleep(10000);
                    } catch (InterruptedException e0) {
                        // TODO Auto-generated catch block
                        e0.printStackTrace();
                    }

                }

            } catch (ClientProtocolException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
        } else if (!agreementStatus) {
            logger.info("The use of NeDRex app is only allowed upon agreeing with the NeDRex Terms of Use available at: https://api.nedrex.net/static/licence");
            showWarningTime();
            taskMonitor.showMessage(Level.WARN, "If you want to use the NeDRex app, please agree with our terms of use. The use of NeDRex app is only allowed upon agreeing with the NeDRex Terms of Use available at: https://api.nedrex.net/static/licence");
        }
    }
}

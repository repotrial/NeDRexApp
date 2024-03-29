package org.cytoscape.myApp.internal.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.myApp.internal.Constant;
import org.cytoscape.myApp.internal.LoadNetworkTask;
import org.cytoscape.myApp.internal.RepoApplication;
import org.cytoscape.myApp.internal.ui.ComorbOptionPanel;
import org.cytoscape.myApp.internal.ui.SearchOptionPanel;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.swing.DialogTaskManager;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeDRex App
 * @author Sepideh Sadegh
 */
public class ImportComorbidTask extends AbstractTask{
	private Logger logger = LoggerFactory.getLogger(getClass());
    private RepoApplication app;
    private ComorbOptionPanel optionsPanel;

    public ImportComorbidTask(RepoApplication app, ComorbOptionPanel optionsPanel){
        this.app = app;
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

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		Boolean agreementStatus = optionsPanel.getAgreementStatus();    	
    	if (agreementStatus) {
        taskMonitor.setTitle("Importing the network");
        taskMonitor.setProgress(0.0);
        JSONObject payload = new JSONObject();
                
        taskMonitor.setProgress(0.1);
        taskMonitor.setStatusMessage("Processing your request...");
        
        if(optionsPanel.includePhiCorMin()) {
        	double minPhiCor = optionsPanel.getPhiCorMin();
        	payload.put("min_phi_cor", minPhiCor);
        }
        if(optionsPanel.includePValMax()) {
        	double maxPVal = optionsPanel.getPValMax();
        	payload.put("max_p_value", maxPVal);
        }

//        System.out.println("This is the selected threshold: " + optionsPanel.getThreshold());
        String networkName = optionsPanel.getNetworkName();
        logger.info("The entered name of the new network by user: " + networkName);

        if(optionsPanel.isInduced()) {
        	CyNetwork network = app.getCurrentNetwork();
        	Set<CyNode> mondoSelectedNodes = new HashSet<CyNode>(CyTableUtil.getNodesInState(network,CyNetwork.SELECTED,true));
        	List<String> selectedNodeNames = new ArrayList<String>();
        	for (CyNode n: mondoSelectedNodes) {
    			String nodeName = network.getRow(n).get(CyNetwork.NAME, String.class);
    			selectedNodeNames.add(nodeName);
    		}
        	payload.put("mondo", selectedNodeNames);
        }
//        logger.info("The post JSON converted to string: " + payload.toString());
        
        app.deselectCurrentNetwork();

        HttpPost post = new HttpPost(Constant.Dev_API_LINK+ "comorbiditome/submit_comorbiditome_build");
        HttpClient client = new DefaultHttpClient();

        post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
        String uidd = new String();

        taskMonitor.setProgress(0.2);
        taskMonitor.setStatusMessage("Sending your request to our server...");

        try {

            HttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();
            BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent()));
            String line = "";
            logger.info("Response entity: ");
            while ((line = rd.readLine()) != null) {
                System.out.println(line);
                logger.info("The uri of the response to the post: "+line + "\n");
                uidd = line;
            }
            EntityUtils.consume(entity);
        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        //// now GET
        String uid = uidd.replace("\"", "");
//        HttpGet request = new HttpGet(Constant.Dev_API_LINK+"comorbiditome/comorbiditome_build_status"+uid); // uid is the query parameter here (different than graph build. Modify this line  
        
        HttpGet request = new HttpGet(Constant.Dev_API_LINK+"comorbiditome/comorbiditome_build_status");
//		HttpClient client2 = new DefaultHttpClient();
		URI uri = new URIBuilder(request.getURI()).addParameter("uid", uid).build();
		((HttpRequestBase) request).setURI(uri);
		
		

        taskMonitor.setProgress(0.3);
        taskMonitor.setStatusMessage("Your network is being built...");
        double progress = 0.3;
        try {
            HttpResponse response = client.execute(request);
            boolean Success = false;
            boolean Failed = false;

            // we're letting it build for t*2 seconds
            for (int t=0; t<90; t++) {
                BufferedReader rd = new BufferedReader (new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    System.out.println(line);
//                    logger.info(line);
                    if (line.contains("completed"))
                        Success=true;
                    if (line.contains("failed")) {
                        Failed=true;
                    }
                }
                if(Success) {
                    taskMonitor.setProgress(1.0);
                    taskMonitor.setStatusMessage("Successfully built the network!");
                    System.out.println("The built was successful!!!");
                    logger.info("The built was successful!");
                    String urlp = "";
                    if (!networkName.equals("")) {
                        urlp = Constant.Dev_API_LINK+"comorbiditome/download_comorbiditome_build/"+uid+"/graphml/"+networkName+".graphml";                        
                    }
                    else {
                    	urlp = Constant.Dev_API_LINK+"comorbiditome/download_comorbiditome_build/"+uid+"/graphml/"+uid.toString()+".graphml";
                        
                    }

                    DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
                    taskmanager.execute(new TaskIterator(new LoadNetworkTask(app,urlp)));
                    break;
                }
                if (Failed) {
                    logger.info("The build is failed!");
                    break;
                }
                response = client.execute(request);
                try {
                    if(progress < 0.9) {
                        progress += 0.1;
                    }
                    taskMonitor.setProgress(progress);
                    logger.info("Waiting for build to complete, sleeping for 2 seconds...");
                    Thread.sleep(5000);
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
    	}
        
        else if (!agreementStatus) {
			logger.info("The use of NeDRex app is only allowed upon agreeing with the NeDRex Terms of Use available at: https://api.nedrex.net/static/licence");
			showWarningTime();
			taskMonitor.showMessage(Level.WARN, "If you want to use the NeDRex app, please agree with our terms of use. The use of NeDRex app is only allowed upon agreeing with the NeDRex Terms of Use available at: https://api.nedrex.net/static/licence");
		}
    }
		
	}
    

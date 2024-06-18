package org.cytoscape.nedrex.internal.tasks;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.EntityUtils;
import org.cytoscape.nedrex.internal.NeDRexService;
import org.cytoscape.nedrex.internal.RepoApplication;
import org.cytoscape.nedrex.internal.RepoResultPanel;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.*;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.util.BoundedInteger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.*;
/**
 * NeDRex App
 * @author Sepideh Sadegh
 * @author Andreas Maier
 */
public class BiConTask extends AbstractTask{
	
	private RepoApplication app;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private Image heatmap;
	private RepoResultPanel resultPanel;
	private HashMap<String, Set<String>> patientgroups;
	
	@ProvidesTitle
	public String getTitle() {return "Set Parameters for BiCoN Algorithm";}
	
	@Tunable(description="Minimal solution subnetwork size", groups="Algorithm settings",
			params="slider=true",
			tooltip="The lower bound for size of the solution subnetwork",
			gravity = 2.0)
    public BoundedInteger lgMin = new BoundedInteger(5, 10, 20, false, false);
	
	@Tunable(description="Maximal solution subnetwork size", groups="Algorithm settings",
			params="slider=true",
			tooltip="The upper bound for size of the solution subnetwork",
			gravity = 2.5)
    public BoundedInteger lgMax = new BoundedInteger(10, 15, 30, false, false);
	
	@Tunable(description="Input file for numerical patient data:" ,  params= "input=true",
			groups="Input file",
			tooltip="<html>Input file should contain gene expression, methylation or any other kind of numerical data for patients. <p>The following format is acceptable: genes as rows, patients as columns, first column genes IDs",
			gravity = 3.0)
	public File inputFile = new File(System.getProperty("user.home"));
	
	@Tunable(description="Use custom name for the result network", groups="Result network",
			tooltip = "Select if you would like to use your own custom name for the result network, otherwise a default name based on the selected algorithm parameters will be assigned",
			gravity = 5.0)
    public Boolean set_net_name = false;
	
	@Tunable(description="Name of the result network", 
	         groups="Result network", 
	         dependsOn="set_net_name=true",
	         tooltip="Enter the name you would like to have assigned to the result network",
	         gravity = 5.0)
	public String new_net_name = new String();
	
	public BiConTask(RepoApplication app, RepoResultPanel resultPanel) {
		this.app = app;
		this.setNedrexService(app.getNedrexService());
		this.resultPanel = resultPanel;
	}
	
	
	protected void showWarningTime() {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, "The computation is taking very long! It continues running in the backend, to get the results please try again using the same parameters and input for the algorithm in 15 mins!", "Long run-time", JOptionPane.WARNING_MESSAGE);
				}
			}
		);
	}
	
	protected void showFailed() {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
//					JOptionPane.showMessageDialog(null, "The request to run the algorithm is failed! Please make sure that either proteins or genes are selected in the network. Uniport AC and entrez Id are acceptable as names for proteins and genes.", "Error", JOptionPane.ERROR_MESSAGE);
					JOptionPane.showMessageDialog(null, "The request to run the algorithm is failed! Please check your inputs.", "Error", JOptionPane.ERROR_MESSAGE);
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
		
		CyNetworkNaming namingUtil = app.getActivator().getService(CyNetworkNaming.class);
		String newNetName = new String();
		if (!set_net_name) {					
			String netName = String.format("BiCoN_lgMin%d_lgMax%d", lgMin.getValue(), lgMax.getValue());
			newNetName = namingUtil.getSuggestedNetworkTitle(netName);
		}
		else if (set_net_name) {
			newNetName = namingUtil.getSuggestedNetworkTitle(new_net_name);
		}
		
		String submiturl = String.format(this.nedrexService.API_LINK + "bicon/submit?lg_min=%s&lg_max=%s", lgMin.getValue(), lgMax.getValue());
		
		logger.info("This is the submitURL: " + submiturl);
		String statusurl = this.nedrexService.API_LINK + "bicon/status";
		String clustermapurl = this.nedrexService.API_LINK + "bicon/clustermap";
		
		HttpPost post = new HttpPost(submiturl);
		FileBody fbody = new FileBody(inputFile);
		MultipartEntity reqEntity = new MultipartEntity();
		reqEntity.addPart("expression_file", fbody);
		int sleep_time = 5; //in seconds
		


		post.setEntity(reqEntity);
		logger.info("The request entity: " + reqEntity);
		logger.info("The original request URI: " + post.getURI());

		
		String uidd = new String();
		Boolean failedSubmit = false;
		
		try {
			//response = client.execute(post);
			HttpResponse response = nedrexService.send(post);
			logger.info("The status line of the response: " + response.getStatusLine());
			HttpEntity entity = response.getEntity();
			BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent()));
			String line = "";
			logger.info("Response entity: ");
			int statusCode = response.getStatusLine().getStatusCode();
			logger.info("The status code of the response: " + statusCode);
			if (statusCode != 200) {
				failedSubmit=true;
			}
			while ((line = rd.readLine()) != null) {
				System.out.println(line);
				logger.info("The uri of the response to the post: "+line + "\n");
				uidd = line;
				if (line.contains("No seed genes submitted")) {
					failedSubmit=true;
				}
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
		if (!failedSubmit) {
			taskMonitor.setProgress(0.1);
			taskMonitor.setStatusMessage("Processing your request...");
			String uid = uidd.replace("\"", "");		
			HttpGet request = new HttpGet(statusurl);
			URI uri = new URIBuilder(request.getURI()).addParameter("uid", uid).build();
			((HttpRequestBase) request).setURI(uri);

			HttpGet requestClustermap = new HttpGet(clustermapurl);
			URI uriclustermap = new URIBuilder(requestClustermap.getURI()).addParameter("uid", uid).build();
			((HttpRequestBase) requestClustermap).setURI(uriclustermap);

			logger.info("The URI: "+ uri);
			logger.info("The uid: " + uid);		
			logger.info("The request URI: "+request.getURI().toString());
			logger.info("The request line: "+ request.getRequestLine());
			logger.info("The request URI for the clustermap: "+ requestClustermap.getURI().toString());
			logger.info("The request line for the clustermap: "+ requestClustermap.getRequestLine());
			
			boolean Success = false;
			try {
				HttpResponse response = nedrexService.send(request);
				boolean Failed = false;
				  
				  // we're letting it build for t*5 seconds
				double n = 120;
				for (int t=0; t<120; t++) {
					taskMonitor.setProgress(0.1+ t* (1.0-0.1)/n);
					String  responseText = "";
					BufferedReader rd = new BufferedReader (new InputStreamReader(response.getEntity().getContent()));
					String line = "";
					while ((line = rd.readLine()) != null) {
						System.out.println(line);
						logger.info("The response entity of the status: " +line);
						if (line.contains("completed"))
							Success=true;
							responseText = line;
						if (line.contains("failed")) {
							Failed=true;
						}
					}
					if(Success) {
						System.out.println("Yeay success!!!");
						logger.info("Yeaaay successful! This is the response: " + response.getParams());
						logger.info("The status line of the response:" + response.getStatusLine());
						
						JSONParser parser = new JSONParser();
						//logger.info("This is the line from the response: " + responseText );
						JSONObject json = (JSONObject) parser.parse(responseText);
						//logger.info("The JSONObject is: " + json);
						logger.info("The result values of the response json onbject: " + json.get("result"));
						JSONObject json2 = (JSONObject) json.get("result");
						patientgroups = new HashMap<>();
						logger.info("Result patients2: " + ((JSONObject) json.get("result")).get("patients2"));
						JSONArray ar = (JSONArray) ((JSONObject) json.get("result")).get("patients2");
						Set<String> tmp = new HashSet<>();
						if (ar != null) {
							for (Object o : ar) {
								tmp.add(o.toString());
							}
						}
						logger.info("patients2 size: " + tmp.size());
						patientgroups.put("patients2", tmp);
						logger.info("Result patients1: " + ((JSONObject) json.get("result")).get("patients1"));
						JSONArray ar2 = (JSONArray) ((JSONObject) json.get("result")).get("patients1");
						Set<String> tmp2 = new HashSet<>();
						if (ar2 != null) {
							for (Object o : ar2) {
								tmp2.add(o.toString());
							}
						}
						logger.info("patients1 size: " + tmp2.size());
						patientgroups.put("patients1", tmp2);
						//logger.info("The edges value of the result json object: " + json2.get("edges"));
						JSONArray jarrEdges = (JSONArray) json2.get("edges");
						JSONArray jarrGenes1 = (JSONArray) json2.get("genes1");
						JSONArray jarrGenes2 = (JSONArray) json2.get("genes2");
						
						//logger.info("Here's the list of edges: " + jarr);
						List<List<String>> edges = new ArrayList<List<String>> ();
						Set<String> nodes = new HashSet<String> ();
						Set<String> genes1 = new HashSet<String>();
						Set<String> genes2 = new HashSet<String>();
						Map<String, Double> genesMap1 = new HashMap <String, Double>();
						Map<String, Double> genesMap2 = new HashMap <String, Double>();
						
						for (Object e: jarrEdges) {
							List<String> nn = (ArrayList<String>)e;
							edges.add(nn);
							nodes.add(nn.get(0));
							nodes.add(nn.get(1));
							logger.info(e.toString() + " - and the ndoes: " + nn.get(0) + " and " + nn.get(1) );
						}
						
						for (Object gene1: jarrGenes1) {
							 JSONObject gobj1 = (JSONObject) gene1;
							 String g1 = (String) gobj1.get("gene");
							 Double mde = (Double) gobj1.get("mean diff expression");
							 genes1.add(g1);
							 genesMap1.put(g1, mde);
						}
						
						for (Object gene2: jarrGenes2) {
							 JSONObject gobj2 = (JSONObject) gene2;
							 String g2 = (String) gobj2.get("gene");
							 Double mde = (Double) gobj2.get("mean diff expression");
							 genes2.add(g2);
							 genesMap2.put(g2, mde);
						}
						
						logger.info("The gene set 1: " + genes1);
						logger.info("The gene set 2: " + genes2);
						
						DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
						taskmanager.execute(new TaskIterator(new BiConCreateNetTask(app, nodes, edges, genesMap1, genesMap2, newNetName)));

						HttpURLConnection con = (HttpURLConnection)requestClustermap.getURI().toURL().openConnection();
						this.nedrexService.setCredentials(con);
						BufferedImage myPicture = ImageIO.read(con.getInputStream());
						ImageIcon icon = new ImageIcon(myPicture);
						setHeatmap(icon.getImage());
						resultPanel.activateFromBicon(this);
						
						break;
					}
					if (Failed) {
						logger.info("The build has failed!");
						showFailed();
						break;
					}
					response = nedrexService.send(request);
					try {
						logger.info(String.format("Waiting for build to complete, sleeping for %d seconds...", sleep_time));
						Thread.sleep(sleep_time*1000);
					} catch (InterruptedException e0) {
						// TODO Auto-generated catch block
						e0.printStackTrace();
					}

				}
				if (!Success & !Failed) {
					logger.info("The run is taking very long (more than 10 mins), please try again in 15 mins!");
					showWarningTime();
					taskMonitor.showMessage(Level.WARN, "The computation is taking very long! It continues running in the backend, to get the results please try again using the same parameters and input for the algorithm in 15 mins!");
//					JOptionPane.showMessageDialog(null, "The run is taking very long (more than 10 mins), please try again in 15 mins!", "Run-time out", 3);
				}
				   
				  
			} catch (ClientProtocolException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
		}
		else if (failedSubmit) {
			logger.info("The request is failed!");
			showFailed();
		}
		
		
	}

	public BoundedInteger getLgMin() {
		return lgMin;
	}

	public BoundedInteger getLgMax() {
		return lgMax;
	}

	public File getInputFile() {
		return inputFile;
	}

	public Image getHeatmap() {
		return heatmap;
	}

	public void setHeatmap(Image heatmap) {
		this.heatmap = heatmap;
	}

	public HashMap<String, Set<String>> getPatientgroups() {
		return patientgroups;
	}
}

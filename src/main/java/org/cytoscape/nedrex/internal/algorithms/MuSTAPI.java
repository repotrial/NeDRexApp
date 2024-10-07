package org.cytoscape.nedrex.internal.algorithms;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.nedrex.internal.NeDRexService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
/**
 * NeDRex App
 * @author Sepideh Sadegh
 * @author Andreas Maier
 */
public class MuSTAPI {
	CyNetwork network;
	Set<CyNode> seeds;
	Integer treeNumber;
	Integer iterNumber;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	Set<String> seeds_in_network = new HashSet<String>();
	Set<List<String>> edges = new HashSet<List<String>> ();
	Set<String> mustNodes = new HashSet<String> ();
	Map<String, Integer> nodeParticipationMap = new HashMap <String, Integer>();
	Map<List<String>, Integer> edgeParticipationMap = new HashMap <List<String>, Integer>();
	Boolean Success;
	private NeDRexService nedrexService;
	public MuSTAPI(NeDRexService nedrexService, CyNetwork network, Set<CyNode> seeds, Integer treeNumber, Integer iterNumber) throws URISyntaxException, ParseException {
		this.network = network;
		this.seeds = seeds;
		this.treeNumber = treeNumber;
		this.iterNumber = iterNumber;
		this.nedrexService = nedrexService;
		runAlgorithm();
	}



	private void runAlgorithm() throws URISyntaxException, ParseException {
				
		String submit_url = this.nedrexService.API_LINK + "must/submit";
		String status_url = this.nedrexService.API_LINK + "must/status";
		System.out.println("The submit url: " + submit_url);
		logger.info("The submit url: " + submit_url);
		JSONObject payload = new JSONObject();
		int sleep_time = 3; //in seconds
		List<String> geneNames = new ArrayList<String>();
			
		for (CyNode n: seeds) {
			geneNames.add(network.getRow(n).get(CyNetwork.NAME, String.class).replaceFirst("entrez.", ""));
		}

		payload.put("seeds", geneNames);
		payload.put("multiple", true);
		payload.put("maxit", iterNumber);
		payload.put("network", "DEFAULT");
		payload.put("hubpenalty", 0.0);
		payload.put("trees", treeNumber);

		
		HttpPost post = new HttpPost(submit_url);
		post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
		System.out.println("The payload: " + payload.toString());
		logger.info("The payload: " + payload.toString());
		String uidd = new String();
		Boolean failedSubmit = false;
		try {
			HttpResponse response = nedrexService.send(post);
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
		if (!failedSubmit) {
			String uid = uidd.replace("\"", "");
			HttpGet request = new HttpGet(status_url);
			URI uri = new URIBuilder(request.getURI()).addParameter("uid", uid).build();
			((HttpRequestBase) request).setURI(uri);
			
//			logger.info("The URI: "+ uri);
			logger.info("The uid: " + uid);		
//			logger.info("The request URI: "+request.getURI().toString());
//			logger.info("The request line: "+ request.getRequestLine());
			
//			boolean Success = false;
			Success = false;
			try {
				HttpResponse response = nedrexService.send(request);
				boolean Failed = false;				  
				// we're letting it to run for t*3 seconds
				for (int t=0; t<200; t++) {
					System.out.println("polling...");
					String  responseText = "";
					BufferedReader rd = new BufferedReader (new InputStreamReader(response.getEntity().getContent()));
					String line = "";
					while ((line = rd.readLine()) != null) {
						System.out.println(line);
//						logger.info("The response entity of the status: " +line);
						if (line.contains("completed"))
							Success=true;
							responseText = line;
						if (line.contains("failed")) {
							Failed=true;
						}
					}
					if(Success) {
						logger.info("The run is successfully completed! This is the response: " + response.getParams());
//						logger.info("The status line of the response:" + response.getStatusLine());
//						logger.info("This is the response text of the successful: " + responseText);
						
						JSONParser parser = new JSONParser();
						JSONObject json = (JSONObject) parser.parse(responseText);
						logger.info("The result values of the response json onbject: " + json.get("results"));
						JSONObject json2 = (JSONObject) json.get("results");
						
						JSONArray jarrEdges = (JSONArray) json2.get("edges");
						JSONArray jarrMustNodes = (JSONArray) json2.get("nodes");
						JSONArray jarrSeeds = (JSONArray) json2.get("seeds_in_network");
						
						for (Object e: jarrEdges) {
							JSONObject mstnobj = (JSONObject) e;
							List<String> nn = new ArrayList<String>();
							String participEdge = (String) mstnobj.get("participation_number");
							Integer partEdgeNumb = Integer.parseInt(participEdge);							
							String srcNode = (String) mstnobj.get("srcNode");
							this.mustNodes.add(srcNode);
							String targetNode = (String) mstnobj.get("targetNode");
							this.mustNodes.add(targetNode);
							nn.add(srcNode);
							nn.add(targetNode);
							this.edges.add(nn);
							this.edgeParticipationMap.put(nn, partEdgeNumb);					
//							logger.info(e.toString() + " - and the ndoes: " + nn.get(0) + " and " + nn.get(1) );
						}
						
						for (Object mustNode: jarrMustNodes) {
							 JSONObject mstnobj = (JSONObject) mustNode;
							 String mstnName = (String) mstnobj.get("node");
							 String participNode = (String) mstnobj.get("participation_number");
							 Integer partNodeNumb = Integer.parseInt(participNode);
							 this.mustNodes.add(mstnName);
							 this.nodeParticipationMap.put(mstnName, partNodeNumb);

						}
						
						for (Object seedObj: jarrSeeds) {
							 String seed = (String) seedObj;
							 this.mustNodes.add(seed);
							 this.seeds_in_network.add(seed);
						}
						logger.info("The must result via API, edges: " + edges);
						logger.info("The must result via API, nodes: " + mustNodes);
						logger.info("The must result via API, participation nodes: " + nodeParticipationMap);
						logger.info("The must result via API, participation edges: " + edgeParticipationMap);
						
//						DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
//						taskmanager.execute(new TaskIterator(new MuSTapiCreateNetTask(app, mustNodes, edges, nodeParticipationMap, edgeParticipationMap, seeds_in_network, ggType, newNetName)));
						break;
					}
					if (Failed) {
						logger.info("The run is failed!");
						break;
					}
					response = nedrexService.send(request);
					try {
						logger.info(String.format("Waiting for run to complete, sleeping for %d seconds...", sleep_time));
						Thread.sleep(sleep_time*1000);
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

	}
	
	public Set<String> getMuSTNodes(){
		return this.mustNodes;
	}
	
	public Set<List<String>> getMuSTEdges(){
		return this.edges;
	}
	
	public Set<String> getSeedsInNetwork(){
		return this.seeds_in_network;
	}
	
	public Map<String, Integer> getNodesParticipation(){
		return this.nodeParticipationMap;
	}
	
	public Map<List<String>, Integer> getEdgesParticipation(){
		return this.edgeParticipationMap;
	}
	
	public Boolean getSuccess() {
		return this.Success;
	}
	

}

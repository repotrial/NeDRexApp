package org.cytoscape.nedrex.internal.tasks;

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
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.nedrex.internal.NeDRexService;
import org.cytoscape.nedrex.internal.NodeType;
import org.cytoscape.nedrex.internal.RepoApplication;
import org.cytoscape.nedrex.internal.RepoResultPanel;
import org.cytoscape.nedrex.internal.utils.FilterType;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedInteger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NeDRex App
 * @author Sepideh Sadegh
 * @author Andreas Maier
 */
public class JointValidationTask extends AbstractTask{
	private RepoApplication app;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private RepoResultPanel resultPanel;
	private String pvalue;
	private String pvalue_prec;
	
	@ProvidesTitle
	public String getTitle() {return "Set Parameters for Joint Validation Algorithm";}
	
	@Tunable(description="Number of permutations", groups="Algorithm settings",
			params="slider=true",
			tooltip="The number of random lists of drugs to build background distribution",
			gravity = 2.0)
    public BoundedInteger permutations = new BoundedInteger(1000, 1000, 10000, false, false);
	
	@Tunable(description="Include only approved drugs", groups="Algorithm settings",
			tooltip="If selected, only approved (registered) drugs should be considered for validation",
			gravity = 2.5)
    public Boolean only_approved = false;
	
	@Tunable(description="Read drugs indicated for disease from a file", groups="Reference drugs",
			tooltip="If selected, drugs indicated for treatment of disease will be read from a file. Otherwise, selected drugs in the current network will be taken as indicated drugs.",
			gravity = 3.0)
    public Boolean trueDrugFile = false;
	
	@Tunable(description="Input file for drugs indicated for disease:" ,  params= "input=true", groups="Reference drugs",
			dependsOn="trueDrugFile=true",
			tooltip="Input file containing list of indicated drugs for treatment of disease, one drug per line. The file should be a tab-separated file and the first column will be taken as drugs. Drugs with DrugBank IDs are acceptable as input.",
			gravity = 3.5)
	public File inputTDFile = new File(System.getProperty("user.home"));
	
	@Tunable(description="Read drugs to be validated from a file", groups="Drugs for validation",
			tooltip="If selected, a list of drugs to-be-validated will be read from a file. Otherwise, all drugs in the current network will be taken as drugs to-be-validated.",
			gravity = 3.0)
    public Boolean resultDrugFile = false;
	
	@Tunable(description="Input file for drugs to be validated:" ,  params= "input=true", groups="Drugs for validation",
			dependsOn="resultDrugFile=true",
			tooltip="Input file containing list of drugs with their ranks, one drug per line. The file should be a tab-separated file, the first column will be taken as drugs."
					+ "DrugBank IDs are acceptable.",
			gravity = 3.5)
	public File inputRDFile = new File(System.getProperty("user.home"));
	
	@Tunable(description="Read disease module from a file", groups="Disease module",
			tooltip="If selected, genes/proteins of the disease module, that was returned in the previous step of drug repurposing,  will be read from a file. Otherwise, all the genes/protein in the current network will be taken as disease module.",
			gravity = 3.0)
    public Boolean moduleFile = false;
	
	@Tunable(description="Input file for disease module:" ,  params= "input=true", groups="Disease module",
			dependsOn="moduleFile=true",
			tooltip="Input file containing list of module's genes/proteins, one entity per line. The file should be a tab-separated file, the first column will be taken as module's gene/protein."
					+ "Entrez IDs for genes and Uniprot AC for proteins are acceptable.",
			gravity = 4.0)
	public File inputModuleFile = new File(System.getProperty("user.home"));
	
	@Tunable(description="Description of the validation run", groups="Validation result", 
	         tooltip="Write a description of the validation job you are running to be shown in the result panel. For example, name of the disease. This helps tracking your analyses",
	         gravity = 5.0)
	public String job_description = new String();
	
	public JointValidationTask(RepoApplication app, RepoResultPanel resultPanel) {
		this.app = app;
		this.setNedrexService(app.getNedrexService());
		this.resultPanel = resultPanel;
	}
	
	protected void showWarningTime() {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, "The computation is taking very long! It continues running in the backend, to get the results please try again using the same parameters and input for the algorithm in 10 mins!", "Long run-time", JOptionPane.WARNING_MESSAGE);
				}
			}
		);
	}
	
	protected void showFailed() {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, "The request to run the validation is failed! Please check your inputs.", "Error", JOptionPane.ERROR_MESSAGE);
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
		CyNetwork network = app.getCurrentNetwork();
		String submit_url = this.nedrexService.API_LINK + "validation/joint";
		String status_url = this.nedrexService.API_LINK + "validation/status";
		
		JSONObject payload = new JSONObject();
		List<String> true_drugs = new ArrayList<>();
		List<String> result_drugs = new ArrayList<>();
		List<String> module_members = new ArrayList<>();
		String module_member_type = "";
		int sleep_time = 2; //in seconds
		
		if (!trueDrugFile) {
			List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network,CyNetwork.SELECTED,true);
			for (CyNode n: selectedNodes) {
				if (network.getRow(n).get("type", String.class).equals(NodeType.Drug.toString())) {
					true_drugs.add(network.getRow(n).get(CyNetwork.NAME, String.class));
				}
			}				
		}
		else if (trueDrugFile) {
			String fp = inputTDFile.getPath();
			try {
				BufferedReader br = new BufferedReader(new FileReader(fp));
				String dataRow = br.readLine();//skip header line
				while (dataRow != null){
					String[] data = dataRow.split("\t");
					if(!true_drugs.contains(data[0])) {
						true_drugs.add(data[0]);
					}						
					dataRow = br.readLine();
				}
			}			
			catch (IOException e) {e.printStackTrace();}
		}
		logger.info("The list of true drugs: " + true_drugs);
		logger.info("Length of true drugs list: " + true_drugs.size());
		
		if (!resultDrugFile) {
			Set<CyNode> result_drugs_nodes = FilterType.nodesOfType(network, NodeType.Drug);
			for (CyNode n: result_drugs_nodes) {
				result_drugs.add(network.getRow(n).get(CyNetwork.NAME, String.class));
			}
		}
		else if (resultDrugFile) {
			String fp = inputRDFile.getPath();
			try {
				BufferedReader br = new BufferedReader(new FileReader(fp));
				String dataRow = br.readLine();//skip header line
				while (dataRow != null){
					String[] data = dataRow.split("\t");
					result_drugs.add(data[0]);
					dataRow = br.readLine();
				}
			}			
			catch (IOException e) {e.printStackTrace();}
		}
		logger.info("The list of result drugs: " + result_drugs);
		logger.info("Length of result drugs list: " + result_drugs.size());
		
		if (!moduleFile) {
			Set<String> nodeTypes = new HashSet<String>(network.getDefaultNodeTable().getColumn("type").getValues(String.class));
			Set<CyNode> module_nodes = new HashSet<CyNode>();
			if (nodeTypes.contains(NodeType.Gene.toString())) {
				module_nodes = FilterType.nodesOfType(network, NodeType.Gene);
				module_member_type = "gene";
			}
			else if (nodeTypes.contains(NodeType.Protein.toString())) {
				module_nodes = FilterType.nodesOfType(network, NodeType.Protein);
				module_member_type = "protein";
			}		
			for (CyNode n: module_nodes) {
				module_members.add(network.getRow(n).get(CyNetwork.NAME, String.class));
			}					
		}
		else if (moduleFile) {
			String fp = inputModuleFile.getPath();
			try {
				BufferedReader br = new BufferedReader(new FileReader(fp));
				String dataRow = br.readLine();//skip header line
				while (dataRow != null){
					String[] data = dataRow.split("\t");
					if(!module_members.contains(data[0])) {
						if (data[0].contains("entrez") || data[0].matches("[0-9]+")) {
							module_member_type = "gene";
							}
						else if (data[0].contains("uniprot")) {
							module_member_type = "protein";
							}
						module_members.add(data[0]);
					}						
					dataRow = br.readLine();
				}
			}			
			catch (IOException e) {e.printStackTrace();}
		}
		logger.info("The list of module nodes: " + module_members);
		logger.info("Size of module: " + module_members.size());
		
		payload.put("test_drugs", result_drugs);
		payload.put("true_drugs", true_drugs);
		payload.put("permutations", permutations.getValue());
		payload.put("only_approved_drugs", only_approved);
		payload.put("module_members", module_members);
		payload.put("module_member_type", module_member_type);
	    
		logger.info("The post JSON converted to string: " + payload.toString());
		
		HttpPost post = new HttpPost(submit_url);
		post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
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
				System.out.println(line);
				logger.info("The uri of the response to the post: "+line + "\n");
				uidd = line;
				if (line.contains("Internal Server Error")) {
					failedSubmit=true;
				}
//				if (line.length() < 3) {
//					failedSubmit = true;
//				}			
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
			HttpGet request = new HttpGet(status_url);
			URI uri = new URIBuilder(request.getURI()).addParameter("uid", uid).build();
			((HttpRequestBase) request).setURI(uri);
			
//			logger.info("The URI: "+ uri);
			logger.info("The uid: " + uid);		
//			logger.info("The request URI: "+request.getURI().toString());
//			logger.info("The request line: "+ request.getRequestLine());
			
			boolean Success = false;
			try {
				HttpResponse response = nedrexService.send(request);
//				boolean Success = false;
				boolean Failed = false;
				  
				  // we're letting it to run for t*2 seconds
				double n = 200;
				for (int t=0; t<200; t++) {
					taskMonitor.setProgress(0.1+ t* (1.0-0.1)/n);
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

						JSONParser parser = new JSONParser();
						JSONObject json = (JSONObject) parser.parse(responseText);
						logger.info("The p-val of the response json onbject: " + json.get("emprirical p-value"));
						logger.info("The p-val (precision-based) of the response json onbject: " + json.get("empircal (precision-based) p-value"));
						pvalue = String.valueOf(json.get("emprirical p-value"));
						pvalue_prec = String.valueOf(json.get("empircal (precision-based) p-value"));
						
						resultPanel.activateFromJointValidation(this);
						break;
					}
					if (Failed) {
						logger.info("The run is failed!");
						showFailed();
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
				
				if (!Success & !Failed) {
					logger.info("The run is taking very long (more than 10 mins), please try again in 15 mins!");
					showWarningTime();
					taskMonitor.showMessage(Level.WARN, "The computation is taking very long! It continues running in the backend, to get the results please try again using the same parameters and input for the algorithm in 15 mins!");
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
	
	public String getPVal() {
		return pvalue;
	}
	
	public String getPValPrec() {
		return pvalue_prec;
	}
	
	public Integer getPermutations() {
		return permutations.getValue();
	}
	
	public String getApproved() {
		String approved;
		if (only_approved) {
			approved = "only approved";
		}
		else {
			approved = "all";
		}		
		return approved;
	}
	
	public String getDescription() {
		return job_description;
	}

}

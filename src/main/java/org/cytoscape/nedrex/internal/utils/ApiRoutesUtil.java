package org.cytoscape.nedrex.internal.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.nedrex.internal.ModelUtil;
import org.cytoscape.nedrex.internal.NeDRexService;
import org.cytoscape.nedrex.internal.io.HttpGetWithEntity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * NeDRex App
 *
 * @author Sepideh Sadegh
 * @author Andreas Maier
 */
@Component
public class ApiRoutesUtil {

    private NeDRexService nedrexService;

    @Reference
    public void setNedrexService(NeDRexService nedrexService) {
        this.nedrexService = nedrexService;
    }

    public void unsetNedrexService(NeDRexService nedrexService) {
        if (this.nedrexService == nedrexService)
            this.nedrexService = null;
    }

    public ApiRoutesUtil(NeDRexService nedrexService){
        this.setNedrexService(nedrexService);
        descendant_url = this.nedrexService.API_LINK + "disorder/descendants";
        children_url = this.nedrexService.API_LINK + "disorder/children";
        encodedProts_url = this.nedrexService.API_LINK + "relations/get_encoded_proteins";
        drugsTargetingGenes_url = this.nedrexService.API_LINK + "relations/get_drugs_targeting_gene_products";
        drugsTargetingProts_url = this.nedrexService.API_LINK + "relations/get_drugs_targeting_proteins";
    }

    public String descendant_url ;
    public String children_url;
    public String encodedProts_url;
    public String drugsTargetingGenes_url;
    public String drugsTargetingProts_url;

    /**
     * A method which returns all the children of input disorder CyNodes via API (disorder route for children). It also updates the map of children Name with intial diseases being the keys
     *
     * @param network
     * @param disorders       Set of CyNodes containing disorders
     * @param childrenNameMap Empty map of children Name to be filled with input diseases as keys
     * @throws URISyntaxException
     * @throws ParseException
     * @return Set of diseases children names in mondoID
     */
    public Set<String> getDisordersChildren(CyNetwork network, Set<CyNode> disorders, Map<CyNode, Set<String>> childrenNameMap) throws URISyntaxException, ParseException {
        Set<String> disordersChildren = new HashSet<String>();
        HttpGet request = new HttpGet(this.children_url);
        for (CyNode n : disorders) {
            String diseaseName = network.getRow(n).get(CyNetwork.NAME, String.class);
            URI uri = new URIBuilder(request.getURI()).addParameter("q", diseaseName).build();
            ((HttpRequestBase) request).setURI(uri);
        }
        try {
            HttpResponse response = nedrexService.send(request);
            String responseText = "";
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                responseText = line;
            }
            JSONParser parser = new JSONParser();
            if (responseText.length() > 2) { // otherwise there's no children {}
                JSONObject json = (JSONObject) parser.parse(responseText);
                for (CyNode n : disorders) {
                    String diseaseName = network.getRow(n).get(CyNetwork.NAME, String.class);
                    JSONArray jarrChildren = (JSONArray) json.get(diseaseName);
                    childrenNameMap.put(n, new HashSet<String>());
                    if (jarrChildren != null) {
                        for (Object childObj : jarrChildren) {
                            String child = (String) childObj;
                            childrenNameMap.get(n).add(child);
                            disordersChildren.add(child);
                        }
                    }
                }
            }
        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        System.out.println("The childrenMap: " + childrenNameMap);
        return disordersChildren;
    }

    /**
     * A method which returns all the descendants of input disorder CyNodes via API (disorder route for descendants). It also updates the map of descendants Name with intial diseases being the keys
     *
     * @param network
     * @param disorders         Set of CyNodes containing disorders
     * @param descendantNameMap Empty map of descendants Name to be filled with input diseases as keys
     * @return Set of diseases descendants names in mondoID
     * @throws URISyntaxException
     * @throws ParseException
     */
    public Set<String> getDisordersDescendants(CyNetwork network, Set<CyNode> disorders, Map<CyNode, Set<String>> descendantNameMap) throws URISyntaxException, ParseException {
        Set<String> disordersDescendants = new HashSet<String>();
        HttpGet request = new HttpGet(this.descendant_url);
        for (CyNode n : disorders) {
            String diseaseName = network.getRow(n).get(CyNetwork.NAME, String.class);
            URI uri = new URIBuilder(request.getURI()).addParameter("q", diseaseName).build();
            ((HttpRequestBase) request).setURI(uri);
        }
        try {
            HttpResponse response = nedrexService.send(request);
            String responseText = "";
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                responseText = line;
            }
            JSONParser parser = new JSONParser();
            if (responseText.length() > 2) { // otherwise there's no descendants {}
                JSONObject json = (JSONObject) parser.parse(responseText);
                for (CyNode n : disorders) {
                    String diseaseName = network.getRow(n).get(CyNetwork.NAME, String.class);
                    JSONArray jarrDescendants = (JSONArray) json.get(diseaseName);
                    descendantNameMap.put(n, new HashSet<String>());
                    if (jarrDescendants != null) {
                        for (Object descendObj : jarrDescendants) {
                            String descend = (String) descendObj;
                            descendantNameMap.get(n).add(descend);
                            disordersDescendants.add(descend);
                        }
                    }

                }
            }
        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        System.out.println("The descendantsMap: " + descendantNameMap);
        return disordersDescendants;

    }

    /**
     * A method which returns a map where input genes (CyNodes) are the keys and the corresponding encoded proteins (names) are the values,
     * by using Relation route from API
     *
     * @param network
     * @param genes
     * @return
     * @throws URISyntaxException
     * @throws ParseException
     */
    public Map<CyNode, Set<String>> getEncodedProteins(CyNetwork network, Set<CyNode> genes) throws URISyntaxException, ParseException {
        Map<CyNode, Set<String>> geneProteinsMap = new HashMap<CyNode, Set<String>>();
        JSONObject payload = new JSONObject();
        Map<String, CyNode> geneNamesMap = ModelUtil.getNodeNameMap(network, genes);
        System.out.println("This is the geneNamesMap: " + geneNamesMap);
        payload.put("nodes", new ArrayList<String>(geneNamesMap.keySet()));

        HttpPost request = new HttpPost();
        request.setURI(new URI(this.encodedProts_url));
        request.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
        try {
            HttpResponse response = nedrexService.send(request);
            String responseText = "";
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                responseText = line;
            }
            JSONParser parser = new JSONParser();
            System.out.println("This is the responseText for encoded prots: " + responseText);
            JSONObject json = (JSONObject) parser.parse(responseText);
            for (String gName : geneNamesMap.keySet()) {
                JSONArray jarrEncodedProts = (JSONArray) json.get(gName.replaceFirst("entrez.", ""));
                geneProteinsMap.put(geneNamesMap.get(gName), new HashSet<>());
                if (jarrEncodedProts != null) {
                    for (Object protObj : jarrEncodedProts) {
                        String prot = (String) protObj;
                        geneProteinsMap.get(geneNamesMap.get(gName)).add("uniprot." + prot);
                    }
                }
            }

        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        return geneProteinsMap;
    }

    /**
     * A method which returns a map where input genes (CyNodes) are the keys and the drugs targeting them (names) are the values,
     * by using Relation route from API. It also updates the empty input set of drugs to contain returned targeting drugs.
     *
     * @param network
     * @param genes
     * @return
     * @throws URISyntaxException
     * @throws ParseException
     */
    public Map<CyNode, Set<String>> getDrugsTargetingGenes(CyNetwork network, Set<CyNode> genes, Set<String> targeting_drugs) throws URISyntaxException, ParseException {
        Map<CyNode, Set<String>> geneDrugsMap = new HashMap<CyNode, Set<String>>();
        JSONObject payload = new JSONObject();
        Map<String, CyNode> geneNamesMap = ModelUtil.getNodeNameMap(network, genes);
        System.out.println("This is the geneNamesMap: " + geneNamesMap);
        payload.put("nodes", new ArrayList<>(geneNamesMap.keySet()));

        HttpPost request = new HttpPost();
        request.setURI(new URI(this.drugsTargetingGenes_url));
        request.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
        try {
            HttpResponse response = nedrexService.send(request);
            String responseText = "";
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                responseText = line;
            }
            JSONParser parser = new JSONParser();
            System.out.println("The responseText for drugs targeting genes: " + responseText);
            JSONObject json = (JSONObject) parser.parse(responseText);
            for (String gName : geneNamesMap.keySet()) {
                JSONArray jarrdrugGene = (JSONArray) json.get(gName.replaceFirst("entrez.", ""));
                geneDrugsMap.put(geneNamesMap.get(gName), new HashSet<String>());
                if (jarrdrugGene != null) {
                    for (Object drugObj : jarrdrugGene) {
                        String dr = (String) drugObj;
                        geneDrugsMap.get(geneNamesMap.get(gName)).add("drugbank." + dr);
                        targeting_drugs.add("drugbank." + dr);
                    }
                }
            }

        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        return geneDrugsMap;
    }

    /**
     * A method which returns a map where input proteins (CyNodes) are the keys and the drugs targeting them (names) are the values,
     * by using Relation route from API. It also updates the empty input set of drugs to contain returned targeting drugs.
     *
     * @param network
     * @param proteins
     * @return
     * @throws URISyntaxException
     * @throws ParseException
     */
    public Map<CyNode, Set<String>> getDrugsTargetingProteins(CyNetwork network, Set<CyNode> proteins, Set<String> targeting_drugs) throws URISyntaxException, ParseException {
        Map<CyNode, Set<String>> proteinDrugsMap = new HashMap<CyNode, Set<String>>();
        JSONObject payload = new JSONObject();
        Map<String, CyNode> protNamesMap = ModelUtil.getNodeNameMap(network, proteins);
        System.out.println("This is the protNamesMap: " + protNamesMap);
        payload.put("nodes", new ArrayList<>(protNamesMap.keySet()));

        HttpPost request = new HttpPost();
        request.setURI(new URI(this.drugsTargetingProts_url));
        request.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
        try {
            HttpResponse response = nedrexService.send(request);
            String responseText = "";
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                responseText = line;
            }
            JSONParser parser = new JSONParser();
            System.out.println("The responseText for drugs targeting proteins: " + responseText);
            JSONObject json = (JSONObject) parser.parse(responseText);
            for (String pName : protNamesMap.keySet()) {
                JSONArray jarrdrugProt = (JSONArray) json.get(pName.replaceFirst("uniprot.", ""));
                proteinDrugsMap.put(protNamesMap.get(pName), new HashSet<String>());
                if (jarrdrugProt != null) {
                    for (Object drugObj : jarrdrugProt) {
                        String dr = (String) drugObj;
                        proteinDrugsMap.get(protNamesMap.get(pName)).add("drugbank." + dr);
                        targeting_drugs.add("drugbank." + dr);
                    }
                }
            }

        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        return proteinDrugsMap;
    }

    /**
     * A method which filters the input list of drugs and returns a list of only approved ones
     *
     * @param drug_ids
     * @return
     * @throws ParseException
     * @throws URISyntaxException
     */
    public List<String> getApprovedDrugsList(Set<String> drug_ids) throws ParseException, URISyntaxException {
        List<String> approved_drugs = new ArrayList<String>();
        JSONObject payload = new JSONObject();
        List<String> attributes = new ArrayList<String>();
        String attrDrugGroups = "drugGroups";
        attributes.add(attrDrugGroups);
        payload.put("attributes", attributes);
        payload.put("node_ids", new ArrayList<String>(drug_ids));

        String url = String.format(this.nedrexService.API_LINK + "%s/attributes/json", "drug");
        HttpGetWithEntity e = new HttpGetWithEntity();
        e.setURI(new URI(url));
        e.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

        try {
            HttpResponse response = nedrexService.send(e);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            String responseText = "";
            while ((line = rd.readLine()) != null) {
                responseText = line;
            }

            JSONParser parser = new JSONParser();
            JSONArray jarrDrugs = (JSONArray) parser.parse(responseText);
            for (Object drug : jarrDrugs) {
                JSONObject drugobj = (JSONObject) drug;
                String dr = (String) drugobj.get("primaryDomainId");
                JSONArray drugGroups = (JSONArray) drugobj.get(attrDrugGroups);
                List<String> groups = new ArrayList<String>();
                for (Object groupObj : drugGroups) {
                    String group = (String) groupObj;
                    groups.add(group);
                }
                if (groups.contains("approved")) {
                    approved_drugs.add(dr);
                }
            }
        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        return approved_drugs;

    }

}

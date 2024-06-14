package org.cytoscape.nedrex.internal.ui;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.nedrex.internal.InteractionType;
import org.cytoscape.nedrex.internal.RepoApplication;

/**
 * NeDRex App
 * @author Sepideh Sadegh
 * @author Andreas Maier
 */
public class SearchOptionPanel extends JPanel{
	
	private RepoApplication app;
	
	JPanel edgeOptions;
	JCheckBox edgeTypeGD, edgeTypeGP, edgeTypePP, edgeTypeDrP, edgeTypeDrDis, edgeTypeDD, edgeTypePwP;
	JCheckBox agreed, approved, experimental, illicit, investig, nutraceu, vetApprov, withdrawn;
	JComboBox<String> taxid;
	JCheckBox selfLoop;
	JCheckBox iidEvid;
	JCheckBox omim;
	JCheckBox reviewedProteins;
	JCheckBox disgenet;
	JCheckBox concise;
	JSlider disgenetScore;
	JTextField scoreField;
	JTextField newNetworkName;
	
	public SearchOptionPanel(RepoApplication app) {
		this.app = app;
		
		initOptions();
	}
	
	private void initOptions() {
		setPreferredSize(new Dimension(725,200));
		EasyGBC c = new EasyGBC();
		
		// Edge choices to add
		edgeOptions = createEdgeOptions();
		add(edgeOptions, c.down().expandBoth().insets(5,5,0,5));
		
		JPanel additionalPPOptions = createAdditionalPPOptions();
		add(additionalPPOptions, c.down().expandBoth().insets(7,5,0,5));

		JPanel additionalPOptions = createAdditionalPOptions();
		add(additionalPOptions, c.down().expandBoth().insets(7,5,0,5));
		
		JPanel additionalGDOptions = createAdditionalGDOptions();
		add(additionalGDOptions, c.down().expandBoth().insets(7,5,0,5));
		
		JPanel additionalDrugOptions = createAdditionalDrugOptions();
		add(additionalDrugOptions, c.down().expandBoth().insets(7,5,0,5));
		
		JPanel newNetworkOptions = createnewNetOptions();
		add(newNetworkOptions, c.down().expandBoth().insets(7,5,0,5));
		
		JPanel licenseAgreement = createLicenseAgreement();
		add(licenseAgreement, c.down().expandBoth().insets(7,5,0,5));

	}
	
	JPanel createEdgeOptions() {
		JPanel edgeOptionPanel = new JPanel(new GridBagLayout());
		edgeOptionPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();
		JLabel optionsLabel = new JLabel("<html><b>Association Options:</b></html>");
		c.anchor("west").insets(0,5,0,5);
		edgeOptionPanel.add(optionsLabel, c);
		
		c.right().noExpand().insets(0,10,0,5);		
		edgeTypeGD = new JCheckBox("Gene-Disorder", true);
		edgeOptionPanel.add(edgeTypeGD, c);
		edgeTypeGD.setToolTipText("Relationships between disorders and genes associated to them");
		
		c.right().noExpand().insets(0,10,0,5);
		edgeTypeGP = new JCheckBox("Gene-Protein", false);
		edgeOptionPanel.add(edgeTypeGP, c);
		edgeTypeGP.setToolTipText("Relationships between genes and proteins encoded by them");
		
		c.right().noExpand().insets(0,10,0,5);
		edgeTypePP = new JCheckBox("Protein-Protein", true);
		edgeOptionPanel.add(edgeTypePP, c);
		edgeTypePP.setToolTipText("Protein protein interactions");
		
//		c.right().noExpand().insets(0,10,0,5);
//		edgeTypeDrP = new JCheckBox("Drug-Protein", true);
//		edgeOptionPanel.add(edgeTypeDrP, c);
//		edgeTypeDrP.setToolTipText("Relationships between drugs and proteins targeted by them");
		
		c.right().expandHoriz().insets(0,10,0,5);
		edgeTypeDD = new JCheckBox("Disorder-Disorder (hierarchy)", false);
		edgeOptionPanel.add(edgeTypeDD, c);
		edgeTypeDD.setToolTipText("Relationships representing the disorder hierarchy in MONDO");
		
		c.down().expandHoriz().insets(0,10,0,5);
		JLabel dummy = new JLabel("");
		edgeOptionPanel.add(dummy, c);
		
		c.right().expandHoriz().insets(0,10,0,5);
		edgeTypePwP = new JCheckBox("Pathway-Protein", false);
		edgeOptionPanel.add(edgeTypePwP, c);
		edgeTypePwP.setToolTipText("Relationships between proteins and pathways they participate in");
		
		c.right().expandHoriz().insets(0,10,0,5);
		edgeTypeDrDis = new JCheckBox("Drug-Disorder", true);
		edgeOptionPanel.add(edgeTypeDrDis, c);
		edgeTypeDrDis.setToolTipText("Relationships between disorders and the drugs that are indicated for the treatment of disorders");
		
//		c.right().expandHoriz().insets(0,10,0,5);
//		edgeTypeDD = new JCheckBox("Disorder-Disorder (hierarchy)", false);
//		edgeOptionPanel.add(edgeTypeDD, c);
//		edgeTypeDD.setToolTipText("Relationships representing the disorder hierarchy in MONDO");
		
		c.right().expandHoriz().insets(0,10,0,5);
		edgeTypeDrP = new JCheckBox("Drug-Protein", true);
		edgeOptionPanel.add(edgeTypeDrP, c);
		edgeTypeDrP.setToolTipText("Relationships between drugs and proteins targeted by them");
		
		return edgeOptionPanel;
	}
	
	JPanel createAdditionalPPOptions() {
		JPanel additionalOptionPanel = new JPanel(new GridBagLayout());
		additionalOptionPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();
		JLabel optionsLabel = new JLabel("<html><b>Protein-Protein Options:</b></html>");
		c.anchor("west").insets(0,5,0,5);
		additionalOptionPanel.add(optionsLabel, c);

		c.right().noExpand().insets(0,10,0,5);		
		selfLoop = new JCheckBox("self-loop", false);
		selfLoop.setToolTipText("Check if you want to include self-loop PPIs. By default, they are not added.");
		additionalOptionPanel.add(selfLoop, c);
		
		c.right().expandHoriz().insets(0,10,0,5);
		iidEvid = new JCheckBox("All PPI evidence", false);
		iidEvid.setToolTipText("Select if you want to include all PPI evidence types (experimentally detected, orthologous, computationally predicted). Default is experimental!");
		additionalOptionPanel.add(iidEvid, c);
				
		//*** listener added
		edgeTypePP.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED){
                	selfLoop.setEnabled(true);
                	iidEvid.setEnabled(true);
                }
                else if(e.getStateChange() == ItemEvent.DESELECTED){
                	selfLoop.setEnabled(false);
                	iidEvid.setEnabled(false);
                }
                validate();
                repaint();
            }
        });
		
		return additionalOptionPanel;
	}

	JPanel createAdditionalPOptions() {
		JPanel additionalOptionPanel = new JPanel(new GridBagLayout());
		additionalOptionPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();
		JLabel optionsLabel = new JLabel("<html><b>Protein Options:</b></html>");
		c.anchor("west").insets(0,5,0,5);
		additionalOptionPanel.add(optionsLabel, c);

		c.right().noExpand().insets(0,10,0,5);
		reviewedProteins = new JCheckBox("reviewed_proteins", true);
		reviewedProteins.setToolTipText("Uncheck if you want to exclude Proteins unreviewed in UniProt.");
		additionalOptionPanel.add(reviewedProteins, c);
		return additionalOptionPanel;
	}
	
	JPanel createAdditionalGDOptions() {
		JPanel additionalOptionPanel = new JPanel(new GridBagLayout());
		additionalOptionPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();
		JLabel optionsLabel = new JLabel("<html><b>Gene-Disorder Options:</b></html>");
		c.anchor("west").insets(0,5,0,5);
		additionalOptionPanel.add(optionsLabel, c);

		c.right().noExpand().insets(0,10,0,5);		
		omim = new JCheckBox("OMIM associations", true);
		omim.setToolTipText("Check if you want to include disease-gene associations from OMIM database.");
		additionalOptionPanel.add(omim, c);
		
		c.right().noExpand().insets(0,5,0,2);		
		disgenet = new JCheckBox("DisGeNET associations:", true);
		disgenet.setToolTipText("Check if you want to include disease-gene associations from DisGeNET curated database.");
		additionalOptionPanel.add(disgenet, c);
		
		c.right().expandHoriz().insets(0, 5, 0, 2);
		disgenetScore = new JSlider(0, 100, 0);
		disgenetScore.setMajorTickSpacing(25);
		disgenetScore.setMinorTickSpacing(5);
		disgenetScore.setPaintTicks(true);
	    java.util.Hashtable<Integer,JLabel> labelTable = new java.util.Hashtable<Integer,JLabel>();
	    labelTable.put(100, new JLabel("1.0"));
	    labelTable.put(75, new JLabel("0.75"));
	    labelTable.put(50, new JLabel("0.50"));
	    labelTable.put(25, new JLabel("0.25"));
	    labelTable.put(0, new JLabel("0.0"));
	    disgenetScore.setLabelTable(labelTable);
	    disgenetScore.setPaintLabels(true);

	    disgenetScore.setToolTipText("<html>" +
	    		"Cutoff threshold for disease-gene association score from DisGeNET. Default: 0 (gives all assocations)"+
	    		"<br>The DisGeNET score is a measure to rank the gene-disease associations according to their level of evidence. "
	    		+ "<br> The score takes into account the number and type of sources (level of curation, model organisms)," +
	    		"<br>  and the number of publications supporting the association."+
	    		"</html>");
	    
	    additionalOptionPanel.add(disgenetScore, c);
	    
		disgenet.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED){
                	disgenetScore.setEnabled(true);
                }
                else if(e.getStateChange() == ItemEvent.DESELECTED){
                	disgenetScore.setEnabled(false);
                }
                validate();
                repaint();
            }
        });
		
	    c.right().expandHoriz().insets(0, 5, 0, 2);
		scoreField = new JTextField(3);
		additionalOptionPanel.add(scoreField, c);
		
	    disgenetScore.addChangeListener(new ChangeListener() {
	    	@Override
	    	public void stateChanged(ChangeEvent e) {
//	            JSlider slider = (JSlider) e.getSource();
	    		scoreField.setText(String.valueOf((double)disgenetScore.getValue()/100));
	        }
	    });
	    
	    //threshold = (double) disgenetScore.getValue()/100;
		
	  //*** listener added
	    edgeTypeGD.addItemListener(new ItemListener(){
	    	@Override
	    	public void itemStateChanged(ItemEvent e) {
	    		if(e.getStateChange() == ItemEvent.SELECTED){
	    			omim.setEnabled(true);
	    			disgenet.setEnabled(true);
	    			disgenetScore.setEnabled(true);
	    			scoreField.setEnabled(true);
	    		}
	    		else if(e.getStateChange() == ItemEvent.DESELECTED){
	    			omim.setEnabled(false);
	    			disgenet.setEnabled(false);
	    			disgenetScore.setEnabled(false);
	    			scoreField.setEnabled(false);
	    		}
	    		validate();
	    		repaint();
	    	}
	    });
	  		
		return additionalOptionPanel;
	}
	
	JPanel createAdditionalDrugOptions() {
		JPanel additionalOptionPanel = new JPanel(new GridBagLayout());
		additionalOptionPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();
		JLabel optionsLabel = new JLabel("<html><b>Drug Options:</b></html>");
		optionsLabel.setToolTipText("<html>" + 
				"Drug groups to include. For drug groups definitions refer to DrugBank website:"
				+ "<br> <a href=\\\"https://dev.drugbank.com/guides/terms/\\\">dev.drugbank.com/guides/terms</a> "
				+ "</html");

		c.anchor("west").insets(0,5,0,5);
		additionalOptionPanel.add(optionsLabel, c);

		c.right().noExpand().insets(0,10,0,5);		
		approved = new JCheckBox("Approved", true);
		additionalOptionPanel.add(approved, c);
		
		c.right().noExpand().insets(0,10,0,5);
		experimental = new JCheckBox("Experimental", true);
		additionalOptionPanel.add(experimental, c);
		
		c.right().noExpand().insets(0,10,0,5);
		investig = new JCheckBox("Investigational", true);
		additionalOptionPanel.add(investig, c);
		
		c.right().noExpand().insets(0,10,0,5);
		vetApprov = new JCheckBox("Vet_approved", true);
		additionalOptionPanel.add(vetApprov, c);
		
		c.down().expandHoriz().insets(0,10,0,5);
		JLabel dummy = new JLabel("");
		additionalOptionPanel.add(dummy, c);
		
		c.right().expandHoriz().insets(0,10,0,5);
		nutraceu = new JCheckBox("Nutraceutical", true);
		additionalOptionPanel.add(nutraceu, c);
		
		c.right().expandHoriz().insets(0,10,0,5);
		withdrawn = new JCheckBox("Withdrawn", true);
		additionalOptionPanel.add(withdrawn, c);
		
		c.right().expandHoriz().insets(0,10,0,5);
		illicit = new JCheckBox("Illicit", false);
		additionalOptionPanel.add(illicit, c);
		
		c.down().expandHoriz().insets(0,10,0,5);
		JLabel dummy2 = new JLabel("");
		additionalOptionPanel.add(dummy2, c);
		
		c.right().noExpand().insets(0,5,0,2);
		JLabel taxonomy = new JLabel("Taxonomy:");
		additionalOptionPanel.add(taxonomy, c);
		
		c.right().noExpand().insets(0,5,0,2);
		String[] taxids = new String[] {"Human", "All"};
		taxid = new JComboBox<>(taxids);
		taxid.setToolTipText("If all selected, non-human drug targets will be included as well, such as bacterial proteins.");
		additionalOptionPanel.add(taxid, c);
		
		//*** listener added need to also add edgeTypeDrDis
		edgeTypeDrDis.addItemListener(new ItemListener(){
	    	@Override
	    	public void itemStateChanged(ItemEvent e) {
	    		if(e.getStateChange() == ItemEvent.SELECTED){
	    			approved.setEnabled(true);
	    			experimental.setEnabled(true);
	    			investig.setEnabled(true);
	    			vetApprov.setEnabled(true);
	    			nutraceu.setEnabled(true);
	    			withdrawn.setEnabled(true);
	    			illicit.setEnabled(true);
	    		}
	    		else if(e.getStateChange() == ItemEvent.DESELECTED){
	    			if(!edgeTypeDrP.isSelected()) {
	    				approved.setEnabled(false);
		    			experimental.setEnabled(false);
		    			investig.setEnabled(false);
		    			vetApprov.setEnabled(false);
		    			nutraceu.setEnabled(false);
		    			withdrawn.setEnabled(false);
		    			illicit.setEnabled(false);
	    			}
	    			else if(!edgeTypeDrP.isSelected()) {
	    				approved.setEnabled(true);
		    			experimental.setEnabled(true);
		    			investig.setEnabled(true);
		    			vetApprov.setEnabled(true);
		    			nutraceu.setEnabled(true);
		    			withdrawn.setEnabled(true);
		    			illicit.setEnabled(true);
	    			}
	    		}
	    		validate();
	    		repaint();
	    	}
	    });
		
	    edgeTypeDrP.addItemListener(new ItemListener(){
	    	@Override
	    	public void itemStateChanged(ItemEvent e) {
	    		if(e.getStateChange() == ItemEvent.SELECTED){
	    			approved.setEnabled(true);
	    			experimental.setEnabled(true);
	    			investig.setEnabled(true);
	    			vetApprov.setEnabled(true);
	    			nutraceu.setEnabled(true);
	    			withdrawn.setEnabled(true);
	    			illicit.setEnabled(true);
                	taxid.setEnabled(true);
                	taxonomy.setEnabled(true);
	    		}
	    		else if(e.getStateChange() == ItemEvent.DESELECTED){
	    			if(!edgeTypeDrDis.isSelected()) {
	    				approved.setEnabled(false);
		    			experimental.setEnabled(false);
		    			investig.setEnabled(false);
		    			vetApprov.setEnabled(false);
		    			nutraceu.setEnabled(false);
		    			withdrawn.setEnabled(false);
		    			illicit.setEnabled(false);
	    			}
	    			else if(!edgeTypeDrDis.isSelected()) {
	    				approved.setEnabled(true);
		    			experimental.setEnabled(true);
		    			investig.setEnabled(true);
		    			vetApprov.setEnabled(true);
		    			nutraceu.setEnabled(true);
		    			withdrawn.setEnabled(true);
		    			illicit.setEnabled(true);
	    			}
                	taxid.setEnabled(false);
                	taxonomy.setEnabled(false);
	    		}
	    		validate();
	    		repaint();
	    	}
	    });
	    
		return additionalOptionPanel;
	}
	
	JPanel createnewNetOptions() {
		
		JPanel additionalOptionPanel = new JPanel(new GridBagLayout());
		additionalOptionPanel.setPreferredSize(new Dimension(600,30));
		additionalOptionPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();
		JLabel optionsLabel = new JLabel("<html><b>Network Options:</b></html>");		
		c.anchor("west").insets(0,5,0,5);
		additionalOptionPanel.add(optionsLabel, c);
		
		c.right().noExpand().insets(0,10,0,1);
		JLabel netName = new JLabel("Network name");
		netName.setToolTipText("Enter the name you would like to be assigned to the loaded network, otherwise a unique id will be assigned.");
		additionalOptionPanel.add(netName, c);

		newNetworkName = new JTextField();
		c.right().expandHoriz().insets(0,10,0,5);
		newNetworkName.setColumns(15);
		additionalOptionPanel.add(newNetworkName, c);
		
		c.right().expandHoriz().insets(0,10,0,5);
		JLabel dummy = new JLabel("");
		additionalOptionPanel.add(dummy, c);
		
		c.right().expandHoriz().insets(0,10,0,5);
		concise = new JCheckBox("Concise", false);
		concise.setToolTipText("<html>"+"Check if you want to include a concise list of attributes for nodes and edges in the graph. "
				+ "<br>This doesn't affect the number of nodes and edges in the network "
				+ "<br>but returns fewer attributes for them to reduce the size of network file"
				+ "</html>");
		additionalOptionPanel.add(concise, c);
		
		return additionalOptionPanel;
	}
	
	JPanel createLicenseAgreement() {

		JPanel licenseAgreementPanel = new JPanel(new GridBagLayout());
		licenseAgreementPanel.setPreferredSize(new Dimension(600,30));
		licenseAgreementPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();
		JLabel agreementLabel = new JLabel("<html><b>License Agreement:</b></html>");		
		c.anchor("west").insets(0,5,0,5);
		licenseAgreementPanel.add(agreementLabel, c);
		
		c.right().noExpand().insets(0,10,0,5);		
		agreed = new JCheckBox("I agree with the NeDRex Terms of Use.", false);
		agreed.setToolTipText("If you want to use the NeDRex app, you need to first agree with our terms of use. The NeDRex Terms of Use are available at: https://api.nedrex.net/static/licence");
		licenseAgreementPanel.add(agreed, c);
		
//		c.right().noExpand().insets(0,10,0,1);
//		JLabel netName = new JLabel("Network name");
//		netName.setToolTipText("Enter the name you would like to be assigned to the loaded network, otherwise a unique id will be assigned.");
//		additionalOptionPanel.add(netName, c);

		return licenseAgreementPanel;
	}
	
	public Boolean getSelfLoop() {
		Boolean sl = selfLoop.isSelected();
		return sl;
	}
	
	public List<String> getIIDevidence(){
		List<String> iidEvidences = new ArrayList<String>();
		if (iidEvid.isSelected()) {
			iidEvidences.add("exp");
			iidEvidences.add("ortho");
			iidEvidences.add("pred");
		}
		else {
			iidEvidences.add("exp");
		}
		return iidEvidences;						
	}
	
	public Boolean allTaxIDSelected() {
		Boolean txall =  false;
		if (taxid.getSelectedIndex()==1) {
			txall = true;
		}
		return txall; 
	}
	
	public Double getThreshold() {
		//return threshold;
		return (double) disgenetScore.getValue()/100;
	}
	
	public Boolean includeOMIM() {
		return omim.isSelected();
	}

	public Boolean reviewedProteins() {
		return reviewedProteins.isSelected();
	}
	
	public Boolean includeDisGeNet() {
		return disgenet.isSelected();
	}
	
	public Boolean conciseVersion() {
		return concise.isSelected();
	}
	
	public String getNetworkName() {
		//return this.networkName;
		return newNetworkName.getText();
	}
	
	/// use this one 
	public List<String> getSelectedEdgeTypes() {
		List<String> selEdgeType = new ArrayList<String>();
		if (edgeTypeGD.isSelected())
			selEdgeType.add(InteractionType.gene_disease.getAPIname());
		if (edgeTypeGP.isSelected())
			selEdgeType.add(InteractionType.gene_protein.getAPIname());
		if (edgeTypePP.isSelected())
			selEdgeType.add(InteractionType.protein_protein.getAPIname());
		if (edgeTypeDrP.isSelected())
			selEdgeType.add(InteractionType.drug_protein.getAPIname());
		if (edgeTypePwP.isSelected())
			selEdgeType.add(InteractionType.protein_pathway.getAPIname());
		if (edgeTypeDrDis.isSelected())
			selEdgeType.add(InteractionType.drug_disease.getAPIname());
		if (edgeTypeDD.isSelected())
			selEdgeType.add(InteractionType.disease_is_disease.getAPIname());

		return selEdgeType;
	}

	public List<InteractionType> getSelectedEdgeTypeObjects() {
		List<InteractionType> selEdgeType = new ArrayList<>();
		if (edgeTypeGD.isSelected())
			selEdgeType.add(InteractionType.gene_disease);
		if (edgeTypeGP.isSelected())
			selEdgeType.add(InteractionType.gene_protein);
		if (edgeTypePP.isSelected())
			selEdgeType.add(InteractionType.protein_protein);
		if (edgeTypeDrP.isSelected())
			selEdgeType.add(InteractionType.drug_protein);
		if (edgeTypePwP.isSelected())
			selEdgeType.add(InteractionType.protein_pathway);
		if (edgeTypeDrDis.isSelected())
			selEdgeType.add(InteractionType.drug_disease);
		if (edgeTypeDD.isSelected())
			selEdgeType.add(InteractionType.disease_is_disease);

		return selEdgeType;
	}
	
	public List<InteractionType> getSelectedInteractionTypes() {
		List<InteractionType> selInteractionType = new ArrayList<InteractionType>();
		if (edgeTypeGD.isSelected())
			selInteractionType.add(InteractionType.gene_disease);
		if (edgeTypeGP.isSelected())
			selInteractionType.add(InteractionType.gene_protein);
		if (edgeTypePP.isSelected())
			selInteractionType.add(InteractionType.protein_protein);
		if (edgeTypeDrP.isSelected())
			selInteractionType.add(InteractionType.drug_protein);
		if (edgeTypePwP.isSelected())
			selInteractionType.add(InteractionType.protein_pathway);
		if (edgeTypeDrDis.isSelected())
			selInteractionType.add(InteractionType.drug_disease);
		if (edgeTypeDD.isSelected())
			selInteractionType.add(InteractionType.disease_is_disease);
		
		return selInteractionType;
	}
	/// use this one if necessary (it is not used, though)
	public List<String> getSelectedNodeTypes() {
		List<InteractionType> selIntTypes = getSelectedInteractionTypes();
		Set<String> selNodeTypeSet = new HashSet<String>();
		for (InteractionType intType: selIntTypes) {
			selNodeTypeSet.add(intType.getSourceType().toString());
			selNodeTypeSet.add(intType.getTargetType().toString());
		}
		List<String> selNodeTypes = new ArrayList<String>(selNodeTypeSet);
		return selNodeTypes;
	}

	public void setEdgeTypes(boolean gd, boolean gp, boolean pp, boolean drp, boolean pwp, boolean drdis, boolean dd) {
		edgeTypeGD.setSelected(gd);
		edgeTypeGP.setSelected(gp);
		edgeTypePP.setSelected(pp);
		edgeTypePP.setSelected(drp);
		edgeTypePP.setSelected(pwp);
		edgeTypeDrDis.setSelected(drdis);
		edgeTypeDD.setSelected(dd);
	}
	
	public List<String> getSelectedDrugGroups() {
		List<String> selDrugGroup = new ArrayList<String>();
		if (approved.isSelected())
			selDrugGroup.add("approved");
		if (experimental.isSelected())
			selDrugGroup.add("experimental");
		if (investig.isSelected())
			selDrugGroup.add("investigational");
		if (nutraceu.isSelected())
			selDrugGroup.add("nutraceutical");
		if (vetApprov.isSelected())
			selDrugGroup.add("vet_approved");
		if (withdrawn.isSelected())
			selDrugGroup.add("withdrawn");
		if (illicit.isSelected())
			selDrugGroup.add("illicit");

		return selDrugGroup;
	}
	
	public Boolean getAgreementStatus() {
		return agreed.isSelected();
	}

}

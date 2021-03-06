package org.cytoscape.myApp.internal.menuactions;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.myApp.internal.Constant;
import org.cytoscape.myApp.internal.InfoBox;
import org.cytoscape.myApp.internal.RepoApplication;
import org.cytoscape.myApp.internal.tasks.MuSTTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * NeDRex App
 * @author Sepideh Sadegh
 */
public class MuSTAction extends AbstractCyAction{
	private RepoApplication app;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private InfoBox infoBox;
	
	public MuSTAction(RepoApplication app) {
		super("MuST on current network");
		setPreferredMenu("Apps.NeDRex");
		setMenuGravity(10.0f);
		this.app = app;
		String message = "<html><body>" +
				"By selecting proteins associated with a disease under study (seeds), MuST (Multi-Steiner trees)<br>" +
				"extracts a connected subnetwork involved in the disease pathways based on the aggregation of<br>" +
				"several non-unique approximates of Steiner trees.<br><br>" +
				"This function can be run on any custom PPI network loaded in Cytoscape.<br><br>" +
				//"The protein nodes have to be selected. Alternatively, you can read the seeds from file. <br><br></body></html>";
				"Before continuing with this function, make sure you have: <br>" +
				"selected a set of genes or proteins (seeds) in your network. <br><br></body></html>";
		this.infoBox = new InfoBox(app, message, "Sadegh et al.", "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC7360763/", Constant.TUTORIAL_LINK+"availableFunctions.html#must-on-current-network");
		putValue(SHORT_DESCRIPTION, "By selecting proteins associated with a disease under study (seeds), MuST (Multi-Steiner trees) extracts a connected"
				+ " subnetwork involved in the disease pathways based on the aggregation of several non-unique approximates of Steiner trees."
				+ " This function can be run on any custom PPI network loaded in Cytoscape."
				);
	}
	
	@Override
    public boolean insertSeparatorBefore() {
		return true;
	}
	
	@Override
    public boolean insertSeparatorAfter() {
		return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (!infoBox.isHide()) {
			int returnedValue = infoBox.showMessage();
			if (returnedValue == 0) {
				//Continue
				DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
				taskmanager.execute(new TaskIterator(new MuSTTask(app)));
				if (infoBox.getCheckbox().isSelected()) {
					//Don't show this again
					infoBox.setHide(true);
				}
			}
		}else {
			DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
			taskmanager.execute(new TaskIterator(new MuSTTask(app)));
		}
		
	}

}

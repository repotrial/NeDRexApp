package org.cytoscape.myApp.internal.menuactions;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.myApp.internal.Constant;
import org.cytoscape.myApp.internal.InfoBox;
import org.cytoscape.myApp.internal.RepoApplication;
import org.cytoscape.myApp.internal.tasks.GetDiseaseGenesTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * NeDRex App
 * @author Sepideh Sadegh
 */
public class GetDiseaseGenesAction extends AbstractCyAction{
	private RepoApplication app;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private InfoBox infoBox;
	
	public GetDiseaseGenesAction(RepoApplication app) {
		super("Get Disease Genes");
		setPreferredMenu("Apps.NeDRex");
		setMenuGravity(1.1f);
		this.app = app;
		String message = "<html><body>" +
				"Get genes associated with the selected disorders (based on databases integrated in NedRexDB)<br><br>" +
				"Required imported network from NeDRexDB:<br>" +
				"A network with at least Gene-Disorder and Disorder-Disorder association types.<br>"+
				"Starting point: <br>" +
				"Selected disorder(s) in the network." +
				"<br><br></body></html>";
		this.infoBox = new InfoBox(app, message, Constant.TUTORIAL_LINK+"availableFunctions.html#get-disease-genes");
		putValue(SHORT_DESCRIPTION, "Get genes associated with the selected disorders (based on databases integrated in NedRexDB)");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (!infoBox.isHide()) {
			int returnedValue = infoBox.showMessage();
			if (returnedValue == 0) {
				//Continue
				DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
				taskmanager.execute(new TaskIterator(new GetDiseaseGenesTask(app)));
				if (infoBox.getCheckbox().isSelected()) {
					//Don't show this again
					infoBox.setHide(true);
				}
			}
		}else {
			DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
			taskmanager.execute(new TaskIterator(new GetDiseaseGenesTask(app)));
		}
	}

}

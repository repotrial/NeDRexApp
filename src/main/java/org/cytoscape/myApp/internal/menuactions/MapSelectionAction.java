package org.cytoscape.myApp.internal.menuactions;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.myApp.internal.Constant;
import org.cytoscape.myApp.internal.InfoBox;
import org.cytoscape.myApp.internal.RepoApplication;
import org.cytoscape.myApp.internal.tasks.MapSelectionToNetTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * NeDRex App
 * @author Sepideh Sadegh
 */
public class MapSelectionAction extends AbstractCyAction{
	private RepoApplication app;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private InfoBox infoBox;
	
	public MapSelectionAction (RepoApplication app) {
		super("Map Selection to another Network");
		setPreferredMenu("Apps.NeDRex.Supplementary Functions");
		setMenuGravity(38.0f);
		this.app = app;
		String message = "<html><body>" +
				"The nodes and edges selected in the current network will be selected <br>" +
				"in another network, if they exist.<br><br>" +
				"Starting point:<br>" +
				"A selection of nodes in the network.<br><br><br></body></html>";
		this.infoBox = new InfoBox(app, message, Constant.TUTORIAL_LINK+"availableFunctions.html#map-selection-to-another-network");
		putValue(SHORT_DESCRIPTION,"The nodes & edges selected in the current network will be selected in the specified target network, if they exist.");
		//this.menuGravity = 4;
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (!infoBox.isHide()) {
			int returnedValue = infoBox.showMessage();
			if (returnedValue == 0) {
				//Continue
				DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
				taskmanager.execute(new TaskIterator(new MapSelectionToNetTask(app)));
				if (infoBox.getCheckbox().isSelected()) {
					//Don't show this again
					infoBox.setHide(true);
				}
			}
		} else {
			DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
			taskmanager.execute(new TaskIterator(new MapSelectionToNetTask(app)));
		}
		
	}

}

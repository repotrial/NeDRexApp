package org.cytoscape.myApp.internal.menuactions;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.myApp.internal.Constant;
import org.cytoscape.myApp.internal.InfoBox;
import org.cytoscape.myApp.internal.RepoApplication;
import org.cytoscape.myApp.internal.RepoResultPanel;
import org.cytoscape.myApp.internal.tasks.DrugValidationTask;
import org.cytoscape.myApp.internal.utils.WarningMessages;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeDRex App
 * @author Sepideh Sadegh
 */
public class DrugValidationAction extends AbstractCyAction{
	
	private RepoApplication app;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private RepoResultPanel resultPanel;
	private InfoBox infoBox;
	
	public DrugValidationAction(RepoApplication app, RepoResultPanel resultPanel) {
		super("Drug List");
		setPreferredMenu("Apps.NeDRex.Validation");
		setMenuGravity(40.0f);
		this.app = app;
		this.resultPanel = resultPanel;
		String message = "<html><body>This drug validation method is one of the three validation methods that can be used to<br>" + 
				"evaluate the statistical significance of the results returned by NeDRex repurposing pipeline.<br>" +
				"The significance of the result drugs is estimated by calculating an empirical P-value by<br>" +
				"counting the number of randomly generated drug lists having larger overlap with the reference<br>" +
				"list of drugs than that of the NeDRex result list. In a variation of this method (DCG-based)<br>" +
				"the ranks of the reference drugs in the output are also considered.<br><br>" + 
				"Before continuing with this function, make sure you have:<br>" +
				"a) run one of the drug prioritization functions and the returned subnetwork is open;<br> " +
				"b) a list of drugs indicated for the treatment of the disease to be used as reference true drugs.<br><br></body></html>";
		this.infoBox = new InfoBox(app, message, Constant.TUTORIAL_LINK+"availableFunctions.html#drug-list", true);
		putValue(SHORT_DESCRIPTION, "A statistical method to validate the drug candidates returned by NeDRex repurposing approach.");
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(!infoBox.isHide()) {
			int returnedValue = infoBox.showMessage();
			if (returnedValue == 0 && infoBox.getLicensbox().isSelected()) {
				//Continue
				DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
				taskmanager.execute(new TaskIterator(new DrugValidationTask(app, resultPanel)));
				if (infoBox.getCheckbox().isSelected()) {
					//Don't show this again
					infoBox.setHide(true);
				}
			}
			else if (returnedValue == 0 && !infoBox.getLicensbox().isSelected()) {
				WarningMessages.showAgreementWarning();
			}

		}else{
			DialogTaskManager taskmanager = app.getActivator().getService(DialogTaskManager.class);
			taskmanager.execute(new TaskIterator(new DrugValidationTask(app, resultPanel)));
		}
		
	}

}

package fi.csc.microarray.module.sequence;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.jdesktop.swingx.JXHyperlink;

import fi.csc.microarray.client.QuickLinkPanel;
import fi.csc.microarray.client.Session;
import fi.csc.microarray.client.dialog.CreateFromTextDialog;
import fi.csc.microarray.client.dialog.SequenceImportDialog;
import fi.csc.microarray.client.visualisation.VisualisationMethod;
import fi.csc.microarray.constants.VisualConstants;
import fi.csc.microarray.databeans.DataBean;
import fi.csc.microarray.databeans.DataManager;
import fi.csc.microarray.module.Module;

public class SequenceModule implements Module {

	private static final String EXAMPLE_SESSION_URL = "https://extras.csc.fi/biosciences/chipster-manual/embster.cs";
	public static final String SERVER_MODULE_SEQUENCE = "sequence";

	@Override
	public void plugContentTypes(DataManager manager) {
		manager.plugContentType("chemical/x-fasta", true, false, "FASTA", VisualConstants.ICON_TYPE_TEXT, "fasta", "fa", "fna", "fsa", "mpfa");
		manager.plugContentType("text/wig", true, false, "WIG", VisualConstants.ICON_TYPE_TEXT, "wig");
		manager.plugContentType("text/bed", true, false, "BED", VisualConstants.ICON_TYPE_TEXT, "bed");
		manager.plugContentType("text/bed-reads", true, false, "READS", VisualConstants.ICON_TYPE_TEXT, "reads");
	}

	@Override
	public void plugFeatures(DataManager manager) {
		// TODO Auto-generated method stub

	}

	@Override
	public void plugModifiers(DataManager manager) {
		// TODO Auto-generated method stub

	}

	@Override
	public void plugTypeTags(DataManager manager) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getServerModuleName() {
		return SERVER_MODULE_SEQUENCE;
	}

	@Override
	public void addImportMenuItems(JMenu importMenu) {
		importMenu.add(getImportSequenceMenuItem());
		importMenu.addSeparator();
		importMenu.add(getCreateFromTextMenuItem());
	}

	private JMenuItem getImportSequenceMenuItem() {
		JMenuItem importSequenceMenuItem = new JMenuItem();
		importSequenceMenuItem.setText("Database...");
		importSequenceMenuItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				doImportSequence();
			}
		});
		return importSequenceMenuItem;
	}

	private JMenuItem getCreateFromTextMenuItem() {
		JMenuItem createFromTextMenuItem = new JMenuItem();
		createFromTextMenuItem.setText("Text...");
		createFromTextMenuItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				doCreateFromText();
			}
		});
		return createFromTextMenuItem;
	}

	@Override
	public void addImportLinks(QuickLinkPanel quickLinkPanel, List<JXHyperlink> importLinks) {
		importLinks.add(quickLinkPanel.createLink("Import from UniProt, EMBL, PDB... ", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doImportSequence();
			}
		}));
		
		importLinks.add(quickLinkPanel.createLink("Create dataset from text ", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCreateFromText();
			}
		}));
	}

	private void doCreateFromText() {
		try {
		    new CreateFromTextDialog(Session.getSession().getApplication());
		    
		} catch (Exception me) {
			Session.getSession().getApplication().reportException(me);
		}
	}

	private void doImportSequence() {
		try {
	        new SequenceImportDialog(Session.getSession().getApplication());
	        
		} catch (Exception me) {
			Session.getSession().getApplication().reportException(me);
		}
	}

	@Override
	public boolean isImportToolSupported() {
		return false;
	}

	@Override
	public boolean isWorkflowCompatible(DataBean data) {
		return true; // all operations should be workflow compatible
	}

	@Override
	public VisualisationMethod[] getVisualisationMethods() {
		return new VisualisationMethod[] {};
	}

	@Override
	public URL getExampleSessionUrl() throws MalformedURLException {
		return new URL(EXAMPLE_SESSION_URL);
	}

}
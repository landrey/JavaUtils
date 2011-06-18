package fr.loria.madynes.javautils.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ActionFromProperties extends AbstractActionFromProperties{

	private static final long serialVersionUID = 1L;
	private ActionListener delegateListener;

	public ActionFromProperties(String propertiesPrefix, String command, ActionListener delegateListener) {
		super(propertiesPrefix, command);
		this.delegateListener=delegateListener;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.delegateListener.actionPerformed(e);
	}

}

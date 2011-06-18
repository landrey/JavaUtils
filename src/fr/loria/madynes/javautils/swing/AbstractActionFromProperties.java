package fr.loria.madynes.javautils.swing;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import fr.loria.madynes.javautils.Properties;

public abstract class AbstractActionFromProperties extends AbstractAction {
	private static final long serialVersionUID = 1L;
	private static final String nameSuffix=".name";
	private static final String shortdescriptionSuffix=".shortdescription";
	private static final String longdescriptionSuffix=".longdescription";
	private static final String acceleratorSuffix=".accelerator";
	private static final String mnemonicSuffix=".mnemonic";
	
	protected AbstractActionFromProperties(String propertiesPrefix, String command){ // protected, this class is abstract anyway
		String s=Properties.getOptionalMessage(propertiesPrefix+nameSuffix);
		if (s!=null){
			this.putValue(NAME, s);
		}
		s=Properties.getOptionalMessage(propertiesPrefix+shortdescriptionSuffix);
		if (s!=null){
			this.putValue(SHORT_DESCRIPTION, s);
		}
		s=Properties.getOptionalMessage(propertiesPrefix+longdescriptionSuffix);
		if (s!=null){
			this.putValue(LONG_DESCRIPTION, s);
		}
		/* Not in message bundle. FIXED by program.
		s=Properties.getOptionalMessage(propertiesPrefix+".command");
		if (s!=null){
			this.putValue(ACTION_COMMAND_KEY, s);
		}
		*/
		this.putValue(ACTION_COMMAND_KEY, command);
		
		s=Properties.getOptionalMessage(propertiesPrefix+acceleratorSuffix);
		if (s!=null){
			this.putValue(ACCELERATOR_KEY,  KeyStroke.getKeyStroke(s)); // KeyStroke.getKeyStroke for KeyStroke string specification format.
		}
		s=Properties.getOptionalMessage(propertiesPrefix+mnemonicSuffix);
		if (s!=null){
			this.putValue(MNEMONIC_KEY, Integer.parseInt(s));
		}
		//TODO: icons...
	}
}

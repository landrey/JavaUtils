package fr.loria.madynes.javautils;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import fr.loria.madynes.javautils.Properties.PropertyChangedEvent;

/** This class provided a basic graphical editor for editable properties (==preferences).
 * 
 * Properties are coming from a {@link Properties} object. Any property of name <i>k</i> with a joint 
 * property of key <i>k._editable</i> of value true is displayed and changeable by the user.
 *  
 *
 * @see Properties
 * @author andrey
 *
 */
public class PropertiesPreferencesEditor extends JFrame 
implements ItemListener, ActionListener {

	private static final long serialVersionUID = 448106588988456342L;
	private static final String ressourceBundleName = "fr.loria.madynes.javautils.MessageBundle";
	// Icon path can be provided as a property in edited properties...
	private static final String reloadIconPropertyKey="fr.loria.madynes.javautils.PropertiesPreferencesEditor";
	private static final String defaultReloadIconName="images/reload-icon.png";
	public static final String LABEL_KEY_SUFFIX="._label";
	public static final String TIP_KEY_SUFFIX="._tip";

	public static final String TEXT_EDITED_CMD = "t";
	public static final String RESET_TO_DEFAULT_CMD = "r";
	public static final String CHOOSE_COLOR_CMD = "c";
	private static final String windowTitleKey = "fr.loria.madynes.javautils.PropertiesPreferencesEditor";
	private static final String windowTitleDef = "Preferences Editor";
	
	
	private ResourceBundle applicationMessages;
	private ResourceBundle defaultMessages;
	
	private Properties editedProperties;
	private Map<String,PropertyView> keyToView=new HashMap<String,PropertyView>();
	private Observer globalPropertiesObserver=new Observer(){
		@Override
		public void update(Observable o, Object arg) {
			PropertyChangedEvent pe=(PropertyChangedEvent)arg;
			PropertyView pv=keyToView.get(pe.getKey());
			if (pv!=null){
				pv.updateFromProperties(pe.getSource(), pe.getKey(), pe.getNewVal());
			}else{
				Logger.getLogger("").logp(Level.WARNING,
						this.getClass().getName(), "globalPropertiesObserver.update", "can not find view for changed preference key.");
			}
		}
	};
	/**
	 * Show a modal dialog to edit editable properties (preferences) of a Properties object.
	 * 
	 * The message parameter allows to specify where the PropertiesPrefrencesEditor will get its messages.
	 * If a given message is not found in this messages bundle, the one from the fr.loria.madynes.javautils.MessagesBundle
	 * will be used.
	 *  
	 * @param properties properties set to edit editable elements.
	 * @param messages messages bundle to get various strings (window title, error message...) may be null.
	 */
	public PropertiesPreferencesEditor(Properties properties, ResourceBundle messages){
		super();
		this.editedProperties=properties;
		this.applicationMessages=messages;
		this.defaultMessages=null;// see this.getMessage().
		ImageIcon reloadIcon;
		try{
			String reloadIconPath=properties.getString(reloadIconPropertyKey);
			reloadIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(
				((java.net.URLClassLoader) ClassLoader
						.getSystemClassLoader())
						.findResource(reloadIconPath)));
		}catch(Exception e){
			Logger.getLogger("").logp(Level.INFO,
					this.getClass().getName(), "constructor", "use default "+defaultReloadIconName);
			reloadIcon=new ImageIcon(this.getClass().getResource(defaultReloadIconName));
		}
		// properties.getProperty("applicationIconeFileName")
		this.editedProperties.addGlobalObserver(this.globalPropertiesObserver);
		JPanel topPanel=new JPanel();
		topPanel.setLayout(new GridBagLayout());
		add(topPanel);
		setTitle(this.getMessage(windowTitleKey, windowTitleDef)); 
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JPanel scrolledPanel=new JPanel();
		JScrollPane scrollPane = new JScrollPane(scrolledPanel);
		scrollPane.setPreferredSize(new Dimension(800, 400));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		
		//GridLayout spgl=new GridLayout(0,3);
		//spgl.setColumns(2);
		//scrolledPanel.setLayout(spgl);
		// = (GridLayout) scrolledPanel.getLayout();
		scrolledPanel.setLayout(new GridBagLayout());
		GridBagConstraints spgbc=new GridBagConstraints();
		Dimension dimLabel=null;
		//Border labelBorder=BorderFactory.createLineBorder(Color.BLACK);
		for (String key:properties.editablePropertiesKeySet()){
			spgbc.gridwidth=GridBagConstraints.RELATIVE;
			// label
			String labelStr;
			try {
				labelStr=messages.getString(key+LABEL_KEY_SUFFIX);
			}catch (MissingResourceException mre){
				labelStr=key;
			}
			String tipStr=null;
			try{
				tipStr=messages.getString(key+TIP_KEY_SUFFIX);
			}catch(Exception e){
				tipStr=key;
			}
			JTextField lb=new JTextField(labelStr);
			//JLabel lb=new JLabel(labelStr);
			if (dimLabel==null){
				dimLabel=lb.getPreferredSize();
				dimLabel.setSize(500, dimLabel.height); // Force width, but not height...
				//TODO: 500 as properties...
			}
			lb.setPreferredSize(dimLabel);
			lb.setEditable(false);
			//lb.setBorder(labelBorder);
			if (tipStr!=null){
				lb.setToolTipText(tipStr);
			}
			//scrolledPanel.add(lb);
			scrolledPanel.add(lb, spgbc);
			// Add a little something for properties actually coming from System (and editable...)...
			if (properties.comesFromSystem(key)){
				lb.setBackground(Color.red); // TODO: push RED as a properties. 
			}
			// edit property according to its type...
			String typeStr=properties.getPropertyType(key); // look-up in properties not in messages. May be null.
			//JComponent edit=null;
			PropertyView pv=null;
			if (typeStr!=null){
				if (Properties.BOOLEAN_TYPE_STR.equals(typeStr)){
					/*---
					JCheckBox cb=new JCheckBox();
					cb.setSelected(properties.getOptinalBooleanProperty(key, true)); 
					// TODO: default for typed but missing properties;
					cb.addItemListener(this);
					edit=cb;
					--*/
					pv=new BooleanPropertyView(this, key);
				} else if(Properties.COLOR_TYPE_STR.equals(typeStr)){
					pv=new ColorPropertyView(this, key);
				} else if(Properties.LOG_LEVEL_TYPE_STR.equals(typeStr)){
					pv=new LogLevelPropertyView(this, key);
				}
			}
			
			if (pv==null){// default type as: String...
				/*--
				JTextField tf=new JTextField(properties.getString(key));
				tf.setEditable(true);
				tf.addActionListener(this);
				edit=tf;
				--*/
				pv=new StringPropertyView(this, key);
			}
			pv.doMoreInit(key, dimLabel.height);
			//scrolledPanel.add(pv.getView());
			scrolledPanel.add(pv.getView(), spgbc);
			
			spgbc.gridwidth=GridBagConstraints.REMAINDER;
			JButton rb=new JButton();
			rb.setName(key);
			rb.addActionListener(this);
			rb.setActionCommand(RESET_TO_DEFAULT_CMD);
			rb.setText("Reset to default"); // Icons + externalize.
			rb.setIcon(reloadIcon);
			//scrolledPanel.add(rb);
			scrolledPanel.add(rb, spgbc);
		}//for
		//c.fill = GridBagConstraints.HORIZONTAL;
		//add(textField, c);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		topPanel.add(scrollPane, gbc);
		JTextField msg=new JTextField("Use return to validate changes in text properties"); // TODO: externalize. messages.get...
		msg.setEditable(false);
		topPanel.add(msg);
		pack();
		setAlwaysOnTop(true);
		setVisible(true);
	}
	
	// For ItemListener
	@Override
	public void itemStateChanged(ItemEvent e) {
		JCheckBox cb=(JCheckBox)e.getSource();
		this.editedProperties.setPreference(cb.getName(), Boolean.toString(cb.isSelected()));
	}
	// For actionListener
	@Override
	public void actionPerformed(ActionEvent e) {
		if (TEXT_EDITED_CMD.equals(e.getActionCommand())){
			JTextField tf=(JTextField)e.getSource();
			// TODO: add a Properties.validate(key, value) and use it here !
			this.editedProperties.setPreference(tf.getName(), tf.getText());
		}else if(RESET_TO_DEFAULT_CMD.equals(e.getActionCommand())){
			JButton b=(JButton)e.getSource(); 
			this.editedProperties.removePreference(b.getName());
		}else if(CHOOSE_COLOR_CMD.equals(e.getActionCommand())){
			JButton b=(JButton)e.getSource();
			Color c=JColorChooser.showDialog(b, "Pick up a color", b.getBackground());
			if (c!=null){
				// no need for validation. We got a Color object.
				this.editedProperties.setPreference(b.getName(), c);
			}
		}else{
			Logger.getLogger("").logp(Level.WARNING,
									  this.getClass().getName(), "actionPerformed", "unknown action cmd: "+e.getActionCommand());
		}
	}
	
	private static abstract class PropertyView{
		private PropertyView(PropertiesPreferencesEditor pEdit, String key){
			pEdit.keyToView.put(key, this); // <<<==============================!!
		}
		protected abstract JComponent getView();
		protected abstract void updateFromProperties(Properties prop, String key, String value);
		protected void doMoreInit(String key, int height){
			JComponent v=this.getView();
			// may be not enough if view is made of several Components triggering action =>
			// sub class MUST setName of inner components to allow actionListener (propertiesEditor) to works correctly.
			v.setName(key);
			v.setToolTipText(key);
		}
	}
	private static class StringPropertyView extends PropertyView{
		JTextField tf;
		private  StringPropertyView(PropertiesPreferencesEditor pEdit, String key){
			super(pEdit, key);
			tf=new JTextField(pEdit.editedProperties.getString(key));
			tf.setEditable(true);
			tf.addActionListener(pEdit);
			tf.setActionCommand(TEXT_EDITED_CMD);
		}
		@Override
		protected void updateFromProperties(Properties prop, String key, String value) {
			this.tf.setText(value);
		}
		@Override
		protected JComponent getView(){
			return this.tf;
		}
		@Override
		protected void doMoreInit(String key, int height){
			super.doMoreInit(key, height);
			this.tf.setPreferredSize(new Dimension(400, height));
		}
	}
	private static class BooleanPropertyView extends PropertyView{
		JCheckBox cb;
		private  BooleanPropertyView(PropertiesPreferencesEditor pEdit, String key){
			super(pEdit, key);
			cb=new JCheckBox();
			cb.setSelected(pEdit.editedProperties.getOptinalBooleanProperty(key, true)); 
			// TODO: default for typed but missing (badly written) properties;
			cb.addItemListener(pEdit);
		}
		@Override
		protected void updateFromProperties(Properties prop,String key, String value) {
			this.cb.setSelected(Boolean.parseBoolean(value));
		}
		@Override
		protected JComponent getView(){
			return this.cb;
		}
	}
	private static class ColorPropertyView extends PropertyView{
		JPanel p;
		JTextField tf;
		JButton cl;
		private  ColorPropertyView(PropertiesPreferencesEditor pEdit, String key){
			super(pEdit, key);
			p=new JPanel();
			tf=new JTextField(pEdit.editedProperties.getString(key));
			tf.setName(key);
			tf.setEditable(true);
			tf.addActionListener(pEdit);
			tf.setActionCommand(TEXT_EDITED_CMD);
			cl=new JButton();
			cl.setName(key);
			cl.addActionListener(pEdit);
			cl.setActionCommand(CHOOSE_COLOR_CMD);
			//cl.setBackground(pEdit.editedProperties.getOptinalColorProperty(key, Color.WHITE)); //TODO: default color then color is not good + error message to user.
			updateFromProperties(pEdit.editedProperties, key, pEdit.editedProperties.getString(key));
			this.p.add(this.tf);
			this.p.add(this.cl);
		}
		
		@Override
		protected void updateFromProperties(Properties prop, String key, String value) {
			this.tf.setText(value);
			// Re-sucks from properties 
			// TODO: add a public static stringToColor conversion  method in Properties class...
			Color newcolor=prop.getOptinalColorProperty(key, Color.WHITE); //TODO: default color then color is not good + error message to user.
			cl.setForeground(newcolor);
			cl.setBackground(newcolor); 
			// trouble when playing with color alpha attribute... We set foreground and background. See JComponent java doc about alpha support...
			// Not enough. Button repaint : no effect. Container repaint: ok. Probably due to pluggable UI interaction.
			Container parent=cl.getParent();
			if (parent!=null){ // can be null at construction time !
				//cl.repaint();
				parent.repaint();
			}
		}
		@Override
		protected JComponent getView(){
			return this.p;
		}
		@Override
		protected void doMoreInit(String key, int height){
			super.doMoreInit(key, height);
			Dimension d=new Dimension(200, height);
			this.tf.setPreferredSize(d);
			this.cl.setPreferredSize(d);
		}
	}
	// TO DO CHECK...
	static private class LogLevelPropertyView extends StringPropertyView {
		private  LogLevelPropertyView(PropertiesPreferencesEditor pEdit, String key){
			super(pEdit, key);
		}
	}
	
	private String getMessage(String key, String def){
		assert def!=null;
		String res=null;
		if (this.applicationMessages!=null){
			try{
				res=this.applicationMessages.getString(key);
			}catch(Exception e){
			}
		}
		if (res==null){
			if (this.defaultMessages==null){
				try {
					this.defaultMessages = ResourceBundle.getBundle(ressourceBundleName); 
					res=this.defaultMessages.getString(key);
				} catch (MissingResourceException mre) {
					Logger
							.getLogger("")
							.logp(
									Level.SEVERE,
									this.getClass().getName(),
									"getMessage",
									"Can not load  MessagesBundles (messages properties ressource file in package): "+ressourceBundleName,
									mre);
				}
			}else{
				res=this.defaultMessages.getString(key);
			}
		}
		return res!=null?res:def;
	}		
}

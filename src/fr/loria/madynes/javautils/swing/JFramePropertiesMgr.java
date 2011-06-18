package fr.loria.madynes.javautils.swing;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import fr.loria.madynes.javautils.Properties;

/**
 * A way to manage the saving of  a window bounds as preferences.
 * This is indeed a WindowListener. Original window is decorated by this manager.
 * TODO: remove this manager ?
 * 
 * @author andrey
 *
 */
public class JFramePropertiesMgr extends WindowAdapter {
	private static final String preferedSizeSuffix = ".preferedsize"; // TODO: push definition in Properties ?;
	private static final Dimension defaultPreferedSize = new Dimension(600, 400);// TODO: get screen size ?
	private static final String posSuffix = ".pos";
	@SuppressWarnings("unused")
	private static final Point defaultPreferedPos=new Point(10, 10);
	
	private JFrame managed;
	private Properties inProps;
	private String keyPrefix;
	
	public JFramePropertiesMgr(JFrame managed, Properties inProps, String keyPrefix){
		assert managed!=null;
		assert inProps!=null;
		assert keyPrefix!=null;
		this.managed=managed; // could be no use... see WindowEvent...
		this.inProps=inProps;
		this.keyPrefix=keyPrefix;
		managed.addWindowFocusListener(this); //What for ?
		managed.setPreferredSize(inProps.getOptionalDimensionProperty(keyPrefix+preferedSizeSuffix, defaultPreferedSize));
		Point topLeft=inProps.getIntPointProperty(keyPrefix+posSuffix);
		if (topLeft==null){
			managed.setLocationByPlatform(true);
		}else{
			managed.setLocation(topLeft);
		}
		//TOOO ensure  bounds are ok for actual screen...
	}
	// WindowAdapter
	// opening: getBounds or used provided defs 
	@Override
	public void windowClosing(WindowEvent e) {
		Logger.getLogger("").logp(Level.INFO, this.getClass().getName(), "windowClosing", "CLOSING");
	}
	
	@Override
	public void windowClosed(WindowEvent e) {
		Rectangle b=this.managed.getBounds();
		if (this.inProps.isEditable(this.keyPrefix+preferedSizeSuffix)){
			this.inProps.setPreference(this.keyPrefix+preferedSizeSuffix, this.managed.getSize());
		}
		if (this.inProps.isEditable(this.keyPrefix+posSuffix)){
			this.inProps.setPreference(this.keyPrefix+posSuffix, this.managed.getLocation());
		}
		Logger.getLogger("").logp(Level.INFO, this.getClass().getName(), "windowClosed", "CLOSED "+this.managed.getBounds());
	}
	
	public static JFrame manage(JFrame managed, Properties inProps, String keyPrefix){
		assert managed!=null;
		managed.addWindowListener(new JFramePropertiesMgr(managed, inProps, keyPrefix));
		return managed;
	}
}


    	

/* 
 * Utility  class  to handle resources in concrete packages
 * - "typed" configuration data with a hierarchical mechanism (userpref, application level config file, and system properties (todo)).
 * - Static mechanism to access the "application" resources bundle for various texts and messages which are internationalized...
 * - Observable/Observer for any key.  
 */
package fr.loria.madynes.javautils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.Observable;
import java.util.Observer;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Utility  class  to handle resources in concrete packages
 * 
 * @author andrey
 * 
 * 
 * TODO: 
 * - use hierarchical properties files  and load/store method (which only apply to last file) to handle default/preferences
 * mechanism. 
 *
 */
public  class Properties {
	static public class PropertyChangedEvent {	
		private Properties source;
		private String key;
		private String newVal;
		private String oldVal;
		private PropertyChangedEvent(Properties source, String k, String nv, String ov){
			this.setSource(source);
			this.setKey(k);
			this.setNewVal(nv);
			this.setOldVal(ov);
		}
		private void setKey(String key) {
			this.key = key;
		}
		public String getKey() {
			return key;
		}
		private void setNewVal(String newVal) {
			this.newVal = newVal;
		}
		public String getNewVal() {
			return newVal;
		}
		private void setOldVal(String oldVal) {
			this.oldVal = oldVal;
		}
		public String getOldVal() {
			return oldVal;
		}
		private void setSource(Properties source) {
			this.source = source;
		}
		public Properties getSource() {
			return source;
		}	
	}
	
	static private class PropertyObservable extends Observable {
		void setChangedAndNotifyObservers(PropertyChangedEvent e){
				this.setChanged();
				this.notifyObservers(e);
		}
	}

	// additional special suffix in PROPERTIES file property names.
	// For edit mechanism.
	public static final String EDITABLE_KEY_SUFFIX="._editable"; // That also means that this property is a potential preference -- No, not in properties ;-)
	public static final String TYPE_KEY_SUFFIX="._type";
		// value for TYPE_KEY_SUFFIX properties key
		public static final String BOOLEAN_TYPE_STR="B";
		public static final String COLOR_TYPE_STR="C";
		public static final String STRING_TYPE_STR="S";
		public static final String INT_TYPE_STR="I";
		public static final String LOG_LEVEL_TYPE_STR="LL";
	// Take system property first if exists. (-D option)
	public static final String SYS_PROP_FIRST_KEY_SUFFIX = "._sysfirst";	
	public static final char STRING_LIST_SEPARATOR=';';
	//private static final String dimensionWidthSuffix = ".width";
	//private static final String dimensionHeightSuffix = ".height";
	
	private static Properties defaultProperties;
	private static ResourceBundle messages;
	
	/*
	 * Set application texts and messages (localized bundle)
	 * @param ressourceBundleName the bundle to load, as a class name
	 */
	public static void setMessages(String ressourceBundleName){
		try {
			messages = ResourceBundle.getBundle(ressourceBundleName); 
		} catch (MissingResourceException mre) {
			Logger
					.getLogger("")
					.logp(
							Level.SEVERE,
							"fr.loria.madynes.javautils.Properties",
							"setMessages",
							"Can not load  MessagesBundles (messages properties ressource file in package): "+ressourceBundleName,
							mre);
		}
	}
	/**
	 * get  application level messages.
	 */
	public static ResourceBundle getMessages(){
		return messages;
	}
	
	/**
	 * get an application level messages.
	 * @param key a name in the global messages bundle.
	 */
	public static String getMessage(String key){
		return messages.getString(key);
	}
	
	/**
	 * get an application level option message.
	 * @param key a name in the global messages bundle.
	 * @return message associated to key or null if not found
	 */
	public static String getOptionalMessage(String key){
		try{
			return messages.getString(key);
		}catch(MissingResourceException mre){
			return null;
		}
	}
	
	/**
	 * get an application level option message.
	 * @param key a name in the global messages bundle.
	 * @param defaultValue
	 * @return message associated to key or null if not found
	 */
	public static String getOptionalMessage(String key, String defaultValue){
		try{
			return messages.getString(key);
		}catch(MissingResourceException mre){
			return defaultValue;
		}
	}
	/**
	 * Static mechanism to have a per application default "Properties" file.
	 */
	public static void setDefaultProperties(String baseName){
		if (defaultProperties!=null){
			LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
					 "fr.loria.madynes.javautil.Properties",
					 "setDefaultProperties",
					 "try to reset default properties");
		}
		defaultProperties=getProperties(baseName);
	}
	public static Properties getDefaultProperties(){
		// TODO ? : log null return ?
		return defaultProperties; 
	}
	
	/** Get the properties associated to a resource name (class loader name).
	 * We use a factory method to create properties object for a given resource and avoid
	 * several LOCAL preferences creation for the same resource.
	 * 
	 * @param baseName a resource name in dotted notation
	 * @return
	 */
	public static Properties getProperties(String baseName){
		Properties result=builtProperties.get(baseName);
		if (result==null){
			result=new Properties(baseName);
			builtProperties.put(baseName, result); // keep the ref to the new bundle, and so ensure uniqueness. 
		}
		return result;
	}
	private Properties(String baseName){
		this.baseName=baseName;
		this.prefFilePath=null;
		try{
			this.configuration = ResourceBundle.getBundle(baseName);
		}catch (java.util.MissingResourceException e){
			// For Logger messages we have a chicken-eggs problem 
				LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
													 this.getClass().getName(),
													 "Constructor",
													 "No "+baseName+" file has not been found");
		}
		prefs=new HashMap<String, String>();
		String prefFilePath=this.getOptionalProperty("preferenceFile", "."+baseName+"."+"prefs");
		File prefFile= new File(prefFilePath);
		InputStream prefIs=null;
		if (prefFile.canRead()&&prefFile.isFile()){
			try {
				prefIs=new FileInputStream(prefFile);
			} catch (FileNotFoundException e) {
				LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
						 this.getClass().getName(),
						 "Constructor",
						 "preference file from properties file (or default): "+prefFilePath+"  has not been found or not readable");
			}
		}
		if (prefIs==null){
			// try base pref in user home...
			prefFilePath=System.getProperty("user.home", "~")+System.getProperty("file.separator")+prefFile.getName();
			prefFile=new File(prefFilePath);
			try {
				prefIs=new FileInputStream(prefFile);
			} catch (FileNotFoundException e) {
				LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
						 this.getClass().getName(),
						 "Constructor",
						 "preference file from HOME: "+prefFilePath+"  has not been found");
			}
		}
		if (prefIs==null){
			LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
					 this.getClass().getName(),
					 "Constructor",
					 "no preference file found....");
			this.prefFilePath=null;
		}else{
			LogManager.getLogManager().getLogger("").logp(Level.INFO, 
					 this.getClass().getName(),
					 "Constructor",
					 "read preferences from file:"+prefFile.getAbsolutePath());
			// suck prefBundle  from pref...
			PropertyResourceBundle prefBundle;
			this.prefFilePath=prefFilePath;
			try {
				prefBundle = new PropertyResourceBundle(prefIs);
				String k=null;
				for(Enumeration<String> ek=prefBundle.getKeys(); ek.hasMoreElements(); ){
					k=ek.nextElement();
					try{
						prefs.put(k, prefBundle.getString(k));
					}catch(Exception e){
						LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
								this.getClass().getName(),
								"Constructor",
							 	"internal error, can not access known key "+e);
					}
				}
			} catch (IOException ioe) {
				LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
						 this.getClass().getName(),
						 "Constructor",
						 "IO when reading "+prefFilePath+" preferences file. "+ioe);
				this.prefFilePath=null;
			}
		}
	}
	
	public void savePreferences() throws FileNotFoundException{
		if (this.isPreferencesChanged()){
			String prefFilePath=this.getOptionalProperty("preferenceFile", "."+baseName+"."+"prefs");
			File prefFile= new File(prefFilePath);
			PrintStream prefPs=null;
			LogManager.getLogManager().getLogger("").logp(Level.INFO, 
					 this.getClass().getName(),
					 "savePreferences",
					 "save preferences to: "+prefFilePath);
			if (prefFile.canWrite()){
				try {
					prefPs=new PrintStream(prefFile);
				} catch (Exception e) {
					LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
							 this.getClass().getName(),
							 "savePreferences",
							 "preferences file from properties file (or default): "+prefFilePath+"  is not writable: "+e);
				}
			}
			if (prefPs==null){
				// try base pref in user home... 
				//We also have user.dir=/home/andreylocal/workspace/Test and java.class.path=/home/andreylocal/workspace/Test/bin
				prefFilePath=System.getProperty("user.home", "~")+System.getProperty("file.separator")+prefFile.getName();
				prefFile=new File(prefFilePath);
				try {
					prefPs=new PrintStream(prefFile);
				} catch (FileNotFoundException fnfe) {
					LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
							 this.getClass().getName(),
							 "Constructor",
							 "preference file in HOME: "+prefFilePath+"  is not writable: "+fnfe);
					// giveUp
					throw fnfe;
				}catch(SecurityException se){
					LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
							 this.getClass().getName(),
							 "Constructor",
							 "preference file in HOME: "+prefFilePath+"  is not writable: "+se);
					// giveUp
					throw se;
				}
			}
			for (String key:this.prefs.keySet()){
				prefPs.println(key+"="+this.prefs.get(key));
			}
		}
	}
	/** Get properties keys (without preferences ones, which should be a subset anyway)
	 * 
	 * @return
	 */
	public Set<String> propertiesKeySet(){
		return this.configuration.keySet();
	}
	
	/** Get editable (=possible preferences) properties keys
	 * 
	 * "key._editable" properties key are not return even if a "key._editable._editable" exits. 
	 * So key._editable, are not editable.
	 * 
	 * @return a sorted set of preferences keys
	 */
	public SortedSet<String> editablePropertiesKeySet(){
		SortedSet<String> result=new TreeSet<String>();
		Set<String> propKeys=this.propertiesKeySet(); // =this.configuration.keySet()
		// take all key prefixes  with  EDITABLE_KEY_SUFFIX as a suffix 
		for (String key: propKeys){
			if (!key.endsWith(EDITABLE_KEY_SUFFIX) && propKeys.contains(key+EDITABLE_KEY_SUFFIX)){ // or this.getOptinalBooleanProperty(key+editableSuffix, false) ?
				result.add(key);
			}
		}
		return result;
	}
	/** Check if property is editable (= a key._editable property exists)
	 * 
	 * Such properties are candidate to be user preferences.
	 * 
	 * @param key the property key
	 * @return true if the property with the given key is editable 
	 */
	public boolean isEditable(String key){
		// check in properties only not in preferences
		return this.configuration.containsKey(key+EDITABLE_KEY_SUFFIX);
	}
	
	/**
	 * return the type of a property (preference)
	 * @param key the property key
	 * @return the value of the key+."_type" or "S" by default
	 */
	public String getPropertyType(String key){
		// get only from properties not preferences.
		String result=STRING_TYPE_STR;
		try{
			result=this.configuration.getString(key+TYPE_KEY_SUFFIX);
		}catch(Exception e){
		}
		return result;
	}
	
	/**
	 * Set a new or change a property, which is indeed changing a PREFERENCE.
	 * @param key must not be null
	 * @param value must not be null
	 * @return true if preference has really been updated
	 */
	public boolean setPreference(String key, String value){
		assert key!=null;
		assert value!=null;
		String oldValue=null;
		boolean result=false;
		if (isEditable(key)){ // ._editable property are not supposed to have a ._editable...
			try{
				oldValue=this.getString(key);// Get in prefs then properties
			}catch(Exception e){
			}
			// value is not null (see assertion) so let us test this way:
			if (!value.equals(oldValue)){ 
				result=true;
				this.prefs.put(key, value);
				this.setPreferencesChanged(true);
				this.notifyChange(key, value, oldValue); // update observer.
			}
		}else{
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					 this.getClass().getName(),
					 "setPreference",
					 key+" is not an editable property");
		}
		return result;
	}
	
	/** Remove a property from preferences.
	 * 
	 * This can also be seen a a "reset to default" method 
	 * @param key
	 * @return true if preference has really been removed.
	 */
	public boolean removePreference(String key){
		assert key!=null;
		boolean result=false;
		if (isEditable(key)){ // ._editable property are not supposed to have a ._editable...
			if (this.prefs.containsKey(key)){
				String inPrefValue=this.prefs.get(key);
				try{
					String inProValue=this.configuration.getString(key);
					this.prefs.remove(key);
					if (!(inPrefValue==null||inPrefValue.equals(inProValue))){
						result=true;
						this.setPreferencesChanged(true);
						this.notifyChange(key, inProValue, inPrefValue); // update observer.
					}
				}catch(Exception e){
					LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
							 this.getClass().getName(),
							 "removePreference",
							 key+" default value in properties can not be found", e);
				}
			}else{
				// Nothing to do. Not in preferences...
				LogManager.getLogManager().getLogger("").logp(Level.INFO, 
						 this.getClass().getName(),
						 "removePreference",
						 key+" is in not in preferences.");
			}
		}else{
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					 this.getClass().getName(),
					 "removePreference",
					 key+" is not an editable property");
		}
		return result;
	}
	
	/** Check if property must be taken for system property first.
	 * 
	 * Remark: properties set using command line option -D are seen as System properties.
	 * This property on property must be set in default properties file, 
	 * try to set one of these "meta" properties in a preferences file has no effect.
	 * 
	 * @param key the property key
	 * @return true if the property with the given key must be taken from System properties first
	 */
	public boolean takeSystemPropertyFirst(String key){
		// check in properties only not in preferences
		return this.configuration.containsKey(key+SYS_PROP_FIRST_KEY_SUFFIX);
	}
	
	/** Check if property actually comes from System properties.
	 * 
	 * @param key the property key
	 * @return true if the property will be taken from System 
	 * 		   AND exists in System properties.
	 */
	public boolean comesFromSystem(String key){
		return this.configuration.containsKey(key+SYS_PROP_FIRST_KEY_SUFFIX) &&
			System.getProperties().containsKey(key);
	}
	/**
	 * Get a (string) Property
	 * @param property property key
	 * @return property value or null
	 * 
	 * @see #getString(String) same with exceptions
	 */
	public String getProperty(String property) {
		try {
			return this.getString(property);
		} catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					 this.getClass().getName(),
					 "getProperty",
					 this.baseName+ ": Mandatory String property "+property+" is missing");
			return null;
		}
	}
	
	public String getOptionalProperty(String property, String byDefault) {
		try {
			return this.getString(property);
		}catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
					 this.getClass().getName(),
					 "getOptionalProperty",
					 this.baseName+": Optional String property "+property+" not present");
			return byDefault;
		}
	}
	public String getOptionalProperty(String property) {
		return getOptionalProperty(property, null);
	}
	
	/** Get a mandatory integer property. Raised Exception.
	 *  
	 * NO EXCEPTION are caught. NumberFormatException and some other runtime exception might be raised.
	 *  
	 * @param property
	 * @return
	 */
	public int getIntPropertyRe(String property){
		return Integer.parseInt(this.getString(property));
	}
	
	public int getIntProperty(String property){
		try{ 
			return  this.getIntPropertyRe(property);
		}catch (NumberFormatException nfe){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					 this.getClass().getName(),
					 "getIntProperty",
					 this.baseName+": Mandatory integer property "+property+" as a bad format");
		}catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					 this.getClass().getName(),
					 "getIntProperty",
					 this.baseName+": Mandatory integer property "+property+" is missing");
		}
		return 0;
	}
	public int getOptionalIntProperty(String property, int byDefault){
		try{ 
			return Integer.parseInt(this.getString(property));
		}catch (NumberFormatException nfe){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					 this.getClass().getName(),
					 "getOptionalIntIntProperty",
					 this.baseName+": Optional integer property "+property+" as a bad format");
		}catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
					 this.getClass().getName(),
					 "getOptionalIntProperty",
					 this.baseName+": Option integer property "+property+" not present");
		}
		return byDefault;
	}
	public boolean getBooleanProperty(String property){
		try{ 
			return Boolean.parseBoolean(this.getString(property));
		}catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					 this.getClass().getName(),
					 "getBooleanProperty",
					 this.baseName+": Mandatory boolean property "+property+" is missing");
		}
		return false;
	}
	public boolean getOptinalBooleanProperty(String property, boolean byDefault){
		try{ 
			return Boolean.parseBoolean(this.getString(property));
		}catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
					 this.getClass().getName(),
					 "getOptionalBooleanProperty",
					 this.baseName+": Optional boolean property "+property+" not present");
		}
		return byDefault;
	}
	
	public Level getLogLevelProperty(String property){
			try{	
				return Level.parse(this.getString(property));
			}catch (Exception e){
				LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
						 this.getClass().getName(),
						 "getLogLevel",
						 this.baseName+": Mandatory (log) Level property "+property+" is missing");
			}
			return null;
	}
	public Level getOptionalLogLevelProperty(String property, Level byDefault){
		try{	
			return Level.parse(this.getString(property));
		}catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
					 this.getClass().getName(),
					 "getLogLevel",
					 this.baseName+": Optional (log) Level property "+property+"  not present");
		}
		return byDefault;
	}
	
	// A basic color name in a properties files  is a name matching the  static FIELD name in Color class
	// Ex: black, white...
	private static Pattern colorByNamePattern=Pattern.compile("\\p{Alpha}+");
	// Read R G B **Saturation** color properties from a properties  file.
	private static String unitaryPattern="(0*(?:2[0-5]\\p{Digit})|(?:1?\\p{Digit}{1,2}))";
	private static Pattern sRGBpattern = Pattern.compile(unitaryPattern+"\\p{Blank}+"+
														 unitaryPattern+"\\p{Blank}+"+
														 unitaryPattern+"\\p{Blank}+"+
														 unitaryPattern+"(\\p{Space}*)");
	/**
	 * Build a Color object from a RGB alpha String.
	 * 
	 * @param rgbsString a R G B Sat string given as 4 decimal bytes.
	 * @return a color or null if rgbs string parameter are in in really bad format. 
	 * @throws Exception if r g b s values are in a bad interval  (not in [0-255])
	 */
	private Color getSRGBColorFromString(String rgbsString) throws NumberFormatException, IllegalArgumentException {
		Color result=null;
		Matcher m = sRGBpattern.matcher(rgbsString);
		if (m.matches()){
			// sRGBpattern has 5 groups. Group 0 is the overall match, but last group (extra space) 
			// is *never* returned by m.group(..) ! Bug ?
			// That is why we add this extra group (\\p{Space}*) to be able to get last (4th) byte group.
			result=new Color(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), 
					Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
		}
		return result;
	}
	
	public Color getOptinalColorProperty(String property, Color byDefault){
		Color result=null;
		String propStr;
		try{ 
			propStr=this.getString(property);
			if (colorByNamePattern.matcher(propStr).matches()){
				try {
					result=(Color)Color.class.getField(propStr).get(Color.class);
				}catch(Throwable all){
					LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
							this.getClass().getName(),
							"getOptionalColorProperty",
							this.baseName+": Optional Color property "+property+"="+propStr+" not basic color name format nor rgbs)");
				}
			}else{
				try{
					result=getSRGBColorFromString(propStr);
				}catch(NumberFormatException nfe){
					LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
							this.getClass().getName(),
							"getOptionalColorProperty",
							this.baseName+": Optional Color property "+property+" bad R G B S format (not bytes ?)");
				}catch(IllegalArgumentException iae){
					LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
							this.getClass().getName(),
							"getOptionalColorProperty",
							this.baseName+": Optional Color property "+property+" bad R G B S format (not bytes ?)");
				}catch (Exception e){
					LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
							this.getClass().getName(),
							"getOptionalColorProperty",
							this.baseName+": Optional Color property "+property+" other error (internal ?)");
				}
			}
		}catch (MissingResourceException mre){
			LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
					this.getClass().getName(),
					"getOptionalColorProperty",
					this.baseName+": Optional Color property "+property+" not present");
		}
		if (result==null){
			LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
					 this.getClass().getName(),
					 "getOptionalColorProperty",
					 this.baseName+": Optional Color property "+property+" really bad color format (not a basic name or rgbs? see java.awt.Color)");
			result=byDefault;
		}
		return result;
	}
	
	private static Pattern dimensionPattern = Pattern.compile(
 		"(\\d+)\\p{Blank}+(\\d+)(\\p{Space}*)"); 
	/**
	 * Build a Dimension object for an width height string 
	 * 
	 * @param dimString a width height string given as 2 decimal integers.
	 * @return a Dimension object or null if dimension string parameter are in in really bad format. 
	 * @throws NumberFormatException if numbers (width height) are in a bad format
	 * @throws IllegalArgumentException if overall dimension string width height have a bad format.
	 * Remark: usable for a display point given as a couple of integers: x y
	 */
	private Dimension getDimensionFromString(String dimString) throws NumberFormatException, IllegalArgumentException {
		Dimension result=null;
		Matcher m = dimensionPattern.matcher(dimString);
		if (m.matches()){
			// dimensionPattern has 3 groups. Group 0 is the overall match, but last group (3, extra spaces) is never 
			// returned by m.group(..) ! Bug ?
			// That is why we add this extra 3rd  group (\\p{Space}*) to be able to get last (2nd)  group... Simple no ?
			result=new Dimension(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
		}else{
			throw new IllegalArgumentException();
		}
		return result;
	}
	/** Find a dimension (width, height)  from a property key prefix.
	 * 
	 * @param property  a prefix, key.width and key.height will be looked-up.
	 * @param def a default dimension if none can be built from properties file
	 * @return a dimension.
	 */
	public Dimension getOptionalDimensionProperty(String property, Dimension def){
		Dimension res=null;
		try{
			res=this.getDimensionFromString(this.getString(property));
		}catch (NumberFormatException nfe){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					this.getClass().getName(),
					"getOptionalDimensionProperty",
					this.baseName+": Optional Dimension property "+property+" has integer with bad format");
		}catch(IllegalArgumentException iae){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					this.getClass().getName(),
					"getOptionalDimensionProperty",
					this.baseName+": Optional Dimension property "+property+" has a really bad format");
		}catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
				 this.getClass().getName(),
				 "getOptionalDimensionProperty",
				 this.baseName+": Optional Dimension property "+property+" is missing");
		}
		if(res==null){
			res=def;
		}
		return res;
	}
	
	/**
	 *  Find a dimension (width, height)  from a property.
	 * 
	 * No exception are caught..... NumberFormatException and some other runtime exception might be raised. 
	 * @param property. A responding width  height string will be parsed. 
	 * @return a dimension or null (very bad dimension value)
	 */
	public Dimension getDimensionProperty(String property){
		try{
			return getDimensionFromString(this.getString(property));
		}catch (NumberFormatException nfe){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					this.getClass().getName(),
					"getDimensionProperty",
					this.baseName+": Mandatory Dimension property "+property+" has integer with bad format");
		}catch(IllegalArgumentException iae){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					this.getClass().getName(),
					"getDimensionProperty",
					this.baseName+": Mandatory Dimension property "+property+" has a really bad format");
		}catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
				 this.getClass().getName(),
				 "getDimensionProperty",
				 this.baseName+": Mandatory Dimension property "+property+" is missing");
		}
		return null;
	}
	/*
	private static Pattern intPointPattern = Pattern.compile(
		"(\\d+)\\p{Blank}+(\\d+)\\p{Blank}+(\\d+)\\p{Blank}+(\\p{Space}*)"); 
	*/
	private static Pattern intPointPattern=dimensionPattern;
	/**
	 * Build a integer Point  object for an x y  string 
	 * 
	 * @param pointString a width height string given as 2 decimal integers.
	 * @return a Dimension object or null if dimension string parameter are in in really bad format. 
	 * @throws NumberFormatException if numbers (width height) are in a bad format
	 * @throws IllegalArgumentException if overall dimension string width height have a bad format.
	 * Remark: usable for a display point given as a couple of integers: x y
	 */
	private Point getIntPointFromString(String pointString) throws NumberFormatException, IllegalArgumentException {
		Point result=null;
		Matcher m = intPointPattern.matcher(pointString);
		if (m.matches()){
			// intPointPattern has 3 groups. Group 0 is the overall match, but last group (3, extra spaces) is never 
			// returned by m.group(..) ! Bug ?
			// That is why we add this extra 3rd  group (\\p{Space}*) to be able to get last (2nd)  group... Simple no ?
			result=new Point(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
		}else{
			throw new IllegalArgumentException();
		}
		return result;
	}
	/** Find an optional integer point  (x, y)  from a property.
	 * 
	 * @param property.  The  corresponding x y  string will be parsed from it.
	 * @param def a default Point if none can be built from properties file
	 * @return a dimension.
	 */
	public Point getOptionalIntPointProperty(String property, Point def){
		Point res=null;
		try{
			res=this.getIntPointFromString(this.getString(property));
		}catch (NumberFormatException nfe){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					this.getClass().getName(),
					"getOptionalIntPointProperty",
					this.baseName+": Optional integer Point property "+property+" has integer with bad format");
		}catch(IllegalArgumentException iae){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					this.getClass().getName(),
					"getOptionalIntPointProperty",
					this.baseName+": Optional integer Point property "+property+" has a really bad format");
		}catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.WARNING, 
				 this.getClass().getName(),
				 "getOptionalIntPointProperty",
				 this.baseName+": Optional integer Point property "+property+" is missing");
		}
		if(res==null){
			res=def;
		}
		return res;
	}
	
	/**
	 *  Find a integer point  (x, y)  from a property.
	 * 
	 * No exception are caught..... NumberFormatException and some other runtime exception might be raised. 
	 * @param property. A x y string will be parsed from the corresponding string.
	 * @return a dimension or null (very bad dimension value)
	 */
	public Point getIntPointProperty(String property){
		try{
			return getIntPointFromString(this.getString(property));
		}catch (NumberFormatException nfe){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					this.getClass().getName(),
					"getIntPointProperty",
					this.baseName+": Mandatory integer Point property "+property+" has integer with bad format");
		}catch(IllegalArgumentException iae){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
					this.getClass().getName(),
					"getIntPointProperty",
					this.baseName+": Mandatory integer Point property "+property+" has a really bad format");
		}catch (Exception e){
			LogManager.getLogManager().getLogger("").logp(Level.SEVERE, 
				 this.getClass().getName(),
				 "getIntPointProperty",
				 this.baseName+": Mandatory integer Point property "+property+" is missing");
		}
		return null;
	}
	
	public String dump(){
		String result="---- Preferences("+(this.prefFilePath==null?"**no preferences file**":this.prefFilePath)+")\n";
		for (String key:this.prefs.keySet()){
			result+=(key+"="+this.prefs.get(key)+"\n");
		}
		result+="--- Bundle("+this.baseName+")\n";
		for (Enumeration<String> keys=this.configuration.getKeys(); keys.hasMoreElements();){
			String k=(String)keys.nextElement();
			result+=(k+"="+this.configuration.getString(k)+"\n");
		}
		return result;
	}

	/** Get property as String checking first in local preferences. 
	 * @param key property key
	 */
	public String getString(String key) throws NullPointerException, 
		MissingResourceException, ClassCastException{
		//if (!key.endsWith(EDITABLE_KEY_SUFFIX) && propKeys.contains(key+EDITABLE_KEY_SUFFIX)){
			String result=null;
			if (this.takeSystemPropertyFirst(key)){
				result=System.getProperty(key);
				if (result!=null){
					Logger.getLogger("").logp(Level.INFO,
							this.getClass().getName(), "getString", "get property "+key+" from System properties"); //TODO externalize message.
					return result;
				}
			}
			result=prefs.get(key);
			if (result==null){
				result=this.configuration.getString(key);
			}
			if (result==null){
				throw new MissingResourceException("", this.getClass().getName(), key);
			}
			return result;
	}
	
	/** Get a list of string for a simple property string.
	 * 
	 * @param key string key in properties file
	 */
	public String[] getStringList(String key){
		return getStringList(key, this.getStringListSeparator());
	}
	/** Get a list of string for a simple property string using default list separator.
	 * 
	 * @param key string key in properties file
	 * @param defaultList a default string list as a String array. 
	 */
	public String[] getOptionalStringList(String key, String[] defaultValue){
		try {
			return getStringList(key);
		}catch(Throwable thr){
			return defaultValue;
		}
	}
	/** Get a list of strings for a simple property string.
	 * 
	 * @param key string key in properties file
	 * @param stringListSeparator2 list separator to use.
	 */
	public String[] getStringList(String key, char stringListSeparator2) {
		Vector<String> result=new Vector<String>();
		String listString=this.getString(key);
		int startIdx=0; 
		int endIdx=0;
		while (startIdx>=0){
			endIdx=listString.indexOf(stringListSeparator2, startIdx);
			if (endIdx>=0){
				result.add(listString.substring(startIdx, endIdx));
				startIdx=endIdx+1;// skip separator !
			}else{
				result.add(listString.substring(startIdx)); // take all remaining chars from string.
				startIdx=endIdx; // we are done.
			}
			
		}
		return result.toArray(new String[result.size()]);
	}
	
	/** Get a list of string for a simple property string using default list separator.
	 * 
	 * @param key string key in properties file
	 * @param defaultList a default string list as a String array. 
	 * @param stringListSeparator2 list separator to use.
	 */
	public String[] getOptionalStringList(String key, String[] defaultValue, char stringListSeparator2){
		try {
			return getStringList(key, stringListSeparator2);
		}catch(Throwable thr){
			return defaultValue;
		}
	}
	public void setPreferencesChanged(boolean preferencesChanged) {
		this.preferencesChanged = preferencesChanged;
	}
	public boolean isPreferencesChanged() {
		return preferencesChanged;
	}

	public void setStringListSeparator(char stringListSeparator) {
		this.stringListSeparator = stringListSeparator;
	}
	public char getStringListSeparator() {
		return stringListSeparator;
	}
	/**
	 * A an observer for a given key
	 * @param k the property (preference) key to observe for changes
	 * @param observer the observer
	 */
	public void addKeyObserver(String k, Observer observer){
		PropertyObservable observable=this.keyToObservable.get(k);
		if (observable==null){
			observable=new PropertyObservable();
			observable.addObserver(observer);
			this.keyToObservable.put(k, observable);
		}else{
			observable.addObserver(observer); // see Observable.addObserver comment: double are filtered.
		}
	}
	/**
	 * Add an observer which will be informed of any change on any property (or preferences).
	 * 
	 * @param observer
	 */
	public void addGlobalObserver(Observer observer){
		assert observer!=null:"null observer, Observable.addObserver() does not like it";
		if (this.globalObservable==null){
			this.globalObservable=new PropertyObservable();
		}
		this.globalObservable.addObserver(observer); // care of doubles...
	}
	
	/**
	 * Remove an observer from any observed key
	 * 
	 * Remark: does not remove parameter from global observers.
	 * 
	 * @param observer observer to remove.
	 */
	public void removeObserverFromAllKeys(Observer observer){
		//We use explicite Iterator loop has we may delete some entry with
		// no more observer in keysWithNotObserver
		// Java documentation seems clear: .values() is backed by the Map and iterator.remove should do the Job... 
		for (Iterator<PropertyObservable> observablesIterator=this.keyToObservable.values().iterator();
			 observablesIterator.hasNext();){
			PropertyObservable observable=observablesIterator.next();
			observable.deleteObserver(observer);
			if (observable.countObservers()<=0){
				observablesIterator.remove();
			}
		}
		//TODO: remove globalObserver ?
	}
	/**
	 * Remove observer for a given key.
	 * @param key
	 * @param observer
	 */
	public void removeObserver(String key, Observer observer){
		assert key!=null:"null property key";
		PropertyObservable observable=this.keyToObservable.get(key);
		if (observable!=null){
			observable.deleteObserver(observer);
			if (observable.countObservers()<=0){
				this.keyToObservable.remove(key);
			}
		}else{
			Logger.getLogger("").logp(Level.WARNING,
					this.getClass().getName(), "removeObserver", key+" has no observer"); //TODO externalise message.
		}
	}
	/**
	 * Remove one global observer
	 * @see #addGlobalObserver(Observer)
	 * @param observer
	 */
	public void removeGlobalObserver(Observer observer){
		assert observer!=null:"null observer, Observable.addObserver() does not like it";
		if (this.globalObservable!=null){
			this.globalObservable.deleteObserver(observer);
			if (this.globalObservable.countObservers()<0){
				this.globalObservable=null;
			}
		}
	}
	
	private void notifyChange(String k, String newValue, String oldValue){
		PropertyObservable observable=this.keyToObservable.get(k);
		PropertyChangedEvent evt=null; //PropertyChangedEvent is unmutable => can be shared by several observables...
		if (observable!=null){
			evt=new PropertyChangedEvent(this, k, newValue, oldValue);
			observable.setChangedAndNotifyObservers(evt);
		}
		if (this.globalObservable!=null){
			if (evt==null){
				evt=new PropertyChangedEvent(this, k, newValue, oldValue);
			}
			this.globalObservable.setChangedAndNotifyObservers(evt);
		}
	}
	
	private ResourceBundle configuration = null;
	private String baseName=null; // for logging
	private String prefFilePath;
	private HashMap<String, String> prefs; //HashMap: null must not be a value. TODO: use java.util.Properties ?
	private boolean preferencesChanged=false;
	private char stringListSeparator=STRING_LIST_SEPARATOR;
	// Global observer for the one who wants to keep track to changes on any property kketys.
	private PropertyObservable globalObservable=null;
	// key -> Observable, so one Observable keeps all observers for one key. 
	private HashMap<String, PropertyObservable> keyToObservable=new HashMap<String, PropertyObservable>();
	// Map to keep instantiated Properties object...
	private static HashMap<String, Properties> builtProperties=new HashMap<String, Properties>();

	public void setPreference(String key, Color c) {
		this.setPreference(key, Integer.toString(c.getRed())+" "+Integer.toString(c.getGreen())+" "+
				                Integer.toString(c.getBlue())+" "+Integer.toString(c.getAlpha()));
	}
	/**
	 * Set a Dimension preference
	 * @param key
	 * @param size
	 * @see #getDimensionProperty(String)
	 * @see #getOptionalDimensionProperty(String, Dimension)
	 */
	public void setPreference(String key, Dimension size) {
		this.setPreference(key, Integer.toString(size.width)+" "+Integer.toString(size.height));
	}
	/**
	 * Set a Point preference
	 * @param key
	 * @param point
	 * @see #getIntPointProperty(String)
	 * @see #getOptionalIntPointProperty(String, Point)
	 */
	public void setPreference(String key, Point point) {
		this.setPreference(key, Integer.toString(point.x)+" "+Integer.toString(point.y));
	}
}

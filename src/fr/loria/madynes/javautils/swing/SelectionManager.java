package fr.loria.madynes.javautils.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.text.MessageFormat;

/**
 * Manager selection for a given C container (ex subclass of JPanel). The selected
 * E element (JComponent) has to be be owned by the container.
 * 
 * Element E (JComponent) of the container will be selected/deselected, dragged when needed. 
 * 
 *   Some MouseListener and MouseMotionListener may be added to container and elements.
 *   
 *   @see Selectionable<Elt extends JComponent>
 *   
 * @author andrey
 *
 * @param <C> Class of the container
 * @param <E> Class of the managed elements.
 */
public class SelectionManager<C extends JComponent, E extends JComponent> { // Container, Selectionable elts.
	private Set<E> selectedElts=new HashSet<E>();
	private Set<E> managedElts=new HashSet<E>(); // For easy selectAll support. NOT EFFICENT. IMPROVE (visitor on container ? ....)
	private Selectionable<C,E> selectionable;
	//private C container;
	// see ContainerMouseListener and ContainerMouseMouseMotionListener
	private int selectionRectStartX;
	private int selectionRectStartY;
	
	private Cursor moveCursor;
	private Cursor pressedCursor;
	private C container;
	
	// For Elt drag... Not in EltMouseListener, nor EltMouseMotionListener
	// To avoid to create on mouselistener par elt and to avoid 
	// to recreate mouse motion listener many times.
	// We assume the is ONE mouse and only mouse (wasa double touch ?).
	/* NO... No optimization for now. Keep it simple and bulletproof...
	private boolean mousePressed;
	private int startDragX;
	private int startDragY;
	*/
	/**
	 * Create a SelectionManager for a given container.
	 * 
	 * A mouse listener will be added to the container.
	 * 
	 * TODO: add a removeContainer ? and propagate effect to Selectionable interface.
	 * @param container, a container to be managed
	 * @param s complementary operations given by delegation...
	 * @param moveCursor cursor to display when an element is moved
	 * @param pressedCursor cursor to display when an element is pressed (mouse is pressed in..)
	 */
	public  SelectionManager(C container,  
							 Selectionable<C,E> s, 
							 Cursor moveCursor, 
							 Cursor pressedCursor){
		this.selectionable=s;
		s.workFor(this); // this could have no effect. But it may be useful for some Selectionable implementation. See SelectionableAdapter.
		this.moveCursor=moveCursor;
		this.pressedCursor=pressedCursor;
		this.setContainer(container);
		container.addMouseListener(new ContainerMouseListener()); // ContainerMouseListener in an inner class
																  // => one instance per SelectionManager and managed container.
	}
	
	public void addToSelection(E e){
		assert managedElts.contains(e):"can not select an unmaged elt"; 
		this.selectedElts.add(e);
		this.selectionable.displaySelectionStatus(e, true);
	}
	public void removeFromSelection(E e){
		assert managedElts.contains(e):"can not unselect an unmaged elt"; 
		// Check membership ? Avoid for optimization...
		this.selectedElts.remove(e);
		this.selectionable.displaySelectionStatus(e, false);
	}
	
	public void toggleSelection(E e){
		assert managedElts.contains(e):"can not (un)select an unmaged elt"; 
		if (this.selectedElts.contains(e)){
			this.removeFromSelection(e);
		}else{
			this.addToSelection(e);
		}
	}
	/**
	 * Notice the Selection manager that an element  must be selected/deselected
	 * under its responsibility.
	 * 
	 * Remark: on element (JComponent...) must be in ONE Selection Manager AT MOST. 
	 * (all  dedicated EltMouseListener are clean in {@link #unmanage(JComponent)}.
	 * This method adds a MouseListener to the component. 
	 * @param e the element to managed
	 */
	public void manage(E e){
		this.managedElts.add(e);
		e.addMouseListener(new EltMouseListener());
	}
	
	/**
	 * Notice the selection manager that an element must not be selected/deselected under its responsibility any more. 
	 * @param e the element to withdraw from responsibility.
	 */
	public void unmanage(E e){
		assert managedElts.contains(e):"can not unmaged an unmaged elt"; 
		this._unmanage(e);
		this.managedElts.remove(e);
	}
	
	private void _unmanage(E e){
		this.removeFromSelection(e);
		for (MouseListener ml:e.getMouseListeners()){
			if (ml instanceof SelectionManager.EltMouseListener){
				// instanceof is not working on parameterized types as they are erased at run time ! That works...
				e.removeMouseListener(ml);
			}
		}
	}
		
	/**
	 * Unmanaged all managed elements
	 * @see #manage(JComponent)
	 * @see #unmanage(JComponent)
	 */
	public void unmanageAll(){
		for (E e:this.managedElts){
			this._unmanage(e); // does NOT update this.managedElts...
		}
		// this.unmanage updates this.managedElts that would be empty at this point : NO NO see _unmanage(..). We have current update trouble is this.managedElts....
		//assert this.managedElts.isEmpty():"this.managedElts is not empty at the end !!!";
		this.managedElts.clear();
	}
	/** 
	 * Deleselect all currently selected elements
	 */
	public void deselectAll(){
		for (E e:this.selectedElts){
			this.selectionable.displaySelectionStatus(e, false);
		}
		this.selectedElts.clear();
		//this.container.repaint();
	}
	
	/**
	 * Select all managed elements.
	 */
	public void selectAll(){
		for (E e:this.managedElts){
			this.addToSelection(e);
		}
	}
	
	private void setContainer(C container) {
		this.container = container;
	}

	/**
	 * Get the container this manager manages elements for selection and dragging.
	 * @return the container (JComponent)
	 */
	public C getContainer() {
		return container;
	}
	
	private void translateSelection(int dx, int dy){
		for (E e:this.selectedElts){
			this.selectionable.translate(e, dx, dy);
		}
		//this.container.repaint();
	}
	/**
	 * 
	 * @author andrey
	 *
	 * @param <E>
	 */
	public interface Selectionable<C extends JComponent, E extends JComponent> {
		/**
		 * Inform container with selectionable elements that the selection
		 * rectangle (tracking Button1 down+drag) has changed.
		 * 
		 * Guaranteed:  top-left and bottom-right corners. Selection rectangle is active (must be drawn).
		 * @param xLeft
		 * @param yTop
		 * @param xRight
		 * @param yBottom
		 */
		void changeSelectionRect(int xLeft, int yTop,int xRight, int yBottom);
		/** Translate one selected element.
		 * 
		 * One element is to be drag.
		 * 
		 * @param e the element to translate
		 * @param dx horizontal translation
		 * @param dy vertical translation
		 */
		void translate(E e, int dx, int dy);
		/**
		 *  Inform container with selectionable elements that where no more
		 *  selection rectangle to display.
		 */
		void killSelectionRect();
		
		/**
		 * Change the display of one element according to its selection status.
		 * 
		 * @param e the element which display must be updated
		 * @param selected true if the element is now selected, false if it is now deselected.
		 */
		void displaySelectionStatus(E e, boolean selected);
	
		/**
		 * Ask the container with selectionable elements to add to selection
		 * the one in the indicated selection rectangle.
		 *  Guaranteed  top-left and bottom-right corners.
		 * @param xLeft
		 * @param yTop
		 * @param xRight
		 * @param yBottom
		 */
		void selectComponentsInSelectionRectangle(int xLeft, int yTop,int xRight, int yBottom);
	
		/** 
		 * OPTIONAL. Get informed that we work for a given manager.
		 * This method could do nothing...
		 * Call by SlectionManager constructor ({@link SelectionManager.SelectionManager()})
		 * @param sm  a manager we work for.
		 */
		void workFor(SelectionManager<C,E> sm);
	}

	public static abstract class SelectionableAdapter <C extends JComponent, E extends JComponent> implements Selectionable<C,E>{
		//private C container;
		// For drawing selection rectangle
		boolean drawSelectionRect=false;
		private int selectionRectXLeft;
		private int selectionRectYTop;
		private int selectionRectXRight;
		private int selectionRectYBottom;
		
		private SelectionManager<C,E> selectionMgr; // need for addToSelection...
		public SelectionableAdapter(){ // Can not give manager at construction time... See SelectionManager constructor... :-/
			//this.container=c;
		}
		protected C getContainer(){
			return this.selectionMgr.getContainer();
		}
		
		/**
		 * To be call by the container paint() or  paintComponent() method. 
		 * @see #getContainer() 
		 * @param g
		 */
		public void drawSelectionRect(Graphics g){
			if (this.drawSelectionRect){
				g.setColor(Color.LIGHT_GRAY);
				g.drawRect(this.selectionRectXLeft, this.selectionRectYTop, 
						   this.selectionRectXRight-this.selectionRectXLeft, 
						   this.selectionRectYBottom-this.selectionRectYTop);
			}
		}
		
		@Override
		public void changeSelectionRect(int xLeft, int yTop, int xRight,
				int yBottom) {
			this.drawSelectionRect=true;
			this.selectionRectXLeft=xLeft;
			this.selectionRectYTop=yTop;
			this.selectionRectXRight=xRight;
			this.selectionRectYBottom=yBottom;
			this.getContainer().repaint();
		}
		@Override
		public void killSelectionRect() {
			this.drawSelectionRect=false;
			this.getContainer().repaint();
		}
		@Override
		public void selectComponentsInSelectionRectangle(int xLeft, int yTop, int xRight, int yBottom){
			assert this.selectionMgr!=null:"Null selectiogr, use workFor method";
			for (E e:this.selectionMgr.managedElts){ // <<================== This adaptor is a big friend of SelectionManager....
				// E extends JComponent....
				int x=e.getX();
				int y=e.getY();	
				if((xLeft<x)&&(x+e.getWidth()<xRight)&&(yTop<y)&&(y+e.getHeight()<yBottom)){
					this.selectionMgr.addToSelection(e);
				}
			}
		}
		
		@Override
		public void translate(E e, int dx, int dy) {
			e.setLocation(e.getX()+dx, e.getY()+dy);
		}
		
		@Override
		public void workFor(SelectionManager<C,E> sm){
			this.selectionMgr=sm;
		}
	}
	private class ContainerMouseListener extends MouseAdapter {	
		public void mousePressed(MouseEvent e) {
			if (e.getButton()==MouseEvent.BUTTON1){
				if (!e.isShiftDown()){
					deselectAll();
				}
				Component cp=e.getComponent(); // Use this.ontainer ?
				//cp.addMouseMotionListener(new _MouseMotionListener());	
				cp.addMouseMotionListener(mouseMotionListener);
				//drawSelectionRect=true;
				selectionRectStartX=e.getX();
				selectionRectStartY= e.getY();
				//selectionRectX2=selectionRectStartX;
				//selectionRectY2=selectionRectStartY;
				selectionable.changeSelectionRect(selectionRectStartX, selectionRectStartY, 
												  selectionRectStartX, selectionRectStartY);
			}
			//System.out.println(e);
		}
		public void mouseReleased(MouseEvent e){ 
			Component cp=e.getComponent();// cp is outer this !
			// Just clean up our listener
			cp.removeMouseMotionListener(mouseMotionListener);
			int x=e.getX(); // cp-> e
			int y=e.getY(); // cp ->e
			int xLeft;
			int yTop;
			int xRight;
			int yBottom;
			// ensure lefttop-rightbottom corners
			if (selectionRectStartX<x){
				xLeft=selectionRectStartX;
				xRight=x;
			}else{
				xLeft=x;
				xRight=selectionRectStartX;
			}
			if (selectionRectStartY<y){
				yTop=selectionRectStartY;
				yBottom=y;
			}else{
				yTop=y;
				yBottom=selectionRectStartY;
			}
			selectionable.selectComponentsInSelectionRectangle(xLeft, yTop, xRight, yBottom);
			selectionable.killSelectionRect();
			//cp.repaint(); // remove selection rectangle
			//System.out.println(e);
		}
	}//ContainerManagerMouseListener
	
	private ContainerMouseMotionListener mouseMotionListener=new ContainerMouseMotionListener();
	private class ContainerMouseMotionListener extends MouseMotionAdapter {
	    public void mouseDragged(MouseEvent e) {
	    	int x=e.getX();
			int y=e.getY();
			int xLeft;
			int yTop;
			int xRight;
			int yBottom;
			// We guarantee left-top right-bottom corners... 
			if (selectionRectStartX<x){
				xLeft=selectionRectStartX;
				xRight=x;
			}else{
				xLeft=x;
				xRight=selectionRectStartX;
			}
			if (selectionRectStartY<y){
				yTop=selectionRectStartY;
				yBottom=y;
			}else{
				yTop=y;
				yBottom=selectionRectStartY;
			}
	    	selectionable.changeSelectionRect(xLeft, yTop, xRight, yBottom);
	    }
	}// ContainerMouseMotionListener
	
	private class EltMouseListener extends MouseAdapter {	
		private boolean mousePressed=false;
		private EltMouseMotionListener mml; // Keep it to remove it...x
		public void mousePressed(MouseEvent evt) {
			if (evt.getButton()==MouseEvent.BUTTON1){
				E e=(E)evt.getComponent();
				e.setCursor(pressedCursor);
				mousePressed=true;
				boolean isSelected=false;
				if (evt.isControlDown()){
					toggleSelection(e);
					isSelected=selectedElts.contains(e);
				}else{	
					isSelected=selectedElts.contains(e);
					if (!(evt.isShiftDown()||isSelected)){
						// Not shift and not click in selected elt...
						deselectAll();
					}
					if (!isSelected){
						addToSelection(e);	
					}
					isSelected=true;
				}
				if (isSelected){
					if (this.mml==null){
						this.mml=new EltMouseMotionListener(evt.getX(), evt.getY());
					}else{
						this.mml.setStartDragXY(evt.getX(), evt.getY());
					}
					e.addMouseMotionListener(mml);	
				}
			}
			assert evt.getComponent().getMouseMotionListeners().length<=1:"Number of MouseMotinListener > 1 !";
		}
		public void mouseReleased(MouseEvent e){ 
			Component cp=e.getComponent();
			cp.removeMouseMotionListener(mml);
			mousePressed=false;
			e.getComponent().setCursor(moveCursor);
			
		}
		 public void mouseEntered(MouseEvent e) {
			 e.getComponent().setCursor((mousePressed)?pressedCursor:moveCursor);
		 }

		 public void mouseExited(MouseEvent e) {
			 e.getComponent().setCursor(Cursor.getDefaultCursor());
		 }
	}//EltMouseListener
	
	private class EltMouseMotionListener extends MouseMotionAdapter {
		private int startDragX;
		private int startDragY;
		private EltMouseMotionListener(int startDragX, int startDragY){
			this.startDragX=startDragX;
			this.startDragY=startDragY;
		}

	    private void setStartDragXY(int startDragX, int startDragY) {
	    	this.startDragX=startDragX;
	    	this.startDragY=startDragY;
		}

		public void mouseDragged(MouseEvent e) {
	    	//translate(e.getX()-startX, e.getY()-startY);
	    	//getMemoryView().repaint(); // for arrow and other things...
	    	translateSelection(e.getX()-startDragX, e.getY()-startDragY);
	    }
	}// MouseMotionListener
	
	
	
	
	
	
	
	
	
	
	
	
	
	// Poor test
	
	public static void main(String[] args){
		Logger.getLogger("").setLevel(Level.FINE);
		Logger.getLogger("").setFilter(new Filter(){
			@Override
			public boolean isLoggable(LogRecord record) {
				//System.out.println(record+" "+record.getLevel());
				return record.getLevel().intValue()>=Logger.getLogger(record.getLoggerName()).getLevel().intValue();
			}
		});
		Logger.getLogger("").addHandler(new Handler(){
			@Override
			public void close() throws SecurityException {
			}
			@Override
			public void flush() {
				System.out.flush();
			}
			@Override
			public void publish(LogRecord record) {
				System.out.println(record.getLevel()+" "+record.getSourceClassName()+" "+record.getMessage());
			}
		});

		final JFrame f=new JFrame();
		final TestPanel p=new TestPanel();
		TestSelectionable<TestPanel,JLabel> ts=new TestSelectionable<TestPanel,JLabel>();
		final SelectionManager<TestPanel,JLabel> sm=new SelectionManager<TestPanel, JLabel>(p, ts,
						Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), 
						Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		p.setTs(ts);
		f.setSize(500, 500);
		f.setPreferredSize(new Dimension(500, 500));
		f.getContentPane().add(p);
		f.pack();
		f.setVisible(true);
		p.populated(sm);
		p.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton()==MouseEvent.BUTTON3){
					for (Component c:p.getComponents()){
						sm.addToSelection((JLabel)c);
					}
				}
			}
		});
		
		f.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				e.getWindow().dispose();
			}

			@Override
			public void windowClosed(WindowEvent e) {
				sm.unmanageAll();
			}
		});
		
	}
	
	static private class TestPanel extends JPanel{
		private TestSelectionable<TestPanel,JLabel> ts;
		private TestPanel(){
			this.setLayout(null);
		}
		private void populated(SelectionManager<TestPanel,JLabel> sm){
			for (int i=0; i<20; i++){
				JLabel l=new JLabel("truc#"+i);
				//l.setLocation(i*20, i*20);
				l.setBounds(i*20, i*20, 60, 26);
				sm.manage(l);
				this.add(l);
			}
		}
		@Override
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			ts.drawSelectionRect(g);
			
		}
		public void setTs(TestSelectionable<TestPanel,JLabel> ts) {
			this.ts = ts;
		}
		public TestSelectionable<TestPanel,JLabel> getTs() {
			return ts;
		}
	}
	static private class TestSelectionable<C extends TestPanel, E extends JLabel> extends SelectionableAdapter<C, E> {
		private Border normalBorder=BorderFactory.createLineBorder(Color.black);		
		private Border selectedBorder=BorderFactory.createLineBorder(Color.black, 4);
		@Override
		public void displaySelectionStatus(E e, boolean selected) {
		//	System.out.println(MessageFormat.format("displaySelectionStatus {0}", selected));
			e.setBorder(selected?this.selectedBorder:this.normalBorder);
		}
	}
}

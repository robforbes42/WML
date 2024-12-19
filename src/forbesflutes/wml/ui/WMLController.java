/**
 *  Author: Rob Forbes Nov 2024
 */
package forbesflutes.wml.ui;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import forbesflutes.wml.WMLFileModel;
import forbesflutes.wml.WMLParser;
import forbesflutes.wml.ui.WMLSketchPanel;

class WMLController implements ChangeListener, ActionListener {
   // Constants:
   private static final long serialVersionUID = 1L;
   private static final String title = "Woodwind Measurement Language (WML) Tools";
   // Tab names:
   private static final String EDIT = "Edit";
   private static final String BORES = "Bores";
   private static final String SKETCH = "Sketch";
   private static final String BROWSE = "Browse";
   // File Menu:
   private static final String FILE = "File";
   private static final String NEW = "New";
   private static final String OPEN = "Open";
   private static final String REFRESH = "Refresh";
   private static final String SAVE = "Save";
   private static final String SAVE_AS = "Save As";
   private static final String SAVE_IMAGE = "Save Image";
   private static final String PRINT = "Print";
   private static final String CONVERT_UNITS = "Convert Units";
   private static final String EXPORT = "Export TSV";
   private static final String EXIT = "Exit";
   // Edit Menu:
   private static final String CUT = "Cut";
   private static final String COPY = "Copy";
   private static final String PASTE = "Paste";
   // Help Menu:
   private static final String HELP = "Help";
   private static final String EXAMPLES = "Examples";
   private static final String SPEC = "WML Specification";
   private static final String ABOUT = "About";
   
   // Menu items
   JMenuItem newItem = new JMenuItem(NEW);
   JMenuItem openItem = new JMenuItem(OPEN);
   JMenuItem refreshItem = new JMenuItem(REFRESH);
   JMenuItem printItem = new JMenuItem(PRINT);
   JMenuItem saveItem = new JMenuItem(SAVE);
   JMenuItem saveAsItem = new JMenuItem(SAVE_AS);
   JMenuItem saveImageItem = new JMenuItem(SAVE_IMAGE);
   JMenuItem convertItem = new JMenuItem(CONVERT_UNITS);
   JMenuItem exportItem = new JMenuItem(EXPORT);
   
   // Frame and top-level UI components:
   private JFrame frame;
   private JTabbedPane tabbedPane;
   private JMenuBar menuBar;
   JMenu editMenu;

   private WMLFileModel model = new WMLFileModel();
   private File fileChooserPath = null;

   // Tab panels:
   WMLEditPanel editPanel = new WMLEditPanel(model, this);
   WMLBoresPanel boresPanel = new WMLBoresPanel(model, this);
   WMLSketchPanel sketchPanel = new WMLSketchPanel(model, this);
   WMLBrowsePanel browsePanel = new WMLBrowsePanel(model, this);
   WMLPanel[] panels = new WMLPanel[] {editPanel, boresPanel, sketchPanel, browsePanel};
   WMLPanel selectedPanel;
   WMLPanel previousPanel;
   
   /** Top-level controller for the application. Constructs the main frame, the menus,
    *  and the tabs and handles actions for menus (mostly by delegation) and tab changes. 
    */
   public WMLController() {
      // build the frame:
      frame = new JFrame(title);
      tabbedPane = new JTabbedPane();
      frame.getContentPane().add(tabbedPane);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(700, 700);      
      try {
         UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
         MetalLookAndFeel.setCurrentTheme(new OceanTheme());
      } catch (Exception e) {}
      
      // create the menus
      menuBar = new JMenuBar();
      frame.setJMenuBar(menuBar);
      createFileMenu();
      createEditMenu();
      createHelpMenu();
      
      // add the tabs:
      tabbedPane.addChangeListener(this);
      addTab(EDIT, editPanel, tabbedPane);
      addTab(SKETCH, sketchPanel, tabbedPane);
      addTab(BORES, boresPanel, tabbedPane);
      addTab(BROWSE, browsePanel, tabbedPane);
      tabbedPane.setSelectedIndex(0);
   }
   
   private void addTab(String label, JPanel wimPanel, JTabbedPane tabbedPane) {
      JPanel wrapper = new JPanel(new BorderLayout());
      wrapper.setBorder(new EmptyBorder(10,10,10,10));
      wrapper.add(wimPanel);
      tabbedPane.add(label, wrapper);
   }

   public void show() {
      frame.setVisible(true);
   }

   private void addMenuItem(JMenu menu, String label) {
      JMenuItem mi = new JMenuItem(label);
      mi.addActionListener(this);
      menu.add(mi);
   }

   private void addMenuItem(JMenu menu, JMenuItem item) {
      item.addActionListener(this);
      menu.add(item);
   }

   private void createHelpMenu() {
      JMenu helpMenu = new JMenu(HELP);
      menuBar.add(helpMenu);
      addMenuItem(helpMenu, ABOUT);
   }

   private void createEditMenu() {
      editMenu = new JMenu(EDIT);
      menuBar.add(editMenu);
      addMenuItem(editMenu, CUT);
      addMenuItem(editMenu, COPY);
      addMenuItem(editMenu, PASTE);
      editMenu.setEnabled(false); // enabled when edit tab opens
   }

   private void createFileMenu() {
      JMenu menu = new JMenu(FILE);
      menuBar.add(menu);
      addMenuItem(menu, newItem);
      addMenuItem(menu, openItem);
      addMenuItem(menu, refreshItem);
      addMenuItem(menu, saveItem);
      addMenuItem(menu, saveAsItem);
      addMenuItem(menu, saveImageItem);
      addMenuItem(menu, printItem);
      addMenuItem(menu, convertItem);
      addMenuItem(menu, exportItem);
      addMenuItem(menu, EXIT);
    }

   
   public void actionPerformed(ActionEvent evt) {
      String cmd = evt.getActionCommand();
      try {
         handleAction(cmd);
      }
      catch(Exception exc) {
         JOptionPane.showMessageDialog(frame, "Error handling command " + cmd + ": " + exc.getMessage());
      }
   }

   /** Handle all the menu item actions. */
   private void handleAction(String cmd) throws Exception {
      switch(cmd) {
         // Edit Menu
         case CUT: editPanel.cut(); break;
         case COPY: editPanel.copy(); break;
         case PASTE: editPanel.paste(); break;
         // File Menu
         case NEW: editPanel.fileNew(); break;
         case OPEN: editPanel.fileOpen(); break;
         case REFRESH: editPanel.fileRefresh(); break;
         case SAVE: editPanel.fileSave(); break;
         case SAVE_IMAGE: saveImage(); break;
         case PRINT: print(); break;
         case CONVERT_UNITS: convertUnits(); break;
         case EXPORT: export(); break;
         case EXIT: exit(); break;
         // Help Menu
         case ABOUT: helpAbout(); break;
         default: JOptionPane.showMessageDialog(frame, "Unknown command " + cmd, "", JOptionPane.WARNING_MESSAGE);
      }
   }

   /** Action for the File/Convert menu item. */
   private void convertUnits() {
      File sourceFile = chooseFile();
      if(sourceFile == null) return;
      File destFile = chooseFile();
      if(destFile == null) return;
      try {
         WMLParser.convertUnits(sourceFile, destFile);
      } catch (IOException e) {
         JOptionPane.showMessageDialog(frame, "Error converting units: " + e.getMessage());
      }
      
   }

   /** Check if model is valid, and notify user if it is not. */
   boolean checkValid() {
      if(!model.isValid())
         showMessageDialog("Operation cannot be performed because file is not valid; please fix errors in editor.");
      return model.isValid();
   }
   
   private void export() throws IOException {
      if(checkValid()) {
         File file = chooseFile();
         model.getData().exportTSV(file.toString());
      }
   }

   private void print() {
      selectedPanel.doPrint();
   }
      
   private void saveImage() {
      selectedPanel.doSaveImage();
   }
   
   WMLPanel getSelectedPanel() {
      JPanel wrapper = (JPanel) tabbedPane.getSelectedComponent();
      return (WMLPanel) wrapper.getComponent(0);
   }   

   @Override
   public void stateChanged(ChangeEvent e) {
      selectedPanel = getSelectedPanel();
      if(previousPanel != null && previousPanel != selectedPanel)
         previousPanel.tabClosed();
      selectedPanel.tabOpened();
      previousPanel = selectedPanel;
   }

   /** Get a yes/no response from the user.
       A utility for all panels. */
   int showConfirmDialog(String title, String msg) {
      return JOptionPane.showConfirmDialog(frame, 
         msg, title, JOptionPane.YES_NO_OPTION);
   }

   /** Get a file choice from the user. Returns null if user cancelled. 
    *  A utility for all panels. */
   File chooseFile() {
      File file = null;
      JFileChooser chooser = new JFileChooser();
      if(fileChooserPath != null)
         chooser.setCurrentDirectory(fileChooserPath);
      if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
         file = new File(chooser.getSelectedFile().getAbsolutePath());
         fileChooserPath = file.getParentFile();
      }
      return file;
   }

   Point getImageDimensions() {
      JTextField widthField = new JTextField();
      JTextField heightField = new JTextField();
      
      final JComponent[] inputs = new JComponent[] {
           new JLabel("Width"), widthField,
           new JLabel("Height"), heightField,
      };
      int result = JOptionPane.showConfirmDialog(frame, inputs, "Enter Dimensions for Image", 
         JOptionPane.OK_OPTION);
      try {
         int width = Integer.parseInt(widthField.getText());
         int height = Integer.parseInt(heightField.getText());
         return new Point(width, height);
      } catch(NumberFormatException e) {
         JOptionPane.showMessageDialog(frame, "Illegal number format.", "", JOptionPane.ERROR_MESSAGE);
         return null;
      }
   }
   
   /** Show a message to the user.
       A utility for all panels. */
   void showMessageDialog(String msg) {
      JOptionPane.showMessageDialog(frame, msg);
   }
   
   void helpAbout() {
      showMessageDialog("WML Tools by Rob Forbes.\nVersion 0.1\nHome Page: https://www.forbesflutes.com/wim");
   }

   private void exit() {
      for(WMLPanel panel: panels)
         panel.onExit();
      frame.setVisible(false);   
   }   
    
   // Main class
   public static void main(String args[])  {
      try {
         WMLController app = new WMLController();
         app.show();
         app.editPanel.openFile("c:/temp/rf1.wim");
      } catch (Throwable e) {
         e.printStackTrace();
      }
   }

}
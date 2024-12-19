/**
 *  Author: Rob Forbes Nov 2024
 */

package forbesflutes.wml.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.horstmann.corejava.GBC;

import forbesflutes.wml.WMLFileModel;
import forbesflutes.wml.TextFileModel.TextModelListener;

public class WMLEditPanel extends JPanel implements WMLPanel, TextModelListener {
   private static final long serialVersionUID = 1L;

   protected TextEditorPanel textEditorPanel;
   protected JTextArea messageArea;
   protected JMenuBar menuBar;
   protected JFrame frame;
   JMenuItem[] myFileMenuItems;
   private WMLFileModel model; 
   private WMLController controller;
   
   public WMLEditPanel(WMLFileModel model, WMLController controller) {
      this.model = model;
      this.controller = controller;
      model.addListener(this);
      setLayout(new GridBagLayout());

      myFileMenuItems = new JMenuItem[] {
         controller.newItem, 
         controller.openItem, 
         controller.refreshItem, 
         controller.saveItem, 
         controller.saveAsItem, 
         controller.exportItem };
      
      JButton validateButton = new JButton("Validate");
      validateButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            validateModel();
         }         
      });
      add(validateButton, new GBC(0,0).fill(GBC.NONE));
      
      textEditorPanel = new TextEditorPanel(model);
      textEditorPanel.setMinimumSize(new Dimension(200, 200));
      add(textEditorPanel, new GBC(0,1).weight(1.,.5).fill(GBC.BOTH));
            
      messageArea= new JTextArea();
      messageArea.setEditable(false);
      messageArea.setMinimumSize(new Dimension(200, 200));
      JPanel errorPanel = new JPanel(new BorderLayout());
      errorPanel.add(messageArea);
      errorPanel.setBorder(BorderFactory.createCompoundBorder(            
         new TitledBorder("Validation Messages"),
         new EmptyBorder(UIUtil.smallInsets)));
      add(errorPanel, new GBC(0,2).weight(0.,.5).fill(GBC.BOTH));
   }

   protected void validateModel() {
      // respond to user pushing the Validate button
      // sync on-screen text with the model. If there are unsaved changes,
      // the resulting change notification will trigger a new parse,
      // and call modelChanged() below 
      textEditorPanel.syncWithModel();
   }
   
   @Override
   public void modelChanged() {
      // Update the message area to reflect current parse results (or none)
      if(!model.isEmpty())
         messageArea.setText(model.getParser().getAllMessages());
      else 
         messageArea.setText("");
   }

   void setMyFileMenuItemsEnabled(boolean flag) {
      for(JMenuItem item: myFileMenuItems) 
         item.setEnabled(flag);
   }

   public void tabOpened() {
      controller.editMenu.setEnabled(true);
      setMyFileMenuItemsEnabled(true);
      controller.saveImageItem.setEnabled(false);
      textEditorPanel.tabOpened();
   }
   
   public void tabClosed() {
      controller.editMenu.setEnabled(false);
      setMyFileMenuItemsEnabled(false);
      controller.saveImageItem.setEnabled(true);
      textEditorPanel.tabClosed();
   }

   void openFile(String filepath) {
      textEditorPanel.openFile(filepath);
   }
   
   // Menu item actions:
   
   void cut() {textEditorPanel.cut();}
   
   void copy() {textEditorPanel.copy();}
   
   void paste() {textEditorPanel.paste();}
   
   void fileNew() {textEditorPanel.fileNew();} 
   
   void fileOpen() {textEditorPanel.fileOpen();}
   
   void fileRefresh() {textEditorPanel.fileRefresh();}
   
   void fileSave() {textEditorPanel.fileSave();}

   public void onExit() {textEditorPanel.onExit();}   

   public void doSaveImage() {}  // we don't do images
   
   public void doPrint() {textEditorPanel.print();}

}

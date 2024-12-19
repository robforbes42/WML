/**
 *  Author: Rob Forbes Nov 2024
 */
package forbesflutes.wml.ui;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.horstmann.corejava.GBC;

import forbesflutes.wml.ui.WMLDataTable.WMLDataTableModel;
import forbesflutes.wml.ui.WMLDataTable.WMLDataTableRow;
import forbesflutes.wml.WMLFileModel;
import forbesflutes.wml.WMLParser;

public class WMLBrowsePanel extends JPanel implements WMLPanel {
   private static final long serialVersionUID = 1L;
   private WMLFileModel model; 
   private WMLController controller;
   private JTextField dirField = UIUtil.createTextField(20);
   private WMLDataTable dataTable = new WMLDataTable();
   private WMLDataTableModel tableModel = dataTable.getModel();
   
   WMLBrowsePanel(WMLFileModel model, WMLController controller) {
      this.model = model;
      this.controller = controller;
      setLayout(new GridBagLayout());
      JButton browseButton = new JButton("Browse");
      browseButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {browse();}});
      JPanel dirPanel = new JPanel();
      dirPanel.add(new JLabel("Browse Directory"));
      dirPanel.add(dirField);
      dirPanel.add(browseButton);
      add(dirPanel, new GBC(0,0));
      
      // File Table
      JScrollPane scrollPane = new JScrollPane(dataTable);
      scrollPane.setMinimumSize(new Dimension(200, 100));  // needed for initial size
      scrollPane.setPreferredSize(new Dimension(200, 100));  // needed for max width
      
      // Buttons 
      JButton openInEditorButton = new JButton("Open in Editor");
      openInEditorButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            openInEditor();
         }         
      });
      JButton addToBoresButton = new JButton("Add to Bores");
      addToBoresButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            addToBores();
         }         
      });
            
      JPanel browsePanel = new JPanel(new GridBagLayout());
      browsePanel.setBorder(new TitledBorder("Files"));
      browsePanel.add(scrollPane, new GBC(0,0).span(1,2));
      JPanel browseButtonPanel = new JPanel(new GridLayout(2, 1, UIUtil.smallInset, UIUtil.smallInset));
      browseButtonPanel.add(openInEditorButton);
      browseButtonPanel.add(addToBoresButton);
      browsePanel.add(browseButtonPanel, new GBC(1,0));
      add(browsePanel, new GBC(0,1));
   }
   
   // button action method
   private void browse() {
      Path rootPath = Path.of(dirField.getText());
      try {
         Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
               String filepath = path.toString();
               if(filepath.endsWith(".wim") || filepath.endsWith(".tsv")) {
                 WMLParser parser = new WMLParser();
                 parser.parseFile(filepath);
                 if(parser.dataIsValid()) {
                    tableModel.addRow(new WMLDataTableRow(false, parser.getData()));
                 }
                 else {
                    JOptionPane.showMessageDialog(WMLBrowsePanel.this, "Could not parse " + filepath);
                 }
                }
              return FileVisitResult.CONTINUE;
            }
         });
      } catch (IOException e) {
         JOptionPane.showMessageDialog(this, "Error browsing files: " + e.getMessage());
      }
   }
   
   // button action method
   private void addToBores() {
       for(int i: dataTable.getSelectedRows()) {
          WMLDataTableRow row = tableModel.rows.get(i);
          controller.boresPanel.addData(row.data);
       }
       dataTable.clearSelection();
   }

   // button action method
   private void openInEditor() {
      if(dataTable.getSelectedRowCount() > 1) {
         JOptionPane.showMessageDialog(this, "Only one file can be opened in the editor.");
         return;
      }
      for(int i: dataTable.getSelectedRows()) {
         WMLDataTableRow row = tableModel.rows.get(i);
         controller.editPanel.openFile(row.data.filepath);
      }
      dataTable.clearSelection();
   }

   public void tabOpened() {
      // enable things that are exclusively mine:
      // disable things that I can't use:
      controller.saveImageItem.setEnabled(false);
      controller.printItem.setEnabled(false);
   }
   
   public void tabClosed() {
      // disable things that are exclusively mine:
      // enable things I disabled:
      controller.saveImageItem.setEnabled(true);
      controller.printItem.setEnabled(true);      
   }
   
   public void onExit() {}

   @Override
   public void doSaveImage() {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void doPrint() {
      // TODO Auto-generated method stub
      
   }
   
}

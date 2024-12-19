/**
 *  Author: Rob Forbes Nov 2024
 */
package forbesflutes.wml.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;

import com.horstmann.corejava.GBC;

import forbesflutes.boreptb.core.XYCurve;
import forbesflutes.wml.ui.WMLDataTable.WMLDataTableModel;
import forbesflutes.wml.ui.WMLDataTable.WMLDataTableRow;
import forbesflutes.wml.WMLData;
import forbesflutes.wml.WMLFileModel;
import forbesflutes.wml.TextFileModel.TextModelListener;
import forbesflutes.wml.WMLData.Section;
import forbesflutes.wml.WMLData.ToneHole;

public class WMLBoresPanel extends JPanel implements WMLPanel, TextModelListener {
   private static final long serialVersionUID = 1L;
   private ChartPanel chartPanel;
   private WMLDataTable dataTable = new WMLDataTable();
   private WMLDataTableModel tableModel = dataTable.getModel();
   private JCheckBox showToneHoleBox = new JCheckBox("Show Tone Holes and Section Breaks", false);
   private JTextField xMinField = UIUtil.createTextField(6);
   private JTextField xMaxField = UIUtil.createTextField(6);
   private WMLFileModel model; 
   private WMLController controller;
   private JRadioButton mmButton = new JRadioButton("mm");
   private JRadioButton inchButton = new JRadioButton("inch");

   public WMLBoresPanel(WMLFileModel model, WMLController controller) {
      this.model = model;
      this.controller = controller;
      model.addListener(this);
      setLayout(new GridBagLayout());
      
      // File Table
      JScrollPane scrollPane = new JScrollPane(dataTable);
      scrollPane.setMinimumSize(new Dimension(200, 100));  // needed for initial size
      scrollPane.setPreferredSize(new Dimension(200, 100));  // needed for max width
      
      // Buttons 
      JButton addFileButton = new JButton("Add");
      addFileButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            addFile();
         }         
      });
      JButton removeFileButton = new JButton("Remove");
      removeFileButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            removeFile();
         }         
      });
      JButton removeAllButton = new JButton("Remove All");
      removeAllButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            removeAllFiles();
         }         
      });
            
      JPanel browsePanel = new JPanel(new GridBagLayout());
      browsePanel.setBorder(new TitledBorder("Files"));
      browsePanel.add(scrollPane, new GBC(0,0).span(1,3));
      JPanel browseButtonPanel = new JPanel(new GridLayout(3, 1, UIUtil.smallInset, UIUtil.smallInset));
      browseButtonPanel.add(addFileButton);
      browseButtonPanel.add(removeFileButton);
      browseButtonPanel.add(removeAllButton);
      browsePanel.add(browseButtonPanel, new GBC(1,0));
      browsePanel.add(new JLabel("If names are not unique, edit Alias field to specify alternative."), 
          new GBC(0,3).span(2, 1).insets(UIUtil.smallInset));
      add(browsePanel, new GBC(0,0).span(1,2).insets(UIUtil.bigInset).anchor(GBC.WEST));

      JPanel unitsPanel = new JPanel(new GridLayout(1, 2));
      mmButton.setSelected(true);
      ButtonGroup group = new ButtonGroup();
      mmButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            unitsChanged();
         }         
      });
      inchButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            unitsChanged();
         }         
      });
      group.add(mmButton);
      group.add(inchButton);
      unitsPanel.add(mmButton);
      unitsPanel.add(inchButton);

      String[] unitChoices = {WMLData.MM_UNITS, WMLData.INCH_UNITS};
      JPanel optionsPanel = new JPanel(new GridBagLayout());
      optionsPanel.setBorder(new TitledBorder("Options"));
      optionsPanel.add(new JLabel("X Min: "), new GBC(0,0).anchor(GBC.EAST));
      optionsPanel.add(xMinField, new GBC(1,0).anchor(GBC.WEST).fill(GBC.BOTH).insets(UIUtil.smallInset) );
      optionsPanel.add(new JLabel("X Max: "), new GBC(0,1).anchor(GBC.EAST));
      optionsPanel.add(xMaxField, new GBC(1,1).anchor(GBC.WEST).fill(GBC.BOTH).insets(UIUtil.smallInset));
      optionsPanel.add(new JLabel("Display Units: "), new GBC(0,2));
      //optionsPanel.add(unitBox, new GBC(1,2).anchor(GBC.WEST).insets(UIUtil.smallInset));
      optionsPanel.add(unitsPanel, new GBC(1,2).anchor(GBC.WEST));
      showToneHoleBox.setPreferredSize(new Dimension(300,12));
      optionsPanel.add(showToneHoleBox, new GBC(0,3).span(2,1).weight(.5, 0.).insets(UIUtil.smallInset).anchor(GBC.WEST));
      
      JButton bigWindowButton = new JButton("BIG Graph");
      bigWindowButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            bigWindow();
         }         
      });
      JButton graphButton = new JButton("Graph");
      graphButton.setForeground(Color.blue);
      graphButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            graph();
         }         
      });
      JPanel graphButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      graphButtonsPanel.add(graphButton, new GBC(0,4));
      graphButtonsPanel.add(bigWindowButton, new GBC(1,4));
      
      JPanel rightPanel = new JPanel(new GridBagLayout());
      rightPanel.add(optionsPanel, new GBC(0,0).anchor(GBC.WEST));
      rightPanel.add(graphButtonsPanel, new GBC(0,1));
      add(rightPanel, new GBC(1, 0).insets(UIUtil.bigInset, UIUtil.bigInset, 0, UIUtil.bigInset).anchor(GBC.WEST));

      JPanel emptyPanel = new JPanel();
      add(emptyPanel, new GBC(2, 0).weight(1., 0.));
      
      // Graph Panel
      chartPanel = new ChartPanel(createBoreChart(WMLData.Units.MM), false);
      chartPanel.setMinimumSize(new Dimension(700, 400));  // needed for initial size
      add(chartPanel, new GBC(0,2).span(3,1).fill(GBC.BOTH).weight(0., 1.));
      
   }

   private void setChartDomainBounds(double xMin, double xMax, JFreeChart chart) {
      XYPlot plot = (XYPlot) chart.getPlot();
      ValueAxis domainAxis = plot.getDomainAxis();
      double oldXMin = domainAxis.getLowerBound();
      double oldXMax = domainAxis.getUpperBound();
      xMin = Double.isNaN(xMin) ? oldXMin : xMin;
      xMax = Double.isNaN(xMax) ? oldXMax : xMax;
      domainAxis.setRange(xMin, xMax);
   }
   
   private void unitsChanged() {
      double factor = mmButton.isSelected() ? WMLData.mmPerInch : WMLData.inchesPerMM;
      double xMin = getDoubleValue(xMinField, "X Min");
      String numberFormat = "%.3g";
      if(!Double.isNaN(xMin)) 
         xMinField.setText(String.format(numberFormat, xMin * factor));
      double xMax = getDoubleValue(xMaxField, "X Max");
      if(!Double.isNaN(xMax))
         xMaxField.setText(String.format(numberFormat, xMax * factor));
      
   }
   
   private JFreeChart createBoreChart(WMLData.Units units) {
      String unitName = units.toString().toLowerCase();
      JFreeChart chart =  ChartFactory.createXYLineChart("Assembled Bore Profiles", "Distance (" + unitName + ")", "Diameter (" + unitName + ")",
         new DefaultXYDataset(),  PlotOrientation.VERTICAL, true, true, false);
      XYPlot plot = (XYPlot) chart.getPlot();
      NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
      rangeAxis.setAutoRangeIncludesZero(false);
      return chart;
   }
   
   private void addDataToGraph(String name, XYCurve xyCurve, JFreeChart chart) {
      XYPlot plot = (XYPlot) chart.getPlot();
      DefaultXYDataset dataset = (DefaultXYDataset) plot.getDataset();
      dataset.addSeries(name, xyCurve.getXYValues());
   }

   private void addMarkerToGraph(double d, Color color, JFreeChart chart) {
      chart.getXYPlot().addDomainMarker(new ValueMarker(d, color, UIUtil.dashedStroke));       
   }
   
   private double getDoubleValue(JTextField textField, String name) {
      String svalue = textField.getText();
      double value = Double.NaN;
      if(!svalue.isEmpty()) {
         try {
            value = Double.parseDouble(svalue);
         } catch(NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Illegal format for " + name + ": " + svalue);
         }
      }
      return value;
   }
      
   private JFreeChart createChart() {
      WMLData.Units unitsChoice = mmButton.isSelected() ? WMLData.Units.MM : WMLData.Units.INCH;
      JFreeChart chart = createBoreChart(unitsChoice);
      for (WMLDataTableRow row : dataTable.getModel().rows) {
         if (row.isSelected) {
            WMLData data = row.getDataForUnits(unitsChoice);
            WMLData.Part part = data.parts.get(0);
            WMLData.AssembledSectionInfo assembledInfo = part.createAssembledSection();
            Section assembledSection = assembledInfo.section;
            String label = row.alias.isEmpty() ? data.name : row.alias;
            addDataToGraph(label, assembledSection.boreCurve, chart);
            if (showToneHoleBox.isSelected()) {
               Color toneHoleColor = Color.yellow;
               for(ToneHole toneHole: assembledSection.toneHoles) {
                  addMarkerToGraph(toneHole.xCoord, toneHoleColor, chart);
               }
               Color sectionBreakColor = Color.darkGray;
               for(int i: assembledInfo.boreSectionBreakIndices) {
                  addMarkerToGraph(assembledInfo.section.boreCurve.getX(i),
                        sectionBreakColor, chart);
               }
            }
         }
      }
      double xMin = getDoubleValue(xMinField, "X Min");
      double xMax = getDoubleValue(xMaxField, "X Max");
      setChartDomainBounds(xMin, xMax, chart);
      return chart;
   }
   
   private void graph() {
      chartPanel.setChart(createChart());
   }
 
   /** Open a new window with just the current graph. */
   private void bigWindow() {
      JFrame frame = new JFrame();
      frame.getContentPane().add(new ChartPanel(createChart(), false));
      frame.setSize(1200, 700);
      frame.setVisible(true);
   }
   
   private void addFile()  {
      File file = controller.chooseFile();
      if(file == null) return;
      WMLFileModel newModel = new WMLFileModel();
      try {
         newModel.open(file.toString());
      } catch (IOException e) {
         JOptionPane.showMessageDialog(this, "Error adding file: " + e.getMessage());
      }
      if(newModel.isValid()) {
         addData(newModel.getData());
      }
      else {
         JOptionPane.showMessageDialog(this, "File failed validation; not added.");         
      }
   }

   void addData(WMLData data) {
      Set<String> ids = tableModel.getIds();
      // check for duplicate id:
      if(ids.contains(data.id)) {
         int response = JOptionPane.showConfirmDialog(this, 
            "A file with id " + data.id + " is already present. Do you want to continue?");
         if(response == JOptionPane.NO_OPTION) return;
      }
      dataTable.getModel().addRow(new WMLDataTableRow(true, data));
   }
   
   private void removeFile()  {
      String id = JOptionPane.showInputDialog("Enter id of file to remove.");
      int rowNum = tableModel.findRow(id);
      if(rowNum >= 0) {
         tableModel.removeRow(rowNum);
      }
      else {
         JOptionPane.showMessageDialog(this, "File " + id + " not found.");
      }
   }
   
   private void removeAllFiles()  {
      tableModel.clear();
   }
   
   @Override
   public void modelChanged() {
      // file could be new, updated, or closed
      if(model.isValid()) {
         WMLData data = model.getData();
         // if the file isn't in the table, add it:
         if(tableModel.findRow(data.id) == -1) {
            tableModel.addRow(new WMLDataTableRow(true, data));
         }
      }
      // If a previous file has been closed, it's going to stay in this list.
      // That might be desired behavior...even if you're finished editing it, you still might want it
   }

   public void tabOpened() {}
   
   public void tabClosed() {}
   
   public void onExit() {}

   @Override
   public void doSaveImage() {
      File chosenFile = controller.chooseFile();
      if(chosenFile != null) {
         Point imageDimensions = controller.getImageDimensions();
         if(imageDimensions != null) {
            try {
               ChartUtilities.saveChartAsPNG(chosenFile, chartPanel.getChart(), imageDimensions.x, imageDimensions.y );
            } catch (IOException e) {
               JOptionPane.showMessageDialog(this, "Error saving image: " + e.getMessage(), "", JOptionPane.ERROR_MESSAGE);
            }
         }
         // else there was a bad number format
      }
   }

   @Override
   public void doPrint() {
      chartPanel.createChartPrintJob();   
   }
   
}

/**
 *  Author: Rob Forbes Nov 2024
 */

package forbesflutes.wml.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import forbesflutes.wml.WMLData;

public class WMLDataTable extends JTable {
    
   static class WMLDataTableModel extends AbstractTableModel {
      String[] columnNames = {"Graph", "ID", "Name", "Alias", "Units"};
      List<WMLDataTableRow> rows = new ArrayList<WMLDataTableRow>();
      public String getColumnName(int col) {return columnNames[col].toString();}
      public int getRowCount() {return rows.size();}
      public int getColumnCount() {return columnNames.length;}
      public Object getValueAt(int row, int col) {return rows.get(row).getValueAt(col);}
      public boolean isCellEditable(int row, int col) {return col == 0 || col == 3;}
      public void setValueAt(Object value, int row, int col) {
         rows.get(row).setValueAt(col, value);
         fireTableCellUpdated(row, col);
      }
            
      public void addRow(WMLDataTableRow row) {
         rows.add(row);
         fireTableDataChanged();
      }
      
      public void removeRow(int row) {
         rows.remove(row);
         fireTableDataChanged();
      }
      
      public int findRow(String id) {
         int rowNum = 0;
         for(WMLDataTableRow row: rows) {
            if(row.data.id.equals(id))
               return rowNum;
            ++rowNum;
         }
         return -1;
      }
      
      public Set<String> getIds() {
         HashSet<String> ids = new HashSet<String>();
         for(WMLDataTableRow row: rows) 
            ids.add(row.data.id);
         return ids;
      }
      
      public void clear() {
         rows.clear();
         fireTableDataChanged();
      }
      
      @Override
      public
      Class getColumnClass(int col) {
         if(col == 0)
            return Boolean.class;
         else
            return String.class;
      }
   }
   
   static class WMLDataTableRow {
      boolean isSelected;
      String alias;
      WMLData data;
      WMLData mmData;
      WMLData inchData;
      
      public WMLDataTableRow(boolean isSelected, WMLData data) {
         this.isSelected = isSelected;
         alias = "";
         this.data = data;
         if(data.units == WMLData.Units.MM) {
            mmData = data;
            inchData = mmData.copy();
            inchData.convertUnits( WMLData.Units.INCH);
         }
         else {
            inchData = data;
            mmData = inchData.copy();
            mmData.convertUnits(WMLData.Units.MM);            
         }
      }
      
      public WMLData getDataForUnits(WMLData.Units units) {
         if(units == WMLData.Units.MM)
            return mmData;
         else
            return inchData;
      }
      
      public void setValueAt(int col, Object value) {
         switch(col) {
            case 0: isSelected = (Boolean) value; break;
            case 3: alias = (String) value; break;
            default: break;
         }
      }
      
      public Object getValueAt(int col) {
         switch(col) {
            case 0: return isSelected; 
            case 1: return data.id; 
            case 2: return data.name;
            case 3: return alias;
            case 4: return data.units;
            default: return null; 
         }
      }
   }
      
   static class CenterRenderer extends DefaultTableCellRenderer {
      public void setValue(Object value) {
         setText(value.toString());
         setHorizontalAlignment(SwingConstants.CENTER);
      }
   }
   
   public WMLDataTable() {
      super(new WMLDataTableModel());
      // disable selection:
      setFocusable(false);
      setRowSelectionAllowed(true);    
      setShowHorizontalLines(showHorizontalLines);
      setDefaultRenderer(Object.class, new CenterRenderer());
   }
   
   public WMLDataTableModel getModel() {
      return (WMLDataTableModel) super.getModel();
   }

}

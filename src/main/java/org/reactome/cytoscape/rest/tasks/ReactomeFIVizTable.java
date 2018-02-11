package org.reactome.cytoscape.rest.tasks;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.cytoscape.ci.model.CIResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A generic table model as returned message in the REST API.
 * @author wug
 */
@ApiModel(value = "Analysis Results in Table", description = "Wrap analysis in a table.")
public class ReactomeFIVizTable {
    @ApiModelProperty(value = "Table Headers")
    private List<String> tableHeaders;
    @ApiModelProperty(value = "Table Content", notes = "All values are returned as String.")
    private List<List<String>> tableContent;
    
    public ReactomeFIVizTable() {
    }

    public List<String> getTableHeaders() {
        return tableHeaders;
    }

    public void setTableHeaders(List<String> tableHeaders) {
        this.tableHeaders = tableHeaders;
    }

    public List<List<String>> getTableContent() {
        return tableContent;
    }

    public void setTableContent(List<List<String>> tableContent) {
        this.tableContent = tableContent;
    }
    
    public void addRow(List<String> row) {
        if (tableContent == null)
            tableContent = new ArrayList<>();
        tableContent.add(row);
    }
    
    /**
     * Copy contents from the passed table to this object.
     * @param table
     */
    public void fill(JTable table) {
        TableModel tableModel = table.getModel();
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String header = tableModel.getColumnName(i);
            headers.add(header);
        }
        setTableHeaders(headers);
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                Object value = tableModel.getValueAt(i, j);
                row.add(value + "");
            }
            addRow(row);
        }
    }
    
    @ApiModel(value="Table Model", description="Results generated from FIReactomeFIViz in a table model", parent=CIResponse.class)
    public static class ReactomeFIVizTableResponse extends CIResponse<ReactomeFIVizTable> {
        
    }

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.google.javaguipst;

import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;
import java.io.IOException;
import java.util.HashMap;
import javax.swing.table.AbstractTableModel;

class EmailTableModel extends AbstractTableModel {

	PSTFolder theFolder = null;
	PSTFile theFile = null;

	HashMap cache = new HashMap();

	public EmailTableModel(PSTFolder theFolder, PSTFile theFile) {
		super();

		this.theFolder = theFolder;
		this.theFile = theFile;
	}

	String[] columnNames = {
    		"Descriptor ID",
    		"Subject",
    		"From",
    		"To",
    		"Date",
    		"Has Attachments"
	};
	String[][] rowData = {{"","","","",""}};
	int rowCount = 0;
	public String getColumnName(int col) {
        return columnNames[col].toString();
    }
    public int getColumnCount() { return columnNames.length; }

    public int getRowCount() {
    	try {
    		return theFolder.getContentCount();
    	} catch (Exception err) {
    		err.printStackTrace();
    		System.exit(0);
    	}
    	return 0;
    }

    public PSTMessage getMessageAtRow(int row) {
    	PSTMessage next = null;
		try {
	    	if (cache.containsKey(row)) {
				next = (PSTMessage)cache.get(row);
			} else {
	    		theFolder.moveChildCursorTo(row);
				next = (PSTMessage)theFolder.getNextChild();
	    		cache.put(row, next);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return next;
    }


    public Object getValueAt(int row, int col) {
    	// get the child at...
    	try {
			PSTMessage next = getMessageAtRow(row);
            if (next == null) {
				return null;
			}

			switch (col) {
				case 0:
					return next.getDescriptorNode().descriptorIdentifier+"";
				case 1:
					return next.getSubject();
				case 2:
					return next.getSentRepresentingName() + " <"+ next.getSentRepresentingEmailAddress() +">";
				case 3:
					return next.getReceivedByName() + " <"+next.getReceivedByAddress()+">" +
						next.displayTo();
				case 4:
					return next.getClientSubmitTime();
//					return next.isFlagged();
//					return next.isDraft();
//					PSTTask task = next.toTask();
//					return task.toString();
				case 5:
					return (next.hasAttachments() ? "Yes" : "No");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

        return "";
    }
    public boolean isCellEditable(int row, int col) { return false; }

    public void setFolder(PSTFolder theFolder) throws IOException, PSTException {
    	theFolder.moveChildCursorTo(0);
    	this.theFolder = theFolder;
    	cache = new HashMap();
    	this.fireTableDataChanged();
    }

}
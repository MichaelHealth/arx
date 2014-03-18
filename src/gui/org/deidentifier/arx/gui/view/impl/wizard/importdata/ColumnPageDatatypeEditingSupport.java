package org.deidentifier.arx.gui.view.impl.wizard.importdata;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;


/**
 * Implements the editing support for datatype column within the column page
 *
 * This allows to change the datatype of columns with the column page
 * {@link ColumnPage}. The modifications are performed with a combo box
 * {@link ComboBoxCellEditor}. The datatype itself is stored with the
 * appropriate {@link ImportDataColumn} object.
 *
 * TODO Implement better editor for datatypes of arx framework
 */
public class ColumnPageDatatypeEditingSupport extends EditingSupport {

    /**
     * Actual editor
     */
    private ComboBoxCellEditor editor;

    /**
     * Allowed values for the user to choose from
     */
    private String[] choices = new String[]{"String", "Numerical", "Date/Time"};


    /**
     * Creates a new editor for the given {@link TableViewer}.
     *
     * @param viewer The TableViewer this editor is implemented for
     */
    public ColumnPageDatatypeEditingSupport(TableViewer viewer)
    {

        super(viewer);

        editor = new ComboBoxCellEditor(viewer.getTable(), choices, SWT.READ_ONLY);

    }

    /**
     * Indicate that all cells within this column can be edited
     */
    @Override
    protected boolean canEdit(Object arg0)
    {

        return true;

    }

    /**
     * Returns a reference to {@link #editor}.
     */
    @Override
    protected CellEditor getCellEditor(Object arg0)
    {

        return editor;

    }

    /**
     * Gets datatype of column
     *
     * By default all columns are assumed to be strings.
     *
     * TODO Get datatype from column object after detecting it previously
     */
    @Override
    protected Object getValue(Object arg0)
    {

        return 0;

    }

    /**
     * Sets datatype of column
     *
     * Internally this function makes use of
     * {@link ImportDataColumn#setDatatype(String)}. The values are taken
     * from {@link #choices}.
     */
    @Override
    protected void setValue(Object element, Object value)
    {

        ((ImportDataColumn)element).setDatatype(choices[(int)value]);
        getViewer().update(element, null);

    }

}

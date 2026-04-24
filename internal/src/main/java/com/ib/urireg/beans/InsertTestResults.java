package com.ib.urireg.beans;

import com.ib.urireg.db.dao.DocDAO;
import com.ib.urireg.db.dao.IzpitDAO;
import com.ib.urireg.db.dto.Doc;
import com.ib.urireg.db.dto.IzpitResult;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.CellEditEvent;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class InsertTestResults extends IndexUIbean {


    private static final Logger LOGGER = LoggerFactory.getLogger(InsertTestResults.class);

    //За копиране при Деси
    private transient IzpitDAO izpitDAO;
    private List<Object[]> listResIzpitTest = new ArrayList<>();
    private List<Object[]> listResIzpitCase = new ArrayList<>();


    @PostConstruct
    void initData() {

        izpitDAO = new IzpitDAO(getUserData());
        actionSearchIzpitResults();
        actionSearchCaseResults();
        IzpitResult result = new IzpitResult();
//        result.setLiceId(1);
//        resultsList.add(result);
//        result = new IzpitResult();
//        result.setLiceId(2);
//        resultsList.add(result);
    }

    //За копиране при Деси
    public void actionSaveResultTest(RowEditEvent<Object[]> event) {
        Object[] editedRow = event.getObject();
        IzpitResult editedResult = (IzpitResult) editedRow[10];
        try {
            Doc zapIzpit = new DocDAO(getUserData()).findById(Integer.valueOf(18));//TODO от бийна
            JPA.getUtil().runInTransaction(() ->   izpitDAO.saveResult(editedResult, zapIzpit));
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
        } catch (DbErrorException e) {
            throw new RuntimeException(e);
        } catch (InvalidParameterException e) {
            throw new RuntimeException(e);
        } catch (BaseException e) {
            throw new RuntimeException(e);
        }
    }

    //За копиране при Деси
    public void actionSearchCaseResults(){
        try {
            listResIzpitCase= izpitDAO.findIzpitResults(Integer.valueOf(5),Integer.valueOf(26));//TODO параметри от бийна
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при работа!");
        }


    }

    //За копиране при Деси
    public IzpitResult getIzpitResultFromRow(Object[] row) {
        if (row != null && row.length > 10 && row[10] instanceof IzpitResult) {
            return (IzpitResult) row[10];
        }
        return null;
    }

    //За копиране при Деси
    public void actionSearchIzpitResults(){
        try {
            listResIzpitTest= izpitDAO.findIzpitResults(Integer.valueOf(5),Integer.valueOf(19));//TODO параметри от бийна
        } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
        JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при работа!");
    }


    }
    //За тестването на таблицата
    private List<IzpitResult> resultsList =new ArrayList<>();
    private IzpitResult selectedRow;
    private IzpitResult lastRow;
    private Integer lastRowIndex;




    public void onCellEdit(CellEditEvent event) {

        int currentRowIndex = event.getRowIndex();
        DataTable table = (DataTable) event.getSource();
        IzpitResult currentRow = (IzpitResult) table.getRowData();

        int colIndex = table.getColumns().indexOf(event.getColumn());
        int lastColIndex = table.getColumns().size() - 1;
        if (lastRowIndex != null && !lastRowIndex.equals(currentRowIndex) && lastRow != null) {
            saveRow(lastRow);
        }

        lastRowIndex = currentRowIndex;
        lastRow = currentRow;
    }

    public void onRowSelect(SelectEvent<IzpitResult> event) {
        if (lastRow != null && !lastRow.equals(event.getObject())) {
            saveRow(lastRow);
            lastRow = null;
        }
    }


    private void saveRow(IzpitResult row) {
        //TODO запис
        System.out.println("Запис: " + row.getId());
    }

    public void actionNewRow(){
        resultsList.add(new IzpitResult());
    }

    public List<IzpitResult> getResultsList() {
        return resultsList;
    }

    public void setResultsList(List<IzpitResult> resultsList) {
        this.resultsList = resultsList;
    }

    public IzpitResult getSelectedRow() {
        return selectedRow;
    }

    public void setSelectedRow(IzpitResult selectedRow) {
        this.selectedRow = selectedRow;
    }

    public List<Object[]> getListResIzpitTest() {
        return listResIzpitTest;
    }

    public void setListResIzpitTest(List<Object[]> listResIzpitTest) {
        this.listResIzpitTest = listResIzpitTest;
    }

    public List<Object[]> getListResIzpitCase() {
        return listResIzpitCase;
    }

    public void setListResIzpitCase(List<Object[]> listResIzpitCase) {
        this.listResIzpitCase = listResIzpitCase;
    }
}

package com.ib.urireg.utils;

import com.ib.system.utils.SearchUtils;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringTokenizer;


@FacesConverter("glConverter")
public class GlobalSearchConverter  implements Converter<Object> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSearchConverter.class);

	@Override
    public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
        if(value != null && value.trim().length() > 0) {
            try {

            	StringTokenizer stitem = new StringTokenizer(value, "@@");

            	Object[] item = new Object[6];

            	item[0] = Integer.parseInt(stitem.nextToken()); // OBJ_ID
            	item[1] = SearchUtils.asString(stitem.nextToken()); // RN_OBJ
            	item[2] = SearchUtils.asString(stitem.nextToken()); // DATE_OBJ
            	item[3] = Integer.parseInt(stitem.nextToken()); // TIP
            	item[4] = Integer.parseInt(stitem.nextToken()); // EDIT_MODE

            	String idDocTask= stitem.nextToken(); // ID_DOC_IN_TASK
            	if(idDocTask!= null && idDocTask.equals("null")) {
            		item[5] = null;
            	} else {
            		item[5] = idDocTask;
            	}


                return item;
            } catch(NumberFormatException e) {
            	LOGGER.debug("NumberFormatException getAsObject->{}", value);
            	 return null;
            }
        }
        else {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext fc, UIComponent uic, Object object) {
        if(object != null) {
        	if(object instanceof Object[] ){
        		Object[] object_	= (Object[])object;

        		StringBuilder str = new StringBuilder();
        		str.append(SearchUtils.asString(object_[0]));  // OBJ_ID
        		str.append("@@");
        		str.append(SearchUtils.asString(object_[1]));  // RN_OBJ
        		str.append("@@");
        		str.append(SearchUtils.asString(object_[2]));  // DATE_OBJ
        		str.append("@@");
        		str.append(SearchUtils.asString(object_[3])); // TIP
        		str.append("@@");
        		str.append(SearchUtils.asString(object_[4])); // EDIT_MODE = 1-актуализация,0-преглед<br>
        		str.append("@@");
	        		if(object_[5]!=null) {
	        			str.append(SearchUtils.asString(object_[5])); // ID_DOC_IN_TASK
	        		} else {
	        			str.append("null"); // ID_DOC_IN_TASK
	        		}
	            return str.toString();

			}else if(object instanceof String ){
				return (String) object;
			} else {
				return null;
			}
        }
        else {
            return null;
        }
    }

}

package com.ib.urireg.udostDocs;

import jakarta.persistence.AttributeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author n.kanev
 */
public class StringListConverter implements AttributeConverter<List<String>, String> {
    private static final String DELIMITER = "<._.>";

    @Override
    public String convertToDatabaseColumn(List<String> stringList) {
        if(stringList != null && !stringList.isEmpty()) {
            return String.join(DELIMITER, stringList);
        }
        else return null;
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if(dbData != null) {
            String[] array = dbData.split(Pattern.quote(DELIMITER));
            if(array != null) {
            	ArrayList<String> list = new ArrayList<>();
            	for(String s : array) list.add(s);
            	return list;
            }
            else return null;

        }
        else return null;
    }
}

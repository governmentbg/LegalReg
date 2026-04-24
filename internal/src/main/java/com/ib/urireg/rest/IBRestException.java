package com.ib.urireg.rest;


import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/** Предване на информация от сървър-а към клиента.
 * Ест, това е само за РЕСТ Сървиси.
 * Целта е не простода предадем че има грешка , но и малко по-подробна информация какво се е случило на сървъра.
 * 
 * Това е възможно няй-простата имплементаци на WebApplicationException,
 * като най-важния конструктор е : IBRestException(int status, long  corelationId, String customMessage, Exception ex)
 * 
 * Когато се изхвърли подобна грешка, при клиента освен статус ще се изпрати и мап със следните атрибути:
 * corelationId - някакво уникално число (примерно може да е:new Date().getTime()
 * customMessage - 
 * exception - 
 * 
 * При клиента се работи така:
  if (response.getStatus() != 200) {
     if(response.hasEntity()){	
     	Map<String,String> errorMap = (Map<String, String>) JSonUtils.json2Object(result, Map.class);
		System.out.println("corId:"+errorMap.get("corelationId"));
		System.out.println("customMessage:"+errorMap.get("customMessage"));
		System.out.println("exception:"+errorMap.get("exception"));
     }
     ...
 * @author krasig
 *
 */
public class IBRestException extends WebApplicationException {
    private static final long serialVersionUID = -9079411854450419091L;

   

//    public IBRestException2() {
//    }
//
//    public IBRestException2(int status) {
//        super(status);
//    }
//
//    public IBRestException2(Response response) {
//        super(response);
//    }
//
//    public IBRestException2(Status status) {
//        super(status);
//    }
//
//    public IBRestException2(String message, Response response) {
//        super(message, response);
//    }
//
    
    /**
     * @param status - standarten HTTP Status
     * @param corelationId - някакво уникално число, , което е важно да се записва в лог-а на сървъра, за да може да се намери когато се покаже при клиента (може да се ползва 
     * @param customMessage -- Някакво съобщение за интерфейс-а
     * @param  - От това ще се взем само ex.getMessage(). Не бива да се показва на клиента
     */
    public IBRestException(int status, long  corelationId, String customMessage) {
    	
    	
    	super(Response.status(status).entity(
    			Stream.of(new String[][] {
    				  { "corelationId", String.valueOf(corelationId) }, 
    				  { "customMessage", customMessage }, 
    				  { "exception", "" }, 
    				}).collect(Collectors.toMap(data -> data[0], data -> data[1]))
    			).type(MediaType.APPLICATION_JSON).build());
    }
    /**
     * @param status - standarten HTTP Status
     * @param corelationId - някакво уникално число, , което е важно да се записва в лог-а на сървъра, за да може да се намери когато се покаже при клиента (може да се ползва 
     * @param customMessage -- Някакво съобщение за интерфейс-а
     * @param ex - От това ще се взем само ex.getMessage(). Не бива да се показва на клиента
     */
    public IBRestException(int status, long  corelationId, String customMessage, Exception ex) {
    	
    	
    	super(Response.status(status).entity(
    			Stream.of(new String[][] {
    				  { "corelationId", String.valueOf(corelationId) }, 
    				  { "customMessage", customMessage }, 
    				  { "exception", ex.getMessage() }, 
    				}).collect(Collectors.toMap(data -> data[0], data -> data[1]))
    			).type(MediaType.APPLICATION_JSON).build());
    }
//
//   
//    
//    public IBRestException2(Status status, String message) {
//        this(status.getStatusCode(), message);
//    }
//
//    public IBRestException2(String message) {
//        this(500, message);
//    }
    
    
    private Map<String,String> buildMap(int corelationId,String customMessage,Exception ex){
    	Map<String,String> error = new HashMap<>();
    	error.put("corelationId", String.valueOf(corelationId));
    	error.put("customMessage", customMessage);
    	error.put("exception", ex.getMessage());
    	return error;
    }
    
    private String stack2string(Exception e) {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			return "------"+System.lineSeparator() + sw.toString() + "------"+System.lineSeparator();
		} catch (Exception e2) {
			return "bad stack2string";
		}
	}

}

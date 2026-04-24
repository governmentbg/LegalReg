package com.ib.urireg.rest;

import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.urireg.system.UriregConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Map;

@Path("/regix")
public class RegixServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegixServices.class);
    @GET
    @Produces("text/plain")
    public String version() {
        return "RegixServices 1.0";
    }

    @GET
    @Path("/findperson")
    @Produces("application/json")
    @Operation(tags = "RegIx",summary = "Справка по лице за придобита юридическа правоспособност",
            description = "По-подадено егн или лнч на лице връща данни за придобита юридическа правоспособност")
    @ApiResponses(value ={
            @ApiResponse(responseCode = "400", description = "Missing or invalid parameter", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = IBRestException.class, type = "IBRestError"),
                            examples = {@ExampleObject(value = "{status:400,corelationId:1710328523,customMessage:missing or invalid parameter}")}
                    ),

            }),
            @ApiResponse(responseCode = "500", description = "Error", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = IBRestException.class, type = "IBRestException"),
                            examples = {@ExampleObject(value = "{status:500,corelationId:1710328523,customMessage:someMessage,stackTrace:someStackTrace}")}
                    )}),
    }
    )
    public Response findUriPerson (@Nonnull @QueryParam("egnlnch")
                                   @Parameter(name="egnlnch",description = "Person identifier",example="type EGN or LNCH")
                                   String egnLnch) {

        LOGGER.info("findUriPerson egnlnch{}",egnLnch);


        long correlationId=new Date().getTime();
        try {
            if (egnLnch == null || egnLnch.trim().isEmpty()) {
                LOGGER.debug("RestError:corelationId:{}, parameter egnlnch is null,empty or missing!!!",correlationId);
                throw new IBRestException(Response.Status.BAD_REQUEST.getStatusCode(), correlationId, "parameter egnLnch is null,empty or missing");
            }


            String jsonString;
            try{

                StringBuilder sqlBuilder = new StringBuilder();

                sqlBuilder.append(" select ")
                        .append(" COALESCE (l.egn , l.lnc) egnLnc, ")
                        .append(" l.firstname firstname, ")
                        .append(" l.surname surname, ")
                        .append(" l.lastname lastname, ")
                        .append(" udost.rn_doc upNomer, ")
                        .append(" udost.doc_date upDate, ")
                        .append("CASE WHEN  l.status = ")//заради сортирането
                        .append(UriregConstants.CODE_ZNACHENIE_LICE_STATUS_PRAVO_LISHEN)
                        .append(" THEN 'да'  ELSE 'не' END lishenOtPravo, ")
                        .append("CASE WHEN  udost.original = ")//заради сортирането
                        .append(UriregConstants.CODE_ZNACHENIE_NE)
                        .append(" THEN 'да' ELSE 'не' END dublikat ")
                        .append(" from lice l ")
                        .append(" JOIN doc udost on l.udost_id = udost.doc_id ")
                        .append(" where (l.egn = :egnlnc or l.lnc = :egnlnc)");

                String sql = sqlBuilder.toString();

                sql = "SELECT  CAST(json_agg(myRest) AS TEXT) myJson FROM ( "+sql+" ) myRest";

                Query querySelect = JPA.getUtil().getEntityManager().createNativeQuery(sql);
                querySelect.setParameter("egnlnc", egnLnch);


                Object rez = querySelect.getSingleResult();



                if(rez!=null) {
                    jsonString = (String)rez;
                } else {
                    //jsonString="[]";
                    jsonString="[]";
                }
            }catch (Exception e){
                LOGGER.error("Exception: corelationId={}, error:{}",correlationId,e.getMessage(),e);
                throw new IBRestException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),correlationId,"Грешка при извличане на данни от БД",e);


            }finally {
                JPA.getUtil().closeConnection();
            }


            return  Response.status(Response.Status.OK).entity(jsonString).build();

        } catch (IBRestException e){
            throw e;
        } catch (Exception e){
            LOGGER.error("Exception: corelationId={}, error:{}",correlationId,e.getMessage(),e);
            throw new IBRestException(Response.Status.BAD_REQUEST.getStatusCode(),correlationId,"Error parsing parameters",e);
        }

    }

}

package com.ib.urireg.rest;

import com.ib.system.db.JPA;
import com.ib.urireg.search.PublicRegister;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Path("/register")
public class PublicReg {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicReg.class);
    @GET
    @Produces("text/plain")
    public String hello() {
        return "Hello, World!";
    }

    @GET
    @Path("/show")
    @Produces("application/json")
    public Response getRegister(@QueryParam("upnum") String upNomer,
                                @QueryParam("firstname") String name,
                                @QueryParam("surname") String prezeme,
                                @QueryParam("lastname") String familia,
                                @QueryParam("sortBy") String sortBy,
                                @QueryParam("sortDirection") String sortDirection,
                                @QueryParam("page") String page,
                                @QueryParam("pageSize") String pageSize
                                ) {

        LOGGER.info("getRegister upnum{}, name={}, surname={}, lastname={}, sortBy{},sortDirection{}, page={}, pageSize={}", upNomer,name,prezeme,familia,sortBy,sortDirection,page, pageSize);

        String singleResult="";
        {

            // Default pagination values
            Integer tmpPage = (page!=null && page.length()>0)?Integer.parseInt(page):0;
            Integer tmpPageSize = (pageSize!=null && pageSize.length()>0)?Integer.parseInt(pageSize):null; // Set to null to get all rows
            Integer offset = (tmpPageSize != null && tmpPageSize > 0 ? (tmpPage * tmpPageSize) : 0);


            String myJsonSQL = "" +
                    "WITH total_count AS (\n" +
                    "{SQL_FOR_COUNT}" +
                    ")" +
                    "SELECT \n" +
                    "    json_build_object( 'data',json_agg(myResult), 'totalCount', \n" +
                    "    (  select cnt from total_count  ),\n" +
                    "        'page',:page,\n" +
                    "        'pageSize',:pageSize," +
                    "        'totalPages', CASE \n" +
                    "            WHEN :pageSize IS NULL OR :pageSize <= 0 THEN 1\n" +
                    "            ELSE CEIL((SELECT cnt FROM total_count)::numeric / :pageSize)\n" +
                    "        END )\n" +
                    "FROM \n" +
                    "    (   {SQL_FOR_DATA} \n" +
                    "        {PAGINATION}" +
                    "        ) myResult";

            try {
                PublicRegister search = new PublicRegister();
                if (upNomer != null) {
                    search.setRnUdost(upNomer);
                }
               // search.setFirstname("Гинка");
                if (name != null) {
                    search.setFirstname(name);
                }
                if (prezeme != null) {
                    search.setSurname(prezeme);
                }
                if (familia != null) {
                    search.setLastname(familia);
                }

                if (sortBy != null) {
                    search.setSortColumn(sortBy);
                }
                if (sortDirection != null) {
                    search.setSortDirection(sortDirection);
                }

                search.buildQuery();
                myJsonSQL = myJsonSQL.replace("{SQL_FOR_COUNT}", search.getSqlCount());
                myJsonSQL = myJsonSQL.replace("{SQL_FOR_DATA}", search.getSql());

                if (tmpPageSize != null && tmpPageSize > 0) {
                    myJsonSQL = myJsonSQL.replace("{PAGINATION}", "LIMIT :pageSize OFFSET :offset\n");
                } else {
                    myJsonSQL = myJsonSQL.replace("{PAGINATION}", "");
                }

               // System.out.println(myJsonSQL);
                Map<String, Object> sqlParameters = search.getSqlParameters();
                Query query = JPA.getUtil().getEntityManager().createNativeQuery(myJsonSQL);
                LOGGER.debug("Parameters");
                sqlParameters.forEach((key, value) -> {
                    LOGGER.debug("parm:key={}, value={}", key, value);
                    query.setParameter(key, value);
                });

                query.setParameter("page", tmpPage);

                if (tmpPageSize != null && tmpPageSize > 0) {
                    query.setParameter("offset", offset);
                    LOGGER.debug("ffset={}", offset);
                    query.setParameter("pageSize", tmpPageSize);
                    LOGGER.debug("pageSize={}", tmpPageSize);
                } else {
                    query.setParameter("pageSize", -1);
                }

                singleResult = (String) query.getSingleResult();
                //System.out.println(singleResult);
            } catch (Exception e) {
                e.printStackTrace();
                return Response.serverError().build();
            } finally {
                JPA.getUtil().closeConnection();
            }
        }

        return Response.ok(singleResult).build();
    }

}

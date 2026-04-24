package com.ib.urireg.rest;


import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.urireg.db.dao.MessageDAO;
import jakarta.mail.internet.MimeUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.List;

@Path("/messages")
public class MessagesRest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagesRest.class);

    @GET
    @Produces("text/plain")
    public String hello() {
        return "MessagesRest INIT!";
    }

    @GET
    @Path("/messagesList")
    @Produces("application/json")
    public Response getMessages() {

        try {

            List<Object[]> resultList = new MessageDAO(null).findMessagesRest(1);

            if (resultList.isEmpty()) {
                LOGGER.info("Няма съобщения за публикуване");
                return Response.noContent().build();
            }

            LOGGER.info("Намерени {} съобщения", resultList.size());
            return Response.ok(resultList).build();

        } catch (Exception e) {
            LOGGER.error("Грешка при извличане на съобщения", e);
            return Response.serverError().build();

        } finally {
            JPA.getUtil().closeConnection();
        }
    }



    @GET
    @Path("/file/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("id") Integer id, @Context HttpServletRequest request) {
        try {
            Files files = new MessageDAO(null).downloadFileRest(id);

            if (files == null) {
                LOGGER.warn("Файл с id={} не е намерен!", id);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            byte[] content = files.getContent();
            if (content == null) {
                content = new byte[0];
            }

            String filename = files.getFilename() != null ? files.getFilename() : "file_" + id;
            String agent = request.getHeader("user-agent");
            String codedFilename;

            if (agent != null && (agent.contains("MSIE") || agent.contains("Edge") || (agent.contains("Mozilla") && agent.contains("rv:11")))) {
                codedFilename = URLEncoder.encode(filename, "UTF-8");
            } else {
                codedFilename = MimeUtility.encodeText(filename, "UTF-8", "B");
            }

            LOGGER.info("Сваляне на файл id={} дължина={}", id, content.length);

            return Response.ok(content)
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Length", content.length)
                    .header("Content-Disposition", "attachment; filename=\"" + codedFilename + "\"")
                    .build();

        } catch (Exception e) {
            LOGGER.error("Грешка при сваляне на файл с ID {}", id, e);
            return Response.serverError().build();
        } finally {
            JPA.getUtil().closeConnection();
        }
    }




}

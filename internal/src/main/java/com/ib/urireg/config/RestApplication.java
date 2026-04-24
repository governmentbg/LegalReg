package com.ib.urireg.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
@OpenAPIDefinition(servers = {  @Server(url = "/urireg/api" ) })
public class RestApplication extends Application {


}

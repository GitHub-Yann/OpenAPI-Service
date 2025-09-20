package org.openapi.controller;


import org.openapi.common.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@RestController
public class HelloServiceController {

	private static final Logger LOGGER = LoggerFactory.getLogger(HelloServiceController.class);

	
	@RequestMapping(value = "/api/v1/healthcheck", method = RequestMethod.GET)
	public Mono<ResponseEntity<?>> healthcheck(ServerWebExchange exchange) {
		LOGGER.info("Health Check called");
		return Mono.just(ResponseEntity.ok(BaseResponse.OK("OK")));
	}
}

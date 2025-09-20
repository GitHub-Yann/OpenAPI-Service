package org.openapi.controller;

import org.openapi.common.BaseResponse;
import org.openapi.common.ConstantsHub;
import org.openapi.service.RemoteCallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@RestController
public class ServiceController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceController.class);

	@Autowired
	private RemoteCallService remoteCallService;
	
	@RequestMapping(value = "/api/**")
	public Mono<ResponseEntity<?>> doGetService(ServerWebExchange exchange) {
		String requestUniqueId = (String) exchange.getAttributes().get(ConstantsHub.REQ_UNIQUE_ID);
		return remoteCallService.processInvoke(exchange,requestUniqueId)
							.onErrorResume(Exception.class, ex -> {
								return Mono.just(createErrorResponse(ex,requestUniqueId));
							});
	}

	private ResponseEntity<String> createErrorResponse(Exception e,String requestUniqueId) {
		LOGGER.error("[{}]EXP", requestUniqueId, e);

		int status = 500;
		String message = "OpenAPI - 未知异常";
		if(e.getMessage() != null){
			message = e.getMessage();
		}
		if(e instanceof WebClientRequestException){
			status = 502;
			message = "OpenAPI - BAD_GATEWAY";
		}

		String errorBody = BaseResponse.toErrorJsonString(status, message);
		return ResponseEntity.status(status)
				.header("Content-Type", "application/json;charset=UTF-8")
				.body(errorBody);
	}
}

package com.github.osinfra.gateway.exception;

import lombok.Getter;

@Getter
public class ServiceNoAnyInstanceException extends RuntimeException {

	private String serviceId;

	public ServiceNoAnyInstanceException(String message, String serviceId) {
		super(message);
		this.serviceId = serviceId;
	}
}

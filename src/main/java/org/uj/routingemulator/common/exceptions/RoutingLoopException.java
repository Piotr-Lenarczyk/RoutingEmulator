package org.uj.routingemulator.common.exceptions;

public class RoutingLoopException extends RuntimeException {
	public RoutingLoopException(String message) {
		super(message);
	}
}

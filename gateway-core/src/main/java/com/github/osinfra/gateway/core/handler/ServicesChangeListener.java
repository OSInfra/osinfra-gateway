package com.github.osinfra.gateway.core.handler;

import java.util.List;

public interface ServicesChangeListener {
	void onChange(List<ServiceInstanceWatcher.ServiceInstanceSource> serviceInstanceSources);

	void cancel();
}
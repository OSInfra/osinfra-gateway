package com.github.osinfra.gateway.handler;

import java.util.List;

public interface ServicesChangeListener {
	void onChange(List<ServiceInstanceWatcher.ServiceInstanceSource> serviceInstanceSources);

	void cancel();
}
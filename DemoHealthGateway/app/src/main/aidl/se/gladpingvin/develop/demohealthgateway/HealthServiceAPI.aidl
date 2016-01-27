package se.gladpingvin.develop.demohealthgateway;

import se.gladpingvin.develop.demohealthgateway.HealthAgentAPI;

interface HealthServiceAPI {
	void ConfigurePassive(HealthAgentAPI agt, in int[] specs);
	String GetConfiguration(String dev);
	void RequestDeviceAttributes(String dev);
	void Unconfigure(HealthAgentAPI agt);
}

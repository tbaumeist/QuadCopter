package com.tbaumeist.quadcontroller;

public interface CommunicationManagerListener {
	void OnMessageReceived(final byte[] buffer);
	void OnInitializedFinished();
	void OnError(String error);
}

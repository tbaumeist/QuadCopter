package com.tbaumeist.quadcontroller;

public interface CommunicationManagerListener {
	void OnMessageReceived(final byte[] buffer, final int size);
	void OnInitializedFinished();
}

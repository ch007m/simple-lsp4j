package dev.snowdrop.lsp4j.demo.client;

import dev.snowdrop.lsp4j.demo.shared.UserMessage;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

@JsonSegment("client")
public interface JdtLsClient {

    /**
     * The `client/didPostMessage` is sent by the server to all clients
     * in a response to the `server/postMessage` notification.
     */
    @JsonNotification
    void didPostMessage(UserMessage message);

}
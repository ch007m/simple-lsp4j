package dev.snowdrop.lsp4j.demo.server;

import dev.snowdrop.lsp4j.chat.shared.UserMessage;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;


@JsonSegment("server")
public interface JdtLsServer {

    /**
     * The `server/postMessage` notification is sent by the client to post a new message.
     * The server should store a message and broadcast it to all clients.
     */
    @JsonNotification
    void postMessage(UserMessage message);

}
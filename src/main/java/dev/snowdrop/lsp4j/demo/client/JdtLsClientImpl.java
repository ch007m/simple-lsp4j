package dev.snowdrop.lsp4j.demo.client;

import dev.snowdrop.lsp4j.demo.shared.UserMessage;

public class JdtLsClientImpl implements JdtLsClient {
    @Override
    public void didPostMessage(UserMessage message) {
        System.out.println("Did post called with : " + message.getUser() + ": " + message.getContent());
    }
}

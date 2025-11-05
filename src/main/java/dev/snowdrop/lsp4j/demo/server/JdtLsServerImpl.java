package dev.snowdrop.lsp4j.demo.server;

import dev.snowdrop.lsp4j.demo.shared.UserMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class JdtLsServerImpl implements JdtLsServer {
    private final List<UserMessage> messages = new CopyOnWriteArrayList<>();

    @Override
    public void postMessage(UserMessage message) {
        messages.add(message);
    }
}

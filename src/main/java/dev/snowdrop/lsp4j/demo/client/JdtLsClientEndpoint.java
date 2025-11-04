package dev.snowdrop.lsp4j.demo.client;

import com.google.gson.JsonObject;
import dev.snowdrop.lsp4j.chat.shared.UserMessage;
import org.eclipse.lsp4j.jsonrpc.Endpoint;

import java.util.concurrent.CompletableFuture;

public class JdtLsClientEndpoint implements Endpoint {

    @Override
    public CompletableFuture<?> request(String method, Object parameter) {
        System.out.println("Client request called - Method: " + method);

        try {
            switch (method) {
                case "client/getTextContent":
                    if (parameter instanceof JsonObject) {
                        JsonObject jsonParam = (JsonObject) parameter;
                        String message = jsonParam.get("message").getAsString();
                        System.out.println("Client received message: " + message);
                    }
                    return CompletableFuture.completedFuture("Message received");

                case "client/showMessage":
                    if (parameter instanceof JsonObject) {
                        JsonObject jsonParam = (JsonObject) parameter;
                        String message = jsonParam.get("message").getAsString();
                        System.out.println("Client received message: " + message);
                    }
                    return CompletableFuture.completedFuture("Message received");

                case "client/showTextContent":
                    if (parameter instanceof JsonObject) {
                        JsonObject jsonParam = (JsonObject) parameter;
                        String content = jsonParam.get("content").getAsString();
                        int length = jsonParam.get("length").getAsInt();
                        System.out.println("Client received text content (" + length + " chars): " +
                            (content.length() > 100 ? content.substring(0, 100) + "..." : content));
                    }
                    return CompletableFuture.completedFuture("Text content received");

                default:
                    System.out.println("Client: Unknown request method: " + method);
                    return CompletableFuture.completedFuture(null);
            }
        } catch (Exception e) {
            System.err.println("Client error handling request: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public void notify(String method, Object parameter) {
        System.out.println("Client notify called - Method: " + method);

        try {
            switch (method) {
                case "client/logMessage":
                    if (parameter instanceof JsonObject) {
                        JsonObject jsonParam = (JsonObject) parameter;
                        String message = jsonParam.get("message").getAsString();
                        String level = jsonParam.has("level") ? jsonParam.get("level").getAsString() : "INFO";
                        System.out.println("[" + level + "] " + message);
                    } else {
                        System.out.println("Client log: " + parameter);
                    }
                    break;

                case "client/messagePosted":
                    if (parameter instanceof UserMessage) {
                        UserMessage msg = (UserMessage) parameter;
                        System.out.println("New message posted by " + msg.getUser() + ": " + msg.getContent());
                    } else if (parameter instanceof JsonObject) {
                        JsonObject jsonParam = (JsonObject) parameter;
                        String user = jsonParam.get("user").getAsString();
                        String content = jsonParam.get("content").getAsString();
                        System.out.println("New message posted by " + user + ": " + content);
                    }
                    break;

                default:
                    System.out.println("Client: Unknown notify method: " + method);
            }
        } catch (Exception e) {
            System.err.println("Client error handling notification: " + e.getMessage());
        }
    }
}

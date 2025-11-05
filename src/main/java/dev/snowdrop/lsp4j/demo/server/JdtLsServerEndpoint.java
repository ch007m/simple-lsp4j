package dev.snowdrop.lsp4j.demo.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.snowdrop.lsp4j.demo.shared.UserMessage;
import org.eclipse.lsp4j.jsonrpc.Endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class JdtLsServerEndpoint implements Endpoint {
    private final List<UserMessage> messages = new CopyOnWriteArrayList<>();
    private String textFileContent = "";

    public JdtLsServerEndpoint() {
        // Load default sample.txt from resources
        loadTextFile("sample.txt");
        System.out.println("HelloServerEndpoint initialized with " + messages.size() + " messages");
    }

    public JdtLsServerEndpoint(String textFilePath) {
        loadTextFile(textFilePath);
        System.out.println("HelloServerEndpoint initialized with " + messages.size() + " messages");
    }

    private void loadTextFile(String filePath) {
        try {
            // First try to load from classpath/resources
            textFileContent = loadFromResources(filePath);
            if (textFileContent != null) {
                System.out.println("Loaded text file from resources: " + filePath + " (" + textFileContent.length() + " characters)");
                return;
            }

            // If not found in resources, try to load from file system
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                textFileContent = Files.readString(path);
                System.out.println("Loaded text file from filesystem: " + filePath + " (" + textFileContent.length() + " characters)");
            } else {
                textFileContent = "Default content - file not found: " + filePath;
                System.out.println("Text file not found in resources or filesystem: " + filePath + ", using default content");
            }
        } catch (IOException e) {
            textFileContent = "Error reading file: " + e.getMessage();
            System.err.println("Error loading text file: " + e.getMessage());
        }
    }

    private String loadFromResources(String resourcePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Error loading resource: " + resourcePath + " - " + e.getMessage());
        }
        return null;
    }

    @Override
    public CompletableFuture<?> request(String method, Object parameter) {
        System.out.println("Server request called - Method: " + method);

        try {
            switch (method) {
                case "server/getTextContent":
                    System.out.println("Getting text content - length: " + textFileContent.length());
                    JsonObject response = new JsonObject();
                    response.addProperty("content", textFileContent);
                    response.addProperty("length", textFileContent.length());
                    return CompletableFuture.completedFuture(response);

                case "server/searchText":
                    System.out.println("Processing text search request...");
                    if (parameter instanceof JsonObject) {
                        JsonObject searchParam = (JsonObject) parameter;
                        String query = searchParam.get("query").getAsString();
                        boolean caseSensitive = searchParam.has("caseSensitive") ? searchParam.get("caseSensitive").getAsBoolean() : false;
                        System.out.println("Searching for: '" + query + "' (case sensitive: " + caseSensitive + ")");
                        return CompletableFuture.completedFuture(searchTextInFile(query, caseSensitive));
                    } else {
                        JsonObject error = new JsonObject();
                        error.addProperty("error", "Search requires a JSON object with 'query' field");
                        return CompletableFuture.completedFuture(error);
                    }

                default:
                    System.out.println("Unknown request method: " + method);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Unknown method: " + method);
                    return CompletableFuture.completedFuture(error);
            }
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            return CompletableFuture.completedFuture(error);
        }
    }

    @Override
    public void notify(String method, Object parameter) {
        System.out.println("Server notify called - Method: " + method + ", Parameter type: " + (parameter != null ? parameter.getClass().getSimpleName() : "null"));

        try {
            switch (method) {
                case "server/postMessage":
                    System.out.println("Processing postMessage notification...");
                    if (parameter instanceof JsonObject) {
                        JsonObject jsonParam = (JsonObject) parameter;
                        System.out.println("JsonObject parameter: " + jsonParam);
                        String user = jsonParam.get("user").getAsString();
                        String content = jsonParam.get("content").getAsString();
                        UserMessage message = new UserMessage(user, content);
                        messages.add(message);
                        System.out.println("Successfully added message from " + user + ": " + content + " (total messages: " + messages.size() + ")");
                    } else if (parameter instanceof UserMessage) {
                        messages.add((UserMessage) parameter);
                        System.out.println("Added UserMessage: " + parameter + " (total messages: " + messages.size() + ")");
                    } else {
                        System.out.println("Unexpected parameter type for postMessage: " + parameter + " (type: " + (parameter != null ? parameter.getClass().getName() : "null") + ")");
                    }
                    break;

                case "server/loadTextFile":
                    if (parameter instanceof JsonObject) {
                        JsonObject jsonParam = (JsonObject) parameter;
                        String filePath = jsonParam.get("filePath").getAsString();
                        loadTextFile(filePath);
                    } else if (parameter instanceof String) {
                        loadTextFile((String) parameter);
                    }
                    break;

                default:
                    System.out.println("Unknown notify method: " + method);
            }
        } catch (Exception e) {
            System.err.println("Error handling notification: " + e.getMessage());
        }
    }

    /**
     * Searches for text within the loaded file content and returns detailed match information
     */
    private JsonObject searchTextInFile(String query, boolean caseSensitive) {
        JsonObject result = new JsonObject();
        JsonArray matches = new JsonArray();

        if (textFileContent == null || textFileContent.isEmpty()) {
            result.addProperty("error", "No text content loaded");
            return result;
        }

        if (query == null || query.trim().isEmpty()) {
            result.addProperty("error", "Search query cannot be empty");
            return result;
        }

        try {
            String[] lines = textFileContent.split("\\r?\\n");
            String searchQuery = caseSensitive ? query : query.toLowerCase();

            int totalMatches = 0;

            // Search line by line for better context and line numbers
            for (int lineNum = 0; lineNum < lines.length; lineNum++) {
                String line = lines[lineNum];
                String searchLine = caseSensitive ? line : line.toLowerCase();

                int index = 0;
                while ((index = searchLine.indexOf(searchQuery, index)) != -1) {
                    JsonObject match = new JsonObject();
                    match.addProperty("lineNumber", lineNum + 1);
                    match.addProperty("columnStart", index + 1);
                    match.addProperty("columnEnd", index + query.length());
                    match.addProperty("matchedText", line.substring(index, index + query.length()));

                    // Provide context - show the full line
                    match.addProperty("lineContent", line);

                    // Provide broader context if available (previous and next lines)
                    JsonObject context = new JsonObject();
                    if (lineNum > 0) {
                        context.addProperty("previousLine", lines[lineNum - 1]);
                    }
                    if (lineNum < lines.length - 1) {
                        context.addProperty("nextLine", lines[lineNum + 1]);
                    }
                    match.add("context", context);

                    matches.add(match);
                    totalMatches++;
                    index += query.length(); // Move past this match
                }
            }

            // Build result summary
            result.addProperty("query", query);
            result.addProperty("caseSensitive", caseSensitive);
            result.addProperty("totalMatches", totalMatches);
            result.addProperty("totalLines", lines.length);
            result.addProperty("searchTime", System.currentTimeMillis());
            result.add("matches", matches);

            System.out.println("Search completed: found " + totalMatches + " matches for '" + query + "'");

        } catch (Exception e) {
            result.addProperty("error", "Search failed: " + e.getMessage());
            System.err.println("Search error: " + e.getMessage());
        }

        return result;
    }
}

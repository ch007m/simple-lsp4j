package dev.snowdrop.lsp4j.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.snowdrop.lsp4j.demo.client.JdtLsClientEndpoint;
import dev.snowdrop.lsp4j.demo.server.JdtLsServerEndpoint;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class LauncherApp {
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Pretty prints a response object. If it's a JsonElement, formats it nicely.
     * Otherwise, converts to JSON and then pretty prints.
     */
    private static void printResponse(String label, Object response) {
        System.out.println(label);
        if (response instanceof JsonElement) {
            String prettyJson = PRETTY_GSON.toJson((JsonElement) response);
            System.out.println(prettyJson + "\n");
        } else {
            // Convert to JSON and then pretty print
            JsonElement jsonElement = PRETTY_GSON.toJsonTree(response);
            String prettyJson = PRETTY_GSON.toJson(jsonElement);
            System.out.println(prettyJson + "\n");
        }
    }

    public static void main(String[] args) throws ExecutionException {
        System.out.println("=== Enhanced LSP4J Demo Application ===");
        System.out.println("This demo shows JSON client requests and text file reading capabilities.\n");

        // Initialize server with text file reading capability
        JdtLsServerEndpoint jdtLsServerEndpoint;
        if (args.length > 0) {
            String textFilePath = args[0];
            System.out.println("Initializing server with custom text file: " + textFilePath);
            jdtLsServerEndpoint = new JdtLsServerEndpoint(textFilePath);
        } else {
            System.out.println("Initializing server with default text file (sample.txt from resources)");
            jdtLsServerEndpoint = new JdtLsServerEndpoint();
        }

        // Use try-with-resources for automatic cleanup
        try (PipedInputStream inClient = new PipedInputStream();
             PipedOutputStream outClient = new PipedOutputStream();
             PipedInputStream inServer = new PipedInputStream();
             PipedOutputStream outServer = new PipedOutputStream()) {

            inClient.connect(outServer);
            outClient.connect(inServer);

            Launcher<LanguageClient> serverLauncher = LSPLauncher.createServerLauncher(
                ServiceEndpoints.toServiceObject(jdtLsServerEndpoint, LanguageServer.class), inServer, outServer);
            Future<Void> serverListening = serverLauncher.startListening();
            System.out.println("✓ Language Server started with text file support");

            JdtLsClientEndpoint jdtLsClientEndpoint = new JdtLsClientEndpoint();
            Launcher<LanguageServer> clientLauncher = LSPLauncher.createClientLauncher(
                ServiceEndpoints.toServiceObject(jdtLsClientEndpoint, LanguageClient.class), inClient, outClient);
            clientLauncher.startListening();
            System.out.println("✓ Language Client started with JSON request support\n");

            // Give servers time to initialize
            Thread.sleep(500);

            // Get the remote service endpoint from the client launcher
            Endpoint remoteEndpoint = clientLauncher.getRemoteEndpoint();

            System.out.println("=== Demonstrating JSON Requests ===\n");

            // First, try the LSP proxy approach (this will fail)
            System.out.println("1a. Trying LSP proxy approach (likely to fail)...");
            try {
                System.out.println("   Sending request to remoteEndpoint...");
                CompletableFuture<?> textContentFuture = remoteEndpoint.request("server/getTextContent", null);
                System.out.println("   Request sent, waiting for response...");
                Object textResult = textContentFuture.get();

                if (textResult != null) {
                    printResponse("   LSP Response:", textResult);
                } else {
                    System.out.println("   LSP Response: null (as expected - proxy doesn't route custom methods)\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Now try direct endpoint access to prove it works
            System.out.println("=== Using direct endpoint access instead of the server proxy ...");
            try {
                CompletableFuture<?> directResult = jdtLsServerEndpoint.request("server/getTextContent", null);
                Object textResult = directResult.get();
                printResponse("   Direct Response:", textResult);
            } catch (Exception e) {
                System.err.println("   Direct Error: " + e.getMessage() + "\n");
            }

            // Demonstrate text search functionality
            System.out.println("=== Client testing text search functionality...");

            // 2a. Search for "LSP4J"
            System.out.println("=== Searching for 'LSP4J' (case insensitive)...");
            JsonObject q = new JsonObject();
            q.addProperty("query", "LSP4J");
            q.addProperty("caseSensitive", false);
            try {
                CompletableFuture<?> searchFuture1 = jdtLsServerEndpoint.request("server/searchText", q);
                Object searchResult1 = searchFuture1.get();
                printResponse("   Search Results:", searchResult1);
            } catch (Exception e) {
                System.err.println("   Search Error: " + e.getMessage() + "\n");
            }


            /*
            //  Test error handling with unknown method via LSP client
            System.out.println(" Client testing error handling with unknown method via LSP...");
            try {
                CompletableFuture<?> errorFuture = remoteEndpoint.request("server/unknownMethod", null);
                Object errorResult = errorFuture.get();
                printResponse("   Error response:", errorResult);
            } catch (Exception e) {
                System.err.println("   Exception: " + e.getMessage() + "\n");
            }*/

            // Allow any pending operations to complete before streams are closed
            Thread.sleep(100);

        } catch (Exception e) {
            System.err.println("Error during demo execution: " + e.getMessage());
        }
    }
}

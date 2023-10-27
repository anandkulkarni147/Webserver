/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author kulka
 */
public class ServerApp {

    /**
     * @param args the command line arguments
     */

    // Logger to log levels
    private static Logger logger = Logger.getLogger(ServerApp.class.getName());

    private static ConcurrentHashMap<Socket, Long> liveConnections = new ConcurrentHashMap<>();

    public ServerApp() {
        liveConnections = new ConcurrentHashMap<>();
    }

    public static ConcurrentHashMap<Socket, Long> getLiveConnections() {
        return liveConnections;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        ServerApp.logger = logger;
    }

    // Create a managed thread pool of 20 threads
    private static ExecutorService managedThreadPool;

    private static long CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);

    public static void setManagedThreadPool() {
        managedThreadPool = Executors.newFixedThreadPool(13);
    }

    private static void threadToFindIdleConnectionsUsingInterrupts() {
        Thread timeout = new Thread(() -> {
            while (true) {
                // close all connections
                while (Thread.currentThread().isInterrupted()) Thread.currentThread().interrupt();
                managedThreadPool.shutdown();
                try {
                    // Sleep after 30 seconds (connections remain alive)
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        timeout.setDaemon(true);
        timeout.start();
    }

    private static void threadToFindIdleConnectionsUsingMap() {
        Thread timeout = new Thread(() -> {
            while (true) {
                // close all idle connections
                for (ConcurrentHashMap.Entry<Socket, Long> entry: liveConnections.entrySet()) {
                    long lastActivityTime = entry.getValue();
                    if (System.currentTimeMillis() - lastActivityTime >= CONNECTION_TIMEOUT) {
                        try {
                            entry.getKey().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            liveConnections.remove(entry.getKey());
                        }
                    }
                }
                try {
                    // Sleep after 60 seconds (connections remain alive)
                    Thread.sleep(TimeUnit.MINUTES.toMillis(CONNECTION_TIMEOUT-1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        timeout.setDaemon(true);
        timeout.start();
    }

    public static void main(String[] args) {
        // TODO code application logic here

        // Loop until correct command line arguments are passed
        while (true) {
            if (args.length != 4 || !args[0].equals("-document_root") || !args[2].equals("-port")) {
                System.out.println("Usage: java ServerApp -document_root 'webcontent' -port <port>");
                System.exit(1);
            } else {
                break;
            }
        }

        // Use below to options for active connections handling
        threadToFindIdleConnectionsUsingMap();
//        threadToFindIdleConnectionsUsingInterrupts();

        ServerApp.setManagedThreadPool();

        // Get the webcontent directory
        String documentRoot = args[1];

        // Get the port number
        int port = Integer.parseInt(args[3]);

        // Active connections
        ConcurrentHashMap<Socket, Long> liveConnections = ServerApp.getLiveConnections();

        // try with resources where server continuously listens on port mentioned
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server listening on port " + port);

            // server listening continuously
            while (true) {
                // accept client request
                Socket clientSocket = serverSocket.accept();

                // Add new request as new connection in active connections list
                liveConnections.put(clientSocket, System.currentTimeMillis());

                // spawn new thread for each request
                managedThreadPool.execute(new ClientHttpRequest(documentRoot, clientSocket));
            }

            // Exception handling
        } catch (IOException e) {
            logger.severe("Exception" + e.toString());
            e.printStackTrace();
        }

    }

}

class ClientHttpRequest implements Runnable {
    private String webcontentDocumentRoot;
    private Socket clientSocket;
    private Logger logger = ServerApp.getLogger();

    private ConcurrentHashMap<Socket, Long> liveConnections = ServerApp.getLiveConnections();

    public String getWebcontentDocumentRoot() {
        return webcontentDocumentRoot;
    }

    public void setWebcontentDocumentRoot(String webcontentDocumentRoot) {
        this.webcontentDocumentRoot = webcontentDocumentRoot;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void setClientSocket(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public ClientHttpRequest(String webcontentDocumentRoot, Socket clientSocket) {
        this.webcontentDocumentRoot = webcontentDocumentRoot;
        this.clientSocket = clientSocket;
    }


    // Overriding run method for each thread
    @Override
    public void run() {

        long activeTime = System.currentTimeMillis();

        try (
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream socketWriter = clientSocket.getOutputStream();
        ) {

            // Get client request and print
            String clientRequest = socketReader.readLine();

            logger.info("Received request " + clientRequest);

            // check if not null
            if (clientRequest != null) {

                // Divide into parts
                String[] splitRequest = clientRequest.split(" ");

                // Check if reuest is valid
                if (splitRequest == null || !splitRequest[0].equals("GET") || splitRequest.length != 3) {
                    logger.warning("Bad or unsupported Request Received: " + clientRequest);

                    // Raise error
                    badRequest_400(socketWriter);
                } else {
                    String resource = splitRequest[1];
                    String httpType = splitRequest[2];

                    if (resource.length() == 0 || resource.isEmpty() || resource.equals("/")) resource = "/index.html";

                    // Generate Resource
                    String requestedResource = webcontentDocumentRoot + resource;

                    // Generate File
                    File requestedFile = new File(requestedResource);

                    // Check if file exists
                    if (!requestedFile.exists() || !requestedFile.isFile()) {
                        logger.warning("Resource not found: " + clientRequest);

                        // File not found
                        notFound_404(socketWriter);
                    } else if (!requestedFile.canRead()) {
                        logger.warning("Request cannot be accessed: " + requestedFile.getPath());

                        // Access denied
                        forbidden_403(socketWriter);
                    } else {
                        String contentType = getContentType(requestedFile);
                        logger.info("Serving file " + requestedFile.getPath());
                        serveFile(socketWriter, requestedFile);
                        activeTime = System.currentTimeMillis();
                    }
                }
            } else {
                logger.warning("Empty request ");
            }

            // set last active time of connection
//            clientSocket.setSoTimeout(0);

            // Exception handling
        } catch (IOException e) {
            logger.severe("Exception" + e.toString());
            e.printStackTrace();
        } finally {
            // close the connection
            try {
                clientSocket.close();
                liveConnections.remove(clientSocket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                liveConnections.remove(clientSocket);
            }
        }
    }

    private void serveFile(OutputStream socketWriter, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            // Write to Stream
            socketWriter.write("HTTP/1.0 200 OK\r\n".getBytes());
            socketWriter.write(("Content-Length: " + file.length() + "\r\n").getBytes());
            socketWriter.write(("Content-Type: " + getContentType(file) + "\r\n").getBytes());
            socketWriter.write(("Date: " + new Date() + "\r\n").getBytes());
            socketWriter.write("\r\n".getBytes());

            // Write in chunks
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                socketWriter.write(buffer, 0, bytesRead);
            }
        }
    }

    private void notFound_404(OutputStream out) throws IOException {

        // Build web page
        String pageToDisplay = "<html>" +
                "<body>" +
                "<h2>404 Page Not found</h2>" +
                "</body>" +
                "</html>";

        // Get Content-Length
        int length = pageToDisplay.length();

        // Write to Stream
        out.write("HTTP/1.0 404 Not Found\r\n".getBytes());
        out.write(("Content-Length: " + length + "\r\n").getBytes());
        out.write("Content-Type: text/html\r\n".getBytes());
        out.write(("Date: " + new Date() + "\r\n").getBytes());
        out.write("\r\n".getBytes());
        out.write(pageToDisplay.getBytes());
    }

    private void forbidden_403(OutputStream out) throws IOException {

        // Build web page
        String pageToDisplay = "<html>" +
                "<body>" +
                "<h2>403 Forbidden Access</h2>" +
                "</body>" +
                "</html>";

        // Get Content-Length
        int length = pageToDisplay.length();

        // Write to Stream
        out.write("HTTP/1.0 403 Not Found\r\n".getBytes());
        out.write(("Content-Length: " + length + "\r\n").getBytes());
        out.write("Content-Type: text/html\r\n".getBytes());
        out.write(("Date: " + new Date() + "\r\n").getBytes());
        out.write("\r\n".getBytes());
        out.write(pageToDisplay.getBytes());
    }

    private void badRequest_400(OutputStream socketWriter) throws IOException {
        // Build web page
        String pageToDisplay = "<html>" +
                "<body>" +
                "<h2>400 Bad Request</h2>" +
                "</body>" +
                "</html>";

        // Get Content-Length
        int length = pageToDisplay.length();

        // Write to Stream
        socketWriter.write("HTTP/1.0 400 Bad Request\r\n".getBytes());
        socketWriter.write(("Content-Length: " + length + "\r\n").getBytes());
        socketWriter.write("Content-Type: text/html\r\n".getBytes());
        socketWriter.write(("Date: " + new Date() + "\r\n").getBytes());
        socketWriter.write("\r\n".getBytes());
        socketWriter.write(pageToDisplay.getBytes());
    }


    // Get Content - Type
    private String getContentType(File file) {
        String fileName = file.getName();

        if (fileName.endsWith(".html") || file.getName().endsWith(".htm")) return "text/html";

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";

        if (fileName.endsWith(".txt")) return "text/plain";

        if (fileName.endsWith(".gif")) return "image/gif";

        if (fileName.endsWith(".png")) return "image/png";

        if (fileName.endsWith(".css")) return "text/css";

        if (fileName.endsWith(".js")) return "text/js";

        return "application/octet-stream";
    }
}

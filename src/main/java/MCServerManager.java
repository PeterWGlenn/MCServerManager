import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

class MCServerManager {

    private static MCServer TestServer1 = new MCServer("TestServer1", 1);
    private static MCServer TestServer2 = new MCServer("TestServer2", 1);

    private static List<MCServer> Servers;

    private static final String APPLICATION_NAME = "MCServerManager";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    public static void main(String args[]) 
    {
        Servers = Arrays.asList(TestServer1, TestServer2);

        say("Starting MCServerManager utility...");
        
        startServers();

        Scanner input = new Scanner(System.in);

        boolean exit = false;
        while(!exit) 
        {
            String line = input.nextLine();
            String[] params = line.split(" ");

            if (params.length > 0) 
            {
                switch (params[0])
                {
                    case "exit":
                        exit = true;
                        break;
                    case "restartAll":
                        stopServers();
                        startServers();
                        break;
                    case "backupAndRestartAll":
                        stopServers();
                        backupServers();
                        startServers();
                        break;
                    case "list":
                        String serverList = "";
                        for (MCServer server : Servers)
                            serverList += server.folder() + " ";
                        say("SERVERS: " + serverList);
                        break;
                    case "status":
                        for (MCServer server : Servers)
                        {
                            if (params.length == 2 && server.folder().equals(params[1]))
                                say(server.status());
                        }
                        break;
                    case "command":
                        for (MCServer server : Servers)
                        {
                            if (params.length >= 3 && server.folder().equals(params[1]))
                            {
                                String com = "";
                                for (int i = 2; i < params.length; i++)
                                    com += params[i] + " ";
                                server.command(com);
                            }
                        }
                        break;
                    default:
                    say("That command is not recognized! Valid commands: exit, restartAll, backupAndRestartAll, list, status [server], command [server] [command]");
                }
            }
        }

        stopServers();

        input.close();
        say("MCServerManager utility closed.");
    }

    private static void startServers()
    {
        for (MCServer server : Servers)
            server.startInThread();
    }

    private static void stopServers()
    {
        for (MCServer server : Servers)
        {
            server.command("say shutting down!");
            server.command("stop");
            server.stopThread();
        }
    }

    private static void backupServers()
    {
        say("starting backups...");
        for (MCServer server : Servers)
            backupServer(server);
        say("finished backups!");
    }

    private static void backupServer(MCServer server)
    {
        say("starting backup for " + server.folder() + "...");
        
		try 
		{
	        // Build a new authorized API client service.
	        NetHttpTransport HTTP_TRANSPORT;
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		    Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
		            .setApplicationName(APPLICATION_NAME)
		            .build();
		
		    // Print the names and IDs for up to 10 files.
		    FileList result = service.files().list()
		            .setPageSize(10)
		            .setFields("nextPageToken, files(id, name)")
		            .execute();
		    List<com.google.api.services.drive.model.File> files = result.getFiles();
		    if (files == null || files.isEmpty())
		        System.out.println("No files found.");
		    else 
		    {
		        System.out.println("Files:");
		        for (com.google.api.services.drive.model.File file : files) 
		            System.out.printf("%s (%s)\n", file.getName(), file.getId());
		    }
		} 
		catch (GeneralSecurityException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        say("finished backup for " + server.folder() + "!");
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     * Code from: https://developers.google.com/drive/api/v3/quickstart/java
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException 
    {
        // Load client secrets.
        InputStream in = MCServerManager.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) 
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static void say(String s)
    {
        System.out.println("[MCSM] -> " + s);
    }

    public static class MCServer implements Runnable
    {
        private Thread _thread;
        private BufferedWriter _buffWriter;
        private boolean _stopThread;

        private StringBuilder _statusString;

        private String _folder;
        private String _ram;

        public MCServer(String folder, int ram) 
        {
            _folder = folder;
            _ram = String.valueOf(ram);
        }

        public String folder()
        {
            return _folder;
        }

        public void startInThread()
        {
            _thread = new Thread(this);
            _stopThread = false;
            _thread.start();
        }

        public void stopThread()
        {
            _stopThread = true;

            try 
            {
                _thread.join();
            }
            catch (InterruptedException interptEx) 
            {
                say(interptEx.toString());
            }
        }

        public void command(String command) 
        {
            try
            {
                _buffWriter.write(command + "\n");
                _buffWriter.flush();
            }
            catch (IOException iOEx) 
            {
                say(iOEx.toString());
            }
        }

        public String status()
        {
            return _statusString.toString();
        }

        public void run()
        {
            Process p = null;

            try 
            {
                ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-Xms" + _ram + "G",
                    "-Xmx" + _ram + "G",
                    "-XX:+UseG1GC",
                    "-XX:+ParallelRefProcEnabled",
                    "-XX:MaxGCPauseMillis=200",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+DisableExplicitGC",
                    "-XX:+AlwaysPreTouch",
                    "-XX:G1NewSizePercent=30",
                    "-XX:G1MaxNewSizePercent=40",
                    "-XX:G1HeapRegionSize=8M",
                    "-XX:G1ReservePercent=20",
                    "-XX:G1HeapWastePercent=5",
                    "-XX:G1MixedGCCountTarget=4",
                    "-XX:InitiatingHeapOccupancyPercent=15",
                    "-XX:G1MixedGCLiveThresholdPercent=90",
                    "-XX:G1RSetUpdatingPauseTimePercent=5",
                    "-XX:SurvivorRatio=32",
                    "-XX:+PerfDisableSharedMem",
                    "-XX:MaxTenuringThreshold=1",
                    "-jar",
                    "server.jar",
                    "nogui");
                pb.directory(new File(_folder));
                //pb.redirectOutput(new File(_folder +  "/MCServerManager_latest_log.txt"));
                pb.redirectErrorStream(true);

                p = pb.start();

                _buffWriter = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                _statusString = new StringBuilder();

                say("Starting server in folder " + _folder + ".");

                // Print all current output lines
                String currLine;
                while((currLine = reader.readLine()) != null && _stopThread == false)
                {
                    _statusString.append("\n\t" + currLine);
                }

                int exitVal = p.waitFor();
                if (exitVal == 0)
                    say("Server in folder " + _folder + " has exited safely.");
                else
                    say("Server in folder " + _folder + " has exited with error code " + exitVal + ".");
            }
            catch (IOException ioEx)
            {
                say(ioEx.toString());
            }
            catch (InterruptedException interptEx) 
            {
                say(interptEx.toString());
            }
            
            if (p != null)
                p.destroy();
        }

        private void say(String s) 
        {
            System.out.println("[" + _folder + "] -> " + s);
        }
    } 
}
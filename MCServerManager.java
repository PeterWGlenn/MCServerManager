import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Scanner;

class MCServerManager {

    private static MCServer TestServer1 = new MCServer("TestServer1", 1);
    private static MCServer TestServer2 = new MCServer("TestServer2", 1);

    private static List<MCServer> Servers;

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
                    case "list":
                        String serverList = "";
                        for (MCServer server : Servers)
                        {
                            serverList += server.folder() + " ";
                        }
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
                    say("That command is not recognized! Try exit or status.");
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
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
import java.util.Scanner;

class MCServerManager {

    private static MCServer TestServer;

    public static void main(String args[]) 
    {
        TestServer = new MCServer("TestServer", 1);

        say("Starting MCServerManager utility...");
        
        TestServer.startInThread();

        Scanner input = new Scanner(System.in);

        boolean exit = false;
        while(!exit) 
        {
            System.out.print("[MCSM] <- ");
            String line = input.nextLine();
            String[] params = line.split(" ");

            switch (params[0])
            {
                case "exit":
                    exit = true;
                    break;
                case "status":
                    say(TestServer.status());
                    break;
                case "command":
                    break;
                case "test":
                    TestServer.command("say testing213!!!");
                    break;
                default:
                say("That command is not recognized! Try exit or status.");
            }
        }

        TestServer.command("say shutting down!");
        TestServer.command("stop");
        TestServer.stopThread();

        input.close();
        say("MCServerManager utility closed.");
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

        public void startInThread()
        {
            _thread = new Thread(TestServer);
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
            catch (InterruptedException interptEx) 
            {
                say(interptEx.toString());
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
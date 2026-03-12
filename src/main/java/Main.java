import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);


        while (true){
            System.out.print("$ ");
            String input = scanner.nextLine().trim();

            if(input.isEmpty()) continue;

            List<String> tokens = Commands.inputTokenizer(input);
            String stdOutFile = Commands.parseRedirection(tokens).get("stdout");
            String stdErrFile = Commands.parseRedirection(tokens).get("stderr");
            String[] parts = tokens.toArray(new String[0]);

            String commandName = parts[0];


            Command command = Commands.get(commandName);

            if(command != null){
                PrintStream originalOut = System.out;

                if(stdOutFile != null){
                    System.setOut(new PrintStream(new FileOutputStream(stdOutFile)));
                }
                if(stdErrFile != null){
                    System.setErr(new PrintStream(new FileOutputStream(stdErrFile)));
                }

                command.execute(parts);
                System.setOut(originalOut);
            }else{

                try{
                    Optional<String> fullPath = Commands.pathResolver(commandName);
                    if(fullPath.isPresent()){

                        String[] cmdArgs = Arrays.copyOfRange(parts, 1, parts.length);
                        StringBuilder sb = new StringBuilder();
                        sb.append("exec -a '");
                        sb.append(commandName.replace("'", "'\\''"));
                        sb.append("' '");
                        sb.append(fullPath.get().replace("'", "'\\''"));
                        sb.append("'");
                        for(String arg : cmdArgs){
                            sb.append(" '");
                            sb.append(arg.replace("'", "'\\''"));
                            sb.append("'");
                        }
                        List<String> commandList = Arrays.asList("/bin/sh", "-c", sb.toString());
                        ProcessBuilder pb = new ProcessBuilder(commandList);
                        if(stdOutFile != null){
                            pb.redirectOutput(new File(stdOutFile));
                            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                        } else if (stdErrFile != null) {
                            pb.redirectError(new File(stdErrFile));
                            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                        } else{
                            pb.inheritIO();
                        }

                        Process process = pb.start();
                        process.waitFor();

                    }else{
                        System.out.println(commandName + ": command not found");
                    }
                } catch (IOException e) {
                    System.out.println(commandName + ": failed to execute");
                } catch (InterruptedException e){
                    System.out.println(commandName + ": interrupted");
                }

            }

        }

    }
}

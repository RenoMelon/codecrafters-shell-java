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
            Map<String, String> redirections = Commands.parseRedirection(tokens);
            String stdOutFile = redirections.get("stdout");
            String stdOutAppendFile = redirections.get("stdoutAppend");
            String stdErrAppendFile = redirections.get("stderrAppend");
            String stdErrFile = redirections.get("stderr");
            String[] parts = tokens.toArray(new String[0]);

            String commandName = parts[0];


            Command command = Commands.get(commandName);

            if(command != null){
                PrintStream originalOut = System.out;
                PrintStream originalErr = System.err;

                if(stdOutFile != null){
                    System.setOut(new PrintStream(new FileOutputStream(stdOutFile)));
                }
                if(stdOutAppendFile != null){
                    System.setOut(new PrintStream(new FileOutputStream(stdOutAppendFile, true)));
                }
                if(stdErrFile != null){
                    System.setErr(new PrintStream(new FileOutputStream(stdErrFile)));
                }
                if(stdErrAppendFile != null){
                    System.setErr(new PrintStream(new FileOutputStream(stdErrAppendFile, true)));
                }


                command.execute(parts);
                System.setOut(originalOut);
                System.setErr(originalErr);
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
                        if (stdOutFile != null) {
                            pb.redirectOutput(new File(stdOutFile));
                        } else if (stdOutAppendFile != null) {
                            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(stdOutAppendFile)));
                        } else {
                            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                        }
                        if(stdErrFile != null){
                            pb.redirectError(new File(stdErrFile));
                        }else if(stdErrAppendFile != null){
                            pb.redirectError(ProcessBuilder.Redirect.appendTo(new File(stdErrAppendFile)));
                        } else{
                            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
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

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {


        Terminal terminal = TerminalBuilder.terminal();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .build();

        boolean[] lastWasTab = {false};

        reader.getKeyMaps().get(LineReader.MAIN).bind((Widget) () -> {

            String buffer = reader.getBuffer().toString();
            boolean commandCompletionState = !buffer.contains(" ");
            boolean filenameCompletionState = buffer.contains(" ");



        if(commandCompletionState){
            List<String> matches = ShellCompleter.getMatches(buffer);
            if(matches.isEmpty()){
                terminal.writer().print("\007"); // bell
                terminal.writer().flush();
                lastWasTab[0] = true;
                return true;
            }

            if (matches.size() == 1) {
                reader.getBuffer().clear();
                reader.getBuffer().write(matches.get(0) + " ");
                lastWasTab[0] = false;
                return true;
            }
            String lcp = ShellCompleter.longestCommonPrefix(matches);

            if(lcp.length() > buffer.length()){
                reader.getBuffer().clear();
                reader.getBuffer().write(lcp);
                lastWasTab[0] = false;
                return true;
            }

            if (!lastWasTab[0]) {
                terminal.writer().print("\007"); // bell
                terminal.writer().flush();
                lastWasTab[0] = true;
            } else {
                terminal.writer().println("\n" + String.join("  ", matches));
                terminal.writer().flush();
                lastWasTab[0] = false;
                reader.callWidget(LineReader.REDRAW_LINE);
            }
            return true;
        }
        if(filenameCompletionState){
            String beforePrefix = buffer.substring(0, buffer.lastIndexOf(" ") + 1);
            List<String> filenameMatches = ShellCompleter.getFileMatches(buffer.substring(buffer.lastIndexOf(" ") + 1));
            if(filenameMatches.isEmpty()){
                terminal.writer().print("\007");
                terminal.writer().flush();
                return true;
            }
            if(filenameMatches.size() == 1){
                reader.getBuffer().clear();
                String match = filenameMatches.getFirst();
                String suffix = match.endsWith("/") ? "" : " ";
                reader.getBuffer().write(beforePrefix + match + suffix);
                return true;
            }

        }

            return true;
        }, "\t");




        while (true){
            //Builtins sectie
            String input = reader.readLine("$ ");

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
            // Executables sectie
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

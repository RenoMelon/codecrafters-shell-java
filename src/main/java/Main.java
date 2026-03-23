import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

            List<String> strippedMatches = filenameMatches.stream()
                    .map(m -> m.endsWith("/") ? m.substring(0, m.length() - 1) : m)
                    .collect(Collectors.toList());
            String lcp = ShellCompleter.longestCommonPrefix(strippedMatches);

            String filePrefix = buffer.substring(buffer.lastIndexOf(" ") + 1);
            if(lcp.length() > filePrefix.length()){
                reader.getBuffer().clear();
                reader.getBuffer().write(beforePrefix + lcp);
                lastWasTab[0] = false;
                return true;
            }

            if (!lastWasTab[0]) {
                terminal.writer().print("\007"); // bell
                terminal.writer().flush();
                lastWasTab[0] = true;
            } else {
                filenameMatches.replaceAll(match -> {
                    String withoutTrailing = match.endsWith("/") ? match.substring(0, match.length() - 1) : match;
                    int lastSlash = withoutTrailing.lastIndexOf("/");
                    return lastSlash >= 0 ? match.substring(lastSlash + 1) : match;
                });
                terminal.writer().println("\n" + String.join("  ", filenameMatches));
                terminal.writer().flush();
                lastWasTab[0] = false;
                reader.callWidget(LineReader.REDRAW_LINE);
            }

        }

            return true;
        }, "\t");

        String histFile = System.getenv("HISTFILE");
        if(histFile != null && !histFile.isEmpty()){
            try {
                Path histPath = Paths.get(histFile);
                if(Files.exists(histPath)){
                    Files.readAllLines(histPath).stream()
                            .filter(l -> !l.isBlank())
                            .forEach(Commands.commandHistory::add);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }


        while (true){
            //Builtins sectie
            String input = reader.readLine("$ ");

            if(input.isEmpty()) continue;
            Commands.commandHistory.add(input);

            if (input.contains("|")) {
                List<String> segments = Commands.splitOnPipe(input);
                executePipeline(segments);
                continue;
            }

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

    private static void executePipeline(List<String> segments) throws IOException, InterruptedException {
        boolean allExternal = segments.stream().allMatch(s -> {
            List<String> t = Commands.inputTokenizer(s);
            return !t.isEmpty() && Commands.get(t.get(0)) == null;
        });

        if (allExternal) {
            List<ProcessBuilder> builders = new ArrayList<>();
            for (String segment : segments) {
                List<String> tokens = Commands.inputTokenizer(segment);
                if (tokens.isEmpty()) continue;
                Optional<String> fp = Commands.pathResolver(tokens.get(0));
                if (fp.isPresent()) tokens.set(0, fp.get());
                ProcessBuilder pb = new ProcessBuilder(tokens);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                builders.add(pb);
            }
            builders.get(0).redirectInput(ProcessBuilder.Redirect.INHERIT);
            builders.get(builders.size() - 1).redirectOutput(ProcessBuilder.Redirect.INHERIT);
            List<Process> processes = ProcessBuilder.startPipeline(builders);
            for (Process p : processes) p.waitFor();
            return;
        }

        // Hybride pad: minstens één builtin aanwezig
        List<Thread> threads = new ArrayList<>();
        List<Process> processes = new ArrayList<>();
        InputStream currentStdin = null;

        for (int i = 0; i < segments.size(); i++) {
            boolean isLast = (i == segments.size() - 1);
            List<String> tokens = Commands.inputTokenizer(segments.get(i));
            if (tokens.isEmpty()) continue;

            String cmdName = tokens.get(0);
            Command builtin = Commands.get(cmdName);
            final InputStream segStdin = currentStdin;

            if (builtin != null) {
                PipedOutputStream writeEnd = null;
                PipedInputStream readEnd = null;
                if (!isLast) {
                    writeEnd = new PipedOutputStream();
                    readEnd = new PipedInputStream(writeEnd);
                }
                final String[] cmdArgs = tokens.toArray(new String[0]);
                final PipedOutputStream outPipe = writeEnd;

                Thread t = new Thread(() -> {
                    PrintStream savedOut = System.out;
                    InputStream savedIn = System.in;
                    try {
                        if (segStdin != null) System.setIn(segStdin);
                        if (outPipe != null) System.setOut(new PrintStream(outPipe, true));
                        builtin.execute(cmdArgs);
                    } finally {
                        System.setOut(savedOut);
                        System.setIn(savedIn);
                        try { if (outPipe != null) outPipe.close(); } catch (IOException e) {}
                    }
                });
                threads.add(t);
                t.start();
                currentStdin = readEnd;

            } else {
                Optional<String> fullPath = Commands.pathResolver(cmdName);
                List<String> cmd = new ArrayList<>(tokens);
                if (fullPath.isPresent()) cmd.set(0, fullPath.get());

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                pb.redirectInput(segStdin == null ? ProcessBuilder.Redirect.INHERIT : ProcessBuilder.Redirect.PIPE);
                pb.redirectOutput(isLast ? ProcessBuilder.Redirect.INHERIT : ProcessBuilder.Redirect.PIPE);

                Process p = pb.start();
                processes.add(p);

                if (segStdin != null) {
                    final InputStream feed = segStdin;
                    final OutputStream sink = p.getOutputStream();
                    Thread feeder = new Thread(() -> {
                        try { feed.transferTo(sink); sink.close(); }
                        catch (IOException e) {}
                    });
                    feeder.start();
                    threads.add(feeder);
                }
                currentStdin = isLast ? null : p.getInputStream();
            }
        }

        for (Thread t : threads) t.join();
        for (Process p : processes) p.waitFor();
    }



}

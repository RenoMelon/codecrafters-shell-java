import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

interface Command {
    void execute(String[] args);
}

public class Commands {
    private static final Map<String, Command> commands = new HashMap<>();
    public static Path currentWorkingDir = Paths.get(System.getProperty("user.dir"));
    public static List<String> commandNames = new ArrayList<String>(Arrays.asList("echo","exit"));

    static {
        commands.put("exit", new Exit());
        commands.put("echo", new Echo());
        commands.put("type", new Type());
        commands.put("pwd", new Pwd());
        commands.put("cd", new Cd());
    }

    public static Command get(String name){
        return commands.get(name);
    }

    public static Optional<String> pathResolver(String commandName){
        String PATH = System.getenv("PATH");
        if(PATH == null || PATH.isEmpty()){
            return Optional.empty();
        }
        String[] paths = PATH.split(File.pathSeparator);

        for(String path : paths){
            File file = new File(path, commandName);
            if(file.exists() && file.canExecute()){
                return Optional.of(file.getAbsolutePath());
            }
        }
        return Optional.empty();
    }

    public static List<String> inputTokenizer(String input){
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        for(char c : input.toCharArray()){
            if(escaped){
                if(inDoubleQuote && c != '"' && c != '\\'){
                    currentArg.append('\\');
                    currentArg.append(c);
                }else{
                    currentArg.append(c);
                }
                escaped = false;
            }
            else if(c == '\\' && !inSingleQuote){
                escaped = true;
            } else if(c == '\'' && !inDoubleQuote){
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == ' ' && !inSingleQuote && !inDoubleQuote) {
                if(!currentArg.isEmpty()){
                    args.add(currentArg.toString());
                    currentArg = new StringBuilder();
                }
            }else{
                currentArg.append(c);
            }

        }

        if(!currentArg.isEmpty()){
            args.add(currentArg.toString());
        }

        return args;
    }

    public static Map<String, String> parseRedirection(List<String> tokens){
        Map<String, String> redirectionMap = new HashMap<>();
        String stdOutFile = null;
        String stdOutAppendFile = null;
        String stdErrFile = null;
        String stdErrAppendFile = null;

        int i = tokens.indexOf(">");
        if(i == -1) i = tokens.indexOf("1>");
        int ii = tokens.indexOf(">>");
        if(ii == -1) ii = tokens.indexOf("1>>");

        int j = tokens.indexOf("2>");
        int jj = tokens.indexOf("2>>");


        if(i != -1) {
            stdOutFile = tokens.get(i + 1);
            tokens.remove(i + 1);
            tokens.remove(i);
        }
        if (ii != -1) {
            stdOutAppendFile = tokens.get(ii + 1);
            tokens.remove(ii + 1);
            tokens.remove(ii);
        }
        if (j != -1) {
            stdErrFile = tokens.get(j + 1);
            tokens.remove(j + 1);
            tokens.remove(j);
        }
        if (jj != -1) {
            stdErrAppendFile = tokens.get(jj + 1);
            tokens.remove(jj + 1);
            tokens.remove(jj);
        }

        redirectionMap.put("stdout", stdOutFile);
        redirectionMap.put("stderr", stdErrFile);
        redirectionMap.put("stdoutAppend", stdOutAppendFile);
        redirectionMap.put("stderrAppend", stdErrAppendFile);

        return redirectionMap;

    }

}

class Exit implements Command{

    public void execute(String[] args) {
        if(args.length > 1) System.exit(Integer.parseInt(args[1]));
        else System.exit(0);
    }
}

class Echo implements Command{

    public void execute(String[] args) {
        if(args.length > 1) System.out.println(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        else System.out.println("echo: missing operand");
    }
}

class Type implements Command{

    public void execute(String[] args) {
        if(args.length < 2){
            System.out.println("type: missing operand");
            return;
        }

        String commandName = args[1];
        Command cmd = Commands.get(commandName);
        if(cmd != null){
            System.out.println(commandName + " is a shell builtin");
        }
        else{
            String commandPath = Commands.pathResolver(commandName).orElse("");
            if(commandPath.isEmpty()){
                System.out.println(commandName + " not found");
            }else{
                System.out.println(commandName + " is " + commandPath);
            }
        }

    }
}

class Pwd implements Command{

    public void execute(String[] args) {
        System.out.println(Commands.currentWorkingDir.toAbsolutePath());
    }
}

class Cd implements Command{

    public void execute(String[] args) {
        if(args.length < 2){
            System.out.println("cd: missing operand");
            return;
        }
        //mss later nog zorgen voor subdir navigatie mogelijkheid met ~ (bv ~/Documents)
        if(args[1].startsWith("~")){
            String home = System.getenv("HOME");
            if(home == null) home = System.getProperty("user.home");
            Path homeDir = Paths.get(home);
            Commands.currentWorkingDir = homeDir.normalize();
            return;
        }

        Path requestedPath = Commands.currentWorkingDir.resolve(args[1]);

        if(Files.isDirectory(requestedPath)){
            Commands.currentWorkingDir = requestedPath.normalize();
        } else {
            System.out.println("cd: " + args[1] + ": " + "No such file or directory");
        }

    }
}

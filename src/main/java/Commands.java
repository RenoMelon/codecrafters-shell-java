import java.io.File;
import java.util.*;

interface Command {
    void execute(String[] args);
}

public class Commands {
    private static final Map<String, Command> commands = new HashMap<>();

    static {
        commands.put("exit", new Exit());
        commands.put("echo", new Echo());
        commands.put("type", new Type());
    }

    public static Command get(String name){
        return commands.get(name);
    }
    /*public boolean getCommands(String command){
        return this.commands.containsKey(command);
    }*/

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
        else System.out.println("echo. missing operand");
    }
}

class Type implements Command{

    public void execute(String[] args) {
        if(args.length < 2){
            System.out.println("type: missing operand");
            return;
        }
        String PATH = System.getenv("PATH");
        String[] paths = PATH.split(File.pathSeparator);

        String commandName = args[1];


        Command cmd = Commands.get(commandName);
        if(cmd != null){
            System.out.println(commandName + " is a shell builtin");
        }
        else{
            for(String path : paths){
                File file = new File(path, commandName);
                if(file.exists() && file.canExecute()){
                    System.out.println(commandName + " is " + file.getAbsolutePath());
                    return;
                }
            }
            System.out.println(commandName + " not found");
        }

    }
}

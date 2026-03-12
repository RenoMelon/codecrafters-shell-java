import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);


        while (true){
            System.out.print("$ ");
            String input = scanner.nextLine().trim();

            if(input.isEmpty()) continue;

            String[] parts = Commands.inputTokenizer(input).toArray(new String[0]);
            String commandName = parts[0];


            Command command = Commands.get(commandName);

            if(command != null){
                command.execute(parts);
            }else{

                try{
                    Optional<String> fullPath = Commands.pathResolver(commandName);
                    if(fullPath.isPresent()){

                        String[] cmdArgs = Arrays.copyOfRange(parts, 1, parts.length);
                        StringBuilder sb = new StringBuilder();
                        sb.append("exec -a ");
                        sb.append(commandName.replace("'", "'\\''"));
                        sb.append(" ");
                        sb.append(fullPath.get().replace("'", "'\\''"));
                        for(String arg : cmdArgs){
                            sb.append(" '");
                            sb.append(arg.replace("'", "'\\''"));
                            sb.append("'");
                        }
                        List<String> commandList = Arrays.asList("/bin/sh", "-c", sb.toString());
                        ProcessBuilder pb = new ProcessBuilder(commandList);
                        pb.inheritIO();
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

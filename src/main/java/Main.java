import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);


        while (true){
            System.out.print("$ ");
            String input = scanner.nextLine().trim();

            if(input.isEmpty()) continue;

            String[] parts = input.split(" ");
            String commandName = parts[0];


            Command command = Commands.get(commandName);

            if(command != null){
                command.execute(parts);
            }else{

                try{
                    Optional<String> fullPath = Commands.pathResolver(commandName);
                    if(fullPath.isPresent()){
                        parts[0] = fullPath.get();
                        ProcessBuilder pb = new ProcessBuilder(parts);
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

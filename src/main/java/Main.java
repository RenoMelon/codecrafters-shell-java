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
                System.out.println(commandName + ": command not found");
            }

        }

    }
}

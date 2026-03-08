import java.util.Objects;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner myScanner = new Scanner(System.in);

        while (true){
            System.out.print("$ ");
            String input = myScanner.nextLine();
            if(input.equals("exit")){
                System.exit(0);
            } else if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));
            }
            else{
                System.out.println(input + ": command not found");
            }

        }

    }
}

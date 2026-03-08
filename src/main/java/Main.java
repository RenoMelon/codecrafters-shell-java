import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner myScanner = new Scanner(System.in);

        while (true){
            System.out.print("$ ");
            String command = myScanner.nextLine();
            System.out.println(command + ": command not found");
        }

    }
}

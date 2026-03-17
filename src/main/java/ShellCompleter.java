import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class ShellCompleter {
    public static List<String> getMatches(String prefix) {
        Set<String> matches = new TreeSet<>(); // TreeSet = auto gesorteerd

        Commands.commands.keySet().stream()
                .filter(name -> name.startsWith(prefix))
                .forEach(matches::add);

        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return new ArrayList<>(matches);

        for (String dir : pathEnv.split(File.pathSeparator)) {
            File folder = new File(dir);
            if (!folder.exists() || !folder.isDirectory()) continue;
            File[] files = folder.listFiles();
            if (files == null) continue;
            for (File file : files) {
                if (file.getName().startsWith(prefix) && file.canExecute())
                    matches.add(file.getName());
            }
        }
        return new ArrayList<>(matches);
    }

    public static String longestCommonPrefix(List<String> matches){
        if(matches.isEmpty()) return "";
        String prefix = matches.get(0);
        for (int i = 1; i < matches.size(); i++){
            while(!matches.get(i).startsWith(prefix)){
                prefix = prefix.substring(0, prefix.length() - 1);
            }
        }
        return prefix;
    }

    public static List<String> getFileMatches(String prefix){
        // zoek in currentWorkingDir naar files die beginnen met de prefix
        List<String> fileMatches = new ArrayList<>();
        File folder;
        String filePrefix = prefix;
        String subDirectory = "";
        if(prefix.contains("/")){
            subDirectory = prefix.substring(0, prefix.lastIndexOf("/") + 1);
            filePrefix = prefix.substring(prefix.lastIndexOf("/") + 1);
            folder = Commands.currentWorkingDir.resolve(subDirectory).toFile();
        }else{
            folder = new File(String.valueOf(Commands.currentWorkingDir));
        }

        if(!folder.exists() || !folder.isDirectory()) return List.of();
        for(File file : folder.listFiles()){
            if(file.getName().startsWith(filePrefix)){
                fileMatches.add(subDirectory + file.getName());
            }
        }
        Collections.sort(fileMatches);
        return fileMatches;
    }
}














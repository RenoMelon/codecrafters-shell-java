import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
}


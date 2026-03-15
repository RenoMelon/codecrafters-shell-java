import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.util.List;

public class ShellCompleter implements Completer {
    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> candidates) {
        String word = parsedLine.word();

        // Builtins completion
        Commands.commands.keySet().forEach(name -> {
            if(name.startsWith(word)) candidates.add(new Candidate(name));
        });

        // External executables completion
        String pathEnv = System.getenv("PATH");
        if(pathEnv == null) return;

        for(String dir : pathEnv.split(File.pathSeparator)) {
            File folder = new File(dir);
            if(!folder.exists() || !folder.isDirectory()) continue;

            File[] files = folder.listFiles();
            if(files == null) continue;

            for(File file : files){
                if(file.getName().startsWith(word) && file.canExecute()){
                    candidates.add(new Candidate(file.getName()));
                }
            }
        }
    }
}

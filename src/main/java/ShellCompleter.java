import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public class ShellCompleter implements Completer {
    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> candidates) {
        String word = parsedLine.word();

        Commands.commands.keySet().forEach(name -> {
            if(name.startsWith(word)) candidates.add(new Candidate(name));
        });
    }
}

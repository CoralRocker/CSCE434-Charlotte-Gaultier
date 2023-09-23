package coco;

import java.io.*;
import java.util.ArrayList;

public class BacktrackableLineReader {

    private BufferedReader file;
    private ArrayList<String> lines;
    private int cursor = 0;
    public BacktrackableLineReader( Reader reader ) throws FileNotFoundException {
        file = new BufferedReader( reader );
        lines = new ArrayList<>();
    }

    public String readLine() throws IOException {
        String line;
        if( cursor < lines.size() ) {
            line = lines.get( cursor - 1 );
        }
        else {
            line = file.readLine();
            lines.add(line);
        }
        cursor++;
        return line;
    }

    public void backtrack( int line ) throws IOException {
        cursor = line;
        if( cursor < 1 || cursor >= lines.size() ) {
            throw new IllegalArgumentException("Line number invalid");
        }
    }

    public void close() throws IOException {
        file.close();
    }

    public int read() throws IOException {
        throw new RuntimeException("Cannot use the read function with BacktrackableLineReader!");
    }
}

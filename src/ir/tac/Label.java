package ir.tac;

public class Label {
    private final String name;
    private final TAC instr;

    public Label(String name, TAC instr) {
        this.name = name;
        this.instr = instr;
    }
}

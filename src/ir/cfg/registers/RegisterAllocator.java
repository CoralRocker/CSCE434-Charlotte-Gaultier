package ir.cfg.registers;

import ir.cfg.CFG;
import ir.tac.Variable;

import java.util.ArrayList;

public class RegisterAllocator {
    private RegisterInteferenceGraph rig;
    private int numRegisters;

    public RegisterAllocator(int n) {
        numRegisters = n;
    }

    public void allocateRegisters(CFG cfg) {
        ArrayList<Variable> nodes = new ArrayList<>();

        rig = new RegisterInteferenceGraph(nodes);


    }
}

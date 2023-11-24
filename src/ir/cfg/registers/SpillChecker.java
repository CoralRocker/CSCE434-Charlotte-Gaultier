package ir.cfg.registers;

import ir.cfg.CFG;
import ir.tac.Assign;
import ir.tac.Assignable;

import java.util.Spliterator;

/**
 * Verify the sensibility of the spill made for a certain variable
 *
 * When possible, minimize the amount of load/store instructions
 */
public class SpillChecker {

    private CFG cfg; // CFG to verify
    private Assignable var; // Target to focus on

    public SpillChecker(CFG cfg, Assignable var) {
        this.cfg = cfg;
        this.var = var;
    }



}

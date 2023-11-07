package ir.cfg.optimizations;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.cfg.CFGVisitor;
import ir.tac.DefinedInBlock;
import ir.tac.UsedInBlock;
import ir.tac.Variable;

import java.util.*;

import java.util.TreeSet;

public class Liveness extends CFGVisitor{

    private CFG cfg;
    private int iters = 0;


    public Liveness(CFG cfg, boolean do_print, boolean do_dce) {
        this.cfg = cfg;

        cfg.markUnvisited();

        // init live entry and exit sets to empty
        cfg.breadthFirst((BasicBlock b) -> {
            // live in
            b.entry = new HashSet<Variable>();
            // live out
            b.exit = new HashSet<Variable>();
        });

        var changed = new Object(){ boolean b = true; };
        while(changed.b) {
            changed.b = false;
            iters++;
            int finalIters = iters;

            cfg.breadthFirst((BasicBlock b) -> {
                if (do_print)
                    System.out.printf("%2d: Processing BB%d\n", finalIters, b.getNum());
                // save cur exit set as prev
                HashSet<Variable> prevExit = new HashSet((HashSet<Variable>)b.exit);
                // anything in the live_in set for successors should be live out for parent (cur)
                for (BasicBlock succ : b.getSuccessors()) {
                    ((HashSet<Variable>)b.exit).addAll((Collection<? extends Variable>) succ.entry);
                }
                // iterate thru instructions to get lists of variable uses and assignments
                HashSet<Variable> uses = getUses(b);
                HashSet<Variable> defs = getDefs(b);

                // kill redefs
                HashSet<Variable> exitNoDefs = new HashSet<>((HashSet<Variable>)b.exit);
                exitNoDefs.removeAll(defs);

                // gen uses + pass-through variables
                ((HashSet<Variable>)b.entry).addAll(uses);
                System.out.print(uses);
                ((HashSet<Variable>)b.entry).addAll(exitNoDefs);

                // check for new uses/defs
                changed.b = !prevExit.equals(b.exit);

                if (do_print)
                    System.out.println();
            });
        }

        if(do_dce){
            // TODO implement
            // loop thru basic blocks
            //      for each instruction
            //      check if its defs has a variable not in exit set
            //          if var is also not in uses add it to kill set
            // run kill
        }


        System.out.printf("Post Optimization:\n%s\n", cfg.asDotGraph());
    }

    private HashSet<Variable> getUses(BasicBlock block) {
        // TODO implement this
        // loop thru instructions
        // read right-hand side
        // create SymbolVals of them
        // maybe create new class to handle
        return new HashSet<Variable>(UsedInBlock.usedInBlock(block));
    }

    private HashSet<Variable> getDefs(BasicBlock block) {
        // TODO implement this
        // loop thru instructions
        // grab all assignments
        // create SymbolVals of them

        // logic in definedInBlock

        return new HashSet<Variable>(DefinedInBlock.defInBlock(block));
    }

    @Override
    public Object visit(BasicBlock blk) {
        return null;
    }

    public int getIterations() {
        return iters;
    }
}

package ir.cfg.registers;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.tac.*;

import java.util.HashMap;

public class LoadStoreCleaner {

    private CFG cfg;
    private final boolean do_print;

    private HashMap<Assignable, Integer> registers;

    private boolean isSpilled(Assignable var) {
        return registers.get(var) == -1;
    }

    public LoadStoreCleaner(CFG cfg, HashMap<Assignable, Integer> regs, boolean do_print) {
        this.do_print = do_print;
        this.cfg = cfg;
        this.registers = regs;
    }

    public void clean() {

        for( BasicBlock blk : cfg.allNodes ) {

            int i = 0;
            var instructions = blk.getInstructions();
            while( i < instructions.size() ) {
                TAC prev = null, curr = null, next = null;
                if( i > 0 ) prev = instructions.get(i-1);
                curr = instructions.get(i);
                if( (i+1) < instructions.size() ) next = instructions.get(i+1);

                if( prev instanceof LoadStack && ((LoadStack)prev).cause == curr ) {
                    LoadStack load = (LoadStack) prev;
                    StoreStack store = (next instanceof StoreStack) ? (StoreStack) next : null;
                    if( store != null && store.cause != curr ) store = null;

                    if( curr instanceof Store) {
                        if( store != null ) { // Have Load -> store -> Store
                            instructions.remove( i );
                            curr.getIdObj().remove();
                            continue;
                        }


                        if( load.dest.equals(((Store) curr).source) && !isSpilled(curr.dest) ) {
                            load.dest = curr.dest;

                            // Remove store
                            instructions.remove(i);
                            curr.getIdObj().remove();
                            continue; // don't increment index
                        }
                    }
                }

                i++;
            }

        }

    }

}

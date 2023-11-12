package ir.cfg.optimizations;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.cfg.CFGVisitor;
import ir.tac.*;

import java.util.*;

import java.util.concurrent.atomic.AtomicInteger;

public class Liveness extends CFGVisitor{

    private CFG cfg;
    private int iters = 0;

    protected boolean isChanged = false;

    public boolean isChanged() {
        return isChanged;
    }

    public Liveness(CFG cfg, boolean do_print, boolean do_dce) {
        this.cfg = cfg;

        cfg.markUnvisited();

        // init live entry and exit sets to empty
        cfg.breadthFirst((BasicBlock b) -> {
            // for each node n in CFG
            // in[n] = ∅; out[n] = ∅
            // live in
            b.live_in = new HashSet<Variable>();
            // live out
            b.live_out = new HashSet<Variable>();
        });

        List<BasicBlock> blockStack = new ArrayList<BasicBlock>();
        cfg.breadthFirst((BasicBlock b) -> {
            blockStack.add(b);
        });

        List<BasicBlock> reverseBlockList = new ArrayList<BasicBlock>();
        while(blockStack.size() > 0){
            reverseBlockList.add(blockStack.get(blockStack.size() - 1));
            blockStack.remove(blockStack.size() - 1);
        }

        var changed = new Object(){ boolean b = true; };
        iters = 0;
        while(changed.b) {

            changed.b = false;
            iters++;
            int finalIters = iters;

            for (BasicBlock b : reverseBlockList) {

//                if (do_print)
//                    System.out.printf("%2d: Processing BB%d\n", finalIters, b.getNum());
                // save cur in/out sets as prev to compare
                HashSet<Variable> prevIn = new HashSet((HashSet<Variable>) b.live_in);
                HashSet<Variable> prevOut = new HashSet((HashSet<Variable>) b.live_out);

                // iterate through instructions to get lists of variable uses and assignments
                HashSet<Variable> uses = getUses(b);

                HashSet<Variable> defs = getDefs(b);

                HashSet<Variable> tempOut = new HashSet<>(prevOut);
                tempOut.removeAll(defs);
                uses.addAll(tempOut);
                b.live_in.addAll(uses);

                for (BasicBlock succ : b.getSuccessors()) {
                    b.live_out.addAll(succ.live_in);
                }

                changed.b = !(prevOut.equals(b.live_out) && (prevIn.equals(b.live_in)));

                if (do_print)
                    System.out.println();
            }
        }

        // printing final live in/out sets for testing
//        cfg.breadthFirst((BasicBlock b) -> {
//            System.out.print(b.getNum());
//            System.out.print(" live in: ");
//            System.out.print(b.live_in);
//            System.out.print(" live out: ");
//            System.out.print(b.live_out);
//            System.out.println();
//        });

        if(do_dce){
            // loop thru basic blocks
            // run kill

            //code deletion
            //
            //find unused variables:
            //
            //use flag = false

            //


            cfg.breadthFirst((BasicBlock b) -> {
                boolean use = false;
                List<TAC> kill = new ArrayList<>();

                HashSet<Variable> uses = getUses(b);
                List<TAC> gen = new ArrayList<>();
                // for each instruction
                int i = 0;

                for (TAC instr : b.getInstructions()) {
                    i++;

                    if (i == 1){
//                        kill.add(instr);
//                        continue;
                    }

                    if (instr instanceof Assign || instr instanceof Store) {
                        TAC assignInstr = instr;

//                        if(!(assignInstr.dest instanceof Variable)){
//                            gen.add(instr);
//                            continue;
//                        }

                        Assignable defVar = (Assignable) assignInstr.dest;

                        //hit an assign/store instruction (not to a temporary)
                        //iterate through subsequent instructions
                        //if used:
                        //	use flag = true

                        int j = i + 1;
                        while(j < b.getInstructions().size()){
                            TAC newInstr = b.getInstructions().get(j);
                            List<Variable> usesInTAC = UsedInBlock.usedInInstr(newInstr);
                            for(Variable var : usesInTAC){
                                if(defVar.equals(var)){
                                    // set use flag
                                    use = true;
                                    break;
                                }
                            }
                            List<Variable> defInTAC = DefinedInBlock.defInInstr(newInstr);
                            for(Variable var : defInTAC){
                                if(defVar.equals(var)){
                                    // set use flag
                                    if(!use){
                                        kill.add(instr);
                                        use = false;
                                    }
                                    break;
                                }
                            }
                            //if it's a redef of the same variable:
                            //	if use flag false:
                            //		add initial instr to kill set
                            //		use flag = false
                            //
                            j++;
                        }


                        //if use flag = false and instr not in liveOut:
                        //	add instr to kill set
                        //

                        //      check if it defs a variable not in exit set

                        if (!((HashSet<Variable>)b.live_out).contains(defVar)) {
                //          if var is also not in uses add it to kill set
                            if(!use){
                                isChanged = true;
                                if (do_print) {
                                    System.out.printf("Removing dead instruction: %s\n", instr);
                                }
                                kill.add(instr);
                                continue;
                            }
                        }
                    }
                    gen.add(instr);
                }

                // update instructions
                for (TAC instr : kill) {
                    cfg.instrNumberer.remove(instr.getIdObj());
                }
                b.getInstructions().clear();
                for (TAC instr : gen){
                    // was erroring with ++ and .getAndIncrement() was the suggested fix
                    b.getInstructions().add(instr);
                }
                cfg.instrNumberer.genNum();
                // reassign IDs to genned instructions
            });
        }
    }

    private HashSet<Variable> getUses(BasicBlock block) {

        // loop thru instructions
        // read right-hand side
        // create SymbolVals of them
        // maybe create new class to handle
        HashSet<Variable> uses = new HashSet<Variable>(UsedInBlock.usedInBlock(block));
        HashSet<Variable> cleanedUses = new HashSet<Variable>(uses);

        for (Variable var : uses){
            for (Variable var2 : uses){
//                System.out.println("( "+var+" , "+var2+" ) " + var.equals(var2));
                if(var.equals(var2)){
                    cleanedUses.remove(var2);
                    cleanedUses.add(var);
                }
            }
        }
        return cleanedUses;
    }

    private HashSet<Variable> getDefs(BasicBlock block) {

        // loop thru instructions
        // grab all assignments
        // create SymbolVals of them

        // logic in definedInBlock
        HashSet<Variable> defs = new HashSet<Variable>(DefinedInBlock.defInBlock(block));
        HashSet<Variable> cleanedDefs = new HashSet<Variable>(defs);

        for (Variable var : defs){
            for (Variable var2 : defs){
//                System.out.println("( "+var+" , "+var2+" ) " + var.equals(var2));
                if(var.equals(var2)){
                    cleanedDefs.remove(var2);
                    cleanedDefs.add(var);
                }
            }
        }
        return cleanedDefs;
    }

    @Override
    public Object visit(BasicBlock blk) {
        return null;
    }

    public int getIterations() {
        return iters;
    }
}

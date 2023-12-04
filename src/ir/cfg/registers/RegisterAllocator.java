package ir.cfg.registers;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.cfg.optimizations.ProgramPointLiveness;
import ir.tac.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class LiveRange {

    public final TacID begin, end;

    public LiveRange( TacID begin, TacID end ) {
        this.begin = begin;
        this.end = end;
    }

    @Override
    public String toString() {
        return String.format("[%d, %d]", begin.getNum(), end.getNum());
    }
}

public class RegisterAllocator {
    private RegisterInteferenceGraph rig;
    private int numRegisters;

    private boolean do_print;

    public RegisterAllocator(int n, boolean do_print) {
        numRegisters = n;
        this.do_print = do_print;
    }

    public HashMap<Assignable, ArrayList<LiveRange>> calculateLiveRange(CFG cfg) {
        HashMap<Assignable, ArrayList<LiveRange>> liveRanges = new HashMap<>();
        HashMap<Assignable, TacID> openRanges = new HashMap<>();

        for( var blk : cfg.allNodes ) {
            for (TAC tac : blk.getInstructions()) {

                tac.liveAfterPP.forEach(var -> {
                    if (!openRanges.containsKey(var)) {
                        openRanges.put(var, tac.getIdObj());
                        if (!liveRanges.containsKey(var)) {
                            liveRanges.put(var, new ArrayList<>());
                        }
                    }
                });

                // We can only kill one var per instruction
                Assignable remove = null;
                for (Assignable asn : openRanges.keySet()) {
                    if (!tac.liveAfterPP.contains(asn)) {
                        remove = asn;
                        break;
                    }
                }

                if (remove != null) {
                    liveRanges.get(remove).add(new LiveRange(openRanges.get(remove), tac.getIdObj()));
                    openRanges.remove(remove);
                }
            }
        }
        return liveRanges;
    }

    public RegisterInteferenceGraph calculateRIG(CFG cfg) {
        RegisterInteferenceGraph newRIG = new RegisterInteferenceGraph();
        for( var blk : cfg.allNodes ) {
            for (TAC tac : blk.getInstructions()) {
                Set<Assignable> liveAtPoint = new HashSet<>();
                liveAtPoint.addAll( tac.liveBeforePP );

                if( tac.dest != null && !tac.liveBeforePP.contains(tac.dest) ) {
                    liveAtPoint.add( tac.dest );
                }

                newRIG.addVariables(cfg, liveAtPoint);
            }
        }

        if( rig != null ) {
            // Copy over old node metadata
            newRIG.mergeNodeInfo(rig);
        }

        return newRIG;
    }


    public HashMap<Assignable, Integer> allocateRegisters(CFG cfg) {


        ProgramPointLiveness liveness = new ProgramPointLiveness(cfg);
        liveness.calculate(do_print);

        if( do_print ) {
            System.out.printf("%-25s | %-20s | %-20s\n", "Instruction", "Live Before", "Live After");
            for (BasicBlock blk : cfg.allNodes) {
                for (TAC tac : blk.getInstructions()) {
                    System.out.printf("%3d: %-20s | %-20s | %-20s\n", tac.getId(), tac.genDot(), tac.liveBeforePP, tac.liveAfterPP);
                }
            }
        }

        rig = calculateRIG(cfg);

        int K = numRegisters;

        TreeSet<Integer> registers = new TreeSet<>(IntStream.range(1, K+1).boxed().collect(Collectors.toSet()));
        HashMap<Assignable, Integer> allocation = new HashMap<>();
        Stack<VariableNode> popped = new Stack<>();

        while( !rig.isEmpty() ) {
            VariableNode ltK =  rig.nodeDegreeLessThan(K);
            if( ltK == null ) {
                ltK = rig.spillHeuristic();
                if( ltK == null )
                    throw new RuntimeException("No spillable nodes???");
            }

            ltK.exclude = true;
            popped.push(ltK);
        }

        int spillNo = 1;
        while( !popped.isEmpty() ) {
            VariableNode node = popped.pop();

            node.exclude = false;
            var connections = rig.connections(node);
            TreeSet<Integer> available = (TreeSet<Integer>) registers.clone();
            available.removeAll(connections);
            if( !available.isEmpty() ) {
                node.assignedRegister = available.first();
                rig.updateNode(node);
                allocation.put(node.var, node.assignedRegister);
            }
            else {
                node.spill = true;
                node.var.spilled = new Spill(spillNo++, Spill.Register.NONE);
                RegisterSpiller spiller = new RegisterSpiller(cfg, node.var, node.var.spilled);
                spiller.generateLoadStores();
                liveness.calculate(false);
                rig = calculateRIG(cfg);

                rig.updateNode(node);

                allocation.put(node.var, -1);
            }

        }

        if( do_print ) {
            rig.resetExclusion();
            System.out.printf("Interference Graph: \n%s\n", rig.asDotGraph());
            // System.out.printf("Modified CFG: \n%s\n", cfg.asDotGraph());
            System.out.printf("Allocation Map: %s\n", allocation);
        }

        return allocation;
    }
}

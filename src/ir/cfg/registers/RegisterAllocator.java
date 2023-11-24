package ir.cfg.registers;

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

    public RegisterAllocator(int n) {
        numRegisters = n;
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
        RegisterInteferenceGraph rig = new RegisterInteferenceGraph();
        for( var blk : cfg.allNodes ) {
            for (TAC tac : blk.getInstructions()) {

                rig.addVariables(tac.liveAfterPP);
            }
        }
        return rig;
    }


    public HashMap<Assignable, Integer> allocateRegisters(CFG cfg) {


        ProgramPointLiveness liveness = new ProgramPointLiveness(cfg);
        liveness.calculate(false);

        rig = calculateRIG(cfg);

        int K = numRegisters - 2; // Reserve Last 2 registers for spilling

        TreeSet<Integer> registers = new TreeSet<>(IntStream.range(0, K).boxed().collect(Collectors.toSet()));
        HashMap<Assignable, Integer> allocation = new HashMap<>();
        Stack<VariableNode> popped = new Stack<>();

        while( !rig.isEmpty() ) {
            VariableNode ltK = rig.nodeDegreeLessThan(K);
            if( ltK == null ) {
                ltK = rig.getNode();
                if( ltK == null )
                    throw new RuntimeException("NEED TO SPILL!");
            }

            ltK.exclude = true;
            popped.push(ltK);
        }

        int spillNo = 0;
        while( !popped.isEmpty() ) {
            VariableNode node = popped.pop();

            node.exclude = false;
            var connections = rig.connections(node);
            TreeSet<Integer> available = (TreeSet<Integer>) registers.clone();
            available.removeAll(connections);
            if( !available.isEmpty() ) {
                node.assignedRegister = available.first();
                allocation.put(node.var, node.assignedRegister);
            }
            else {
                node.spill = true;
                RegisterSpiller spiller = new RegisterSpiller(cfg, node.var, new Spill(spillNo++));
                spiller.generateLoadStores();
                liveness.calculate(false);
                rig = calculateRIG(cfg);

                allocation.put(node.var, -1);
            }

        }

        System.out.printf("Interference Graph: \n%s\n", rig.asDotGraph());
        System.out.printf("Modified CFG: \n%s\n", cfg.asDotGraph());

        System.out.printf("Allocation Map: %s\n", allocation);

        return allocation;
    }
}

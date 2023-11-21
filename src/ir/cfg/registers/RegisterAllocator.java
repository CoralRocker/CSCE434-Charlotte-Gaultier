package ir.cfg.registers;

import ir.cfg.CFG;
import ir.cfg.optimizations.ProgramPointLiveness;
import ir.tac.Assignable;
import ir.tac.TAC;
import ir.tac.TacID;
import ir.tac.Variable;

import java.util.ArrayList;
import java.util.HashMap;

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

    public void allocateRegisters(CFG cfg) {

        rig = new RegisterInteferenceGraph();

        ProgramPointLiveness liveness = new ProgramPointLiveness(cfg);

        HashMap<Assignable, ArrayList<LiveRange>> liveRanges = new HashMap<>();
        HashMap<Assignable, TacID> openRanges = new HashMap<>();

        for( var blk : cfg.allNodes ) {
            for (TAC tac : blk.getInstructions()) {

                rig.addVariables(tac.liveAfterPP);

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

        System.out.printf("Interference Graph: \n%s\n", rig.asDotGraph());

    }
}

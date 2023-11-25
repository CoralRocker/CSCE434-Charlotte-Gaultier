package ir.tac;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.cfg.registers.RegisterAllocator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CodeGenerator extends TACVisitor<DLX> {

    private Map<Assignable, Integer> registers;

    public static List<DLX> generate(CFG cfg, int nRegs) {
        CodeGenerator visitor = new CodeGenerator();

        RegisterAllocator allocator = new RegisterAllocator(nRegs);
        visitor.registers = allocator.allocateRegisters(cfg);

        List<DLX> instructions = new ArrayList<>();

        for( BasicBlock blk : cfg.allNodes ) {
            for( var instr : blk.getInstructions() ) {
                DLX dlx = instr.accept(visitor);
                if( dlx == null ) {
                    throw new RuntimeException("DLX generation returned null for instruction " + instr);
                }
                instructions.add(dlx);
            }
        }

        return instructions;
    }

    public static String generate(BasicBlock blk) {
        boolean changed = false;

        CodeGenerator visitor = new CodeGenerator();

        String toReturn = "";
        int ctr = -1;
        for( var instr : blk.getInstructions() ) {
            ctr++;
            DLX line = instr.accept(visitor);
            if( line != null ) {
                toReturn = toReturn + line.toString() + "\n";
            }
        }
        return toReturn;
    }
    @Override
    public DLX visit(Return ret) {
        return DLX.regOp(DLX.OPCODE.RET, 0, 0, 0);
    }

    @Override
    public DLX visit(Literal lit) {
        return null;
    }

    @Override
    public DLX visit(Call call) {

        // TODO: Generate Code To Save Registers and Whatnot

        switch( call.function.name() ) {
            case "printInt" -> {
                return DLX.regOp(DLX.OPCODE.WRI, 0, registers.get(call.args.get(0)), 0);
            }
        }
        return null;
    }

    @Override
    public DLX visit(Variable var) {
        return null;
    }

    @Override
    public DLX visit(Add add) {
        int dest = registers.get(add.dest);
        if( add.hasImmediate() ) {
            if( add.left instanceof Literal ) {
                return DLX.immediateOp(DLX.OPCODE.ADDI, dest, registers.get(add.right), ((Literal) add.left).getInt());
            }
            else {
                return DLX.immediateOp(DLX.OPCODE.ADDI, dest, registers.get(add.left), ((Literal) add.right).getInt());
            }
        }
        return DLX.immediateOp(DLX.OPCODE.ADD, dest, registers.get(add.left), registers.get(add.right));
    }

    @Override
    public DLX visit(Assign asn) {
        return null;
    }

    @Override
    public DLX visit(Div div) {
        int dest = registers.get(div.dest);
        if( div.hasImmediate() ) {
            if( div.left instanceof Literal ) {
                return DLX.immediateOp(DLX.OPCODE.DIVI, dest, registers.get(div.right), ((Literal) div.left).getInt());
            }
            else {
                return DLX.immediateOp(DLX.OPCODE.DIVI, dest, registers.get(div.left), ((Literal) div.right).getInt());
            }
        }
        return DLX.immediateOp(DLX.OPCODE.DIV, dest, registers.get(div.left), registers.get(div.right));
    }

    @Override
    public DLX visit(Mod mod) {
        int dest = registers.get(mod.dest);
        if( mod.hasImmediate() ) {
            if( mod.left instanceof Literal ) {
                return DLX.immediateOp(DLX.OPCODE.MODI, dest, registers.get(mod.right), ((Literal) mod.left).getInt());
            }
            else {
                return DLX.immediateOp(DLX.OPCODE.MODI, dest, registers.get(mod.left), ((Literal) mod.right).getInt());
            }
        }
        return DLX.immediateOp(DLX.OPCODE.MOD, dest, registers.get(mod.left), registers.get(mod.right));
    }

    @Override
    public DLX visit(Mul mul) {
        int dest = registers.get(mul.dest);
        if( mul.hasImmediate() ) {
            if( mul.left instanceof Literal ) {
                return DLX.immediateOp(DLX.OPCODE.MULI, dest, registers.get(mul.right), ((Literal) mul.left).getInt());
            }
            else {
                return DLX.immediateOp(DLX.OPCODE.MULI, dest, registers.get(mul.left), ((Literal) mul.right).getInt());
            }
        }
        return DLX.immediateOp(DLX.OPCODE.MUL, dest, registers.get(mul.left), registers.get(mul.right));
    }

    @Override
    public DLX visit(Sub sub) {
        int dest = registers.get(sub.dest);
        if( sub.hasImmediate() ) {
            if( sub.left instanceof Literal ) {
                return DLX.immediateOp(DLX.OPCODE.SUBI, dest, registers.get(sub.right), ((Literal) sub.left).getInt());
            }
            else {
                return DLX.immediateOp(DLX.OPCODE.SUBI, dest, registers.get(sub.left), ((Literal) sub.right).getInt());
            }
        }
        return DLX.immediateOp(DLX.OPCODE.SUB, dest, registers.get(sub.left), registers.get(sub.right));
    }

    @Override
    public DLX visit(LoadStack lstack) {
        return null;
    }

    @Override
    public DLX visit(Branch bra) {
        return null;
    }

    @Override
    public DLX visit(Cmp cmp) {
        int dest = registers.get(cmp.dest);
        if( cmp.hasImmediate() ) {
            if( cmp.left instanceof Literal ) {
                return DLX.immediateOp(DLX.OPCODE.CMPI, dest, registers.get(cmp.right), ((Literal) cmp.left).getInt());
            }
            else {
                return DLX.immediateOp(DLX.OPCODE.CMPI, dest, registers.get(cmp.left), ((Literal) cmp.right).getInt());
            }
        }
        return DLX.immediateOp(DLX.OPCODE.CMP, dest, registers.get(cmp.left), registers.get(cmp.right));
    }

    @Override
    public DLX visit(Store store) {
        if( store.source instanceof Literal ) {
            // DEST = R0(always 0) + literal
            return DLX.immediateOp(DLX.OPCODE.ADDI, registers.get(store.dest), 0, ((Literal) store.source).getInt());
        }

        return DLX.regOp(DLX.OPCODE.ADD, registers.get(store.dest), 0, registers.get((Assignable)store.source));
    }

    @Override
    public DLX visit(StoreStack sstack) {
        return null;
    }

    @Override
    public DLX visit(Phi phi) {
        return null;
    }

    @Override
    public DLX visit(Temporary temporary) {
        return null;
    }
}


package ir.cfg.CodeGen;

import ir.cfg.BasicBlock;
import ir.cfg.*;
import ir.cfg.registers.RegisterAllocator;
import ir.tac.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CodeGenerator extends TACVisitor<List<DLXCode>> {

    public static final int STACK_PTR = 29, FRAME_PTR = 28, SPILL_DEST = 27, SPILL_LHS = 26, SPILL_RHS = 25, GLOB_VAR = 30, PREV_PC = 31;

    private Map<Assignable, Integer> registers;
    private boolean isMain;

    public static List<DLXCode> generate(CFG cfg, int nRegs, boolean isMain) {
        CodeGenerator visitor = new CodeGenerator();

        RegisterAllocator allocator = new RegisterAllocator(nRegs);
        visitor.registers = allocator.allocateRegisters(cfg);
        visitor.isMain = isMain;

        List<DLXCode> instructions = new ArrayList<>();

        // if is main, generate the necessary start of stack bullshit
        if( isMain ) {

            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.SUBI, STACK_PTR, GLOB_VAR, -2 * cfg.getSymbols().size() ) );
            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.ADDI, FRAME_PTR, STACK_PTR, 0) );

        }

        for( BasicBlock blk : cfg.allNodes ) {
            for( var instr : blk.getInstructions() ) {
                List<DLXCode> dlx = instr.accept(visitor);
                if( dlx == null ) {
                    throw new RuntimeException("DLXCode generation returned null for instruction " + instr);
                }
                instructions.addAll(dlx);
            }
        }

        if( !instructions.get(instructions.size()-1).getOpcode().equals(DLXCode.OPCODE.RET) ) {
            instructions.add( DLXCode.regOp(DLXCode.OPCODE.RET, 0, 0, 0));
        }

        return instructions;
    }

    @Override
    public List<DLXCode> visit(Return ret) {
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.RET, 0, 0, 0));
    }

    @Override
    public List<DLXCode> visit(Literal lit) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Call call) {

        // TODO: Generate Code To Save Registers and Whatnot

        switch( call.function.name() ) {
            case "printInt" -> {
                return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.WRI, 0, registers.get(call.args.get(0)), 0));
            }
            case "println" -> {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.WRL, 0, 0, 0));
            }
        }
        return null;
    }

    @Override
    public List<DLXCode> visit(Variable var) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Add add) {
        int dest = registers.get(add.dest);
        if( add.hasImmediate() ) {
            if( add.left instanceof Literal ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ADDI, dest, registers.get(add.right), ((Literal) add.left).getInt()));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ADDI, dest, registers.get(add.left), ((Literal) add.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.ADD, dest, registers.get(add.left), registers.get(add.right)));
    }

    @Override
    public List<DLXCode> visit(Assign asn) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Div div) {
        int dest = registers.get(div.dest);
        if( div.hasImmediate() ) {
            if( div.left instanceof Literal ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.DIVI, dest, registers.get(div.right), ((Literal) div.left).getInt()));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.DIVI, dest, registers.get(div.left), ((Literal) div.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.DIV, dest, registers.get(div.left), registers.get(div.right)));
    }

    @Override
    public List<DLXCode> visit(Mod mod) {
        int dest = registers.get(mod.dest);
        if( mod.hasImmediate() ) {
            if( mod.left instanceof Literal ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MODI, dest, registers.get(mod.right), ((Literal) mod.left).getInt()));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MODI, dest, registers.get(mod.left), ((Literal) mod.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.MOD, dest, registers.get(mod.left), registers.get(mod.right)));
    }

    @Override
    public List<DLXCode> visit(Mul mul) {
        int dest = registers.get(mul.dest);
        if( mul.hasImmediate() ) {
            if( mul.left instanceof Literal )
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MULI, dest, registers.get(mul.right), ((Literal) mul.left).getInt()));
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MULI, dest, registers.get(mul.left), ((Literal) mul.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.MUL, dest, registers.get(mul.left), registers.get(mul.right)));
    }

    @Override
    public List<DLXCode> visit(Sub sub) {
        int dest = registers.get(sub.dest);
        if( sub.hasImmediate() ) {
            if( sub.left instanceof Literal ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, registers.get(sub.right), ((Literal) sub.left).getInt()));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, registers.get(sub.left), ((Literal) sub.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.SUB, dest, registers.get(sub.left), registers.get(sub.right)));
    }

    @Override
    public List<DLXCode> visit(LoadStack lstack) {
        return List.of( DLXCode.immediateOp(DLXCode.OPCODE.LDW, lstack.loc.reg.num, FRAME_PTR, -1 * lstack.loc.spillNo) );
    }

    @Override
    public List<DLXCode> visit(Branch bra) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Cmp cmp) {
        int dest = registers.get(cmp.dest);
        if( cmp.hasImmediate() ) {
            if( cmp.left instanceof Literal ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.CMPI, dest, registers.get(cmp.right), ((Literal) cmp.left).getInt()));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.CMPI, dest, registers.get(cmp.left), ((Literal) cmp.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.CMP, dest, registers.get(cmp.left), registers.get(cmp.right)));
    }

    @Override
    public List<DLXCode> visit(Store store) {
        if( store.source instanceof Literal ) {
            // DEST = R0(always 0) + literal
            return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ADDI, registers.get(store.dest), 0, ((Literal) store.source).getInt()));
        }

        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.ADD, registers.get(store.dest), 0, registers.get((Assignable) store.source)));
    }

    @Override
    public List<DLXCode> visit(StoreStack sstack) {
        return List.of( DLXCode.immediateOp(DLXCode.OPCODE.STW, registers.get(sstack.dest), FRAME_PTR, -1 * sstack.loc.spillNo) );
    }

    @Override
    public List<DLXCode> visit(Phi phi) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Temporary temporary) {
        return null;
    }
}


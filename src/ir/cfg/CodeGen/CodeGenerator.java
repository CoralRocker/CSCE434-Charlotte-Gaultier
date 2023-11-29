package ir.cfg.CodeGen;

import coco.DLX;
import coco.FunctionSymbol;
import ir.cfg.BasicBlock;
import ir.cfg.*;
import ir.cfg.registers.RegisterAllocator;
import ir.tac.*;

import java.util.*;

public class CodeGenerator implements TACVisitor<List<DLXCode>> {

    public static final int STACK_PTR = 29, FRAME_PTR = 28, SPILL_DEST = 27, SPILL_LHS = 26, SPILL_RHS = 25, GLOB_VAR = 30, PREV_PC = 31;

    private Map<Assignable, Integer> registers;
    private Map<Integer, Integer> labels; // Associate label number to instruction number at start of label (relative to CFG numbering, not global)

    private int numSpills;
    private boolean isMain;

    private int numSavedRegisters;

    private int instrnum;

    private List<DLXCode> move(int destReg, int srcReg) {
        if( destReg == srcReg )
            return Collections.emptyList();

        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.ADD, destReg, 0, srcReg));
    }

    private List<DLXCode> move(int destReg, Literal src) {
        return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ADDI, destReg, 0, src.getInt()));
    }

    private List<DLXCode> move(int destReg, Assignable src) {
        int reg = registers.get(src);
        return move( destReg, reg );
    }

    public static List<DLXCode> generate(CFG cfg, int nRegs, boolean isMain) {
        cfg.genAllNodes();

        CodeGenerator visitor = new CodeGenerator();

        RegisterAllocator allocator = new RegisterAllocator(nRegs);
        visitor.registers = allocator.allocateRegisters(cfg);
        visitor.labels = new HashMap<>();
        visitor.isMain = isMain;
        visitor.instrnum = 0;
        visitor.numSpills = 0;
        visitor.numSavedRegisters = 4;

        for( var entry : visitor.registers.entrySet() ) {
            if( entry.getValue() == -1 ) visitor.numSpills++;
        }

        List<DLXCode> instructions = new ArrayList<>();

        // if is main, generate the necessary start of stack bullshit
        if( isMain ) {

            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.SUBI, STACK_PTR, GLOB_VAR, 4 * cfg.getSymbols().size() ) );
            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.ADDI, FRAME_PTR, STACK_PTR, 0) );

        }
        else { // Generate Stack Frame Shit
            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, PREV_PC, FRAME_PTR, 0 )); // Save return address
            int arg = 1;
            for( var param : cfg.function.getArgList() ) {
                int dest = visitor.registers.get(new Variable(param));
                System.out.printf("R%d <=> %s (R%d)\n", arg++, param, dest );
                instructions.addAll( visitor.move(dest, arg) );
            }
        }

        for( BasicBlock blk : cfg.allNodes ) {

            visitor.labels.put(blk.getNum(), instructions.size());

            for( var instr : blk.getInstructions() ) {
                List<DLXCode> dlx = instr.accept(visitor);
                if( dlx == null ) {
                    throw new RuntimeException("DLXCode generation returned null for instruction " + instr);
                }
                instructions.addAll(dlx);

                visitor.instrnum = instructions.size();
            }
        }

        if( !instructions.get(instructions.size()-1).getOpcode().equals(DLXCode.OPCODE.RET) ) {
            instructions.add( DLXCode.regOp(DLXCode.OPCODE.RET, 0, 0, 0));
        }

        // Set the address of all branches properly
        var iter = instructions.listIterator();
        int counter = 0;
        while( iter.hasNext() ) {
            var asm = iter.next();

            if( asm.getFormat().equals(DLXCode.FORMAT.UNRESOLVED_BRANCH) ) {
                int bb = visitor.labels.get(asm.immediate);
                int c = bb - counter;
                if( c == 1 ) {
                    iter.remove();
                } else {
                    iter.set(DLXCode.immediateOp(asm.opcode, asm.regA, asm.regB, c));
                }
            }

            counter++;
        }


        return instructions;
    }

    @Override
    public List<DLXCode> visit(Return ret) {
        if( isMain ) {
            return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.RET, 0, 0, 0));
        }
        else {
            List<DLXCode> code = new ArrayList<>();

            // Save Return Value
            if( ret.var instanceof Assignable ) {
                int dest = registers.get( (Assignable) ret.var );
                code.addAll( move(1, dest) );
            }
            else if( ret.var instanceof Literal ){
                code.addAll( move( 1, (Literal) ret.var));
            }

            // Restore RA to R31
            code.add( DLXCode.regOp(DLXCode.OPCODE.LDX, 31, FRAME_PTR, 0 ) );

            // Restore SP and FP
            // old SP is at [SP + 1] and old FP is at [SP]
            code.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, STACK_PTR, FRAME_PTR, 8) );

            code.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, FRAME_PTR, FRAME_PTR, 4) );

            // Issue Return
            code.add( DLXCode.regOp(DLXCode.OPCODE.RET, 0, 0, 31));

            return code;
        }
    }

    @Override
    public List<DLXCode> visit(Literal lit) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Call call) {

        // TODO: Generate Code To Save Registers and Whatnot

        // Default Function Name
        switch( call.function.name() ) {
            case "printInt" -> {
                return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.WRI, 0, registers.get(call.args.get(0)), 0));
            }
            case "println" -> {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.WRL, 0, 0, 0));
            }
            case "printBool" -> {
                return List.of( DLXCode.regOp(DLXCode.OPCODE.WRB, 0, registers.get(call.args.get(0)), 0) );
            }

            case "readInt" -> {
                return List.of( DLXCode.regOp( DLXCode.OPCODE.RDI, registers.get(call.dest), 0, 0) );
            }

            case "readBool" -> {
                return List.of( DLXCode.regOp( DLXCode.OPCODE.RDB, registers.get(call.dest), 0, 0) );
            }
        }

        List<DLXCode> callCode = new ArrayList<>();


        // Save Each Register
        for( int i = 1; i <= numSavedRegisters; i++ ) {
            callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, i, FRAME_PTR, -1 * 4 * (numSpills+i)) );
        }
        // Save the current SP and FP
        callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, STACK_PTR, FRAME_PTR, -1 * 4 * (numSpills + numSavedRegisters + 1)) );
        callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, FRAME_PTR, FRAME_PTR, -1 * 4 * (numSpills + numSavedRegisters + 2)) );

        // Set the arguments
        for ( int arg = 0; arg < call.args.size(); arg++ ) {
            int srcReg = registers.get(call.args.get(arg));
            if( srcReg != (arg + 1) ) {
                callCode.addAll( move(arg+1, srcReg) );
            }
        }

        // Set the new SP and FP
        callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.SUBI, STACK_PTR, FRAME_PTR, 4 * (numSpills + numSavedRegisters + 2)) );
        callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.SUBI, FRAME_PTR, STACK_PTR, 4)); // TODO: Stack spilled args

        callCode.add(DLXCode.unresolvedCall(DLXCode.OPCODE.JSR, ((FunctionSymbol)call.function).typeSignatures()));

        // Restore Registers (but not R1)
        for( int i = 2; i <= numSavedRegisters; i++ ) {
            callCode.add( DLXCode.immediateOp( DLXCode.OPCODE.LDW, i, FRAME_PTR, -1 * 4  * ( numSpills + i ) ) );
        }

        // Save Return to proper variable
        if(registers.containsKey(call.dest)){
            int dest = registers.get(call.dest);
            if( dest != 1 ) {
                callCode.addAll( move(dest, 1) );
            }
        }


        return callCode;
    }

    @Override
    public List<DLXCode> visit(Variable var) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Add add) {
        int dest = registers.get(add.dest);
        if( add.hasImmediate() ) {
            boolean lit_lhs = add.left instanceof Literal,
                    lit_rhs = add.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) add.left).getInt()),
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, dest, SPILL_LHS, ((Literal) add.right).getInt())
                );
            }else if( lit_lhs ) {
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
        throw new RuntimeException("Cannot generate ASM by polymorphic assign! : " + asn );
    }

    @Override
    public List<DLXCode> visit(Div div) {
        int dest = registers.get(div.dest);
        if( div.hasImmediate() ) {
            boolean lit_lhs = div.left instanceof Literal,
                    lit_rhs = div.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) div.left).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.DIVI, dest, SPILL_LHS, ((Literal) div.right).getInt())
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.DIVI, dest, registers.get(div.left), ((Literal) div.right).getInt()));
            }
            else {
                return List.of(
                        move( SPILL_LHS, (Literal) div.left).get(0),
                        DLXCode.regOp(DLXCode.OPCODE.DIV, dest, SPILL_LHS, registers.get(div.right))
                );
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.DIV, dest, registers.get(div.left), registers.get(div.right)));
    }

    @Override
    public List<DLXCode> visit(Mod mod) {
        int dest = registers.get(mod.dest);
        if( mod.hasImmediate() ) {
            boolean lit_lhs = mod.left instanceof Literal,
                    lit_rhs = mod.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) mod.left).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.MODI, dest, SPILL_LHS, ((Literal) mod.right).getInt())
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MODI, dest, registers.get(mod.left), ((Literal) mod.right).getInt()));
            }
            else {
                return List.of(
                        move( SPILL_LHS, (Literal) mod.left).get(0),
                        DLXCode.regOp(DLXCode.OPCODE.MOD, dest, SPILL_LHS, registers.get(mod.right))
                );
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.MOD, dest, registers.get(mod.left), registers.get(mod.right)));
    }

    @Override
    public List<DLXCode> visit(Mul mul) {
        int dest = registers.get(mul.dest);
        if( mul.hasImmediate() ) {
            boolean lit_lhs = mul.left instanceof Literal,
                    lit_rhs = mul.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) mul.left).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.MULI, dest, SPILL_LHS, ((Literal) mul.right).getInt())
                );
            }else if( lit_lhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MULI, dest, registers.get(mul.right), ((Literal) mul.left).getInt()));
            }
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
            boolean lit_lhs = sub.left instanceof Literal,
                    lit_rhs = sub.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) sub.left).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, SPILL_LHS, ((Literal) sub.right).getInt())
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, registers.get(sub.left), ((Literal) sub.right).getInt()));
            }
            else {
                return List.of(
                        move( SPILL_LHS, (Literal) sub.left).get(0),
                        DLXCode.regOp(DLXCode.OPCODE.SUB, dest, SPILL_LHS, registers.get(sub.right))
                );
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.SUB, dest, registers.get(sub.left), registers.get(sub.right)));
    }

    @Override
    public List<DLXCode> visit(LoadStack lstack) {
        int dest = registers.get( lstack.val );
        if( dest == -1 ) throw new RuntimeException("Load stack into spill???");
        return List.of( DLXCode.immediateOp(DLXCode.OPCODE.LDW, dest, FRAME_PTR, -4 * lstack.loc.spillNo) );
    }

    @Override
    public List<DLXCode> visit(Branch bra) {

        int dest = bra.getJumpTo().getNum();
        DLXCode.OPCODE opcode;
        switch( bra.getRel() ) {
            case ">" -> {
                opcode = DLXCode.OPCODE.BGT;
            }
            case ">=" -> {
                opcode = DLXCode.OPCODE.BGE;
            }
            case "==" -> {
                opcode = DLXCode.OPCODE.BEQ;
            }
            case "!=" -> {
                opcode = DLXCode.OPCODE.BNE;
            }
            case "<" -> {
                opcode = DLXCode.OPCODE.BLT;
            }
            case "<=" -> {
                opcode = DLXCode.OPCODE.BLE;
            }
            default -> {
                opcode = DLXCode.OPCODE.BSR;
            }
        }
        if( !labels.containsKey(dest) ) {
            if( bra.isConditional() ) {
                return List.of(DLXCode.unresolvedBranch(opcode, registers.get((Assignable) bra.getVal()), dest));
            }
            else {
                return List.of(DLXCode.unresolvedBranch(opcode, 0, dest));
            }
        }
        else {
            dest = labels.get(dest) - instrnum;
            if( !opcode.equals(DLXCode.OPCODE.BSR) ) {
                return List.of(DLXCode.immediateOp(opcode, registers.get((Assignable) bra.getVal()), 0, dest));
            }
            else {
                return List.of(DLXCode.immediateOp(opcode, 0, 0, dest));
            }
        }
    }

    @Override
    public List<DLXCode> visit(Cmp cmp) {

        //
        // NOTE! The LHS and RHS are flipped here for a reason:
        // The CMP instruction performs R.b - R.c, followed
        //

        int dest = registers.get(cmp.dest);
        if( cmp.hasImmediate() ) {
            boolean lit_lhs = cmp.left instanceof Literal,
                    lit_rhs = cmp.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) cmp.left).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.CMPI, dest, SPILL_LHS, ((Literal) cmp.right).getInt())
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.CMPI, dest, registers.get(cmp.left), ((Literal) cmp.right).getInt()));
            }
            else {
                return List.of(
                        move( SPILL_LHS, (Literal) cmp.left).get(0),
                        DLXCode.regOp(DLXCode.OPCODE.CMP, dest, SPILL_LHS, registers.get(cmp.right))
                );
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.CMP, dest, registers.get(cmp.left), registers.get(cmp.right)));
    }

    @Override
    public List<DLXCode> visit(Store store) {
        int dest = registers.get(store.dest);
        if( dest == -1 ) {
            dest = SPILL_DEST;
        }
        if( store.source instanceof Literal ) {
            // DEST = R0(always 0) + literal
            return move( dest, (Literal) store.source);
        }

        return move( dest, (Assignable) store.source);
    }

    @Override
    public List<DLXCode> visit(StoreStack sstack) {
        return List.of( DLXCode.immediateOp(DLXCode.OPCODE.STW, sstack.loc.reg.num, FRAME_PTR, -4 * sstack.loc.spillNo) );
    }

    @Override
    public List<DLXCode> visit(Phi phi) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Temporary temporary) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Not not) {
        if( not.src instanceof Literal ) {
            // DEST = !literal
            return List.of(
                    move( SPILL_DEST, Literal.get(1) ).get(0),
                    DLXCode.immediateOp(DLXCode.OPCODE.SUB, registers.get(not.dest), SPILL_DEST, ((Literal) not.src).getInt() )
            );
        }
        return List.of(
                move( SPILL_DEST, Literal.get(1)).get(0),
                DLXCode.immediateOp(DLXCode.OPCODE.SUB, registers.get(not.dest), SPILL_DEST, registers.get((Assignable) not.src) )
        );
    }

    @Override
    public List<DLXCode> visit(And and) {
        int dest = registers.get(and.dest);
        if( and.hasImmediate() ) {
            boolean lit_lhs = and.left instanceof Literal,
                    lit_rhs = and.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) and.left).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.ANDI, dest, SPILL_LHS, ((Literal) and.right).getInt())
                );
            }else if( lit_lhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ANDI, dest, registers.get(and.right), ((Literal) and.left).getInt()));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ANDI, dest, registers.get(and.left), ((Literal) and.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.AND, dest, registers.get(and.left), registers.get(and.right)));
    }

    @Override
    public List<DLXCode> visit(Or or) {
        int dest = registers.get(or.dest);
        if (or.hasImmediate()) {
            boolean lit_lhs = or.left instanceof Literal,
                    lit_rhs = or.right instanceof Literal;
            if (lit_rhs && lit_lhs) {
                return List.of(
                        move( SPILL_LHS, (Literal) or.left).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.ORI, dest, SPILL_LHS, ((Literal) or.right).getInt())
                );
            } else if (lit_lhs) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ORI, dest, registers.get(or.right), ((Literal) or.left).getInt()));
            } else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ORI, dest, registers.get(or.left), ((Literal) or.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.OR, dest, registers.get(or.left), registers.get(or.right)));
    }

    @Override
    public List<DLXCode> visit(Xor xor) {
        int dest = registers.get(xor.dest);
        boolean lit_lhs = xor.left instanceof Literal,
                lit_rhs = xor.right instanceof Literal;
        if( lit_rhs && lit_lhs ) {
            return List.of(
                    move( SPILL_LHS, (Literal) xor.left).get(0),
                    DLXCode.immediateOp(DLXCode.OPCODE.XORI, dest, SPILL_LHS, ((Literal) xor.right).getInt())
            );
        }else if( lit_lhs ) {
            return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.XORI, dest, registers.get(xor.right), ((Literal) xor.left).getInt()));
        }
        else if( lit_rhs ) {
            return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.XORI, dest, registers.get(xor.left), ((Literal) xor.right).getInt()));
        }
        else {
            return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.XOR, dest, registers.get(xor.left), registers.get(xor.right)));
        }
    }

    @Override
    public List<DLXCode> visit(Lsh lsh) {
        List<DLXCode> code = new ArrayList<>();
        int dest = registers.get(lsh.dest);
        if( dest == -1 ) {
            dest = SPILL_DEST;
        }
        int lhs;
        int rhs;

        if( lsh.hasImmediate() ) {
            boolean lit_lhs = lsh.left instanceof Literal;
            boolean lit_rhs = lsh.right instanceof Literal;

            if( lit_rhs && lit_lhs ) {
                lhs = SPILL_LHS;
                rhs = SPILL_RHS;

                int exp = ((Literal) lsh.right).getInt();
                code.addAll( move( SPILL_LHS, (Literal) lsh.left) );
                code.add( DLXCode.immediateOp(DLXCode.OPCODE.LSHI, dest, lhs, exp));

            }
            else if( lit_rhs ) {
                lhs = registers.get((Assignable) lsh.left);
                rhs = SPILL_RHS;

                int exp = ((Literal) lsh.right).getInt();

                code.add( DLXCode.immediateOp(DLXCode.OPCODE.LSHI, dest, lhs, exp));

            }
            else {
                lhs = SPILL_LHS;
                rhs = registers.get((Assignable) lsh.right);

                int base = ((Literal) lsh.left).getInt();

                code.addAll( move( SPILL_LHS, (Literal) lsh.left) );
                code.add( DLXCode.regOp(DLXCode.OPCODE.LSH, dest, lhs, rhs));

            }
        }
        else {
            lhs = registers.get(lsh.left);
            if( lhs < 0 ) lhs = SPILL_LHS;
            rhs = registers.get(lsh.right);
            if( rhs < 0 ) rhs = SPILL_RHS;

            code.add( DLXCode.regOp(DLXCode.OPCODE.LSH, dest, lhs, rhs));
        }
        return code;
    }

    @Override
    public List<DLXCode> visit(Ash ash) {
        List<DLXCode> code = new ArrayList<>();
        int dest = registers.get(ash.dest);
        if( dest == -1 ) {
            dest = SPILL_DEST;
        }
        int lhs;
        int rhs;

        if( ash.hasImmediate() ) {
            boolean lit_lhs = ash.left instanceof Literal;
            boolean lit_rhs = ash.right instanceof Literal;

            if( lit_rhs && lit_lhs ) {
                lhs = SPILL_LHS;
                rhs = SPILL_RHS;

                int exp = ((Literal) ash.right).getInt();
                code.addAll( move( SPILL_LHS, (Literal) ash.left) );
                code.add( DLXCode.immediateOp(DLXCode.OPCODE.ASHI, dest, lhs, exp));

            }
            else if( lit_rhs ) {
                lhs = registers.get((Assignable) ash.left);
                rhs = SPILL_RHS;

                int exp = ((Literal) ash.right).getInt();

                code.add( DLXCode.immediateOp(DLXCode.OPCODE.ASHI, dest, lhs, exp));

            }
            else {
                lhs = SPILL_LHS;
                rhs = registers.get((Assignable) ash.right);

                int base = ((Literal) ash.left).getInt();

                code.addAll( move( SPILL_LHS, (Literal) ash.left) );
                code.add( DLXCode.regOp(DLXCode.OPCODE.ASH, dest, lhs, rhs));

            }
        }
        else {
            lhs = registers.get(ash.left);
            if( lhs < 0 ) lhs = SPILL_LHS;
            rhs = registers.get(ash.right);
            if( rhs < 0 ) rhs = SPILL_RHS;

            code.add( DLXCode.regOp(DLXCode.OPCODE.ASH, dest, lhs, rhs));
        }
        return code;
    }

    @Override
    public List<DLXCode> visit(Pow pow) {
        List<DLXCode> code = new ArrayList<>();
        int dest = registers.get(pow.dest);
        if( dest == -1 ) {
            dest = SPILL_DEST;
        }
        int lhs;
        int rhs;

        if( pow.hasImmediate() ) {
            boolean lit_lhs = pow.left instanceof Literal;
            boolean lit_rhs = pow.right instanceof Literal;

            if( lit_rhs && lit_lhs ) {
                lhs = SPILL_LHS;
                rhs = SPILL_RHS;

                int exp = ((Literal) pow.right).getInt();
                code.addAll( move( SPILL_LHS, (Literal) pow.left) );
                code.add( DLXCode.immediateOp(DLXCode.OPCODE.POWI, dest, lhs, exp));

            }
            else if( lit_rhs ) {
                lhs = registers.get((Assignable) pow.left);
                rhs = SPILL_RHS;

                int exp = ((Literal) pow.right).getInt();

                code.add( DLXCode.immediateOp(DLXCode.OPCODE.POWI, dest, lhs, exp));

            }
            else {
                lhs = SPILL_LHS;
                rhs = registers.get((Assignable) pow.right);

                code.addAll( move( SPILL_LHS, (Literal) pow.left) );
                code.add( DLXCode.regOp(DLXCode.OPCODE.POW, dest, lhs, rhs));

            }
        }
        else {
            lhs = registers.get(pow.left);
            if( lhs < 0 ) lhs = SPILL_LHS;
            rhs = registers.get(pow.right);
            if( rhs < 0 ) rhs = SPILL_RHS;

            code.add( DLXCode.regOp(DLXCode.OPCODE.POW, dest, lhs, rhs));
        }
        return code;
    }
}


package ir.tac;

import coco.Symbol;
import ir.cfg.BasicBlock;
import ir.cfg.optimizations.SymbolVal;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class DefinedInBlock implements TACVisitor<List<Variable>> {

    public List<Variable> defined = new ArrayList<>();

    public static List<Variable> defInBlock(BasicBlock blk) {
        DefinedInBlock visitor = new DefinedInBlock();

        for( TAC tac : blk.getInstructions() ) {
            List<Variable> vars = tac.accept(visitor);
            if( vars != null )
                visitor.defined.addAll(vars);
        }

        return visitor.defined;
    }
    public static List<Variable> defInInstr(TAC instr) {
        DefinedInBlock visitor = new DefinedInBlock();

        List<Variable> vars = instr.accept(visitor);
        if( vars != null )
            visitor.defined.addAll(vars);

        return visitor.defined;
    }

    @Override
    public List<Variable> visit(Return ret) {
        return null;
    }

    @Override
    public List<Variable> visit(Load load) {
        return null;
    }


    @Override
    public List<Variable> visit(Literal lit) {
        return null;
    }

    @Override
    public List<Variable> visit(Call call) {
        return null;
    }

    @Override
    public List<Variable> visit(Variable var) {
        return List.of(var);
    }

    @Override
    public List<Variable> visit(Add add) {
        return add.dest.accept(this);
    }

    @Override
    public List<Variable> visit(Assign asn) {
        return asn.dest.accept(this);
    }

    @Override
    public List<Variable> visit(Div div) {
        return div.dest.accept(this);
    }

    @Override
    public List<Variable> visit(Mod mod) {
        return mod.dest.accept(this);
    }

    @Override
    public List<Variable> visit(Mul mul) {
        return mul.dest.accept(this);
    }

    @Override
    public List<Variable> visit(Sub sub) {
        return sub.dest.accept(this);
    }

    @Override
    public List<Variable> visit(LoadStack lstack) {
        return null;
    }

    @Override
    public List<Variable> visit(Branch bra) {
        return null;
    }

    @Override
    public List<Variable> visit(Cmp cmp) {
        return cmp.dest.accept(this);
    }

    @Override
    public List<Variable> visit(Store store) {
        return store.dest.accept(this);
    }

    @Override
    public List<Variable> visit(StoreStack sstack) {
        return null;
    }

    @Override
    public List<Variable> visit(Phi phi) {
        return null;
    }

    @Override
    public List<Variable> visit(Temporary temporary) {
        return null;
    }

    @Override
    public List<Variable> visit(Not not) {
        return not.dest.accept(this);
    }

    @Override
    public List<Variable> visit(And and) {
        return and.dest.accept(this);
    }

    @Override
    public List<Variable> visit(Or or) {
        return or.dest.accept(this);
    }
}

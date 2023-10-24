package ir.tac;

import ir.cfg.BasicBlock;

import java.util.ArrayList;
import java.util.List;

public class DefinedInBlock extends TACVisitor<List<Variable>> {

    protected List<Variable> defined = new ArrayList<>();

    public static List<Variable> defInBlock(BasicBlock blk) {
        DefinedInBlock visitor = new DefinedInBlock();

        for( TAC tac : blk.getInstructions() ) {
            List<Variable> vars = tac.accept(visitor);
            if( vars != null )
                visitor.defined.addAll(vars);
        }

        return visitor.defined;
    }

    @Override
    public List<Variable> visit(Return ret) {
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
    public List<Variable> visit(Phi phi) {
        return null;
    }

    @Override
    public List<Variable> visit(Temporary temporary) {
        return null;
    }
}

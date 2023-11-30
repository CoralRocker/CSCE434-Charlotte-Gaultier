package ir.cfg.optimizations;

import ir.cfg.BasicBlock;
import ir.tac.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExprInBlock implements TACVisitor<Expression> {

    private HashMap<Expression, Expression> avail;
    private boolean do_cse = false;
    private boolean do_cpp = false;

    // Given a store "a = Store b"
    // Searches for an expression which stores to b (can only have one by definition)
    //
    private Expression containsStoreDest( Expression cont ) {
        if( cont.args.length != 1 ) throw new RuntimeException("Can only check for store source of a store expression!");
        if( !(cont.args[0] instanceof Assignable) ) return null; // No redefining literals

        Assignable str = (Assignable) cont.args[0];
        for( Expression expr : avail.keySet() ) {
            if( expr.dest.equals(str) ) {
                return expr;
            }
        }

        return null;
    }

    private Expression containsByDest( Expression cont ) {
        for( Expression expr : avail.keySet() ) {
            if( expr.dest.equals(cont.dest) )
                return expr;
        }
        return null;
    }

    // O(1)
    private Expression contains(Expression cont) {
        return avail.getOrDefault(cont, null);
    }

    // Remove all which contain dest
    // O(n)
    private void kill(Expression expr) {
        List<Expression> toDelete = new ArrayList<>();
        var keys = avail.keySet().iterator();
        while (keys.hasNext()) {
            var key = keys.next();
            if (key.contains(expr.dest) || (key != expr && key.dest.equals(expr.dest)) ) {
                toDelete.add(key);
            }
        }

        for (Expression del : toDelete)
            avail.remove(del);
    }

    public static boolean ExprInBlock(BasicBlock blk, boolean do_cse, boolean do_cpp, boolean do_print) {
        ExprInBlock visitor = new ExprInBlock();
        visitor.do_cse = do_cse;
        visitor.do_cpp = do_cpp;
        visitor.avail = (HashMap<Expression, Expression>) ((HashMap<Expression, Expression>) blk.entry).clone();
        boolean changed = false;

        int ctr = -1;
        for (TAC instr : blk.getInstructions()) {
            ctr++;
            Expression ret = instr.accept(visitor);
            // System.out.printf("Post instruction %2d: %s\n", instr.getId(), visitor.avail.keySet());
            if (ret != null && ret.op.getId() != instr.getId()) {
                // Do Copy Propagation
                if (instr instanceof Store && do_cpp && !(((Store) instr).source instanceof Literal) ) {
                    Store str = new Store( instr.getIdObj(), ((Store)instr).dest, ret.args[0]);
                    blk.getInstructions().set(ctr, str);
                    Expression expr = new Expression(str.dest, str, str.source);
                    if( visitor.avail.containsKey(expr) ) {
                        visitor.containsByDest(expr).setExprNotDest(expr);
                    }

                } else if (instr instanceof Assign && do_cse) {
                    blk.getInstructions().set(ctr, new Store(instr.getIdObj(), ((Assign) instr).dest, ret.dest));
                }
            }
        }

        // Remove Temporaries From Available Expressions To Be propagated.
        var keyIter = visitor.avail.keySet().iterator();
        while( keyIter.hasNext() ) {
            var key = keyIter.next();
            if( key.isTemporary() )
                keyIter.remove();
        }

        HashMap<Expression, Expression> blkmap = (HashMap<Expression, Expression>) blk.exit;
        blk.exit = visitor.avail;

        if (blkmap == null)
            return true;

        if (blkmap.size() != visitor.avail.size())
            return true;

        for (Expression expr : blkmap.keySet()) {
            if (!visitor.avail.containsKey(expr)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Expression visit(Return ret) {
        return null;
    }

    @Override
    public Expression visit(Literal lit) {
        return null;
    }

    @Override
    public Expression visit(Call call) {
        return null;
    }

    @Override
    public Expression visit(Variable var) {
        return null;
    }

    @Override
    public Expression visit(Add add) {
        return visit((Assign) add);
    }

    @Override
    public Expression visit(Assign asn) {
        Expression expr = new Expression(asn.dest, asn, asn.left, asn.right);

        Expression contained = contains(expr);
        Expression retval = null;
        if (contained == null)
            avail.put(expr, expr);
        else
            retval = contained;
        kill(expr);
        return retval;
    }

    @Override
    public Expression visit(Div div) {
        return visit((Assign) div);
    }

    @Override
    public Expression visit(Mod mod) {
        return visit((Assign) mod);
    }

    @Override
    public Expression visit(Mul mul) {
        return visit((Assign) mul);
    }

    @Override
    public Expression visit(Sub sub) {
        return visit((Assign) sub);
    }

    @Override
    public Expression visit(LoadStack lstack) {
        return null;
    }

    @Override
    public Expression visit(Branch bra) {
        return null;
    }

    @Override
    public Expression visit(Cmp cmp) {
        return visit((Assign) cmp);
    }

    @Override
    public Expression visit(Store store) {
        Expression expr = new Expression(store.dest, store, store.source);
        Expression contained = contains( expr );
        // Expression retval = null;
        if (contained == null)
            avail.put(expr, expr);
        // else
        //     retval = contained;
        kill(expr);
        return containsStoreDest(expr);
    }

    @Override
    public Expression visit(StoreStack sstack) {
        return null;
    }

    @Override
    public Expression visit(Phi phi) {
        return null;
    }

    @Override
    public Expression visit(Temporary temporary) {
        return null;
    }

    @Override
    public Expression visit(Not not) {
        Expression expr = new Expression(not.dest, not, not.src);
        Expression contained = containsStoreDest( expr );
        Expression retval = null;
        if (contained == null)
            avail.put(expr, expr);
        else
            retval = contained;
        kill(expr);
        return retval;
    }

    @Override
    public Expression visit(And and) {
        return visit((Assign) and);
    }

    @Override
    public Expression visit(Or or) {
        return visit((Assign) or);
    }
}

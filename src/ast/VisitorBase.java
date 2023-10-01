package ast;

import coco.Token;

import java.util.ArrayList;

public abstract class VisitorBase implements NodeVisitor {

    public VisitorBase() {
    }

    public void exprAction(AST expr) {}
    public void statAction(AST stat) {}

    public void compAction(RootAST comp) {}

    @Override
    public void visit(Addition add) {
        exprAction(add);
        add.getLvalue().accept(this);
        add.getRvalue().accept(this);
        
    }

    @Override
    public void visit(ArgList list) {
        exprAction(list);
        for( AST ast : list.getArgs() ) {
            ast.accept(this);
        }
        
    }

    @Override
    public void visit(ArrayIndex idx) {
        exprAction(idx);
        idx.getArray().accept(this);
        idx.getIndex().accept(this);
        
    }

    @Override
    public void visit(Assignment asn) {
        statAction(asn);
        asn.getTarget().accept(this);
        asn.getRvalue().accept(this);
        
    }

    @Override
    public void visit(BoolLiteral bool) {
        exprAction(bool);
    }

    @Override
    public void visit(DeclarationList list) {
        statAction(list);
        for( AST decl : list.getContained() ) {
            decl.accept(this);
        }
        

    }

    @Override
    public void visit(Designator des) {
        exprAction(des);
    }

    @Override
    public void visit(Division div) {
        exprAction(div);
        div.getLvalue().accept(this);
        div.getRvalue().accept(this);
        
    }

    @Override
    public void visit(FloatLiteral flt) {
        exprAction(flt);
    }

    @Override
    public void visit(FuncBody fb) {
        statAction(fb);
        if( fb.getVarList() != null )
            fb.getVarList().accept(this);
        fb.getSeq().accept(this);
        
    }

    @Override
    public void visit(FuncCall fc) {
        exprAction(fc);
        fc.getArgs().accept(this);
        
    }

    @Override
    public void visit(FuncDecl fd) {
        statAction(fd);
        fd.getBody().accept(this);
        
    }

    @Override
    public void visit(IfStat is) {
        statAction(is);
        is.getIfrel().accept(this);
        is.getIfseq().accept(this);
        if( is.getElseseq() != null ) {
            is.getElseseq().accept(this);
        }
        
    }

    @Override
    public void visit(IntegerLiteral il) {
        exprAction(il);
    }

    @Override
    public void visit(LogicalAnd la) {
        exprAction(la);
        la.getLvalue().accept(this);
        la.getRvalue().accept(this);
        
    }

    @Override
    public void visit(LogicalNot ln) {
        exprAction(ln);
        ln.getRvalue().accept(this);
        
    }

    @Override
    public void visit(LogicalOr lo) {
        exprAction(lo);
        lo.getLvalue().accept(this);
        lo.getRvalue().accept(this);
        
    }

    @Override
    public void visit(Modulo mod) {
        exprAction(mod);
        mod.getLvalue().accept(this);
        mod.getRvalue().accept(this);
        
    }

    @Override
    public void visit(Multiplication mul) {
        exprAction(mul);
        mul.getLvalue().accept(this);
        mul.getRvalue().accept(this);
        
    }

    @Override
    public void visit(Power pwr) {
        exprAction(pwr);
        pwr.getLvalue().accept(this);
        pwr.getRvalue().accept(this);
        

    }

    @Override
    public void visit(Relation rel) {
        exprAction(rel);
        rel.getLvalue().accept(this);
        rel.getRvalue().accept(this);
        

    }


    @Override
    public void visit(RepeatStat rep) {
        statAction(rep);
        rep.getRelation().accept(this);
        rep.getSeq().accept(this);
        
    }

    @Override
    public void visit(Return ret) {
        statAction(ret);
        if( ret.getReturn() != null ) {
            
            ret.getReturn().accept(this);
            
        }
    }

    @Override
    public void visit(RootAST root) {
        compAction(root);
        if( root.getVars() != null )
            root.getVars().accept(this);
        if( root.getFuncs() != null )
            root.getFuncs().accept(this);
        root.getSeq().accept(this);
        
    }

    @Override
    public void visit(StatSeq seq) {
        statAction(seq);
        for( AST ast : seq.getSequence() ) {
            ast.accept(this);
        }
        
    }

    @Override
    public void visit(Subtraction sub) {
        exprAction(sub);
        sub.getLvalue().accept(this);
        sub.getRvalue().accept(this);
        

    }

    @Override
    public void visit(VariableDeclaration var) {
        statAction(var);
    }

    @Override
    public void visit(WhileStat wstat) {
        statAction(wstat);
        wstat.getRelation().accept(this);
        wstat.getSeq().accept(this);
        
    }
}

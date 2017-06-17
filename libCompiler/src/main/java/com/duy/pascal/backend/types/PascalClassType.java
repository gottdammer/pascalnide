package com.duy.pascal.backend.types;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.duy.pascal.backend.ast.AbstractFunction;
import com.duy.pascal.backend.ast.ConstantDefinition;
import com.duy.pascal.backend.ast.FunctionDeclaration;
import com.duy.pascal.backend.ast.VariableDeclaration;
import com.duy.pascal.backend.ast.codeunit.CodeUnit;
import com.duy.pascal.backend.ast.codeunit.classunit.ClassConstructor;
import com.duy.pascal.backend.ast.codeunit.classunit.ClassConstructorCall;
import com.duy.pascal.backend.ast.codeunit.classunit.ClassExpressionContext;
import com.duy.pascal.backend.ast.codeunit.classunit.PascalClassDeclaration;
import com.duy.pascal.backend.ast.codeunit.classunit.RuntimePascalClass;
import com.duy.pascal.backend.ast.expressioncontext.ExpressionContext;
import com.duy.pascal.backend.ast.expressioncontext.ExpressionContextMixin;
import com.duy.pascal.backend.ast.runtime_value.value.NullValue;
import com.duy.pascal.backend.ast.runtime_value.value.RuntimeValue;
import com.duy.pascal.backend.parse_exception.ParsingException;
import com.duy.pascal.backend.parse_exception.define.AmbiguousFunctionCallException;
import com.duy.pascal.backend.parse_exception.define.BadFunctionCallException;
import com.duy.pascal.backend.parse_exception.index.NonArrayIndexed;
import com.duy.pascal.backend.tokens.WordToken;
import com.google.common.collect.ArrayListMultimap;

import java.util.ArrayList;
import java.util.List;

public class PascalClassType extends ObjectType {
    private static final String TAG = "ClassType";

    private PascalClassDeclaration mPascalClassDeclaration;
    private ExpressionContext parent;

    private ArrayListMultimap<String, ClassConstructor> mConstructors = ArrayListMultimap.create();

    public PascalClassType(CodeUnit root, ExpressionContext parent) throws ParsingException {
        this.parent = parent;
        mPascalClassDeclaration = new PascalClassDeclaration(root, parent, null);
        addDefaultConstructor();
    }

    private void addDefaultConstructor() throws ParsingException {
        ClassConstructor def = new ClassConstructor(this, parent);
        addConstructor(def);
    }

    public ArrayListMultimap<String, ClassConstructor> getConstructors() {
        return mConstructors;
    }

    public ClassConstructorCall generateConstructor(WordToken name, List<RuntimeValue> arguments,
                                                    ExpressionContext expressionContext)
            throws ParsingException {
        List<ClassConstructor> classConstructors = mConstructors.get(name.getName().toLowerCase());

        boolean matching = false;

        AbstractFunction chosen = null;
        AbstractFunction ambiguous = null;
        ClassConstructorCall result;
        ClassConstructorCall runtimeValue = null;

        for (ClassConstructor function : classConstructors) {
            result = function.generatePerfectFitCall(name.getLineNumber(), arguments, expressionContext);
            if (result != null) {
                chosen = function;
                runtimeValue = result;
                break;
            }
            result = function.generateCall(name.getLineNumber(), arguments, expressionContext);
            if (result != null) {
                if (chosen != null) {
                    ambiguous = chosen;
                }
                chosen = function;
                if (runtimeValue == null)
                    runtimeValue = result;
            }
            if (function.argumentTypes().length == arguments.size()) {
                matching = true;
            }
        }

        if (runtimeValue == null) {
            ArrayList<String> argsType = new ArrayList<>();
            for (int i = 0; i < arguments.size(); i++) {
                argsType.add(String.valueOf(arguments.get(i).getType(expressionContext)));
            }
            ArrayList<String> listFunctions = new ArrayList<>();
            for (AbstractFunction function : classConstructors) {
                listFunctions.add(function.toString());
            }
            throw new BadFunctionCallException(name.getLineNumber(),
                    name.name, !classConstructors.isEmpty(), matching, argsType, listFunctions, expressionContext);
        } else if (ambiguous != null) {
            throw new AmbiguousFunctionCallException(name.getLineNumber(), chosen, ambiguous);
        } else {
            return runtimeValue;
        }
    }

    public FunctionDeclaration getConstructor(ClassConstructor other) throws ParsingException {
        List<ClassConstructor> abstractFunctions = mConstructors.get(other.getName());
        for (ClassConstructor constructor : abstractFunctions) {
            if (constructor.headerMatches(other)) {
                return constructor;
            }
        }
        return null;
    }

    public void addConstructor(ClassConstructor constructor) {
        this.mConstructors.put(constructor.getName(), constructor);
    }

    public ClassExpressionContext getClassContext() {
        return (ClassExpressionContext) mPascalClassDeclaration.getContext();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public void setName(String name) {
        super.setName(name);
    }

    @NonNull
    @Override
    public Object initialize() {
        return NullValue.get();
    }

    @Nullable
    @Override
    public Class getTransferClass() {
        return RuntimePascalClass.class;
    }

    @Nullable
    @Override
    public RuntimeValue convert(RuntimeValue other, ExpressionContext f) throws ParsingException {
        RuntimeType otherType = other.getType(f);
        if (this.equals(otherType.declType)) {
            return other;
        }
        return null;
    }

    @Override
    public boolean equals(DeclaredType other) {
        if (this == other) return true;
        if (other instanceof PascalClassType) {
            PascalClassType otherClassType = (PascalClassType) other;
            return otherClassType.getDeclaration().equals(mPascalClassDeclaration);
        }
        return false;
    }

    @Nullable
    @Override
    public RuntimeValue cloneValue(RuntimeValue r) {
        return r;
    }

    @Nullable
    @Override
    public RuntimeValue generateArrayAccess(RuntimeValue array, RuntimeValue index) throws NonArrayIndexed {
        throw new NonArrayIndexed(lineInfo, this);
    }

    @Nullable
    @Override
    public Class<?> getStorageClass() {
        return RuntimePascalClass.class;
    }

    public void addPrivateFunction(FunctionDeclaration functionDeclaration) {
        mPascalClassDeclaration.context.declareFunction(functionDeclaration);
    }

    public void addPublicFunction(FunctionDeclaration functionDeclaration) {
        mPascalClassDeclaration.context.declareFunction(functionDeclaration);
    }

    @Override
    public DeclaredType getMemberType(String name) throws ParsingException {
        ExpressionContextMixin context = mPascalClassDeclaration.getContext();

        VariableDeclaration var = context.getVariableDefinitionLocal(name);
        if (var != null) return var.getType();

        ConstantDefinition constant = context.getConstantDefinitionLocal(name);
        if (constant != null) return constant.getType();

        return null;
    }

    @Override
    public String getEntityType() {
        return "class";
    }


    public void addPrivateField(ArrayList<VariableDeclaration> vars) {
        for (VariableDeclaration var : vars) {
            mPascalClassDeclaration.context.declareVariable(var);
        }
        // TODO: 16-Jun-17
    }

    public void addPublicFields(List<VariableDeclaration> vars) {
        // TODO: 16-Jun-17
        for (VariableDeclaration var : vars) {
            mPascalClassDeclaration.context.declareVariable(var);
        }
    }


    public PascalClassDeclaration getDeclaration() {
        return mPascalClassDeclaration;
    }
}
package com.duy.pascal.backend.pascaltypes;

import com.duy.pascal.backend.exceptions.ParsingException;
import com.duy.pascal.backend.pascaltypes.rangetype.SubrangeType;
import com.js.interpreter.ast.expressioncontext.ExpressionContext;
import com.js.interpreter.ast.returnsvalue.ArrayAccess;
import com.js.interpreter.ast.returnsvalue.ReturnValue;
import com.js.interpreter.ast.returnsvalue.cloning.ArrayCloner;
import com.ncsa.common.util.TypeUtils;

import java.lang.reflect.Array;


public class ArrayType<T extends DeclaredType> implements DeclaredType {
    private static final String TAG = "ArrayType";
    public final T element_type;
    private SubrangeType bounds;


    public ArrayType(T elementclass, SubrangeType bounds) {
        this.element_type = elementclass;
        this.bounds = bounds;
    }


    public T getElement_type() {
        return element_type;
    }

    public SubrangeType getBounds() {
        return bounds;
    }

    /**
     * This basically tells if the types are assignable from each other
     * according to Pascal.
     */
    public boolean superset(DeclaredType obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ArrayType) {
            ArrayType<?> o = (ArrayType<?>) obj;
            if (o.element_type.equals(element_type)) {
                if (this.bounds.contains(o.bounds)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(DeclaredType obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ArrayType) {
            ArrayType<?> o = (ArrayType<?>) obj;
            if (o.element_type.equals(element_type)) {
                if (this.bounds.equals(o.bounds)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (element_type.hashCode() * 31 + bounds.hashCode());
    }

    /**
     * TODO: Must make this actually fill in array with default values
     */
    @Override
    public Object initialize() {
        Object result = Array.newInstance(element_type.getTransferClass(),
                bounds.size);
        for (int i = 0; i < bounds.size; i++) {
            Array.set(result, i, element_type.initialize());
        }
        return result;
    }

    @Override
    public Class<?> getTransferClass() {
        String s = element_type.getTransferClass().getName();
        StringBuilder b = new StringBuilder();
        b.append('[');
        b.append('L');
        b.append(s);
        b.append(';');
        try {
            return Class.forName(b.toString());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(element_type.toString());
        result.append('[');
        result.append(bounds);
        result.append(']');
        return result.toString();
    }

    /**
     * This basically won't do any conversions, as array types have to be exact,
     * except variable length arrays, but they are checked in the {@link
     */
    @Override
    public ReturnValue convert(ReturnValue value, ExpressionContext f)
            throws ParsingException {
        RuntimeType other = value.getType(f);
        return this.superset(other.declType) ? cloneValue(value) : null;
    }

    @Override
    public ReturnValue cloneValue(final ReturnValue r) {
        return new ArrayCloner<T>(r);
    }


    @Override
    public ReturnValue generateArrayAccess(ReturnValue array,
                                           ReturnValue index) {
        return new ArrayAccess(array, index, bounds.lower);
    }

    @Override
    public Class<?> getStorageClass() {
        Class c = element_type.getStorageClass();
        if (c.isArray()) {
            try {
                return Class.forName("[" + c.getName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        } else if (c.isPrimitive()) {
            c = TypeUtils.getClassForType(c);
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        b.append('L');
        b.append(c.getName());
        b.append(';');
        try {
            return Class.forName(b.toString());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

    }


}

package net.robinx.lib.http.utils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Parse helper
 * @author Robin
 * @since 2015-11-10 14:50:21
 *
 */
public class GenericsUtils {

    public static Type[] getGenericOuterTypeByMethod(Class<?> cls,String methodName,Class<?> parameterTypes){
        try {
            Method method = cls.getMethod(methodName, parameterTypes) ;
            Type[] pType = method.getParameterTypes();
            return pType;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Type[] getGenericInnerTypeByMethod(Class<?> cls,String methodName,Class<?> parameterTypes){
        try {
            Method method = cls.getMethod(methodName, parameterTypes) ;
            Type[] innerType = method.getGenericParameterTypes();
            return innerType;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


	public static <T> Type getBeanClassType(T listener) {
        Type type;
        try {
            Type[] typs = GenericsUtils.getGenericInterfaces(listener.getClass());
            if (typs != null) {
                type = typs[0];
            } else {
                type = GenericsUtils.getGenericSuperclass(listener.getClass())[0];
            }
        } catch (Exception e) {
            throw new RuntimeException("unknow type");
        }
        return type;
    }
    
    /**
     * Take the parent class generic
     * @param clazz
     * @return 
     */
    public static Type[] getGenericSuperclass(Class<?> clazz) {
        try {
            Type typeGeneric = clazz.getGenericSuperclass();
            if (typeGeneric != null) {
                if (typeGeneric instanceof ParameterizedType) {
                    return getGeneric((ParameterizedType) typeGeneric);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * Take the parent interface generic
     * @param clazz
     * @return 
     */
    public static Type[] getGenericInterfaces(Class<?> clazz) {
        try {
            Type typeGeneric = clazz.getGenericInterfaces()[0];
            if (typeGeneric != null) {
                if (typeGeneric instanceof ParameterizedType) {
                    return getGeneric((ParameterizedType) typeGeneric);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * Take a generic
     * @param type
     * @return 
     */
    public static Type[] getGeneric(ParameterizedType type) {
        try {
            if (type != null) {
                Type[] typeArgs = type.getActualTypeArguments();
                if (typeArgs != null && typeArgs.length > 0) {
                    return typeArgs;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

}

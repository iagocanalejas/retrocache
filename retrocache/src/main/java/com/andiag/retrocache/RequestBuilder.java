package com.andiag.retrocache;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import okhttp3.Request;
import retrofit2.Call;

/**
 * Created by IagoCanalejas on 02/02/2017.
 * Required because {@link retrofit2.RequestBuilder} is final and package local
 */
final class RequestBuilder {

    private static Object[] getCallArgs(Call call)
            throws NoSuchFieldException, IllegalAccessException {
        Field argsField = call.getClass().getDeclaredField("args");
        argsField.setAccessible(true);
        return (Object[]) argsField.get(call);
    }

    private static Object getRequestFactory(Call call)
            throws IllegalAccessException, NoSuchFieldException {
        Field serviceMethodField = call.getClass().getDeclaredField("serviceMethod");
        serviceMethodField.setAccessible(true);
        return serviceMethodField.get(call);
    }

    static Request build(Call call) {
        try {
            Object requestFactory = getRequestFactory(call);
            Method createMethod = requestFactory.getClass()
                    .getDeclaredMethod("toRequest", Object[].class);

            createMethod.setAccessible(true);

            return (Request) createMethod.invoke(requestFactory, new Object[]{getCallArgs(call)});
        } catch (Exception exc) {
            return null;
        }
    }

}

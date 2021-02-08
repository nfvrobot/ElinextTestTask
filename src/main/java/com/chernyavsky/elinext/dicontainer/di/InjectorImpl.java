package com.chernyavsky.elinext.dicontainer.di;

import com.chernyavsky.elinext.dicontainer.annotations.Inject;
import com.chernyavsky.elinext.dicontainer.expetions.BindingNotFoundException;
import com.chernyavsky.elinext.dicontainer.expetions.ConstructorNotFoundException;
import com.chernyavsky.elinext.dicontainer.expetions.TooManyConstructorsException;
import com.chernyavsky.elinext.dicontainer.interfaces.Injector;
import com.chernyavsky.elinext.dicontainer.interfaces.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InjectorImpl implements Injector {

    private Map<Class, Class> classMappings = new HashMap<>();
    private Map<Class, Object> singletonInstances = new HashMap<>();
    private Map<Class, Class> singletonMappings = new HashMap<>();

    @Override
    public <T> Provider<T> getProvider(Class<T> type) {
        if (classMappings.containsKey(type) || singletonMappings.containsKey(type)) {
            isConstructorExist(retrieveClassType(type).getConstructors());
            return () -> prepareInstance(type);
        } else if (!type.isInterface()) {
            throw new BindingNotFoundException();
        } else {
            return null;
        }
    }

    public <T> T prepareInstance(Class<T> requestedType) {
        return prepareInstance(requestedType, null);
    }


    private <T> T prepareInstance(Class<T> requestedType, Class<?> parent) throws
            ConstructorNotFoundException, TooManyConstructorsException, BindingNotFoundException {

        Class<T> type = retrieveClassType(requestedType);

        if (singletonInstances.containsKey(type)) {
            return (T) singletonInstances.get(type);
        }
        return createNewInstance(type);

    }

    private <T> Class<T> retrieveClassType(Class<T> requestedType) {
        if (classMappings.containsKey(requestedType)) {
            return classMappings.get(requestedType);
        } else if (singletonMappings.containsKey(requestedType)) {
            return singletonMappings.get(requestedType);
        }
        return requestedType;
    }


    private <T> T createNewInstance(Class<T> type) throws ConstructorNotFoundException, TooManyConstructorsException {
        final Constructor<T> constructor = findConstructor(type);
        final Parameter[] parameters = constructor.getParameters();

        final List<Object> arguments = prepareArguments(type, parameters);
        try {
            final T newInstance = constructor.newInstance(arguments.toArray());
            if (isSingleton(type)) {
                singletonInstances.put(type, newInstance);
            }
            return newInstance;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> List<Object> prepareArguments(Class<T> type, Parameter[] parameters) {
        return Arrays.stream(parameters)
                .map(param -> (Object) prepareInstance(param.getType(), type))
                .collect(Collectors.toList());
    }

    private <T> Constructor<T> findConstructor(Class<T> type) throws ConstructorNotFoundException {
        final Constructor<?>[] constructors = type.getConstructors();
        if (constructors.length > 1) {
            final List<Constructor<?>> constructorsWithInject = findConstructorsWithInjectAnnotation(constructors);
            if (constructorsWithInject.size() >= 2) {
                throw new ConstructorNotFoundException();
            }
            return (Constructor<T>) constructorsWithInject.get(0);
        } else {
            return (Constructor<T>) constructors[0];
        }
    }

    private List<Constructor<?>> findConstructorsWithInjectAnnotation(Constructor<?>[] constructors) {
        return Arrays
                .stream(constructors)
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
    }

    private void isConstructorExist(Constructor<?>[] constructors) {
        if ((constructors[0].getParameterCount() != 0) && !constructors[0].isAnnotationPresent(Inject.class)) {
            throw new ConstructorNotFoundException();
        }
    }

    private boolean isSingleton(Class type) {
        return singletonMappings.containsValue(type);
    }

    @Override
    public <T> void bind(Class<T> intf, Class<? extends T> impl) {
        classMappings.put(intf, impl);
    }

    @Override
    public <T> void bindSingleton(Class<T> intf, Class<? extends T> impl) {
        singletonMappings.put(intf, impl);
    }
}

/*    
 *     Copyright (c) 2015, NeumimTo https://github.com/NeumimTo
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *     
 */

package cz.neumimto.core.ioc;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Created by NeumimTo on 29.6.2015.
 */
@Singleton
public class IoC {

    private Map<Class<?>, Object> referenceMap = new HashMap<>();
    private Map<Object, Set<Method>> postProcess = new HashMap<>();
    private Map<Class<? extends Annotation>, AnnotationCallback> annotationCallbackMap = new HashMap();
    private static IoC ioc;

    public static class InjectContext {
        public final AnnotatedElement annotatedElement;
        public final Object instance;
        public final Class<?> clazz;

        private InjectContext(AnnotatedElement annotatedElement, Object instance, Class<?> clazz) {
            this.annotatedElement = annotatedElement;
            this.instance = instance;
            this.clazz = clazz;
        }
    }

    @FunctionalInterface
    public interface AnnotationCallback {

        void process(InjectContext injectContext);
    }

    protected IoC() {
        annotationCallbackMap.put(Inject.class, injectContext -> {
                Field f = (Field) injectContext.annotatedElement;
                f.setAccessible(true);
                Class fieldtype = f.getType();
                Object instance = build(fieldtype);
                try {
                    f.set(injectContext.instance, instance);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

        });
    }

    public static IoC get() {
        if (ioc == null) {
            ioc = new IoC();
        }
        return ioc;
    }

    public void registerAnnotationCallback(Class<? extends Annotation> annotation, AnnotationCallback callback) {
        annotationCallbackMap.put(annotation, callback);
    }

    public void registerDependency(Object object) {
        referenceMap.put(object.getClass(), object);
    }

    public <T> T build(Class<? extends T> cl) {
        if (referenceMap.containsKey(cl)) {
            return (T) referenceMap.get(cl);
        }
        T t = null;
        try {
            t = cl.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return get(cl, t);
    }

    public void registerInterfaceImplementation(Class cl, Object o) {
        referenceMap.put(cl, o);
    }

    public <T> T get(Class<? extends T> cl, T t) {
        if (referenceMap.containsKey(cl))
            return (T) referenceMap.get(cl);
        if (cl.isAnnotationPresent(Singleton.class)) {
            register(t);
        }
        injectFields(t, cl);
        findAnnotatedMethods(cl, t);
        return t;
    }

    private void findAnnotatedMethods(Class cl, Object o) {
        if (postProcess.containsKey(o))
            return;
        Set<Method> set = new HashSet<>();
        Class superClass = cl.getSuperclass();
        findAnnotatedMethods(cl, o, set);
        postProcess.put(o, set);
        if (superClass != Object.class) {
            findAnnotatedMethods(cl, o);
        }
    }

    private void findAnnotatedMethods(Class cl, Object o, Set<Method> set) {
        for (Method method : cl.getMethods()) {
            if (method.isAnnotationPresent(PostProcess.class)) {
                set.add(method);
            }
        }
    }

    private void injectFields(Object o, Class cl) {
        for (Field f : cl.getDeclaredFields()) {
            Annotation[] annotations = f.getAnnotations();
            for (Annotation annotation : annotations) {
                Class<? extends Annotation> aClass = annotation.annotationType();
                annotationCallbackMap.computeIfPresent(aClass, (aClass1, callback) -> {
                    callback.process(new InjectContext(f,o,cl));
                    return callback;
                });
            }
        }
        Class superClass = cl.getSuperclass();
        if (superClass != Object.class) {
            injectFields(o, superClass);
        }
    }


    public void register(Object o) {
        if (!referenceMap.containsKey(o.getClass()))
            referenceMap.put(o.getClass(), o);
    }

    public void postProcess() {
        long i = postProcess.values().stream().filter(m -> m.size() >= 1).count();
        for (Map.Entry<Object, Set<Method>> entry : entriesSortedByValues(postProcess)) {
            Set<Method> set = entry.getValue();
            for (Method m : set) {
                m.setAccessible(true);
                try {
                    m.invoke(entry.getKey());

                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();

                }
            }
        }
        postProcess.clear();
    }

    static SortedSet<Map.Entry<Object, Set<Method>>> entriesSortedByValues(Map<Object, Set<Method>> map) {
        SortedSet<Map.Entry<Object, Set<Method>>> sortedEntries = new TreeSet<Map.Entry<Object, Set<Method>>>(
                (o1, o2) -> {
                    int res = 0;
                    int first = 0;
                    int second = 0;
                    for (Method method : o1.getValue()) {
                        first = method.getAnnotation(PostProcess.class).priority();
                        break;
                    }
                    for (Method method : o2.getValue()) {
                        second = method.getAnnotation(PostProcess.class).priority();
                        break;
                    }
                    res = first - second;
                    return res != 0 ? res : 1;
                });
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
}

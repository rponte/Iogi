package br.com.caelum.iogi.reflection;

import br.com.caelum.iogi.Instantiator;
import br.com.caelum.iogi.parameters.Parameters;
import net.vidageek.mirror.dsl.Matcher;
import net.vidageek.mirror.dsl.Mirror;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NewObject {
    public static NewObject nullNewObject() {
        return new NewObject(null, null, null, null) {
            @Override
            public Object value() {
                return null;
            }

            @Override
            public Object withPropertiesSet() {
                return null;
            }
        };
    }

    private Instantiator<?> propertiesInstantiator;
    private ClassConstructor constructorUsed;
    private Parameters parameters;
    private Object object;

    public NewObject(Instantiator<?> propertiesInstantiator, ClassConstructor constructorUsed, Parameters parameters, Object newObjectValue) {
        this.propertiesInstantiator = propertiesInstantiator;
        this.constructorUsed = constructorUsed;
        this.parameters = parameters;
        this.object = newObjectValue;
    }

    public Object value() {
        return object;
    }

    public Object withPropertiesSet() {
        populateProperties(remainingParameters());
        return value();
    }

    private Parameters remainingParameters() {
        return parameters.notUsedBy(constructorUsed);
    }

    private void populateProperties(final Parameters parametersForProperties) {
		for (final Setter setter : Setter.settersIn(object)) {
            setProperty(setter, parametersForProperties);
        }
	}

    private void setProperty(Setter setter, Parameters remainingParameters) {
        if (remainingParameters.hasRelatedTo(setter.asTarget())) {
            final Object propertyValue = propertiesInstantiator.instantiate(setter.asTarget(), remainingParameters);
            setter.set(propertyValue);
        }
    }

    private static class Setter {
        private static final Matcher<Method> SETTERS = new Matcher<Method>() {
            public boolean accepts(final Method method) {
                return method.getName().startsWith("set");
            }
        };

        private static Collection<Setter> settersIn(final Object object) {
            List<Method> setterMethods = new Mirror().on(object.getClass()).reflectAll().methodsMatching(SETTERS);

            final ArrayList<Setter> setters = new ArrayList<Setter>();
            for (final Method setterMethod: setterMethods) {
                setters.add(new Setter(setterMethod, object));
            }

            return Collections.unmodifiableList(setters);
        }
        
        private final Method setter;

        private final Object object;

        public Setter(final Method setter, final Object object) {
			this.setter = setter;
			this.object = object;
		}

		public void set(final Object argument) {
			new Mirror().on(object).invoke().method(setter).withArgs(argument);
		}

        private Target<Object> asTarget() {
            return new Target<Object>(type(), propertyName());
        }

        private String propertyName() {
            final String capitalizedPropertyName = setter.getName().substring(3);
            final String propertyName = capitalizedPropertyName.substring(0, 1).toLowerCase() + capitalizedPropertyName.substring(1);
            return propertyName;
        }

        private Type type() {
            return setter.getGenericParameterTypes()[0];
        }

        @Override
        public String toString() {
            return "Setter(" + propertyName() +")";
        }
    }
}

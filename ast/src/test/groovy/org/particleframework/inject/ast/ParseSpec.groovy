package org.particleframework.inject.ast

import org.particleframework.context.DefaultComponentDefinition
import org.particleframework.context.DefaultComponentDefinitionClass
import org.particleframework.inject.ComponentDefinition
import spock.lang.Specification

/**
 * Created by graemerocher on 11/05/2017.
 */
class ParseSpec extends Specification {

    void "test parse simple singleton"() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        gcl.parseClass('''
@javax.inject.Singleton
class FooService {}
''')

        then:
        gcl.loadedClasses.size() == 3

        when:
        def cls = gcl.loadClass('FooServiceComponentDefinitionClass')

        then:
        DefaultComponentDefinitionClass.isAssignableFrom(cls)
        cls.genericSuperclass.typeName == 'org.particleframework.context.DefaultComponentDefinitionClass<FooService>'

        when:
        DefaultComponentDefinitionClass cdefc = cls.newInstance()
        ComponentDefinition cdef = cdefc.load()

        then:
        cdef.constructor.arguments.size() == 0

    }

    void "test inheritance with differing packages"() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        gcl.parseClass('''

package foo
@javax.inject.Singleton
class FooService {
    @javax.inject.Inject
    @groovy.transform.PackageScope
    void injectPackageScopeMethod() {}
}

''')

        gcl.parseClass('''

package bar
@javax.inject.Singleton
class BarService extends foo.FooService {

}
''')

        then:
        gcl.loadedClasses.size() == 6


    }

    void "test parse with abstract inheritance"() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        gcl.parseClass('''
import javax.inject.*

    @Singleton
    class A {

    }

    abstract class AbstractB {
        // inject via field
        @Inject protected A a
        private A another
        // inject via method
        @Inject void setAnother(A a) {
            this.another = a
        }

        A getA() {
            return a
        }

        A getAnother() {
            return another
        }
    }

    @Singleton
    class B extends AbstractB {

    }
''')

        then:
        gcl.loadedClasses.size() == 7


    }

    void "test parse singleton with array injection"() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        gcl.parseClass('''
@javax.inject.Singleton
class FooService {}

class BarService {
    @javax.inject.Inject
    void setFooServices(FooService[] fooServices) {
    
    }
}
''')

        then:
        gcl.loadedClasses.size() == 6

        when:
        def cls = gcl.loadClass('FooServiceComponentDefinitionClass')

        then:
        DefaultComponentDefinitionClass.isAssignableFrom(cls)
        cls.genericSuperclass.typeName == 'org.particleframework.context.DefaultComponentDefinitionClass<FooService>'

        when:
        DefaultComponentDefinitionClass cdefc = cls.newInstance()
        ComponentDefinition cdef = cdefc.load()

        then:
        cdef.constructor.arguments.size() == 0

    }


    void "test parse singleton with array constructor injection"() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        gcl.parseClass('''
@javax.inject.Singleton
class FooService {}

    class BarService {
        @javax.inject.Inject
        BarService(FooService[] fooServices) {

        }
    }
    ''')

        then:
        gcl.loadedClasses.size() == 6

        when:
        def cls = gcl.loadClass('FooServiceComponentDefinitionClass')

        then:
        DefaultComponentDefinitionClass.isAssignableFrom(cls)
        cls.genericSuperclass.typeName == 'org.particleframework.context.DefaultComponentDefinitionClass<FooService>'

        when:
        DefaultComponentDefinitionClass cdefc = cls.newInstance()
        ComponentDefinition cdef = cdefc.load()

        then:
        cdef.constructor.arguments.size() == 0

    }


    void "test parse singleton with collection constructor injection"() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        gcl.parseClass('''
@javax.inject.Singleton
class FooService {}

class BarService {
    @javax.inject.Inject
    BarService(Collection<FooService> fooServices) {
    
    }
}
''')

        then:
        gcl.loadedClasses.size() == 6

        when:
        def cls = gcl.loadClass('FooServiceComponentDefinitionClass')

        then:
        DefaultComponentDefinitionClass.isAssignableFrom(cls)
        cls.genericSuperclass.typeName == 'org.particleframework.context.DefaultComponentDefinitionClass<FooService>'

        when:
        DefaultComponentDefinitionClass cdefc = cls.newInstance()
        ComponentDefinition cdef = cdefc.load()

        then:
        cdef.constructor.arguments.size() == 0

    }

    void "test parse simple singleton with constructor argument"() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        gcl.parseClass('''
@javax.inject.Singleton
class FooService {
    FooService(URL url){}
}
''')

        then:
        gcl.loadedClasses.size() == 3

        when:
        def cls = gcl.loadClass('FooServiceComponentDefinition')

        then:
        DefaultComponentDefinition.isAssignableFrom(cls)
        cls.genericSuperclass.typeName == 'org.particleframework.context.DefaultComponentDefinition<FooService>'

        when:
        ComponentDefinition cdef = cls.newInstance()

        then:
        cdef.constructor.arguments.size() == 1

    }


    void "test parse singleton and property injected bean"() {
        given:
        def gcl = new GroovyClassLoader()

        when:
        gcl.parseClass('''
@javax.inject.Singleton
class FooService {}

class BarService {
    @javax.inject.Inject FooService fooService
    
    @javax.inject.Inject private FooService anotherFooService
}
''')

        then:
        gcl.loadedClasses.size() == 6

        when:
        def cls = gcl.loadClass('BarServiceComponentDefinition')

        then:
        DefaultComponentDefinition.isAssignableFrom(cls)
        cls.genericSuperclass.typeName == 'org.particleframework.context.DefaultComponentDefinition<BarService>'

        when:
        ComponentDefinition cdef = cls.newInstance()

        then:
        cdef.constructor.arguments.size() == 0
        cdef.injectedFields.size() == 1
        cdef.injectedFields.first().name == 'anotherFooService'
        cdef.injectedMethods.size() == 1
        cdef.injectedMethods.first().method.name == "setFooService"

    }
}

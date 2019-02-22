package io.micronaut.inject.visitor.beans

import io.micronaut.annotation.processing.TypeElementVisitorProcessor
import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.annotation.processing.test.JavaParser
import io.micronaut.context.ApplicationContext
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.beans.BeanIntrospection
import io.micronaut.core.beans.BeanIntrospectionReference
import io.micronaut.core.beans.BeanProperty
import io.micronaut.core.reflect.exception.InstantiationException
import io.micronaut.inject.beans.visitor.IntrospectedTypeElementVisitor
import io.micronaut.inject.visitor.TypeElementVisitor

import javax.annotation.processing.SupportedAnnotationTypes
import javax.validation.constraints.Size

class BeanIntrospectionSpec extends AbstractTypeElementSpec {

    void "test write bean introspection data for classes"() {
        given:
        ApplicationContext context = buildContext('test.Test', '''
package test;

import io.micronaut.core.annotation.*;
import javax.validation.constraints.*;
import java.util.*;
import io.micronaut.inject.visitor.beans.*;

@Introspected(classes=TestBean.class)
class Test {}
''')

        when:"the reference is loaded"
        def clazz = context.classLoader.loadClass('test.$Test$IntrospectionRef0')
        BeanIntrospectionReference reference = clazz.newInstance()

        then:"The reference is valid"
        reference != null
        reference.getBeanType() == TestBean


        cleanup:
        context?.close()
    }

    void "test write bean introspection data for package with sources"() {
        given:
        ApplicationContext context = buildContext('test.Test', '''
package test;

import io.micronaut.core.annotation.*;
import javax.validation.constraints.*;
import java.util.*;

@Introspected(packages="io.micronaut.inject.visitor.beans", includedAnnotations=Introspected.class)
class Test {}
''')

        when:"the reference is loaded"
        def clazz = context.classLoader.loadClass('test.$Test$IntrospectionRef0')
        BeanIntrospectionReference reference = clazz.newInstance()

        then:"The reference is valid"
        reference != null
        reference.getBeanType() == TestBean


        cleanup:
        context?.close()
    }

    void "test write bean introspection data for package with compiled classes"() {
        given:
        ApplicationContext context = buildContext('test.Test', '''
package test;

import io.micronaut.core.annotation.*;
import javax.validation.constraints.*;
import java.util.*;

@Introspected(packages="io.micronaut.inject.beans.visitor", includedAnnotations=Internal.class)
class Test {}
''')

        when:"the reference is loaded"
        def clazz = context.classLoader.loadClass('test.$Test$IntrospectionRef0')
        BeanIntrospectionReference reference = clazz.newInstance()

        then:"The reference is valid"
        reference != null
        reference.getBeanType() == IntrospectedTypeElementVisitor


        cleanup:
        context?.close()
    }

    void "test write bean introspection data"() {
        given:
        ApplicationContext context = buildContext('test.Test', '''
package test;

import io.micronaut.core.annotation.*;
import javax.validation.constraints.*;
import java.util.*;

@Introspected
class Test extends ParentBean {
    private String name;
    @Size(max=100)
    private int age;
    
    private List<Number> list;
    private String[] stringArray;
    private int[] primitiveArray;
    private boolean flag;
    
    public boolean isFlag() {
        return flag;
    }
    
    public void setFlag(boolean flag) {
        this.flag = flag;
    }
    
    public String getName() {
        return this.name;
    }
    public void setName(String n) {
        this.name = n;
    }
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
    
    public List<Number> getList() {
        return this.list;
    }
    
    public void setList(List<Number> l) {
        this.list = l;
    }

    public int[] getPrimitiveArray() {
        return this.primitiveArray;
    }

    public void setPrimitiveArray(int[] a) {
        this.primitiveArray = a;
    }

    public String[] getStringArray() {
        return this.stringArray;
    }

    public void setStringArray(String[] s) {
        this.stringArray = s;
    }
}

class ParentBean {
    private List<byte[]> listOfBytes;
    
    public List<byte[]> getListOfBytes() {
        return this.listOfBytes;
    }
    
    public void setListOfBytes(List<byte[]> list) {
        this.listOfBytes = list;
    }
}
''')

        when:"the reference is loaded"
        def clazz = context.classLoader.loadClass('test.$Test$IntrospectionRef')
        BeanIntrospectionReference reference = clazz.newInstance()

        then:"The reference is valid"
        reference != null
        reference.getAnnotationMetadata().hasAnnotation(Introspected)
        reference.isPresent()
        reference.beanType.name == 'test.Test'

        when:"the introspection is loaded"
        BeanIntrospection introspection = reference.load()

        then:"The introspection is valid"
        introspection != null
        introspection.hasAnnotation(Introspected)
        introspection.instantiate().getClass().name == 'test.Test'
        introspection.getBeanProperties().size() == 7
        introspection.getProperty("name").isPresent()
        introspection.getProperty("name", String).isPresent()
        !introspection.getProperty("name", Integer).isPresent()

        when:
        BeanProperty nameProp = introspection.getProperty("name", String).get()
        BeanProperty boolProp = introspection.getProperty("flag", boolean.class).get()
        BeanProperty ageProp = introspection.getProperty("age", int.class).get()
        BeanProperty listProp = introspection.getProperty("list").get()
        BeanProperty primitiveArrayProp = introspection.getProperty("primitiveArray").get()
        BeanProperty stringArrayProp = introspection.getProperty("stringArray").get()
        BeanProperty listOfBytes = introspection.getProperty("listOfBytes").get()
        def instance = introspection.instantiate()

        then:
        nameProp != null
        !nameProp.isReadOnly()
        !nameProp.isWriteOnly()
        nameProp.isReadWrite()
        boolProp.read(instance) == false
        nameProp.read(instance) == null
        ageProp.read(instance) == 0
        stringArrayProp.read(instance) == null
        primitiveArrayProp.read(instance) == null
        ageProp.hasAnnotation(Size)
        listOfBytes.asArgument().getFirstTypeVariable().get().type == byte[].class
        listProp.asArgument().getFirstTypeVariable().isPresent()
        listProp.asArgument().getFirstTypeVariable().get().type == Number

        when:
        boolProp.write(instance, true)
        nameProp.write(instance, "foo")
        ageProp.write(instance, 10)
        primitiveArrayProp.write(instance, [10] as int[])
        stringArrayProp.write(instance, ['foo'] as String[])


        then:
        boolProp.read(instance) == true
        nameProp.read(instance) == 'foo'
        ageProp.read(instance) == 10
        stringArrayProp.read(instance) == ['foo'] as String[]
        primitiveArrayProp.read(instance) == [10] as int[]

        when:
        introspection.instantiate("blah") // illegal argument

        then:
        def e = thrown(InstantiationException)
        e.message == 'Argument count [1] doesn\'t match required argument count: 0'

        cleanup:
        context?.close()
    }

    @Override
    protected JavaParser newJavaParser() {
        return new JavaParser() {
            @Override
            protected TypeElementVisitorProcessor getTypeElementVisitorProcessor() {
                return new MyTypeElementVisitorProcessor()
            }
        }
    }

    @SupportedAnnotationTypes("*")
    static class MyTypeElementVisitorProcessor extends TypeElementVisitorProcessor {
        @Override
        protected Collection<TypeElementVisitor> findTypeElementVisitors() {
            return [new IntrospectedTypeElementVisitor()]
        }
    }
}
package com.kaedea.kotlin

import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Kotlin reflect tweak with:
 * - [kotlin.reflect.KAnnotatedElement]
 * - [kotlin.reflect.KClass]
 * - [kotlin.reflect.KParameter]
 * - [kotlin.reflect.KCallable]
 * - [kotlin.reflect.KFunction], [Getter][kotlin.reflect.KProperty.Getter], [Setter][kotlin.reflect.KMutableProperty.Setter]
 * - [kotlin.reflect.KProperty]
 *
 * - [kotlin.reflect.KClassifier]
 * - [kotlin.reflect.KType]
 * - [kotlin.reflect.KVisibility]
 * - [kotlin.reflect.KVariance]
 * - [kotlin.reflect.KTypeParameter]
 *
 * Extension utils:
 * - [kotlin.reflect.full.KClasses.kt][createInstance]
 * - [kotlin.reflect.full.KAnnotatedElements.kt][findAnnotation]
 * - [kotlin.reflect.full.KCallables.kt][findParameterByName]
 * - [kotlin.reflect.full.KProperties.kt][kotlin.reflect.full.getExtensionDelegate]
 * - [kotlin.reflect.full.KClassifier.kt][createType]
 * - [kotlin.reflect.full.KTypes.kt][kotlin.reflect.full.isSubtypeOf]
 *
 * @author Kaede
 * @since  2018/8/13
 */

@RunWith(JUnit4::class)
class KtReflectTest {

    /**
     * @see [kotlin.reflect.KAnnotatedElement]
     * @see [kotlin.reflect.full.KAnnotatedElements.kt]
     */
    @Test
    fun ktAnnotationElement() {
        val annotations = KtReflectTest::class.annotations
        assertNotNull(annotations)

        val runWith = annotations.find { it is RunWith } as RunWith
        assertEquals(JUnit4::class, runWith.value)

        // ext utils
        // kotlin.reflect.full.KClasses.kt
        val findAnnotation = KtReflectTest::class.findAnnotation<RunWith>()!!
        assertEquals(JUnit4::class, findAnnotation.value)
    }

    open class User {
        val name
            get() = _name ?: "n/a"
        var age
            get() = _age ?: 0
            set(value) {
                _age = value
            }
        public var sex: String by Delegates.observable("male", { pro, old, new ->
            if (new != "male" && new != "female") throw IllegalArgumentException("Unknown sex")
        })

        private var _name: String? = null
        private var _age: Int? = null

        constructor()
        constructor(name: String, age: Int) {
            _name = name
            _age = age
        }

        fun changeName(name: String) {
            _name = name
        }

        override fun toString() = "$name($age)"

        val User.email get() = "$name@gmail.com"
        val User.id by Delegates.notNull<Int>()
    }

    object Guest : User("guest", 0)

    fun info(user: User) = user.toString()

    /**
     * @see [kotlin.reflect.KClass]
     * @see [kotlin.reflect.KClassifier]
     * @see [kotlin.reflect.full.KClasses.kt]
     */
    @Test
    fun ktClassOrObject() {
        // KClass of class
        val userClass = User::class
        val user1 = userClass.createInstance()
        val user2 = userClass.objectInstance

        assertTrue(userClass is KClass<User>)
        assertTrue(userClass is KClassifier)
        assertNotNull(user1)
        assertNull(user2)

        // KClass of object
        val guestClass = Guest::class // object is regarded as KClass
        try {
            val guest1 = guestClass.createInstance()
            fail()
        } catch (ignored: Exception) {
        }
        val guest2 = guestClass.objectInstance
        assertTrue(userClass is KClass<*>) // info of KClass<Guest> is erased
        assertTrue(userClass is KClassifier)
        assertNotNull(guest2)

        // KClass
        assertEquals("User", userClass.simpleName)
        assertEquals("com.kaedea.kotlin.KtReflectTest.User", userClass.qualifiedName)
        assertEquals(KVisibility.PUBLIC, userClass.visibility)
        assertTrue(!userClass.isFinal)
        assertTrue(userClass.isOpen)
        assertTrue(!userClass.isData)
        assertTrue(!userClass.isSealed)
        assertTrue(!userClass.isAbstract)
        assertTrue(!userClass.isInner)
        assertNull(userClass.objectInstance)
        assertNotNull(guestClass.objectInstance)
        assertTrue(!userClass.isCompanion)
        assertTrue(!guestClass.isCompanion)

        assertEquals(2, userClass.constructors.size)
        arrayOf("_name", "_age", "name", "age", "changeName", "toString").asSequence().forEach { name ->
            userClass.members.find { it.name == name }!!
        }
        assertEquals(emptyList(), userClass.nestedClasses)
        assertEquals(emptyList(), userClass.typeParameters)
        assertEquals(Any::class.createType(), userClass.supertypes.first())

        // ext utils
        // kotlin.reflect.full.KClasses.kt
    }

    /**
     * @see [kotlin.reflect.KParameter]
     */
    @Test
    fun ktParam() {
        val func = ::info

        val param = func.parameters.find {
            it.index == 0
        }!!

        assertEquals("user", param.name)
        assertTrue(param.type == User::class.createType())
        assertTrue(param.kind == KParameter.Kind.VALUE)
        assertTrue(!param.isOptional)
        assertTrue(!param.isVararg)

        val type = func.returnType
        assertEquals(String::class, type.classifier)
    }

    /**
     * @see [kotlin.reflect.KCallable]
     * @see [kotlin.reflect.KFunction]
     * @see [kotlin.reflect.full.KCallables.kt]
     */
    @Test
    fun ktFunc() {
        val func = ::info

        assertTrue(func is KCallable<*>)
        assertTrue {
            !func.isOpen &&
                    !func.isAbstract &&
                    func.isFinal

        }
        assertEquals("info", func.name)
        assertEquals(KVisibility.PUBLIC, func.visibility)
        assertEquals("kaede(17)", func.call(User("kaede", 17)))
        assertEquals("kaede(17)", func.callBy(mapOf(
                func.parameters[0] to User("kaede", 17)
        )))
        func.returnType

        assertTrue(func is KFunction<*>)
        assertTrue {
            !func.isInline &&
                    !func.isExternal &&
                    !func.isOperator &&
                    !func.isInfix &&
                    !func.isSuspend
        }

        // ext utils
        // kotlin.reflect.full.KCallables.kt
        val param = func.findParameterByName("user")!!
        assertTrue(param.type == User::class.createType())
        assertTrue(param.kind == KParameter.Kind.VALUE)

        assertNull(func.instanceParameter)
        assertNull(func.extensionReceiverParameter)
        assertEquals(listOf(param), func.valueParameters)
        assertTrue {
            func.valueParameters.all { it.kind == KParameter.Kind.VALUE}
        }

        val extFunc = User::copy
        assertEquals(emptyList(), extFunc.valueParameters)
        assertNull(extFunc.instanceParameter)
        assertTrue(extFunc.extensionReceiverParameter!!.type == User::class.createType())
        assertTrue(extFunc.extensionReceiverParameter!!.kind == KParameter.Kind.EXTENSION_RECEIVER)

        val insFunc = User::changeName
        assertTrue(insFunc.instanceParameter!!.type == User::class.createType())
        assertTrue(insFunc.instanceParameter!!.kind == KParameter.Kind.INSTANCE)
        assertNull(insFunc.extensionReceiverParameter)
    }

    /**
     * @see [kotlin.reflect.KProperty]
     * @see [kotlin.reflect.full.KProperties.kt]
     */
    @Test
    fun ktProp() {
        // KProperty
        val prop = User::name
        assertEquals(Guest.name, prop.get(Guest))
        assertEquals(prop.call(Guest), prop.get(Guest))
        assertTrue(!prop.isLateinit)
        assertTrue(!prop.isConst)

        val getter = prop.getter
        assertTrue(getter is KFunction<*>)
        assertEquals(Guest.name, getter.invoke(Guest))
        assertEquals(prop, getter.property)

        // KMutableProperty
        val mutProp = User::age
        val user = User()
        mutProp.set(user, 17)
        assertEquals(17, user.age)
        val setter = mutProp.setter
        assertTrue(setter is KFunction<*>)
        setter.call(user, 20)
        assertEquals(20, user.age)

        // Ext Property
        val extProperty = User::class.memberExtensionProperties.find {
            it.name == "email"
        } as KProperty2<User, User, String>
        assertEquals("guest@gmail.com", extProperty.get(Guest, Guest))

        // Delegated Property
        assertNull(prop.getDelegate(user))
        val propProxy = User::sex
        propProxy.isAccessible = true
        val propDelegate = propProxy.getDelegate(Guest) as ObservableProperty<String>
        assertNotNull(propDelegate)
        assertEquals("male", propDelegate.getValue(user, propProxy))
        propDelegate.setValue(user, propProxy, "female")
        assertEquals("female", propDelegate.getValue(user, propProxy))
        try {
            propDelegate.setValue(user, propProxy, "otoko no ko")
            fail()
        } catch (e: Exception) {
        }

        // ext utils
        // kotlin.reflect.full.KProperties.kt
        val extPropProxy = User::class.memberExtensionProperties.find {
            it.name == "id"
        }!!
        extPropProxy.isAccessible = true
        val extPropDelegate = extPropProxy.getExtensionDelegate(user) as ReadWriteProperty<User, Int>
        try {
            extPropDelegate.getValue(user, extPropProxy)
            fail("NotNullVar delegated")
        } catch (e: Exception) {
        }
        extPropDelegate.setValue(user, extPropProxy, 2233)
        assertEquals(2233, extPropDelegate.getValue(user, extPropProxy))
    }

    /**
     * @see [kotlin.reflect.KClassifier]
     * @see [kotlin.reflect.KType]
     * @see [kotlin.reflect.full.KClassifier.kt]
     * @see [kotlin.reflect.full.KTypes.kt]
     */
    @Test
    fun ktType() {
        // KClass, KClassifier & KType
        val userClass = User::class
        assertTrue(userClass is KClassifier)

        val type = ::info.returnType
        assertTrue {
            !type.isMarkedNullable &&
                    type.arguments === emptyList<KTypeProjection>() &&
                    type.classifier == String::class
        }

        // ext utils
        // kotlin.reflect.full.KClassifier.kt
        val guestType = Guest::class.createType()
        assertEquals(Guest::class, guestType.classifier)

        // kotlin.reflect.full.KTypes.kt
        assertTrue(guestType.isSubtypeOf(User::class.createType()))
        assertTrue(!guestType.isSupertypeOf(User::class.createType()))
        assertTrue(!guestType.isMarkedNullable)
        val nullableType = guestType.withNullability(true)
        assertTrue(nullableType.isMarkedNullable)
    }
}

fun KtReflectTest.User.copy() = KtReflectTest.User(name, age)
@file:JvmName("TestUtil")

package nu.fgv.register.server

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import org.assertj.core.api.Assertions.assertThat

fun <T : Any> equalsVerifier(clazz: KClass<T>) {
    val domainObject1 = clazz.createInstance()
    assertThat(domainObject1.toString()).isNotNull()
    assertThat(domainObject1).isEqualTo(domainObject1)
    assertThat(domainObject1.hashCode()).isEqualTo(domainObject1.hashCode())
    val testOtherObject = Any()
    assertThat(domainObject1).isNotEqualTo(testOtherObject)
    assertThat(domainObject1).isNotEqualTo(null)
    val domainObject2 = clazz.createInstance()
    assertThat(domainObject1).isNotEqualTo(domainObject2)
    assertThat(domainObject1.hashCode()).isEqualTo(domainObject2.hashCode())
}

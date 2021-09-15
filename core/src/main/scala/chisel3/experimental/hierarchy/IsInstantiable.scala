// SPDX-License-Identifier: Apache-2.0

package chisel3.experimental.hierarchy
import scala.reflect.runtime.universe.TypeTag

/** While this is public, it is not recommended for users to extend directly.
  * Instead, use the [[@instantiable]] annotation on your trait or class.
  *
  * This trait indicates whether a class can be returned from an Instance.
  * 
  */
trait IsInstantiable

object IsInstantiable {
  implicit class IsInstantiableExtensions[T <: IsInstantiable : TypeTag](i: T) {
    def toInstance: Instance[T] = new Instance(Left(i), implicitly[TypeTag[T]])
  }
}

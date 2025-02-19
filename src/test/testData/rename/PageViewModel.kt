package foo

interface SomeParentInterface {
    val someDefaultedProperty get() = 123
}

data class PageViewModel(
    val greeting: String,
    val name: String,
    val age: Int,
): SomeParentInterface {
    val someProperty: Boolean = true
}

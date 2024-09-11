## Json to Dart

**Generate Dart data class from json text.**

Create dart data class from json text, support nested data class structure creation, the same structure will generate the same data class.

Naming of the data class is based on the json key name and with the custom suffix, file name is lower case, class name is upper camel case.
field name is lower camel case.

Primitive types and array is optional nullable.

json type float and double will convert to dart type `num`.

All code will be formatted after generate.

*Support language: Dart Only*

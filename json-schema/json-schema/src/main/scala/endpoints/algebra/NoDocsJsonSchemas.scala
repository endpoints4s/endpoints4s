package endpoints.algebra

/**
  * Helper trait that can be mixed into [[JsonSchemas]] to implement (as no-ops)
  * the documentation related methods. This is useful for implementing any
  * non-documentation inteprereters.
  */
trait NoDocsJsonSchemas extends JsonSchemas {

  def namedRecord[A](record: Record[A], name: String): Record[A] = record

  def namedTagged[A](tagged: Tagged[A], name: String): Tagged[A] = tagged

  def namedEnum[A](enum: Enum[A], name: String): Enum[A] = enum

  def withExampleRecord[A](record: Record[A], example: A): Record[A] = record

  def withExampleTagged[A](tagged: Tagged[A], example: A): Tagged[A] = tagged

  def withExampleEnum[A](enum: Enum[A], example: A): Enum[A] = enum

  def withExampleJsonSchema[A](
      schema: JsonSchema[A],
      example: A
  ): JsonSchema[A] = schema

  def withDescriptionRecord[A](
      record: Record[A],
      description: String
  ): Record[A] = record

  def withDescriptionTagged[A](
      tagged: Tagged[A],
      description: String
  ): Tagged[A] = tagged

  def withDescriptionEnum[A](enum: Enum[A], description: String): Enum[A] = enum

  def withDescriptionJsonSchema[A](
      schema: JsonSchema[A],
      description: String
  ): JsonSchema[A] = schema

  def withTitleRecord[A](record: Record[A], title: String): Record[A] = record

  def withTitleTagged[A](tagged: Tagged[A], title: String): Tagged[A] = tagged

  def withTitleEnum[A](enum: Enum[A], title: String): Enum[A] = enum

  def withTitleJsonSchema[A](
      schema: JsonSchema[A],
      title: String
  ): JsonSchema[A] = schema
}

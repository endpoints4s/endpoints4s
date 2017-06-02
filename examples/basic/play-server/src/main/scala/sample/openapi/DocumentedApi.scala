package sample.openapi

import endpoints.openapi.{Info, OpenApi}

object DocumentedApi
  extends sample.algebra.DocumentedApi
    with endpoints.openapi.DocumentedEndpoints {

  /**
    * Produces the following API description:
    *
    * {{{
    *   {
    *     "openapi": "3.0.0",
    *     "info": {
    *       "title": "API to get information about users",
    *       "version": "1.0.0"
    *     },
    *     "paths": {
    *       "/users/{id}": {
    *         "get": {
    *           "parameters": [{
    *             "name": "id",
    *             "in": "path",
    *             "required": true
    *           }],
    *           "responses": {
    *             "200": {
    *               "description": "Details of the user"
    *             },
    *             "500": {
    *               "description": "Internal Server Error"
    *             }
    *           }
    *         }
    *       }
    *     }
    *   }
    * }}}
    */
  val documentation: OpenApi =
    openApi(
      Info("API to get information about users", "1.0.0")
    )(
      getUser
    )

}

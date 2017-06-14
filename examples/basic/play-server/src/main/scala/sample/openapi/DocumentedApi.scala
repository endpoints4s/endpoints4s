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
    *       "title": "API to get information about items",
    *       "version": "1.0.0"
    *     },
    *     "paths": {
    *       "/items/{category}": {
    *         "get": {
    *           "parameters": [{
    *             "name": "category",
    *             "in": "path",
    *             "required": true
    *           }, {
    *             "name": "page",
    *             "in": "query"
    *           }],
    *           "responses": {
    *             "200": {
    *               "description": "List all the items of the given category"
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
      Info("API to get information about items", "1.0.0")
    )(
      getUser
    )

}

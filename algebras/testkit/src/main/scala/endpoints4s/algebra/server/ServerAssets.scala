package endpoints4s.algebra.server

import endpoints4s.algebra.Assets

trait ServerAssets extends Assets {

  type AssetContent

  def notFoundAssetResponse: AssetResponse

  def foundAssetResponse(
      content: AssetContent,
      contentLength: Long,
      fileName: String,
      isGzipped: Boolean,
      isExpired: Boolean,
      lastModifiedSeconds: Long
  ): AssetResponse

  def noopAssetContent: AssetContent

}

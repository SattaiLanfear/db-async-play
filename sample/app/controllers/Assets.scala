package controllers

import controllers.Assets.Asset
import play.api.http.LazyHttpErrorHandler

/**
	* Asset handler for the application.
	*/
class ApplicationAssets extends AssetsBuilder(LazyHttpErrorHandler) {

	def public(path: String, file: Asset) = versioned(path, file)

	def lib(path: String, file: Asset) = versioned(path, file)

}

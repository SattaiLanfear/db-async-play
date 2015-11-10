import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.csrf.CSRFFilter
import play.filters.gzip.GzipFilter

/**
	* Configures the Play filters.
	*/
class Filters @Inject()(gzipFilter: GzipFilter, csrfFilter: CSRFFilter) extends HttpFilters {
	def filters = Seq(gzipFilter, csrfFilter)
}

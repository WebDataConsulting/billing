/**
 * EntityTagLib
 *
 * @author Khobab Chaudhary
 */
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserDTO

class EntityTagLib {

	/**
	 * Shows body only if logged in company is root
	 */
	def isRoot = { attrs, body ->
		def company = CompanyDTO.get(session['company_id'])
		def childEntities = CompanyDTO.findAllByParent(company)
		def root = CompanyDTO.get(session['company_id'])?.parent == null || (childEntities != null && childEntities.size() > 0)
		if (root) {
			out << body()
		}
	}
	
	/**
	 * Shows body only if logged in company is not root
	 */
	def isNotRoot = { attrs, body ->
		def company = CompanyDTO.get(session['company_id'])
		def childEntities = CompanyDTO.findAllByParent(company)
		def root = company?.parent == null || (childEntities != null && childEntities.size() > 0)
		if (!root) {
			out << body()
		}
	}

	/**
	 * Shows body only if logged in company is root and has at least one child entities
	 */
	def isGlobal = { attrs, body ->
	    def company = CompanyDTO.get(session['company_id'])
		def childEntities = CompanyDTO.findAllByParent(company)
		def isGlobal = (childEntities != null && childEntities.size() > 0)
		if (isGlobal) {
		   out << body()
		 }
	}
}

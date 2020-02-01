package nu.fgv.register.server.web.util

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import nu.fgv.register.server.ApplicationConstants
import org.springframework.http.HttpHeaders

class HeaderUtil {

    companion object {
        @JvmStatic
        fun createAlert(message: String?, param: String?): HttpHeaders? {
            val headers = HttpHeaders()
            headers.add(String.format("X-%s-alert", ApplicationConstants.APPLICATION_NAME), message)
            try {
                headers.add(String.format("X-%s-params", ApplicationConstants.APPLICATION_NAME), URLEncoder.encode(param, StandardCharsets.UTF_8.toString()))
            } catch (e: UnsupportedEncodingException) { //
            }
            return headers
        }

        @JvmStatic
        fun createEntityCreationAlert(entityName: String?, param: String?): HttpHeaders? {
            return createAlert(String.format("%s.%s.created", ApplicationConstants.APPLICATION_NAME, entityName), param)
        }

        @JvmStatic
        fun createEntityUpdateAlert(entityName: String?, param: String?): HttpHeaders? {
            return createAlert(String.format("%s.%s.updated", ApplicationConstants.APPLICATION_NAME, entityName), param)
        }

        @JvmStatic
        fun createEntityDeletionAlert(entityName: String?, param: String?): HttpHeaders? {
            return createAlert(String.format("%s.%s.deleted", ApplicationConstants.APPLICATION_NAME, entityName), param)
        }

        @JvmStatic
        fun createFailureAlert(entityName: String?, errorKey: String?): HttpHeaders? {
            val headers = HttpHeaders()
            headers.add(String.format("X-%s-error", ApplicationConstants.APPLICATION_NAME), String.format("error.%s", errorKey))
            headers.add(String.format("X-%s-params", ApplicationConstants.APPLICATION_NAME), entityName)
            return headers
        }
    }
}

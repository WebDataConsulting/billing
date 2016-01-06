package jbilling

import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator

class TemplateExistsTagLib {

    static namespace = "jB"

    def GrailsConventionGroovyPageLocator groovyPageLocator;

    /**
     * Check if a template exists the same way as if the
     * rendering mechanism would check if the template exists.
     *
     * @param var - a variable in which a result will be returned. True if the template exists, false otherwise.
     * @param template - a string variable with the template name to check if template exists.
     */
    def templateExists = { attrs, body ->
        def var = attrs.var
        if (!var) throw new IllegalArgumentException("[var] attribute must be specified to for <jB:templateExists>!")

        def template = attrs.template
        if (!template) throw new IllegalArgumentException("[template] attribute must be specified to for <jB:templateExists>!")

        def script = groovyPageLocator.findTemplate(template)
        this."pageScope"."$var" = script ? true : false
        null
    }
}

package com.cabolabs.security

import net.kaleidos.grails.plugin.security.stateless.annotation.SecuredStateless

class AuthInterceptor {

   int order = HIGHEST_PRECEDENCE + 100

   def authService

   public AuthInterceptor()
   {
      matchAll()
         .excludes(controller:'auth')
         .excludes(controller:'restAuth')

   }

   // FIXME: copied from stateless plugin, should refactor to a common src class with static method

   // Checks if an action was annotated with SecuredStateless
   private boolean isSecuredStateless(String controllerName, String actionName, grailsApplication)
   {
      // when accessing to root I don't get the name of the controller if an urlmapping is not defined!
      if (!controllerName) return false

      def controller = grailsApplication.controllerClasses.find{controllerName.toLowerCase() == it.name.toLowerCase()} //WordUtils.uncapitalize(it.name)}
      if (controller) {
         def clazz = controller.clazz
         if (clazz.isAnnotationPresent(SecuredStateless)) {
            return true
         }
         if (!actionName) {
            actionName = controller.defaultAction
         }
         def method = clazz.methods.find{actionName == it.name}
         if (method) {
            return method.isAnnotationPresent(SecuredStateless)
         }
      }
      return false
   }

   boolean before() {

      //println "AUTH INTERCEPTOR"

      // if SecuredStateless, the session check should not apply
      if (isSecuredStateless(controllerName, actionName, grailsApplication))
      {
         // Make session available in the session if comes from REST
         // TODO: with this we can change all request.securityStatelessMap.extradata.org_uid for sesison.organization.uid

         // ISSUE: if the stateless interceptor is not executed before this one, the request.securityStatelessMap is not set
         session.organization = Organization.findByUid(request.securityStatelessMap.extradata.org_uid)

         return true
      }

      //println "AuthInterceptor: c: ${controllerName}, a: ${actionName}"

      // Not logged in? Go to the login page
      def sessman = SessionManager.instance
      if (!sessman.hasSession(session.id.toString()))
      {
         //println "redirects to auth"
         redirect controller: 'auth', action: 'login'
         return false
      }

      // Check access to current section by user role
      def path = request.requestURI

      // TODO: move to an on-memory singleton
      // TODO: check request method
      def rms = RequestMap.list() //findByUrl(path)
      def rm = rms.find {
         path.matches(it.url) // current path matches reges in RequestMap.url?
      }

      if (!rm)
      {
         log.info "${path} doesnt match any RequestMap URL"
         //println rms.url
         render view: "/noPermissions.gsp"
         return false // all URLs are closed by default!
      }

      log.info "${path} matches ${rm.url}"

      if (rm.configAttribute == 'OPEN_ACCESS')
      {
         log.info "open access to url "+ rm.url
         return true
      }

      // verify role
      if (!authService.loggedInUserHasAnyRole(rm.configAttribute))
      {
         render view: "/noPermissions.gsp"
         return false // all URLs are closed by default!
      }

      true
   }

   boolean after() { true }

   void afterView() {
     // no-op
   }
}

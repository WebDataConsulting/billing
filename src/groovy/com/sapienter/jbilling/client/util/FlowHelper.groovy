package com.sapienter.jbilling.client.util

/**
 * This can be used together with the FlowTagLib to create alternate flows (view/redirects/templates) through your controller.
 * The taglib will put additional hidden inputs into your html form based on property values passed to the page.
 * The FlowHelper must then be used in the controller to change for a change in flow.
 *
 * As an example /myAccount/editUser.gsp includes /user/_editForm.gsp and passes values for altFailureView and altRedirect
 * The form submits to UserController.save which will change it's normal flow with the help of the FlowHelper
 *
 * @author Gerhard
 * @since 03/04/13
 */
class FlowHelper {

    static boolean display(controller, boolean success,  map, Closure c=null) {
        def view
        if(success && ((view = property(controller, 'altSuccessView')) != null)) {
            controller.render view: view, model: map['model']
            return true
        } else if(success && ((view = property(controller, 'altSuccessTemplate')) != null)) {
            controller.render template: view, model: map['model']
            return true
        } else if(!success && ((view = property(controller, 'altFailureView')) != null)) {
            controller.render view: view, model: map['model']
            return true
        } else if(!success && ((view = property(controller, 'altFailureTemplate')) != null)) {
            controller.render template: view, model:  map['model']
            return true
        } else if((view = property(controller, 'altView')) != null) {
            controller.render view: view, model: map['model']
            return true
        } else if((view = property(controller, 'altTemplate')) != null) {
            controller.render template: view, model: map['model']
            return true
        } else if((view = property(controller, 'altChain')) != null) {
            controller.chain uri: view, model: map['model'], params: map['params']
            return true
        } else if((view = property(controller, 'altRedirect')) != null) {
            controller.redirect uri: view, model: map['model'], params: map['params']
            return true
        } else if((view = map['view']) != null) {
            controller.render view: view, model: map['model']
            return true
        } else if((view = map['template']) != null) {
            controller.render template: view, model: map['model']
            return true
        } else if (c != null) {
            c()
            return true
        }
        return false
    }

    private static String property(controller, name) {
        return controller.params[name] ?:
            controller.request.getAttribute(name) ?:
            controller.flash[name] ?:
            null
    }
}

/*
 * Licensed under the GPL License.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF
 * MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.googlecode.psiprobe.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

/**
 * Creates a list of all configured datasources for all web applications within
 * the container.
 *
 * @author Vlad Ilyushchenko
 */
public class ListAllJdbcResourcesController extends TomcatContainerController{

    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse httpServletResponse) throws Exception {
        request.setAttribute("global_resources", Boolean.valueOf(!getContainerWrapper().getResourceResolver().supportsPrivateResources()));
        return new ModelAndView(getViewName(), "resources", getContainerWrapper().getDataSources());
    }
}

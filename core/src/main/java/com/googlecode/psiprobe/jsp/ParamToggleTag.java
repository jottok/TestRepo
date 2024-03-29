/*
 * Licensed under the GPL License. You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE.
 */
package com.googlecode.psiprobe.jsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.ServletRequestUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * 
 * @author Vlad Ilyushchenko
 * @author Mark Lewis
 */
public class ParamToggleTag extends TagSupport {

  private Log logger = LogFactory.getLog(getClass());
  private String param = "size";

  public int doStartTag() throws JspException {
    boolean getSize =
        ServletRequestUtils.getBooleanParameter(pageContext.getRequest(), param, false);
    StringBuffer query = new StringBuffer();
    query.append(param).append("=").append(!getSize);
    String encoding = pageContext.getResponse().getCharacterEncoding();
    for (Enumeration en = pageContext.getRequest().getParameterNames(); en.hasMoreElements();) {
      String name = (String) en.nextElement();
      if (!param.equals(name)) {
        try {
          String value = ServletRequestUtils.getStringParameter(pageContext.getRequest(), name, "");
          String encodedValue = URLEncoder.encode(value, encoding);
          query.append("&").append(name).append("=").append(encodedValue);
        } catch (UnsupportedEncodingException e) {
          throw new JspException(e);
        }
      }
    }
    try {
      pageContext.getOut().print(query);
    } catch (IOException e) {
      logger.debug("Exception printing query string to JspWriter", e);
      throw new JspException(e);
    }
    return EVAL_BODY_INCLUDE;
  }

  public String getParam() {
    return param;
  }

  public void setParam(String param) {
    this.param = param;
  }

}

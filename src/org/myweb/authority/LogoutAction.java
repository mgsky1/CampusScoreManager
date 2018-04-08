package org.myweb.authority;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionSupport;

public class LogoutAction extends ActionSupport 
{
     public String execute()
     {
    	 HttpServletRequest req = ServletActionContext.getRequest();
    	 HttpSession session = req.getSession();
    	 session.removeAttribute("id");
    	 session.removeAttribute("loginName");
    	 return "SUCCESS";
     }
}

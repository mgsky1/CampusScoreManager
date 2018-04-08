package org.myweb.Interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

public class LoginInterceptor extends AbstractInterceptor
{

	@Override
	public String intercept(ActionInvocation arg0) throws Exception {
		// TODO Auto-generated method stub
		//从ServletActionContext获取HttpServletRequest对象，HttpServletRequest对象是ServletRequest对象的子类
		HttpServletRequest req= ServletActionContext.getRequest();
		HttpSession session = req.getSession();
		if(session.getAttribute("role") == null)
		{
			return "LOGIN";
		}
		
		String url = req.getRequestURI();
		if(!url.contains(session.getAttribute("role").toString()))
		{
			return "LOGIN";
		}
		
		return arg0.invoke();
	}
   
}

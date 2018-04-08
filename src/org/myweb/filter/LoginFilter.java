package org.myweb.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginFilter implements Filter {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest arg0, ServletResponse arg1,
			FilterChain arg2) throws IOException, ServletException {
		// TODO Auto-generated method stub
		// 向下转型，HttpServletRequest是ServletRequest的子类
		HttpServletRequest req = (HttpServletRequest) arg0;
		// 向下转型
		HttpServletResponse res = (HttpServletResponse) arg1;
		// 获取Session对象
		HttpSession session = req.getSession();
		// 获取当前页面完整URL
		String url = req.getRequestURI();
		System.out.println("请求的URL为：" + url);
		// 判断请求URL的后缀是否是jsp，若是，进一步处理，若不是，放行
		int t = url.lastIndexOf("jsp");
		if (t == url.length() - 3) {
			// 如果是，则进行session验证
			if (session.getAttribute("role") == null
					&& !url.equals(req.getContextPath() + "/Login.jsp")) {
				System.out.println("小远小远小远~~~");
				res.sendRedirect(req.getContextPath() + "/Login.jsp");
			}
			if (session.getAttribute("role") != null
					&& !url.equals(req.getContextPath() + "/Login.jsp")) {
				String role = session.getAttribute("role").toString();
				if (!url.contains(role)) {
					res.sendRedirect(req.getContextPath() + "/Login.jsp");
				}
			}
			// 重定向后放行，否则页面为空白
			arg2.doFilter(arg0, arg1);
		} else {
			arg2.doFilter(arg0, arg1);
		}
		return;
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}

}

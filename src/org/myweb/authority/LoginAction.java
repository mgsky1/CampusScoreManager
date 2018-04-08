package org.myweb.authority;
/*
 * 登陆Action
 * Written By 小远
 * 2016-12-07
 * 
 */

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;


import org.myweb.dao.Dao;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

//登陆Action
public class LoginAction extends ActionSupport{

	String userName = "";//用户名
	String password = "";//密码
	
/********************各变量的get set方法**************************************/
	
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	
	public String getPassword() {
		return password;
	}
	
	public String getUserName() {
		return userName;
	}
   /*******登陆Action********/
	public String login()
	{
		ActionContext actionContext = ActionContext.getContext();//Action上下文，用于获取和设置Session
		Dao dao = new Dao();
		try {
			//先根据用户名和密码查询
			PreparedStatement state = dao.getConn().prepareStatement("select * from user where loginName = ? and password = ?");
			state.setObject(1,userName);
			state.setObject(2, this.password);
			ResultSet rs = dao.executeQuery(state);
			Map session = actionContext.getSession();//通过继承来的ActionContext获取Session对象
			session.put("loginName", userName);
			if(rs.next())//如果有对应记录
			{
				session.put("user", rs.getObject(2));//设置真实姓名，用于JavaBean构造实体
				String identity = rs.getString(4);//获得用户身份
				//根据身份进行跳转
				if(identity.equals("1"))
				{
				   session.put("id", rs.getObject(1));
				   session.put("role", "Teacher");
				   return "TEACHER";
				}
				else if(identity.equals("0"))
				{
				  session.put("id", rs.getObject(1));
				  session.put("role", "Student");
				   return "STUDENT";
				}
			   return "OK";
			}
			addFieldError("login", "登陆失败");
			return "FAILD";
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "FAILD";
		}
	}
	
}

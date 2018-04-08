package org.myweb.account;

import java.sql.PreparedStatement;
import java.util.Map;

import org.myweb.dao.Dao;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

public class ModifyTeacherInfoAction extends ActionSupport
{
    private String loginName;//用户登陆名
    private String tel;//用户电话号码
    private Dao db;//数据库操作类
    ActionContext actionContext = ActionContext.getContext();//Action上下文，用于获取和设置Session
    
    public String getLoginName() {
		return loginName;
	}
    
    public String getTel() {
		return tel;
	}
    
    public void setLoginName(String loginName) {
		this.loginName = loginName;
	}
    
    public void setTel(String tel) {
		this.tel = tel;
	}
    
    //主要业务逻辑
    public String execute()
    {
    	//通过session获取用户id
    	Map session = actionContext.getSession();
    	String id = session.get("id").toString();
    	//准备工作，包括数据库初始化，表达式构造
        db = new Dao();
        try
        {
        	 PreparedStatement state0 = db.getConn().prepareStatement("update user set loginName = ? where id = ?");
        	 state0.setObject(1, this.loginName);
        	 state0.setObject(2, id);
        	 
        	 PreparedStatement state1 = db.getConn().prepareStatement("update teacher set tel = ? where id = ?");
        	 state1.setObject(1, this.tel);
        	 state1.setObject(2, id);
        	 
        	 //开始执行更新操作
        	 int isSuccess0 = db.executeUpdate(state0);
        	 int isSuccess1 = db.executeUpdate(state1);
        	 
        	 if(isSuccess0 == 0 && isSuccess1 == 0)
        	 {
        		 return "SUCCESS";
        	 }
        	 else
        	 {
        		 addActionError("修改失败");
        		 return "FAILED";
        	 }
        	
        }catch (Exception e)
        {
        	addActionError("内部服务器错误");
        	return "ERROR";
        }
       
    }
}

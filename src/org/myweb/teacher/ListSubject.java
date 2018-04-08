package org.myweb.teacher;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import org.myweb.dao.Dao;
import org.myweb.vo.Subject;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

public class ListSubject extends ActionSupport
{
	private Dao db = new Dao();
	private List<Subject> subject;
	ActionContext actionContext = ActionContext.getContext();//Action上下文，用于获取和设置Session
	
	public void setSubject(List<Subject> subject) {
		this.subject = subject;
	}
	
	public List<Subject> getSubject() {
		return subject;
	}
	
	public String execute() throws Exception
	{
		 Map session = actionContext.getSession();//通过继承来的ActionContext获取Session对象
		 String id = session.get("id").toString();
		 String sql = "select id , name , grade from subject where teacher = ? order by grade";
		 PreparedStatement state = db.getConn().prepareStatement(sql);
		 state.setObject(1, id);
		 ResultSet set = db.executeQuery(state);
		 subject = new ArrayList<Subject>();
		 while(set.next())
		 {
			 Subject t = new Subject();
			 t.setId(set.getInt(1));
			 t.setName(set.getString(2));
			 t.setTeacher(Integer.parseInt(id));
			 t.setGrade(set.getInt(3));
			 subject.add(t);
		 }
		 return "SUCCESS";
	}
}

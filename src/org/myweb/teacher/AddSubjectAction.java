package org.myweb.teacher;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import org.myweb.dao.Dao;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

public class AddSubjectAction extends ActionSupport
{
    private String subjectName;
    private int grade;
    ActionContext context = ActionContext.getContext();
    private Dao db = new Dao();
    
    public String getSubjectName() {
		return subjectName;
	}
    
    public int getGrade() {
		return grade;
	}
    
    public void setSubjectName(String subjectName) {
		this.subjectName = subjectName;
	}
    
    public void setGrade(int grade) {
		this.grade = grade;
	}
    
    public boolean isSubjectExist(String name) throws Exception
    {
    	Map session = context.getSession();
    	String id = session.get("id").toString();
    	String sql = "select * from subject where name = ? and teacher = ?";
    	PreparedStatement state = db.getConn().prepareStatement(sql);
    	state.setObject(1, name);
    	state.setObject(2, id);
    	ResultSet set = db.executeQuery(state);
        return set.next() ? true : false;
    }
    
    public String execute() throws Exception
    {
    	Map session = context.getSession();
    	String id = session.get("id").toString();
    	if(!isSubjectExist(this.subjectName))
    	{
    		String sql = "insert into subject (name,teacher,grade) values (?,?,?)";
    		PreparedStatement state = db.getConn().prepareStatement(sql);
    		state.setObject(1, this.subjectName);
    		state.setObject(2, id);
    		state.setObject(3, this.grade);
    		return db.executeUpdate(state) == 0 ? "SUCCESS" : "FAILED";
    	}
    	else
    	{
    		addActionError("同一门课一位老师只能添加一次！");
    		return "EXIST";
    	}
    }
}

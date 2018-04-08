package org.myweb.teacher;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import org.myweb.dao.Dao;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

public class ModifySubjectAction extends ActionSupport
{
    private String subjectName;
    private Dao db = new Dao();
    private int id;
    private int grade;
    
    public void setGrade(int grade) {
		this.grade = grade;
	}
    
    public int getGrade() {
		return grade;
	}
    
    public int getId() {
		return id;
	}
    
    public void setId(int id) {
		this.id = id;
	}
    
    public String getSubjectName() {
		return subjectName;
	}
    
    public void setSubjectName(String subjectName) {
		this.subjectName = subjectName;
	}
    
    
    public boolean isSubjectExist(String name) throws Exception
    {
    	String sql = "select * from subject where name = ?";
    	PreparedStatement state = db.getConn().prepareStatement(sql);
    	state.setObject(1, name);
    	ResultSet set = db.executeQuery(state);
        return set.next() ? true : false;
    }
    
    public String execute() throws Exception
    {
    	ActionContext context = ActionContext.getContext();
    	Map session = context.getSession();
    	if(!this.subjectName.equals(session.get("subjectNow").toString()) && isSubjectExist(session.get("subjectNow").toString()))
    	{
    		addActionError("¿Î³ÌÒÑ´æÔÚ");
    		return "EXIST";
    	}
    	String sql = "update subject set name = ?, grade = ? where id = ?";
    	PreparedStatement state = db.getConn().prepareStatement(sql);
    	state.setObject(1, this.subjectName);
    	state.setObject(2, this.grade);
    	state.setObject(3, this.id);
    	return db.executeUpdate(state) == 0 ? "SUCCESS" : "FAILED";
    }
}

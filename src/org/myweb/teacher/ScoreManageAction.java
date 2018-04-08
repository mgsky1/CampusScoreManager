package org.myweb.teacher;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.myweb.dao.Dao;
import org.myweb.vo.Score;

import com.opensymphony.xwork2.ActionContext;


public class ScoreManageAction 
{
	
	private String stuId;
	private Dao db = new Dao();
	private List<Score> stuScore;
	
	public List<Score> getStuScore() {
		return stuScore;
	}
	
	public void setStuScore(List<Score> stuScore) {
		this.stuScore = stuScore;
	}
	
	public String getStuId() {
		return stuId;
	}
	
	public void setStuId(String stuId) {
		this.stuId = stuId;
	}
	
	
	public String execute()
	{
		
		ActionContext actionContext = ActionContext.getContext();//Action上下文，用于获取和设置Session
    	Map session = actionContext.getSession();
    	//this.stuId = session.get("stuId").toString();
    	String tid = session.get("id").toString();
    	String sql = "SELECT subjectAll.`name` , score.score , score.grade , score.id ,score.stu FROM score , subjectAll WHERE score.stu = ? and score.teacher = ? and score.`subject` in (subjectAll.id) GROUP BY subjectAll.name";
    	try
    	{
    		PreparedStatement state0 = db.getConn().prepareStatement(sql);
    	    state0.setObject(1, this.stuId);
    	    state0.setObject(2, tid);
    	    ResultSet set = db.executeQuery(state0);
    	    stuScore = new ArrayList<Score>();
    	    while(set.next())
  		  {
  				Score s = new Score();
  				s.setSubject(set.getString(1));
  				s.setScore(set.getDouble(2));
				s.setId(set.getInt(4));
  				int grade = set.getInt(3);
  				switch(grade)
  				{
  				  case 1:{s.setGrade("大一"); break;}
  				  case 2:{s.setGrade("大二"); break;}
  				  case 3:{s.setGrade("大三"); break;}
  				  case 4:{s.setGrade("大四"); break;}
  				}
  				stuScore.add(s);
  		  }
  		  
  		  return "SUCCESS";
    	}catch(Exception e)
    	{
    		return "ERROR";
    	}
    	
	}
}

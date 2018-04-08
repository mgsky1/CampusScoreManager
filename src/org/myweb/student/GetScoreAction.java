package org.myweb.student;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.myweb.dao.Dao;
import org.myweb.vo.*;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;


public class GetScoreAction extends ActionSupport 
{
	private List<Score> stuScore;
	private Dao db = new Dao();
	ActionContext actionContext = ActionContext.getContext();//Action上下文，用于获取和设置Session
	
	public List<Score> getStuScore() {
		return stuScore;
	}
	
	public void setStuScore(List<Score> stuScore) {
		this.stuScore = stuScore;
	}
	
	public ResultSet getScore()
	{
		//通过session获取用户id
	     Map session = actionContext.getSession();
	   	 String id = session.get("id").toString();
	   	 String sql = "SELECT subjectAll.`name` , score.score , score.grade , score.id ,score.stu FROM score , subjectAll WHERE score.stu = ? and score.`subject` in (subjectAll.id) GROUP BY subjectAll.name";
	   	 try
	   	 {
	   		 PreparedStatement state0 = db.getConn().prepareStatement(sql);
		   	 state0.setObject(1, id);
		   	 return db.executeQuery(state0);
	   	 }catch(Exception e)
	   	 {
	   		 e.printStackTrace();
	   		 return null;
	   	 }	   	
	}
	
	public String execute() 
	{
	  try
	  {
		  ResultSet set = getScore();
		  stuScore = new ArrayList<Score>();
		  
		  while(set.next())
		  {
				Score s = new Score();
				s.setSubject(set.getString(1));
				s.setScore(set.getDouble(2));
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

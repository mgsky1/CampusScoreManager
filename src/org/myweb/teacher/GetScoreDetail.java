package org.myweb.teacher;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.myweb.dao.Dao;

public class GetScoreDetail 
{
	private int stuId;
	private String grade;
	private String subject;
	private double score;
	private String stuName;
	
	public GetScoreDetail(int id) throws Exception
	{
		Dao db = new Dao();
		String sql = "SELECT "+
	"`user`.id, "+
	"`user`.realName, "+
	"student.grade, "+
	"`subject`.`name`, "+
	"score.score "+
   "FROM "+
	"("+
		"("+
			"`user` "+
			"INNER JOIN student ON `user`.id = student.id "+
		")"+
		"INNER JOIN score ON `user`.id = score.stu "+
	")"+
  "INNER JOIN `subject` ON score.`subject` = `subject`.id "+
   "WHERE "+
	"score.id = ?";
		PreparedStatement state0 = db.getConn().prepareStatement(sql);
		state0.setObject(1, id);
		ResultSet set = db.executeQuery(state0);
		int grade_num = -1;
		while(set.next())
		{
			this.stuId = set.getInt(1);
			this.stuName = set.getString(2);
			grade_num = set.getInt(3);
			this.subject = set.getString(4);
			this.score = set.getDouble(5);
			
		}
		
		switch(grade_num)
		{
		   case 1 :{this.grade = "大一"; break;}
		   case 2 :{this.grade = "大二"; break;}
		   case 3 :{this.grade = "大三"; break;}
		   case 4 :{this.grade = "大四"; break;}
		}
	}
	
	public int getStuId() {
		return stuId;
	}
	
	public String getGrade() {
		return grade;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public double getScore() {
		return score;
	}
	
	public String getStuName() {
		return stuName;
	}
	
}

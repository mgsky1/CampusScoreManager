package org.myweb.teacher;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.myweb.dao.Dao;
import org.myweb.vo.Score;
import org.myweb.vo.Student;

import com.opensymphony.xwork2.ActionSupport;

public class ListStudent extends ActionSupport
{
	private Dao db = new Dao();
	private List<Student> stu;
	private HttpServletRequest request;
	
	public List<Student> getStu() {
		return stu;
	}
	
	public void setStu(List<Student> stu) {
		this.stu = stu;
	}
	
	
   public String execute() throws Exception
   {
	 try
     {
	   request= ServletActionContext.getRequest();//获取HTTP Request对象
	   String name = "";
	   String sql = "SELECT `user`.id , `user`.realName , student.grade FROM `user` , student WHERE `user`.id in (student.id)";
	   PreparedStatement state = db.getConn().prepareStatement(sql);
	   if(request.getParameter("name") != null)
	   {
		   //解决乱码的利器！
		   name=new String(request.getParameter("name").getBytes("iso-8859-1"),"UTF-8");
		   if(!name.equals(""))
		   {
			   sql = "SELECT `user`.id , `user`.realName , student.grade FROM `user` , student WHERE  `user`.realName = ? AND `user`.id in (student.id)";
			   state = db.getConn().prepareStatement(sql);
			   state.setObject(1, name);
		   }
		  
	   }
	 
		
		  ResultSet set = db.executeQuery(state);
		  stu = new ArrayList<Student>();
		  while(set.next())
		  {
			  Student t = new Student();
			  t.setId(set.getString(1));
			  t.setName(set.getString(2));
			  t.setGrade(set.getInt(3));
			  stu.add(t);
			 // System.out.println("面包面包面包！");
		  }
		  return "SUCCESS";
		  
	  }catch(Exception e)
	  {
         return "ERROR";		  
 	  }
	  
   }
}

package org.myweb.teacher;

import java.sql.PreparedStatement;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.myweb.dao.Dao;

import com.opensymphony.xwork2.ActionSupport;

public class DelSubjectAction extends ActionSupport
{
   private HttpServletRequest request;
   private Dao db = new Dao();
   
   public String execute() throws Exception
   {
	   request = ServletActionContext.getRequest();
	   if(request.getParameter("id") != null)
	   {
		   String id = request.getParameter("id").toString();
		   String sql = "delete from subject where id = ?";
		   PreparedStatement state = db.getConn().prepareStatement(sql);
		   state.setObject(1, id);
		   return db.executeUpdate(state) == 0 ? "SUCCESS" : "FAILED";
	   }
	   else
	   {
		   return "ERROR";
	   }
   }
   
}

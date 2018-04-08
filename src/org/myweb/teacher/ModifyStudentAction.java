package org.myweb.teacher;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.myweb.dao.Dao;

import com.opensymphony.xwork2.ActionSupport;

public class ModifyStudentAction extends ActionSupport {
	private String loginName;
	private String pwd;
	private String tel;
	private String address;
	private int grade;
	private Dao db = new Dao();
	private int id;

	public int getId() {
		return id;
	}

	public String getLoginName() {
		return loginName;
	}

	public String getPwd() {
		return pwd;
	}

	public String getTel() {
		return tel;
	}

	public String getAddress() {
		return address;
	}

	public int getGrade() {
		return grade;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public void setTel(String tel) {
		this.tel = tel;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setGrade(int grade) {
		this.grade = grade;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String execute() {
		try {

			PreparedStatement state0 = db
					.getConn()
					.prepareStatement(
							"update user set password = ? , loginName = ? where id = ?");
			state0.setObject(1, this.pwd);
			state0.setObject(2, this.loginName);
			state0.setObject(3, this.id);
			int isSuucess1 = db.executeUpdate(state0);
			String id = null;

			PreparedStatement state1 = db.getConn().prepareStatement(
					"select max(id) from user");
			ResultSet set = db.executeQuery(state1);
			while (set.next()) {
				id = set.getString(1);
			}

			PreparedStatement state2 = db
					.getConn()
					.prepareStatement(
							"update student set tel = ? , address = ? , grade = ?  where id = ?");
			state2.setObject(1, this.tel);
			state2.setObject(2, this.address);
			state2.setObject(3, this.grade);
			state2.setObject(4, this.id);

			int isSuccess2 = db.executeUpdate(state2);

			if (isSuucess1 == 0 && isSuccess2 == 0) {
				return "SUCCESS";
			} else {
				addActionError("添加失败");
				return "FAILD";
			}
		} catch (Exception e) {
			addActionError("内部服务器错误");
			return "ERROR";
		}
	}
}

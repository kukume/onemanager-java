package me.kuku.onemanager.entity;

import act.db.jpa.JPADao;
import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.kuku.onemanager.pojo.DriveType;

import javax.persistence.*;

@Entity
@Table(name = "drive")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriveEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@Column(unique = true)
	private String name;
	private DriveType driveType;
	@Column(length = 30000)
	private String config;
	@Column(length = 30000)
	private String otherConfig;

	public DriveEntity(String name){
		this.name = name;
	}

	public <T> T getConfigParse(Class<T> clazz){
		if (config == null) {
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		else return JSON.parseObject(config, clazz);
	}

	public <T> void setConfigParse(T t){
		this.config = JSON.toJSONString(t);
	}

	public <T> T getOtherConfigParse(Class<T> clazz){
		if (otherConfig == null) {
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		else return JSON.parseObject(otherConfig, clazz);
	}

	public <T> void setOtherConfigParse(T t){
		this.otherConfig = JSON.toJSONString(t);
	}

	public static class Dao extends JPADao<Integer, DriveEntity> {}

}

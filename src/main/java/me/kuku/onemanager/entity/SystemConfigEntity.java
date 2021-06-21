package me.kuku.onemanager.entity;

import act.db.jpa.JPADao;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.kuku.onemanager.pojo.SystemConfigType;

import javax.persistence.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "system_config")
public class SystemConfigEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private String id;
	private SystemConfigType systemConfigType;
	private String content;

	public SystemConfigEntity(SystemConfigType systemConfigType){
		this.systemConfigType = systemConfigType;
	}

	public static class Dao extends JPADao<Integer, SystemConfigEntity> {}
}

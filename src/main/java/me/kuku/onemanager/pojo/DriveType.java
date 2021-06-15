package me.kuku.onemanager.pojo;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum DriveType {
	ONEDRIVE("onedrive");

	private final String type;

	DriveType(String type){
		this.type = type;
	}
}

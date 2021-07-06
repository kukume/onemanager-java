package me.kuku.onemanager.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnedriveItemPojo implements Serializable {
	private String id;
	private String name;
	private String createTime;
	private String lastModifiedTime;
	private Long size;
	private String url;
	private Boolean isFile;
	private String mimeType;

	public void setCreateTime(String createTime) {
		this.createTime = createTime.replace("T", " ").replace("Z", "");
	}

	public void setLastModifiedTime(String lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime.replace("T", " ").replace("Z", "");;
	}

	public OnedriveItemPojo(String id, String name, String createTime, String lastModifiedTime, Long size){
		this.id = id;
		this.name = name;
		this.createTime = createTime.replace("T", " ").replace("Z", "");
		this.lastModifiedTime = lastModifiedTime.replace("T", " ").replace("Z", "");
		this.size = size;
	}

	public OnedriveItemPojo(String id, String name, String createTime, String lastModifiedTime, Long size, String url, Boolean isFile){
		this(id, name, createTime, lastModifiedTime, size, url, isFile, null);
	}

	public OnedriveItemPojo(String id, String url, Boolean isFile){
		this.id = id;
		this.url = url;
		this.isFile = isFile;
	}

	public OnedriveItemPojo(String id, Boolean isFile){
		this.id = id;
		this.isFile = isFile;
	}
}

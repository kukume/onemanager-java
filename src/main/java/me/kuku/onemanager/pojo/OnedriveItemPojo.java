package me.kuku.onemanager.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnedriveItemPojo {
	private String id;
	private String createTime;
	private String lastModifiedTime;
	private Long size;
	private String url;
	private Boolean isFile;


	public OnedriveItemPojo(String id, String createTime, String lastModifiedTime, Long size){
		this.id = id;
		this.createTime = createTime;
		this.lastModifiedTime = lastModifiedTime;
		this.size = size;
	}
}

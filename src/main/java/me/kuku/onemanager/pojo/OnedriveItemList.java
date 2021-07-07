package me.kuku.onemanager.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnedriveItemList {
	private List<OnedriveItemPojo> list;
	private String nextLink;
}

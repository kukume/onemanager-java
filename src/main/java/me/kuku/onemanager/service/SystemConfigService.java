package me.kuku.onemanager.service;

import act.inject.AutoBind;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.pojo.SystemConfigType;

import java.util.List;
import java.util.Map;

@AutoBind
public interface SystemConfigService {
	SystemConfigEntity findByType(SystemConfigType systemConfigType);
	Map<SystemConfigType, SystemConfigEntity> findByTypeIn(SystemConfigType...systemConfigTypes);
	List<SystemConfigEntity> findAll();
	SystemConfigEntity save(SystemConfigEntity systemConfigEntity);
}

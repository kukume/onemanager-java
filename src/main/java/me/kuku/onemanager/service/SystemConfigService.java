package me.kuku.onemanager.service;

import act.inject.AutoBind;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.pojo.SystemConfigType;

import java.util.List;

@AutoBind
public interface SystemConfigService {
	SystemConfigEntity findByType(SystemConfigType systemConfigType);
	List<SystemConfigEntity> findAll();
	SystemConfigEntity save(SystemConfigEntity systemConfigEntity);
}

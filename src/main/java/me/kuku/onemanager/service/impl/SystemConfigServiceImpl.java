package me.kuku.onemanager.service.impl;

import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.pojo.SystemConfigType;
import me.kuku.onemanager.service.SystemConfigService;

import javax.inject.Inject;
import java.util.List;

public class SystemConfigServiceImpl implements SystemConfigService {

	@Inject
	private SystemConfigEntity.Dao dao;

	@Override
	public SystemConfigEntity findByType(SystemConfigType systemConfigType) {
		return dao.findOneBy("systemConfigType", systemConfigType);
	}

	@Override
	public List<SystemConfigEntity> findAll() {
		return dao.findAllAsList();
	}

	@Override
	public SystemConfigEntity save(SystemConfigEntity systemConfigEntity) {
		return dao.save(systemConfigEntity);
	}
}

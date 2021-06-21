package me.kuku.onemanager.service.impl;

import act.db.jpa.JPAQuery;
import me.kuku.onemanager.entity.SystemConfigEntity;
import me.kuku.onemanager.pojo.SystemConfigType;
import me.kuku.onemanager.service.SystemConfigService;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemConfigServiceImpl implements SystemConfigService {

	@Inject
	private SystemConfigEntity.Dao dao;

	@Override
	public SystemConfigEntity findByType(SystemConfigType systemConfigType) {
		return dao.findOneBy("systemConfigType", systemConfigType);
	}

	@Override
	public Map<SystemConfigType, SystemConfigEntity> findByTypeIn(SystemConfigType... systemConfigTypes) {
		StringBuilder sb = new StringBuilder();
		int l = systemConfigTypes.length;
		for (int i = 0; i < l; i++) {
			sb.append("systemConfigType = ?").append(i + 1);
			if (i != l - 1) sb.append(" or ");
		}
		JPAQuery<SystemConfigEntity> query = dao.createQuery("from SystemConfigEntity where " + sb, systemConfigTypes);
		List<SystemConfigEntity> list = query.getResultList();
		Map<SystemConfigType, SystemConfigEntity> map = new HashMap<>();
		list.forEach(it -> {
			map.put(it.getSystemConfigType(), it);
		});
		return map;
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

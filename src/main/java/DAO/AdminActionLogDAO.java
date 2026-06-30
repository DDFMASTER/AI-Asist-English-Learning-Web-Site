package DAO;

import Entities.AdminActionLog;

public interface AdminActionLogDAO {
    /**
     * 插入一条管理员操作日志
     * @param log 日志对象
     * @return 影响行数
     */
    int insert(AdminActionLog log);
}

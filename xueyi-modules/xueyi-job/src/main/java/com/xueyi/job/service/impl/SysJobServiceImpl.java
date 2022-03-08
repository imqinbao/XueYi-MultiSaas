package com.xueyi.job.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.xueyi.common.core.constant.job.ScheduleConstants;
import com.xueyi.common.core.exception.job.TaskException;
import com.xueyi.common.datascope.annotation.DataScope;
import com.xueyi.job.domain.dto.SysJobDto;
import com.xueyi.job.manager.SysJobManager;
import com.xueyi.job.service.ISysJobService;
import com.xueyi.job.util.ScheduleUtils;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.xueyi.common.core.constant.basic.SecurityConstants.CREATE_BY;

/**
 * 调度任务管理 服务层实现
 *
 * @author xueyi
 */
@Service
public class SysJobServiceImpl implements ISysJobService {

    @Autowired
    private SysJobManager baseManager;

    @Autowired
    private Scheduler scheduler;

    /**
     * 项目启动时，初始化定时器 主要是防止手动修改数据库导致未同步到定时任务处理（注：不能手动修改数据库Id和任务组名，否则会导致脏数据）
     */
    @PostConstruct
    public void init() throws SchedulerException, TaskException {
        scheduler.clear();
        List<SysJobDto> jobList = baseManager.selectList(null);
        for (SysJobDto job : jobList)
            ScheduleUtils.createScheduleJob(scheduler, job);
    }

    /**
     * 查询调度任务对象列表 | 数据权限 | 附加数据
     *
     * @param job 调度任务对象
     * @return 调度任务对象集合
     */
    @Override
    @DataScope(userAlias = CREATE_BY, mapperScope = "SysJobMapper")
    public List<SysJobDto> selectListScope(SysJobDto job) {
        return baseManager.selectList(job);
    }

    /**
     * 根据Id查询单条调度任务对象
     *
     * @param id Id
     * @return 调度任务对象
     */
    @Override
    public SysJobDto selectById(Long id) {
        return baseManager.selectById(id);
    }

    /**
     * 新增调度任务对象
     *
     * @param job 调度任务对象
     * @return 结果
     */
    @Override
    @DSTransactional
    public int insert(SysJobDto job) throws SchedulerException, TaskException {
        job.setStatus(ScheduleConstants.Status.PAUSE.getCode());
        int rows = baseManager.insert(job);
        if (rows > 0)
            ScheduleUtils.createScheduleJob(scheduler, job);
        return rows;
    }

    /**
     * 修改调度任务对象
     *
     * @param job 调度任务对象
     * @return 结果
     */
    @Override
    @DSTransactional
    public int update(SysJobDto job) throws SchedulerException, TaskException {
        SysJobDto properties = baseManager.selectById(job.getId());
        int rows = baseManager.update(job);
        if (rows > 0)
            updateSchedulerJob(job, properties.getJobGroup());
        return rows;
    }

    /**
     * 修改调度任务对象状态
     *
     * @param id     Id
     * @param status 状态
     * @return 结果
     */
    @Override
    @DSTransactional
    public int updateStatus(Long id, String status) throws SchedulerException {
        SysJobDto job = baseManager.selectById(id);
        return StrUtil.equals(status, ScheduleConstants.Status.NORMAL.getCode())
                ? resumeJob(job)
                : StrUtil.equals(status, ScheduleConstants.Status.PAUSE.getCode())
                ? pauseJob(job)
                : 0;
    }

    /**
     * 根据Id集合删除调度任务对象
     *
     * @param idList Id集合
     * @return 结果
     */
    @Override
    @DSTransactional
    public int deleteByIds(List<Long> idList) throws SchedulerException {
        List<SysJobDto> jobList = baseManager.selectListByIds(idList);
        int rows = baseManager.deleteByIds(idList);
        if (rows > 0) {
            for (SysJobDto job : jobList) {
                scheduler.deleteJob(ScheduleUtils.getJobKey(job.getId(), job.getJobGroup()));
            }
        }
        return rows;
    }

    /**
     * 暂停任务
     *
     * @param job 调度信息
     * @return 结果
     */
    @Override
    @DSTransactional
    public int pauseJob(SysJobDto job) throws SchedulerException {
        int row = baseManager.updateStatus(job.getId(), ScheduleConstants.Status.PAUSE.getCode());
        if (row > 0)
            scheduler.pauseJob(ScheduleUtils.getJobKey(job.getId(), job.getJobGroup()));
        return row;
    }

    /**
     * 恢复任务
     *
     * @param job 调度信息
     * @return 结果
     */
    @Override
    @DSTransactional
    public int resumeJob(SysJobDto job) throws SchedulerException {
        int row = baseManager.updateStatus(job.getId(), ScheduleConstants.Status.NORMAL.getCode());
        if (row > 0)
            scheduler.resumeJob(ScheduleUtils.getJobKey(job.getId(), job.getJobGroup()));
        return row;
    }

    /**
     * 立即运行任务
     *
     * @param job 调度信息
     */
    @Override
    @DSTransactional
    public void run(SysJobDto job) throws SchedulerException {
        Long Id = job.getId();
        String jobGroup = job.getJobGroup();
        SysJobDto properties = baseManager.selectById(Id);
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, properties);
        scheduler.triggerJob(ScheduleUtils.getJobKey(Id, jobGroup), dataMap);
    }


    /**
     * 更新任务
     *
     * @param job      任务对象
     * @param jobGroup 任务组名
     */
    public void updateSchedulerJob(SysJobDto job, String jobGroup) throws SchedulerException, TaskException {
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(job.getId(), jobGroup);
        // 防止创建时存在数据问题 先移除，然后在执行创建操作
        if (scheduler.checkExists(jobKey))
            scheduler.deleteJob(jobKey);
        ScheduleUtils.createScheduleJob(scheduler, job);
    }
}
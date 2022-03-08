package com.xueyi.job.manager;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xueyi.common.web.entity.manager.SubBaseManager;
import com.xueyi.job.domain.dto.SysJobDto;
import com.xueyi.job.domain.dto.SysJobLogDto;
import com.xueyi.job.mapper.SysJobLogMapper;
import com.xueyi.job.mapper.SysJobMapper;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 调度任务管理 数据封装层
 *
 * @author xueyi
 */
@Component
public class SysJobManager extends SubBaseManager<SysJobDto, SysJobMapper, SysJobLogDto, SysJobLogMapper> {

    /**
     * 设置主子表中子表外键值
     */
    @Override
    protected void setForeignKey(LambdaQueryWrapper<SysJobLogDto> queryWrapper, LambdaUpdateWrapper<SysJobLogDto> updateWrapper, SysJobDto job, Serializable key) {
        Serializable jobGroup = ObjectUtil.isNotNull(job) ? job.getJobGroup() : key;
        if (ObjectUtil.isNotNull(queryWrapper))
            queryWrapper.eq(SysJobLogDto::getJobGroup, jobGroup);
        else
            updateWrapper.eq(SysJobLogDto::getJobGroup, jobGroup);
    }
}

package com.xueyi.tenant.tenant.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.xueyi.common.core.constant.BaseConstants;
import com.xueyi.common.core.exception.ServiceException;
import com.xueyi.common.core.web.result.AjaxResult;
import com.xueyi.common.log.annotation.Log;
import com.xueyi.common.log.enums.BusinessType;
import com.xueyi.common.security.annotation.Logical;
import com.xueyi.common.security.annotation.RequiresPermissions;
import com.xueyi.common.security.auth.Auth;
import com.xueyi.common.web.entity.controller.BaseController;
import com.xueyi.tenant.api.source.domain.dto.TeSourceDto;
import com.xueyi.tenant.api.tenant.domain.dto.TeStrategyDto;
import com.xueyi.tenant.source.service.ITeSourceService;
import com.xueyi.tenant.tenant.service.ITeStrategyService;
import com.xueyi.tenant.tenant.service.ITeTenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.List;

/**
 * 数据源策略管理 业务处理
 *
 * @author xueyi
 */
@RestController
@RequestMapping("/strategy")
public class TeStrategyController extends BaseController<TeStrategyDto, ITeStrategyService> {

    @Autowired
    private ITeTenantService tenantService;

    @Autowired
    private ITeSourceService sourceService;

    /** 定义节点名称 */
    @Override
    protected String getNodeName() {
        return "数据源策略";
    }

    /**
     * 查询数据源策略列表
     */
    @Override
    @GetMapping("/list")
    @RequiresPermissions(Auth.TE_STRATEGY_LIST)
    public AjaxResult listExtra(TeStrategyDto strategy) {
        return super.listExtra(strategy);
    }

    /**
     * 查询源策略详细
     */
    @Override
    @GetMapping(value = "/{id}")
    @RequiresPermissions(Auth.TE_STRATEGY_SINGLE)
    public AjaxResult getInfoExtra(@PathVariable Serializable id) {
        return super.getInfoExtra(id);
    }

    /**
     * 数据源策略导出
     */
    @Override
    @PostMapping("/export")
    @RequiresPermissions(Auth.TE_STRATEGY_EXPORT)
    public void export(HttpServletResponse response, TeStrategyDto strategy) {
        super.export(response, strategy);
    }

    /**
     * 数据源策略新增
     */
    @Override
    @PostMapping
    @RequiresPermissions(Auth.TE_STRATEGY_ADD)
    @Log(title = "数据源策略管理", businessType = BusinessType.INSERT)
    public AjaxResult add(@Validated @RequestBody TeStrategyDto strategy) {
        return super.add(strategy);
    }

    /**
     * 数据源策略修改
     */
    @Override
    @PutMapping
    @RequiresPermissions(Auth.TE_STRATEGY_EDIT)
    @Log(title = "数据源策略管理", businessType = BusinessType.UPDATE)
    public AjaxResult edit(@Validated @RequestBody TeStrategyDto strategy) {
        return super.edit(strategy);
    }

    /**
     * 数据源策略修改状态
     */
    @Override
    @PutMapping("/status")
    @RequiresPermissions(value = {Auth.TE_STRATEGY_EDIT, Auth.TE_STRATEGY_EDIT_STATUS}, logical = Logical.OR)
    @Log(title = "数据源策略管理", businessType = BusinessType.UPDATE_STATUS)
    public AjaxResult editStatus(@Validated @RequestBody TeStrategyDto strategy) {
        return super.editStatus(strategy);
    }

    /**
     * 数据源策略批量删除
     */
    @Override
    @DeleteMapping("/batch/{idList}")
    @RequiresPermissions(Auth.TE_STRATEGY_DELETE)
    @Log(title = "数据源策略管理", businessType = BusinessType.DELETE)
    public AjaxResult batchRemove(@PathVariable List<Long> idList) {
        return super.batchRemove(idList);
    }

    /**
     * 获取数据源策略选择框列表
     */
    @Override
    @GetMapping("/option")
    public AjaxResult option() {
        return super.option();
    }

    /**
     * 前置校验 （强制）增加/修改
     */
    @Override
    protected void baseRefreshValidated(BaseConstants.Operate operate, TeStrategyDto strategy) {
        if (baseService.checkNameUnique(strategy.getId(), strategy.getName()))
            throw new ServiceException(StrUtil.format("{}{}{}失败，{}名称已存在", operate.getInfo(), getNodeName(), strategy.getName(), getNodeName()));
        TeSourceDto source = sourceService.selectById(strategy.getSourceId());
        if (ObjectUtil.isNull(source))
            throw new ServiceException(StrUtil.format("{}{}{}失败，设置的数据源不存在", operate.getInfo(), getNodeName(), strategy.getName()));
        else
            strategy.setSourceSlave(source.getSlave());
    }

    /**
     * 前置校验 （强制）删除
     */
    @Override
    protected void baseRemoveValidated(BaseConstants.Operate operate, List<Long> idList) {
        int size = idList.size();
        for (int i = idList.size() - 1; i >= 0; i--) {
            if (tenantService.checkStrategyExist(idList.get(i)))
                idList.remove(i);
            else if (baseService.checkIsDefault(idList.get(i)))
                idList.remove(i);
        }
        if (CollUtil.isEmpty(idList)) {
            throw new ServiceException(StrUtil.format("删除失败，默认{}及已被使用的{}不允许删除！", getNodeName(), getNodeName()));
        } else if (idList.size() != size) {
            baseService.deleteByIds(idList);
            throw new ServiceException(StrUtil.format("默认{}及已被使用的{}不允许删除，其余{}删除成功！", getNodeName(), getNodeName(), getNodeName()));
        }
    }
}
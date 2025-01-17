package com.px.mgr.controller;

import com.github.pagehelper.PageInfo;
import com.px.db.model.bean.*;
import com.px.db.model.service.*;
import com.px.db.util.PageModel;
import com.px.db.util.QueryHelper;
import com.px.db.util.QueryModel;
import com.px.db.util.SqlSortModel;
import com.px.web.controller.BaseController;
import com.px.web.util.Nav.NavEnum;
import com.px.web.util.ResponseResultUtil;
import com.px.web.util.login.Authorization;
import com.px.web.util.login.LoginUtil;
import com.px.web.util.login.PermissionEnum;
import com.px.web.util.login.PermissionGroupEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 自营平台油价管理
 */
@Slf4j
@RequestMapping("/qrcode")
@Controller
public class QrcodeController extends BaseController {


    @Resource
    private QrcodeListService qrcodeListService;

    /**
     * 二维码列表页面
     *
     * @param map
     * @return
     */
    @RequestMapping("/list.html")
    @Authorization(PermissionEnum.qrcodeManage)
    public String listInput(ModelMap map) {
        return "setting/qrcode";
    }


    /**
     * 二维码列表
     *@param page
     * @param page
     * @param limit
     * @return
     */
    @ResponseBody
    @RequestMapping("/list")
    @Authorization(PermissionEnum.qrcodeManage)
    public PageModel list(QrcodeList search,Integer page, Integer limit) {

        PageInfo<QrcodeList> pageInfo = qrcodeListService.selectLikeTWithPage(search, page, limit, new SqlSortModel(QrcodeList.LENGTH_PROPERTY, true));
        return new PageModel<>(pageInfo);
    }


    /**
     * 修改状态
     *
     * @param id     油价id
     * @param status 状态
     * @return
     */
    @ResponseBody
    @RequestMapping("/changeStatus")
    public ResponseResultUtil changeStatus(Integer id, Integer status) {
        //判空
        if (null == id) {
            return ResponseResultUtil.lose("操作失败");
        }
        if (null == status) {
            return ResponseResultUtil.lose("操作失败");
        }

        /*****************
         * 判断状态是否能修改
         ****************/
        //修改状态
        boolean isShow = false;
        if (status == 1) {
            isShow = true;
        }
        boolean flag = qrcodeListService.updateByPrimaryKey(new QrcodeList().setId(id).setIsShow(isShow));
        if(flag){
            return ResponseResultUtil.success();
        }else{
            return ResponseResultUtil.lose("操作失败");
        }
    }


}

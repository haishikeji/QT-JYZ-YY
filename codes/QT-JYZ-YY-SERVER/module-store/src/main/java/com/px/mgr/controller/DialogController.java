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
import com.px.web.util.login.LoginUtil;
import com.px.web.util.login.PermissionGroupEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.px.db.util.QueryModel.EQUALS;


@Slf4j
@RequestMapping("/dialog")
@Controller
public class DialogController extends BaseController {


    @Resource
    private OilNumberService oilNumberService;
    @Resource
    private OilTypeService oilTypeService;

    /**
     * 油号
     *
     * @param map
     * @return
     */
    @RequestMapping("/oil_number.html")
    public String oilNumberInput(ModelMap map) {
        //油品下拉选
        List<OilType> oilTypeList = oilTypeService.selectAll();
        map.put("oilTypeList", oilTypeList);
        return "dialog/oil_number";
    }

    /**
     * 油号列表
     *
     * @param oilNumber 油号
     * @param oilType   油品
     * @param page
     * @param limit
     * @return
     */
    @ResponseBody
    @RequestMapping("/oilNumberList")
    public PageModel oilNumberList(String oilNumber, Integer oilType, Integer page, Integer limit) {
        QueryHelper.SimpleQueryHandler handler = QueryHelper.startSimpleQuery();
        handler.addQuery(new QueryHelper.OrQueryModel(OilType.STATUS_PROPERTY, 1, QueryModel.EQUALS));
        //油号
        if (null != oilNumber && !"".equals(oilNumber)) {
            handler.addQuery(new QueryHelper.OrQueryModel(OilNumber.OIL_NUMBER_PROPERTY, oilNumber, QueryModel.FULL_LIKE));
        }
        //油品
        if (null != oilType) {
            handler.addQuery(new QueryHelper.OrQueryModel(OilNumber.OIL_TYPE_ID_PROPERTY, oilType, QueryModel.EQUALS));
        }
        PageInfo<OilNumber> pageInfo = oilNumberService.selectByQueryWithPage(handler, page, limit, new SqlSortModel("id", true));
        Integer count = oilNumberService.selectCountByQuery(handler);
        PageModel pageModel = new PageModel();
        pageModel.setList(pageInfo.getList()).setRel(true).setCount(count);
        return pageModel;
    }

}
